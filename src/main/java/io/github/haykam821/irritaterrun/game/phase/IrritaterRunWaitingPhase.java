package io.github.haykam821.irritaterrun.game.phase;

import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMap;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMapBuilder;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.GameMode;
import xyz.nucleoid.fantasy.BubbleWorldConfig;
import xyz.nucleoid.plasmid.game.GameOpenContext;
import xyz.nucleoid.plasmid.game.GameOpenProcedure;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.GameWaitingLobby;
import xyz.nucleoid.plasmid.game.StartResult;
import xyz.nucleoid.plasmid.game.config.PlayerConfig;
import xyz.nucleoid.plasmid.game.event.OfferPlayerListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.RequestStartListener;
import xyz.nucleoid.plasmid.game.player.JoinResult;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;

public class IrritaterRunWaitingPhase implements PlayerAddListener, PlayerDeathListener, OfferPlayerListener, RequestStartListener {
	private final GameSpace gameSpace;
	private final IrritaterRunMap map;
	private final IrritaterRunConfig config;

	public IrritaterRunWaitingPhase(GameSpace gameSpace, IrritaterRunMap map, IrritaterRunConfig config) {
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
	}

	public static GameOpenProcedure open(GameOpenContext<IrritaterRunConfig> context) {
		IrritaterRunMapBuilder mapBuilder = new IrritaterRunMapBuilder(context.getConfig());
		IrritaterRunMap map = mapBuilder.create();

		BubbleWorldConfig worldConfig = new BubbleWorldConfig()
			.setGenerator(map.createGenerator(context.getServer()))
			.setDefaultGameMode(GameMode.ADVENTURE);

		return context.createOpenProcedure(worldConfig, game -> {
			IrritaterRunWaitingPhase phase = new IrritaterRunWaitingPhase(game.getSpace(), map, context.getConfig());
			GameWaitingLobby.applyTo(game, context.getConfig().getPlayerConfig());

			IrritaterRunActivePhase.setRules(game);
			game.setRule(GameRule.PVP, RuleResult.DENY);

			// Listeners
			game.on(PlayerAddListener.EVENT, phase);
			game.on(PlayerDeathListener.EVENT, phase);
			game.on(OfferPlayerListener.EVENT, phase);
			game.on(RequestStartListener.EVENT, phase);
		});
	}

	private boolean isFull() {
		return this.gameSpace.getPlayerCount() >= this.config.getPlayerConfig().getMaxPlayers();
	}

	// Listeners
	@Override
	public void onAddPlayer(ServerPlayerEntity player) {
		IrritaterRunActivePhase.spawn(this.gameSpace.getWorld(), this.map, player);
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		IrritaterRunActivePhase.spawn(this.gameSpace.getWorld(), this.map, player);
		return ActionResult.FAIL;
	}

	@Override
	public JoinResult offerPlayer(ServerPlayerEntity player) {
		return this.isFull() ? JoinResult.gameFull() : JoinResult.ok();
	}

	@Override
	public StartResult requestStart() {
		PlayerConfig playerConfig = this.config.getPlayerConfig();
		if (this.gameSpace.getPlayerCount() < playerConfig.getMinPlayers()) {
			return StartResult.NOT_ENOUGH_PLAYERS;
		}

		IrritaterRunActivePhase.open(this.gameSpace, this.map, this.config);
		return StartResult.OK;
	}
}