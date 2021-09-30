package io.github.haykam821.irritaterrun.game.phase;

import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMap;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameResult;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class IrritaterRunWaitingPhase implements PlayerDeathEvent, GamePlayerEvents.Offer, GameActivityEvents.RequestStart {
	private final GameSpace gameSpace;
	private final ServerWorld world;
	private final IrritaterRunMap map;
	private final IrritaterRunConfig config;

	public IrritaterRunWaitingPhase(GameSpace gameSpace, ServerWorld world, IrritaterRunMap map, IrritaterRunConfig config) {
		this.gameSpace = gameSpace;
		this.world = world;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<IrritaterRunConfig> context) {
		IrritaterRunMapBuilder mapBuilder = new IrritaterRunMapBuilder(context.config());
		IrritaterRunMap map = mapBuilder.create();

		RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
			.setGenerator(map.createGenerator(context.server()));

		return context.openWithWorld(worldConfig, (activity, world) -> {
			IrritaterRunWaitingPhase phase = new IrritaterRunWaitingPhase(activity.getGameSpace(), world, map, context.config());
			GameWaitingLobby.addTo(activity, context.config().getPlayerConfig());

			IrritaterRunActivePhase.setRules(activity);
			activity.deny(GameRuleType.PVP);

			// Listeners
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(GameActivityEvents.REQUEST_START, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		IrritaterRunActivePhase.spawn(this.world, this.map, player);
		return ActionResult.FAIL;
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawnPos()).and(() -> {
			offer.player().changeGameMode(GameMode.ADVENTURE);
		});
	}

	@Override
	public GameResult onRequestStart() {
		IrritaterRunActivePhase.open(this.gameSpace, this.world, this.map, this.config);
		return GameResult.ok();
	}
}