/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

//? if <26.3 {
package eu.midnightdust.midnightcontrols.client.controller.backend;

import eu.midnightdust.midnightcontrols.client.controller.GamepadConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWGamepadState;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;

//
 // {@link InputBackend} backed by GLFW, used for Minecraft versions up to 26.2.
 // <p>
 // GLFW's gamepad button/axis numbering is identical to {@link GamepadConstants}, so no translation is needed.
 //
 // @since 1.13.0
public final class GlfwInputBackend implements InputBackend {
    @Override
    public @NotNull String name() {
        return "GLFW";
    }

    @Override
    public void init() {
        // GLFW is initialized by the game itself; joysticks are available as soon as the window exists.
    }

    @Override
    public void tick() {
        // GLFW delivers connection changes through the joystick callback; nothing to poll.
    }

    @Override
    public boolean isConnected(int slot) {
        return GLFW.glfwJoystickPresent(slot);
    }

    @Override
    public boolean isGamepad(int slot) {
        return this.isConnected(slot) && GLFW.glfwJoystickIsGamepad(slot);
    }

    @Override
    public @Nullable String getName(int slot) {
        return this.isGamepad(slot) ? GLFW.glfwGetGamepadName(slot) : GLFW.glfwGetJoystickName(slot);
    }

    @Override
    public @NotNull String getGuid(int slot) {
        String guid = GLFW.glfwGetJoystickGUID(slot);
        return guid == null ? "" : guid;
    }

    @Override
    public void readState(int slot, @NotNull GamepadState out) {
        out.reset();
        if (!this.isGamepad(slot)) return;
        try (var stack = MemoryStack.stackPush()) {
            var state = GLFWGamepadState.malloc(stack);
            if (!GLFW.glfwGetGamepadState(slot, state)) return;
            var buttons = state.buttons();
            int buttonCount = Math.min(buttons.limit(), GamepadConstants.BUTTON_GLFW_LAST + 1);
            for (int i = 0; i < buttonCount; i++) {
                out.buttons[i] = buttons.get(i) == (byte) GLFW.GLFW_PRESS;
            }
            var axes = state.axes();
            int axisCount = Math.min(axes.limit(), GamepadConstants.AXIS_COUNT);
            for (int i = 0; i < axisCount; i++) {
                out.axes[i] = axes.get(i);
            }
        }
    }

    @Override
    public boolean addMappings(@NotNull ByteBuffer database) {
        return GLFW.glfwUpdateGamepadMappings(database);
    }

    @Override
    public @Nullable String pollError() {
        try (var stack = MemoryStack.stackPush()) {
            var description = stack.mallocPointer(1);
            int code = GLFW.glfwGetError(description);
            if (code == GLFW.GLFW_NO_ERROR) return null;
            long address = description.get(0);
            return address == 0L ? "GLFW error " + code : MemoryUtil.memUTF8(address);
        }
    }

    @Override
    public void setConnectionListener(@Nullable ConnectionListener listener) {
        if (listener == null) {
            GLFW.glfwSetJoystickCallback(null);
            return;
        }
        GLFW.glfwSetJoystickCallback((jid, event) -> {
            if (event == GLFW.GLFW_CONNECTED) listener.onConnectionChanged(jid, true);
            else if (event == GLFW.GLFW_DISCONNECTED) listener.onConnectionChanged(jid, false);
        });
    }

    @Override
    public boolean isWayland() {
        return GLFW.glfwGetVersionString().contains("Wayland");
    }

    @Override
    public void setCursorMode(long window, @NotNull CursorMode mode) {
        int glfwMode = switch (mode) {
            case NORMAL -> GLFW.GLFW_CURSOR_NORMAL;
            case HIDDEN -> GLFW.GLFW_CURSOR_HIDDEN;
            case DISABLED -> GLFW.GLFW_CURSOR_DISABLED;
        };
        GLFW.glfwSetInputMode(window, GLFW.GLFW_CURSOR, glfwMode);
    }

    @Override
    public void setCursorPos(long window, double x, double y) {
        GLFW.glfwSetCursorPos(window, x, y);
    }
}
//?}
