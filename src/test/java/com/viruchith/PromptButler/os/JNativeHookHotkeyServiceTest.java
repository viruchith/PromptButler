package com.viruchith.PromptButler.os;

import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JNativeHookHotkeyService} hotkey matching logic.
 */
class JNativeHookHotkeyServiceTest {

    private AtomicInteger callCount;
    private JNativeHookHotkeyService service;

    @BeforeEach
    void setUp() {
        callCount = new AtomicInteger(0);
        service = new JNativeHookHotkeyService(callCount::incrementAndGet);
    }

    /**
     * Creates a NativeKeyEvent for testing. The jNativeHook 6-param constructor
     * maps the 2nd int parameter to getModifiers() and uses setKeyCode for the key.
     */
    private static NativeKeyEvent makeKeyEvent(int id, int modifiers, int keyCode) {
        NativeKeyEvent e = new NativeKeyEvent(id, modifiers, 0, 0, NativeKeyEvent.CHAR_UNDEFINED, 0);
        e.setKeyCode(keyCode);
        return e;
    }

    @Test
    void defaultHotkey_matchesPlatformDefault() {
        int mods = JNativeHookHotkeyService.isMac()
                ? NativeKeyEvent.META_MASK | NativeKeyEvent.ALT_MASK
                : NativeKeyEvent.CTRL_MASK | NativeKeyEvent.ALT_MASK;
        NativeKeyEvent event = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, mods, NativeKeyEvent.VC_P);
        assertTrue(service.matchesHotkey(event));
    }

    @Test
    void defaultHotkey_doesNotMatchWrongKey() {
        int mods = JNativeHookHotkeyService.isMac()
                ? NativeKeyEvent.META_MASK | NativeKeyEvent.ALT_MASK
                : NativeKeyEvent.CTRL_MASK | NativeKeyEvent.ALT_MASK;
        NativeKeyEvent event = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, mods, NativeKeyEvent.VC_Q);
        assertFalse(service.matchesHotkey(event));
    }

    @Test
    void defaultHotkey_doesNotMatchMissingModifier() {
        NativeKeyEvent event = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED,
                NativeKeyEvent.ALT_MASK, NativeKeyEvent.VC_P);
        assertFalse(service.matchesHotkey(event));
    }

    @Test
    void customHotkey_matchesConfiguredKeyAndModifiers() {
        int customMods = NativeKeyEvent.CTRL_MASK | NativeKeyEvent.SHIFT_MASK;
        service.setCustomHotkey(NativeKeyEvent.VC_K, customMods);

        NativeKeyEvent match = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, customMods, NativeKeyEvent.VC_K);
        assertTrue(service.matchesHotkey(match));

        NativeKeyEvent wrongKey = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, customMods, NativeKeyEvent.VC_P);
        assertFalse(service.matchesHotkey(wrongKey));
    }

    @Test
    void customHotkey_negativeOneUsesDefault() {
        service.setCustomHotkey(-1, -1);
        assertEquals(NativeKeyEvent.VC_P, service.getEffectiveKeyCode());
    }

    @Test
    void arming_preventsDuplicateFiring() {
        int mods = JNativeHookHotkeyService.isMac()
                ? NativeKeyEvent.META_MASK | NativeKeyEvent.ALT_MASK
                : NativeKeyEvent.CTRL_MASK | NativeKeyEvent.ALT_MASK;
        NativeKeyEvent press = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, mods, NativeKeyEvent.VC_P);
        NativeKeyEvent release = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_RELEASED, 0, NativeKeyEvent.VC_P);

        service.nativeKeyPressed(press);
        assertEquals(1, callCount.get());

        service.nativeKeyPressed(press);
        assertEquals(1, callCount.get());

        service.nativeKeyReleased(release);

        service.nativeKeyPressed(press);
        assertEquals(2, callCount.get());
    }

    @Test
    void matchesDefaultHotkey_staticMethod() {
        int mods = JNativeHookHotkeyService.isMac()
                ? NativeKeyEvent.META_MASK | NativeKeyEvent.ALT_MASK
                : NativeKeyEvent.CTRL_MASK | NativeKeyEvent.ALT_MASK;
        NativeKeyEvent event = makeKeyEvent(NativeKeyEvent.NATIVE_KEY_PRESSED, mods, NativeKeyEvent.VC_P);
        assertTrue(JNativeHookHotkeyService.matchesDefaultHotkey(event));
    }
}
