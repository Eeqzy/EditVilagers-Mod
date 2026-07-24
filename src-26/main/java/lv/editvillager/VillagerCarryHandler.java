package lv.editvillager;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerCarryHandler {
    private static final Map<UUID, UUID> carryingMap = new HashMap<>();

    public static void startCarrying(ServerPlayer player, Villager villager) {
        if (carryingMap.containsKey(player.getUUID())) {
            stopCarrying(player);
        }

        carryingMap.put(player.getUUID(), villager.getUUID());
        LanguageManager.bind(player);
        player.sendSystemMessage(Component.literal(LanguageManager.tr("carry.picked_up")), true);

        villager.setNoGravity(true);
    }

    public static void stopCarrying(ServerPlayer player) {
        UUID villagerId = carryingMap.remove(player.getUUID());
        if (villagerId != null) {
            Entity e = ((ServerLevel) player.level()).getEntity(villagerId);
            if (e instanceof Villager v) {
                v.setNoGravity(false);
            }
            LanguageManager.bind(player);
            player.sendSystemMessage(Component.literal(LanguageManager.tr("carry.released")), true);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        for (UUID playerId : new HashMap<>(carryingMap).keySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null) {
                carryingMap.remove(playerId);
                continue;
            }

            UUID villagerId = carryingMap.get(playerId);
            Entity entity = ((ServerLevel) player.level()).getEntity(villagerId);

            if (entity == null || !entity.isAlive() || !(entity instanceof Villager villager)) {
                carryingMap.remove(playerId);
                continue;
            }

            if (player.isShiftKeyDown()) {
                stopCarrying(player);
                continue;
            }

            Vec3 look = player.getViewVector(1.0f);
            Vec3 eyePos = player.getEyePosition();

            double dist = 2.5;
            Vec3 target = eyePos.add(look.scale(dist));

            target = target.subtract(0, 1.0, 0);

            villager.absSnapTo(target.x, target.y, target.z, player.getYRot() + 180.0f, 0.0f);
            villager.setYHeadRot(player.getYRot() + 180.0f);
            villager.setYBodyRot(player.getYRot() + 180.0f);

            villager.setDeltaMovement(Vec3.ZERO);
            villager.fallDistance = 0;
        }
    }
}
