/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.controller.backend;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Abstraction over the native input library the game is built on.
 * <p>
 * Minecraft up to 26.2 uses GLFW ({@code GlfwInputBackend}); Minecraft 26.3+ uses SDL3 ({@code SdlInputBackend}).
 * All controller ids ("slots") handed to this interface are {@code 0..GamepadConstants.JOYSTICK_LAST},
 * matching GLFW's joystick ids, and all button/axis ids are {@link eu.midnightdust.midnightcontrols.client.controller.GamepadConstants}.
 *
 * @since 1.13.0
 */
public interface InputBackend {
    /**
     * @return a short human-readable name of the backend, for logs
     */
    @NotNull String name();

    /**
     * Initializes the backend. Must be called on the main thread once the game window exists.
     * Calling it more than once is harmless.
     */
    void init();

    /**
     * Called once per client tick on the main thread. Used for hot-plug detection where the platform
     * does not provide a callback.
     */
    void tick();

    /**
     * @return true if a joystick (gamepad or not) is present in the slot
     */
    boolean isConnected(int slot);

    /**
     * @return true if the slot holds a joystick with a known gamepad mapping
     */
    boolean isGamepad(int slot);

    /**
     * @return the device name, or null if unknown / not connected
     */
    @Nullable String getName(int slot);

    /**
     * @return the SDL-style GUID string of the device, or an empty string
     */
    @NotNull String getGuid(int slot);

    /**
     * Reads the current gamepad state into {@code out}. If the slot is not a gamepad, {@code out} is reset.
     * May be called from a thread other than the main thread (the camera thread polls at 1 kHz).
     */
    void readState(int slot, @NotNull GamepadState out);

    /**
     * Adds gamepad mappings in the SDL_GameControllerDB format.
     *
     * @param database NUL-terminated direct buffer with the mapping lines
     * @return true on success
     */
    boolean addMappings(@NotNull ByteBuffer database);

    /**
     * @return the last platform error message and clears it, or null if there was none
     */
    @Nullable String pollError();

    /**
     * Registers the callback invoked when a controller is (dis)connected.
     */
    void setConnectionListener(@Nullable ConnectionListener listener);

    /**
     * @return true if the game runs on a Wayland session (affects cursor rendering)
     */
    boolean isWayland();

    void setCursorMode(long window, @NotNull CursorMode mode);

    void setCursorPos(long window, double x, double y);

    enum CursorMode {
        /** Visible, free cursor. */
        NORMAL,
        /** Invisible, but still free (GLFW_CURSOR_HIDDEN). */
        HIDDEN,
        /** Invisible and captured, relative motion only (GLFW_CURSOR_DISABLED). */
        DISABLED
    }

    @FunctionalInterface
    interface ConnectionListener {
        void onConnectionChanged(int slot, boolean connected);
    }
}
