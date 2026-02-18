package lv.editvillager;

import net.minecraft.village.TradeOfferList;

public interface EvVillagerLock {


        void ev$setTradesLocked(boolean locked);

        boolean ev$areTradesLocked();


        void ev$setProfessionLocked(boolean locked);

        boolean ev$isProfessionLocked();

        void ev$setXpDropEnabled(boolean enabled);

        boolean ev$isXpDropEnabled();

        void ev$forceSetOffers(TradeOfferList offers);

        void ev$setAlwaysLookAtPlayer(boolean enable);

        boolean ev$shouldAlwaysLookAtPlayer();


        void ev$setPriceLock(boolean locked);

        boolean ev$isPriceLocked();


        void ev$setCustomLevelTrades(int level, TradeOfferList trades);

        TradeOfferList ev$getCustomLevelTrades(int level);

        void ev$setKeepTrades(boolean keep);

        boolean ev$shouldKeepTrades();
}
