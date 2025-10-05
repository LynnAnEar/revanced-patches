package app.revanced.extension.youtube.utils;

import static app.revanced.extension.shared.utils.ResourceUtils.getColor;
import static app.revanced.extension.shared.utils.ResourceUtils.getDrawable;
import static app.revanced.extension.shared.utils.ResourceUtils.getStyleIdentifier;

import android.graphics.drawable.Drawable;

import app.revanced.extension.shared.utils.BaseThemeUtils;

public class ThemeUtils extends BaseThemeUtils {

    public static int getThemeId() {
        final String themeName = isDarkModeEnabled()
                ? "Theme.YouTube.Settings.Dark"
                : "Theme.YouTube.Settings";

        return getStyleIdentifier(themeName);
    }

    public static Drawable getBackButtonDrawable(boolean isDarkModeEnabled) {
        Drawable drawable = getDrawable("revanced_settings_toolbar_arrow_left");
        if (drawable != null) {
            drawable.setTint(getAppForegroundColor(isDarkModeEnabled));
            return drawable;
        }
        final String drawableName = isDarkModeEnabled
                ? "yt_outline_arrow_left_white_24"
                : "yt_outline_arrow_left_black_24";

        return getDrawable(drawableName);
    }

    public static Drawable getMenuButtonDrawable(boolean isDarkModeEnabled) {
        final String drawableName = isDarkModeEnabled
                ? "yt_outline_overflow_vertical_white_24"
                : "yt_outline_overflow_vertical_black_24";

        return getDrawable(drawableName);
    }

    public static Drawable getTrashButtonDrawable() {
        final String drawableName = isDarkModeEnabled()
                ? "yt_outline_trash_can_white_24"
                : "yt_outline_trash_can_black_24";

        return getDrawable(drawableName);
    }

    public static int getDialogBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black1"
                : "yt_white1";

        return getColor(colorName);
    }

    /**
     * Adjusts the background color based on the current theme.
     *
     * @param isHandleBar If true, applies a stronger darkening factor (0.9) for the handle bar in light theme;
     *                    if false, applies a standard darkening factor (0.95) for other elements in light theme.
     * @return A modified background color, lightened by 20% for dark themes or darkened by 5% (or 10% for handle bar)
     * for light themes to ensure visual contrast.
     */
    public static int getAdjustedBackgroundColor(boolean isHandleBar) {
        final int baseColor = getDialogBackgroundColor();
        float darkThemeFactor = isHandleBar ? 1.25f : 1.115f; // 1.25f for handleBar, 1.115f for others in dark theme.
        float lightThemeFactor = isHandleBar ? 0.9f : 0.95f; // 0.9f for handleBar, 0.95f for others in light theme.
        return isDarkModeEnabled()
                ? adjustColorBrightness(baseColor, darkThemeFactor)  // Lighten for dark theme.
                : adjustColorBrightness(baseColor, lightThemeFactor); // Darken for light theme.
    }

    /**
     * Since {@link android.widget.Toolbar} is used instead of {@link android.support.v7.widget.Toolbar},
     * We have to manually specify the toolbar background.
     *
     * @return toolbar background color.
     */
    public static int getToolbarBackgroundColor() {
        final String colorName = isDarkModeEnabled()
                ? "yt_black3"   // Color names used in the light theme
                : "yt_white1";  // Color names used in the dark theme

        return getColor(colorName);
    }
}
