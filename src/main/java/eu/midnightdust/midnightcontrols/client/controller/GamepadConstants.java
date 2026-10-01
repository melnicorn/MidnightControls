/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.controller;

/**
 * Mod-local gamepad button, axis and joystick-slot identifiers.
 * <p>
 * The numbering intentionally mirrors GLFW's {@code GLFW_GAMEPAD_BUTTON_*} / {@code GLFW_GAMEPAD_AXIS_*}
 * constants, because these integers are what the mod has always persisted in its configuration files
 * ({@code controller.controls.*} entries) and what {@link ButtonBinding#axisAsButton(int, boolean)} builds on.
 * Since Minecraft 26.3 the game no longer ships GLFW (it uses SDL3), so the raw platform values are translated
 * at the backend boundary ({@link eu.midnightdust.midnightcontrols.client.controller.backend.InputBackend})
 * and the rest of the mod only ever sees these values.
 *
 * @since 1.13.0
 */
public final class GamepadConstants {
    private GamepadConstants() {}

    // --- Buttons (identical to GLFW_GAMEPAD_BUTTON_*) ---
    public static final int BUTTON_A = 0;
    public static final int BUTTON_B = 1;
    public static final int BUTTON_X = 2;
    public static final int BUTTON_Y = 3;
    public static final int BUTTON_LEFT_BUMPER = 4;
    public static final int BUTTON_RIGHT_BUMPER = 5;
    public static final int BUTTON_BACK = 6;
    public static final int BUTTON_START = 7;
    public static final int BUTTON_GUIDE = 8;
    public static final int BUTTON_LEFT_THUMB = 9;
    public static final int BUTTON_RIGHT_THUMB = 10;
    public static final int BUTTON_DPAD_UP = 11;
    public static final int BUTTON_DPAD_RIGHT = 12;
    public static final int BUTTON_DPAD_DOWN = 13;
    public static final int BUTTON_DPAD_LEFT = 14;
    /** Last button GLFW can report. */
    public static final int BUTTON_GLFW_LAST = BUTTON_DPAD_LEFT;

    // --- Extended buttons (never reported by GLFW; provided by the SDL3 backend) ---
    // These ids were already reserved by the mod's icon atlas and translations (L4/L5/R4/R5).
    public static final int BUTTON_LEFT_PADDLE_1 = 15;  // L4
    public static final int BUTTON_LEFT_PADDLE_2 = 16;  // L5
    public static final int BUTTON_RIGHT_PADDLE_1 = 17; // R4
    public static final int BUTTON_RIGHT_PADDLE_2 = 18; // R5
    public static final int BUTTON_LAST = BUTTON_RIGHT_PADDLE_2;
    /** Number of button slots in a {@link eu.midnightdust.midnightcontrols.client.controller.backend.GamepadState}. */
    public static final int BUTTON_COUNT = BUTTON_LAST + 1;

    // --- Axes (identical to GLFW_GAMEPAD_AXIS_*) ---
    // Values follow the GLFW convention: sticks -1..1 (Y positive = down), triggers -1 (released) .. 1 (pressed).
    public static final int AXIS_LEFT_X = 0;
    public static final int AXIS_LEFT_Y = 1;
    public static final int AXIS_RIGHT_X = 2;
    public static final int AXIS_RIGHT_Y = 3;
    public static final int AXIS_LEFT_TRIGGER = 4;
    public static final int AXIS_RIGHT_TRIGGER = 5;
    public static final int AXIS_LAST = AXIS_RIGHT_TRIGGER;
    public static final int AXIS_COUNT = AXIS_LAST + 1;

    // --- Controller slots (identical to GLFW_JOYSTICK_*) ---
    public static final int JOYSTICK_1 = 0;
    public static final int JOYSTICK_LAST = 15;
    public static final int MAX_CONTROLLERS = JOYSTICK_LAST + 1;
}
