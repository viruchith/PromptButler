package com.viruchith.PromptButler.core.model;

import java.util.Objects;

/**
 * Persisted UI / overlay preferences (stored as {@code preferences.json}).
 */
public final class UserPreferences {

    private AutoHideMode autoHideMode = AutoHideMode.OPACITY;
    /** Opacity when defocused and {@link AutoHideMode#OPACITY} is active (0–1). */
    private double defocusOpacity = 0.1d;
    /** Whether dark mode is enabled. */
    private boolean darkMode = false;
    /** Custom global hotkey key code (jNativeHook VC_ constant); -1 means default (VC_P). */
    private int hotkeyKeyCode = -1;
    /** Custom global hotkey modifiers (bitmask of jNativeHook modifier masks); -1 means default. */
    private int hotkeyModifiers = -1;
    /** Whether compiled {{variables}} should be wrapped with double quotes. Defaults to true. */
    private boolean quoteCompiledVariables = true;
    /** Default category applied to newly created prompts when not explicitly set. */
    private String defaultCategory = "General";
    /** Persisted main-window bounds; NaN means unknown/unset. */
    private double windowX = Double.NaN;
    private double windowY = Double.NaN;
    private double windowWidth = Double.NaN;
    private double windowHeight = Double.NaN;

    public AutoHideMode getAutoHideMode() {
        return autoHideMode;
    }

    public void setAutoHideMode(AutoHideMode autoHideMode) {
        this.autoHideMode = Objects.requireNonNull(autoHideMode, "autoHideMode");
    }

    public double getDefocusOpacity() {
        return defocusOpacity;
    }

    public void setDefocusOpacity(double defocusOpacity) {
        if (defocusOpacity < 0d || defocusOpacity > 1d) {
            throw new IllegalArgumentException("defocusOpacity must be between 0 and 1");
        }
        this.defocusOpacity = defocusOpacity;
    }

    public boolean isDarkMode() {
        return darkMode;
    }

    public void setDarkMode(boolean darkMode) {
        this.darkMode = darkMode;
    }

    public int getHotkeyKeyCode() {
        return hotkeyKeyCode;
    }

    public void setHotkeyKeyCode(int hotkeyKeyCode) {
        this.hotkeyKeyCode = hotkeyKeyCode;
    }

    public int getHotkeyModifiers() {
        return hotkeyModifiers;
    }

    public void setHotkeyModifiers(int hotkeyModifiers) {
        this.hotkeyModifiers = hotkeyModifiers;
    }

    public boolean isQuoteCompiledVariables() {
        return quoteCompiledVariables;
    }

    public void setQuoteCompiledVariables(boolean quoteCompiledVariables) {
        this.quoteCompiledVariables = quoteCompiledVariables;
    }

    public String getDefaultCategory() {
        return defaultCategory;
    }

    public void setDefaultCategory(String defaultCategory) {
        String normalized = defaultCategory == null ? "" : defaultCategory.trim();
        this.defaultCategory = normalized.isEmpty() ? "General" : normalized;
    }

    public double getWindowX() {
        return windowX;
    }

    public void setWindowX(double windowX) {
        this.windowX = windowX;
    }

    public double getWindowY() {
        return windowY;
    }

    public void setWindowY(double windowY) {
        this.windowY = windowY;
    }

    public double getWindowWidth() {
        return windowWidth;
    }

    public void setWindowWidth(double windowWidth) {
        this.windowWidth = windowWidth;
    }

    public double getWindowHeight() {
        return windowHeight;
    }

    public void setWindowHeight(double windowHeight) {
        this.windowHeight = windowHeight;
    }

    public boolean hasWindowBounds() {
        return !Double.isNaN(windowX)
                && !Double.isNaN(windowY)
                && !Double.isNaN(windowWidth)
                && !Double.isNaN(windowHeight);
    }
}
