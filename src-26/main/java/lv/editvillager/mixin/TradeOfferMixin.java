package lv.editvillager.mixin;

import lv.editvillager.EvTradeOfferExtension;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin для TradeOffer (MerchantOffer в Mojang mappings).
 * Добавляет флаг ежедневного обновления торга.
 */
@Mixin(MerchantOffer.class)
public class TradeOfferMixin implements EvTradeOfferExtension {

    @Unique
    private boolean ev$dailyRestock = false;

    @Override
    public void ev$setDailyRestock(boolean enabled) {
        this.ev$dailyRestock = enabled;
    }

    @Override
    public boolean ev$isDailyRestock() {
        return ev$dailyRestock;
    }

    @Inject(method = "copy", at = @At("RETURN"))
    private void ev$preserveDailyRestockOnCopy(CallbackInfoReturnable<MerchantOffer> cir) {
        MerchantOffer copy = cir.getReturnValue();
        if (copy instanceof EvTradeOfferExtension dst) {
            dst.ev$setDailyRestock(ev$dailyRestock);
        }
    }
}
