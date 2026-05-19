package com.niconator.createarmedarms;

import com.niconator.createarmedarms.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

final class ArmedArmAnimationStateService {
    private static final int MIN_COOLDOWN = 3;

    private ArmedArmAnimationStateService() {
    }

    static void resetAimAnimation(ArmedArmState state) {
        state.createArmedArms$setAimProgress(0.0f);
    }

    static boolean advanceAimAnimation(ArmedArmState state, float speed) {
        float aimProgress = state.createArmedArms$getAimProgress();
        if (aimProgress >= 1.0f) {
            return false;
        }
        state.createArmedArms$setAimProgress(aimProgress + ArmedArmSpeedScaling.progressStep(speed));
        return state.createArmedArms$getAimProgress() < 1.0f;
    }

    static boolean advanceActionProgress(ArmedArmState state, float speed) {
        float progress = state.createArmedArms$getActionProgress();
        state.createArmedArms$setActionProgress(progress + ArmedArmSpeedScaling.progressStep(speed));
        return state.createArmedArms$getActionProgress() < 1.0f;
    }

    static void beginMeleeSwingAnimation(ArmedArmState state, float speed) {
        int swingTicks = meleeSwingTicksFor(state.createArmedArms$getWeapon(), speed);
        state.createArmedArms$setMeleeSwingTicks(swingTicks);
        state.createArmedArms$setMaxMeleeSwingTicks(swingTicks);
    }

    static void updateMeleeSwingState(ArmBlockEntity arm, ArmedArmState state) {
        int swingTicks = state.createArmedArms$getMeleeSwingTicks();
        if (swingTicks <= 0) {
            return;
        }
        state.createArmedArms$setMeleeSwingTicks(swingTicks - 1);
        if (swingTicks == 1) {
            state.createArmedArms$setMaxMeleeSwingTicks(0);
        }
        arm.setChanged();
        arm.notifyUpdate();
    }

    static void setArmVisualItem(ArmBlockEntity arm, ItemStack stack) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        ItemStack visualStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches(accessor.createArmedArms$getHeldItem(), visualStack)) {
            return;
        }
        accessor.createArmedArms$setHeldItem(visualStack);
        Level level = arm.getLevel();
        if (level != null && !level.isClientSide) {
            arm.notifyUpdate();
        } else {
            arm.setChanged();
        }
    }

    private static int meleeSwingTicksFor(ItemStack weapon, float speed) {
        int baseSwingTicks = Math.max(MIN_COOLDOWN, WeaponStats.meleeSwingBaseTicks(weapon));
        return ArmedArmSpeedScaling.adjustedTickCount(baseSwingTicks, speed, MIN_COOLDOWN);
    }
}
