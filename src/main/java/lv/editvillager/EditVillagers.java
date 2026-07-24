package lv.editvillager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EditVillagers {

	public static void init() {
		CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> register(dispatcher));

		InteractionEvent.INTERACT_ENTITY.register(EditVillagers::onEntityInteract);

		TickEvent.SERVER_POST.register(VillagerCarryHandler::onServerTick);
	}

	private static EventResult onEntityInteract(net.minecraft.entity.player.PlayerEntity player, Entity entity,
			Hand hand) {
		if (player.getEntityWorld().isClient())
			return EventResult.pass();

		if (!(entity instanceof VillagerEntity villager))
			return EventResult.pass();

		if (player.isSneaking() && player instanceof ServerPlayerEntity sp && player.isCreative()) {
			LanguageManager.bind(sp);
			sp.openHandledScreen(
					new SimpleNamedScreenHandlerFactory(
							(syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
							Text.literal(LanguageManager.tr("menu.main.title"))));
			return EventResult.interruptTrue();
		}

		return EventResult.pass();
	}

	private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(
				CommandManager.literal("ev")
						.requires(
								source -> source.getEntity() instanceof ServerPlayerEntity player
										&& player.isCreative())
						.executes(ctx -> runHelp(ctx.getSource()))
						.then(CommandManager.literal("help").executes(ctx -> runHelp(ctx.getSource())))
						.then(CommandManager.literal("lang")
								.then(CommandManager.argument("lang", StringArgumentType.string())
										.executes(ctx -> runLang(ctx.getSource(),
												StringArgumentType.getString(ctx, "lang")))))
						.then(CommandManager.literal("create")
								.then(CommandManager.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> create(ctx.getSource(),
												StringArgumentType.getString(ctx, "name")))))
						.then(CommandManager.literal("edit").executes(ctx -> openEdit(ctx.getSource())))
						.then(CommandManager.literal("trades").executes(ctx -> openTrades(ctx.getSource())))
						.then(CommandManager.literal("carry").executes(ctx -> runCarry(ctx.getSource())))
						.then(CommandManager.literal("name")
								.then(CommandManager.argument("name", StringArgumentType.greedyString())
										.executes(ctx -> runName(ctx.getSource(),
												StringArgumentType.getString(ctx, "name")))))
						.then(CommandManager.literal("tradefiles")
								.then(CommandManager.literal("cancel")
										.executes(ctx -> TradeFilePromptManager.cancel(ctx.getSource())))
								.then(CommandManager.literal("apply")
										.then(CommandManager.argument("file", StringArgumentType.greedyString())
												.executes(ctx -> TradeFilePromptManager.applyFromCommand(
														ctx.getSource(),
														StringArgumentType.getString(ctx, "file")))))));
	}

	private static int runHelp(ServerCommandSource source) {
		if (source.getEntity() instanceof ServerPlayerEntity player) {
			LanguageManager.bind(player);
		}
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.title")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.list")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.create")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.edit")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.trades")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.name")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.carry")), false);
		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.help.lang")), false);
		return 1;
	}

	private static int runLang(ServerCommandSource source, String lang) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.player_only")));
			return 0;
		}
		LanguageManager.bind(player);
		if (LanguageManager.isSupported(lang)) {
			LanguageManager.setLanguage(player, lang);
			source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.success.lang_changed", lang)), false);
			return 1;
		}
		source.sendError(Text.literal(LanguageManager.tr("command.error.invalid_lang")));
		return 0;
	}

	private static int runName(ServerCommandSource source, String name) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.player_only")));
			return 0;
		}

		VillagerEntity villager = getTargetedVillager(player, source.getWorld());
		if (villager == null) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.no_villager")));
			return 0;
		}

		villager.setCustomName(Text.literal(name.replace('&', '§')));
		villager.setCustomNameVisible(true);
		source.sendFeedback(
				() -> Text.literal(LanguageManager.tr("command.success.name_changed", name.replace('&', '§'))),
				false);
		return 1;
	}

	private static int runCarry(ServerCommandSource source) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.player_only")));
			return 0;
		}

		VillagerEntity villager = getTargetedVillager(player, source.getWorld());
		if (villager == null) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.no_villager")));
			return 0;
		}

		VillagerCarryHandler.startCarrying(player, villager);
		return 1;
	}

	private static int create(ServerCommandSource source, String name) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Only players can use this command"));
			return 0;
		}

		ServerWorld world = source.getWorld();

		VillagerEntity newVillager = new VillagerEntity(EntityType.VILLAGER, world);
		newVillager.initialize(world, world.getLocalDifficulty(newVillager.getBlockPos()), SpawnReason.COMMAND,
				null);

		newVillager.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), 0.0f);
		newVillager.setHeadYaw(player.getYaw());
		newVillager.setBodyYaw(player.getYaw());

		newVillager.setCustomName(Text.literal(name.replace('&', '§')));
		newVillager.setCustomNameVisible(false);
		newVillager.setAiDisabled(true);

		newVillager.setSilent(false);
		newVillager.setExperience(1);

		world.spawnEntity(newVillager);

		source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.success.villager_created", name)),
				false);
		return 1;
	}

	private static int openTrades(ServerCommandSource source) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Only players can use this command"));
			return 0;
		}

		VillagerEntity villager = getTargetedVillager(player, source.getWorld());
		if (villager == null) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.no_villager")));
			return 0;
		}

		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new TradesScreenHandler(syncId, inventory, villager),
				Text.literal(LanguageManager.tr("menu.edit_trades.title"))));
		return 1;
	}

	private static int openEdit(ServerCommandSource source) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Only players can use this command"));
			return 0;
		}

		VillagerEntity villager = getTargetedVillager(player, source.getWorld());
		if (villager == null) {
			source.sendError(Text.literal(LanguageManager.tr("command.error.no_villager")));
			return 0;
		}

		player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
				(syncId, inventory, p) -> new EditScreenHandler(syncId, inventory, villager),
				Text.literal(LanguageManager.tr("menu.main.title"))));
		return 1;
	}

	private static VillagerEntity getTargetedVillager(ServerPlayerEntity player, ServerWorld world) {
		Vec3d start = player.getCameraPosVec(1.0f);
		Vec3d direction = player.getRotationVec(1.0f);

		Box box = player.getBoundingBox().expand(5.0);
		VillagerEntity villager = null;
		double closestDot = -1.0;

		for (Entity e : world.getEntitiesByClass(VillagerEntity.class, box, x -> true)) {
			Vec3d entityPos = new Vec3d(e.getX(), e.getY() + e.getHeight() / 2, e.getZ());
			Vec3d toEntity = entityPos.subtract(start);
			double distance = toEntity.length();
			if (distance > 5.0)
				continue;

			Vec3d normalized = toEntity.normalize();
			double dot = direction.dotProduct(normalized);

			if (dot > 0.8 && dot > closestDot) {
				villager = (VillagerEntity) e;
				closestDot = dot;
			}
		}

		return villager;
	}
}
