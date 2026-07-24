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

import java.util.ArrayList;
import java.util.List;

public class EffectsScreenHandler extends ScreenHandler {

    private static final int SIZE = 27;

    private static final int SLOT_CONTINUOUS_TOGGLE = 11;
    private static final int SLOT_CONTINUOUS_PARTICLE = 14;
    private static final int SLOT_CONTINUOUS_COUNT = 15;

    private static final int SLOT_APPLY = 22;
    private static final int SLOT_BACK = 26;

    private final VillagerEntity villager;
    private final SimpleInventory menu = new SimpleInventory(SIZE);

    private boolean pendingContinuousEffect;
    private boolean appliedContinuousEffect;

    private String pendingContinuousParticle;
    private String appliedContinuousParticle;

    private int pendingContinuousParticleCount;
    private int appliedContinuousParticleCount;

    private static final List<String> PARTICLE_LIST = new ArrayList<>();

    public static net.minecraft.particle.ParticleEffect getParticleEffect(String idStr) {
        if ("default".equals(idStr)) return null;
        try {
            net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(idStr);
            if (id != null) {
                return (net.minecraft.particle.ParticleEffect) net.minecraft.registry.Registries.PARTICLE_TYPE.get(id);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public EffectsScreenHandler(int syncId, PlayerInventory playerInventory, VillagerEntity villager) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.villager = villager;

        if (PARTICLE_LIST.isEmpty()) {
            PARTICLE_LIST.add("default");
            net.minecraft.registry.Registries.PARTICLE_TYPE.stream().forEach(pt -> {
                if (pt instanceof net.minecraft.particle.SimpleParticleType) {
                    net.minecraft.util.Identifier id = net.minecraft.registry.Registries.PARTICLE_TYPE.getId(pt);
                    if (id != null && !id.getPath().equals("damage_indicator")) {
                        PARTICLE_LIST.add(id.toString());
                    }
                }
            });
        }

        EvVillagerLock lock = (EvVillagerLock) villager;
        this.appliedContinuousEffect = lock.ev$hasContinuousEffect();
        this.pendingContinuousEffect = this.appliedContinuousEffect;

        this.appliedContinuousParticle = lock.ev$getContinuousParticle();
        this.pendingContinuousParticle = this.appliedContinuousParticle;

        this.appliedContinuousParticleCount = lock.ev$getContinuousParticleCount();
        this.pendingContinuousParticleCount = this.appliedContinuousParticleCount;

        fillMenu();

        for (int i = 0; i < SIZE; i++) {
            int x = 8 + (i % 9) * 18;
            int y = 18 + (i / 9) * 18;
            this.addSlot(new LockedSlot(menu, i, x, y));
        }

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                this.addSlot(new Slot(playerInventory, i * 9 + j + 9, 8 + j * 18, 84 + i * 18));
            }
        }
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void fillMenu() {
        for (int i = 0; i < SIZE; i++) {
            if (i == 10 || i == 12 || i == 13 || i == 16) {
                continue;
            }
            ItemStack glass = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponentTypes.CUSTOM_NAME, Text.literal(" "));
            menu.setStack(i, glass);
        }

        refreshContinuousToggle();
        refreshContinuousParticle();
        refreshContinuousCount();

        ItemStack apply = new ItemStack(Items.GOLD_INGOT);
        apply.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.confirm")));
        menu.setStack(SLOT_APPLY, apply);

        ItemStack back = new ItemStack(Items.RED_CONCRETE);
        back.set(DataComponentTypes.CUSTOM_NAME, Text.literal(LanguageManager.tr("button.back")));
        menu.setStack(SLOT_BACK, back);
    }

