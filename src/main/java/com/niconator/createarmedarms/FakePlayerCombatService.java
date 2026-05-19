package com.niconator.createarmedarms;

import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import javax.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public final class FakePlayerCombatService {
    private static final GameProfile ARMORY_PROFILE = new GameProfile(
            UUID.fromString("5a5f7d56-f403-4d48-97b4-3d05ed7107b1"), "[CreateArmedArms]");

    private FakePlayerCombatService() {
    }

    public static FakePlayer getSharedFakePlayer(ServerLevel level) {
        return FakePlayerFactory.get(level, ARMORY_PROFILE);
    }

    public static FakePlayer prepareFakeCombatPlayer(ServerLevel level, ArmBlockEntity arm, ItemStack weapon,
            ItemStack ammo,
            @Nullable LivingEntity target) {
        FakePlayer shooter = getSharedFakePlayer(level);
        clearFakeCombatPlayer(shooter);
        Vec3 origin = target == null
                ? TargetingService.turretOrigin(arm.getBlockPos())
                : TargetingService.projectileOrigin(arm.getBlockPos(), weapon, target);
        shooter.getAbilities().instabuild = false;
        shooter.getInventory().selected = 0;
        shooter.setPos(origin.x, origin.y + 0.1D - shooter.getEyeHeight(), origin.z);
        shooter.setItemInHand(InteractionHand.MAIN_HAND, weapon);
        shooter.setItemInHand(InteractionHand.OFF_HAND, ammo.isEmpty() ? ItemStack.EMPTY : ammo.copy());

        if (target != null) {
            Vec3 aim = TargetingService.rangedTargetPoint(arm.getBlockPos(), weapon, target);
            double dx = aim.x - origin.x;
            double dy = aim.y - origin.y;
            double dz = aim.z - origin.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            float yRot = (float) (Mth.atan2(dz, dx) * 180.0D / Math.PI) - 90.0F;
            float xRot = (float) -(Mth.atan2(dy, horizontalDistance) * 180.0D / Math.PI);
            shooter.setYRot(yRot);
            shooter.setXRot(xRot);
            shooter.yHeadRot = yRot;
            shooter.yBodyRot = yRot;
            shooter.yRotO = yRot;
            shooter.xRotO = xRot;
            shooter.yHeadRotO = yRot;
            shooter.yBodyRotO = yRot;
        }
        return shooter;
    }

    public static void clearFakeCombatPlayer(FakePlayer shooter) {
        shooter.stopUsingItem();
        for (int slot = 0; slot < shooter.getInventory().getContainerSize(); slot++) {
            shooter.getInventory().setItem(slot, ItemStack.EMPTY);
        }
    }

    public static void clearProjectileThreatOwner(ServerLevel level, FakePlayer shooter) {
        AABB searchArea = shooter.getBoundingBox().inflate(4.0D);
        List<Projectile> projectiles = level.getEntitiesOfClass(
                Projectile.class,
                searchArea,
                projectile -> projectile.getOwner() == shooter && projectile.tickCount <= 1);

        for (Projectile projectile : projectiles) {
            projectile.setOwner(null);
        }
    }
}
