package lv.editvillager;

import com.mojang.serialization.DynamicOps;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EntityType;
//? if !(1.21.1) {
import net.minecraft.entity.TypedEntityData;
//?}
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryOps;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerData;

/**
 * Shared clone logic. Never uses Entity#readData with full NBT on 1.21.11.
 */
public final class VillagerCloneHelper {

    public static final String EGG_DATA_KEY = "ev_villager";

    private VillagerCloneHelper() {
    }

    public static VillagerEntity cloneNearby(ServerPlayerEntity player, VillagerEntity source) {
        ServerWorld world = (ServerWorld) player.getEntityWorld();
        VillagerEntity copy = createClone(world, source);
        copy.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
        copy.setHeadYaw(player.getYaw());
        copy.setBodyYaw(player.getYaw());
        world.spawnEntity(copy);
        return copy;
    }

    public static VillagerEntity spawnFromCloneData(ServerWorld world, BlockPos spawnPos, NbtCompound data, float yaw) {
        VillagerEntity copy = new VillagerEntity(EntityType.VILLAGER, world);
        applySafeCloneData(copy, data);
        Vec3d spawn = Vec3d.ofBottomCenter(spawnPos);
        copy.refreshPositionAndAngles(spawn.x, spawn.y, spawn.z, yaw, 0.0f);
        copy.setHeadYaw(yaw);
        copy.setBodyYaw(yaw);
        copy.setPersistent();
        world.spawnEntity(copy);
        return copy;
    }

    public static VillagerEntity createClone(ServerWorld world, VillagerEntity source) {
        VillagerEntity copy = new VillagerEntity(EntityType.VILLAGER, world);
        copyFromLive(source, copy);
        return copy;
    }

