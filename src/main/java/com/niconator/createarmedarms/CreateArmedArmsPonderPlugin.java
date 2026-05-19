package com.niconator.createarmedarms;

import com.simibubi.create.AllBlocks;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

final class CreateArmedArmsPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateArmedArms.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<Block> blockHelper = helper.withKeyFunction(BuiltInRegistries.BLOCK::getKey);

        blockHelper.forComponents(AllBlocks.MECHANICAL_ARM.get())
                .addStoryBoard("mechanical_arm/armed_arms", ArmedArmsPonderScenes::armedArmsIntro);
    }
}
