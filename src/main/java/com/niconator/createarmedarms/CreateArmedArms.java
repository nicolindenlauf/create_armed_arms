/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.logging.LogUtils
 *  com.simibubi.create.AllBlockEntityTypes
 *  com.simibubi.create.content.kinetics.base.KineticBlockEntity
 *  com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget
 *  com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity
 *  com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity$SelectionMode
 *  com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint
 *  net.createmod.catnip.animation.LerpedFloat
 *  net.createmod.catnip.animation.LerpedFloat$Chaser
 *  net.minecraft.core.BlockPos
 *  net.minecraft.core.Direction
 *  net.minecraft.network.chat.Component
 *  net.minecraft.server.level.ServerLevel
 *  net.minecraft.sounds.SoundEvent
 *  net.minecraft.sounds.SoundEvents
 *  net.minecraft.sounds.SoundSource
 *  net.minecraft.util.Mth
 *  net.minecraft.world.InteractionHand
 *  net.minecraft.world.InteractionResult
 *  net.minecraft.world.entity.Entity
 *  net.minecraft.world.entity.LivingEntity
 *  net.minecraft.world.entity.Mob
 *  net.minecraft.world.entity.monster.Enemy
 *  net.minecraft.world.entity.player.Player
 *  net.minecraft.world.item.ItemStack
 *  net.minecraft.world.level.Level
 *  net.minecraft.world.level.block.Block
 *  net.minecraft.world.level.block.entity.BlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraft.world.phys.Vec3
 *  net.neoforged.bus.api.SubscribeEvent
 *  net.neoforged.fml.ModContainer
 *  net.neoforged.fml.common.Mod
 *  net.neoforged.fml.config.IConfigSpec
 *  net.neoforged.fml.config.ModConfig$Type
 *  net.neoforged.neoforge.capabilities.Capabilities$ItemHandler
 *  net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent
 *  net.neoforged.neoforge.common.NeoForge
 *  net.neoforged.neoforge.common.util.FakePlayer
 *  net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent
 *  net.neoforged.neoforge.event.entity.player.PlayerInteractEvent$RightClickBlock
 *  org.slf4j.Logger
 */
package com.niconator.createarmedarms;

import com.mojang.logging.LogUtils;
import com.niconator.createarmedarms.ArmedArmItemHandlerView;
import com.niconator.createarmedarms.ArmedArmState;
import com.niconator.createarmedarms.CreateArmedArmsConfig;
import com.niconator.createarmedarms.FakePlayerCombatService;
import com.niconator.createarmedarms.TargetingService;
import com.niconator.createarmedarms.WeaponStats;
import com.niconator.createarmedarms.mixin.ArmAngleTargetAccessor;
import com.niconator.createarmedarms.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import java.util.List;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.IConfigSpec;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.slf4j.Logger;

@Mod(value="create_armed_arms")
public class CreateArmedArms {
    public static final String MODID = "create_armed_arms";
    public static final Logger LOGGER = LogUtils.getLogger();
    private static final double RANGED_MIN_TARGET_RANGE_SQR = 4.0;
    private static final double MIN_SPEED = 1.0;
    private static final float DEFAULT_RPM = 32.0f;
    private static final int MIN_COOLDOWN = 3;
    private static final float REST_ANGLE_EPSILON = 0.75f;
    private static final double REST_RETURN_SPEED_FACTOR = 0.55;
    private static final float REST_BASE_ANGLE = 0.0f;

