/*
 * Copyright © 2021 LambdAurora <aurora42lambda@gmail.com>
 *
 * This file is part of midnightcontrols.
 *
 * Licensed under the MIT license. For more information,
 * see the LICENSE file.
 */

package eu.midnightdust.midnightcontrols.client.util;

import eu.midnightdust.midnightcontrols.client.mixin.MouseAccessor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;

/**
 * Helpers that hide version differences of {@code MouseHandler}.
 *
 * @since 1.13.0
 */
public final class MouseUtil {
    private MouseUtil() {}

    /**
     * Feeds a synthetic cursor position into the game's mouse handler, as if the platform had reported it.
     * <p>
     * Since 26.3 (SDL3) {@code MouseHandler#onMove} also receives the relative motion, which is derived here
     * from the previous position.
     */
    public static void onCursorPos(@NotNull Minecraft client, double x, double y) {
        var accessor = (MouseAccessor) client.mouseHandler;
        //? if >=26.3 {
        /*double deltaX = x - client.mouseHandler.xpos();
        double deltaY = y - client.mouseHandler.ypos();
        accessor.midnightcontrols$onCursorPos(client.getWindow().handle(), x, y, deltaX, deltaY);
        *///?} else {
        accessor.midnightcontrols$onCursorPos(client.getWindow().handle(), x, y);
        //?}
    }
}
