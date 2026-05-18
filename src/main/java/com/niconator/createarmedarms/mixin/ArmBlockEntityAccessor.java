package com.niconator.createarmedarms.mixin;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollOptionBehaviour;
import net.createmod.catnip.animation.LerpedFloat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(ArmBlockEntity.class)
public interface ArmBlockEntityAccessor {
    @Accessor("inputs")
    List<ArmInteractionPoint> createArmedArms$getInputs();

    @Accessor("heldItem")
    ItemStack createArmedArms$getHeldItem();

    @Accessor("heldItem")
    void createArmedArms$setHeldItem(ItemStack heldItem);

    @Accessor("selectionMode")
    ScrollOptionBehaviour<ArmBlockEntity.SelectionMode> createArmedArms$getSelectionMode();

    @Accessor("lastInputIndex")
    int createArmedArms$getLastInputIndex();

    @Accessor("lastInputIndex")
    void createArmedArms$setLastInputIndex(int lastInputIndex);

    @Accessor("chasedPointProgress")
    void createArmedArms$setChasedPointProgress(float chasedPointProgress);

    @Accessor("baseAngle")
    LerpedFloat createArmedArms$getBaseAngle();

    @Accessor("lowerArmAngle")
    LerpedFloat createArmedArms$getLowerArmAngle();

    @Accessor("upperArmAngle")
    LerpedFloat createArmedArms$getUpperArmAngle();

    @Accessor("headAngle")
    LerpedFloat createArmedArms$getHeadAngle();

    @Invoker("initInteractionPoints")
    void createArmedArms$initInteractionPoints();

    @Invoker("isOnCeiling")
    boolean createArmedArms$isOnCeiling();
}
