package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerWorld.class)
public class ServerWorldMixin {

    //? if neoforge {
    @Inject(method = "sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I", at = @At("HEAD"), cancellable = true)
    //?} else {
    @Inject(method = "spawnParticles(Lnet/minecraft/particle/ParticleEffect;DDDIDDDD)I", at = @At("HEAD"), cancellable = true)
    //?}
    private <T extends ParticleEffect> void ev$cancelDamageParticles(T particle, double x, double y, double z, int count, double deltaX, double deltaY, double deltaZ, double speed, CallbackInfoReturnable<Integer> cir) {
        if (particle.getType() == ParticleTypes.DAMAGE_INDICATOR) {
            ServerWorld world = (ServerWorld) (Object) this;
            List<VillagerEntity> villagers = world.getEntitiesByClass(VillagerEntity.class, new Box(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5), v -> true);
            for (VillagerEntity villager : villagers) {
                EvVillagerLock lock = (EvVillagerLock) villager;
                if (!lock.ev$hasEffectsOnActions()) {
                    cir.setReturnValue(0);
                    return;
                }
            }
        }
    }
}
