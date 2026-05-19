package com.niconator.createarmedarms;

import com.niconator.createarmedarms.mixin.ArmAngleTargetAccessor;
import com.niconator.createarmedarms.mixin.ArmBlockEntityAccessor;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import javax.annotation.Nullable;

final class ArmedArmPoseService {
    private static final float REST_ANGLE_EPSILON = 0.75f;
    private static final double REST_RETURN_SPEED_FACTOR = 0.55;
    private static final float REST_BASE_ANGLE = 0.0f;

    private ArmedArmPoseService() {
    }

    static void chaseArmPose(ArmBlockEntity arm, @Nullable Vec3 target) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        double chaseSpeed = ArmedArmSpeedScaling.poseChaseSpeed(Math.abs(arm.getSpeed()));
        if (target == null) {
            chaseAngleNormalized(accessor.createArmedArms$getBaseAngle(), REST_BASE_ANGLE,
                    chaseSpeed * REST_RETURN_SPEED_FACTOR);
            chaseAngleNormalized(accessor.createArmedArms$getLowerArmAngle(), 135.0f, chaseSpeed);
            chaseAngleNormalized(accessor.createArmedArms$getUpperArmAngle(), 45.0f, chaseSpeed);
            chaseAngleNormalized(accessor.createArmedArms$getHeadAngle(), REST_BASE_ANGLE, chaseSpeed);
            return;
        }
        ArmAngleTarget angleTarget = new ArmAngleTarget(arm.getBlockPos(), target, Direction.UP,
                accessor.createArmedArms$isOnCeiling());
        ArmAngleTargetAccessor targetAccessor = (ArmAngleTargetAccessor) angleTarget;
        chaseAngleNormalized(accessor.createArmedArms$getBaseAngle(), targetAccessor.createArmedArms$getBaseAngle(),
                chaseSpeed);
        chaseAngleNormalized(accessor.createArmedArms$getLowerArmAngle(),
                targetAccessor.createArmedArms$getLowerArmAngle(), chaseSpeed);
        chaseAngleNormalized(accessor.createArmedArms$getUpperArmAngle(),
                targetAccessor.createArmedArms$getUpperArmAngle(), chaseSpeed);
        chaseAngleNormalized(accessor.createArmedArms$getHeadAngle(), targetAccessor.createArmedArms$getHeadAngle(),
                chaseSpeed);
    }

    static boolean isNearRestPose(ArmBlockEntity arm) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        float base = accessor.createArmedArms$getBaseAngle().getValue();
        float lower = accessor.createArmedArms$getLowerArmAngle().getValue();
        float upper = accessor.createArmedArms$getUpperArmAngle().getValue();
        float head = accessor.createArmedArms$getHeadAngle().getValue();
        return Math.abs(Mth.wrapDegrees((float) (base - REST_BASE_ANGLE))) <= REST_ANGLE_EPSILON
                && Math.abs(Mth.wrapDegrees((float) (lower - 135.0f))) <= REST_ANGLE_EPSILON
                && Math.abs(Mth.wrapDegrees((float) (upper - 45.0f))) <= REST_ANGLE_EPSILON
                && Math.abs(Mth.wrapDegrees(head)) <= REST_ANGLE_EPSILON;
    }

    private static void chaseAngle(LerpedFloat angle, float target, double speed) {
        angle.chase((double) target, speed, LerpedFloat.Chaser.EXP);
        angle.tickChaser();
    }

    private static void chaseAngleNormalized(LerpedFloat angle, float target, double speed) {
        float current = angle.getValue();
        float nearestEquivalent = current + Mth.wrapDegrees((float) (target - current));
        chaseAngle(angle, nearestEquivalent, speed);
    }
}
