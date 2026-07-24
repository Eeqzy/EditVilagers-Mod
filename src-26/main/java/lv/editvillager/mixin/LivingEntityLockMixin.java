package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для LivingEntity.
 * Mojang mappings (MC 26.1):
 *  - getExperienceToDrop → getExperienceReward (или аналог)
 *  - getEntityWorld → level()
 *  - getLookControl → getLookControl (без изменений)
 *  - isAiDisabled → isNoAi
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityLockMixin {

    /**
     * Блокировка дропа XP для кастомных жителей.
     * В 26.1 (Mojang): getExperienceReward(ServerLevel, Entity)
     * или getBaseExperienceReward() — зависит от версии.
     * Используем require=0 для безопасности.
     */
    @Inject(method = "getExperienceReward",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$noXp_getExperienceReward(
            net.minecraft.server.level.ServerLevel world,
            Entity attacker,
            CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Villager villager) {
            if (!((EvVillagerLock) villager).ev$isXpDropEnabled()) {
                cir.setReturnValue(0);
            }
        }
    }

    /**
     * Всегда смотреть на игрока — обработка в tick.
     * В 26.1 (Mojang): isNoAi() вместо isAiDisabled().
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void ev$tickNoAiLook(CallbackInfo ci) {
        if ((Object) this instanceof Villager villager) {
            if (((EvVillagerLock) villager).ev$shouldAlwaysLookAtPlayer() && villager.isNoAi()) {
                net.minecraft.world.entity.player.Player p =
                        villager.level().getNearestPlayer(villager, 10.0);
                if (p != null) {
                    double d = p.getX() - villager.getX();
                    double e = p.getEyeY() - villager.getEyeY();
                    double f = p.getZ() - villager.getZ();
                    double g = Math.sqrt(d * d + f * f);
                    float pitch = (float) (-(Math.atan2(e, g) * 57.2957763671875));
                    float yaw = (float) (Math.atan2(f, d) * 57.2957763671875) - 90.0F;
                    villager.setYRot(yaw);
                    villager.setYHeadRot(yaw);
                    villager.setXRot(pitch);
                    villager.setYBodyRot(yaw);
                }
            }
        }
    }
}
