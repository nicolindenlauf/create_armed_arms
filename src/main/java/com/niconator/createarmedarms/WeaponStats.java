package com.niconator.createarmedarms;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.UseAnim;
import net.minecraft.tags.ItemTags;
import net.neoforged.neoforge.common.Tags;
import javax.annotation.Nullable;

public final class WeaponStats {
    public enum WeaponType {
        NONE,
        MELEE,
        RANGED
    }

    public enum RangedActionModel {
        RELEASE_USE,
        CHARGE_THEN_USE
    }

    public record WeaponProfile(
            WeaponType type,
            @Nullable RangedActionModel rangedActionModel,
            int defaultUseTicks,
            int readyDelayTicks,
            int meleeSwingBaseTicks,
            float projectileVelocity) {
    }

    private static final WeaponProfile NONE_PROFILE = new WeaponProfile(WeaponType.NONE, null, 0, 0, 0, 0.0F);
    private static final WeaponProfile MELEE_PROFILE = new WeaponProfile(
            WeaponType.MELEE,
            null,
            20,
            0,
            10,
            0.0F);
    private static final WeaponProfile RANGED_RELEASE_PROFILE = new WeaponProfile(
            WeaponType.RANGED,
            RangedActionModel.RELEASE_USE,
            20,
            0,
            0,
            3.0F);
    private static final WeaponProfile RANGED_CHARGE_USE_PROFILE = new WeaponProfile(
            WeaponType.RANGED,
            RangedActionModel.CHARGE_THEN_USE,
            25,
            20,
            0,
            3.15F);

    private WeaponStats() {
    }

    @SuppressWarnings("null")
    public static WeaponType getWeaponType(ItemStack stack) {
        if (stack.isEmpty()) {
            return WeaponType.NONE;
        }
        if (stack.is(Tags.Items.MELEE_WEAPON_TOOLS)) {
            return WeaponType.MELEE;
        }
        if (stack.getItem() instanceof ProjectileWeaponItem) {
            return WeaponType.RANGED;
        }
        return WeaponType.NONE;
    }

    public static WeaponProfile getWeaponProfile(ItemStack stack) {
        WeaponType type = getWeaponType(stack);
        if (type == WeaponType.MELEE) {
            return MELEE_PROFILE;
        }
        if (type != WeaponType.RANGED) {
            return NONE_PROFILE;
        }
        return rangedActionModel(stack) == RangedActionModel.CHARGE_THEN_USE
                ? RANGED_CHARGE_USE_PROFILE
                : RANGED_RELEASE_PROFILE;
    }

    @Nullable
    public static RangedActionModel rangedActionModel(ItemStack stack) {
        if (getWeaponType(stack) != WeaponType.RANGED) {
            return null;
        }
        return stack.getUseAnimation() == UseAnim.CROSSBOW
                ? RangedActionModel.CHARGE_THEN_USE
                : RangedActionModel.RELEASE_USE;
    }

    public static boolean isReleaseUseRangedWeapon(ItemStack stack) {
        return rangedActionModel(stack) == RangedActionModel.RELEASE_USE;
    }

    public static boolean isChargeUseRangedWeapon(ItemStack stack) {
        return rangedActionModel(stack) == RangedActionModel.CHARGE_THEN_USE;
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return getWeaponType(stack) == WeaponType.RANGED;
    }

    public static boolean isArmWeapon(ItemStack stack) {
        return getWeaponType(stack) != WeaponType.NONE;
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return getWeaponType(stack) == WeaponType.MELEE;
    }

    @SuppressWarnings("null")
    public static boolean isArrow(ItemStack stack) {
        return stack.is(ItemTags.ARROWS);
    }

    public static int defaultUseTicks(ItemStack stack) {
        return getWeaponProfile(stack).defaultUseTicks();
    }

    public static int readyDelayTicks(ItemStack stack) {
        return getWeaponProfile(stack).readyDelayTicks();
    }

    public static int meleeSwingBaseTicks(ItemStack stack) {
        return getWeaponProfile(stack).meleeSwingBaseTicks();
    }

    public static float projectileVelocity(ItemStack stack) {
        return getWeaponProfile(stack).projectileVelocity();
    }

    @SuppressWarnings("null")
    public static int chargeDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity shooter) {
        if (stack.getItem() instanceof CrossbowItem) {
            return CrossbowItem.getChargeDuration(stack, shooter);
        }
        return Math.max(1, defaultUseTicks(stack));
    }

    public static boolean isPreCharged(ItemStack stack) {
        return stack.getItem() instanceof CrossbowItem && CrossbowItem.isCharged(stack);
    }

    public static double getAttackDamage(ItemStack stack) {
        return computeAttributeValue(stack, Attributes.ATTACK_DAMAGE, 1.0D);
    }

    public static double getAttackSpeed(ItemStack stack) {
        return computeAttributeValue(stack, Attributes.ATTACK_SPEED, 4.0D);
    }

    public static double getMeleeReach(ItemStack stack) {
        // Vanilla baseline interaction reach is 3 blocks; weapon modifiers can extend
        // this.
        return computeAttributeValue(stack, Attributes.ENTITY_INTERACTION_RANGE, 3.0D);
    }

    private static double computeAttributeValue(
            ItemStack stack,
            net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute,
            double baseValue) {
        double[] value = { baseValue };
        stack.forEachModifier(EquipmentSlot.MAINHAND, (candidateAttribute, modifier) -> {
            if (!candidateAttribute.equals(attribute)) {
                return;
            }

            value[0] = applyAttributeModifier(value[0], baseValue, modifier);
        });
        return value[0];
    }

    private static double applyAttributeModifier(double value, double baseValue, AttributeModifier modifier) {
        return switch (modifier.operation()) {
            case ADD_VALUE -> value + modifier.amount();
            case ADD_MULTIPLIED_BASE -> value + modifier.amount() * baseValue;
            case ADD_MULTIPLIED_TOTAL -> value * (1.0D + modifier.amount());
        };
    }
}
