package lv.editvillager.mixin;

import lv.editvillager.EvTradeOfferExtension;
import lv.editvillager.EvVillagerLock;
import lv.editvillager.LanguageManager;
import lv.editvillager.ReflectionUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Основной Mixin для Villager.
 * Реализует EvVillagerLock — кастомное хранение торгов, блокировки, NBT-сохранение.
 *
 * Mojang mappings (MC 26.1):
 *  - TradeOfferList → MerchantOffers
 *  - TradeOffer → MerchantOffer
 *  - writeCustomDataToNbt(CompoundTag) — без WrapperLookup!
 *  - readCustomDataFromNbt(CompoundTag) — без WrapperLookup!
 *  - levelUp() → increaseMerchantCareer() или levelUp(ServerLevel)
 *  - canLevelUp() → canRestock()
 *  - fillRecipes() — может быть переименован/удалён
 *  - setOffers() → setTradingOffers() или остался setOffers()
 *  - getOffers() → getOffers() (обычно не меняется)
 *  - prepareOffersFor() → updateTrades() или остался
 */
@Mixin(Villager.class)
public abstract class VillagerEntityLockMixin implements EvVillagerLock {

    // ─── Shadow-методы (Mojang mappings 26.1) ───────────────────────────────

    @Shadow
    protected abstract boolean shouldIncreaseLevel();

    @Shadow
    protected abstract void increaseMerchantCareer(net.minecraft.server.level.ServerLevel level);

    // ─── Кастомные поля ─────────────────────────────────────────────────────

    @Unique private boolean ev$tradesLocked = true;
    @Unique private boolean ev$professionLocked = false;
    @Unique private boolean ev$xpDropEnabled = true;
    @Unique private boolean ev$bypassOffersLock = false;
    @Unique private MerchantOffers ev$offersBeforeLevelUp = null;
    @Unique private boolean ev$alwaysLookAtPlayer = false;
    @Unique private boolean ev$priceLocked = false;
    @Unique private boolean ev$keepTrades = false;
    @Unique private long ev$lastRestockDay = -1;
    @Unique private boolean ev$continuousEffect = false;
    @Unique private String ev$continuousParticle = "default";
    @Unique private int ev$continuousParticleCount = 5;

    @Unique
    private final Map<Integer, MerchantOffers> ev$customLevelTrades = new HashMap<>();

    // ─── Реализация EvVillagerLock ───────────────────────────────────────────

    @Override public void ev$setTradesLocked(boolean locked) { this.ev$tradesLocked = locked; }
    @Override public boolean ev$areTradesLocked() { return ev$tradesLocked; }

    @Override public void ev$setProfessionLocked(boolean locked) { this.ev$professionLocked = locked; }
    @Override public boolean ev$isProfessionLocked() { return ev$professionLocked; }

    @Override public void ev$setAlwaysLookAtPlayer(boolean enable) { this.ev$alwaysLookAtPlayer = enable; }
    @Override public boolean ev$shouldAlwaysLookAtPlayer() { return ev$alwaysLookAtPlayer; }

    @Override public void ev$setPriceLock(boolean locked) { this.ev$priceLocked = locked; }
    @Override public boolean ev$isPriceLocked() { return ev$priceLocked; }

    @Override public void ev$setKeepTrades(boolean keep) { this.ev$keepTrades = keep; }
    @Override public boolean ev$shouldKeepTrades() { return ev$keepTrades; }

    @Override public void ev$setXpDropEnabled(boolean enabled) { this.ev$xpDropEnabled = enabled; }
    @Override public boolean ev$isXpDropEnabled() { return ev$xpDropEnabled; }

    @Override public void ev$setContinuousEffect(boolean enabled) { this.ev$continuousEffect = enabled; }
    @Override public boolean ev$hasContinuousEffect() { return this.ev$continuousEffect; }

    @Override public void ev$setContinuousParticle(String particle) { this.ev$continuousParticle = particle; }
    @Override public String ev$getContinuousParticle() { return this.ev$continuousParticle; }

    @Override public void ev$setContinuousParticleCount(int count) { this.ev$continuousParticleCount = count; }
    @Override public int ev$getContinuousParticleCount() { return this.ev$continuousParticleCount; }

    @Override
    public void ev$forceSetOffers(MerchantOffers offers) {
        ev$bypassOffersLock = true;
        try {
            ((Villager) (Object) this).setOffers(offers);
        } finally {
            ev$bypassOffersLock = false;
        }
    }

