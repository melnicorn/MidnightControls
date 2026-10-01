package eu.midnightdust.midnightcontrols.client.mixin;

import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MouseHandler.class)
public interface MouseAccessor {
    //? if >=26.3 {
    /*// SDL3-based MouseHandler also receives the relative motion of the event.
    // Use eu.midnightdust.midnightcontrols.client.util.MouseUtil#onCursorPos instead of calling this directly.
    @Invoker("onMove")
    void midnightcontrols$onCursorPos(long window, double x, double y, double deltaX, double deltaY);
    *///?} else {
    @Invoker("onMove")
    void midnightcontrols$onCursorPos(long window, double x, double y);
    //?}
    @Invoker("onButton")
    void midnightcontrols$onMouseButton(long window, MouseButtonInfo input, int action);
}
