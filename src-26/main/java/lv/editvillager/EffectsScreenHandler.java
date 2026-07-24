package lv.editvillager;

import lv.editvillager.McCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.SimpleParticleType;

import java.util.ArrayList;
import java.util.List;

public class EffectsScreenHandler extends AbstractContainerMenu {

    private static final int SIZE = 27;

    private static final int SLOT_CONTINUOUS_TOGGLE = 11;
    private static final int SLOT_CONTINUOUS_PARTICLE = 14;
    private static final int SLOT_CONTINUOUS_COUNT = 15;

    private static final int SLOT_APPLY = 22;
    private static final int SLOT_BACK = 26;

    private final Villager villager;
    private final SimpleContainer menu = new SimpleContainer(SIZE);

    private boolean pendingContinuousEffect;
    private boolean appliedContinuousEffect;

    private String pendingContinuousParticle;
    private String appliedContinuousParticle;

    private int pendingContinuousParticleCount;
    private int appliedContinuousParticleCount;

    private static final List<String> PARTICLE_LIST = new ArrayList<>();

    public static ParticleOptions getParticleEffect(String idStr) {
        if ("default".equals(idStr)) return null;
        try {
            Identifier id = Identifier.tryParse(idStr);
            if (id != null) {
                return (ParticleOptions) BuiltInRegistries.PARTICLE_TYPE.getValue(id);
            }
        } catch (Exception ignored) {}
        return null;
    }

    public EffectsScreenHandler(int syncId, Inventory playerInventory, Villager villager) {
        super(MenuType.GENERIC_9x3, syncId);
        this.villager = villager;
        LanguageManager.bind(playerInventory.player);

        if (PARTICLE_LIST.isEmpty()) {
            PARTICLE_LIST.add("default");
            BuiltInRegistries.PARTICLE_TYPE.stream().forEach(pt -> {
                if (pt instanceof SimpleParticleType) {
                    Identifier id = BuiltInRegistries.PARTICLE_TYPE.getKey(pt);
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
            ItemStack glass = new ItemStack(McCompat.GRAY_STAINED_GLASS_PANE);
            glass.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
            menu.setItem(i, glass);
        }

        refreshContinuousToggle();
        refreshContinuousParticle();
        refreshContinuousCount();

        ItemStack apply = new ItemStack(Items.GOLD_INGOT);
        apply.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.confirm")));
        menu.setItem(SLOT_APPLY, apply);

        ItemStack back = new ItemStack(McCompat.RED_CONCRETE);
        back.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("button.back")));
        menu.setItem(SLOT_BACK, back);
    }

    private net.minecraft.world.item.Item getParticleIcon(String particle) {
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
        if (particle.contains("electric") || particle.contains("lightning")) return McCompat.LIGHTNING_ROD;
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
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("effects.continuous.toggle", status)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
            Component.literal(LanguageManager.tr("effects.continuous.lore.1")),
            Component.literal(LanguageManager.tr("effects.continuous.lore.2"))
        )));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, pendingContinuousEffect);
        menu.setItem(SLOT_CONTINUOUS_TOGGLE, stack);
    }

    private void refreshContinuousParticle() {
        ItemStack stack = new ItemStack(getParticleIcon(pendingContinuousParticle));
        String name = pendingContinuousParticle.equals("default")
                ? LanguageManager.tr("effects.particle.none")
                : pendingContinuousParticle.replace("minecraft:", "").toUpperCase();
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("effects.continuous.particle", name)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
            Component.literal(LanguageManager.tr("effects.continuous.cycle"))
        )));
        menu.setItem(SLOT_CONTINUOUS_PARTICLE, stack);
    }

    private void refreshContinuousCount() {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(LanguageManager.tr("effects.continuous.count", pendingContinuousParticleCount)));
        stack.set(DataComponents.LORE, new net.minecraft.world.item.component.ItemLore(java.util.List.of(
            Component.literal(LanguageManager.tr("effects.continuous.count.lore.1")),
            Component.literal(LanguageManager.tr("effects.continuous.count.lore.2"))
        )));
        menu.setItem(SLOT_CONTINUOUS_COUNT, stack);
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
    public void clicked(int slotIndex, int button, ContainerInput actionType, Player player) {
        LanguageManager.bind(player);
        if (actionType == ContainerInput.QUICK_MOVE && slotIndex != SLOT_CONTINUOUS_COUNT) {
            ReflectionUtils.handleQuickMoveSync(actionType, player);
            return;
        }
        if (slotIndex >= 0 && slotIndex < SIZE) {
            if (player instanceof ServerPlayer sp) {
                if (slotIndex == SLOT_CONTINUOUS_TOGGLE) {
                    pendingContinuousEffect = !pendingContinuousEffect;
                    refreshContinuousToggle();
                } else if (slotIndex == SLOT_CONTINUOUS_PARTICLE) {
                    int dir = button == 1 ? 1 : -1;
                    pendingContinuousParticle = cycleParticle(pendingContinuousParticle, dir);
                    refreshContinuousParticle();
                } else if (slotIndex == SLOT_CONTINUOUS_COUNT) {
                    int change = (actionType == ContainerInput.QUICK_MOVE) ? 10 : 1;
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
                    lock.ev$setContinuousEffect(pendingContinuousEffect);
                    lock.ev$setContinuousParticle(pendingContinuousParticle);
                    lock.ev$setContinuousParticleCount(pendingContinuousParticleCount);

                    appliedContinuousEffect = pendingContinuousEffect;
                    appliedContinuousParticle = pendingContinuousParticle;
                    appliedContinuousParticleCount = pendingContinuousParticleCount;

                    sp.sendSystemMessage(Component.literal(LanguageManager.tr("msg.effects_saved")), true);
                } else if (slotIndex == SLOT_BACK) {
                    sp.openMenu(new SimpleMenuProvider(
                            (syncId, inv, p) -> new SettingScreenHandler(syncId, inv, villager),
                            Component.literal(LanguageManager.tr("menu.settings.title"))));
                    return;
                }
                
                broadcastChanges();

                if (actionType == ContainerInput.QUICK_MOVE) {
                    for (int i = 0; i < sp.containerMenu.slots.size(); i++) {
                        sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                                sp.containerMenu.containerId, 
                                sp.containerMenu.getStateId(), 
                                i, 
                                sp.containerMenu.slots.get(i).getItem().copy()
                        ));
                    }
                    sp.connection.send(new net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket(
                            -1, 
                            sp.containerMenu.getStateId(), 
                            -1, 
                            sp.containerMenu.getCarried().copy()
                    ));
                }
            }
            return;
        }

        super.clicked(slotIndex, button, actionType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slot) {
        ReflectionUtils.forceSyncScreen(player);
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    private static class LockedSlot extends Slot {
        public LockedSlot(SimpleContainer inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player playerEntity) {
            return false;
        }
    }
}


