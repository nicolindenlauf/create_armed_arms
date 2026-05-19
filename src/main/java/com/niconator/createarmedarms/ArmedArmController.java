package com.niconator.createarmedarms;

import com.niconator.createarmedarms.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;

final class ArmedArmController {
    private static final double RANGED_MIN_TARGET_RANGE_SQR = 4.0;
    private static final double MIN_SPEED = 1.0;
    private static final int MIN_COOLDOWN = 3;

    private ArmedArmController() {
    }

    static void equipOrSwapWeapon(Level level, ArmBlockEntity arm, Player player, InteractionHand hand,
            ItemStack heldStack) {
        ArmedArmState state = (ArmedArmState) arm;
        ItemStack weapon = heldStack.copyWithCount(1);
        ItemStack previousWeapon = state.createArmedArms$getWeapon().copy();
        ItemStack previousLoadedAmmo = state.createArmedArms$getLoadedAmmo().copy();
        state.createArmedArms$setWeapon(weapon);
        state.createArmedArms$setLoadedAmmo(ItemStack.EMPTY);
        state.createArmedArms$setDrawTicks(0);
        state.createArmedArms$setMaxDrawTicks(0);
        state.createArmedArms$setCooldownTicks(0);
        state.createArmedArms$setMeleeSwingTicks(0);
        state.createArmedArms$setMaxMeleeSwingTicks(0);
        state.createArmedArms$setAimProgress(1.0f);
        state.createArmedArms$setActionProgress(0.0f);
        state.createArmedArms$setPendingInputIndex(-1);
        ArmedArmAnimationStateService.setArmVisualItem(arm, weapon);
        refreshArmStress(arm);
        if (!level.isClientSide) {
            giveOrDrop(level, arm.getBlockPos(), player, previousLoadedAmmo);
            if (!player.getAbilities().instabuild) {
                heldStack.shrink(1);
                if (heldStack.isEmpty() && !previousWeapon.isEmpty()) {
                    player.setItemInHand(hand, previousWeapon);
                } else {
                    player.setItemInHand(hand, heldStack);
                    giveOrDrop(level, arm.getBlockPos(), player, previousWeapon);
                }
            } else if (!previousWeapon.isEmpty()) {
                player.setItemInHand(hand, previousWeapon);
            }
            arm.notifyUpdate();
        }
    }

    static void disarm(Level level, ArmBlockEntity arm, Player player, InteractionHand hand) {
        ArmedArmState state = (ArmedArmState) arm;
        if (!state.createArmedArms$isArmed()) {
            return;
        }
        if (level.isClientSide) {
            state.createArmedArms$setWeapon(ItemStack.EMPTY);
            ArmedArmAnimationStateService.setArmVisualItem(arm, ItemStack.EMPTY);
            return;
        }
        ItemStack weapon = state.createArmedArms$getWeapon().copy();
        player.setItemInHand(hand, weapon);
        returnLoadedAmmo(level, arm.getBlockPos(), player, state.createArmedArms$getLoadedAmmo());
        state.createArmedArms$setWeapon(ItemStack.EMPTY);
        ArmedArmAnimationStateService.setArmVisualItem(arm, ItemStack.EMPTY);
        refreshArmStress(arm);
        arm.notifyUpdate();
        player.displayClientMessage(Component.translatable("message.create_armed_arms.arm_disarmed"), true);
    }

    static void tickArm(ArmBlockEntity arm) {
        ArmedArmState state = (ArmedArmState) arm;
        if (!state.createArmedArms$isArmed()) {
            return;
        }
        ArmedArmAnimationStateService.setArmVisualItem(arm, state.createArmedArms$getWeapon());
    }

    static boolean tickWeaponMovement(ArmBlockEntity arm) {
        boolean keepCreatePaused;
        ArmedArmState state = (ArmedArmState) arm;
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        Level level = arm.getLevel();
        if (level == null || !state.createArmedArms$isArmed()) {
            return false;
        }
        ArmedArmAnimationStateService.setArmVisualItem(arm, state.createArmedArms$getWeapon());
        ItemStack weapon = state.createArmedArms$getWeapon();
        if (WeaponStats.isRangedWeapon(weapon)) {
            keepCreatePaused = updateRangedWeaponMovement(level, arm, state);
        } else {
            updateMeleeWeaponMovement(level, arm, state);
            keepCreatePaused = true;
        }
        if (keepCreatePaused) {
            accessor.createArmedArms$setChasedPointProgress(0.999f);
        }
        return false;
    }