    public CreateArmedArms(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, (IConfigSpec)CreateArmedArmsConfig.SPEC);
        modContainer.getEventBus().addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register((Object)this);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, (BlockEntityType)AllBlockEntityTypes.MECHANICAL_ARM.get(), (arm, context) -> {
            ArmedArmState state = (ArmedArmState)arm;
            return state.createArmedArms$isArmed() ? new ArmedArmItemHandlerView(state) : null;
        });
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        BlockPos pos;
        Player player = event.getEntity();
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(pos = event.getPos());
        if (!(blockEntity instanceof ArmBlockEntity)) {
            return;
        }
        ArmBlockEntity arm = (ArmBlockEntity)blockEntity;
        ItemStack heldStack = player.getItemInHand(event.getHand());
        if (WeaponStats.isArmWeapon(heldStack)) {
            CreateArmedArms.equipOrSwapWeapon(level, arm, player, event.getHand(), heldStack);
            event.setCancellationResult(InteractionResult.sidedSuccess((boolean)level.isClientSide));
            event.setCanceled(true);
            return;
        }
        if (heldStack.isEmpty()) {
            CreateArmedArms.disarm(level, arm, player, event.getHand());
            event.setCancellationResult(InteractionResult.sidedSuccess((boolean)level.isClientSide));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        Mob mob;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Mob) || !((mob = (Mob)livingEntity) instanceof Enemy)) {
            return;
        }
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (!(newTarget instanceof FakePlayer)) {
            return;
        }
        FakePlayer fakePlayer = (FakePlayer)newTarget;
        if ("[CreateArmedArms]".equals(fakePlayer.getGameProfile().getName())) {
            mob.setTarget(null);
            event.setCanceled(true);
        }
    }

    private static void equipOrSwapWeapon(Level level, ArmBlockEntity arm, Player player, InteractionHand hand, ItemStack heldStack) {
        ArmedArmState state = (ArmedArmState)arm;
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
        CreateArmedArms.setArmVisualItem(arm, weapon);
        CreateArmedArms.refreshArmStress(arm);
        if (!level.isClientSide) {
            CreateArmedArms.giveOrDrop(level, arm.getBlockPos(), player, previousLoadedAmmo);
            if (!player.getAbilities().instabuild) {
                heldStack.shrink(1);
                if (heldStack.isEmpty() && !previousWeapon.isEmpty()) {
                    player.setItemInHand(hand, previousWeapon);
                } else {
                    player.setItemInHand(hand, heldStack);
                    CreateArmedArms.giveOrDrop(level, arm.getBlockPos(), player, previousWeapon);
                }
            } else {
                CreateArmedArms.giveOrDrop(level, arm.getBlockPos(), player, previousWeapon);
            }
            arm.notifyUpdate();
        }
    }

    private static void disarm(Level level, ArmBlockEntity arm, Player player, InteractionHand hand) {
        ArmedArmState state = (ArmedArmState)arm;
        if (!state.createArmedArms$isArmed()) {
            return;
        }
        if (level.isClientSide) {
            state.createArmedArms$setWeapon(ItemStack.EMPTY);
            CreateArmedArms.setArmVisualItem(arm, ItemStack.EMPTY);
            return;
        }
        ItemStack weapon = state.createArmedArms$getWeapon().copy();
        player.setItemInHand(hand, weapon);
        CreateArmedArms.returnLoadedAmmo(level, arm.getBlockPos(), player, state.createArmedArms$getLoadedAmmo());
        state.createArmedArms$setWeapon(ItemStack.EMPTY);
        CreateArmedArms.setArmVisualItem(arm, ItemStack.EMPTY);
        CreateArmedArms.refreshArmStress(arm);
        arm.notifyUpdate();
        player.displayClientMessage((Component)Component.translatable((String)"message.create_armed_arms.arm_disarmed"), true);
    }

    public static void tickArm(ArmBlockEntity arm) {
        ArmedArmState state = (ArmedArmState)arm;
        if (!state.createArmedArms$isArmed()) {
            return;
        }
        CreateArmedArms.setArmVisualItem(arm, state.createArmedArms$getWeapon());
        Level level = arm.getLevel();
        if (level == null) {
            return;
        }
        ItemStack weapon = state.createArmedArms$getWeapon();
        LOGGER.debug("Unexpected armed mechanical arm tick outside movement hook: {}", (Object)weapon);
    }

    public static boolean tickWeaponMovement(ArmBlockEntity arm) {
        boolean keepCreatePaused;
        ArmedArmState state = (ArmedArmState)arm;
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        Level level = arm.getLevel();
        if (level == null || !state.createArmedArms$isArmed()) {
            return false;
        }
        CreateArmedArms.setArmVisualItem(arm, state.createArmedArms$getWeapon());
        ItemStack weapon = state.createArmedArms$getWeapon();
        if (WeaponStats.isRangedWeapon(weapon)) {
            keepCreatePaused = CreateArmedArms.tickRangedWeaponMovement(level, arm, state);
        } else {
            CreateArmedArms.tickMeleeWeaponMovement(level, arm, state);
            keepCreatePaused = true;
        }
        if (keepCreatePaused) {
            accessor.createArmedArms$setChasedPointProgress(0.999f);
        }
        return false;
    }

    public static boolean shouldOverrideMovement(ArmBlockEntity arm) {
        ArmedArmState state = (ArmedArmState)arm;
        Level level = arm.getLevel();
        if (level == null || !state.createArmedArms$isArmed()) {
            return false;
        }
        ItemStack weapon = state.createArmedArms$getWeapon();
        if (!WeaponStats.isRangedWeapon(weapon)) {
            return true;
        }
        if (CreateArmedArms.needsRangedAmmo(state)) {
            return true;
        }
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), CreateArmedArms.rangedTargetingRange(), 4.0);
        if (target != null) {
            return true;
        }
        return !CreateArmedArms.isNearRestPose(arm);
    }

    private static boolean tickRangedWeaponMovement(Level level, ArmBlockEntity arm, ArmedArmState state) {
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), CreateArmedArms.rangedTargetingRange(), 4.0);
        if (target == null && !CreateArmedArms.needsRangedAmmo(state)) {
            state.createArmedArms$setAimProgress(0.0f);
            if (level.isClientSide) {
                CreateArmedArms.chaseArmPose(arm, null);
            }
            return !CreateArmedArms.isNearRestPose(arm);
        }
        float speed = Math.abs(arm.getSpeed());
        if ((double)speed < 1.0) {
            return true;
        }
        if (CreateArmedArms.needsRangedAmmo(state)) {
            CreateArmedArms.tickArrowPickup(level, arm, state, speed);
            return true;
        }
        Vec3 targetPoint = TargetingService.rangedTargetPoint(arm.getBlockPos(), state.createArmedArms$getWeapon(), target);
        if (level.isClientSide) {
            CreateArmedArms.chaseArmPose(arm, targetPoint);
            return true;
        }
        CreateArmedArms.tickRangedArm((ServerLevel)level, arm, state, target);
        return true;
    }

    private static void tickArrowPickup(Level level, ArmBlockEntity arm, ArmedArmState state, float speed) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        accessor.createArmedArms$initInteractionPoints();
        List<ArmInteractionPoint> inputs = accessor.createArmedArms$getInputs();
        int inputIndex = state.createArmedArms$getPendingInputIndex();
        if (inputIndex < 0 || inputIndex >= inputs.size() || !inputs.get(inputIndex).isValid()) {
            if (level.isClientSide) {
                CreateArmedArms.chaseArmPose(arm, null);
                return;
            }
            inputIndex = CreateArmedArms.findArrowInputIndex(arm);
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
            CreateArmedArms.chaseArmPose(arm, input.getPos().getCenter());
        }
        if (CreateArmedArms.advanceActionProgress(state, speed)) {
            return;
        }
        if (level.isClientSide) {
            return;
        }
        ItemStack ammo = CreateArmedArms.consumeArrowFromInputPoint(arm, input);
        state.createArmedArms$setPendingInputIndex(-1);
        state.createArmedArms$setActionProgress(0.0f);
        if (ammo.isEmpty()) {
            arm.setChanged();
            arm.notifyUpdate();
            return;
        }
        state.createArmedArms$setLoadedAmmo(ammo);
        int drawTicks = CreateArmedArms.drawTicksFor((ServerLevel)level, arm, state.createArmedArms$getWeapon(), ammo, speed);
        state.createArmedArms$setDrawTicks(drawTicks);
        state.createArmedArms$setMaxDrawTicks(drawTicks);
        CreateArmedArms.resetAimAnimation(state);
        if (WeaponStats.rangedActionModel(state.createArmedArms$getWeapon()) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            level.playSound(null, arm.getBlockPos(), (SoundEvent)SoundEvents.CROSSBOW_LOADING_START.value(), SoundSource.BLOCKS, 0.7f, 1.0f);
        }
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static boolean needsRangedAmmo(ArmedArmState state) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        return state.createArmedArms$getLoadedAmmo().isEmpty() && (WeaponStats.rangedActionModel(weapon) != WeaponStats.RangedActionModel.CHARGE_THEN_USE || !WeaponStats.isPreCharged(weapon));
    }

    private static void tickMeleeWeaponMovement(Level level, ArmBlockEntity arm, ArmedArmState state) {
        double meleeRange = Math.max(1.5, WeaponStats.getMeleeReach(state.createArmedArms$getWeapon()));
        LivingEntity target = TargetingService.findTarget(level, arm.getBlockPos(), meleeRange, 0.0);
        if (level.isClientSide) {
            float speed = Math.abs(arm.getSpeed());
            if ((double)speed < 1.0) {
                CreateArmedArms.chaseArmPose(arm, null);
                return;
            }
            CreateArmedArms.chaseArmPose(arm, target == null ? null : TargetingService.meleeTargetPoint(target));
            return;
        }
        CreateArmedArms.tickMeleeArm((ServerLevel)level, arm, state);
    }

    private static void tickRangedArm(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        float speed = Math.abs(arm.getSpeed());
        if ((double)speed < 1.0) {
            return;
        }
        if (target == null) {
            state.createArmedArms$setAimProgress(0.0f);
            return;
        }
        CreateArmedArms.suppressEnemyAggro(target);
        if (WeaponStats.rangedActionModel(state.createArmedArms$getWeapon()) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            CreateArmedArms.tickCrossbowArm(level, arm, state, target);
            return;
        }
        CreateArmedArms.tickBowArm(level, arm, state, target);
    }

    private static void tickBowArm(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        float speed = Math.abs(arm.getSpeed());
        if (CreateArmedArms.advanceAimAnimation(state, speed)) {
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
        CreateArmedArms.performRangedWeaponActionLikePlayer(level, arm, state, target, Math.abs(arm.getSpeed()), RangedAction.FIRE);
        state.createArmedArms$setDrawTicks(0);
        state.createArmedArms$setMaxDrawTicks(0);
        state.createArmedArms$setAimProgress(0.0f);
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static void tickCrossbowArm(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        int drawTicks = state.createArmedArms$getDrawTicks();
        if (drawTicks > 0) {
            CreateArmedArms.playCrossbowLoadingSound(level, arm.getBlockPos(), state.createArmedArms$getWeapon(), drawTicks, Math.abs(arm.getSpeed()));
            state.createArmedArms$setDrawTicks(drawTicks - 1);
            arm.setChanged();
            arm.notifyUpdate();
            if (drawTicks == 1) {
                CreateArmedArms.performRangedWeaponActionLikePlayer(level, arm, state, target, Math.abs(arm.getSpeed()), RangedAction.CHARGE);
                state.createArmedArms$setMaxDrawTicks(0);
                state.createArmedArms$setCooldownTicks(WeaponStats.readyDelayTicks(state.createArmedArms$getWeapon()));
                level.playSound(null, arm.getBlockPos(), (SoundEvent)SoundEvents.CROSSBOW_LOADING_END.value(), SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return;
        }
        int cooldownTicks = state.createArmedArms$getCooldownTicks();
        if (cooldownTicks > 0) {
            state.createArmedArms$setCooldownTicks(cooldownTicks - 1);
            return;
        }
        CreateArmedArms.performRangedWeaponActionLikePlayer(level, arm, state, target, Math.abs(arm.getSpeed()), RangedAction.FIRE);
        state.createArmedArms$setAimProgress(0.0f);
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static void tickMeleeArm(ServerLevel level, ArmBlockEntity arm, ArmedArmState state) {
        float speed = Math.abs(arm.getSpeed());
        if ((double)speed < 1.0) {
            return;
        }
        CreateArmedArms.tickMeleeSwingAnimation(arm, state);
        int cooldownTicks = state.createArmedArms$getCooldownTicks();
        if (cooldownTicks > 0) {
            state.createArmedArms$setCooldownTicks(cooldownTicks - 1);
            return;
        }
        double meleeRange = Math.max(1.5, WeaponStats.getMeleeReach(state.createArmedArms$getWeapon()));
        LivingEntity target = TargetingService.findTarget((Level)level, arm.getBlockPos(), meleeRange, 0.0);
        if (target == null) {
            return;
        }
        CreateArmedArms.attackMelee(level, arm, state, target);
        state.createArmedArms$setCooldownTicks(CreateArmedArms.cooldownFor(state.createArmedArms$getWeapon(), speed));
        CreateArmedArms.startMeleeSwingAnimation(state, speed);
        CreateArmedArms.setArmVisualItem(arm, state.createArmedArms$getWeapon());
        arm.setChanged();
        arm.notifyUpdate();
    }

    private static int findArrowInputIndex(ArmBlockEntity arm) {
        int scanRange;
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        accessor.createArmedArms$initInteractionPoints();
        List<ArmInteractionPoint> inputs = accessor.createArmedArms$getInputs();
        if (inputs.isEmpty()) {
            return -1;
        }
        ArmBlockEntity.SelectionMode selectionMode = (ArmBlockEntity.SelectionMode)accessor.createArmedArms$getSelectionMode().get();
        int lastInputIndex = accessor.createArmedArms$getLastInputIndex();
        int startIndex = selectionMode == ArmBlockEntity.SelectionMode.PREFER_FIRST ? 0 : lastInputIndex + 1;
        int n = scanRange = selectionMode == ArmBlockEntity.SelectionMode.FORCED_ROUND_ROBIN ? lastInputIndex + 2 : inputs.size();
        if (scanRange > inputs.size()) {
            scanRange = inputs.size();
        }
        for (int inputIndex = Math.max(0, startIndex); inputIndex < scanRange; ++inputIndex) {
            ArmInteractionPoint input = inputs.get(inputIndex);
            if (!input.isValid() || !CreateArmedArms.inputHasArrow(arm, input)) continue;
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
            if (!WeaponStats.isArrow(input.extract(arm, slot, 1, true))) continue;
            return true;
        }
        return false;
    }

    private static ItemStack consumeArrowFromInputPoint(ArmBlockEntity arm, ArmInteractionPoint input) {
        int slotCount = input.getSlotCount(arm);
        for (int slot = 0; slot < slotCount; ++slot) {
            ItemStack stack = input.extract(arm, slot, 1, true);
            if (!WeaponStats.isArrow(stack)) continue;
            return input.extract(arm, slot, 1, false);
        }
        return ItemStack.EMPTY;
    }

    private static void performRangedWeaponActionLikePlayer(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target, float speed, RangedAction action) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        ItemStack ammo = state.createArmedArms$getLoadedAmmo();
        WeaponStats.RangedActionModel rangedActionModel = WeaponStats.rangedActionModel(weapon);
        if (rangedActionModel == null) {
            return;
        }
        if (rangedActionModel == WeaponStats.RangedActionModel.RELEASE_USE) {
            if (action != RangedAction.FIRE) {
                return;
            }
            FakePlayer shooter = FakePlayerCombatService.prepareFakeShooter(level, arm, weapon, ammo, target);
            int chargeTicks = Math.max(WeaponStats.defaultUseTicks(weapon), CreateArmedArms.drawTicksFor(level, arm, weapon, ammo, speed));
            shooter.startUsingItem(InteractionHand.MAIN_HAND);
            weapon.releaseUsing((Level)level, (LivingEntity)shooter, weapon.getUseDuration((LivingEntity)shooter) - chargeTicks);
            FakePlayerCombatService.clearProjectileThreatOwner(level, shooter);
            state.createArmedArms$setWeapon(shooter.getMainHandItem());
            state.createArmedArms$setLoadedAmmo(shooter.getOffhandItem().isEmpty() ? ItemStack.EMPTY : shooter.getOffhandItem().copyWithCount(1));
            FakePlayerCombatService.clearFakeShooter(shooter);
            return;
        }
        if (action == RangedAction.CHARGE) {
            FakePlayer shooter = FakePlayerCombatService.prepareFakeShooter(level, arm, weapon, ammo, target);
            shooter.startUsingItem(InteractionHand.MAIN_HAND);
            weapon.releaseUsing((Level)level, (LivingEntity)shooter, weapon.getUseDuration((LivingEntity)shooter) - WeaponStats.chargeDuration(weapon, (LivingEntity)shooter));
            state.createArmedArms$setWeapon(shooter.getMainHandItem());
            state.createArmedArms$setLoadedAmmo(shooter.getOffhandItem().isEmpty() ? ItemStack.EMPTY : shooter.getOffhandItem().copyWithCount(1));
            FakePlayerCombatService.clearFakeShooter(shooter);
            return;
        }
        FakePlayer shooter = FakePlayerCombatService.prepareFakeShooter(level, arm, weapon, ItemStack.EMPTY, target);
        weapon.use((Level)level, (Player)shooter, InteractionHand.MAIN_HAND);
        FakePlayerCombatService.clearProjectileThreatOwner(level, shooter);
        state.createArmedArms$setWeapon(shooter.getMainHandItem());
        state.createArmedArms$setLoadedAmmo(ItemStack.EMPTY);
        FakePlayerCombatService.clearFakeShooter(shooter);
    }

    private static void attackMelee(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        FakePlayer attacker = FakePlayerCombatService.prepareFakeShooter(level, arm, weapon, ItemStack.EMPTY, target);
        attacker.attack((Entity)target);
        state.createArmedArms$setWeapon(attacker.getMainHandItem());
        FakePlayerCombatService.clearFakeShooter(attacker);
        CreateArmedArms.suppressEnemyAggro(target);
    }

    private static int cooldownFor(ItemStack weapon, float speed) {
        int baseCooldown = CreateArmedArms.meleeBaseCooldown(weapon);
        int cooldown = Math.round((float)baseCooldown * 32.0f / speed);
        return Math.max(3, cooldown);
    }

    private static int meleeSwingTicksFor(ItemStack weapon, float speed) {
        int baseSwingTicks = Math.max(3, WeaponStats.meleeSwingBaseTicks(weapon));
        int swingTicks = Math.round((float)baseSwingTicks * 32.0f / speed);
        return Math.max(3, swingTicks);
    }

    private static void startMeleeSwingAnimation(ArmedArmState state, float speed) {
        int swingTicks = CreateArmedArms.meleeSwingTicksFor(state.createArmedArms$getWeapon(), speed);
        state.createArmedArms$setMeleeSwingTicks(swingTicks);
        state.createArmedArms$setMaxMeleeSwingTicks(swingTicks);
    }

    private static void tickMeleeSwingAnimation(ArmBlockEntity arm, ArmedArmState state) {
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

    private static int drawTicksFor(ServerLevel level, ArmBlockEntity arm, ItemStack weapon, ItemStack ammo, float speed) {
        int baseCooldown = Math.max(3, WeaponStats.defaultUseTicks(weapon));
        if (WeaponStats.rangedActionModel(weapon) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            FakePlayer shooter = FakePlayerCombatService.prepareFakeShooter(level, arm, weapon, ammo, null);
            baseCooldown = WeaponStats.chargeDuration(weapon, (LivingEntity)shooter);
            FakePlayerCombatService.clearFakeShooter(shooter);
        }
        int cooldown = Math.round((float)baseCooldown * 32.0f / speed);
        return Math.max(3, cooldown);
    }

    private static int meleeBaseCooldown(ItemStack weapon) {
        double attackSpeed = WeaponStats.getAttackSpeed(weapon);
        if (attackSpeed <= 0.0) {
            return Math.max(3, WeaponStats.defaultUseTicks(weapon));
        }
        return Math.max(3, (int)Math.ceil(20.0 / attackSpeed));
    }

    private static double rangedTargetingRange() {
        return (Double)CreateArmedArmsConfig.RANGED_TARGETING_RANGE.get();
    }

    private static void returnLoadedAmmo(Level level, BlockPos pos, Player player, ItemStack loadedAmmo) {
        if (loadedAmmo.isEmpty()) {
            return;
        }
        CreateArmedArms.giveOrDrop(level, pos, player, loadedAmmo);
    }

    private static void giveOrDrop(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        if (!player.addItem(stack.copy())) {
            Block.popResource((Level)level, (BlockPos)pos, (ItemStack)stack.copy());
        }
    }

    private static void resetAimAnimation(ArmedArmState state) {
        state.createArmedArms$setAimProgress(0.0f);
    }

    private static boolean advanceAimAnimation(ArmedArmState state, float speed) {
        float aimProgress = state.createArmedArms$getAimProgress();
        if (aimProgress >= 1.0f) {
            return false;
        }
        state.createArmedArms$setAimProgress(aimProgress += Math.min(256.0f, speed) / 1024.0f);
        return aimProgress < 1.0f;
    }

    private static boolean advanceActionProgress(ArmedArmState state, float speed) {
        float progress = state.createArmedArms$getActionProgress();
        state.createArmedArms$setActionProgress(progress += Math.min(256.0f, speed) / 1024.0f);
        return progress < 1.0f;
    }

    private static void playCrossbowLoadingSound(ServerLevel level, BlockPos pos, ItemStack weapon, int drawTicks, float speed) {
        int totalDrawTicks = Math.round((float)WeaponStats.chargeDuration(weapon, (LivingEntity)FakePlayerCombatService.getSharedFakePlayer(level)) * 32.0f / speed);
        if (drawTicks == totalDrawTicks / 2) {
            level.playSound(null, pos, (SoundEvent)SoundEvents.CROSSBOW_LOADING_MIDDLE.value(), SoundSource.BLOCKS, 0.7f, 1.0f);
        }
    }

    private static void suppressEnemyAggro(LivingEntity entity) {
        Mob mob;
        if (!(entity instanceof Mob) || !((mob = (Mob)entity) instanceof Enemy)) {
            return;
        }
        mob.setTarget(null);
    }

    private static void chaseArmPose(ArmBlockEntity arm, Vec3 target) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        float speed = Math.abs(arm.getSpeed());
        double chaseSpeed = Math.max(0.05, (double)Math.min(256.0f, speed) / 512.0);
        if (target == null) {
            CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getBaseAngle(), 0.0f, chaseSpeed *= 0.55);
            CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getLowerArmAngle(), 135.0f, chaseSpeed);
            CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getUpperArmAngle(), 45.0f, chaseSpeed);
            CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getHeadAngle(), 0.0f, chaseSpeed);
            return;
        }
        ArmAngleTarget angleTarget = new ArmAngleTarget(arm.getBlockPos(), target, Direction.UP, accessor.createArmedArms$isOnCeiling());
        ArmAngleTargetAccessor targetAccessor = (ArmAngleTargetAccessor)angleTarget;
        CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getBaseAngle(), targetAccessor.createArmedArms$getBaseAngle(), chaseSpeed);
        CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getLowerArmAngle(), targetAccessor.createArmedArms$getLowerArmAngle(), chaseSpeed);
        CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getUpperArmAngle(), targetAccessor.createArmedArms$getUpperArmAngle(), chaseSpeed);
        CreateArmedArms.chaseAngleNormalized(accessor.createArmedArms$getHeadAngle(), targetAccessor.createArmedArms$getHeadAngle(), chaseSpeed);
    }

    private static void chaseAngle(LerpedFloat angle, float target, double speed) {
        angle.chase((double)target, speed, LerpedFloat.Chaser.EXP);
        angle.tickChaser();
    }

    private static void chaseAngleNormalized(LerpedFloat angle, float target, double speed) {
        float current = angle.getValue();
        float nearestEquivalent = current + Mth.wrapDegrees((float)(target - current));
        CreateArmedArms.chaseAngle(angle, nearestEquivalent, speed);
    }

    private static boolean isNearRestPose(ArmBlockEntity arm) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        float base = accessor.createArmedArms$getBaseAngle().getValue();
        float lower = accessor.createArmedArms$getLowerArmAngle().getValue();
        float upper = accessor.createArmedArms$getUpperArmAngle().getValue();
        float head = accessor.createArmedArms$getHeadAngle().getValue();
        return Math.abs(Mth.wrapDegrees((float)(base - 0.0f))) <= 0.75f && Math.abs(Mth.wrapDegrees((float)(lower - 135.0f))) <= 0.75f && Math.abs(Mth.wrapDegrees((float)(upper - 45.0f))) <= 0.75f && Math.abs(Mth.wrapDegrees((float)head)) <= 0.75f;
    }

    private static void setArmVisualItem(ArmBlockEntity arm, ItemStack stack) {
        ItemStack visualStack;
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor)arm;
        ItemStack itemStack = visualStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        if (ItemStack.matches((ItemStack)accessor.createArmedArms$getHeldItem(), (ItemStack)visualStack)) {
            return;
        }
        accessor.createArmedArms$setHeldItem(visualStack);
        if (arm.getLevel() != null && !arm.getLevel().isClientSide) {
            arm.notifyUpdate();
        } else {
            arm.setChanged();
        }
    }

    private static void refreshArmStress(ArmBlockEntity arm) {
        if (arm.getLevel() == null || arm.getLevel().isClientSide || !arm.hasNetwork()) {
            return;
        }
        arm.getOrCreateNetwork().updateStressFor((KineticBlockEntity)arm, arm.calculateStressApplied());
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return WeaponStats.isRangedWeapon(stack);
    }

    public static boolean isArmWeapon(ItemStack stack) {
        return WeaponStats.isArmWeapon(stack);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return WeaponStats.isMeleeWeapon(stack);
    }

    private static enum RangedAction {
        CHARGE,
        FIRE;

    }
}
