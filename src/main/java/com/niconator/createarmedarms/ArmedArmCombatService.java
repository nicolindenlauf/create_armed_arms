package com.niconator.createarmedarms;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.FakePlayer;

final class ArmedArmCombatService {
    private static final int MIN_COOLDOWN = 3;

    private ArmedArmCombatService() {
    }

    static void performRangedWeaponActionLikePlayer(ServerLevel level, ArmBlockEntity arm, ArmedArmState state,
            LivingEntity target, float speed, RangedAction action) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        ItemStack ammo = state.createArmedArms$getLoadedAmmo();
        WeaponStats.RangedActionModel rangedActionModel = WeaponStats.rangedActionModel(weapon);
        if (rangedActionModel == WeaponStats.RangedActionModel.RELEASE_USE) {
            if (action != RangedAction.FIRE) {
                return;
            }
            FakePlayer shooter = FakePlayerCombatService.prepareFakeCombatPlayer(level, arm, weapon, ammo, target);
            int chargeTicks = Math.max(WeaponStats.defaultUseTicks(weapon), computeDrawTicks(level, arm, weapon, ammo, speed));
            shooter.startUsingItem(InteractionHand.MAIN_HAND);
            weapon.releaseUsing(level, shooter, weapon.getUseDuration(shooter) - chargeTicks);
            FakePlayerCombatService.clearProjectileThreatOwner(level, shooter);
            state.createArmedArms$setWeapon(shooter.getMainHandItem());
            state.createArmedArms$setLoadedAmmo(
                    shooter.getOffhandItem().isEmpty() ? ItemStack.EMPTY : shooter.getOffhandItem().copyWithCount(1));
            FakePlayerCombatService.clearFakeCombatPlayer(shooter);
            return;
        }
        if (action == RangedAction.CHARGE) {
            FakePlayer shooter = FakePlayerCombatService.prepareFakeCombatPlayer(level, arm, weapon, ammo, target);
            shooter.startUsingItem(InteractionHand.MAIN_HAND);
            weapon.releaseUsing(level, shooter,
                    weapon.getUseDuration(shooter) - WeaponStats.chargeDuration(weapon, shooter));
            state.createArmedArms$setWeapon(shooter.getMainHandItem());
            state.createArmedArms$setLoadedAmmo(
                    shooter.getOffhandItem().isEmpty() ? ItemStack.EMPTY : shooter.getOffhandItem().copyWithCount(1));
            FakePlayerCombatService.clearFakeCombatPlayer(shooter);
            return;
        }
        FakePlayer shooter = FakePlayerCombatService.prepareFakeCombatPlayer(level, arm, weapon, ItemStack.EMPTY,
                target);
        weapon.use(level, shooter, InteractionHand.MAIN_HAND);
        FakePlayerCombatService.clearProjectileThreatOwner(level, shooter);
        state.createArmedArms$setWeapon(shooter.getMainHandItem());
        state.createArmedArms$setLoadedAmmo(ItemStack.EMPTY);
        FakePlayerCombatService.clearFakeCombatPlayer(shooter);
    }

    static void attackMelee(ServerLevel level, ArmBlockEntity arm, ArmedArmState state, LivingEntity target) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        FakePlayer attacker = FakePlayerCombatService.prepareFakeCombatPlayer(level, arm, weapon, ItemStack.EMPTY,
                target);
        attacker.attack(target);
        state.createArmedArms$setWeapon(attacker.getMainHandItem());
        FakePlayerCombatService.clearFakeCombatPlayer(attacker);
        ArmedArmController.suppressEnemyAggro(target);
    }

    static int computeDrawTicks(ServerLevel level, ArmBlockEntity arm, ItemStack weapon, ItemStack ammo,
            float speed) {
        int baseCooldown = Math.max(MIN_COOLDOWN, WeaponStats.defaultUseTicks(weapon));
        if (WeaponStats.rangedActionModel(weapon) == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
            FakePlayer shooter = FakePlayerCombatService.prepareFakeCombatPlayer(level, arm, weapon, ammo, null);
            baseCooldown = WeaponStats.chargeDuration(weapon, shooter);
            FakePlayerCombatService.clearFakeCombatPlayer(shooter);
        }
        return ArmedArmSpeedScaling.adjustedTickCount(baseCooldown, speed, MIN_COOLDOWN);
    }

    enum RangedAction {
        CHARGE,
        FIRE
    }
}
