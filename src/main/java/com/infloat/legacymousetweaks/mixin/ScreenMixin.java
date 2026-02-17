package com.infloat.legacymousetweaks.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.slot.CraftingResultSlot;
import net.minecraft.inventory.slot.Slot;
import net.minecraft.item.ItemStack;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(Screen.class)
public abstract class ScreenMixin {
    @Unique
    private static final boolean WHEEL_TWEAK = true;

    @Unique
    private static final boolean WHEEL_SEARCH_ORDER_LAST_TO_FIRST = true;

    @Unique
    private static final int WHEEL_SCROLL_DIRECTION = 0;

    @Shadow
    protected MinecraftClient client;

    @Shadow
    public int width;

    @Shadow
    public int height;

    @Inject(method = "handleMouse()V", at = @At("TAIL"))
    private void lmt_onHandleMouse(CallbackInfo ci) {
        try {
            if (!WHEEL_TWEAK) {
                return;
            }

            if (!((Object) this instanceof HandledScreen) || !((Object) this instanceof HandledScreenAccessor)) {
                return;
            }
            if (this.client == null || this.client.player == null || this.client.width <= 0 || this.client.height <= 0) {
                return;
            }

            int wheel = Mouse.getEventDWheel();
            if (wheel == 0) {
                return;
            }

            int mouseX = Mouse.getEventX() * this.width / this.client.width;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.client.height - 1;

            HandledScreenAccessor accessor = (HandledScreenAccessor) (Object) this;
            if (accessor.lmt_getScreenHandler() == null || accessor.lmt_getScreenHandler().slots == null) {
                return;
            }

            Slot selectedSlot = accessor.lmt_getSlotAt(mouseX, mouseY);
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

            List<Slot> slots = accessor.lmt_getScreenHandler().slots;
            int numItemsToMove = Math.abs(delta);
            boolean pushItems = delta < 0;

            if (WHEEL_SCROLL_DIRECTION == 2 && lmt_otherInventoryIsAbove(selectedSlot, slots)) {
                pushItems = !pushItems;
            }
            if (WHEEL_SCROLL_DIRECTION == 1) {
                pushItems = !pushItems;
            }

            if (pushItems) {
                lmt_wheelPush(accessor, selectedSlot, slots, numItemsToMove);
            } else {
                lmt_wheelPull(accessor, selectedSlot, slots, numItemsToMove);
            }
        } catch (Throwable t) {
            System.err.println("[LegacyMouseTweaks] Wheel tweak error: " + t);
        }
    }

    @Unique
    private void lmt_wheelPush(HandledScreenAccessor accessor, Slot selectedSlot, List<Slot> slots, int numItemsToMove) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        if (lmt_isStackEmpty(selectedSlotStack)) {
            return;
        }

        ItemStack stackOnMouse = this.client.player.inventory.getCursorStack();
        if (!lmt_isStackEmpty(stackOnMouse) && !selectedSlot.canInsert(stackOnMouse)) {
            return;
        }

        numItemsToMove = Math.min(numItemsToMove, selectedSlotStack.count);
        List<Slot> targetSlots = lmt_findPushSlots(selectedSlot, slots, numItemsToMove, false);
        if (targetSlots.isEmpty()) {
            return;
        }

        accessor.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);

        for (Slot slot : targetSlots) {
            ItemStack slotStack = slot.getStack();
            int current = lmt_isStackEmpty(slotStack) ? 0 : slotStack.count;
            int max = lmt_isStackEmpty(slotStack) ? slot.getMaxStackAmount() : slot.getMaxStackAmount(slotStack);
            int clickTimes = Math.min(numItemsToMove, Math.max(0, max - current));
            numItemsToMove -= clickTimes;

            while (clickTimes-- > 0) {
                accessor.lmt_onMouseClick(slot, slot.id, 1, 0);
            }

            if (numItemsToMove <= 0) {
                break;
            }
        }

        accessor.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
    }

    @Unique
    private void lmt_wheelPull(HandledScreenAccessor accessor, Slot selectedSlot, List<Slot> slots, int numItemsToMove) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        if (lmt_isStackEmpty(selectedSlotStack)) {
            return;
        }

        int maxItemsToMove = selectedSlot.getMaxStackAmount(selectedSlotStack) - selectedSlotStack.count;
        numItemsToMove = Math.min(numItemsToMove, maxItemsToMove);

        while (numItemsToMove > 0) {
            Slot targetSlot = lmt_findPullSlot(selectedSlot, slots);
            if (targetSlot == null) {
                return;
            }

            ItemStack targetStack = targetSlot.getStack();
            if (lmt_isStackEmpty(targetStack)) {
                return;
            }

            ItemStack stackOnMouse = this.client.player.inventory.getCursorStack();
            if (!lmt_isStackEmpty(stackOnMouse) && !targetSlot.canInsert(stackOnMouse)) {
                return;
            }

            int numItemsInTargetSlot = targetStack.count;
            int numItemsToMoveFromTargetSlot = Math.min(numItemsToMove, numItemsInTargetSlot);
            numItemsToMove -= numItemsToMoveFromTargetSlot;

            accessor.lmt_onMouseClick(targetSlot, targetSlot.id, 0, 0);

            if (numItemsToMoveFromTargetSlot == numItemsInTargetSlot) {
                accessor.lmt_onMouseClick(selectedSlot, selectedSlot.id, 0, 0);
            } else {
                for (int i = 0; i < numItemsToMoveFromTargetSlot; i++) {
                    accessor.lmt_onMouseClick(selectedSlot, selectedSlot.id, 1, 0);
                }
            }

            accessor.lmt_onMouseClick(targetSlot, targetSlot.id, 0, 0);
        }
    }

    @Unique
    private Slot lmt_findPullSlot(Slot selectedSlot, List<Slot> slots) {
        ItemStack selectedSlotStack = selectedSlot.getStack();
        PlayerInventory playerInventory = this.client.player.inventory;
        boolean findInPlayerInventory = selectedSlot.inventory != playerInventory;

        int startIndex = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? slots.size() - 1 : 0;
        int endIndex = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? -1 : slots.size();
        int direction = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? -1 : 1;

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
        PlayerInventory playerInventory = this.client.player.inventory;
        boolean findInPlayerInventory = selectedSlot.inventory != playerInventory;

        List<Slot> result = new ArrayList<Slot>();
        List<Slot> emptySlots = new ArrayList<Slot>();

        int startIndex = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? slots.size() - 1 : 0;
        int endIndex = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? -1 : slots.size();
        int direction = WHEEL_SEARCH_ORDER_LAST_TO_FIRST ? -1 : 1;

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
        PlayerInventory playerInventory = this.client.player.inventory;
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

    @Unique
    private static boolean lmt_isStackEmpty(ItemStack stack) {
        return stack == null || stack.getItem() == null || stack.count <= 0;
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
}
