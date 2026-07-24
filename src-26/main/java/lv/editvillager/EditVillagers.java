package lv.editvillager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EditVillagers {

    public static void init() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));

        InteractionEvent.INTERACT_ENTITY.register(EditVillagers::onEntityInteract);

        TickEvent.SERVER_POST.register(VillagerCarryHandler::onServerTick);
        TickEvent.SERVER_POST.register(EditVillagers::tickOpenEditMenus);
    }

    private static void tickOpenEditMenus(net.minecraft.server.MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.containerMenu instanceof EditScreenHandler edit) {
                edit.tickAddonRowAnimation();
            }
        }
    }

    private static EventResult onEntityInteract(net.minecraft.world.entity.player.Player player, Entity entity,
            InteractionHand hand) {
        if (player.level().isClientSide())
            return EventResult.pass();

        if (!(entity instanceof Villager villager))
            return EventResult.pass();

        if (player.isShiftKeyDown() && player instanceof ServerPlayer sp && player.isCreative()) {
            LanguageManager.bind(sp);
            sp.openMenu(new SimpleMenuProvider(
                    (syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
                    Component.literal(LanguageManager.tr("menu.main.title"))));
            return EventResult.interruptTrue();
        }

        return EventResult.pass();
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("ev")
                        .requires(source -> source.getEntity() instanceof ServerPlayer player
                                && player.isCreative())
                        .executes(ctx -> runHelp(ctx.getSource()))
                        .then(Commands.literal("help").executes(ctx -> runHelp(ctx.getSource())))
                        .then(Commands.literal("lang")
                                .then(Commands.argument("lang", StringArgumentType.string())
                                        .executes(ctx -> runLang(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "lang")))))
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("edit").executes(ctx -> openEdit(ctx.getSource())))
                        .then(Commands.literal("trades").executes(ctx -> openTrades(ctx.getSource())))
                        .then(Commands.literal("carry").executes(ctx -> runCarry(ctx.getSource())))
                        .then(Commands.literal("name")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> runName(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("tradefiles")
                                .then(Commands.literal("cancel")
                                        .executes(ctx -> TradeFilePromptManager.cancel(ctx.getSource())))));
    }

    private static int runHelp(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            LanguageManager.bind(player);
        }
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.title")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.list")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.create")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.edit")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.trades")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.name")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.carry")), false);
        source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.help.lang")), false);
        return 1;
    }

    private static int runLang(CommandSourceStack source, String lang) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);
        if (LanguageManager.isSupported(lang)) {
            LanguageManager.setLanguage(player, lang);
            source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.success.lang_changed", lang)), false);
            return 1;
        }
        source.sendFailure(Component.literal(LanguageManager.tr("command.error.invalid_lang")));
        return 0;
    }

    private static int runName(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);

        Villager villager = getTargetedVillager(player, source.getLevel());
        if (villager == null) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.no_villager")));
            return 0;
        }

        villager.setCustomName(Component.literal(name.replace('&', '§')));
        villager.setCustomNameVisible(true);
        source.sendSuccess(
                () -> Component.literal(LanguageManager.tr("command.success.name_changed", name.replace('&', '§'))),
                false);
        return 1;
    }

    private static int runCarry(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);

        Villager villager = getTargetedVillager(player, source.getLevel());
        if (villager == null) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.no_villager")));
            return 0;
        }

        VillagerCarryHandler.startCarrying(player, villager);
        return 1;
    }

    private static int create(CommandSourceStack source, String name) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);

        ServerLevel world = source.getLevel();

        Villager newVillager = new Villager(McCompat.VILLAGER, world);
        newVillager.finalizeSpawn(world, world.getCurrentDifficultyAt(newVillager.blockPosition()),
                EntitySpawnReason.COMMAND, null);

        newVillager.absSnapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0f);
        newVillager.setYHeadRot(player.getYRot());
        newVillager.setYBodyRot(player.getYRot());

        newVillager.setCustomName(Component.literal(name.replace('&', '§')));
        newVillager.setCustomNameVisible(false);
        newVillager.setNoAi(true);

        newVillager.setSilent(false);
        newVillager.setVillagerXp(1);

        if (world.addFreshEntity(newVillager)) {
            source.sendSuccess(() -> Component.literal(LanguageManager.tr("command.success.villager_created", name)),
                    false);
            return 1;
        }

        source.sendFailure(Component.literal(LanguageManager.tr("command.error.spawn_failed")));
        return 0;
    }

    private static int openTrades(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);

        Villager villager = getTargetedVillager(player, source.getLevel());
        if (villager == null) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.no_villager")));
            return 0;
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new TradesScreenHandler(syncId, inventory, villager),
                Component.literal(LanguageManager.tr("menu.edit_trades.title"))));
        return 1;
    }

    private static int openEdit(CommandSourceStack source) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.player_only")));
            return 0;
        }
        LanguageManager.bind(player);

        Villager villager = getTargetedVillager(player, source.getLevel());
        if (villager == null) {
            source.sendFailure(Component.literal(LanguageManager.tr("command.error.no_villager")));
            return 0;
        }

        player.openMenu(new SimpleMenuProvider(
                (syncId, inventory, p) -> new EditScreenHandler(syncId, inventory, villager),
                Component.literal(LanguageManager.tr("menu.main.title"))));
        return 1;
    }

    private static Villager getTargetedVillager(ServerPlayer player, ServerLevel world) {
        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle();

        AABB box = player.getBoundingBox().inflate(5.0);
        Villager villager = null;
        double closestDot = -1.0;

        for (Entity e : world.getEntitiesOfClass(Villager.class, box, x -> true)) {
            Vec3 entityPos = new Vec3(e.getX(), e.getY() + e.getBbHeight() / 2, e.getZ());
            Vec3 toEntity = entityPos.subtract(start);
            double distance = toEntity.length();
            if (distance > 5.0)
                continue;

            Vec3 normalized = toEntity.normalize();
            double dot = direction.dot(normalized);

            if (dot > 0.8 && dot > closestDot) {
                villager = (Villager) e;
                closestDot = dot;
            }
        }

        return villager;
    }
}
