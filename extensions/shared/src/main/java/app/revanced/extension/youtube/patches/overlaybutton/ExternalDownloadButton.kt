package app.revanced.extension.youtube.patches.overlaybutton

import android.view.View
import app.revanced.extension.shared.utils.Logger
import app.revanced.extension.youtube.patches.utils.PlaylistPatch
import app.revanced.extension.youtube.settings.Settings
import app.revanced.extension.youtube.shared.PlayerControlButton
import app.revanced.extension.youtube.shared.RootView.isAdProgressTextVisible
import app.revanced.extension.youtube.shared.VideoInformation
import app.revanced.extension.youtube.utils.VideoUtils

@Suppress("unused")
object ExternalDownloadButton {
    private var instance: PlayerControlButton? = null

    /**
     * Injection point.
     */
    @JvmStatic
    fun initializeButton(controlsView: View) {
        try {
            instance = PlayerControlButton(
                controlsViewGroup = controlsView,
                imageViewButtonId = "revanced_external_download_button",
                buttonVisibility = { isButtonEnabled() },
                onClickListener = { view: View -> onClick(view) },
                onLongClickListener = { view: View ->
                    onLongClick(view)
                    true
                }
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
        return Settings.OVERLAY_BUTTON_EXTERNAL_DOWNLOADER.get()
                && !isAdProgressTextVisible()
    }

    private fun onClick(view: View) {
        if (Settings.OVERLAY_BUTTON_EXTERNAL_DOWNLOADER_QUEUE_MANAGER.get()) {
            PlaylistPatch.prepareDialogBuilder(VideoInformation.getVideoId())
        } else {
            VideoUtils.launchVideoExternalDownloader()
        }
    }

    private fun onLongClick(view: View) {
        VideoUtils.launchLongPressVideoExternalDownloader()
    }
}