    private net.minecraft.item.Item getParticleIcon(String particle) {
        if (particle == null || particle.equals("default")) return Items.BARRIER;
        
        if (particle.contains("obsidian_tear") || particle.contains("crying_obsidian")) return Items.CRYING_OBSIDIAN;
        if (particle.contains("obsidian")) return Items.OBSIDIAN;
        if (particle.contains("cherry")) return Items.CHERRY_LEAVES;
        if (particle.contains("spore")) return Items.SPORE_BLOSSOM;
        if (particle.contains("sculk")) return Items.SCULK;
        if (particle.contains("copper")) return Items.COPPER_INGOT;
        if (particle.contains("crimson")) return Items.CRIMSON_FUNGUS;
        if (particle.contains("warped")) return Items.WARPED_FUNGUS;
        if (particle.contains("slime")) return Items.SLIME_BALL;
        if (particle.contains("glow_squid")) return Items.GLOW_INK_SAC;
        if (particle.contains("squid")) return Items.INK_SAC;
        if (particle.contains("honey") || particle.contains("nectar")) return Items.HONEY_BOTTLE;
        if (particle.contains("lava")) return Items.LAVA_BUCKET;
        if (particle.contains("water") || particle.contains("splash") || particle.contains("bubble") || particle.contains("rain")) return Items.WATER_BUCKET;
        if (particle.contains("snow") || particle.contains("ice")) return Items.SNOWBALL;
        if (particle.contains("dust")) return Items.REDSTONE;
        if (particle.contains("ash")) return Items.BASALT;
        if (particle.contains("wax")) return Items.HONEYCOMB;
        if (particle.contains("mycelium")) return Items.MYCELIUM;
        if (particle.contains("firework")) return Items.FIREWORK_ROCKET;
        if (particle.contains("nautilus")) return Items.NAUTILUS_SHELL;
        if (particle.contains("dolphin")) return Items.COD;
        if (particle.contains("fishing")) return Items.FISHING_ROD;
        if (particle.contains("campfire") && particle.contains("soul")) return Items.SOUL_CAMPFIRE;
        if (particle.contains("campfire")) return Items.CAMPFIRE;
        
        if (particle.contains("heart")) return Items.RED_TULIP;
        if (particle.contains("flame") || particle.contains("fire")) return Items.CAMPFIRE;
        if (particle.contains("smoke")) return Items.FLINT_AND_STEEL;
        if (particle.contains("happy")) return Items.EMERALD;
        if (particle.contains("angry")) return Items.FIRE_CHARGE;
        if (particle.contains("soul")) return Items.SOUL_SAND;
        if (particle.contains("portal")) return Items.OBSIDIAN;
        if (particle.contains("end_rod")) return Items.END_ROD;
        if (particle.contains("dragon")) return Items.DRAGON_BREATH;
        if (particle.contains("totem")) return Items.TOTEM_OF_UNDYING;
        if (particle.contains("glow")) return Items.GLOWSTONE_DUST;
        if (particle.contains("enchant")) return Items.ENCHANTING_TABLE;
        if (particle.contains("cloud")) return Items.COBWEB;
        if (particle.contains("note")) return Items.NOTE_BLOCK;
        if (particle.contains("electric") || particle.contains("lightning")) return Items.LIGHTNING_ROD;
        if (particle.contains("explosion")) return Items.TNT;
        if (particle.contains("magic") || particle.contains("witch") || particle.contains("effect")) return Items.POTION;
        if (particle.contains("sweep")) return Items.IRON_SWORD;
        if (particle.contains("crit")) return Items.DIAMOND_SWORD;
        if (particle.contains("damage")) return Items.WOODEN_SWORD;
        if (particle.contains("composter")) return Items.COMPOSTER;
        if (particle.contains("spit")) return Items.LLAMA_SPAWN_EGG;
        if (particle.contains("flash")) return Items.BEACON;
        if (particle.contains("trial_spawner")) return Items.TRIAL_SPAWNER;
        if (particle.contains("vault_connection") || particle.contains("vault")) return Items.VAULT;
        if (particle.contains("ominous")) return Items.TRIAL_KEY;
        if (particle.contains("shriek")) return Items.SCULK_SHRIEKER;
        if (particle.contains("gust")) return Items.WIND_CHARGE;
        
        if (particle.contains("falling")) return Items.GRAVEL;
        if (particle.contains("dripping") || particle.contains("dripstone")) return Items.POINTED_DRIPSTONE;
        if (particle.contains("item")) return Items.APPLE;
        if (particle.contains("block")) return Items.STONE;
        
        return Items.NETHER_STAR;
    }

