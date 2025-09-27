package app.revanced.extension.youtube.settings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.Toolbar;

import app.revanced.extension.shared.settings.BaseActivityHook;
import app.revanced.extension.shared.utils.Utils;
import app.revanced.extension.youtube.settings.preference.YouTubePreferenceFragment;
import app.revanced.extension.youtube.settings.search.YouTubeSearchViewController;
import app.revanced.extension.youtube.utils.ThemeUtils;

import static app.revanced.extension.youtube.utils.ExtendedUtils.IS_19_34_OR_GREATER;
import static app.revanced.extension.youtube.utils.ExtendedUtils.isSpoofingToLessThan;

/**
 * Hooks LicenseActivity to inject a custom {@link YouTubePreferenceFragment} with a toolbar and search functionality.
 */
@SuppressWarnings("deprecation")
public class YouTubeActivityHook extends BaseActivityHook {

    private static int currentThemeValueOrdinal = -1; // Must initially be a non-valid enum ordinal value.

    /**
     * Controller for managing search view components in the toolbar.
     */
    @SuppressLint("StaticFieldLeak")
    public static YouTubeSearchViewController searchViewController;

    /**
     * Injection point.
     */
    @SuppressWarnings("unused")
    public static void initialize(Activity parentActivity) {
        BaseActivityHook.initialize(new YouTubeActivityHook(), parentActivity);
    }

    /**
     * Customizes the activity theme based on dark/light mode.
     */
    @Override
    protected void customizeActivityTheme(Activity activity) {
        final var theme = ThemeUtils.isDarkModeEnabled()
                ? "Theme.YouTube.Settings.Dark"
                : "Theme.YouTube.Settings";
        activity.setTheme(Utils.getResourceIdentifierOrThrow(theme, "style"));
    }

    /**
     * Returns the resource ID for the YouTube settings layout.
     */
    @Override
    protected int getContentViewResourceId() {
        return LAYOUT_REVANCED_SETTINGS_WITH_TOOLBAR;
    }

    /**
     * Returns the toolbar background color based on dark/light mode.
     */
    @Override
    protected int getToolbarBackgroundColor() {
        final String colorName = ThemeUtils.isDarkModeEnabled()
                ? "yt_black3"
                : "yt_white1";
        return Utils.getColorFromString(colorName);
    }

    /**
     * Returns the navigation icon drawable for the toolbar.
     */
    @Override
    protected Drawable getNavigationIcon() {
        return ThemeUtils.getBackButtonDrawable();
    }

    /**
     * Returns the click listener for the navigation icon.
     */
    @Override
    protected View.OnClickListener getNavigationClickListener(Activity activity) {
        return null;
    }

    /**
     * Adds search view components to the toolbar for {@link YouTubePreferenceFragment}.
     *
     * @param activity The activity hosting the toolbar.
     * @param toolbar  The configured toolbar.
     * @param fragment The PreferenceFragment associated with the activity.
     */
    @Override
    protected void onPostToolbarSetup(Activity activity, Toolbar toolbar, PreferenceFragment fragment) {
        if (fragment instanceof YouTubePreferenceFragment) {
            searchViewController = YouTubeSearchViewController.addSearchViewComponents(
                    activity, toolbar, (YouTubePreferenceFragment) fragment);
        }
    }

    /**
     * Creates a new {@link YouTubePreferenceFragment} for the activity.
     */
    @Override
    protected PreferenceFragment createPreferenceFragment() {
        return new YouTubePreferenceFragment();
    }

    /**
     * Injection point.
     */
    @SuppressWarnings("unused")
    public static boolean useCairoSettingsFragment(boolean original) {
        // Early targets have layout issues and it's better to always force off.
        if (!IS_19_34_OR_GREATER) {
            return false;
        }
        // Spoofing can cause half broken settings menus of old and new settings.
        if (isSpoofingToLessThan("19.35.36")) {
            return false;
        }

        // On the first launch of a clean install, forcing the cairo menu can give a
        // half broken appearance because all the preference icons may not be available yet.
        // 19.34+ cairo settings are always on, so it doesn't need to be forced anyway.
        // Cairo setting will show on the next launch of the app.
        return original;
    }

    /**
     * Injection point.
     * <p>
     * Updates dark/light mode since YT settings can force light/dark mode
     * which can differ from the global device settings.
     */
    @SuppressWarnings("unused")
    public static void updateLightDarkModeStatus(Enum<?> value) {
        final int themeOrdinal = value.ordinal();
        if (currentThemeValueOrdinal != themeOrdinal) {
            currentThemeValueOrdinal = themeOrdinal;
            ThemeUtils.setIsDarkModeEnabled(themeOrdinal == 1);
        }
    }

    /**
     * @return Whether pressing the back button should be allowed.
     */
    @SuppressWarnings("unused")
    public static boolean handleBackPress() {
        return YouTubeSearchViewController.handleFinish(searchViewController);
    }
}
