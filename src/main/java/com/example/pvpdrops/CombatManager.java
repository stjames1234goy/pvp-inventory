package com.example.pvpdrops;

import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/** Tracks which players are in "combat mode" and drives the boss bar + ding sound. Server thread only. */
public final class CombatManager {
    private static final String DEFAULT_SOUND = "minecraft:entity.arrow.hit_player";

    private static final class State {
        long expiryTick;
        long totalTicks;
        long lastDingTick = Long.MIN_VALUE / 2;
        int shownSeconds = -1;
        final ServerBossBar bar;

        State(ServerBossBar bar) {
            this.bar = bar;
        }
    }

    private static final Map<UUID, State> STATES = new HashMap<>();

    private CombatManager() {
    }

    /** Puts the player in combat mode, or resets the timer if they already are. */
    public static void tag(ServerPlayerEntity player, long nowTick, PvpDropsConfig cfg) {
        long ticks = Math.round(cfg.combatDurationSeconds * 20.0);
        if (ticks <= 0) {
            return;
        }

        State state = STATES.get(player.getUuid());
        if (state == null) {
            ServerBossBar bar = new ServerBossBar(
                    title((int) Math.ceil(ticks / 20.0)),
                    BossBar.Color.byName(cfg.bossBarColor),
                    BossBar.Style.byName(cfg.bossBarStyle));
            state = new State(bar);
            STATES.put(player.getUuid(), state);
            if (cfg.showBossBar) {
                bar.addPlayer(player);
            }
        }
        state.expiryTick = nowTick + ticks;
        state.totalTicks = ticks;
    }

    public static boolean isInCombat(ServerPlayerEntity player, long nowTick) {
        State state = STATES.get(player.getUuid());
        return state != null && state.expiryTick > nowTick;
    }

    /** Removes combat mode and its visuals (used on death). */
    public static void clear(ServerPlayerEntity player) {
        State state = STATES.remove(player.getUuid());
        if (state != null) {
            state.bar.clearPlayers();
        }
    }

    /** Called once per server tick. */
    public static void tick(MinecraftServer server) {
        if (STATES.isEmpty()) {
            return;
        }
        PvpDropsConfig cfg = PvpDropsMod.config;
        long now = server.getTicks();

        Iterator<Map.Entry<UUID, State>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, State> entry = it.next();
            State state = entry.getValue();
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
            long remaining = state.expiryTick - now;

            // Combat over (or the player left / died): remove all visuals and sounds.
            if (player == null || player.isDead() || remaining <= 0) {
                state.bar.clearPlayers();
                it.remove();
                continue;
            }

            int seconds = (int) Math.ceil(remaining / 20.0);
            state.bar.setPercent(Math.max(0.0F, Math.min(1.0F, remaining / (float) state.totalTicks)));
            if (seconds != state.shownSeconds) {
                state.bar.setName(title(seconds));
                state.shownSeconds = seconds;
            }

            // One ding per second (the 15-tick gap stops rapid re-hits from spamming it).
            if (cfg.playSound && remaining % 20 == 0 && now - state.lastDingTick >= 15) {
                ding(player, cfg);
                state.lastDingTick = now;
            }
        }
    }

    private static void ding(ServerPlayerEntity player, PvpDropsConfig cfg) {
        Identifier id = Identifier.tryParse(cfg.soundId);
        if (id == null) {
            id = Identifier.tryParse(DEFAULT_SOUND);
        }
        player.playSoundToPlayer(SoundEvent.of(id), SoundCategory.PLAYERS, cfg.soundVolume, cfg.soundPitch);
    }

    /** "COMBAT MODE" in white, the number in red. */
    private static Text title(int seconds) {
        return Text.literal("COMBAT MODE ").formatted(Formatting.WHITE, Formatting.BOLD)
                .append(Text.literal(String.valueOf(seconds)).formatted(Formatting.RED, Formatting.BOLD));
    }
}
