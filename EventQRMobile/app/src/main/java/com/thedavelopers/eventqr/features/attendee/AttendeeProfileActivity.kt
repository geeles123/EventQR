package com.thedavelopers.eventqr.features.attendee

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.thedavelopers.eventqr.core.api.NetworkResult
import com.thedavelopers.eventqr.R
import com.thedavelopers.eventqr.core.session.SessionManager
import com.thedavelopers.eventqr.core.util.RoleMapper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

open class AttendeeProfileActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AttendeeRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        sessionManager = SessionManager(this)
        repository = AttendeeRepository(this)
        
        findViewById<View>(R.id.cardEditProfile).setOnClickListener {
            startActivity(Intent(this, AttendeeEditProfileActivity::class.java))
        }
        findViewById<View>(R.id.cardMyEvents).setOnClickListener {
            startActivity(Intent(this, RegisteredEventsActivity::class.java))
        }
        findViewById<View>(R.id.cardTransactionHistory).setOnClickListener {
            startActivity(Intent(this, AttendeeTransactionsActivity::class.java))
        }
        findViewById<View>(R.id.cardClaimedRewards).setOnClickListener {
            startActivity(Intent(this, ClaimedRewardsActivity::class.java))
        }
        findViewById<View>(R.id.cardMyEventRequests).setOnClickListener {
            startActivity(Intent(this, MyEventRequestsActivity::class.java))
        }
        findViewById<View>(R.id.cardNotifications).setOnClickListener {
            startActivity(Intent(this, AttendeeNotificationsActivity::class.java))
        }

        findViewById<Button>(R.id.btnProfileLogout).setOnClickListener {            sessionManager.clearSession()
            startActivity(
                Intent(this, com.thedavelopers.eventqr.features.auth.login.LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }

        configureAttendeeBottomNav(AttendeeBottomNavItem.PROFILE)
    }

    override fun onResume() {
        super.onResume()
        loadProfile()
    }

    private fun loadProfile() {
        // Initial sync from session
        renderProfile()

        // Fresh fetch from backend
        lifecycleScope.launch {
            when (val result = repository.getMyProfile()) {
                is com.thedavelopers.eventqr.core.api.NetworkResult.Success -> {
                    val user = result.data
                    sessionManager.updateProfile(user.fullName, user.phoneNumber)
                    // If login didn't provide role/email or it changed, update it too
                    // Note: SessionManager doesn't have an update for these yet, but render will use what's there
                    renderProfile()
                }
                else -> Unit
            }
        }
    }

    private fun renderProfile() {
        findViewById<TextView>(R.id.txtProfileName)?.text =
            sessionManager.getFullName()?.takeIf { it.isNotBlank() } ?: "Attendee"
        findViewById<TextView>(R.id.txtProfileRole)?.text =
            RoleMapper.getDisplayName(sessionManager.getUserRole())
        findViewById<TextView>(R.id.txtProfileEmail)?.text =
            sessionManager.getEmail()?.takeIf { it.isNotBlank() } ?: "No email saved"
        findViewById<TextView>(R.id.txtPhone)?.text =
            sessionManager.getPhone()?.takeIf { it.isNotBlank() } ?: "No Phone Number saved"
    }
}

