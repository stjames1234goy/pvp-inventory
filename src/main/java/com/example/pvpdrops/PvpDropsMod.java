package com.example.pvpdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.GameRules;

/**
 * With keepInventory ON:
 *  - any normal death         -> vanilla behaviour, you keep your stuff
 *  - killed by another player -> you lose what the config says (default: everything)
 * With keepInventory OFF, vanilla behaviour applies (everything drops anyway).
 */
public class PvpDropsMod implements ModInitializer {
    public static volatile PvpDropsConfig config = new PvpDropsConfig();

    @Override
    public void onInitialize() {
        config = PvpDropsConfig.load();

        // /pvpdrops reload  (operators only)
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("pvpdrops")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload").executes(context -> {
                            config = PvpDropsConfig.load();
                            context.getSource().sendFeedback(() -> Text.literal("PvP Drops config reloaded."), true);
                            return 1;
                        }))));

        // Fires on the server just before a player's death is processed
        // (before vanilla decides what to drop / keep).
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            PvpDropsConfig cfg = config;
            if (cfg.enabled
                    && entity instanceof ServerPlayerEntity victim
                    && victim.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)
                    && source.getAttacker() instanceof PlayerEntity killer
                    && killer != victim) {
                dropStuff(victim, cfg);
            }
            return true; // never cancel the death
        });
    }

    private static void dropStuff(ServerPlayerEntity victim, PvpDropsConfig cfg) {
        PlayerInventory inv = victim.getInventory();

        // Slots: 0-35 main inventory + hotbar, 36-39 armor, 40 offhand
        for (int i = 0; i < inv.size(); i++) {
            boolean droppable;
            if (i < PlayerInventory.MAIN_SIZE) {
                droppable = cfg.dropMainInventory;
            } else if (i < PlayerInventory.OFF_HAND_SLOT) {
                droppable = cfg.dropArmor;
            } else {
                droppable = cfg.dropOffhand;
            }
            if (!droppable) {
                continue;
            }

            ItemStack stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }

            if (cfg.destroyVanishingCurseItems
                    && EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
                inv.setStack(i, ItemStack.EMPTY);
                continue;
            }

            victim.dropItem(stack, true, false);
            inv.setStack(i, ItemStack.EMPTY);
        }

        if (cfg.dropExperience) {
            int amount = Math.min(victim.experienceLevel * 7, 100); // same formula as vanilla
            if (amount > 0) {
                ExperienceOrbEntity.spawn(victim.getServerWorld(), victim.getPos(), amount);
            }
            // The respawned player copies these values, so zeroing them means the XP is lost.
            victim.experienceLevel = 0;
            victim.experienceProgress = 0.0F;
            victim.totalExperience = 0;
        }
    }
}
