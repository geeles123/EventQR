package com.thedavelopers.eventqr.features.uploads.service;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.thedavelopers.eventqr.features.uploads.model.dto.StoredFileResponse;
import com.thedavelopers.eventqr.features.uploads.model.entity.StoredFile;
import com.thedavelopers.eventqr.features.uploads.repository.StoredFileRepository;
import com.thedavelopers.eventqr.shared.exceptions.BadRequestException;
import com.thedavelopers.eventqr.shared.exceptions.ResourceNotFoundException;

@Service
@Transactional
public class FileStorageService {

    private static final int EVENT_POSTER_MIN_WIDTH = 1200;
    private static final int EVENT_POSTER_MIN_HEIGHT = 675;
    private static final double EVENT_POSTER_MIN_RATIO = 1.55;
    private static final double EVENT_POSTER_MAX_RATIO = 1.90;

    private static final int PROFILE_PHOTO_MIN_WIDTH = 300;
    private static final int PROFILE_PHOTO_MIN_HEIGHT = 300;

    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final StoredFileRepository storedFileRepository;

    public FileStorageService(StoredFileRepository storedFileRepository) {
        this.storedFileRepository = storedFileRepository;
    }

    public StoredFileResponse store(UUID ownerId, String purpose, MultipartFile file) {
        validateFile(file, purpose);
        try {
            byte[] content = file.getBytes();
            StoredFile storedFile = new StoredFile();
            storedFile.setOwnerId(ownerId);
            storedFile.setPurpose(normalizePurpose(purpose));
            storedFile.setFileName(file.getOriginalFilename());
            storedFile.setContentType(normalizeContentType(file.getContentType(), content));
            storedFile.setSize(content.length);
            storedFile.setStoredAt(Instant.now());
            storedFile.setContent(content);
            return toResponse(storedFileRepository.save(storedFile), "STORED", true);
        } catch (IOException exception) {
            throw new BadRequestException("Unable to read uploaded file");
        }
    }

    @Transactional(readOnly = true)
    public StoredFileResponse find(UUID fileId) {
        StoredFile storedFile = requireFile(fileId);
        return toResponse(storedFile, "AVAILABLE", true);
    }

    @Transactional(readOnly = true)
    public StoredFileContent readContent(UUID fileId) {
        StoredFile storedFile = requireFile(fileId);
        String contentType = storedFile.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = MediaTypeDetector.detect(storedFile.getContent());
        }
        return new StoredFileContent(storedFile.getContent(), contentType);
    }

    public StoredFileResponse delete(UUID fileId) {
        StoredFile existing = requireFile(fileId);
        StoredFileResponse response = toResponse(existing, "DELETED", true);
        storedFileRepository.delete(existing);
        return response;
    }

    private StoredFile requireFile(UUID fileId) {
        return storedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found: " + fileId));
    }

    private void validateFile(MultipartFile file, String purpose) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is required");
        }
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedImageType(contentType)) {
            throw new BadRequestException("Only JPG and PNG image uploads are supported");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new BadRequestException("Image must not exceed 5 MB");
        }
        String normalizedPurpose = normalizePurpose(purpose);
        if ("event-poster".equals(normalizedPurpose) || "event-logo".equals(normalizedPurpose)) {
            validateEventPoster(file);
        } else if ("profile-photo".equals(normalizedPurpose)) {
            validateProfilePhoto(file);
        }
    }

    private boolean isAllowedImageType(String contentType) {
        String normalized = contentType == null ? "" : contentType.trim().toLowerCase();
        return "image/jpeg".equals(normalized) || "image/jpg".equals(normalized) || "image/png".equals(normalized);
    }

    private void validateEventPoster(MultipartFile file) {
        BufferedImage image = readImage(file, "Event poster");
        int width = image.getWidth();
        int height = image.getHeight();
        double ratio = height == 0 ? 0.0 : (double) width / (double) height;
        if (width < EVENT_POSTER_MIN_WIDTH || height < EVENT_POSTER_MIN_HEIGHT) {
            throw new BadRequestException("Event poster must be at least 1200 x 675 pixels");
        }
        if (ratio < EVENT_POSTER_MIN_RATIO || ratio > EVENT_POSTER_MAX_RATIO) {
            throw new BadRequestException("Event poster must use a landscape 16:9-style ratio");
        }
    }

    private void validateProfilePhoto(MultipartFile file) {
        BufferedImage image = readImage(file, "Profile photo");
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < PROFILE_PHOTO_MIN_WIDTH || height < PROFILE_PHOTO_MIN_HEIGHT) {
            throw new BadRequestException("Profile photo must be at least 300 x 300 pixels");
        }
    }

    private BufferedImage readImage(MultipartFile file, String label) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new BadRequestException(label + " must be a readable JPG or PNG image");
            }
            return image;
        } catch (BadRequestException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BadRequestException("Unable to validate image");
        }
    }

    private String normalizePurpose(String purpose) {
        return purpose == null || purpose.isBlank() ? "image" : purpose.trim().toLowerCase();
    }

    private String normalizeContentType(String contentType, byte[] content) {
        if (contentType != null && isAllowedImageType(contentType)) {
            return "image/jpg".equalsIgnoreCase(contentType) ? "image/jpeg" : contentType.trim().toLowerCase();
        }
        return MediaTypeDetector.detect(content);
    }

    private StoredFileResponse toResponse(StoredFile storedFile, String status, boolean includeContent) {
        byte[] content = includeContent ? storedFile.getContent() : null;
        return new StoredFileResponse(
                storedFile.getId(),
                storedFile.getOwnerId(),
                storedFile.getPurpose(),
                storedFile.getFileName(),
                storedFile.getContentType(),
                storedFile.getSize(),
                status,
                storedFile.getStoredAt(),
                encode(content));
    }

    public record StoredFileContent(byte[] content, String contentType) {
    }

    private static String encode(byte[] content) {
        return Base64.getEncoder().encodeToString(content == null ? new byte[0] : content);
    }

    private static class MediaTypeDetector {
        private static String detect(byte[] content) {
            if (content == null || content.length < 4) {
                return "application/octet-stream";
            }
            if ((content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8) {
                return "image/jpeg";
            }
            if ((content[0] & 0xFF) == 0x89 && content[1] == 0x50 && content[2] == 0x4E && content[3] == 0x47) {
                return "image/png";
            }
            return "application/octet-stream";
        }
    }
}