    @Override
    public void ev$setCustomLevelTrades(int level, MerchantOffers trades) {
        if (trades == null || trades.isEmpty()) {
            ev$customLevelTrades.remove(level);
        } else {
            ev$customLevelTrades.put(level, deepCopyOffers(trades));
        }
    }

    @Override
    public MerchantOffers ev$getCustomLevelTrades(int level) {
        if (ev$customLevelTrades.containsKey(level)) {
            return deepCopyOffers(ev$customLevelTrades.get(level));
        }
        return new MerchantOffers();
    }

    @Override
    public void ev$rebuildOffersFromMenu(int upToLevel) {
        MerchantOffers combined = new MerchantOffers();
        for (int menuLevel = 1; menuLevel <= upToLevel; menuLevel++) {
            MerchantOffers tierTrades = ev$customLevelTrades.get(menuLevel);
            if (tierTrades == null) {
                continue;
            }
            for (MerchantOffer offer : tierTrades) {
                combined.add(offer.copy());
            }
        }
        ev$forceSetOffers(combined);
    }

    @Override
    public void ev$syncCustomLevelTradesFromFlat(MerchantOffers flat) {
        Villager self = (Villager) (Object) this;
        ev$syncCustomLevelTradesFromFlatInternal(flat, self.getVillagerData().level());
    }

    @Unique
    private void ev$syncCustomLevelTradesFromFlatInternal(MerchantOffers flat, int upToLevel) {
        if (flat == null || flat.isEmpty()) {
            return;
        }
        int idx = 0;
        for (int level = 1; level <= upToLevel; level++) {
            MerchantOffers tier = ev$customLevelTrades.get(level);
            if (tier == null || tier.isEmpty()) {
                continue;
            }
            MerchantOffers updated = new MerchantOffers();
            for (int i = 0; i < tier.size(); i++) {
                if (idx < flat.size()) {
                    updated.add(flat.get(idx++));
                } else {
                    // Flat list ended early (e.g. switched to an empty editor level) —
                    // keep the remaining original offers instead of wiping the tier.
                    updated.add(tier.get(i).copy());
                }
            }
            ev$customLevelTrades.put(level, deepCopyOffers(updated));
        }
    }

    @Override
    public void ev$mergeOfferMetadataFrom(MerchantOffers metadataSource) {
        Villager self = (Villager) (Object) this;
        MerchantOffers current = self.getOffers();
        ev$mergeOfferListsByIndex(current, metadataSource);
        ev$forceSetOffers(current);
    }

    @Unique
    private static void ev$mergeOfferListsByIndex(MerchantOffers target, MerchantOffers metadataSource) {
        for (int i = 0; i < target.size() && i < metadataSource.size(); i++) {
            target.set(i, ev$copyOfferWithMetadata(target.get(i), metadataSource.get(i)));
        }
    }

    @Unique
    private static MerchantOffer ev$copyOfferWithMetadata(MerchantOffer base, MerchantOffer metadata) {
        MerchantOffer result = new MerchantOffer(
                base.getItemCostA(),
                base.getItemCostB(),
                base.getResult(),
                metadata.getUses(),
                metadata.getMaxUses(),
                base.getXp(),
                base.getPriceMultiplier(),
                base.getDemand());
        if (result instanceof EvTradeOfferExtension ext && metadata instanceof EvTradeOfferExtension metaExt) {
            ext.ev$setDailyRestock(metaExt.ev$isDailyRestock());
        }
        return result;
    }

