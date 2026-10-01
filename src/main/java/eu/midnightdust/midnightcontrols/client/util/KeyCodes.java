/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.util;

//
 // Keyboard / mouse codes in the numbering the current Minecraft version uses for {@code KeyEvent},
 // {@code MouseButtonInfo} and {@code KeyMapping}.
 // <p>
 // Up to Minecraft 26.2 these are GLFW key codes and GLFW mouse button ids; since 26.3 (SDL3) key codes are
 // SDL scancodes. The mod only synthesises a handful of keys, listed here, so nothing else needs translating.
 //
 // @since 1.13.0
public final class KeyCodes {
    private KeyCodes() {}

    //? if >=26.3 {
    /*// SDL scancodes (org.lwjgl.sdl.SDLScancode); Minecraft 26.3+ KeyEvent#key() carries scancodes.
    public static final int ENTER = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RETURN;   // 40
    public static final int UP = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_UP;          // 82
    public static final int DOWN = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_DOWN;      // 81
    public static final int LEFT = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_LEFT;      // 80
    public static final int RIGHT = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_RIGHT;    // 79
    public static final int W = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_W;            // 26
    public static final int A = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_A;            // 4
    public static final int S = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_S;            // 22
    public static final int D = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_D;            // 7
    public static final int KP_8 = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_8;      // 96
    public static final int KP_6 = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_6;      // 94
    public static final int KP_2 = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_2;      // 90
    public static final int KP_4 = org.lwjgl.sdl.SDLScancode.SDL_SCANCODE_KP_4;      // 92

    // Mouse buttons and actions as Minecraft's InputConstants defines them for this version.
    public static final int MOUSE_BUTTON_LEFT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
    public static final int MOUSE_BUTTON_RIGHT = com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
    //
     // The "back" thumb button (GLFW_MOUSE_BUTTON_4 / SDL_BUTTON_X1). Both numbering schemes place it three
     // ids after the left button (GLFW: 0 -> 3, SDL: 1 -> 4), so derive it from the left button.
    public static final int MOUSE_BUTTON_BACK = MOUSE_BUTTON_LEFT + 3;
    public static final int PRESS = com.mojang.blaze3d.platform.InputConstants.PRESS;
    public static final int RELEASE = com.mojang.blaze3d.platform.InputConstants.RELEASE;
    *///?} else {
    // GLFW key codes (org.lwjgl.glfw.GLFW); Minecraft up to 26.2 KeyEvent#key() carries GLFW keys.
    public static final int ENTER = org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;   // 257
    public static final int UP = org.lwjgl.glfw.GLFW.GLFW_KEY_UP;         // 265
    public static final int DOWN = org.lwjgl.glfw.GLFW.GLFW_KEY_DOWN;     // 264
    public static final int LEFT = org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT;     // 263
    public static final int RIGHT = org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT;   // 262
    public static final int W = org.lwjgl.glfw.GLFW.GLFW_KEY_W;           // 87
    public static final int A = org.lwjgl.glfw.GLFW.GLFW_KEY_A;           // 65
    public static final int S = org.lwjgl.glfw.GLFW.GLFW_KEY_S;           // 83
    public static final int D = org.lwjgl.glfw.GLFW.GLFW_KEY_D;           // 68
    public static final int KP_8 = org.lwjgl.glfw.GLFW.GLFW_KEY_KP_8;     // 328
    public static final int KP_6 = org.lwjgl.glfw.GLFW.GLFW_KEY_KP_6;     // 326
    public static final int KP_2 = org.lwjgl.glfw.GLFW.GLFW_KEY_KP_2;     // 322
    public static final int KP_4 = org.lwjgl.glfw.GLFW.GLFW_KEY_KP_4;     // 324

    public static final int MOUSE_BUTTON_LEFT = org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;   // 0
    public static final int MOUSE_BUTTON_RIGHT = org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT; // 1
    public static final int MOUSE_BUTTON_BACK = org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_4;      // 3
    public static final int PRESS = org.lwjgl.glfw.GLFW.GLFW_PRESS;                           // 1
    public static final int RELEASE = org.lwjgl.glfw.GLFW.GLFW_RELEASE;                       // 0
    //?}
}
