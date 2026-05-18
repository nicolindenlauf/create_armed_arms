package com.niconator.createarmedarms.mixin;

import com.niconator.createarmedarms.ArmedArmState;
import com.niconator.createarmedarms.CreateArmedArms;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KineticBlockEntity.class)
public class KineticBlockEntityMixin {
    @Shadow
    protected float lastStressApplied;

    @Inject(method = "calculateStressApplied", at = @At("RETURN"), cancellable = true)
    private void createArmedArms$doubleRangedArmStress(CallbackInfoReturnable<Float> cir) {
        if (!((Object) this instanceof ArmBlockEntity arm)) {
            return;
        }

        ArmedArmState state = (ArmedArmState) arm;
        if (!CreateArmedArms.isRangedWeapon(state.createArmedArms$getWeapon())) {
            return;
        }

        float stress = cir.getReturnValueF() * 2.0F;
        lastStressApplied = stress;
        cir.setReturnValue(stress);
    }
}
