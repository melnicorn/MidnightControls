/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

//? if >=26.3 {
/*package eu.midnightdust.midnightcontrols.client.controller.backend;

import eu.midnightdust.midnightcontrols.MidnightControls;
import eu.midnightdust.midnightcontrols.client.controller.GamepadConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.sdl.SDL_GUID;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;

import static org.lwjgl.sdl.SDLError.SDL_ClearError;
import static org.lwjgl.sdl.SDLError.SDL_GetError;
import static org.lwjgl.sdl.SDLGUID.SDL_GUIDToString;
import static org.lwjgl.sdl.SDLGamepad.*;
import static org.lwjgl.sdl.SDLIOStream.SDL_IOFromConstMem;
import static org.lwjgl.sdl.SDLInit.SDL_INIT_GAMEPAD;
import static org.lwjgl.sdl.SDLInit.SDL_InitSubSystem;
import static org.lwjgl.sdl.SDLInit.SDL_WasInit;
import static org.lwjgl.sdl.SDLJoystick.*;
import static org.lwjgl.sdl.SDLMouse.SDL_HideCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_SetWindowRelativeMouseMode;
import static org.lwjgl.sdl.SDLMouse.SDL_ShowCursor;
import static org.lwjgl.sdl.SDLMouse.SDL_WarpMouseInWindow;
import static org.lwjgl.sdl.SDLStdinc.SDL_free;
import static org.lwjgl.sdl.SDLVideo.SDL_GetCurrentVideoDriver;

//
 // {@link InputBackend} backed by SDL3, used for Minecraft 26.3+ (which replaced GLFW with SDL3).
 // <p>
 // Design notes:
 // <ul>
 //     <li>Minecraft only initializes the SDL video subsystem and never opens gamepads, so this backend initializes
 //     {@code SDL_INIT_GAMEPAD} itself. Minecraft's event loop calls {@code SDL_PumpEvents} every frame, which also
 //     updates joystick state, so polling {@code SDL_GetGamepadButton}/{@code SDL_GetGamepadAxis} works without
 //     consuming gamepad events (Minecraft's {@code SDLEventHandler} drops them).</li>
 //     <li>SDL identifies devices by ever-increasing instance ids. The mod (and its saved configs / settings screen)
 //     expects small stable slots {@code 0..15} like GLFW's joystick ids, so this backend keeps a slot table that
 //     assigns the lowest free slot to each connected joystick and frees it on disconnect.</li>
 //     <li>Hot-plug is detected by re-enumerating {@code SDL_GetJoysticks()} every client tick (cheap) instead of
 //     relying on gamepad events, which the game's handler swallows.</li>
 //     <li>Button ids are translated to the mod's GLFW-compatible numbering ({@link GamepadConstants}); SDL's
 //     16-bit axes are normalised to GLFW's float convention (sticks -1..1, triggers -1..1).</li>
 //     <li>Reads may come from the camera thread (1 kHz). SDL's gamepad getters take the joystick lock internally
 //     and validate the handle, and the slot table is guarded with the same lock.</li>
 // </ul>
 //
 // @since 1.13.0
public final class SdlInputBackend implements InputBackend {
    private static final int NO_DEVICE = 0; // SDL_JoystickID 0 is invalid

    // GLFW-numbered button -> SDL_GamepadButton.
    private static final int[] BUTTON_TO_SDL = new int[GamepadConstants.BUTTON_COUNT];
    // GLFW-numbered axis -> SDL_GamepadAxis.
    private static final int[] AXIS_TO_SDL = new int[GamepadConstants.AXIS_COUNT];

    static {
        Arrays.fill(BUTTON_TO_SDL, SDL_GAMEPAD_BUTTON_INVALID);
        BUTTON_TO_SDL[GamepadConstants.BUTTON_A] = SDL_GAMEPAD_BUTTON_SOUTH;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_B] = SDL_GAMEPAD_BUTTON_EAST;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_X] = SDL_GAMEPAD_BUTTON_WEST;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_Y] = SDL_GAMEPAD_BUTTON_NORTH;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_LEFT_BUMPER] = SDL_GAMEPAD_BUTTON_LEFT_SHOULDER;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_RIGHT_BUMPER] = SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_BACK] = SDL_GAMEPAD_BUTTON_BACK;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_START] = SDL_GAMEPAD_BUTTON_START;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_GUIDE] = SDL_GAMEPAD_BUTTON_GUIDE;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_LEFT_THUMB] = SDL_GAMEPAD_BUTTON_LEFT_STICK;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_RIGHT_THUMB] = SDL_GAMEPAD_BUTTON_RIGHT_STICK;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_DPAD_UP] = SDL_GAMEPAD_BUTTON_DPAD_UP;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_DPAD_RIGHT] = SDL_GAMEPAD_BUTTON_DPAD_RIGHT;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_DPAD_DOWN] = SDL_GAMEPAD_BUTTON_DPAD_DOWN;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_DPAD_LEFT] = SDL_GAMEPAD_BUTTON_DPAD_LEFT;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_LEFT_PADDLE_1] = SDL_GAMEPAD_BUTTON_LEFT_PADDLE1;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_LEFT_PADDLE_2] = SDL_GAMEPAD_BUTTON_LEFT_PADDLE2;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_RIGHT_PADDLE_1] = SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1;
        BUTTON_TO_SDL[GamepadConstants.BUTTON_RIGHT_PADDLE_2] = SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2;

        AXIS_TO_SDL[GamepadConstants.AXIS_LEFT_X] = SDL_GAMEPAD_AXIS_LEFTX;
        AXIS_TO_SDL[GamepadConstants.AXIS_LEFT_Y] = SDL_GAMEPAD_AXIS_LEFTY;
        AXIS_TO_SDL[GamepadConstants.AXIS_RIGHT_X] = SDL_GAMEPAD_AXIS_RIGHTX;
        AXIS_TO_SDL[GamepadConstants.AXIS_RIGHT_Y] = SDL_GAMEPAD_AXIS_RIGHTY;
        AXIS_TO_SDL[GamepadConstants.AXIS_LEFT_TRIGGER] = SDL_GAMEPAD_AXIS_LEFT_TRIGGER;
        AXIS_TO_SDL[GamepadConstants.AXIS_RIGHT_TRIGGER] = SDL_GAMEPAD_AXIS_RIGHT_TRIGGER;
    }

    // SDL joystick instance id per slot, {@link #NO_DEVICE} if empty.
    private final int[] slotInstanceIds = new int[GamepadConstants.MAX_CONTROLLERS];
    // Open {@code SDL_Gamepad*} per slot, {@code NULL} if the slot is empty or not a mapped gamepad.
    private final long[] slotGamepads = new long[GamepadConstants.MAX_CONTROLLERS];
    private volatile boolean initialized;
    private volatile @Nullable ConnectionListener listener;

    @Override
    public @NotNull String name() {
        return "SDL3";
    }

    @Override
    public void init() {
        if (this.initialized) return;
        synchronized (this) {
            if (this.initialized) return;
            if ((SDL_WasInit(SDL_INIT_GAMEPAD) & SDL_INIT_GAMEPAD) == 0 && !SDL_InitSubSystem(SDL_INIT_GAMEPAD)) {
                MidnightControls.logger.error("[SDL3] Failed to initialize the gamepad subsystem: {}", SDL_GetError());
                return;
            }
            this.initialized = true;
            MidnightControls.log("[SDL3] Gamepad subsystem initialized (video driver: " + SDL_GetCurrentVideoDriver() + ").");
            // Populate slots for devices that are already plugged in, silently (GLFW did not fire callbacks for those either).
            this.rescan(false);
        }
    }

    @Override
    public void tick() {
        if (!this.initialized) return;
        // Makes sure joystick state is fresh even if the game's event pump did not run this tick.
        SDL_UpdateGamepads();
        this.rescan(true);
    }

    //
     // Synchronises the slot table with SDL's joystick list.
     //
     // @param notify whether to fire the connection listener for changes
    private void rescan(boolean notify) {
        IntBuffer joysticks = SDL_GetJoysticks();
        try {
            SDL_LockJoysticks();
            try {
                // Removals
                for (int slot = 0; slot < this.slotInstanceIds.length; slot++) {
                    int id = this.slotInstanceIds[slot];
                    if (id == NO_DEVICE) continue;
                    if (!contains(joysticks, id)) {
                        this.closeSlot(slot);
                        if (notify) this.fire(slot, false);
                    }
                }
                // Additions and late gamepad mappings
                if (joysticks != null) {
                    for (int i = 0; i < joysticks.limit(); i++) {
                        int id = joysticks.get(i);
                        int slot = this.slotOf(id);
                        if (slot == -1) {
                            slot = this.freeSlot();
                            if (slot == -1) continue; // more than 16 devices, ignore the rest
                            this.slotInstanceIds[slot] = id;
                            this.openGamepad(slot);
                            if (notify) this.fire(slot, true);
                        } else if (this.slotGamepads[slot] == 0L) {
                            // A mapping may have been added after the device appeared (async mapping load).
                            this.openGamepad(slot);
                        }
                    }
                }
            } finally {
                SDL_UnlockJoysticks();
            }
        } finally {
            if (joysticks != null) SDL_free(joysticks);
        }
    }

    private void openGamepad(int slot) {
        int id = this.slotInstanceIds[slot];
        if (id == NO_DEVICE || !SDL_IsGamepad(id)) return;
        long gamepad = SDL_OpenGamepad(id);
        if (gamepad == 0L) {
            MidnightControls.warn("[SDL3] Could not open gamepad " + id + ": " + SDL_GetError());
            return;
        }
        this.slotGamepads[slot] = gamepad;
    }

    private void closeSlot(int slot) {
        if (this.slotGamepads[slot] != 0L) {
            SDL_CloseGamepad(this.slotGamepads[slot]);
            this.slotGamepads[slot] = 0L;
        }
        this.slotInstanceIds[slot] = NO_DEVICE;
    }

    private void fire(int slot, boolean connected) {
        var listener = this.listener;
        if (listener == null) return;
        try {
            listener.onConnectionChanged(slot, connected);
        } catch (Exception e) {
            MidnightControls.logger.error("[SDL3] Controller connection listener failed", e);
        }
    }

    private int slotOf(int instanceId) {
        for (int slot = 0; slot < this.slotInstanceIds.length; slot++) {
            if (this.slotInstanceIds[slot] == instanceId) return slot;
        }
        return -1;
    }

    private int freeSlot() {
        for (int slot = 0; slot < this.slotInstanceIds.length; slot++) {
            if (this.slotInstanceIds[slot] == NO_DEVICE) return slot;
        }
        return -1;
    }

    private static boolean contains(@Nullable IntBuffer ids, int id) {
        if (ids == null) return false;
        for (int i = 0; i < ids.limit(); i++) {
            if (ids.get(i) == id) return true;
        }
        return false;
    }

    private boolean validSlot(int slot) {
        return this.initialized && slot >= 0 && slot < this.slotInstanceIds.length;
    }

    private int instanceId(int slot) {
        return this.validSlot(slot) ? this.slotInstanceIds[slot] : NO_DEVICE;
    }

    @Override
    public boolean isConnected(int slot) {
        return this.instanceId(slot) != NO_DEVICE;
    }

    @Override
    public boolean isGamepad(int slot) {
        if (!this.validSlot(slot)) return false;
        long gamepad = this.slotGamepads[slot];
        return gamepad != 0L && SDL_GamepadConnected(gamepad);
    }

    @Override
    public @Nullable String getName(int slot) {
        int id = this.instanceId(slot);
        if (id == NO_DEVICE) return null;
        String name = SDL_IsGamepad(id) ? SDL_GetGamepadNameForID(id) : null;
        if (name == null) name = SDL_GetJoystickNameForID(id);
        return name;
    }

    @Override
    public @NotNull String getGuid(int slot) {
        int id = this.instanceId(slot);
        if (id == NO_DEVICE) return "";
        try (var stack = MemoryStack.stackPush()) {
            SDL_GUID guid = SDL_GetJoystickGUIDForID(id, SDL_GUID.malloc(stack));
            var data = guid.data();
            boolean zero = true;
            for (int i = 0; i < data.remaining() && zero; i++) zero = data.get(i) == 0;
            if (zero) return "";
            var string = stack.malloc(33);
            SDL_GUIDToString(guid, string);
            return MemoryUtil.memASCII(MemoryUtil.memAddress(string));
        }
    }

    @Override
    public void readState(int slot, @NotNull GamepadState out) {
        out.reset();
        if (!this.validSlot(slot)) return;
        SDL_LockJoysticks();
        try {
            long gamepad = this.slotGamepads[slot];
            if (gamepad == 0L || !SDL_GamepadConnected(gamepad)) return;
            for (int button = 0; button < GamepadConstants.BUTTON_COUNT; button++) {
                int sdlButton = BUTTON_TO_SDL[button];
                if (sdlButton != SDL_GAMEPAD_BUTTON_INVALID) out.buttons[button] = SDL_GetGamepadButton(gamepad, sdlButton);
            }
            for (int axis = 0; axis < GamepadConstants.AXIS_COUNT; axis++) {
                out.axes[axis] = normalizeAxis(axis, SDL_GetGamepadAxis(gamepad, AXIS_TO_SDL[axis]));
            }
        } finally {
            SDL_UnlockJoysticks();
        }
    }

    //
     // Converts an SDL axis value to the GLFW float convention the mod was written against.
     // SDL sticks are {@code -32768..32767}; SDL triggers are {@code 0..32767} while GLFW reports triggers as
     // {@code -1 (released) .. 1 (pressed)}.
    static float normalizeAxis(int axis, short raw) {
        if (axis == GamepadConstants.AXIS_LEFT_TRIGGER || axis == GamepadConstants.AXIS_RIGHT_TRIGGER) {
            float unit = Math.max(0, raw) / 32767.f;
            return unit * 2.f - 1.f;
        }
        return raw < 0 ? raw / 32768.f : raw / 32767.f;
    }

    @Override
    public boolean addMappings(@NotNull ByteBuffer database) {
        var data = database.duplicate();
        // The mod appends a NUL terminator for GLFW; SDL works on the buffer length, so strip it.
        while (data.limit() > data.position() && data.get(data.limit() - 1) == 0) data.limit(data.limit() - 1);
        if (!data.hasRemaining()) return true;
        long stream = SDL_IOFromConstMem(data);
        if (stream == 0L) return false;
        int added = SDL_AddGamepadMappingsFromIO(stream, true);
        if (added < 0) return false;
        MidnightControls.log("[SDL3] Loaded " + added + " gamepad mapping(s).");
        return true;
    }

    @Override
    public @Nullable String pollError() {
        String error = SDL_GetError();
        if (error == null || error.isEmpty()) return null;
        SDL_ClearError();
        return error;
    }

    @Override
    public void setConnectionListener(@Nullable ConnectionListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean isWayland() {
        return "wayland".equalsIgnoreCase(SDL_GetCurrentVideoDriver());
    }

    @Override
    public void setCursorMode(long window, @NotNull CursorMode mode) {
        if (window == 0L) return;
        switch (mode) {
            case NORMAL -> {
                SDL_SetWindowRelativeMouseMode(window, false);
                SDL_ShowCursor();
            }
            case HIDDEN -> {
                SDL_SetWindowRelativeMouseMode(window, false);
                SDL_HideCursor();
            }
            case DISABLED -> {
                SDL_HideCursor();
                SDL_SetWindowRelativeMouseMode(window, true);
            }
        }
    }

    @Override
    public void setCursorPos(long window, double x, double y) {
        if (window == 0L) return;
        SDL_WarpMouseInWindow(window, (float) x, (float) y);
    }
}
*///?}
