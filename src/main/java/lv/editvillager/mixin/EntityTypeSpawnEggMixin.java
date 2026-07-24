package lv.editvillager.mixin;

import lv.editvillager.VillagerCloneHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Clone eggs carry ENTITY_DATA (type) + CUSTOM_DATA.ev_villager.
 * After vanilla spawns the villager, apply clone fields safely.
 */
@Mixin(EntityType.class)
public abstract class EntityTypeSpawnEggMixin {

    //? if neoforge {
    //? if 1.21.1 {
    @Inject(
            method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            require = 0)
    //?} else {
    @Inject(
            method = "spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/EntitySpawnReason;ZZ)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            require = 0)
    //?}
    //?} else {
    @Inject(method = "spawnFromItemStack", at = @At("RETURN"), require = 0)
    //?}
    private void ev$reapplyCloneData(
            ServerWorld world,
            ItemStack stack,
            //? if 1.21.1 {
            net.minecraft.entity.player.PlayerEntity spawner,
            //?} else {
            net.minecraft.entity.LivingEntity spawner,
            //?}
            BlockPos pos,
            SpawnReason spawnReason,
            boolean alignPosition,
            boolean invertY,
            CallbackInfoReturnable<Entity> cir) {
        Entity spawned = cir.getReturnValue();
        if (!(spawned instanceof VillagerEntity villager) || stack == null || stack.isEmpty()) {
            return;
        }
        NbtCompound data = VillagerCloneHelper.readEggCloneData(stack);
        if (data == null) {
            return;
        }
        try {
            VillagerCloneHelper.applySafeCloneData(villager, data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
