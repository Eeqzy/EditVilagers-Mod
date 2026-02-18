package lv.editvillager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class EditVillagers implements ModInitializer {

	@Override
	public void onInitialize() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> register(dispatcher));

		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (world.isClient())
				return ActionResult.PASS;

			if (!(entity instanceof VillagerEntity villager))
				return ActionResult.PASS;

			if (player.isSneaking() && player instanceof ServerPlayerEntity sp && player.isCreative()) {
				sp.openHandledScreen(
						new SimpleNamedScreenHandlerFactory(
								(syncId, inv, p) -> new EditScreenHandler(syncId, inv, villager),
								Text.literal("EditVillagers by Eeqzy")));
				return ActionResult.CONSUME;
			}

			return ActionResult.PASS;
		});

		VillagerCarryHandler.registerTick();
	}

	private void register(CommandDispatcher<ServerCommandSource> dispatcher) {
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
												StringArgumentType.getString(ctx, "name"))))));
	}

	private int runHelp(ServerCommandSource source) {
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

	private int runLang(ServerCommandSource source, String lang) {
		if (lang.equals("ru") || lang.equals("en")) {
			LanguageManager.setLanguage(lang);
			source.sendFeedback(() -> Text.literal(LanguageManager.tr("command.success.lang_changed", lang)), false);
			return 1;
		}
		source.sendError(Text.literal("Invalid language. Use: ru, en"));
		return 0;
	}

	private int runName(ServerCommandSource source, String name) {
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

	private int runCarry(ServerCommandSource source) {
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

	private int create(ServerCommandSource source, String name) {
		if (!(source.getEntity() instanceof ServerPlayerEntity player)) {
			source.sendError(Text.literal("Only players can use this command"));
			return 0;
		}

		ServerWorld world = source.getWorld();

		VillagerEntity newVillager = new VillagerEntity(EntityType.VILLAGER, world);
		if (newVillager != null)
			newVillager.initialize(world, world.getLocalDifficulty(newVillager.getBlockPos()), SpawnReason.COMMAND,
					null);

		if (newVillager != null) {
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

		source.sendError(Text.literal(LanguageManager.tr("command.error.spawn_failed")));
		return 0;
	}

	private int openTrades(ServerCommandSource source) {
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

	private int openEdit(ServerCommandSource source) {
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

	private VillagerEntity getTargetedVillager(ServerPlayerEntity player, ServerWorld world) {
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
