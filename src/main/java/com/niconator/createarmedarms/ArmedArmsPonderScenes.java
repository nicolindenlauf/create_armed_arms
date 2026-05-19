package com.niconator.createarmedarms;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity.Phase;
import com.simibubi.create.content.logistics.depot.DepotBlockEntity;
import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class ArmedArmsPonderScenes {

    private ArmedArmsPonderScenes() {
    }

    static void armedArmsIntro(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("mechanical_arm_armed_arms", "Mechanical Arms as Weapon Arms");
        scene.configureBasePlate(0, 0, 5);
        scene.showBasePlate();
        scene.scaleSceneView(0.9f);

        BlockPos armPos = util.grid().at(2, 1, 2);
        BlockPos powerShaft = util.grid().at(2, 1, 5);
        BlockPos rangedInputDepot = util.grid().at(0, 2, 1);

        scene.world().showSection(util.select().position(armPos), Direction.DOWN);
        scene.idle(8);
        scene.world().showSection(util.select().fromTo(2, 1, 5, 2, 1, 3).add(util.select().position(2, 0, 5)),
                Direction.DOWN);
        scene.world().setKineticSpeed(util.select().position(armPos).add(util.select().position(powerShaft)), 48);

        scene.idle(15);

        scene.overlay().showText(58)
                .attachKeyFrame()
                .text("Right-click the arm with a melee weapon to equip it")
                .colored(PonderPalette.WHITE)
                .pointAt(util.vector().topOf(armPos))
                .placeNearTarget();

        scene.overlay().showControls(util.vector().topOf(armPos), Pointing.DOWN, 30)
                .rightClick()
                .withItem(new ItemStack(Items.IRON_SWORD));
        scene.idle(10);
        scene.world().instructArm(armPos, Phase.SEARCH_OUTPUTS, new ItemStack(Items.IRON_SWORD), -1);
        setVisualWeapon(scene, armPos, new ItemStack(Items.IRON_SWORD));
        scene.idle(58);

        scene.overlay().showText(66)
                .attachKeyFrame()
                .text("Empty-hand right-click disarms and returns the equipped weapon")
                .colored(PonderPalette.RED)
                .pointAt(util.vector().topOf(armPos))
                .placeNearTarget();
        scene.overlay().showControls(util.vector().topOf(armPos), Pointing.DOWN, 30).rightClick().withItem(ItemStack.EMPTY);
        scene.idle(10);
        scene.world().instructArm(armPos, Phase.SEARCH_INPUTS, ItemStack.EMPTY, -1);
        setVisualWeapon(scene, armPos, ItemStack.EMPTY);
        scene.idle(66);

        scene.overlay().showText(58)
                .attachKeyFrame()
                .text("Right-click the arm with a bow to equip ranged mode")
                .colored(PonderPalette.BLUE)
                .pointAt(util.vector().topOf(armPos))
                .placeNearTarget();

        scene.overlay().showControls(util.vector().topOf(armPos), Pointing.DOWN, 35)
                .rightClick()
                .withItem(new ItemStack(Items.BOW));
        scene.idle(10);
        setVisualWeapon(scene, armPos, new ItemStack(Items.BOW));
        scene.idle(58);

        scene.overlay().showText(54)
                .attachKeyFrame()
                .text("Provide arrows on a depot as input ammo source")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().topOf(rangedInputDepot))
                .placeNearTarget();
        scene.world().showSection(util.select().fromTo(0, 1, 1, 0, 2, 1), Direction.DOWN);
        scene.idle(8);
        scene.world().modifyBlockEntity(rangedInputDepot, DepotBlockEntity.class,
                depot -> depot.setHeldItem(new ItemStack(Items.ARROW, 16)));
        scene.overlay().showControls(util.vector().topOf(rangedInputDepot), Pointing.LEFT, 35)
                .withItem(new ItemStack(Items.ARROW));
        scene.idle(54);

        scene.overlay().showText(62)
                .attachKeyFrame()
                .text("The arm consumes arrows from its input and fires at targets")
                .colored(PonderPalette.OUTPUT)
                .pointAt(util.vector().topOf(armPos))
                .placeNearTarget();
        scene.idle(62);

        scene.idle(35);
    }

    private static void setVisualWeapon(CreateSceneBuilder scene, BlockPos armPos, ItemStack stack) {
        scene.world().modifyBlockEntity(armPos, ArmBlockEntity.class, arm -> {
            ItemStack visualStack = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
            ArmedArmState state = (ArmedArmState) arm;
            state.createArmedArms$setWeapon(visualStack);
            ArmedArmAnimationStateService.setArmVisualItem(arm, visualStack);
            arm.setChanged();
        });
    }
}
