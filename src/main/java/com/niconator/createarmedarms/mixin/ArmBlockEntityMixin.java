package com.niconator.createarmedarms.mixin;

import com.niconator.createarmedarms.ArmedArmState;
import com.niconator.createarmedarms.CreateArmedArms;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import javax.annotation.Nonnull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ArmBlockEntity.class)
public class ArmBlockEntityMixin implements ArmedArmState {
    @Shadow
    ItemStack heldItem;

    @Unique
    private ItemStack createArmedArms$weapon = ItemStack.EMPTY;
    @Unique
    private ItemStack createArmedArms$loadedAmmo = ItemStack.EMPTY;
    @Unique
    private int createArmedArms$drawTicks;
    @Unique
    private int createArmedArms$maxDrawTicks;
    @Unique
    private int createArmedArms$cooldownTicks;
    @Unique
    private int createArmedArms$meleeSwingTicks;
    @Unique
    private int createArmedArms$maxMeleeSwingTicks;
    @Unique
    private float createArmedArms$aimProgress = 1.0F;
    @Unique
    private float createArmedArms$actionProgress;
    @Unique
    private int createArmedArms$pendingInputIndex = -1;

    @Override
    public ItemStack createArmedArms$getWeapon() {
        return createArmedArms$weapon;
    }

    @Override
    public void createArmedArms$setWeapon(ItemStack weapon) {
        createArmedArms$weapon = weapon.isEmpty() ? ItemStack.EMPTY : weapon.copyWithCount(1);
        if (createArmedArms$weapon.isEmpty()) {
            createArmedArms$loadedAmmo = ItemStack.EMPTY;
            createArmedArms$drawTicks = 0;
            createArmedArms$maxDrawTicks = 0;
            createArmedArms$cooldownTicks = 0;
            createArmedArms$meleeSwingTicks = 0;
            createArmedArms$maxMeleeSwingTicks = 0;
            createArmedArms$aimProgress = 1.0F;
            createArmedArms$actionProgress = 0.0F;
            createArmedArms$pendingInputIndex = -1;
        }
    }

    @Override
    public ItemStack createArmedArms$getLoadedAmmo() {
        return createArmedArms$loadedAmmo;
    }

    @Override
    public void createArmedArms$setLoadedAmmo(ItemStack ammo) {
        createArmedArms$loadedAmmo = ammo.isEmpty() ? ItemStack.EMPTY : ammo.copyWithCount(1);
    }

    @Override
    public int createArmedArms$getDrawTicks() {
        return createArmedArms$drawTicks;
    }

    @Override
    public void createArmedArms$setDrawTicks(int drawTicks) {
        createArmedArms$drawTicks = Math.max(0, drawTicks);
    }

    @Override
    public int createArmedArms$getMaxDrawTicks() {
        return createArmedArms$maxDrawTicks;
    }

    @Override
    public void createArmedArms$setMaxDrawTicks(int maxDrawTicks) {
        createArmedArms$maxDrawTicks = Math.max(0, maxDrawTicks);
    }

    @Override
    public int createArmedArms$getCooldownTicks() {
        return createArmedArms$cooldownTicks;
    }

    @Override
    public void createArmedArms$setCooldownTicks(int cooldownTicks) {
        createArmedArms$cooldownTicks = Math.max(0, cooldownTicks);
    }

    @Override
    public int createArmedArms$getMeleeSwingTicks() {
        return createArmedArms$meleeSwingTicks;
    }

    @Override
    public void createArmedArms$setMeleeSwingTicks(int meleeSwingTicks) {
        createArmedArms$meleeSwingTicks = Math.max(0, meleeSwingTicks);
    }

    @Override
    public int createArmedArms$getMaxMeleeSwingTicks() {
        return createArmedArms$maxMeleeSwingTicks;
    }

    @Override
    public void createArmedArms$setMaxMeleeSwingTicks(int maxMeleeSwingTicks) {
        createArmedArms$maxMeleeSwingTicks = Math.max(0, maxMeleeSwingTicks);
    }