    @Unique
    private boolean ev$hasAnyCustomLevelTrades() {
        for (MerchantOffers trades : ev$customLevelTrades.values()) {
            if (trades != null && !trades.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    // ─── Блокировка setTradingOffers ─────────────────────────────────────────

    /**
     * Ваниль запрещает торговлю с малышами (isBaby → unhappy).
     * Для EditVillagers разрешаем: маленький житель с торгами открывает меню.
     */
    @Redirect(
            method = "mobInteract(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;isBaby()Z"))
    private boolean ev$allowTradingWithBaby(Villager self) {
        return false;
    }

    /**
     * Блокирует замену торгов, если ev$tradesLocked = true.
     * В 26.1 (Mojang mappings): метод называется setTradingOffers.
     */
    @Inject(method = "setTradingOffers(Lnet/minecraft/world/item/trading/MerchantOffers;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$blockSetTradingOffers(MerchantOffers offers, CallbackInfo ci) {
        if (ev$tradesLocked && !ev$bypassOffersLock) {
            ci.cancel();
        }
    }

    // ─── Перехват levelUp ──────────────────────────────────────────────────

    /**
     * Сохраняем торги перед повышением уровня.
     * В 26.1 метод может называться increaseMerchantCareer.
     */
    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), require = 0)
    private void ev$saveOffersBeforeLevelUp(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        ev$offersBeforeLevelUp = deepCopyOffers(self.getOffers());
    }

    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), require = 0)
    private void ev$restoreOffersAfterLevelUp(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        ev$handleLevelUpOffers();
    }

    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), require = 0)
    private void ev$syncClientAfterLevelUp(net.minecraft.server.level.ServerLevel level, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        if (!self.level().isClientSide()) {
            self.setVillagerData(self.getVillagerData());
        }
    }

    /**
     * Логика восстановления торгов после level-up.
     */
    @Unique
    private void ev$handleLevelUpOffers() {
        Villager self = (Villager) (Object) this;
        int currentLevel = self.getVillagerData().level();

        if (!ev$tradesLocked && ev$hasAnyCustomLevelTrades()) {
            ev$rebuildOffersFromMenu(currentLevel);
            if (ev$offersBeforeLevelUp != null) {
                MerchantOffers merged = self.getOffers();
                ev$mergeOfferListsByIndex(merged, ev$offersBeforeLevelUp);
                ev$forceSetOffers(merged);
                ev$syncCustomLevelTradesFromFlatInternal(merged, currentLevel);
            }
            self.setVillagerData(self.getVillagerData());
            ev$offersBeforeLevelUp = null;
            return;
        }

        MerchantOffers currentOffers = self.getOffers();

        // Вычисляем новые ванильные торги (добавленные за этот уровень)
        MerchantOffers newVanillaTrades = new MerchantOffers();
        int oldSize = (ev$offersBeforeLevelUp != null) ? ev$offersBeforeLevelUp.size() : 0;
        if (currentOffers.size() > oldSize) {
            for (int i = oldSize; i < currentOffers.size(); i++) {
                newVanillaTrades.add(currentOffers.get(i));
            }
        }

        boolean hasCustomForThisLevel = ev$customLevelTrades.containsKey(currentLevel) &&
                ev$customLevelTrades.get(currentLevel) != null &&
                !ev$customLevelTrades.get(currentLevel).isEmpty();

        boolean isCustomVillager = !ev$customLevelTrades.isEmpty();

        MerchantOffers tradesToAdd = new MerchantOffers();
        if (hasCustomForThisLevel) {
            tradesToAdd.addAll(deepCopyOffers(ev$customLevelTrades.get(currentLevel)));
        } else if (!isCustomVillager && !ev$tradesLocked) {
            tradesToAdd.addAll(newVanillaTrades);
        }

        MerchantOffers finalOffers = new MerchantOffers();

        boolean shouldKeepHistory = ev$keepTrades;
        if (!isCustomVillager && !ev$tradesLocked) shouldKeepHistory = true;
        if (isCustomVillager && !hasCustomForThisLevel) shouldKeepHistory = true;

        if (shouldKeepHistory && ev$offersBeforeLevelUp != null) {
            finalOffers.addAll(deepCopyOffers(ev$offersBeforeLevelUp));
        }
        finalOffers.addAll(tradesToAdd);

        ev$forceSetOffers(finalOffers);
        ev$offersBeforeLevelUp = null;
    }

    // ─── Блокировка изменения VillagerData ───────────────────────────────────

    @Inject(method = "setVillagerData",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$blockSetVillagerData(VillagerData newData, CallbackInfo ci) {
        Villager self = (Villager) (Object) this;
        VillagerData currentData = self.getVillagerData();

        if (currentData == null) return;

        int currentLevel = currentData.level();
        int newLevel = newData.level();

        if (newLevel > currentLevel) return; // Позволяем повышение уровня

        if (ev$professionLocked) {
            Object newProf = ReflectionUtils.getEntryValue(ReflectionUtils.getProfession(newData));
            Object currentProf = ReflectionUtils.getEntryValue(ReflectionUtils.getProfession(currentData));
            if (newProf != null && currentProf != null && newProf != currentProf) {
                ci.cancel();
            }
        }
    }

    // ─── Блокировка изменения цен ─────────────────────────────────────────────

    /**
     * В 26.1 (Mojang): updateSpecialPrices() отвечает за цены от сплетен (удары, лечение и т.д.).
     */
    @Inject(method = "updateSpecialPrices(Lnet/minecraft/world/entity/player/Player;)V", at = @At("TAIL"), require = 0)
    private void ev$blockPriceChangeOnOpen(net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (ev$priceLocked) {
            Villager self = (Villager) (Object) this;
            for (MerchantOffer offer : self.getOffers()) {
                offer.resetSpecialPriceDiff();
            }
        }
    }

    // ─── Tick ─────────────────────────────────────────────────────────────────

    @Inject(method = "tick", at = @At("TAIL"))
    private void ev$tick(CallbackInfo ci) {
        Villager self = (Villager) (Object) this;

        // Всегда смотреть на игрока
        if (ev$alwaysLookAtPlayer) {
            net.minecraft.world.entity.player.Player p = self.level().getNearestPlayer(self, 8.0);
            if (p != null) {
                if (self.isNoAi()) {
                    double d = p.getX() - self.getX();
                    double e = p.getEyeY() - self.getEyeY();
                    double f = p.getZ() - self.getZ();
                    float yaw = (float) (Math.atan2(f, d) * 57.2957763671875) - 90.0F;
                    self.setYRot(yaw);
                    self.setYHeadRot(yaw);
                    self.setYBodyRot(yaw);
                } else {
                    self.getLookControl().setLookAt(p, 30.0f, 30.0f);
                }
            }
        }

        if (self.level() instanceof net.minecraft.server.level.ServerLevel) {

            if (!ev$tradesLocked && this.shouldIncreaseLevel() && self.isNoAi()) {
                int level = self.getVillagerData().level();
                if (VillagerData.getMaxXpPerLevel(level) <= self.getVillagerXp()) {
                    this.increaseMerchantCareer((net.minecraft.server.level.ServerLevel) self.level());
                }
            }

            // Ежедневное обновление торгов
            long worldTime = ReflectionUtils.getWorldDayTime(self.level());
            long currentDay = worldTime / 24000L;

            if (ev$lastRestockDay != currentDay && ReflectionUtils.isBedNearby(self)) {
                MerchantOffers offers = self.getOffers();
                int restockedCount = 0;
                boolean hasRestockTrades = false;
                for (MerchantOffer offer : offers) {
                    if (offer instanceof lv.editvillager.EvTradeOfferExtension ext
                            && ext.ev$isDailyRestock()) {
                        hasRestockTrades = true;
                        offer.resetUses();
                        restockedCount++;
                    }
                }
                if (restockedCount > 0) {
                    ev$lastRestockDay = currentDay;
                    self.level().broadcastEntityEvent(self, (byte) 14);
                    self.playSound(net.minecraft.sounds.SoundEvents.VILLAGER_YES, 1.0f, self.getVoicePitch());
                    self.level().players().forEach(p -> {
                        LanguageManager.bind(p);
                        p.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                                LanguageManager.tr("msg.trades_restocked")));
                    });
                } else if (!hasRestockTrades) {
                    ev$lastRestockDay = currentDay;
                }
            }

            // Непрерывные частицы
            if (ev$continuousEffect && !"default".equals(ev$continuousParticle)) {
                if (self.tickCount % 10 == 0) {
                    try {
                        net.minecraft.resources.Identifier id =
                                net.minecraft.resources.Identifier.tryParse(ev$continuousParticle);
                        if (id != null) {
                            net.minecraft.core.particles.ParticleType<?> type =
                                    net.minecraft.core.registries.BuiltInRegistries.PARTICLE_TYPE.getValue(id);
                            if (type instanceof net.minecraft.core.particles.ParticleOptions effect) {
                                ((net.minecraft.server.level.ServerLevel) self.level()).sendParticles(
                                        effect,
                                        self.getRandomX(1.0D),
                                        self.getRandomY(),
                                        self.getRandomZ(1.0D),
                                        ev$continuousParticleCount,
                                        0.3, 0.3, 0.3, 0.01);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    // ─── NBT через ValueOutput / ValueInput (MC 26.x) ───────────────────────

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            at = @At("TAIL"))
    private void ev$addAdditionalSaveData(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("EvTradesLocked", ev$tradesLocked);
        output.putBoolean("EvProfessionLocked", ev$professionLocked);
        output.putBoolean("EvXpDropEnabled", ev$xpDropEnabled);
        output.putBoolean("EvAlwaysLookAtPlayer", ev$alwaysLookAtPlayer);
        output.putBoolean("EvPriceLocked", ev$priceLocked);
        output.putBoolean("EvKeepTrades", ev$keepTrades);
        output.putLong("EvLastRestockDay", ev$lastRestockDay);
        output.putBoolean("EvContinuousEffect", ev$continuousEffect);
        output.putString("EvContinuousParticle", ev$continuousParticle);
        output.putInt("EvContinuousParticleCount", ev$continuousParticleCount);

        Villager self = (Villager) (Object) this;
        MerchantOffers offers = self.getOffers();
        long[] restockBits = new long[offers.size() / 64 + 1];
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i) instanceof EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                restockBits[i / 64] |= (1L << (i % 64));
            }
        }
        CompoundTag restockTag = new CompoundTag();
        restockTag.putLongArray("Bits", restockBits);
        output.store("EvRestockFlags", CompoundTag.CODEC, restockTag);

        CompoundTag customTradesNbt = new CompoundTag();
        ev$customLevelTrades.forEach((level, trades) -> {
            CompoundTag levelNbt = new CompoundTag();
            for (int i = 0; i < trades.size(); i++) {
                final int tradeIndex = i;
                MerchantOffer offer = trades.get(i);
                MerchantOffer.CODEC.encodeStart(NbtOps.INSTANCE, offer)
                        .resultOrPartial(err -> System.err.println("Ev: Failed to encode trade: " + err))
                        .ifPresent(encoded -> levelNbt.put("Trade" + tradeIndex, encoded));
            }
            customTradesNbt.put(String.valueOf(level), levelNbt);
        });
        output.store("EvCustomLevelTrades", CompoundTag.CODEC, customTradesNbt);
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At("HEAD"))
    private void ev$readAdditionalSaveDataHead(ValueInput input, CallbackInfo ci) {
        ev$bypassOffersLock = true;
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V",
            at = @At("TAIL"))
    private void ev$readAdditionalSaveData(ValueInput input, CallbackInfo ci) {
        try {
            ev$tradesLocked = input.getBooleanOr("EvTradesLocked", true);
            ev$professionLocked = input.getBooleanOr("EvProfessionLocked", false);
            ev$xpDropEnabled = input.getBooleanOr("EvXpDropEnabled", true);
            ev$alwaysLookAtPlayer = input.getBooleanOr("EvAlwaysLookAtPlayer", false);
            ev$priceLocked = input.getBooleanOr("EvPriceLocked", false);
            ev$keepTrades = input.getBooleanOr("EvKeepTrades", false);
            ev$lastRestockDay = input.getLongOr("EvLastRestockDay", -1L);
            ev$continuousEffect = input.getBooleanOr("EvContinuousEffect", false);
            ev$continuousParticle = input.getStringOr("EvContinuousParticle", "default");
            ev$continuousParticleCount = input.getIntOr("EvContinuousParticleCount", 5);

            input.read("EvRestockFlags", CompoundTag.CODEC).ifPresent(restockTag -> {
                long[] restockBits = restockTag.getLongArray("Bits").orElse(new long[0]);
                Villager self = (Villager) (Object) this;
                MerchantOffers offers = self.getOffers();
                for (int i = 0; i < offers.size(); i++) {
                    if (i / 64 < restockBits.length
                            && (restockBits[i / 64] & (1L << (i % 64))) != 0
                            && offers.get(i) instanceof EvTradeOfferExtension ext) {
                        ext.ev$setDailyRestock(true);
                    }
                }
            });

            input.read("EvCustomLevelTrades", CompoundTag.CODEC).ifPresent(customTradesNbt -> {
                ev$customLevelTrades.clear();
                for (String levelKey : customTradesNbt.keySet()) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        CompoundTag levelNbt = customTradesNbt.getCompound(levelKey).orElse(new CompoundTag());
                        Map<Integer, MerchantOffer> sortedTrades = new TreeMap<>();
                        for (String tradeKey : levelNbt.keySet()) {
                            if (tradeKey.startsWith("Trade")) {
                                try {
                                    int index = Integer.parseInt(tradeKey.substring(5));
                                    CompoundTag tradeNbt = levelNbt.getCompound(tradeKey).orElse(new CompoundTag());
                                    MerchantOffer.CODEC.parse(NbtOps.INSTANCE, tradeNbt)
                                            .resultOrPartial(err -> System.err.println("Ev: Failed to parse trade: " + err))
                                            .ifPresent(o -> sortedTrades.put(index, o));
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        MerchantOffers trades = new MerchantOffers();
                        trades.addAll(sortedTrades.values());
                        ev$customLevelTrades.put(level, trades);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        } finally {
            ev$bypassOffersLock = false;
        }
    }

    // ─── Вспомогательные методы ───────────────────────────────────────────────

    @Unique
    private static MerchantOffers deepCopyOffers(MerchantOffers offers) {
        MerchantOffers copy = new MerchantOffers();
        for (MerchantOffer o : offers) {
            try {
                copy.add(o.copy());
            } catch (Throwable t) {
                copy.add(o);
            }
        }
        return copy;
    }
}
