package lv.editvillager;

import net.minecraft.world.item.trading.MerchantOffers;

/**
 * Интерфейс-расширение для Villager.
 * Предоставляет кастомные методы блокировки/разблокировки торгов,
 * профессий, а также хранения кастомных торгов по уровням.
 *
 * Примечание: в Mojang mappings (MC 26.1) TradeOfferList → MerchantOffers.
 */
public interface EvVillagerLock {

    void ev$setTradesLocked(boolean locked);
    boolean ev$areTradesLocked();

    void ev$setProfessionLocked(boolean locked);
    boolean ev$isProfessionLocked();

    void ev$setXpDropEnabled(boolean enabled);
    boolean ev$isXpDropEnabled();

    void ev$forceSetOffers(MerchantOffers offers);

    void ev$setAlwaysLookAtPlayer(boolean enable);
    boolean ev$shouldAlwaysLookAtPlayer();

    void ev$setPriceLock(boolean locked);
    boolean ev$isPriceLocked();

    void ev$setCustomLevelTrades(int level, MerchantOffers trades);
    MerchantOffers ev$getCustomLevelTrades(int level);

    void ev$setKeepTrades(boolean keep);
    boolean ev$shouldKeepTrades();

    void ev$rebuildOffersFromMenu(int upToLevel);

    void ev$syncCustomLevelTradesFromFlat(MerchantOffers flat);

    void ev$mergeOfferMetadataFrom(MerchantOffers metadataSource);

    void ev$setContinuousEffect(boolean enabled);
    boolean ev$hasContinuousEffect();

    void ev$setContinuousParticle(String particle);
    String ev$getContinuousParticle();

    int ev$getContinuousParticleCount();
    void ev$setContinuousParticleCount(int count);
}
