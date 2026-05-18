package com.niconator.createarmedarms.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.niconator.createarmedarms.ArmedArmState;
import com.niconator.createarmedarms.CreateArmedArms;
import com.niconator.createarmedarms.client.ArmedArmClientUseEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ArmRenderer.class)
public class ArmRendererMixin {
    @Unique
    private static final float ARMED_ARMS_WEAPON_SCALE = 2.0F;

    @Unique
    private ArmBlockEntity createArmedArms$renderedArm;

    @Unique
    private ArmedArmClientUseEntity createArmedArms$useEntity;

    @Unique
    private float createArmedArms$partialTicks;

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("HEAD"))
    private void createArmedArms$captureRenderedArm(
            ArmBlockEntity arm,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci) {
        createArmedArms$renderedArm = arm;
        createArmedArms$partialTicks = partialTicks;
    }

    @ModifyArgs(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getModel(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;I)Lnet/minecraft/client/resources/model/BakedModel;"))
    private void createArmedArms$provideUsingEntityForRangedModel(Args args) {
        if (createArmedArms$renderedArm == null) {
            return;
        }

        ArmedArmState state = (ArmedArmState) createArmedArms$renderedArm;
        ItemStack stack = args.get(0);
        if (!CreateArmedArms.isRangedWeapon(stack)) {
            return;
        }

        float drawProgress = createArmedArms$drawProgress(state);
        if (drawProgress <= 0.0F) {
            return;
        }

        Level level = args.get(1);
        if (level == null) {
            return;
        }

        if (createArmedArms$useEntity == null || createArmedArms$useEntity.level() != level) {
            createArmedArms$useEntity = new ArmedArmClientUseEntity(level);
        }

        createArmedArms$useEntity.configure(stack, drawProgress);
        args.set(2, (LivingEntity) createArmedArms$useEntity);
    }

    @ModifyArg(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"), index = 1)
    private ItemDisplayContext createArmedArms$renderWeaponInHand(ItemDisplayContext original) {
        if (createArmedArms$renderedArm == null
                || !createArmedArms$shouldRenderWeaponTransform(createArmedArms$renderedArm)) {
            return original;
        }

        return ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
    }

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;render(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemDisplayContext;ZLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;IILnet/minecraft/client/resources/model/BakedModel;)V"))
    private void createArmedArms$transformWeaponInClaw(
            ArmBlockEntity arm,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci) {
        if (!createArmedArms$shouldRenderWeaponTransform(arm)) {
            return;
        }

        poseStack.scale(ARMED_ARMS_WEAPON_SCALE, ARMED_ARMS_WEAPON_SCALE, ARMED_ARMS_WEAPON_SCALE);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        createArmedArms$applyWeaponActionTransform((ArmedArmState) arm, poseStack);
        poseStack.translate(0.0D, -0.05D, 0.0D);
    }

    @Inject(method = "renderSafe(Lcom/simibubi/create/content/kinetics/mechanicalArm/ArmBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V", at = @At("RETURN"))
    private void createArmedArms$clearRenderedArm(
            ArmBlockEntity arm,
            float partialTicks,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int light,
            int overlay,
            CallbackInfo ci) {
        createArmedArms$renderedArm = null;
    }

    @Unique
    private float createArmedArms$drawProgress(ArmedArmState state) {
        int maxDrawTicks = state.createArmedArms$getMaxDrawTicks();
        if (maxDrawTicks <= 0) {
            return 0.0F;
        }

        float remainingTicks = Math.max(0.0F, state.createArmedArms$getDrawTicks() - createArmedArms$partialTicks);
        return Mth.clamp(1.0F - remainingTicks / maxDrawTicks, 0.0F, 1.0F);
    }

    @Unique
    private void createArmedArms$applyWeaponActionTransform(ArmedArmState state, PoseStack poseStack) {
        ItemStack weapon = state.createArmedArms$getWeapon();
        if (CreateArmedArms.isMeleeWeapon(weapon)) {
            createArmedArms$applyMeleeSwingTransform(state, poseStack);
            return;
        }

        if (!CreateArmedArms.isRangedWeapon(weapon)) {
            return;
        }

        float drawProgress = createArmedArms$drawProgress(state);
        if (drawProgress <= 0.0F) {
            return;
        }

        poseStack.translate(0.0D, -0.03D * drawProgress, -0.12D * drawProgress);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-7.0F * drawProgress));
    }

    @Unique
    private void createArmedArms$applyMeleeSwingTransform(ArmedArmState state, PoseStack poseStack) {
        float swingProgress = createArmedArms$meleeSwingProgress(state);
        if (swingProgress <= 0.0F) {
            return;
        }

        float playerSwing = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);
        float windup = Mth.clamp(swingProgress / 0.25F, 0.0F, 1.0F);
        float followThrough = Mth.clamp((swingProgress - 0.55F) / 0.45F, 0.0F, 1.0F);
        poseStack.translate(0.0D, -0.04D * playerSwing, -0.16D * playerSwing);
        poseStack.mulPose(Axis.ZP.rotationDegrees(38.0F * windup - 118.0F * playerSwing + 34.0F * followThrough));
        poseStack.mulPose(Axis.YP.rotationDegrees(26.0F * playerSwing));
        poseStack.mulPose(Axis.XP.rotationDegrees(-18.0F * playerSwing));
    }

    @Unique
    private float createArmedArms$meleeSwingProgress(ArmedArmState state) {
        int maxSwingTicks = state.createArmedArms$getMaxMeleeSwingTicks();
        if (maxSwingTicks <= 0) {
            return 0.0F;
        }

        float remainingTicks = Math.max(0.0F,
                state.createArmedArms$getMeleeSwingTicks() - createArmedArms$partialTicks);
        return Mth.clamp(1.0F - remainingTicks / maxSwingTicks, 0.0F, 1.0F);
    }

    @Unique
    private boolean createArmedArms$shouldRenderWeaponTransform(ArmBlockEntity arm) {
        ArmBlockEntityAccessor accessor = (ArmBlockEntityAccessor) arm;
        ItemStack heldItem = accessor.createArmedArms$getHeldItem();
        return CreateArmedArms.isArmWeapon(heldItem) || ((ArmedArmState) arm).createArmedArms$isArmed();
    }
}
