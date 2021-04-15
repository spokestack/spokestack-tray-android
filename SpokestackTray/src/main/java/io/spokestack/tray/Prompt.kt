package io.spokestack.tray

/**
 * A message/response from the application. Contains either text or the URL of an image to be
 * displayed by the Spokestack Tray and optionally text to be spoken and an indication of whether
 * the Tray should stay open and reactivate the microphone after playing the voice response.
 */
data class Prompt(
    val text: String,
    val voice: String = text,
    val imageURL: String = "",
    val expectFollowup: Boolean = false
)