open class AttendeeEditProfileActivity : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var repository: AttendeeRepository

    private lateinit var btnBack: ImageButton
    private lateinit var edtFullName: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPhone: EditText
    private lateinit var imgAvatar: ImageView
    private lateinit var imgAvatarPlaceholder: ImageView
    private lateinit var txtChangePhoto: TextView
    private lateinit var txtApiError: TextView
    private lateinit var progressLoading: ProgressBar
    private lateinit var btnSaveChanges: Button

    private var initialFullName: String = ""
    private var initialEmail: String = ""
    private var initialPhone: String = ""
    private var initialAvatarPath: String? = null

    private var selectedAvatarFile: File? = null
    private var avatarChanged: Boolean = false
    private var isLoadingProfile: Boolean = false
    private var isSavingProfile: Boolean = false

    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) {
            return@registerForActivityResult
        }
        clearApiError()
        val avatarFile = cacheSelectedAvatar(uri)
        if (avatarFile == null) {
            showApiError("Unable to read selected photo. Please try a different image.")
            return@registerForActivityResult
        }

        selectedAvatarFile = avatarFile
        avatarChanged = true
        renderAvatar(avatarFile)
        updateSaveButtonState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        sessionManager = SessionManager(this)
        repository = AttendeeRepository(this)

        bindViews()
        bindActions()
        prefillFromSession()
        loadCurrentProfile()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackImage)
        edtFullName = findViewById(R.id.edtFullName)
        edtEmail = findViewById(R.id.edtEmail)
        edtPhone = findViewById(R.id.edtPhone)
        imgAvatar = findViewById(R.id.imgAvatar)
        imgAvatarPlaceholder = findViewById(R.id.imgAvatarPlaceholder)
        txtChangePhoto = findViewById(R.id.txtChangePhoto)
        txtApiError = findViewById(R.id.txtApiError)
        progressLoading = findViewById(R.id.progressLoading)
        btnSaveChanges = findViewById(R.id.btnSaveChanges)
    }

    private fun bindActions() {
        btnBack.setOnClickListener { finish() }
        txtChangePhoto.setOnClickListener {
            if (isLoadingProfile || isSavingProfile) {
                return@setOnClickListener
            }
            photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSaveChanges.setOnClickListener {
            attemptSave()
        }

        val formWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable?) {
                clearApiError()
                clearFieldErrors()
                updateSaveButtonState()
            }
        }

        edtFullName.addTextChangedListener(formWatcher)
        edtEmail.addTextChangedListener(formWatcher)
        edtPhone.addTextChangedListener(formWatcher)
    }

    private fun prefillFromSession() {
        edtFullName.setText(sessionManager.getFullName().orEmpty())
        edtEmail.setText(sessionManager.getEmail().orEmpty())
        edtPhone.setText(sessionManager.getPhone().orEmpty())

        val avatarPath = sessionManager.getAvatarLocalPath()
        initialAvatarPath = avatarPath
        if (!avatarPath.isNullOrBlank()) {
            val avatarFile = File(avatarPath)
            if (avatarFile.exists()) {
                renderAvatar(avatarFile)
            }
        }

        captureInitialFormSnapshot()
        updateSaveButtonState()
    }

    private fun loadCurrentProfile() {
        setLoadingState(true)
        lifecycleScope.launch {
            when (val profileResult = repository.getMyProfile()) {
                is NetworkResult.Success -> {
                    val user = profileResult.data
                    edtFullName.setText(user.fullName)
                    edtEmail.setText(user.email)
                    edtPhone.setText(user.phoneNumber.orEmpty())

                    sessionManager.updateProfile(
                        fullName = user.fullName,
                        phone = user.phoneNumber,
                        email = user.email
                    )
                }

                is NetworkResult.Error -> {
                    showApiError(profileResult.message)
                }

                else -> Unit
            }

            captureInitialFormSnapshot()
            setLoadingState(false)
        }
    }

    private fun attemptSave() {
        clearApiError()
        clearFieldErrors()

        if (!validateForm()) {
            return
        }

        if (sanitizeEmail() != initialEmail) {
            edtEmail.error = "Email updates are not supported."
            showApiError("Email updates are not supported by this account endpoint.")
            return
        }

        if (!hasChanges()) {
            return
        }

        isSavingProfile = true
        updateSaveButtonState()

        val fullName = sanitizeName()
        val phone = sanitizePhone().ifBlank { null }

        lifecycleScope.launch {
            when (val updateResult = repository.updateProfile(fullName, phone)) {
                is NetworkResult.Success -> {
                    if (avatarChanged) {
                        val avatarFile = selectedAvatarFile
                        if (avatarFile == null || !avatarFile.exists()) {
                            showApiError("Selected photo is unavailable. Please choose the image again.")
                            isSavingProfile = false
                            updateSaveButtonState()
                            return@launch
                        }
                        when (val avatarResult = repository.uploadAvatar(avatarFile)) {
                            is NetworkResult.Error -> {
                                showApiError(avatarResult.message)
                                isSavingProfile = false
                                updateSaveButtonState()
                                return@launch
                            }

                            else -> Unit
                        }
                        sessionManager.setAvatarLocalPath(avatarFile.absolutePath)
                        initialAvatarPath = avatarFile.absolutePath
                        avatarChanged = false
                    }

                    sessionManager.updateProfile(fullName, phone, initialEmail)
                    captureInitialFormSnapshot()
                    Toast.makeText(this@AttendeeEditProfileActivity, "Profile updated successfully.", Toast.LENGTH_SHORT).show()
                    setResult(RESULT_OK)
                    finish()
                }

                is NetworkResult.Error -> {
                    showApiError(updateResult.message)
                }

                else -> Unit
            }

            isSavingProfile = false
            updateSaveButtonState()
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        if (sanitizeName().isBlank()) {
            edtFullName.error = "Full name is required."
            isValid = false
        }

        val email = sanitizeEmail()
        if (email.isBlank()) {
            edtEmail.error = "Email address is required."
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            edtEmail.error = "Enter a valid email address."
            isValid = false
        }

        val phone = sanitizePhone()
        if (phone.isBlank()) {
            edtPhone.error = "Phone number is required."
            isValid = false
        } else if (!isPhoneValid(phone)) {
            edtPhone.error = "Enter a valid phone number."
            isValid = false
        }

        return isValid
    }

    private fun isPhoneValid(phone: String): Boolean {
        val pattern = Regex("^[+]?[-0-9()\\s]{7,20}$")
        val digits = phone.count { it.isDigit() }
        return pattern.matches(phone) && digits in 7..15
    }

    private fun hasChanges(): Boolean {
        return sanitizeName() != initialFullName ||
            sanitizeEmail() != initialEmail ||
            sanitizePhone() != initialPhone ||
            avatarChanged
    }

    private fun captureInitialFormSnapshot() {
        initialFullName = sanitizeName()
        initialEmail = sanitizeEmail()
        initialPhone = sanitizePhone()
    }

    private fun sanitizeName(): String = edtFullName.text.toString().trim()

    private fun sanitizeEmail(): String = edtEmail.text.toString().trim()

    private fun sanitizePhone(): String = edtPhone.text.toString().trim()

    private fun clearFieldErrors() {
        edtFullName.error = null
        edtEmail.error = null
        edtPhone.error = null
    }

    private fun showApiError(message: String) {
        txtApiError.text = message
        txtApiError.visibility = View.VISIBLE
    }

    private fun clearApiError() {
        txtApiError.text = ""
        txtApiError.visibility = View.GONE
    }

    private fun setLoadingState(loading: Boolean) {
        isLoadingProfile = loading
        progressLoading.visibility = if (loading) View.VISIBLE else View.GONE

        edtFullName.isEnabled = !loading
        edtEmail.isEnabled = !loading
        edtPhone.isEnabled = !loading
        txtChangePhoto.isEnabled = !loading

        updateSaveButtonState()
    }

    private fun updateSaveButtonState() {
        val canSave = !isLoadingProfile && !isSavingProfile && hasChanges()
        btnSaveChanges.isEnabled = canSave
        btnSaveChanges.text = if (isSavingProfile) "Saving..." else "Save Changes"
    }

    private fun cacheSelectedAvatar(uri: Uri): File? {
        return runCatching {
            val avatarDirectory = File(filesDir, "avatars")
            if (!avatarDirectory.exists()) {
                avatarDirectory.mkdirs()
            }

            val userKey = sessionManager.getUserId()?.takeIf { it.isNotBlank() } ?: "current"
            val targetFile = File(avatarDirectory, "avatar_$userKey.jpg")

            contentResolver.openInputStream(uri).use { input ->
                if (input == null) {
                    return null
                }
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile
        }.getOrNull()
    }

    private fun renderAvatar(file: File) {
        if (!file.exists()) {
            imgAvatar.setImageDrawable(null)
            imgAvatarPlaceholder.visibility = View.VISIBLE
            return
        }

        imgAvatar.setImageURI(Uri.fromFile(file))
        imgAvatarPlaceholder.visibility = View.GONE
    }
}
