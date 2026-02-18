package lv.editvillager.mixin;

import lv.editvillager.EvVillagerLock;
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

    @org.spongepowered.asm.mixin.Shadow
    protected abstract boolean canLevelUp();

    @org.spongepowered.asm.mixin.Shadow
    protected abstract void levelUp();

    @Unique
    private boolean ev$tradesLocked = true;

    @Unique
    private boolean ev$professionLocked = false;


    @Unique
    private boolean ev$xpDropEnabled = true;

    @Unique
    private boolean ev$bypassOffersLock = false;


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


    @Inject(method = "setOffers(Lnet/minecraft/village/TradeOfferList;)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void ev$blockSetOffers(TradeOfferList offers, CallbackInfo ci) {
        if (ev$tradesLocked && !ev$bypassOffersLock) {
            ci.cancel();
        }
    }


    @Inject(method = "levelUp", at = @At("HEAD"), require = 0)
    private void ev$saveOffersBeforeLevelUp(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        int currentLevel = ReflectionUtils.getLevel(self.getVillagerData());

        ev$offersBeforeLevelUp = deepCopyOffers(self.getOffers());

        if (!ReflectionUtils.isClient(self.getEntityWorld())) {

        }
    }

    @Inject(method = "levelUp", at = @At("TAIL"), require = 0)
    private void ev$restoreOffersAfterLevelUp(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        int currentLevel = ReflectionUtils.getLevel(self.getVillagerData());
        TradeOfferList currentOffers = self.getOffers();


        TradeOfferList newVanillaTrades = new TradeOfferList();
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


        TradeOfferList tradesToAdd = new TradeOfferList();
        boolean appliedCustom = false;

        if (hasCustomForThisLevel) {

            tradesToAdd.addAll(deepCopyOffers(ev$customLevelTrades.get(currentLevel)));
            appliedCustom = true;
        } else if (!isCustomVillager && !ev$tradesLocked) {
            tradesToAdd.addAll(newVanillaTrades);
        }

        TradeOfferList finalOffers = new TradeOfferList();

        boolean shouldKeepHistory = ev$keepTrades;

        if (!isCustomVillager && !ev$tradesLocked) {
            shouldKeepHistory = true;
        }
        if (isCustomVillager && !hasCustomForThisLevel) {
            shouldKeepHistory = true;
        }

        if (shouldKeepHistory && ev$offersBeforeLevelUp != null) {
            finalOffers.addAll(deepCopyOffers(ev$offersBeforeLevelUp));
        }

        finalOffers.addAll(tradesToAdd);

        ev$forceSetOffers(finalOffers);

        ev$offersBeforeLevelUp = null;
    }

    @Inject(method = "levelUp", at = @At("TAIL"), require = 0)
    private void ev$syncClientAfterLevelUp(CallbackInfo ci) {
        VillagerEntity self = (VillagerEntity) (Object) this;
        if (!ReflectionUtils.isClient(self.getEntityWorld())) {
            self.setVillagerData(self.getVillagerData());
        }
    }

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

    @Inject(method = "writeCustomDataToNbt", at = @At("TAIL"), require = 0)
    private void ev$writeCustomDataToNbt(NbtCompound nbt,
            WrapperLookup registries, CallbackInfo ci) {
        nbt.putBoolean("EvTradesLocked", ev$tradesLocked);
        nbt.putBoolean("EvProfessionLocked", ev$professionLocked);
        nbt.putBoolean("EvXpDropEnabled", ev$xpDropEnabled);
        nbt.putBoolean("EvAlwaysLookAtPlayer", ev$alwaysLookAtPlayer);
        nbt.putBoolean("EvPriceLocked", ev$priceLocked);
        nbt.putBoolean("EvKeepTrades", ev$keepTrades);
        nbt.putLong("EvLastRestockDay", ev$lastRestockDay);

        long[] restockBits = new long[((VillagerEntity) (Object) this).getOffers().size() / 64 + 1];
        TradeOfferList offers = ((VillagerEntity) (Object) this).getOffers();
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i) instanceof lv.editvillager.EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                restockBits[i / 64] |= (1L << (i % 64));
            }
        }
        nbt.putLongArray("EvRestockFlags", restockBits);

        NbtCompound customTradesNbt = new NbtCompound();
        ev$customLevelTrades.forEach((level, trades) -> {
            NbtCompound levelNbt = new NbtCompound();
            for (int i = 0; i < trades.size(); i++) {
                TradeOffer offer = trades.get(i);
                levelNbt.put("Trade" + i, TradeOffer.CODEC.encodeStart(NbtOps.INSTANCE, offer).getOrThrow());
            }
            customTradesNbt.put(String.valueOf(level), levelNbt);
        });
        nbt.put("EvCustomLevelTrades", customTradesNbt);
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"), require = 0)
    private void ev$readCustomDataFromNbtHead(NbtCompound nbt,
            WrapperLookup registries, CallbackInfo ci) {
        ev$bypassOffersLock = true;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("TAIL"), require = 0)
    private void ev$readCustomDataFromNbt(NbtCompound nbt,
            WrapperLookup registries, CallbackInfo ci) {
        try {
            if (nbt.contains("EvTradesLocked"))
                ev$tradesLocked = nbt.getBoolean("EvTradesLocked").orElse(true);
            else
                ev$tradesLocked = true;

            if (nbt.contains("EvProfessionLocked"))
                ev$professionLocked = nbt.getBoolean("EvProfessionLocked").orElse(false);
            else
                ev$professionLocked = false;

            if (nbt.contains("EvXpDropEnabled"))
                ev$xpDropEnabled = nbt.getBoolean("EvXpDropEnabled").orElse(true);
            else
                ev$xpDropEnabled = true;

            if (nbt.contains("EvAlwaysLookAtPlayer"))
                ev$alwaysLookAtPlayer = nbt.getBoolean("EvAlwaysLookAtPlayer").orElse(false);
            else
                ev$alwaysLookAtPlayer = false;

            if (nbt.contains("EvPriceLocked"))
                ev$priceLocked = nbt.getBoolean("EvPriceLocked").orElse(false);
            else
                ev$priceLocked = false;

            if (nbt.contains("EvKeepTrades"))
                ev$keepTrades = nbt.getBoolean("EvKeepTrades").orElse(false);
            else
                ev$keepTrades = false;

            if (nbt.contains("EvLastRestockDay"))
                ev$lastRestockDay = nbt.getLong("EvLastRestockDay").orElse(-1L);
            else
                ev$lastRestockDay = -1L;

            if (nbt.contains("EvRestockFlags")) {
                long[] restockBits = nbt.getLongArray("EvRestockFlags").orElse(new long[0]);
                VillagerEntity self = (VillagerEntity) (Object) this;
                TradeOfferList offers = self.getOffers();
                for (int i = 0; i < offers.size(); i++) {
                    if (i / 64 < restockBits.length) {
                        if ((restockBits[i / 64] & (1L << (i % 64))) != 0) {
                            if (offers.get(i) instanceof lv.editvillager.EvTradeOfferExtension ext) {
                                ext.ev$setDailyRestock(true);
                            }
                        }
                    }
                }
            }

            if (nbt.contains("EvCustomLevelTrades")) {
                NbtCompound customTradesNbt = nbt.getCompound("EvCustomLevelTrades").orElse(new NbtCompound());
                ev$customLevelTrades.clear();
                for (String levelKey : customTradesNbt.getKeys()) {
                    try {
                        int level = Integer.parseInt(levelKey);
                        NbtCompound levelNbt = customTradesNbt.getCompound(levelKey).orElse(new NbtCompound());

                        Map<Integer, TradeOffer> roundedTrades = new TreeMap<>();

                        for (String tradeKey : levelNbt.getKeys()) {
                            if (tradeKey.startsWith("Trade")) {
                                try {
                                    int index = Integer.parseInt(tradeKey.substring(5));
                                    NbtCompound tradeNbt = levelNbt.getCompound(tradeKey).orElse(new NbtCompound());
                                    TradeOffer.CODEC.parse(NbtOps.INSTANCE, tradeNbt)
                                            .resultOrPartial(
                                                    error -> System.err.println("Ev: Failed to parse trade: " + error))
                                            .ifPresent(o -> roundedTrades.put(index, o));
                                } catch (NumberFormatException e) {
                                }
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
        } finally {
            ev$bypassOffersLock = false;
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

    @Inject(method = "prepareOffersFor", at = @At("TAIL"))
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

        if (!ReflectionUtils.isClient(self.getEntityWorld())) {

            if (!ev$tradesLocked && this.canLevelUp() && self.isAiDisabled()) {
                int level = ReflectionUtils.getLevel(self.getVillagerData());
                if (VillagerData.getUpperLevelExperience(level) <= self.getExperience()) {
                    this.levelUp();
                }
            }

            long worldTime = self.getEntityWorld().getTime();
            long currentDay = worldTime / 24000L;

            if (ev$lastRestockDay != currentDay) {
                if (ev$isBedNearby(self)) {
                    ev$lastRestockDay = currentDay;

                    TradeOfferList offers = self.getOffers();
                    int restockedCount = 0;
                    for (TradeOffer offer : offers) {
                        if (offer instanceof lv.editvillager.EvTradeOfferExtension ext && ext.ev$isDailyRestock()) {
                            offer.resetUses();
                            restockedCount++;
                        }
                    }

                    if (restockedCount > 0) {
                        self.getEntityWorld().sendEntityStatus(self, (byte) 14);
                        self.playSound(net.minecraft.sound.SoundEvents.ENTITY_VILLAGER_YES, 1.0f, self.getSoundPitch());

                        final int finalCount = restockedCount;
                        self.getEntityWorld().getPlayers().forEach(p -> p.sendMessage(net.minecraft.text.Text.literal(
                                "§e[EditVillagers] §aТорги обновлены! (Житель у кровати) (" + finalCount + " шт.)"),
                                true));
                    }
                }
            }
        }
    }

    @Unique
    private boolean ev$isBedNearby(VillagerEntity villager) {
        net.minecraft.util.math.BlockPos pos = villager.getBlockPos();
        net.minecraft.world.World world = villager.getEntityWorld();

        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    net.minecraft.util.math.BlockPos p = pos.add(x, y, z);
                    net.minecraft.block.BlockState state = world.getBlockState(p);
                    if (state.isIn(net.minecraft.registry.tag.BlockTags.BEDS)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    @Inject(method = "fillRecipes", at = @At("TAIL"), require = 0)
    private void ev$fillRecipesOverride(CallbackInfo ci) {
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
