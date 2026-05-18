package com.niconator.createarmedarms;

import net.minecraft.world.item.ItemStack;

public interface ArmedArmState {
    ItemStack createArmedArms$getWeapon();

    void createArmedArms$setWeapon(ItemStack weapon);

    ItemStack createArmedArms$getLoadedAmmo();

    void createArmedArms$setLoadedAmmo(ItemStack ammo);

    int createArmedArms$getDrawTicks();

    void createArmedArms$setDrawTicks(int drawTicks);

    int createArmedArms$getMaxDrawTicks();

    void createArmedArms$setMaxDrawTicks(int maxDrawTicks);

    int createArmedArms$getCooldownTicks();

    void createArmedArms$setCooldownTicks(int cooldownTicks);

    int createArmedArms$getMeleeSwingTicks();

    void createArmedArms$setMeleeSwingTicks(int meleeSwingTicks);

    int createArmedArms$getMaxMeleeSwingTicks();

    void createArmedArms$setMaxMeleeSwingTicks(int maxMeleeSwingTicks);

    float createArmedArms$getAimProgress();

    void createArmedArms$setAimProgress(float aimProgress);

    float createArmedArms$getActionProgress();

    void createArmedArms$setActionProgress(float actionProgress);

    int createArmedArms$getPendingInputIndex();

    void createArmedArms$setPendingInputIndex(int pendingInputIndex);

    default boolean createArmedArms$isArmed() {
        return !createArmedArms$getWeapon().isEmpty();
    }
}
