package app.revanced.extension.youtube.patches.spoof.ui

import android.view.View
import app.revanced.extension.shared.patches.spoof.requests.StreamingDataRequest.Companion.lastSpoofedClientHasSingleAudioTrack
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.patches.spoof.AudioTrackPatch
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible

@Suppress("unused")
object AudioTrackButton {
    private var instance: PlayerControlButton? = null

    /**
     * injection point
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_audio_track_button",
                hasPlaceholder = false,
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) }
            )
        } catch (ex: Exception) {
            Logger.printException({ "initializeButton failure" }, ex)
        }
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityNegatedImmediate() {
        instance?.setVisibilityNegatedImmediate()
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibilityImmediate(visible: Boolean) {
        instance?.setVisibilityImmediate(visible)
    }

    /**
     * injection point
     */
    @JvmStatic
    fun setVisibility(visible: Boolean, animated: Boolean) {
        instance?.setVisibility(visible, animated)
    }

    private fun isButtonEnabled(): Boolean {
        return Settings.SPOOF_STREAMING_DATA.get()
                && Settings.SPOOF_STREAMING_DATA_AUDIO_TRACK_BUTTON.get()
                && lastSpoofedClientHasSingleAudioTrack
                && !isAdProgressTextVisible()
                && AudioTrackPatch.audioTrackMapIsNotNull()
    }

    private fun onClick(view: View) {
        AudioTrackPatch.showAudioTrackDialog(view.context)
    }
}
