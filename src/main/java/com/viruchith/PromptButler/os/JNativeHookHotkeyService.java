package com.viruchith.PromptButler.os;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
import com.viruchith.PromptButler.core.logging.AppLogger;

import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Global shortcut: {@code Ctrl+Alt+P} (Windows/Linux) or {@code Cmd+Alt+P} (macOS).
 */
public final class JNativeHookHotkeyService implements NativeKeyListener {

    private final Runnable onHotkey;
    private final AtomicBoolean armed = new AtomicBoolean(true);

    public JNativeHookHotkeyService(Runnable onHotkey) {
        this.onHotkey = Objects.requireNonNull(onHotkey, "onHotkey");
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
        if (e.getKeyCode() == NativeKeyEvent.VC_P) {
            armed.set(true);
        }
    }

    @Override
    public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
    }

    static boolean matchesHotkey(NativeKeyEvent e) {
        boolean p = e.getKeyCode() == NativeKeyEvent.VC_P;
        boolean alt = (e.getModifiers() & NativeKeyEvent.ALT_MASK) != 0;
        if (isMac()) {
            boolean meta = (e.getModifiers() & NativeKeyEvent.META_MASK) != 0;
            return p && alt && meta;
        }
        boolean ctrl = (e.getModifiers() & NativeKeyEvent.CTRL_MASK) != 0;
        return p && alt && ctrl;
    }

    private static boolean isMac() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac");
    }
}
