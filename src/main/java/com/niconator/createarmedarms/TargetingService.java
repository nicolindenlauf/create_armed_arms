package com.niconator.createarmedarms;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

import java.util.List;

public final class TargetingService {
    private static final double MOB_ARC_COMPENSATION = 0.2D;
    private static final float REFERENCE_PROJECTILE_VELOCITY = 1.6F;
    private static final double PROJECTILE_LAUNCH_OFFSET = 1.0D;

    private TargetingService() {
    }

    @Nullable
    public static LivingEntity findTarget(Level level, BlockPos pos, double range, double minRangeSqr) {
        Vec3 origin = turretOrigin(pos);
        AABB area = new AABB(pos).inflate(range);
        List<Mob> candidates = level.getEntitiesOfClass(
                Mob.class,
                area,
                mob -> mob instanceof Enemy && mob.isAlive() && !mob.isSpectator()
                        && hasLineOfSight(level, origin, mob));
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (Mob candidate : candidates) {
            double distance = candidate.distanceToSqr(origin);
            if (distance < minRangeSqr) {
                continue;
            }

            if (distance < closestDistance) {
                closest = candidate;
                closestDistance = distance;
            }
        }
        return closest;
    }

    public static Vec3 turretOrigin(BlockPos pos) {
        return Vec3.atCenterOf(pos).add(0.0D, 0.45D, 0.0D);
    }

    public static Vec3 projectileOrigin(BlockPos armPos, ItemStack weapon, LivingEntity target) {
        Vec3 origin = turretOrigin(armPos);
        Vec3 aim = rangedTargetPoint(armPos, weapon, target);
        Vec3 firingDirection = aim.subtract(origin);
        if (firingDirection.lengthSqr() < 1.0E-6D) {
            return origin;
        }

        return origin.add(firingDirection.normalize().scale(PROJECTILE_LAUNCH_OFFSET));
    }

    public static Vec3 rangedTargetPoint(BlockPos armPos, ItemStack weapon, LivingEntity target) {
        Vec3 targetBase = new Vec3(target.getX(), target.getY(1.0D / 3.0D), target.getZ());
        Vec3 origin = turretOrigin(armPos);
        double horizontalDistance = targetBase.subtract(origin).multiply(1.0D, 0.0D, 1.0D).length();
        double compensation = MOB_ARC_COMPENSATION
                * Math.pow(REFERENCE_PROJECTILE_VELOCITY / WeaponStats.projectileVelocity(weapon), 2.0D);
        return targetBase.add(0.0D, horizontalDistance * compensation, 0.0D);
    }

    public static Vec3 meleeTargetPoint(LivingEntity target) {
        return new Vec3(target.getX(), target.getY(0.5D), target.getZ());
    }

    private static boolean hasLineOfSight(Level level, Vec3 origin, LivingEntity target) {
        Vec3 targetPos = target.getEyePosition();
        ClipContext context = new ClipContext(origin, targetPos, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE,
                target);
        return level.clip(context).getType() == HitResult.Type.MISS;
    }
}
