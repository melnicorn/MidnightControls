/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.controller.backend;

import eu.midnightdust.midnightcontrols.client.controller.GamepadConstants;

import java.util.Arrays;

/**
 * Platform-independent snapshot of a gamepad, replacing LWJGL's {@code GLFWGamepadState}.
 * <p>
 * Indices are {@link GamepadConstants} button/axis ids; axis values use the GLFW convention
 * (sticks -1..1, triggers -1..1 with -1 meaning released).
 *
 * @since 1.13.0
 */
public final class GamepadState {
    public final boolean[] buttons = new boolean[GamepadConstants.BUTTON_COUNT];
    public final float[] axes = new float[GamepadConstants.AXIS_COUNT];

    public GamepadState() {
        this.reset();
    }

    public boolean button(int button) {
        return button >= 0 && button < this.buttons.length && this.buttons[button];
    }

    public float axis(int axis) {
        return axis >= 0 && axis < this.axes.length ? this.axes[axis] : 0.f;
    }

    /**
     * Resets to the "nothing pressed" state (triggers released, i.e. {@code -1}).
     */
    public void reset() {
        Arrays.fill(this.buttons, false);
        Arrays.fill(this.axes, 0.f);
        this.axes[GamepadConstants.AXIS_LEFT_TRIGGER] = -1.f;
        this.axes[GamepadConstants.AXIS_RIGHT_TRIGGER] = -1.f;
    }
}
