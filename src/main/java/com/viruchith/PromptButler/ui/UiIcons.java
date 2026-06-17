package com.viruchith.PromptButler.ui;

import javafx.scene.paint.Color;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Shared Font Awesome (solid) icons for toolbar, list rows, and dialogs.
 */
public final class UiIcons {

    private static final int DEFAULT_SIZE = 15;
    private static final Color DEFAULT_COLOR = Color.web("#111827");

    private UiIcons() {
    }

    public static FontIcon solid(FontAwesomeSolid which) {
        FontIcon icon = new FontIcon(which);
        icon.setIconSize(DEFAULT_SIZE);
        icon.setIconColor(DEFAULT_COLOR);
        return icon;
    }
}
