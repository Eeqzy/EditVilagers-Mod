package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityLockMixin {

    @Inject(method = "getExperienceToDrop", at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$noXp_getXpToDrop(net.minecraft.server.world.ServerWorld world, net.minecraft.entity.Entity attacker,
            CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof VillagerEntity villager) {
            if (!((EvVillagerLock) villager).ev$isXpDropEnabled()) {
                cir.setReturnValue(0);
            }
        }
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void ev$tickNoAiLook(CallbackInfo ci) {
        if ((Object) this instanceof VillagerEntity villager) {
            if (((EvVillagerLock) villager).ev$shouldAlwaysLookAtPlayer() && villager.isAiDisabled()) {
                net.minecraft.entity.player.PlayerEntity p = villager.getEntityWorld().getClosestPlayer(villager, 10.0);
                if (p != null) {
                    double d = p.getX() - villager.getX();
                    double e = p.getEyeY() - villager.getEyeY();
                    double f = p.getZ() - villager.getZ();
                    double g = Math.sqrt(d * d + f * f);
                    float pitch = (float) (-(Math.atan2(e, g) * 57.2957763671875));
                    float yaw = (float) (Math.atan2(f, d) * 57.2957763671875) - 90.0F;
                    villager.setYaw(yaw);
                    villager.setHeadYaw(yaw);
                    villager.setPitch(pitch);
                    villager.setBodyYaw(yaw);
                }
            }
        }
    }
}
