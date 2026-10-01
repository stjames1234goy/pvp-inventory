package com.example.pvpdrops;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.component.EnchantmentEffectComponentTypes;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.GameRules;

/**
 * With keepInventory ON:
 *  - any normal death        -> vanilla behaviour, you keep your stuff
 *  - killed by another player -> your whole inventory is dropped
 * With keepInventory OFF, vanilla behaviour applies (everything drops anyway).
 */
public class PvpDropsMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Fires on the server just before a player's death is processed
        // (before vanilla decides what to drop / keep).
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, damageAmount) -> {
            if (entity instanceof ServerPlayerEntity victim
                    && victim.getServerWorld().getGameRules().getBoolean(GameRules.KEEP_INVENTORY)
                    && source.getAttacker() instanceof PlayerEntity killer
                    && killer != victim) {
                dropInventory(victim);
            }
            return true; // never cancel the death
        });
    }

    private static void dropInventory(ServerPlayerEntity victim) {
        PlayerInventory inv = victim.getInventory();

        // Match vanilla: items with Curse of Vanishing are destroyed, not dropped.
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);
            if (!stack.isEmpty()
                    && EnchantmentHelper.hasAnyEnchantmentsWith(stack, EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP)) {
                inv.removeStack(i);
            }
        }

        // Drops main inventory, armor and offhand, and empties them, so the
        // keepInventory copy-on-respawn then copies an empty inventory.
        inv.dropAll();
    }
}
