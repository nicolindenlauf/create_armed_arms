package com.niconator.createarmedarms.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmAngleTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ArmAngleTarget.class)
public interface ArmAngleTargetAccessor {
    @Accessor("baseAngle")
    float createArmedArms$getBaseAngle();

    @Accessor("lowerArmAngle")
    float createArmedArms$getLowerArmAngle();

    @Accessor("upperArmAngle")
    float createArmedArms$getUpperArmAngle();

    @Accessor("headAngle")
    float createArmedArms$getHeadAngle();
}
