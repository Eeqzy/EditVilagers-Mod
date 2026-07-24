package lv.editvillager;

import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VillagerCarryHandler {
    private static final Map<UUID, UUID> carryingMap = new HashMap<>();

    public static void startCarrying(ServerPlayerEntity player, VillagerEntity villager) {
        if (carryingMap.containsKey(player.getUuid())) {
            stopCarrying(player);
        }

        carryingMap.put(player.getUuid(), villager.getUuid());
        player.sendMessage(Text.literal("§aВы взяли жителя. Нажмите Shift чтобы отпустить."), true);

        villager.setNoGravity(true);
    }

    public static void stopCarrying(ServerPlayerEntity player) {
        UUID villagerId = carryingMap.remove(player.getUuid());
        if (villagerId != null) {
            Entity e = ((ServerWorld) player.getEntityWorld()).getEntity(villagerId);
            if (e instanceof VillagerEntity v) {
                v.setNoGravity(false);
            }
            player.sendMessage(Text.literal("§eЖитель отпущен."), true);
        }
    }

    public static void onServerTick(MinecraftServer server) {
        for (UUID playerId : new HashMap<>(carryingMap).keySet()) {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
            if (player == null) {
                carryingMap.remove(playerId);
                continue;
            }

            UUID villagerId = carryingMap.get(playerId);
            Entity entity = ((ServerWorld) player.getEntityWorld()).getEntity(villagerId);

            if (entity == null || !entity.isAlive() || !(entity instanceof VillagerEntity villager)) {
                carryingMap.remove(playerId);
                continue;
            }

            if (player.isSneaking()) {
                stopCarrying(player);
                continue;
            }

            Vec3d look = player.getRotationVec(1.0f);
            Vec3d eyePos = player.getEyePos();

            double dist = 2.5;
            Vec3d target = eyePos.add(look.multiply(dist));

            target = target.subtract(0, 1.0, 0);

            villager.refreshPositionAndAngles(target.x, target.y, target.z, player.getYaw() + 180.0f, 0.0f);
            villager.setHeadYaw(player.getYaw() + 180.0f);
            villager.setBodyYaw(player.getYaw() + 180.0f);

            villager.setVelocity(Vec3d.ZERO);
            villager.fallDistance = 0;
        }
    }
}
