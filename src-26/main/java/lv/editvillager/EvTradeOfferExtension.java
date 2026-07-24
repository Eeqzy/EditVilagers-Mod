package lv.editvillager;

/**
 * Интерфейс-расширение для MerchantOffer (TradeOffer в Yarn mappings).
 * Добавляет флаг ежедневного обновления торга.
 */
public interface EvTradeOfferExtension {
    void ev$setDailyRestock(boolean enabled);
    boolean ev$isDailyRestock();
}
