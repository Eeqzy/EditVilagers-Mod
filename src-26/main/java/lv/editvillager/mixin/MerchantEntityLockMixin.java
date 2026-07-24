package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin для AbstractVillager (MerchantEntity в Yarn mappings).
 * Резервный файл — основная логика в VillagerLockMixin.
 *
 * В 26.1 (Mojang): MerchantEntity → AbstractVillager.
 */
@Mixin(AbstractVillager.class)
public class MerchantEntityLockMixin {
    // Зарезервировано для будущих перехватов торговой логики
}