    public static ItemStack createCloneEgg(VillagerEntity source) {
        ItemStack egg = new ItemStack(Items.VILLAGER_SPAWN_EGG);
        //? if 1.21.1 {
        // 1.21.1 ENTITY_DATA is NbtComponent (id tag). Clone payload stays in CUSTOM_DATA.
        NbtCompound entityTag = new NbtCompound();
        entityTag.putString("id", EntityType.getId(EntityType.VILLAGER).toString());
        egg.set(DataComponentTypes.ENTITY_DATA, NbtComponent.of(entityTag));
        //?} else {
        // 1.21.10+ getEntityType() reads ONLY ENTITY_DATA — without it spawn always FAILs
        egg.set(DataComponentTypes.ENTITY_DATA, TypedEntityData.create(EntityType.VILLAGER, new NbtCompound()));
        //?}
        NbtCompound customRoot = new NbtCompound();
        customRoot.put(EGG_DATA_KEY, captureCloneData(source));
        egg.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(customRoot));
        if (source.hasCustomName()) {
            egg.set(DataComponentTypes.CUSTOM_NAME, source.getCustomName());
        }
        return egg;
    }

    public static NbtCompound readEggCloneData(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) {
            return null;
        }
        NbtCompound root = custom.copyNbt();
        if (!root.contains(EGG_DATA_KEY)) {
            return null;
        }
        NbtCompound data = NbtCompat.getCompound(root, EGG_DATA_KEY);
        return data.isEmpty() ? null : data;
    }

    public static void copyFromLive(VillagerEntity source, VillagerEntity copy) {
        copy.setVillagerData(source.getVillagerData());
        copy.setExperience(source.getExperience());
        copy.setBaby(source.isBaby());
        ReflectionUtils.setAgeLocked(copy, source.isBaby());

        TradeOfferList newOffers = new TradeOfferList();
        for (TradeOffer offer : source.getOffers()) {
            newOffers.add(offer.copy());
        }
        if (copy instanceof EvVillagerLock lock) {
            lock.ev$forceSetOffers(newOffers);
        } else {
            copy.setOffers(newOffers);
        }

        if (source instanceof EvVillagerLock oldLock && copy instanceof EvVillagerLock newLock) {
            newLock.ev$setTradesLocked(oldLock.ev$areTradesLocked());
            newLock.ev$setProfessionLocked(oldLock.ev$isProfessionLocked());
            newLock.ev$setAlwaysLookAtPlayer(oldLock.ev$shouldAlwaysLookAtPlayer());
            newLock.ev$setPriceLock(oldLock.ev$isPriceLocked());
            newLock.ev$setKeepTrades(oldLock.ev$shouldKeepTrades());
            newLock.ev$setXpDropEnabled(oldLock.ev$isXpDropEnabled());
            newLock.ev$setEffectsOnActions(oldLock.ev$hasEffectsOnActions());
            newLock.ev$setActionParticle(oldLock.ev$getActionParticle());
            newLock.ev$setContinuousEffect(oldLock.ev$hasContinuousEffect());
            newLock.ev$setContinuousParticle(oldLock.ev$getContinuousParticle());
            newLock.ev$setContinuousParticleCount(oldLock.ev$getContinuousParticleCount());
            for (int i = 1; i <= 5; i++) {
                newLock.ev$setCustomLevelTrades(i, oldLock.ev$getCustomLevelTrades(i));
            }
        }

        copy.setAiDisabled(source.isAiDisabled());
        copy.setSilent(source.isSilent());
        copy.setInvulnerable(source.isInvulnerable());
        copy.setGlowing(source.isGlowing());
        copy.setPersistent();

        if (source.hasCustomName()) {
            copy.setCustomName(source.getCustomName());
            copy.setCustomNameVisible(source.isCustomNameVisible());
        }
    }

    /** Build clone blob from live fields — do not rely on Entity save/read. */
    public static NbtCompound captureCloneData(VillagerEntity source) {
        NbtCompound nbt = new NbtCompound();
        RegistryWrapper.WrapperLookup lookup = source.getRegistryManager();
        DynamicOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

        VillagerData.CODEC.encodeStart(ops, source.getVillagerData()).result().ifPresent(el -> nbt.put("VillagerData", el));
        nbt.putInt("Xp", source.getExperience());
        nbt.putInt("Age", source.getBreedingAge());
        nbt.putBoolean("NoAI", source.isAiDisabled());
        nbt.putBoolean("Silent", source.isSilent());
        nbt.putBoolean("Invulnerable", source.isInvulnerable());
        nbt.putBoolean("Glowing", source.isGlowing());
        nbt.putBoolean("CustomNameVisible", source.isCustomNameVisible());

        TradeOfferList.CODEC.encodeStart(ops, source.getOffers()).result().ifPresent(el -> nbt.put("Offers", el));

        if (source instanceof EvVillagerLock lock) {
            nbt.putBoolean("EvTradesLocked", lock.ev$areTradesLocked());
            nbt.putBoolean("EvProfessionLocked", lock.ev$isProfessionLocked());
            nbt.putBoolean("EvXpDropEnabled", lock.ev$isXpDropEnabled());
            nbt.putBoolean("EvAlwaysLookAtPlayer", lock.ev$shouldAlwaysLookAtPlayer());
            nbt.putBoolean("EvPriceLocked", lock.ev$isPriceLocked());
            nbt.putBoolean("EvKeepTrades", lock.ev$shouldKeepTrades());
            nbt.putBoolean("EvEffectsOnActions", lock.ev$hasEffectsOnActions());
            nbt.putBoolean("EvContinuousEffect", lock.ev$hasContinuousEffect());
            nbt.putInt("EvContinuousParticleCount", lock.ev$getContinuousParticleCount());
            if (lock.ev$getActionParticle() != null) {
                nbt.putString("EvActionParticle", lock.ev$getActionParticle());
            }
            if (lock.ev$getContinuousParticle() != null) {
                nbt.putString("EvContinuousParticle", lock.ev$getContinuousParticle());
            }
            NbtCompound levels = new NbtCompound();
            for (int i = 1; i <= 5; i++) {
                TradeOfferList levelOffers = lock.ev$getCustomLevelTrades(i);
                if (levelOffers != null && !levelOffers.isEmpty()) {
                    final int level = i;
                    TradeOfferList.CODEC.encodeStart(ops, levelOffers).result()
                            .ifPresent(el -> levels.put(String.valueOf(level), el));
                }
            }
            if (!levels.isEmpty()) {
                nbt.put("EvCustomLevelTrades", levels);
            }
        }
        return nbt;
    }

    public static void applySafeCloneData(VillagerEntity target, NbtCompound data) {
        if (data == null || data.isEmpty()) {
            return;
        }
        RegistryWrapper.WrapperLookup lookup = target.getRegistryManager();
        DynamicOps<NbtElement> ops = RegistryOps.of(NbtOps.INSTANCE, lookup);

        if (data.contains("VillagerData")) {
            NbtElement vd = data.get("VillagerData");
            if (vd != null) {
                VillagerData.CODEC.parse(ops, vd).result().ifPresent(target::setVillagerData);
            }
        }

        if (data.contains("Xp")) {
            target.setExperience(NbtCompat.getInt(data, "Xp", target.getExperience()));
        }

        if (data.contains("Age")) {
            int age = NbtCompat.getInt(data, "Age", 0);
            target.setBreedingAge(age);
            if (age < 0) {
                ReflectionUtils.setAgeLocked(target, true);
            }
        }

        TradeOfferList offers = decodeOffersElement(data.get("Offers"), ops);
        if (!offers.isEmpty()) {
            if (target instanceof EvVillagerLock lock) {
                lock.ev$forceSetOffers(offers);
            } else {
                target.setOffers(offers);
            }
        }

        if (target instanceof EvVillagerLock lock) {
            if (data.contains("EvTradesLocked")) {
                lock.ev$setTradesLocked(NbtCompat.getBoolean(data, "EvTradesLocked", false));
            }
            if (data.contains("EvProfessionLocked")) {
                lock.ev$setProfessionLocked(NbtCompat.getBoolean(data, "EvProfessionLocked", false));
            }
            if (data.contains("EvXpDropEnabled")) {
                lock.ev$setXpDropEnabled(NbtCompat.getBoolean(data, "EvXpDropEnabled", true));
            }
            if (data.contains("EvAlwaysLookAtPlayer")) {
                lock.ev$setAlwaysLookAtPlayer(NbtCompat.getBoolean(data, "EvAlwaysLookAtPlayer", false));
            }
            if (data.contains("EvPriceLocked")) {
                lock.ev$setPriceLock(NbtCompat.getBoolean(data, "EvPriceLocked", false));
            }
            if (data.contains("EvKeepTrades")) {
                lock.ev$setKeepTrades(NbtCompat.getBoolean(data, "EvKeepTrades", false));
            }
            if (data.contains("EvEffectsOnActions")) {
                lock.ev$setEffectsOnActions(NbtCompat.getBoolean(data, "EvEffectsOnActions", false));
            }
            if (data.contains("EvContinuousEffect")) {
                lock.ev$setContinuousEffect(NbtCompat.getBoolean(data, "EvContinuousEffect", false));
            }
            if (data.contains("EvContinuousParticleCount")) {
                lock.ev$setContinuousParticleCount(NbtCompat.getInt(data, "EvContinuousParticleCount", 1));
            }
            if (data.contains("EvActionParticle")) {
                lock.ev$setActionParticle(NbtCompat.getString(data, "EvActionParticle", ""));
            }
            if (data.contains("EvContinuousParticle")) {
                lock.ev$setContinuousParticle(NbtCompat.getString(data, "EvContinuousParticle", ""));
            }

            NbtCompound customLevels = NbtCompat.getCompound(data, "EvCustomLevelTrades");
            for (String key : customLevels.getKeys()) {
                try {
                    int level = Integer.parseInt(key);
                    TradeOfferList levelOffers = decodeOffersElement(customLevels.get(key), ops);
                    if (!levelOffers.isEmpty()) {
                        lock.ev$setCustomLevelTrades(level, levelOffers);
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (data.contains("NoAI")) {
            target.setAiDisabled(NbtCompat.getBoolean(data, "NoAI", false));
        }
        if (data.contains("Silent")) {
            target.setSilent(NbtCompat.getBoolean(data, "Silent", false));
        }
        if (data.contains("Invulnerable")) {
            target.setInvulnerable(NbtCompat.getBoolean(data, "Invulnerable", false));
        }
        if (data.contains("Glowing")) {
            target.setGlowing(NbtCompat.getBoolean(data, "Glowing", false));
        }
        if (data.contains("CustomNameVisible")) {
            target.setCustomNameVisible(NbtCompat.getBoolean(data, "CustomNameVisible", false));
        }

        target.setPersistent();
    }

    public static TradeOfferList decodeOffersElement(NbtElement element, DynamicOps<NbtElement> ops) {
        TradeOfferList empty = new TradeOfferList();
        if (element == null) {
            return empty;
        }
        var direct = TradeOfferList.CODEC.parse(ops, element).result();
        if (direct.isPresent() && !direct.get().isEmpty()) {
            return direct.get();
        }
        if (element instanceof NbtCompound compound && compound.contains("Recipes")) {
            NbtElement recipes = compound.get("Recipes");
            var fromRecipes = TradeOfferList.CODEC.parse(ops, recipes).result();
            if (fromRecipes.isPresent()) {
                return fromRecipes.get();
            }
        }
        return empty;
    }
}
