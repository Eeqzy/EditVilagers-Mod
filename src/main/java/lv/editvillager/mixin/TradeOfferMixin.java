package lv.editvillager.mixin;

import lv.editvillager.EvTradeOfferExtension;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.village.TradeOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TradeOffer.class)
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


}
