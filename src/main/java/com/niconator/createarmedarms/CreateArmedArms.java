package com.niconator.createarmedarms;

import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber.Bus;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.createmod.ponder.foundation.PonderIndex;

@Mod(CreateArmedArms.MODID)
public class CreateArmedArms {
    public static final String MODID = "create_armed_arms";

    public CreateArmedArms(ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.SERVER, CreateArmedArmsConfig.SPEC);
        modContainer.getEventBus().addListener(this::registerCapabilities);
        NeoForge.EVENT_BUS.register(this);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, AllBlockEntityTypes.MECHANICAL_ARM.get(),
                (arm, context) -> {
                    ArmedArmState state = (ArmedArmState) arm;
                    return state.createArmedArms$isArmed() ? new ArmedArmItemHandlerView(state) : null;
                });
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        Level level = player.level();
        BlockEntity blockEntity = level.getBlockEntity(event.getPos());
        if (!(blockEntity instanceof ArmBlockEntity)) {
            return;
        }
        ArmBlockEntity arm = (ArmBlockEntity) blockEntity;
        InteractionHand hand = event.getHand();
        ItemStack heldStack = player.getItemInHand(hand);
        if (WeaponStats.isArmWeapon(heldStack)) {
            ArmedArmController.equipOrSwapWeapon(level, arm, player, hand, heldStack);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
            return;
        }
        if (heldStack.isEmpty()) {
            ArmedArmController.disarm(level, arm, player, hand);
            event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onLivingChangeTarget(LivingChangeTargetEvent event) {
        Mob mob;
        LivingEntity livingEntity = event.getEntity();
        if (!(livingEntity instanceof Mob) || !((mob = (Mob) livingEntity) instanceof Enemy)) {
            return;
        }
        LivingEntity newTarget = event.getNewAboutToBeSetTarget();
        if (!(newTarget instanceof FakePlayer)) {
            return;
        }
        FakePlayer fakePlayer = (FakePlayer) newTarget;
        if ("[CreateArmedArms]".equals(fakePlayer.getGameProfile().getName())) {
            mob.setTarget(null);
            event.setCanceled(true);
        }
    }

    public static void tickArm(ArmBlockEntity arm) {
        ArmedArmController.tickArm(arm);
    }

    public static boolean tickWeaponMovement(ArmBlockEntity arm) {
        return ArmedArmController.tickWeaponMovement(arm);
    }

    public static boolean shouldOverrideMovement(ArmBlockEntity arm) {
        return ArmedArmController.shouldOverrideMovement(arm);
    }

    public static boolean isRangedWeapon(ItemStack stack) {
        return WeaponStats.isRangedWeapon(stack);
    }

    public static boolean isArmWeapon(ItemStack stack) {
        return WeaponStats.isArmWeapon(stack);
    }

    public static boolean isMeleeWeapon(ItemStack stack) {
        return WeaponStats.isMeleeWeapon(stack);
    }

    public static float weaponStressMultiplier() {
        return (float) CreateArmedArmsConfig.WEAPON_STRESS_MULTIPLIER.get().doubleValue();
    }

    @EventBusSubscriber(modid = MODID, bus = Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(final FMLClientSetupEvent event) {
            PonderIndex.addPlugin(new CreateArmedArmsPonderPlugin());
        }
    }
}
