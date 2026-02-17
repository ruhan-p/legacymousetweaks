package com.infloat.legacymousetweaks.mixin;

import com.infloat.legacymousetweaks.config.ConfigManager;
import com.infloat.legacymousetweaks.config.MouseTweaksConfig;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(HandledScreen.class)
public abstract class GuiContainerMixin {
    @Unique
    private Slot lmt_oldSelectedSlot;

    @Unique
    private boolean lmt_canDoLMBDrag;

    @Unique
    private boolean lmt_canDoRMBDrag;

    @Shadow
    protected boolean isCursorDragging;

    @Shadow
    protected Set<Slot> cursorDragSlots;

    @Invoker("getSlotAt")
    protected abstract Slot lmt_getSlotAt(int mouseX, int mouseY);

    @Invoker("onMouseClick")
    protected abstract void lmt_onMouseClick(Slot slot, int slotId, int button, int mode);

    @Unique
    private static boolean lmt_isStackEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.count <= 0;
    }

    @Unique
    private static MouseTweaksConfig lmt_config() {
        return ConfigManager.get();
    }

    @Unique
    private static boolean lmt_areStacksCompatible(ItemStack a, ItemStack b) {
        if (lmt_isStackEmpty(a) || lmt_isStackEmpty(b)) {
            return true;
        }
        if (!ItemStack.equalsIgnoreNbt(a, b)) {
            return false;
        }
        return !a.hasSubTypes() || a.getData() == b.getData();
    }

    @Unique
    private static boolean lmt_isCraftingOutput(Slot slot) {
        return slot instanceof CraftingResultSlot;
    }

    @Unique
    private void lmt_rmbTweakMaybeClickSlot(Slot slot, ItemStack stackOnMouse) {
        if (slot == null) {
            return;
        }
        if (lmt_isStackEmpty(stackOnMouse)) {
            return;
        }
        if (lmt_isCraftingOutput(slot)) {
            return;
        }

        ItemStack selectedSlotStack = slot.getStack();
        if (!lmt_areStacksCompatible(selectedSlotStack, stackOnMouse)) {
            return;
        }

        if (!lmt_isStackEmpty(selectedSlotStack)
                && selectedSlotStack.count >= slot.getMaxStackAmount(selectedSlotStack)) {
            return;
        }

        this.lmt_onMouseClick(slot, slot.id, 1, 0);
    }

    @Inject(method = "mouseClicked(III)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseClicked(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        Slot selectedSlot = this.lmt_getSlotAt(mouseX, mouseY);
        this.lmt_oldSelectedSlot = selectedSlot;

        ItemStack stackOnMouse = MinecraftClient.getInstance().player.inventory.getCursorStack();
        if (mouseButton == 0) {
            this.lmt_canDoLMBDrag = lmt_isStackEmpty(stackOnMouse);
        } else if (mouseButton == 1) {
            this.lmt_canDoRMBDrag = !lmt_isStackEmpty(stackOnMouse) && lmt_config().rmbTweak;
            if (this.lmt_canDoRMBDrag) {
                // Replace vanilla RMB click/drag behavior with MouseTweaks-style behavior.
                this.isCursorDragging = false;
                this.cursorDragSlots.clear();
                this.lmt_rmbTweakMaybeClickSlot(selectedSlot, stackOnMouse);
                ci.cancel();
            }
        }
    }

    @Inject(method = "mouseDragged(IIIJ)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseDragged(int mouseX, int mouseY, int mouseButton, long timeSinceClick, CallbackInfo ci) {
        Slot selectedSlot = this.lmt_getSlotAt(mouseX, mouseY);

        if (mouseButton == 1 && this.lmt_canDoRMBDrag) {
            if (selectedSlot != this.lmt_oldSelectedSlot) {
                this.lmt_rmbTweakMaybeClickSlot(selectedSlot, MinecraftClient.getInstance().player.inventory.getCursorStack());
                this.lmt_oldSelectedSlot = selectedSlot;
            }

            ci.cancel();
            return;
        }

        if (selectedSlot == this.lmt_oldSelectedSlot) {
            return;
        }

        ItemStack stackOnMouse = MinecraftClient.getInstance().player.inventory.getCursorStack();

        this.lmt_oldSelectedSlot = selectedSlot;

        if (selectedSlot == null) {
            return;
        }

        if (mouseButton == 0) {
            if (!this.lmt_canDoLMBDrag) {
                return;
            }

            ItemStack selectedSlotStack = selectedSlot.getStack();
                if (lmt_isStackEmpty(selectedSlotStack)) {
                    return;
                }

                boolean shiftIsDown = Screen.hasShiftDown();
                if (lmt_isStackEmpty(stackOnMouse)) {
                    if (!lmt_config().lmbTweakWithoutItem || !shiftIsDown) {
                        return;
                    }
                    this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 1);
                } else {
                    if (!lmt_config().lmbTweakWithItem) {
                        return;
                    }
                if (!lmt_areStacksCompatible(selectedSlotStack, stackOnMouse)) {
                    return;
                }

                if (shiftIsDown) {
                    this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 1);
                } else {
                    if (stackOnMouse.count + selectedSlotStack.count > stackOnMouse.getMaxCount()) {
                        return;
                    }
                    this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
                    if (!lmt_isCraftingOutput(selectedSlot)) {
                        this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
                    }
                }
            }
        }
    }

    @Inject(method = "mouseReleased(III)V", at = @At("HEAD"), cancellable = true)
    private void lmt_onMouseReleased(int mouseX, int mouseY, int state, CallbackInfo ci) {
        if (state == 0) {
            this.lmt_canDoLMBDrag = false;
        } else if (state == 1) {
            if (this.lmt_canDoRMBDrag) {
                // Prevent vanilla from adding an extra RMB action on release.
                this.isCursorDragging = false;
                this.cursorDragSlots.clear();
                ci.cancel();
            }
            this.lmt_canDoRMBDrag = false;
        }
    }

    @Inject(method = "render(IIF)V", at = @At("HEAD"))
    private void lmt_onRenderWheel(int mouseXParam, int mouseYParam, float deltaTicks, CallbackInfo ci) {
        try {
            if (!lmt_config().wheelTweak) {
                return;
            }
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc == null || mc.player == null || mc.width <= 0 || mc.height <= 0) {
                return;
            }

            int wheel = Mouse.getDWheel();
            if (wheel == 0) {
                return;
            }
            ScreenHandler screenHandler = ((HandledScreen) (Object) this).screenHandler;
            if (screenHandler == null || screenHandler.slots == null) {
                return;
            }

            int screenWidth = ((Screen) (Object) this).width;
            int screenHeight = ((Screen) (Object) this).height;
            int mouseX = Mouse.getX() * screenWidth / mc.width;
            int mouseY = screenHeight - Mouse.getY() * screenHeight / mc.height - 1;

            Slot selectedSlot = this.lmt_getSlotAt(mouseX, mouseY);
            if (selectedSlot == null) {
                return;
            }

            ItemStack selectedSlotStack = selectedSlot.getStack();
            if (lmt_isStackEmpty(selectedSlotStack)) {
                return;
            }

            int delta = wheel / 120;
            if (delta == 0) {
                delta = wheel > 0 ? 1 : -1;
            }

            List<Slot> slots = screenHandler.slots;
            int numItemsToMove = Math.abs(delta);
            boolean pushItems = delta < 0;

            if (lmt_config().wheelScrollDirection == 2 && this.lmt_otherInventoryIsAbove(selectedSlot, slots)) {
                pushItems = !pushItems;
            }
            if (lmt_config().wheelScrollDirection == 1) {
                pushItems = !pushItems;
            }

            if (pushItems) {
                this.lmt_wheelPush(selectedSlot, slots, numItemsToMove);
            } else {
                this.lmt_wheelPull(selectedSlot, slots, numItemsToMove);
            }
        } catch (Throwable t) {
            System.err.println("[LegacyMouseTweaks] Wheel tweak error: " + t);
        }
    }

    @Unique
    private void lmt_wheelPush(Slot selectedSlot, List<Slot> slots, int numItemsToMove) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        if (lmt_isStackEmpty(selectedSlotStack)) {
            return;
        }

        ItemStack stackOnMouse = MinecraftClient.getInstance().player.inventory.getCursorStack();
        if (!lmt_isStackEmpty(stackOnMouse) && !selectedSlot.canInsert(stackOnMouse)) {
            return;
        }

        numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.count);
        List<Slot> targetSlots = this.lmt_findPushSlots(selectedSlot, slots, numItemsToMove, false);
        if (targetSlots.isEmpty()) {
            return;
        }

        this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);

        for (Slot slot : targetSlots) {
            ItemStack slotStack = slot.getStack();
            int current = lmt_isStackEmpty(slotStack) ? 0 : slotStack.count;
            int max = lmt_isStackEmpty(slotStack) ? slot.getMaxStackAmount() : slot.getMaxStackAmount(slotStack);
            int clickTimes = Math.min(numItemsToMove, Math.max(0, max - current));
            numItemsToMove -= clickTimes;

            while (clickTimes-- > 0) {
                this.lmt_onMouseClick(slot, slot.id, 1, 0);
            }

            if (numItemsToMove <= 0) {
                break;
            }
        }

        this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
    }

    @Unique
    private void lmt_wheelPull(Slot selectedSlot, List<Slot> slots, int numItemsToMove) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        if (lmt_isStackEmpty(selectedSlotStack)) {
            return;
        }

        int maxItemsToMove = selectedSlot.getMaxStackAmount(selectedSlotStack) - selectedSlotStack.count;
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            Slot targetSlot = this.lmt_findPullSlot(selectedSlot, slots);
            if (targetSlot == null) {
                return;
            }

            ItemStack targetStack = targetSlot.getStack();
            if (lmt_isStackEmpty(targetStack)) {
                return;
            }

            ItemStack stackOnMouse = MinecraftClient.getInstance().player.inventory.getCursorStack();
            if (!lmt_isStackEmpty(stackOnMouse) && !targetSlot.canInsert(stackOnMouse)) {
                return;
            }

            int numItemsInTargetSlot = targetStack.count;
            int numItemsToMoveFromTargetSlot = Math.min(numItemsToMove, numItemsInTargetSlot);
            numItemsToMove -= numItemsToMoveFromTargetSlot;

            this.lmt_onMouseClick(targetSlot, targetSlot.id, 0, 0);

            if (numItemsToMoveFromTargetSlot == numItemsInTargetSlot) {
                this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetSlot; i++) {
                    this.lmt_onMouseClick(selectedSlot, selectedSlot.id, 1, 0);
                }
            }

            this.lmt_onMouseClick(targetSlot, targetSlot.id, 0, 0);
        }
    }

    @Unique
    private Slot lmt_findPullSlot(Slot selectedSlot, List<Slot> slots) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        PlayerInventory playerInventory = MinecraftClient.getInstance().player.inventory;
        boolean findInPlayerInventory = selectedSlot.inventory != playerInventory;

        boolean reverse = lmt_config().wheelSearchOrder == 1;
        int startIndex = reverse ? slots.size() - 1 : 0;
        int endIndex = reverse ? -1 : slots.size();
        int direction = reverse ? -1 : 1;

        for (int i = startIndex; i != endIndex; i += direction) {
            Slot slot = slots.get(i);
            if (slot == selectedSlot) {
                continue;
            }

            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (lmt_isStackEmpty(stack)) {
                continue;
            }
            if (!lmt_areStacksCompatible(selectedSlotStack, stack)) {
                continue;
            }

            return slot;
        }

        return null;
    }

    @Unique
    private List<Slot> lmt_findPushSlots(Slot selectedSlot, List<Slot> slots, int itemCount, boolean mustDistributeAll) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        PlayerInventory playerInventory = MinecraftClient.getInstance().player.inventory;
        boolean findInPlayerInventory = selectedSlot.inventory != playerInventory;

        List<Slot> result = new ArrayList<Slot>();
        List<Slot> emptySlots = new ArrayList<Slot>();

        boolean reverse = lmt_config().wheelSearchOrder == 1;
        int startIndex = reverse ? slots.size() - 1 : 0;
        int endIndex = reverse ? -1 : slots.size();
        int direction = reverse ? -1 : 1;

        for (int i = startIndex; i != endIndex && itemCount > 0; i += direction) {
            Slot slot = slots.get(i);
            if (slot == selectedSlot) {
                continue;
            }

            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (findInPlayerInventory != slotInPlayerInventory) {
                continue;
            }
            if (slot instanceof CraftingResultSlot) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (lmt_isStackEmpty(stack)) {
                if (slot.canInsert(selectedSlotStack)) {
                    emptySlots.add(slot);
                }
            } else if (lmt_areStacksCompatible(selectedSlotStack, stack) && stack.count < slot.getMaxStackAmount(stack)) {
                result.add(slot);
                itemCount -= Math.min(itemCount, slot.getMaxStackAmount(stack) - stack.count);
            }
        }

        for (int i = 0; i < emptySlots.size() && itemCount > 0; i++) {
            Slot slot = emptySlots.get(i);
            result.add(slot);
            itemCount -= Math.min(itemCount, slot.getMaxStackAmount());
        }

        if (mustDistributeAll && itemCount > 0) {
            return new ArrayList<Slot>();
        }

        return result;
    }

    @Unique
    private boolean lmt_otherInventoryIsAbove(Slot selectedSlot, List<Slot> slots) {
        PlayerInventory playerInventory = MinecraftClient.getInstance().player.inventory;
        boolean selectedInPlayerInventory = selectedSlot.inventory == playerInventory;

        int otherAbove = 0;
        int otherBelow = 0;

        for (Slot slot : slots) {
            boolean slotInPlayerInventory = slot.inventory == playerInventory;
            if (slotInPlayerInventory == selectedInPlayerInventory) {
                continue;
            }

            if (slot.y < selectedSlot.y) {
                otherAbove++;
            } else {
                otherBelow++;
            }
        }

        return otherAbove > otherBelow;
    }
}
