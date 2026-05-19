package com.niconator.createarmedarms;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import javax.annotation.Nonnull;

public final class ArmedArmItemHandlerView implements IItemHandler {
    private final ArmedArmState state;

    public ArmedArmItemHandlerView(ArmedArmState state) {
        this.state = state;
    }

    @Override
    public int getSlots() {
        if (!state.createArmedArms$isArmed()) {
            return 0;
        }
        return WeaponStats.isRangedWeapon(state.createArmedArms$getWeapon()) ? 2 : 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        if (!state.createArmedArms$isArmed()) {
            return ItemStack.EMPTY;
        }

        if (slot == 0) {
            return state.createArmedArms$getWeapon().copyWithCount(1);
        }

        if (slot == 1 && WeaponStats.isRangedWeapon(state.createArmedArms$getWeapon())) {
            return state.createArmedArms$getLoadedAmmo().isEmpty()
                    ? ItemStack.EMPTY
                    : state.createArmedArms$getLoadedAmmo().copyWithCount(1);
        }

        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return ItemStack.EMPTY;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
        return false;
    }
}
