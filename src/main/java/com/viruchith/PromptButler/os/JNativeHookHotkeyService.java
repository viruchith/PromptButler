package com.viruchith.PromptButler.os;

// SPDX-License-Identifier: GPL-3.0-only

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.viruchith.PromptButler.core.logging.AppLogger;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registers a global native keyboard listener (jNativeHook) so the overlay can be toggled with
 * a configurable hotkey (default: {@code Ctrl+Alt+P} on Windows/Linux, {@code Cmd+Alt+P} on macOS).
 * Callback runs on the native thread — callers must {@code Platform.runLater} before touching JavaFX state.
 * <p>
 * A simple re-arm on key release avoids repeat firing while the chord is held.
 * </p>
 */
public final class JNativeHookHotkeyService implements NativeKeyListener {

    /** Default key code: P */
    public static final int DEFAULT_KEY_CODE = NativeKeyEvent.VC_P;

    /** Default modifiers for Windows/Linux: Ctrl+Alt */
    public static final int DEFAULT_MODIFIERS_NON_MAC = NativeKeyEvent.CTRL_MASK | NativeKeyEvent.ALT_MASK;

    /** Default modifiers for macOS: Cmd+Alt */
    public static final int DEFAULT_MODIFIERS_MAC = NativeKeyEvent.META_MASK | NativeKeyEvent.ALT_MASK;

    private final Runnable onHotkey;
    private final AtomicBoolean armed = new AtomicBoolean(true);
    private final AtomicInteger customKeyCode = new AtomicInteger(-1);
    private final AtomicInteger customModifiers = new AtomicInteger(-1);

    public JNativeHookHotkeyService(Runnable onHotkey) {
        this.onHotkey = Objects.requireNonNull(onHotkey, "onHotkey");
    }

    /**
     * Configures a custom hotkey. Pass -1 for either parameter to use the default.
     */
    public void setCustomHotkey(int keyCode, int modifiers) {
        customKeyCode.set(keyCode);
        customModifiers.set(modifiers);
    }

    public int getEffectiveKeyCode() {
        int kc = customKeyCode.get();
        return kc >= 0 ? kc : DEFAULT_KEY_CODE;
    }

    public int getEffectiveModifiers() {
        int mod = customModifiers.get();
        if (mod >= 0) {
            return mod;
        }
        return isMac() ? DEFAULT_MODIFIERS_MAC : DEFAULT_MODIFIERS_NON_MAC;
    }

    public void start() throws NativeHookException {
        GlobalScreen.registerNativeHook();
        GlobalScreen.addNativeKeyListener(this);
    }

    public void stop() {
        GlobalScreen.removeNativeKeyListener(this);
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            AppLogger.get().warn("Could not unregister native hook: " + e.getMessage());
        }
    }

    @Override
    public void nativeKeyPressed(NativeKeyEvent e) {
        if (matchesHotkey(e) && armed.compareAndSet(true, false)) {
            onHotkey.run();
        }
    }

    @Override
    public void nativeKeyReleased(NativeKeyEvent e) {
        if (e.getKeyCode() == getEffectiveKeyCode()) {
            armed.set(true);
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
    }

    boolean matchesHotkey(NativeKeyEvent e) {
        int expectedKey = getEffectiveKeyCode();
        int expectedMods = getEffectiveModifiers();
        if (e.getKeyCode() != expectedKey) {
            return false;
        }
        // Check that all expected modifier bits are set
        return (e.getModifiers() & expectedMods) == expectedMods;
    }

    static boolean matchesDefaultHotkey(NativeKeyEvent e) {
        boolean p = e.getKeyCode() == NativeKeyEvent.VC_P;
        boolean alt = (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0;
        if (isMac()) {
            boolean meta = (e.getModifiers() & NativeKeyEvent.META_MASK) != 0;
            return p && alt && meta;
        }
        boolean ctrl = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        return p && alt && ctrl;
    }

    static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