    @Override
    public float createArmedArms$getAimProgress() {
        return createArmedArms$aimProgress;
    }

    @Override
    public void createArmedArms$setAimProgress(float aimProgress) {
        createArmedArms$aimProgress = Math.max(0.0F, Math.min(1.0F, aimProgress));
    }

    @Override
    public float createArmedArms$getActionProgress() {
        return createArmedArms$actionProgress;
    }

    @Override
    public void createArmedArms$setActionProgress(float actionProgress) {
        createArmedArms$actionProgress = Math.max(0.0F, Math.min(1.0F, actionProgress));
    }

    @Override
    public int createArmedArms$getPendingInputIndex() {
        return createArmedArms$pendingInputIndex;
    }

    @Override
    public void createArmedArms$setPendingInputIndex(int pendingInputIndex) {
        createArmedArms$pendingInputIndex = pendingInputIndex;
    }

    @Inject(method = "tickMovementProgress", at = @At("HEAD"), cancellable = true)
    private void createArmedArms$tickWeaponMovement(CallbackInfoReturnable<Boolean> cir) {
        createArmedArms$recoverWeaponFromHeldItem();
        if (!createArmedArms$isArmed()) {
            return;
        }

        ArmBlockEntity arm = (ArmBlockEntity) (Object) this;
        if (!CreateArmedArms.shouldOverrideMovement(arm)) {
            return;
        }

        cir.setReturnValue(CreateArmedArms.tickWeaponMovement(arm));
    }

    @Inject(method = "destroy", at = @At("TAIL"))
    private void createArmedArms$destroy(CallbackInfo ci) {
        if (createArmedArms$loadedAmmo.isEmpty()) {
            return;
        }

        ArmBlockEntity arm = (ArmBlockEntity) (Object) this;
        Level level = arm.getLevel();
        if (level != null && !level.isClientSide) {
            Block.popResource(level, arm.getBlockPos(), createArmedArms$loadedAmmo.copy());
        }
        createArmedArms$loadedAmmo = ItemStack.EMPTY;
    }

    @Inject(method = "write", at = @At("TAIL"))
    private void createArmedArms$write(CompoundTag tag, @Nonnull HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        createArmedArms$writeArmoryTag(tag, registries);
    }

    @Inject(method = "writeSafe", at = @At("TAIL"))
    private void createArmedArms$writeSafe(CompoundTag tag, @Nonnull HolderLookup.Provider registries, CallbackInfo ci) {
        createArmedArms$writeArmoryTag(tag, registries);
    }

    @Unique
    private void createArmedArms$writeArmoryTag(CompoundTag tag, @Nonnull HolderLookup.Provider registries) {
        CompoundTag armoryTag = new CompoundTag();
        ItemStack weapon = createArmedArms$weapon.isEmpty() && CreateArmedArms.isArmWeapon(heldItem)
                ? heldItem.copyWithCount(1)
                : createArmedArms$weapon;
        armoryTag.putBoolean("Armed", !weapon.isEmpty());
        armoryTag.put("Weapon", weapon.saveOptional(registries));
        armoryTag.put("LoadedAmmo", createArmedArms$loadedAmmo.saveOptional(registries));
        armoryTag.putInt("DrawTicks", createArmedArms$drawTicks);
        armoryTag.putInt("MaxDrawTicks", createArmedArms$maxDrawTicks);
        armoryTag.putInt("CooldownTicks", createArmedArms$cooldownTicks);
        armoryTag.putInt("MeleeSwingTicks", createArmedArms$meleeSwingTicks);
        armoryTag.putInt("MaxMeleeSwingTicks", createArmedArms$maxMeleeSwingTicks);
        armoryTag.putFloat("AimProgress", createArmedArms$aimProgress);
        armoryTag.putFloat("ActionProgress", createArmedArms$actionProgress);
        armoryTag.putInt("PendingInputIndex", createArmedArms$pendingInputIndex);
        tag.put("CreateArmedArms", armoryTag);
    }

