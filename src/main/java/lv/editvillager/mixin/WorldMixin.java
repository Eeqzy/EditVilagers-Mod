package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(World.class)
public abstract class WorldMixin {

    //? if neoforge {
    @Inject(method = "broadcastEntityEvent", at = @At("HEAD"), cancellable = true, require = 0)
    //?} else {
    @Inject(method = "sendEntityStatus", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void ev$cancelVillagerParticles(Entity entity, byte status, CallbackInfo ci) {
        if (entity instanceof VillagerEntity villager) {
            EvVillagerLock lock = (EvVillagerLock) villager;
            
            if (status == 12 || status == 13 || status == 14) {
                if (!lock.ev$hasEffectsOnActions()) {
                    ci.cancel();
                } else {
                    String customParticle = lock.ev$getActionParticle();
                    if (!"default".equals(customParticle)) {
                        ci.cancel();
                        
                        if (!entity.getEntityWorld().isClient()) {
                            try {
                                net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(customParticle);
                                if (id != null) {
                                    net.minecraft.particle.ParticleType<?> type = net.minecraft.registry.Registries.PARTICLE_TYPE.get(id);
                                    if (type instanceof net.minecraft.particle.ParticleEffect effect) {
                                        net.minecraft.server.world.ServerWorld sw = (net.minecraft.server.world.ServerWorld) entity.getEntityWorld();
                                        for (int i = 0; i < 5; i++) {
                                            double d = entity.getRandom().nextGaussian() * 0.02D;
                                            double e = entity.getRandom().nextGaussian() * 0.02D;
                                            double f = entity.getRandom().nextGaussian() * 0.02D;
                                            sw.spawnParticles(effect, 
                                                entity.getParticleX(1.0D), 
                                                entity.getRandomBodyY() + 1.0D, 
                                                entity.getParticleZ(1.0D), 
                                                1, d, e, f, 0.01);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
            }
        }
    }
}
