package com.dot.gallery.domain.model

import androidx.compose.runtime.Stable
import com.dot.gallery.domain.uri.normalizeNfkc
import kotlinx.serialization.Serializable
import kotlin.math.roundToLong

@Stable
@Serializable
data class LocationMedia(
    val media: Media,
    val location: String,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

fun locationLabelKey(location: String): String = location.normalizedLocationPart()

fun locationCoordinateKey(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null) return null
    val canonicalLatitude = if (latitude == 0.0) 0.0 else latitude
    val canonicalLongitude = if (longitude == 0.0) 0.0 else longitude
    return "${canonicalLatitude.toBits()}/${canonicalLongitude.toBits()}"
}

fun locationCoordinateGroupKey(latitude: Double?, longitude: Double?): String? {
    if (latitude == null || longitude == null ||
        !latitude.isFinite() || !longitude.isFinite() ||
        latitude !in -90.0..90.0 || longitude !in -180.0..180.0
    ) return null
    return "${(latitude * 10_000).roundToLong()}/${(longitude * 10_000).roundToLong()}"
}

fun LocationMedia.locationIdentityKey(): String {
    if (!city.isNullOrBlank() || !country.isNullOrBlank()) {
        return "name:${locationLabelKey(location)}"
    }
    return locationCoordinateKey(latitude, longitude)
        ?.let { "coordinates:$it" }
        ?: "media:${media.id}"
}

fun matchesLocationName(
    candidateCity: String?,
    candidateCountry: String?,
    city: String,
    country: String,
): Boolean {
    val normalizedCity = city.normalizedLocationPart()
    val normalizedCountry = country.normalizedLocationPart()
    if (normalizedCity.isEmpty() && normalizedCountry.isEmpty()) return false
    val candidateLabel = normalizedLocationLabel(candidateCity, candidateCountry)
    val targetLabel = normalizedLocationLabel(city, country)
    if (candidateLabel == targetLabel) return true
    return (normalizedCity.isEmpty() || candidateCity.normalizedLocationPart() == normalizedCity) &&
            (normalizedCountry.isEmpty() || candidateCountry.normalizedLocationPart() == normalizedCountry)
}

fun matchesLocationCoordinates(
    candidateLatitude: Double?,
    candidateLongitude: Double?,
    latitude: Double?,
    longitude: Double?,
): Boolean {
    val candidateKey = locationCoordinateGroupKey(candidateLatitude, candidateLongitude)
    return candidateKey != null && candidateKey == locationCoordinateGroupKey(latitude, longitude)
}

private fun normalizedLocationLabel(city: String?, country: String?): String =
    listOf(city, country)
        .map { it.normalizedLocationPart() }
        .filter(String::isNotEmpty)
        .joinToString(", ")

private fun String?.normalizedLocationPart(): String {
    val normalized = orEmpty().normalizeNfkc()
    return buildString(normalized.length) {
        var previousWasSpace = true
        normalized.forEach { character ->
            val isSpace = character.isWhitespace() ||
                    character.category == CharCategory.SPACE_SEPARATOR
            if (isSpace) {
                if (!previousWasSpace) append(' ')
            } else {
                append(character)
            }
            previousWasSpace = isSpace
        }
    }.trimEnd().lowercase()
}
