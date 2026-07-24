package lv.editvillager;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class SettingScreenHandler extends ScreenHandler {

    private static final int SIZE = 45;

    private static final int SLOT_LEVEL = 2;
    private static final int SLOT_IMMORTALITY = 3;
    private static final int SLOT_NAME_TAG = 4;
    private static final int SLOT_LOOK_AT_PLAYER = 5;
    private static final int SLOT_GLOWING = 6;
    private static final int SLOT_FREEZE = 8;
    private static final int SLOT_SILENT = 11;
    private static final int SLOT_ALIGN = 12;
    private static final int SLOT_ROTATE = 13;
    private static final int SLOT_RESET_XP = 14;
    private static final int SLOT_PRICE_CHANGE = 15;
    private static final int SLOT_EFFECTS = 17;
    private static final int SLOT_XP_DROP = 20;
    private static final int SLOT_BABY = 21;
    private static final int SLOT_SUM = 26;
    private static final int SLOT_NAV_PROFESSION = 18;
    private static final int SLOT_NAV_TRADES = 27;
    private static final int SLOT_APPLY = 40;
    private static final int SLOT_BACK = 44;

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(SIZE);

    private int appliedLevel;
    private int pendingLevel;

    private boolean appliedImmortality;
    private boolean pendingImmortality;

    private boolean appliedNameVisible;
    private boolean pendingNameVisible;

    private boolean appliedLookAtPlayer;
    private boolean pendingLookAtPlayer;

    private boolean appliedGlowing;
    private boolean pendingGlowing;

    private boolean appliedFreeze;
    private boolean pendingFreeze;

    private boolean appliedSilent;
    private boolean pendingSilent;

    private boolean pendingResetXp;

    private boolean appliedPriceLocked;
    private boolean pendingPriceLocked;

    private boolean appliedXpDropEnabled;
    private boolean pendingXpDropEnabled;

    private boolean appliedBaby;
    private boolean pendingBaby;
    
    private boolean pendingAlign;

    public SettingScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X5, syncId);
        this.villager = villager;

        this.pendingResetXp = false;

        boolean locked = ((EvVillagerLock) villager).ev$areTradesLocked();
        int realLevel = ReflectionUtils.getLevel(villager.getVillagerData());
        this.appliedLevel = locked ? realLevel : 0;
        this.pendingLevel = this.appliedLevel;

        this.appliedImmortality = villager.isInvulnerable();
        this.pendingImmortality = this.appliedImmortality;

        this.appliedNameVisible = villager.isCustomNameVisible();
        this.pendingNameVisible = this.appliedNameVisible;

        this.appliedLookAtPlayer = ((EvVillagerLock) villager).ev$shouldAlwaysLookAtPlayer();
        this.pendingLookAtPlayer = this.appliedLookAtPlayer;

        this.appliedGlowing = villager.isGlowing();
        this.pendingGlowing = this.appliedGlowing;

        this.appliedFreeze = villager.isAiDisabled();
        this.pendingFreeze = this.appliedFreeze;

        this.appliedSilent = villager.isSilent();
        this.pendingSilent = this.appliedSilent;

        this.appliedPriceLocked = ((EvVillagerLock) villager).ev$isPriceLocked();
        this.pendingPriceLocked = this.appliedPriceLocked;

        this.appliedXpDropEnabled = ((EvVillagerLock) villager).ev$isXpDropEnabled();
        this.pendingXpDropEnabled = this.appliedXpDropEnabled;

        this.appliedBaby = villager.isBaby();
        this.pendingBaby = this.appliedBaby;

        fillMenu();

        for (int i = 0; i < SIZE; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 120 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 178));
        }
    }

    private static void noName(ItemStack stack) {
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
    }

    private void safeSet(int slot, ItemStack stack) {
        if (slot >= 0 && slot < menu.size()) {
            menu.setStack(slot, stack);
        }
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            menu.setStack(i, ItemStack.EMPTY);
        }

        for (int i = 0; i < SIZE; i++) {
            if (i == 14 || i == 15 || i == 20 || i == 21 || i == 22 || i == 23 || i == 24) {
                continue;
            }
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            noName(glass);
            safeSet(i, glass);
        }

        safeSet(SLOT_LEVEL, createLevelStack());

        safeSet(SLOT_IMMORTALITY, createImmortalityStack());

        safeSet(SLOT_NAME_TAG, createNameVisibilityStack());

        safeSet(SLOT_LOOK_AT_PLAYER, createLookAtPlayerStack());

        safeSet(SLOT_GLOWING, createGlowingStack());

        safeSet(SLOT_FREEZE, createFreezeStack());

        safeSet(SLOT_SILENT, createSilentStack());

        safeSet(SLOT_ALIGN, createAlignStack());

        safeSet(SLOT_ROTATE, createRotateStack());

        safeSet(SLOT_RESET_XP, createResetXpStack());

        safeSet(SLOT_PRICE_CHANGE, createPriceChangeStack());

        safeSet(SLOT_XP_DROP, createXpDropStack());

        safeSet(SLOT_BABY, createBabyStack());

        safeSet(SLOT_EFFECTS, createEffectsStack());

        ItemStack sumInit = new ItemStack(Items.NETHERITE_SCRAP);
        sumInit.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.button.sum")));
        safeSet(SLOT_SUM, sumInit);

        ItemStack navProf = new ItemStack(Items.LECTERN);
        navProf.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.nav.professions")));
        safeSet(SLOT_NAV_PROFESSION, navProf);

        ItemStack navTrades = new ItemStack(Items.EMERALD);
        navTrades.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.nav.trades")));
        safeSet(SLOT_NAV_TRADES, navTrades);

        safeSet(SLOT_NAV_TRADES, navTrades);

        ItemStack navBiomes = new ItemStack(Items.SAND);
        navBiomes.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.nav.biomes")));
        safeSet(36, navBiomes);

        ItemStack apply = new ItemStack(Items.GOLD_INGOT);
        apply.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.confirm")));
        safeSet(SLOT_APPLY, apply);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        safeSet(SLOT_BACK, back);
    }

    private ItemStack createLevelStack() {
        ItemStack stack = new ItemStack(Items.ENCHANTING_TABLE);

        String levelName = getLevelName(pendingLevel);
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.level", levelName)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.level")))));

        boolean matchesApplied = (pendingLevel == appliedLevel);
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, matchesApplied);

        return stack;
    }

    private ItemStack createImmortalityStack() {
        ItemStack stack = new ItemStack(Items.TOTEM_OF_UNDYING);

        String status = pendingImmortality ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.immortality", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.immortality")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingImmortality);

        return stack;
    }

    private String getLevelName(int level) {
        return switch (level) {
            case 1 -> LanguageManager.tr("level.novice");
            case 2 -> LanguageManager.tr("level.apprentice");
            case 3 -> LanguageManager.tr("level.journeyman");
            case 4 -> LanguageManager.tr("level.expert");
            case 5 -> LanguageManager.tr("level.master");
            case 0 -> LanguageManager.tr("level.default");
            default -> LanguageManager.tr("level.novice");
        };
    }

    private void refreshLevelVisual() {
        safeSet(SLOT_LEVEL, createLevelStack());
        sendContentUpdates();
    }

    private void refreshImmortalityVisual() {
        safeSet(SLOT_IMMORTALITY, createImmortalityStack());
        sendContentUpdates();
    }

    private ItemStack createNameVisibilityStack() {
        ItemStack stack = new ItemStack(Items.NAME_TAG);

        String status = pendingNameVisible ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.nametag", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.nametag")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingNameVisible);

        return stack;
    }

    private void refreshNameVisibilityVisual() {
        safeSet(SLOT_NAME_TAG, createNameVisibilityStack());
        sendContentUpdates();
    }

    private ItemStack createLookAtPlayerStack() {
        ItemStack stack = new ItemStack(Items.ENDER_EYE);

        String status = pendingLookAtPlayer ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.lookat", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.lookat")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingLookAtPlayer);

        return stack;
    }

    private void refreshLookAtPlayerVisual() {
        safeSet(SLOT_LOOK_AT_PLAYER, createLookAtPlayerStack());
        sendContentUpdates();
    }

    private ItemStack createGlowingStack() {
        ItemStack stack = new ItemStack(Items.GLOWSTONE_DUST);

        String status = pendingGlowing ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.glowing", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.glowing")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingGlowing);

        return stack;
    }

    private void refreshGlowingVisual() {
        safeSet(SLOT_GLOWING, createGlowingStack());
        sendContentUpdates();
    }

    private ItemStack createFreezeStack() {
        ItemStack stack = new ItemStack(Items.REDSTONE);

        String status = pendingFreeze ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.noai", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.noai")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingFreeze);

        return stack;
    }

    private void refreshFreezeVisual() {
        safeSet(SLOT_FREEZE, createFreezeStack());
        sendContentUpdates();
    }

    private ItemStack createSilentStack() {
        ItemStack stack = new ItemStack(Items.JUKEBOX);

        String status = !pendingSilent ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.silent", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.silent")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingSilent);

        return stack;
    }

    private void refreshSilentVisual() {
        safeSet(SLOT_SILENT, createSilentStack());
        sendContentUpdates();
    }

    private ItemStack createAlignStack() {
        ItemStack stack = new ItemStack(Items.PISTON);

        String status = pendingAlign ? LanguageManager.tr("settings.status.yes")
                : LanguageManager.tr("settings.status.no");
        
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.align", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.align")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingAlign);

        return stack;
    }

    private void refreshAlignVisual() {
        safeSet(SLOT_ALIGN, createAlignStack());
        sendContentUpdates();
    }

    private ItemStack createRotateStack() {
        ItemStack stack = new ItemStack(Items.COMPASS);
        String dir = getCardinalDirection(villager.getYaw());
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.rotate", dir)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.rotate")))));
        return stack;
    }

    private ItemStack createResetXpStack() {
        ItemStack stack = new ItemStack(Items.FIREWORK_STAR);
        String status = pendingResetXp ? LanguageManager.tr("settings.status.yes")
                : LanguageManager.tr("settings.status.no");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.resetxp", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.resetxp")))));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingResetXp);
        return stack;
    }

    private void refreshResetXpVisual() {
        safeSet(SLOT_RESET_XP, createResetXpStack());
        sendContentUpdates();
    }

    private ItemStack createPriceChangeStack() {
        ItemStack stack = new ItemStack(Items.EMERALD);
        String status = !pendingPriceLocked ? LanguageManager.tr("settings.status.on")
                : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.pricelock", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.pricelock")))));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingPriceLocked);
        return stack;
    }

    private void refreshPriceChangeVisual() {
        safeSet(SLOT_PRICE_CHANGE, createPriceChangeStack());

        ItemStack sum = new ItemStack(Items.NETHERITE_SCRAP);
        sum.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("settings.button.sum")));
        safeSet(SLOT_SUM, sum);

        sendContentUpdates();
    }

    private ItemStack createXpDropStack() {
        ItemStack stack = new ItemStack(Items.EXPERIENCE_BOTTLE);

        String status = pendingXpDropEnabled ? LanguageManager.tr("settings.status.on") : LanguageManager.tr("settings.status.off");

        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.noxp", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.noxp")))));

        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, !pendingXpDropEnabled);
        return stack;
    }

    private ItemStack createBabyStack() {
        ItemStack stack = new ItemStack(Items.ARMOR_STAND);
        String status = pendingBaby
                ? LanguageManager.tr("settings.status.baby")
                : LanguageManager.tr("settings.status.adult");
        stack.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal(LanguageManager.tr("settings.button.baby", status)));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(
                java.util.List.of(Text.literal(LanguageManager.tr("settings.lore.baby")))));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingBaby);
        return stack;
    }

    private ItemStack createEffectsStack() {
        ItemStack stack = new ItemStack(Items.WHITE_DYE);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("trades.button.effects")));
        return stack;
    }

    private void refreshXpDropVisual() {
        safeSet(SLOT_XP_DROP, createXpDropStack());
        sendContentUpdates();
    }

    private void refreshBabyVisual() {
        safeSet(SLOT_BABY, createBabyStack());
        sendContentUpdates();
    }

    private void applyChanges(ServerPlayerEntity sp) {
        if (pendingLevel != appliedLevel) {
            EvVillagerLock lock = (EvVillagerLock) villager;
            if (pendingLevel == 0) {
                lock.ev$setTradesLocked(false);
                sp.sendMessage(Text.literal(LanguageManager.tr("settings.msg.level_default")), true);
            } else {
                villager.setVillagerData(villager.getVillagerData().withLevel(pendingLevel));
                lock.ev$setTradesLocked(true);
                sp.sendMessage(
                        Text.literal(LanguageManager.tr("settings.msg.level_fixed", getLevelName(pendingLevel))),
                        true);
            }
            appliedLevel = pendingLevel;
        }

        if (pendingResetXp) {
            villager.setExperience(1);
            villager.setVillagerData(villager.getVillagerData().withLevel(1));
            
            EvVillagerLock lock = (EvVillagerLock) villager;

            net.minecraft.village.TradeOfferList customL1 = lock.ev$getCustomLevelTrades(1);
            if (customL1 != null && !customL1.isEmpty()) {
                net.minecraft.village.TradeOfferList newOffers = new net.minecraft.village.TradeOfferList();
                 for (net.minecraft.village.TradeOffer o : customL1) {
                     newOffers.add(o.copy());
                 }
                 lock.ev$forceSetOffers(newOffers);
            } else {
                net.minecraft.village.TradeOfferList offers = villager.getOffers();
                 while (offers.size() > 2) {
                     offers.remove(offers.size() - 1);
                 }
                 villager.setOffers(offers);
            }

            for (net.minecraft.village.TradeOffer offer : villager.getOffers()) {
                offer.resetUses();
            }

            lock.ev$setProfessionLocked(false);

            this.appliedLevel = 1;
            this.pendingLevel = 1;

            pendingResetXp = false;
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.msg.xp_reset")),
                    true);
            refreshResetXpVisual();
            refreshLevelVisual();
        }

        if (pendingImmortality != appliedImmortality) {
            villager.setInvulnerable(pendingImmortality);
            appliedImmortality = pendingImmortality;
            String status = appliedImmortality ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.immortality", status)), true);
        }

        if (pendingNameVisible != appliedNameVisible) {
            villager.setCustomNameVisible(pendingNameVisible);
            appliedNameVisible = pendingNameVisible;
            String status = appliedNameVisible ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.nametag", status)), true);
        }

        if (pendingLookAtPlayer != appliedLookAtPlayer) {
            ((EvVillagerLock) villager).ev$setAlwaysLookAtPlayer(pendingLookAtPlayer);
            appliedLookAtPlayer = pendingLookAtPlayer;
            String status = appliedLookAtPlayer ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.lookat", status)), true);
        }

        if (pendingGlowing != appliedGlowing) {
            villager.setGlowing(pendingGlowing);
            appliedGlowing = pendingGlowing;
            String status = appliedGlowing ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.glowing", status)), true);
        }

        if (pendingFreeze != appliedFreeze) {
            villager.setAiDisabled(pendingFreeze);
            appliedFreeze = pendingFreeze;
            String status = appliedFreeze ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.noai", status)), true);
        }

        if (pendingSilent != appliedSilent) {
            villager.setSilent(pendingSilent);
            appliedSilent = pendingSilent;
            String status = !appliedSilent ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.silent", status)), true);
        }

        if (pendingPriceLocked != appliedPriceLocked) {
            ((EvVillagerLock) villager).ev$setPriceLock(pendingPriceLocked);
            appliedPriceLocked = pendingPriceLocked;
            String status = !appliedPriceLocked ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.pricelock", status)), true);
        }

        if (pendingXpDropEnabled != appliedXpDropEnabled) {
            ((EvVillagerLock) villager).ev$setXpDropEnabled(pendingXpDropEnabled);
            appliedXpDropEnabled = pendingXpDropEnabled;

            for (net.minecraft.village.TradeOffer offer : villager.getOffers()) {
                ReflectionUtils.setRewardExp(offer, pendingXpDropEnabled);
            }

            String status = appliedXpDropEnabled ? LanguageManager.tr("settings.status.enabled")
                    : LanguageManager.tr("settings.status.disabled");
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.button.noxp", status)), true);
        }

        if (pendingBaby != appliedBaby) {
            villager.setBaby(pendingBaby);
            ReflectionUtils.setAgeLocked(villager, pendingBaby);
            appliedBaby = pendingBaby;
        }

        if (pendingAlign) {
            double targetX = Math.floor(villager.getX()) + 0.5;
            double targetZ = Math.floor(villager.getZ()) + 0.5;
            double currentY = villager.getY();
            double foundY = currentY;
            boolean foundGround = false;

            net.minecraft.world.World world = villager.getEntityWorld();
            net.minecraft.util.math.BlockPos.Mutable mutablePos = new net.minecraft.util.math.BlockPos.Mutable();

            for (int yOffset = 0; yOffset >= -10; yOffset--) {
                int checkY = (int) Math.floor(currentY) + yOffset;
                mutablePos.set(targetX, checkY, targetZ);
                net.minecraft.block.BlockState state = world.getBlockState(mutablePos);

                net.minecraft.util.shape.VoxelShape shape = state.getCollisionShape(world, mutablePos, net.minecraft.block.ShapeContext.of(villager));
                if (!shape.isEmpty()) {
                    foundY = checkY + shape.getMax(net.minecraft.util.math.Direction.Axis.Y);
                    foundGround = true;
                    break;
                }
            }

            if (foundGround) {
                villager.refreshPositionAndAngles(targetX, foundY, targetZ, villager.getYaw(),
                        villager.getPitch());
            } else {
                villager.refreshPositionAndAngles(targetX, currentY, targetZ, villager.getYaw(),
                        villager.getPitch());
            }
            
            pendingAlign = false;
            sp.sendMessage(Text.literal(LanguageManager.tr("settings.msg.aligned")), true);
            refreshAlignVisual();
        }

        refreshLevelVisual();
        refreshImmortalityVisual();
        refreshNameVisibilityVisual();
        refreshLookAtPlayerVisual();
        refreshGlowingVisual();
        refreshFreezeVisual();
        refreshSilentVisual();
        refreshPriceChangeVisual();
        refreshXpDropVisual();
    }

    private String getCardinalDirection(float yaw) {
        float rot = (yaw % 360);
        if (rot < 0)
            rot += 360;

        if (rot >= 337.5 || rot < 22.5)
            return LanguageManager.tr("dir.south");
        if (rot >= 22.5 && rot < 67.5)
            return LanguageManager.tr("dir.southwest");
        if (rot >= 67.5 && rot < 112.5)
            return LanguageManager.tr("dir.west");
        if (rot >= 112.5 && rot < 157.5)
            return LanguageManager.tr("dir.northwest");
        if (rot >= 157.5 && rot < 202.5)
            return LanguageManager.tr("dir.north");
        if (rot >= 202.5 && rot < 247.5)
            return LanguageManager.tr("dir.northeast");
        if (rot >= 247.5 && rot < 292.5)
            return LanguageManager.tr("dir.east");
        if (rot >= 292.5 && rot < 337.5)
            return LanguageManager.tr("dir.southeast");
        return LanguageManager.tr("dir.south");
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.QUICK_MOVE) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (player instanceof ServerPlayerEntity sp) {

                if (slotIndex == SLOT_LEVEL) {
                    pendingLevel++;
                    if (pendingLevel > 5) {
                        pendingLevel = 0;
                    }
                    refreshLevelVisual();
                    return;
                }

                if (slotIndex == SLOT_IMMORTALITY) {
                    pendingImmortality = !pendingImmortality;
                    refreshImmortalityVisual();
                    return;
                }

                if (slotIndex == SLOT_NAME_TAG) {
                    pendingNameVisible = !pendingNameVisible;
                    refreshNameVisibilityVisual();
                    return;
                }

                if (slotIndex == SLOT_LOOK_AT_PLAYER) {
                    pendingLookAtPlayer = !pendingLookAtPlayer;
                    refreshLookAtPlayerVisual();
                    return;
                }

                if (slotIndex == SLOT_GLOWING) {
                    pendingGlowing = !pendingGlowing;
                    refreshGlowingVisual();
                    return;
                }

                if (slotIndex == SLOT_FREEZE) {
                    pendingFreeze = !pendingFreeze;
                    refreshFreezeVisual();
                    return;
                }

                if (slotIndex == SLOT_XP_DROP) {
                    pendingXpDropEnabled = !pendingXpDropEnabled;
                    refreshXpDropVisual();
                    return;
                }

                if (slotIndex == SLOT_BABY) {
                    pendingBaby = !pendingBaby;
                    refreshBabyVisual();
                    return;
                }

                if (slotIndex == SLOT_SILENT) {
                    pendingSilent = !pendingSilent;
                    refreshSilentVisual();
                    return;
                }

                if (slotIndex == SLOT_ALIGN) {
                    pendingAlign = !pendingAlign;
                    refreshAlignVisual();
                    return;
                }

                if (slotIndex == SLOT_ROTATE) {
                    float currentYaw = villager.getYaw();
                    float newYaw = Math.round((currentYaw + 45.0f) / 45.0f) * 45.0f;
                    newYaw = newYaw % 360.0f;
                    if (newYaw < 0)
                        newYaw += 360.0f;

                    villager.refreshPositionAndAngles(villager.getX(), villager.getY(), villager.getZ(), newYaw,
                            villager.getPitch());
                    villager.setHeadYaw(newYaw);
                    villager.setBodyYaw(newYaw);

                    safeSet(SLOT_ROTATE, createRotateStack());
                    sendContentUpdates();

                    String dirName = getCardinalDirection(newYaw);
                    sp.sendMessage(
                            Text.literal(LanguageManager.tr("settings.msg.direction", dirName)),
                            true);
                    return;
                }

                if (slotIndex == SLOT_RESET_XP) {
                    pendingResetXp = !pendingResetXp;
                    refreshResetXpVisual();
                    return;
                }

                if (slotIndex == SLOT_PRICE_CHANGE) {
                    pendingPriceLocked = !pendingPriceLocked;
                    refreshPriceChangeVisual();
                    return;
                }

                if (slotIndex == SLOT_NAV_PROFESSION) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new ProfessionScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.professions.title"))));
                    return;
                }

                if (slotIndex == SLOT_NAV_TRADES) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new TradesScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.trades.title"))));
                    return;
                }

                if (slotIndex == 36) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new BiomsScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.biomes.title"))));
                    return;
                }

                if (slotIndex == SLOT_SUM) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new SumScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.sum.title"))));
                    return;
                }

                if (slotIndex == SLOT_EFFECTS) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new EffectsScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.effects.title"))));
                    return;
                }

                if (slotIndex == SLOT_APPLY) {
                    applyChanges(sp);
                    return;
                }

                if (slotIndex == SLOT_BACK) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.main.title"))));
                    return;
                }
            }
            return;
        }

        super.onSlotClick(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleInventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity playerEntity) {
            return false;
        }
    }
}
