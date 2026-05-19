package com.niconator.createarmedarms.client;

import com.niconator.createarmedarms.WeaponStats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import javax.annotation.Nonnull;

import java.util.List;

public class ArmedArmClientUseEntity extends LivingEntity {
    private ItemStack renderedUseItem = ItemStack.EMPTY;
    private int renderedUseItemRemainingTicks;
    private boolean renderedUsingItem;

    public ArmedArmClientUseEntity(Level level) {
        super(EntityType.ARMOR_STAND, level);
    }

    public void configure(ItemStack stack, float useProgress) {
        renderedUseItem = stack;
        WeaponStats.RangedActionModel actionModel = WeaponStats.rangedActionModel(stack);
        renderedUsingItem = useProgress > 0.0F
                && actionModel != null
                && !(actionModel == WeaponStats.RangedActionModel.CHARGE_THEN_USE && WeaponStats.isPreCharged(stack));

        int elapsedTicks = 0;
        if (renderedUsingItem) {
            if (actionModel == WeaponStats.RangedActionModel.CHARGE_THEN_USE) {
                elapsedTicks = Math.round(WeaponStats.chargeDuration(stack, this) * useProgress);
            } else {
                elapsedTicks = Math.round(WeaponStats.defaultUseTicks(stack) * useProgress);
            }
        }

        renderedUseItemRemainingTicks = Math.max(0, stack.getUseDuration(this) - elapsedTicks);
    }

    @Override
    public boolean isUsingItem() {
        return renderedUsingItem;
    }

    @Override
    public InteractionHand getUsedItemHand() {
        return InteractionHand.MAIN_HAND;
    }

    @Override
    public ItemStack getUseItem() {
        return renderedUseItem;
    }

    @Override
    public int getUseItemRemainingTicks() {
        return renderedUseItemRemainingTicks;
    }

    @Override
    public ItemStack getMainHandItem() {
        return renderedUseItem;
    }

    @Override
    public ItemStack getItemInHand(@Nonnull InteractionHand hand) {
        return hand == InteractionHand.MAIN_HAND ? renderedUseItem : ItemStack.EMPTY;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return List.of();
    }

    @Override
    public ItemStack getItemBySlot(@Nonnull EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND ? renderedUseItem : ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(@Nonnull EquipmentSlot slot, @Nonnull ItemStack stack) {
    }

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }
}
