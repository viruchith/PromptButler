package com.viruchith.PromptButler.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserPreferencesTest {

    @Test
    void rejectsInvalidOpacity() {
        UserPreferences p = new UserPreferences();
        assertThrows(IllegalArgumentException.class, () -> p.setDefocusOpacity(1.5));
    }

    @Test
    void roundTripOpacity() {
        UserPreferences p = new UserPreferences();
        p.setDefocusOpacity(0.5);
        assertEquals(0.5, p.getDefocusOpacity(), 0.0001);
    }

    @Test
    void defaultDarkModeIsFalse() {
        UserPreferences p = new UserPreferences();
        assertFalse(p.isDarkMode());
    }

    @Test
    void setDarkMode() {
        UserPreferences p = new UserPreferences();
        p.setDarkMode(true);
        assertTrue(p.isDarkMode());
    }

    @Test
    void defaultHotkeyIsNegativeOne() {
        UserPreferences p = new UserPreferences();
        assertEquals(-1, p.getHotkeyKeyCode());
        assertEquals(-1, p.getHotkeyModifiers());
    }

    @Test
    void setCustomHotkey() {
        UserPreferences p = new UserPreferences();
        p.setHotkeyKeyCode(42);
        p.setHotkeyModifiers(15);
        assertEquals(42, p.getHotkeyKeyCode());
        assertEquals(15, p.getHotkeyModifiers());
    }
}