    static boolean shouldOverrideMovement(ArmBlockEntity arm) {
        ArmedArmState state = (ArmedArmState) arm;
        Level level = arm.getLevel();
        if (level == null || !state.createArmedArms$isArmed()) {
            return false;
        }
        ItemStack weapon = state.createArmedArms$getWeapon();
        if (!WeaponStats.isRangedWeapon(weapon)) {
            return true;
        }
        if (needsRangedAmmo(state)) {
            return true;
        }
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), rangedTargetingRange(),
                RANGED_MIN_TARGET_RANGE_SQR);
        if (target != null) {
            return true;
        }
        return !ArmedArmPoseService.isNearRestPose(arm);
    }

    private static boolean updateRangedWeaponMovement(Level level, ArmBlockEntity arm, ArmedArmState state) {
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), rangedTargetingRange(),
                RANGED_MIN_TARGET_RANGE_SQR);
        if (target == null && !needsRangedAmmo(state)) {
            state.createArmedArms$setAimProgress(0.0f);
            if (level.isClientSide) {
                ArmedArmPoseService.chaseArmPose(arm, null);
            }
            return !ArmedArmPoseService.isNearRestPose(arm);
        }
        float speed = Math.abs(arm.getSpeed());
        if (speed < MIN_SPEED) {
            return true;
        }
        if (needsRangedAmmo(state)) {
            updateArrowPickup(level, arm, state, speed);
            return true;
        }
        Vec3 targetPoint = TargetingService.rangedTargetPoint(arm.getBlockPos(), state.createArmedArms$getWeapon(),
                target);
        if (level.isClientSide) {
            ArmedArmPoseService.chaseArmPose(arm, targetPoint);
            return true;
        }
        updateRangedCombat((ServerLevel) level, arm, state, target);
        return true;
    }

    private static void updateArrowPickup(Level level, ArmBlockEntity arm, ArmedArmState state, float speed) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        accessor.createArmedArms$initInteractionPoints();
        List<ArmInteractionPoint> inputs = accessor.createArmedArms$getInputs();
        int inputIndex = state.createArmedArms$getPendingInputIndex();
        if (inputIndex < 0 || inputIndex >= inputs.size() || !inputs.get(inputIndex).isValid()) {
            if (level.isClientSide) {
                ArmedArmPoseService.chaseArmPose(arm, null);
                return;
            }
            inputIndex = findArrowInputIndex(arm);
            if (inputIndex < 0) {
                return;
            }
            state.createArmedArms$setPendingInputIndex(inputIndex);
            state.createArmedArms$setActionProgress(0.0f);
            arm.setChanged();
            arm.notifyUpdate();
            return;
        }
        ArmInteractionPoint input = inputs.get(inputIndex);
        if (level.isClientSide) {
            ArmedArmPoseService.chaseArmPose(arm, input.getPos().getCenter());
        }
        if (ArmedArmAnimationStateService.advanceActionProgress(state, speed)) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        ItemStack ammo = consumeArrowFromInputPoint(arm, input);
        state.createArmedArms$setPendingInputIndex(-1);
        state.createArmedArms$setActionProgress(0.0f);
        if (ammo.isEmpty()) {
            arm.setChanged();
            arm.notifyUpdate();
            return;
        }
        state.createArmedArms$setLoadedAmmo(ammo);
        int drawTicks = ArmedArmCombatService.computeDrawTicks((ServerLevel) level, arm, state.createArmedArms$getWeapon(),
                ammo, speed);
        state.createArmedArms$setDrawTicks(drawTicks);
        state.createArmedArms$setMaxDrawTicks(drawTicks);
        ArmedArmAnimationStateService.resetAimAnimation(state);
        if (WeaponStats.rangedActionModel(
                state.createArmedArms$getWeapon()) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            level.playSound(null, arm.getBlockPos(), (SoundEvent) SoundEvents.CROSSBOW_LOADING_START.value(),
                    SoundSource.BLOCKS, 0.7f, 1.0f);
        }
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static boolean needsRangedAmmo(ArmedArmState state) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        return state.createArmedArms$getLoadedAmmo().isEmpty()
                && (WeaponStats.rangedActionModel(weapon) != WeaponStats.RangedActionModel.CHARGE_THEN_USE
                        || !WeaponStats.isPreCharged(weapon));
    }

    private static void updateMeleeWeaponMovement(Level level, ArmBlockEntity arm, ArmedArmState state) {
        double meleeRange = Math.max(1.5, WeaponStats.getMeleeReach(state.createArmedArms$getWeapon()));
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), meleeRange, 0.0);
        if (level.isClientSide) {
            float speed = Math.abs(arm.getSpeed());
            if (speed < MIN_SPEED) {
                ArmedArmPoseService.chaseArmPose(arm, null);
                return;
            }
            ArmedArmPoseService.chaseArmPose(arm, target == null ? null : TargetingService.meleeTargetPoint(target));
            return;
        }
        updateMeleeCombat((ServerLevel) level, arm, state);
    }

    private static void updateRangedCombat(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        float speed = Math.abs(arm.getSpeed());
        if (speed < MIN_SPEED) {
            return;
        }
        if (target == null) {
            state.createArmedArms$setAimProgress(0.0f);
            return;
        }
        suppressEnemyAggro(target);
        if (WeaponStats.rangedActionModel(
                state.createArmedArms$getWeapon()) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            updateChargeUseRangedCombat(level, arm, state, target);
            return;
        }
        updateReleaseUseRangedCombat(level, arm, state, target);
    }

    private static void updateReleaseUseRangedCombat(ServerLevel level, ArmBlockEntity arm, ArmedArmState state,
            LivingEntity target) {
        float speed = Math.abs(arm.getSpeed());
        if (ArmedArmAnimationStateService.advanceAimAnimation(state, speed)) {
            arm.setChanged();
            return;
        }
        int drawTicks = state.createArmedArms$getDrawTicks();
        if (drawTicks > 0) {
            state.createArmedArms$setDrawTicks(drawTicks - 1);
            arm.setChanged();
            arm.notifyUpdate();
            if (drawTicks > 1) {
                return;
            }
        }
        ArmedArmCombatService.performRangedWeaponActionLikePlayer(level, arm, state, target, Math.abs(arm.getSpeed()),
                ArmedArmCombatService.RangedAction.FIRE);
        state.createArmedArms$setDrawTicks(0);
        state.createArmedArms$setMaxDrawTicks(0);
        state.createArmedArms$setAimProgress(0.0f);
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static void updateChargeUseRangedCombat(ServerLevel level, ArmBlockEntity arm, ArmedArmState state,
            LivingEntity target) {
        int drawTicks = state.createArmedArms$getDrawTicks();
        if (drawTicks > 0) {
            playChargeUseLoadingSound(level, arm.getBlockPos(), state.createArmedArms$getWeapon(), drawTicks,
                    Math.abs(arm.getSpeed()));
            state.createArmedArms$setDrawTicks(drawTicks - 1);
            arm.setChanged();
            arm.notifyUpdate();
            if (drawTicks == 1) {
                ArmedArmCombatService.performRangedWeaponActionLikePlayer(level, arm, state, target,
                        Math.abs(arm.getSpeed()), ArmedArmCombatService.RangedAction.CHARGE);
                state.createArmedArms$setMaxDrawTicks(0);
                state.createArmedArms$setCooldownTicks(WeaponStats.readyDelayTicks(state.createArmedArms$getWeapon()));
                level.playSound(null, arm.getBlockPos(), (SoundEvent) SoundEvents.CROSSBOW_LOADING_END.value(),
                        SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return;
        }
        int cooldownTicks = state.createArmedArms$getCooldownTicks();
        if (cooldownTicks > 0) {
            state.createArmedArms$setCooldownTicks(cooldownTicks - 1);
            return;
        }
        ArmedArmCombatService.performRangedWeaponActionLikePlayer(level, arm, state, target, Math.abs(arm.getSpeed()),
                ArmedArmCombatService.RangedAction.FIRE);
        state.createArmedArms$setAimProgress(0.0f);
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static void updateMeleeCombat(ServerLevel level, ArmBlockEntity arm, ArmedArmState state) {
        float speed = Math.abs(arm.getSpeed());
        if (speed < MIN_SPEED) {
            return;
        }
        ArmedArmAnimationStateService.updateMeleeSwingState(arm, state);
        int cooldownTicks = state.createArmedArms$getCooldownTicks();
        if (cooldownTicks > 0) {
            state.createArmedArms$setCooldownTicks(cooldownTicks - 1);
            return;
        }
        double meleeRange = Math.max(1.5, WeaponStats.getMeleeReach(state.createArmedArms$getWeapon()));
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), meleeRange, 0.0);
        if (target == null) {
            return;
        }
        ArmedArmCombatService.attackMelee(level, arm, state, target);
        state.createArmedArms$setCooldownTicks(cooldownFor(state.createArmedArms$getWeapon(), speed));
        ArmedArmAnimationStateService.beginMeleeSwingAnimation(state, speed);
        ArmedArmAnimationStateService.setArmVisualItem(arm, state.createArmedArms$getWeapon());
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static int findArrowInputIndex(ArmBlockEntity arm) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        accessor.createArmedArms$initInteractionPoints();
        List<ArmInteractionPoint> inputs = accessor.createArmedArms$getInputs();
        if (inputs.isEmpty()) {
            return -1;
        }
        ArmBlockEntity.SelectionMode selectionMode = (ArmBlockEntity.SelectionMode) accessor
                .createArmedArms$getSelectionMode().get();
        int lastInputIndex = accessor.createArmedArms$getLastInputIndex();
        int startIndex = selectionMode == ArmBlockEntity.SelectionMode.PREFER_FIRST ? 0 : lastInputIndex + 1;
        int scanRange = selectionMode == ArmBlockEntity.SelectionMode.FORCED_ROUND_ROBIN ? lastInputIndex + 2
                : inputs.size();
        if (scanRange > inputs.size()) {
            scanRange = inputs.size();
        }
        for (int inputIndex = Math.max(0, startIndex); inputIndex < scanRange; ++inputIndex) {
            ArmInteractionPoint input = inputs.get(inputIndex);
            if (!input.isValid() || !inputHasArrow(arm, input)) {
                continue;
            }
            accessor.createArmedArms$setLastInputIndex(inputIndex == inputs.size() - 1 ? -1 : inputIndex);
            return inputIndex;
        }
        if (selectionMode == ArmBlockEntity.SelectionMode.ROUND_ROBIN || lastInputIndex == inputs.size() - 1) {
            accessor.createArmedArms$setLastInputIndex(-1);
        }
        return -1;
    }

    private static boolean inputHasArrow(ArmBlockEntity arm, ArmInteractionPoint input) {
        int slotCount = input.getSlotCount(arm);
        for (int slot = 0; slot < slotCount; ++slot) {
            if (WeaponStats.isArrow(input.extract(arm, slot, 1, true))) {
                return true;
            }
        }
        return false;
    }

    private static ItemStack consumeArrowFromInputPoint(ArmBlockEntity arm, ArmInteractionPoint input) {
        int slotCount = input.getSlotCount(arm);
        for (int slot = 0; slot < slotCount; ++slot) {
            ItemStack stack = input.extract(arm, slot, 1, true);
            if (!WeaponStats.isArrow(stack)) {
                continue;
            }
            return input.extract(arm, slot, 1, false);
        }
        return ItemStack.EMPTY;
    }

    private static int cooldownFor(ItemStack weapon, float speed) {
        int baseCooldown = meleeBaseCooldown(weapon);
        return ArmedArmSpeedScaling.adjustedTickCount(baseCooldown, speed, MIN_COOLDOWN);
    }

    private static int meleeBaseCooldown(ItemStack weapon) {
        double attackSpeed = WeaponStats.getAttackSpeed(weapon);
        if (attackSpeed <= 0.0) {
            return Math.max(MIN_COOLDOWN, WeaponStats.defaultUseTicks(weapon));
        }
        return Math.max(MIN_COOLDOWN, (int) Math.ceil(20.0 / attackSpeed));
    }

    private static double rangedTargetingRange() {
        return CreateArmedArmsConfig.RANGED_TARGETING_RANGE.get();
    }

    private static void returnLoadedAmmo(Level level, BlockPos pos, Player player, ItemStack loadedAmmo) {
        if (loadedAmmo.isEmpty()) {
            return;
        }
        giveOrDrop(level, pos, player, loadedAmmo);
    }

    private static void giveOrDrop(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.addItem(stack.copy())) {
            Block.popResource(level, pos, stack.copy());
        }
    }

    private static void playChargeUseLoadingSound(ServerLevel level, BlockPos pos, ItemStack weapon, int drawTicks,
            float speed) {
        int totalDrawTicks = ArmedArmSpeedScaling.adjustedTickCount(
                WeaponStats.chargeDuration(weapon, FakePlayerCombatService.getSharedFakePlayer(level)),
                speed, MIN_COOLDOWN);
        if (drawTicks == totalDrawTicks / 2) {
            level.playSound(null, pos, (SoundEvent) SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS,
                    0.7f, 1.0f);
        }
    }

    static void suppressEnemyAggro(LivingEntity entity) {
        Mob mob;
        if (!(entity instanceof Mob) || !((mob = (Mob) entity) instanceof Enemy)) {
            return;
        }
        mob.setTarget(null);
    }

    static void refreshArmStress(ArmBlockEntity arm) {
        Level level = arm.getLevel();
        if (level == null || level.isClientSide || !arm.hasNetwork()) {
            return;
        }
        arm.getOrCreateNetwork().updateStressFor(arm, arm.calculateStressApplied());
    }

}
