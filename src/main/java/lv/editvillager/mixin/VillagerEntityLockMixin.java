package lv.editvillager.mixin;

import lv.editvillager.EvTradeOfferExtension;
import lv.editvillager.EvVillagerLock;
import lv.editvillager.NbtCompat;
import net.minecraft.entity.passive.VillagerEntity;
import lv.editvillager.ReflectionUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper.WrapperLookup;
import net.minecraft.village.TradeOffer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.TreeMap;
import java.util.Map;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityLockMixin implements EvVillagerLock {

    @Unique
    private boolean ev$tradesLocked = true;

    @Unique
    private boolean ev$professionLocked = false;


    @Unique
    private boolean ev$xpDropEnabled = true;

    @Unique
    private boolean ev$bypassOffersLock = false;


    @org.spongepowered.asm.mixin.Shadow
    protected abstract boolean canLevelUp();

    //? if 1.21.11 {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void levelUp(net.minecraft.server.world.ServerWorld world);
    //?} else {
    @org.spongepowered.asm.mixin.Shadow
    protected abstract void levelUp();
    //?}

    @Unique
    private TradeOfferList ev$offersBeforeLevelUp = null;

    @Override
    public void ev$setTradesLocked(boolean locked) {
        this.ev$tradesLocked = locked;
    }

    @Override
    public boolean ev$areTradesLocked() {
        return ev$tradesLocked;
    }

    @Override
    public void ev$setProfessionLocked(boolean locked) {
        this.ev$professionLocked = locked;
    }

    @Override
    public boolean ev$isProfessionLocked() {
        return ev$professionLocked;
    }


    @Unique
    private boolean ev$alwaysLookAtPlayer = false;


    @Unique
    private boolean ev$priceLocked = false;


    @Unique
    private boolean ev$keepTrades = false;


    @Unique
    private long ev$lastRestockDay = -1;

    @Unique
    private boolean ev$effectsOnActions = true;

    @Unique
    private String ev$actionParticle = "default";

    @Unique
    private boolean ev$continuousEffect = false;

    @Unique
    private String ev$continuousParticle = "default";

    @Unique
    private int ev$continuousParticleCount = 5;

    @Override
    public void ev$setEffectsOnActions(boolean enabled) {
        this.ev$effectsOnActions = enabled;
    }

    @Override
    public boolean ev$hasEffectsOnActions() {
        return this.ev$effectsOnActions;
    }

    @Override
    public void ev$setActionParticle(String particle) {
        this.ev$actionParticle = particle;
    }

    @Override
    public String ev$getActionParticle() {
        return this.ev$actionParticle;
    }

    @Override
    public void ev$setContinuousEffect(boolean enabled) {
        this.ev$continuousEffect = enabled;
    }

    @Override
    public boolean ev$hasContinuousEffect() {
        return this.ev$continuousEffect;
    }

    @Override
    public void ev$setContinuousParticle(String particle) {
        this.ev$continuousParticle = particle;
    }

    @Override
    public String ev$getContinuousParticle() {
        return this.ev$continuousParticle;
    }

    @Override
    public void ev$setContinuousParticleCount(int count) {
        this.ev$continuousParticleCount = count;
    }

    @Override
    public int ev$getContinuousParticleCount() {
        return this.ev$continuousParticleCount;
    }

    @Override
    public void ev$setAlwaysLookAtPlayer(boolean enable) {
        this.ev$alwaysLookAtPlayer = enable;
    }

    @Override
    public boolean ev$shouldAlwaysLookAtPlayer() {
        return ev$alwaysLookAtPlayer;
    }

    @Override
    public void ev$setPriceLock(boolean locked) {
        this.ev$priceLocked = locked;
    }

    public boolean ev$isPriceLocked() {
        return ev$priceLocked;
    }

    @Override
    public void ev$setKeepTrades(boolean keep) {
        this.ev$keepTrades = keep;
    }

    @Override
    public boolean ev$shouldKeepTrades() {
        return ev$keepTrades;
    }


    @Override
    public void ev$setXpDropEnabled(boolean enabled) {
        this.ev$xpDropEnabled = enabled;
    }

    @Override
    public boolean ev$isXpDropEnabled() {
        return ev$xpDropEnabled;
    }

    @Override
    public void ev$forceSetOffers(TradeOfferList offers) {
        ev$bypassOffersLock = true;
        try {
            ((VillagerEntity) (Object) this).setOffers(offers);
        } finally {
            ev$bypassOffersLock = false;
        }
    }


    //? if neoforge {
    @Inject(method = "setOffers(Lnet/minecraft/world/item/trading/MerchantOffers;)V", at = @At("HEAD"), cancellable = true, require = 0)
    //?} else {
    @Inject(method = "setOffers(Lnet/minecraft/village/TradeOfferList;)V", at = @At("HEAD"), cancellable = true, require = 0)
    //?}
    private void ev$blockSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (ev$tradesLocked && !ev$bypassOffersLock) {
            ci.cancel();
        }
    }

    /**
     * Vanilla refuses trading with babies (isBaby → unhappy head shake).
     * EditVillagers allows trading with small villagers that have offers.
     */
    //? if neoforge {
    //? if 1.21.11 {
    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;isBaby()Z"),
            require = 0)
    private boolean ev$allowTradingWithBaby(VillagerEntity self) {
        return false;
    }
    //?} else {
    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/Villager;isBaby()Z"),
            require = 0)
    private boolean ev$allowTradingWithBaby(VillagerEntity self) {
        return false;
    }
    //?}

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "mobInteract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isBaby()Z"),
            require = 0)
    private boolean ev$allowTradingWithBabyLiving(net.minecraft.entity.LivingEntity self) {
        return false;
    }
    //?} else {
    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "interactMob",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/VillagerEntity;isBaby()Z"),
            require = 0)
    private boolean ev$allowTradingWithBaby(VillagerEntity self) {
        return false;
    }

    @org.spongepowered.asm.mixin.injection.Redirect(
            method = "interactMob",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isBaby()Z"),
            require = 0)
    private boolean ev$allowTradingWithBabyLiving(net.minecraft.entity.LivingEntity self) {
        return false;
    }
    //?}

    @Unique
    private boolean ev$hasAnyCustomLevelTrades() {
        for (TradeOfferList trades : ev$customLevelTrades.values()) {
            if (trades != null && !trades.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Unique
    private void ev$applyTradesAfterLevelUp(VillagerEntity self) {
        if (ReflectionUtils.isClient(self.getEntityWorld())) {
            ev$offersBeforeLevelUp = null;
            return;
        }

        int currentLevel = ReflectionUtils.getLevel(self.getVillagerData());

        if (!ev$tradesLocked && ev$hasAnyCustomLevelTrades()) {
            ev$rebuildOffersFromMenu(currentLevel);
            if (ev$offersBeforeLevelUp != null) {
                TradeOfferList merged = self.getOffers();
                ev$mergeOfferListsByIndex(merged, ev$offersBeforeLevelUp);
                ev$forceSetOffers(merged);
                ev$syncCustomLevelTradesFromFlatInternal(merged, currentLevel);
            }
            self.setVillagerData(self.getVillagerData());
            ev$offersBeforeLevelUp = null;
            return;
        }

        if (!ev$tradesLocked && ev$offersBeforeLevelUp != null) {
            TradeOfferList currentOffers = self.getOffers();
            TradeOfferList newVanillaTrades = new TradeOfferList();
            int oldSize = ev$offersBeforeLevelUp.size();
            if (currentOffers.size() > oldSize) {
                for (int i = oldSize; i < currentOffers.size(); i++) {
                    newVanillaTrades.add(currentOffers.get(i));
                }
            }

            TradeOfferList finalOffers = new TradeOfferList();
            finalOffers.addAll(deepCopyOffers(ev$offersBeforeLevelUp));
            finalOffers.addAll(newVanillaTrades);
            ev$forceSetOffers(finalOffers);
        }

        ev$offersBeforeLevelUp = null;
    }

    //? if 1.21.11 {
    //? if neoforge {
    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("HEAD"), require = 0)
    //?} else {
    @Inject(method = "levelUp(Lnet/minecraft/server/world/ServerWorld;)V", at = @At("HEAD"), require = 0)
    //?}
    private void ev$saveOffersBeforeLevelUp(net.minecraft.server.world.ServerWorld world, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        ev$offersBeforeLevelUp = deepCopyOffers(self.getOffers());
    }

    //? if neoforge {
    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "levelUp(Lnet/minecraft/server/world/ServerWorld;)V", at = @At("TAIL"), require = 0)
    //?}
    private void ev$restoreOffersAfterLevelUp(net.minecraft.server.world.ServerWorld world, CallbackInfo ci) {
        ev$applyTradesAfterLevelUp((VillagerEntity) (Object) this);
    }

    //? if neoforge {
    @Inject(method = "increaseMerchantCareer(Lnet/minecraft/server/level/ServerLevel;)V", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "levelUp(Lnet/minecraft/server/world/ServerWorld;)V", at = @At("TAIL"), require = 0)
    //?}
    private void ev$syncClientAfterLevelUp(net.minecraft.server.world.ServerWorld world, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (!ReflectionUtils.isClient(self.getEntityWorld())) {
            self.setVillagerData(self.getVillagerData());
        }
    }
    //?} else {
    //? if neoforge {
    @Inject(method = "increaseMerchantCareer", at = @At("HEAD"), require = 0)
    //?} else {
    @Inject(method = "levelUp", at = @At("HEAD"), require = 0)
    //?}
    private void ev$saveOffersBeforeLevelUp(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        ev$offersBeforeLevelUp = deepCopyOffers(self.getOffers());
    }

    //? if neoforge {
    @Inject(method = "increaseMerchantCareer", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "levelUp", at = @At("TAIL"), require = 0)
    //?}
    private void ev$restoreOffersAfterLevelUp(CallbackInfo ci) {
        ev$applyTradesAfterLevelUp((VillagerEntity) (Object) this);
    }

    //? if neoforge {
    @Inject(method = "increaseMerchantCareer", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "levelUp", at = @At("TAIL"), require = 0)
    //?}
    private void ev$syncClientAfterLevelUp(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (!ReflectionUtils.isClient(self.getEntityWorld())) {
            self.setVillagerData(self.getVillagerData());
        }
    }
    //?}

    @Unique
    private static TradeOfferList deepCopyOffers(TradeOfferList offers) {
        TradeOfferList copy = new TradeOfferList();
        for (TradeOffer o : offers) {
            try {
                copy.add(o.copy());
            } catch (Throwable t) {
                copy.add(o);
            }
        }
        return copy;
    }

    //? if 1.21.10 || 1.21.11 {
    //? if neoforge {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "writeCustomData", at = @At("TAIL"), require = 0)
    //?}
    private void ev$writeCustomData(net.minecraft.storage.WriteView view, CallbackInfo ci) {
        view.putBoolean("EvTradesLocked", ev$tradesLocked);
        view.putBoolean("EvProfessionLocked", ev$professionLocked);
        view.putBoolean("EvXpDropEnabled", ev$xpDropEnabled);
        view.putBoolean("EvAlwaysLookAtPlayer", ev$alwaysLookAtPlayer);
        view.putBoolean("EvPriceLocked", ev$priceLocked);
        view.putBoolean("EvKeepTrades", ev$keepTrades);
        view.putLong("EvLastRestockDay", ev$lastRestockDay);
        view.putBoolean("EvEffectsOnActions", ev$effectsOnActions);
        view.putString("EvActionParticle", ev$actionParticle);
        view.putBoolean("EvContinuousEffect", ev$continuousEffect);
        view.putString("EvContinuousParticle", ev$continuousParticle);
        view.putInt("EvContinuousParticleCount", ev$continuousParticleCount);

        TradeOfferList offers = ((VillagerEntity) (Object) this).getOffers();
        long[] restockBits = new long[offers.size() / 64 + 1];
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i) instanceof EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                restockBits[i / 64] |= (1L << (i % 64));
            }
        }
        NbtCompound restockTag = new NbtCompound();
        restockTag.putLongArray("Bits", restockBits);
        view.put("EvRestockFlags", NbtCompound.CODEC, restockTag);

        NbtCompound customTradesNbt = encodeCustomLevelTradesNbt();
        view.put("EvCustomLevelTrades", NbtCompound.CODEC, customTradesNbt);
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), require = 0)
    //?} else {
    @Inject(method = "readCustomData", at = @At("HEAD"), require = 0)
    //?}
    private void ev$readCustomDataHead(net.minecraft.storage.ReadView view, CallbackInfo ci) {
        ev$bypassOffersLock = true;
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "readCustomData", at = @At("TAIL"), require = 0)
    //?}
    private void ev$readCustomData(net.minecraft.storage.ReadView view, CallbackInfo ci) {
        try {
            ev$tradesLocked = view.getBoolean("EvTradesLocked", true);
            ev$professionLocked = view.getBoolean("EvProfessionLocked", false);
            ev$xpDropEnabled = view.getBoolean("EvXpDropEnabled", true);
            ev$alwaysLookAtPlayer = view.getBoolean("EvAlwaysLookAtPlayer", false);
            ev$priceLocked = view.getBoolean("EvPriceLocked", false);
            ev$keepTrades = view.getBoolean("EvKeepTrades", false);
            ev$lastRestockDay = view.getLong("EvLastRestockDay", -1L);
            ev$effectsOnActions = view.getBoolean("EvEffectsOnActions", true);
            ev$actionParticle = view.getString("EvActionParticle", "default");
            ev$continuousEffect = view.getBoolean("EvContinuousEffect", false);
            ev$continuousParticle = view.getString("EvContinuousParticle", "default");
            ev$continuousParticleCount = view.getInt("EvContinuousParticleCount", 5);

            view.read("EvRestockFlags", NbtCompound.CODEC).ifPresent(restockTag -> {
                long[] restockBits = NbtCompat.getLongArray(restockTag, "Bits");
                TradeOfferList offers = ((VillagerEntity) (Object) this).getOffers();
                for (int i = 0; i < offers.size(); i++) {
                    if (i / 64 < restockBits.length
                            && (restockBits[i / 64] & (1L << (i % 64))) != 0
                            && offers.get(i) instanceof EvTradeOfferExtension ext) {
                        ext.ev$setDailyRestock(true);
                    }
                }
            });

            view.read("EvCustomLevelTrades", NbtCompound.CODEC).ifPresent(this::ev$loadCustomLevelTradesNbt);
        } finally {
            ev$bypassOffersLock = false;
        }
    }
    //?} else {
    //? if neoforge {
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"), require = 0)
    //?}
    private void ev$writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        nbt.putBoolean("EvTradesLocked", ev$tradesLocked);
        nbt.putBoolean("EvProfessionLocked", ev$professionLocked);
        nbt.putBoolean("EvXpDropEnabled", ev$xpDropEnabled);
        nbt.putBoolean("EvAlwaysLookAtPlayer", ev$alwaysLookAtPlayer);
        nbt.putBoolean("EvPriceLocked", ev$priceLocked);
        nbt.putBoolean("EvKeepTrades", ev$keepTrades);
        nbt.putLong("EvLastRestockDay", ev$lastRestockDay);
        nbt.putBoolean("EvEffectsOnActions", ev$effectsOnActions);
        nbt.putString("EvActionParticle", ev$actionParticle);
        nbt.putBoolean("EvContinuousEffect", ev$continuousEffect);
        nbt.putString("EvContinuousParticle", ev$continuousParticle);
        nbt.putInt("EvContinuousParticleCount", ev$continuousParticleCount);

        long[] restockBits = new long[((VillagerEntity) (Object) this).getOffers().size() / 64 + 1];
        TradeOfferList offers = ((VillagerEntity) (Object) this).getOffers();
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i) instanceof EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                restockBits[i / 64] |= (1L << (i % 64));
            }
        }
        nbt.putLongArray("EvRestockFlags", restockBits);
        nbt.put("EvCustomLevelTrades", encodeCustomLevelTradesNbt());
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"), require = 0)
    //?} else {
    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"), require = 0)
    //?}
    private void ev$readCustomDataFromNbtHead(NbtCompound nbt, CallbackInfo ci) {
        ev$bypassOffersLock = true;
    }

    //? if neoforge {
    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"), require = 0)
    //?}
    private void ev$readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        try {
            if (nbt.contains("EvTradesLocked"))
                ev$tradesLocked = NbtCompat.getBoolean(nbt, "EvTradesLocked", true);
            else
                ev$tradesLocked = true;

            if (nbt.contains("EvProfessionLocked"))
                ev$professionLocked = NbtCompat.getBoolean(nbt, "EvProfessionLocked", false);
            else
                ev$professionLocked = false;

            if (nbt.contains("EvXpDropEnabled"))
                ev$xpDropEnabled = NbtCompat.getBoolean(nbt, "EvXpDropEnabled", true);
            else
                ev$xpDropEnabled = true;

            if (nbt.contains("EvAlwaysLookAtPlayer"))
                ev$alwaysLookAtPlayer = NbtCompat.getBoolean(nbt, "EvAlwaysLookAtPlayer", false);
            else
                ev$alwaysLookAtPlayer = false;

            if (nbt.contains("EvPriceLocked"))
                ev$priceLocked = NbtCompat.getBoolean(nbt, "EvPriceLocked", false);
            else
                ev$priceLocked = false;

            if (nbt.contains("EvKeepTrades"))
                ev$keepTrades = NbtCompat.getBoolean(nbt, "EvKeepTrades", false);
            else
                ev$keepTrades = false;

            if (nbt.contains("EvLastRestockDay"))
                ev$lastRestockDay = NbtCompat.getLong(nbt, "EvLastRestockDay", -1L);
            else
                ev$lastRestockDay = -1L;

            if (nbt.contains("EvEffectsOnActions"))
                ev$effectsOnActions = NbtCompat.getBoolean(nbt, "EvEffectsOnActions", true);
            else
                ev$effectsOnActions = true;

            if (nbt.contains("EvActionParticle"))
                ev$actionParticle = NbtCompat.getString(nbt, "EvActionParticle", "default");
            else
                ev$actionParticle = "default";

            if (nbt.contains("EvContinuousEffect"))
                ev$continuousEffect = NbtCompat.getBoolean(nbt, "EvContinuousEffect", false);
            else
                ev$continuousEffect = false;

            if (nbt.contains("EvContinuousParticle")) {
                this.ev$continuousParticle = NbtCompat.getString(nbt, "EvContinuousParticle", "default");
            } else {
                ev$continuousParticle = "default";
            }
            if (nbt.contains("EvContinuousParticleCount")) {
                this.ev$continuousParticleCount = NbtCompat.getInt(nbt, "EvContinuousParticleCount", 5);
            }

            if (nbt.contains("EvRestockFlags")) {
                long[] restockBits = NbtCompat.getLongArray(nbt, "EvRestockFlags");
                VillagerEntity self = (VillagerEntity) (Object) this;
                TradeOfferList liveOffers = self.getOffers();
                for (int i = 0; i < liveOffers.size(); i++) {
                    if (i / 64 < restockBits.length) {
                        if ((restockBits[i / 64] & (1L << (i % 64))) != 0) {
                            if (liveOffers.get(i) instanceof EvTradeOfferExtension ext) {
                                ext.ev$setDailyRestock(true);
                            }
                        }
                    }
                }
            }

            if (nbt.contains("EvCustomLevelTrades")) {
                ev$loadCustomLevelTradesNbt(NbtCompat.getCompound(nbt, "EvCustomLevelTrades"));
            }
        } finally {
            ev$bypassOffersLock = false;
        }
    }
    //?}

    @Unique
    private NbtCompound encodeCustomLevelTradesNbt() {
        NbtCompound customTradesNbt = new NbtCompound();
        WrapperLookup lookup = ((VillagerEntity) (Object) this).getRegistryManager();
        var ops = lookup != null
                ? net.minecraft.registry.RegistryOps.of(NbtOps.INSTANCE, lookup)
                : NbtOps.INSTANCE;
        ev$customLevelTrades.forEach((level, trades) -> {
            NbtCompound levelNbt = new NbtCompound();
            for (int i = 0; i < trades.size(); i++) {
                final int tradeIndex = i;
                TradeOffer offer = trades.get(i);
                TradeOffer.CODEC.encodeStart(ops, offer)
                        .resultOrPartial(err -> System.err.println("Ev: Failed to encode trade: " + err))
                        .ifPresent(encoded -> levelNbt.put("Trade" + tradeIndex, encoded));
            }
            customTradesNbt.put(String.valueOf(level), levelNbt);
        });
        return customTradesNbt;
    }

    @Unique
    private void ev$loadCustomLevelTradesNbt(NbtCompound customTradesNbt) {
        WrapperLookup lookup = ((VillagerEntity) (Object) this).getRegistryManager();
        var ops = lookup != null
                ? net.minecraft.registry.RegistryOps.of(NbtOps.INSTANCE, lookup)
                : NbtOps.INSTANCE;
        ev$customLevelTrades.clear();
        for (String levelKey : customTradesNbt.getKeys()) {
            try {
                int level = Integer.parseInt(levelKey);
                NbtCompound levelNbt = NbtCompat.getCompound(customTradesNbt, levelKey);
                Map<Integer, TradeOffer> roundedTrades = new TreeMap<>();
                for (String tradeKey : levelNbt.getKeys()) {
                    if (!tradeKey.startsWith("Trade")) {
                        continue;
                    }
                    try {
                        int index = Integer.parseInt(tradeKey.substring(5));
                        net.minecraft.nbt.NbtElement raw = levelNbt.get(tradeKey);
                        if (raw == null) {
                            continue;
                        }
                        TradeOffer.CODEC.parse(ops, raw)
                                .resultOrPartial(error -> System.err.println("Ev: Failed to parse trade: " + error))
                                .ifPresent(o -> roundedTrades.put(index, o));
                    } catch (NumberFormatException ignored) {
                    }
                }
                TradeOfferList trades = new TradeOfferList();
                trades.addAll(roundedTrades.values());
                ev$customLevelTrades.put(level, trades);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Inject(method = "setVillagerData", at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$blockSetVillagerData(VillagerData newData, CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        VillagerData currentData = self.getVillagerData();

        if (currentData == null)
            return;

        int currentLevel = ReflectionUtils.getLevel(currentData);
        int newLevel = ReflectionUtils.getLevel(newData);

        if (newLevel > currentLevel) {
            return;
        }

        Object newProf = ReflectionUtils.getEntryValue(ReflectionUtils.getProfession(newData));
        Object currentProf = ReflectionUtils.getEntryValue(ReflectionUtils.getProfession(currentData));

        if (ev$professionLocked) {
            if (newProf != null && currentProf != null && newProf != currentProf) {
                ci.cancel();
                return;
            }
        }
    }

    //? if neoforge {
    @Inject(method = "updateSpecialPrices", at = @At("TAIL"), require = 0)
    //?} else {
    @Inject(method = "prepareOffersFor", at = @At("TAIL"), require = 0)
    //?}
    private void ev$blockPriceChangeOnOpen(net.minecraft.entity.player.PlayerEntity player, CallbackInfo ci) {
        if (ev$priceLocked) {
            VillagerEntity self = (VillagerEntity) (Object) this;
            for (TradeOffer offer : self.getOffers()) {
                offer.clearSpecialPrice();
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void ev$tick(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;

        if (ev$alwaysLookAtPlayer) {
            net.minecraft.entity.player.PlayerEntity p = self.getEntityWorld().getClosestPlayer(self, 8.0);
            if (p != null) {
                if (self.isAiDisabled()) {
                    double d = p.getX() - self.getX();
                    double e = p.getEyeY() - self.getEyeY();
                    double f = p.getZ() - self.getZ();

                    float yaw = (float) (Math.atan2(f, d) * 57.2957763671875) - 90.0F;

                    self.setYaw(yaw);
                    self.setHeadYaw(yaw);
                    self.setBodyYaw(yaw);
                } else {
                    self.getLookControl().lookAt(p, 30.0f, 30.0f);
                }
            }
        }

        if (self.getEntityWorld() instanceof net.minecraft.server.world.ServerWorld) {

            if (!ev$tradesLocked && this.canLevelUp() && self.isAiDisabled()) {
                int level = ReflectionUtils.getLevel(self.getVillagerData());
                if (VillagerData.getUpperLevelExperience(level) <= self.getExperience()) {
                    //? if 1.21.11 {
                    this.levelUp((net.minecraft.server.world.ServerWorld) self.getEntityWorld());
                    //?} else {
                    this.levelUp();
                    //?}
                }
            }

            // getTimeOfDay() — смена дня (реагирует на /time add и сон).
            // getTime() — внутренние тики, команда /time на него не влияет.
            long worldTime = self.getEntityWorld().getTimeOfDay();
            long currentDay = worldTime / 24000L;

            if (ev$lastRestockDay != currentDay && ReflectionUtils.isBedNearby(self)) {
                TradeOfferList offers = self.getOffers();
                int restockedCount = 0;
                boolean hasRestockTrades = false;
                for (TradeOffer offer : offers) {
                    if (offer instanceof lv.editvillager.EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                        hasRestockTrades = true;
                        offer.resetUses();
                        restockedCount++;
                    }
                }

                if (restockedCount > 0) {
                    ev$lastRestockDay = currentDay;
                    self.getEntityWorld().sendEntityStatus(self, (byte) 14);
                    self.playSound(net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, 1.0f, self.getSoundPitch());

                    self.getEntityWorld().getPlayers().forEach(p -> p.sendMessage(net.minecraft.text.Text.literal(
                            "§e[EditVillagers] §aТорги обновлены!"),
                            true));
                } else if (!hasRestockTrades) {
                    ev$lastRestockDay = currentDay;
                }
            }

            if (ev$continuousEffect && !"default".equals(ev$continuousParticle)) {
                if (self.age % 10 == 0) {
                    try {
                        net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(ev$continuousParticle);
                        if (id != null) {
                            net.minecraft.particle.ParticleType<?> type = net.minecraft.registry.Registries.PARTICLE_TYPE.get(id);
                            if (type instanceof net.minecraft.particle.ParticleEffect effect) {
                                ((net.minecraft.server.world.ServerWorld) self.getEntityWorld()).spawnParticles(effect, 
                                    self.getParticleX(1.0D), 
                                    self.getRandomBodyY(), 
                                    self.getParticleZ(1.0D), 
                                    ev$continuousParticleCount, 0.3, 0.3, 0.3, 0.01);
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    @Unique
    private final java.util.Map<Integer, TradeOfferList> ev$customLevelTrades = new java.util.HashMap<>();

    @Override
    public void ev$setCustomLevelTrades(int level, TradeOfferList trades) {
        if (trades == null || trades.isEmpty()) {
            ev$customLevelTrades.remove(level);
        } else {
            ev$customLevelTrades.put(level, deepCopyOffers(trades));
        }
    }

    @Override
    public TradeOfferList ev$getCustomLevelTrades(int level) {
        if (ev$customLevelTrades.containsKey(level)) {
            return deepCopyOffers(ev$customLevelTrades.get(level));
        }
        return new TradeOfferList();
    }

    @Override
    public void ev$rebuildOffersFromMenu(int upToLevel) {
        TradeOfferList combined = new TradeOfferList();
        for (int menuLevel = 1; menuLevel <= upToLevel; menuLevel++) {
            TradeOfferList tierTrades = ev$customLevelTrades.get(menuLevel);
            if (tierTrades == null) {
                continue;
            }
            for (TradeOffer offer : tierTrades) {
                combined.add(offer.copy());
            }
        }
        ev$forceSetOffers(combined);
    }

    @Override
    public void ev$syncCustomLevelTradesFromFlat(TradeOfferList flat) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        int currentLevel = ReflectionUtils.getLevel(self.getVillagerData());
        ev$syncCustomLevelTradesFromFlatInternal(flat, currentLevel);
    }

    @Unique
    private void ev$syncCustomLevelTradesFromFlatInternal(TradeOfferList flat, int upToLevel) {
        if (flat == null || flat.isEmpty()) {
            return;
        }
        int idx = 0;
        for (int level = 1; level <= upToLevel; level++) {
            TradeOfferList tier = ev$customLevelTrades.get(level);
            if (tier == null || tier.isEmpty()) {
                continue;
            }
            TradeOfferList updated = new TradeOfferList();
            for (int i = 0; i < tier.size(); i++) {
                if (idx < flat.size()) {
                    updated.add(flat.get(idx++));
                } else {
                    updated.add(tier.get(i).copy());
                }
            }
            ev$customLevelTrades.put(level, deepCopyOffers(updated));
        }
    }

    @Override
    public void ev$mergeOfferMetadataFrom(TradeOfferList metadataSource) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        TradeOfferList current = self.getOffers();
        ev$mergeOfferListsByIndex(current, metadataSource);
        ev$forceSetOffers(current);
    }

    @Unique
    private static void ev$mergeOfferListsByIndex(TradeOfferList target, TradeOfferList metadataSource) {
        for (int i = 0; i < target.size() && i < metadataSource.size(); i++) {
            target.set(i, ev$copyOfferWithMetadata(target.get(i), metadataSource.get(i)));
        }
    }

    @Unique
    private static TradeOffer ev$copyOfferWithMetadata(TradeOffer base, TradeOffer metadata) {
        TradeOffer result = new TradeOffer(
                base.getFirstBuyItem(),
                base.getSecondBuyItem(),
                base.getSellItem(),
                metadata.getUses(),
                metadata.getMaxUses(),
                base.getMerchantExperience(),
                base.getPriceMultiplier(),
                base.getDemandBonus());
        if (result instanceof EvTradeOfferExtension ext && metadata instanceof EvTradeOfferExtension metaExt) {
            ext.ev$setDailyRestock(metaExt.ev$isDailyRestock());
        }
        return result;
    }

    @Unique
    private NbtCompound writeOffersToNbt(TradeOfferList offers) {
        return new NbtCompound();
    }

    @Unique
    private TradeOfferList readOffersFromNbt(NbtCompound wrapper) {
        return new TradeOfferList();
    }
}
