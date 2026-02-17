package com.infloat.legacymousetweaks.mixin;

import net.minecraft.inventory.slot.Slot;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(net.minecraft.client.gui.screen.ingame.HandledScreen.class)
public interface HandledScreenAccessor {
    @Invoker("getSlotAt")
    Slot lmt_getSlotAt(int mouseX, int mouseY);

    @Invoker("onMouseClick")
    void lmt_onMouseClick(Slot slot, int slotId, int button, int mode);

    @Accessor("screenHandler")
    ScreenHandler lmt_getScreenHandler();
}