    private void refreshContinuousToggle() {
        ItemStack stack = new ItemStack(Items.REDSTONE);
        String status = pendingContinuousEffect ? LanguageManager.tr("settings.status.on") : LanguageManager.tr("settings.status.off");
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§eПостоянный эффект: " + status));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
            Text.literal("§7Житель будет постоянно"),
            Text.literal("§7излучать выбранные частицы.")
        )));
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, pendingContinuousEffect);
        menu.setStack(SLOT_CONTINUOUS_TOGGLE, stack);
    }

    private void refreshContinuousParticle() {
        ItemStack stack = new ItemStack(getParticleIcon(pendingContinuousParticle));
        String name = pendingContinuousParticle.equals("default") ? "Без частиц" : pendingContinuousParticle.replace("minecraft:", "").toUpperCase();
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§eПостоянные частицы: §b" + name));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
            Text.literal("§7ЛКМ - Назад, ПКМ - Вперед")
        )));
        menu.setStack(SLOT_CONTINUOUS_PARTICLE, stack);
    }

    private void refreshContinuousCount() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§eКол-во частиц: §a" + pendingContinuousParticleCount));
        stack.set(DataComponentTypes.LORE, new net.minecraft.component.type.LoreComponent(java.util.List.of(
            Text.literal("§7ЛКМ: +1 | Shift+ЛКМ: +10"),
            Text.literal("§7ПКМ: -1 | Shift+ПКМ: -10")
        )));
        menu.setStack(SLOT_CONTINUOUS_COUNT, stack);
    }

    private String cycleParticle(String current, int dir) {
        int idx = PARTICLE_LIST.indexOf(current);
        if (idx == -1) idx = 0;
        idx += dir;
        if (idx < 0) idx = PARTICLE_LIST.size() - 1;
        if (idx >= PARTICLE_LIST.size()) idx = 0;
        return PARTICLE_LIST.get(idx);
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (actionType == SlotActionType.QUICK_MOVE && slotIndex != SLOT_CONTINUOUS_COUNT) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (player instanceof ServerPlayerEntity sp) {
                if (slotIndex == SLOT_CONTINUOUS_TOGGLE) {
                    pendingContinuousEffect = !pendingContinuousEffect;
                    refreshContinuousToggle();
                } else if (slotIndex == SLOT_CONTINUOUS_PARTICLE) {
                    int dir = button == 1 ? 1 : -1;
                    pendingContinuousParticle = cycleParticle(pendingContinuousParticle, dir);
                    refreshContinuousParticle();
                } else if (slotIndex == SLOT_CONTINUOUS_COUNT) {
                    int change = (actionType == SlotActionType.QUICK_MOVE) ? 10 : 1;
                    if (button == 1) {
                        pendingContinuousParticleCount -= change;
                    } else {
                        pendingContinuousParticleCount += change;
                    }
                    if (pendingContinuousParticleCount < 1) pendingContinuousParticleCount = 1;
                    if (pendingContinuousParticleCount > 100) pendingContinuousParticleCount = 100;
                    refreshContinuousCount();
                } else if (slotIndex == SLOT_APPLY) {
                    EvVillagerLock lock = (EvVillagerLock) villager;
                    lock.ev$setEffectsOnActions(false);
                    lock.ev$setContinuousEffect(pendingContinuousEffect);
                    lock.ev$setContinuousParticle(pendingContinuousParticle);
                    lock.ev$setContinuousParticleCount(pendingContinuousParticleCount);

                    appliedContinuousEffect = pendingContinuousEffect;
                    appliedContinuousParticle = pendingContinuousParticle;
                    appliedContinuousParticleCount = pendingContinuousParticleCount;

                    sp.sendMessage(Text.literal("§aНастройки эффектов сохранены!"), true);
                } else if (slotIndex == SLOT_BACK) {
                    sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Text.literal(LanguageManager.tr("menu.settings.title"))));
                    return;
                }
                
                sendContentUpdates();

                if (actionType == SlotActionType.QUICK_MOVE) {
                    for (int i = 0; i < sp.currentScreenHandler.slots.size(); i++) {
                        NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                                sp.currentScreenHandler.syncId, 
                                sp.currentScreenHandler.getRevision(), 
                                i, 
                                sp.currentScreenHandler.slots.get(i).getStack().copy()
                        ));
                    }
                    NetworkCompat.send(sp, new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                            -1, 
                            sp.currentScreenHandler.getRevision(), 
                            -1, 
                            sp.currentScreenHandler.getCursorStack().copy()
                    ));
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
