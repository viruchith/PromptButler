package com.viruchith.PromptButler.core.model;

import java.util.Objects;

/**
 * Persisted UI / overlay preferences (stored as {@code preferences.json}).
 */
public final class UserPreferences {

    private AutoHideMode autoHideMode = AutoHideMode.OPACITY;
    /** Opacity when defocused and {@link AutoHideMode#OPACITY} is active (0–1). */
    private double defocusOpacity = 0.1d;

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
}