    @Inject(method = "read", at = @At("TAIL"))
    private void createArmedArms$read(CompoundTag tag, @Nonnull HolderLookup.Provider registries, boolean clientPacket,
            CallbackInfo ci) {
        if (tag.contains("CreateArmedArms", 10)) {
            CompoundTag armoryTag = tag.getCompound("CreateArmedArms");
            createArmedArms$weapon = ItemStack.parseOptional(registries, armoryTag.getCompound("Weapon"));
            createArmedArms$loadedAmmo = ItemStack.parseOptional(registries, armoryTag.getCompound("LoadedAmmo"));
            createArmedArms$drawTicks = armoryTag.getInt("DrawTicks");
            createArmedArms$maxDrawTicks = armoryTag.getInt("MaxDrawTicks");
            createArmedArms$cooldownTicks = armoryTag.getInt("CooldownTicks");
            createArmedArms$meleeSwingTicks = armoryTag.getInt("MeleeSwingTicks");
            createArmedArms$maxMeleeSwingTicks = armoryTag.getInt("MaxMeleeSwingTicks");
            createArmedArms$aimProgress = armoryTag.getFloat("AimProgress");
            createArmedArms$actionProgress = armoryTag.getFloat("ActionProgress");
            createArmedArms$pendingInputIndex = armoryTag.getInt("PendingInputIndex");
            if (armoryTag.getBoolean("Armed") && createArmedArms$weapon.isEmpty()
                    && CreateArmedArms.isArmWeapon(heldItem)) {
                createArmedArms$weapon = heldItem.copyWithCount(1);
            }
        } else {
            createArmedArms$weapon = ItemStack.EMPTY;
            createArmedArms$loadedAmmo = ItemStack.EMPTY;
            createArmedArms$drawTicks = 0;
            createArmedArms$maxDrawTicks = 0;
            createArmedArms$cooldownTicks = 0;
            createArmedArms$meleeSwingTicks = 0;
            createArmedArms$maxMeleeSwingTicks = 0;
            createArmedArms$aimProgress = 1.0F;
            createArmedArms$actionProgress = 0.0F;
            createArmedArms$pendingInputIndex = -1;
        }

        if (createArmedArms$weapon.isEmpty() && CreateArmedArms.isArmWeapon(heldItem)) {
            createArmedArms$weapon = heldItem.copyWithCount(1);
        }

        if (createArmedArms$weapon.isEmpty()) {
            createArmedArms$loadedAmmo = ItemStack.EMPTY;
            createArmedArms$drawTicks = 0;
            createArmedArms$maxDrawTicks = 0;
            createArmedArms$cooldownTicks = 0;
            createArmedArms$meleeSwingTicks = 0;
            createArmedArms$maxMeleeSwingTicks = 0;
            createArmedArms$aimProgress = 1.0F;
            createArmedArms$actionProgress = 0.0F;
            createArmedArms$pendingInputIndex = -1;
            return;
        }

        heldItem = createArmedArms$weapon.copyWithCount(1);
    }

    @Unique
    private void createArmedArms$recoverWeaponFromHeldItem() {
        if (!createArmedArms$weapon.isEmpty() || !CreateArmedArms.isArmWeapon(heldItem)) {
            return;
        }

        createArmedArms$weapon = heldItem.copyWithCount(1);
        createArmedArms$loadedAmmo = ItemStack.EMPTY;
        createArmedArms$drawTicks = 0;
        createArmedArms$maxDrawTicks = 0;
        createArmedArms$cooldownTicks = 0;
        createArmedArms$meleeSwingTicks = 0;
        createArmedArms$maxMeleeSwingTicks = 0;
        createArmedArms$aimProgress = 1.0F;
        createArmedArms$actionProgress = 0.0F;
        createArmedArms$pendingInputIndex = -1;
    }
}
