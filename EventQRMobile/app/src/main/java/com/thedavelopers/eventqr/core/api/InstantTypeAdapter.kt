package com.thedavelopers.eventqr.core.api

import com.google.gson.*
import java.lang.reflect.Type
import java.time.Instant

object InstantTypeAdapter : JsonSerializer<Instant>, JsonDeserializer<Instant> {
    override fun serialize(src: Instant?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return JsonPrimitive(src?.toString())
    }

    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Instant? {
        val value = json?.asString?.trim().orEmpty()
        return if (value.isBlank()) null else Instant.parse(value)
    }
}
