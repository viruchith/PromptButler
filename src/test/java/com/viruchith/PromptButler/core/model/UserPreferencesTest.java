package com.viruchith.PromptButler.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
