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

/**
 * Holds the {@link InputBackend} matching the Minecraft version this jar was built for.
 *
 * @since 1.13.0
 */
public final class InputBackends {
    private InputBackends() {}

    private static final InputBackend BACKEND =
            //? if >=26.3 {
            /*new SdlInputBackend();
            *///?} else {
            new GlfwInputBackend();
            //?}

    public static @NotNull InputBackend get() {
        return BACKEND;
    }
}
