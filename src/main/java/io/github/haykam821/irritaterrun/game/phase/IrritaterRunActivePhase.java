package io.github.haykam821.irritaterrun.game.phase;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import io.github.haykam821.irritaterrun.entity.IrritaterCatchEntityEvent;
import io.github.haykam821.irritaterrun.entity.IrritaterEntity;
import io.github.haykam821.irritaterrun.entity.IrritaterRunEntityTypes;
import io.github.haykam821.irritaterrun.game.IrritaterArmorSet;
import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import io.github.haykam821.irritaterrun.game.IrritaterRunTimerBar;
import io.github.haykam821.irritaterrun.game.PlayerEntry;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMap;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMapConfig;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameActivity;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.event.GameActivityEvents;
import xyz.nucleoid.plasmid.game.event.GamePlayerEvents;
import xyz.nucleoid.plasmid.game.player.PlayerOffer;
import xyz.nucleoid.plasmid.game.player.PlayerOfferResult;
import xyz.nucleoid.plasmid.game.rule.GameRuleType;
import xyz.nucleoid.stimuli.event.player.PlayerAttackEntityEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDamageEvent;
import xyz.nucleoid.stimuli.event.player.PlayerDeathEvent;

public class IrritaterRunActivePhase implements PlayerAttackEntityEvent, GameActivityEvents.Enable, GameActivityEvents.Tick, IrritaterCatchEntityEvent, GamePlayerEvents.Offer, PlayerDamageEvent, PlayerDeathEvent, GamePlayerEvents.Remove {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final IrritaterRunMap map;
	private final IrritaterRunConfig config;
	private final List<PlayerEntry> players;
	private final IrritaterRunTimerBar timerBar;
	private final IrritaterArmorSet armorSet;
	private boolean singleplayer = false;
	private int rounds = 0;
	private int ticksUntilSwitch = 20 * 4;
	private int ticksUntilClose = -1;
	private RoundState roundState = RoundState.BREAK;

	public IrritaterRunActivePhase(GameSpace gameSpace, ServerWorld world, IrritaterRunMap map, IrritaterRunConfig config, GlobalWidgets widgets) {
		this.world = world;
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.players = gameSpace.getPlayers().stream().map(player -> {
			return new PlayerEntry(player, this);
		}).collect(Collectors.toList());
		this.timerBar = new IrritaterRunTimerBar(widgets);
		this.armorSet = new IrritaterArmorSet(config.getArmorColor());
	}

	public static void setRules(GameActivity activity) {
		activity.deny(GameRuleType.BLOCK_DROPS);
		activity.deny(GameRuleType.CRAFTING);
		activity.deny(GameRuleType.FALL_DAMAGE);
		activity.deny(GameRuleType.HUNGER);
		activity.deny(GameRuleType.INTERACTION);
		activity.deny(GameRuleType.PORTALS);
		activity.allow(GameRuleType.PVP);
	}

	public static void open(GameSpace gameSpace, ServerWorld world, IrritaterRunMap map, IrritaterRunConfig config) {
		gameSpace.setActivity(activity -> {
			GlobalWidgets widgets = GlobalWidgets.addTo(activity);
			IrritaterRunActivePhase phase = new IrritaterRunActivePhase(gameSpace, world, map, config, widgets);

			IrritaterRunActivePhase.setRules(activity);

			// Listeners
			activity.listen(PlayerAttackEntityEvent.EVENT, phase);
			activity.listen(GameActivityEvents.ENABLE, phase);
			activity.listen(GameActivityEvents.TICK, phase);
			activity.listen(IrritaterCatchEntityEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.OFFER, phase);
			activity.listen(PlayerDamageEvent.EVENT, phase);
			activity.listen(PlayerDeathEvent.EVENT, phase);
			activity.listen(GamePlayerEvents.REMOVE, phase);
		});
	}

	// Listeners
	@Override
	public ActionResult onAttackEntity(ServerPlayerEntity attacker, Hand hand, Entity attacked, EntityHitResult hitResult) {
		if (!this.isGameEnding() && attacked instanceof ServerPlayerEntity) {
			PlayerEntry attackedEntry = this.getEntryFromPlayer((ServerPlayerEntity) attacked);
			if (attackedEntry != null) {
				PlayerEntry attackerEntry = this.getEntryFromPlayer(attacker);
				if (attackerEntry != null) {
					attackerEntry.attemptTransferTo(attackedEntry);
				}
			}
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onEnable() {
		int index = 0;
		this.singleplayer = this.players.size() == 1;

		IrritaterRunMapConfig mapConfig = this.config.getMapConfig();
		int spawnRadius = (Math.min(mapConfig.getX(), mapConfig.getZ()) - 4) / 2;

		Vec3d center = this.map.getSpawnPos();

		for (PlayerEntry entry : this.players) {
			entry.update();

			double theta = ((double) index / this.players.size()) * 2 * Math.PI;
			float yaw = (float) theta * MathHelper.DEGREES_PER_RADIAN + 90;

			double x = center.getX() + Math.cos(theta) * spawnRadius;
			double z = center.getZ() + Math.sin(theta) * spawnRadius;

			Vec3d spawnPos = new Vec3d(x, center.getY(), z);
			IrritaterRunActivePhase.spawn(this.world, spawnPos, yaw, entry.getPlayer());

			index += 1;
		}
	}

	@Override
	public void onTick() {
		// Decrease ticks until game end to zero
		if (this.isGameEnding()) {
			if (this.ticksUntilClose == 0) {
				this.gameSpace.close(GameCloseReason.FINISHED);
			}

			this.ticksUntilClose -= 1;
			return;
		}

		if (this.roundState != RoundState.IRRITATER_CATCHING) {
			this.ticksUntilSwitch -= 1;
		}

		this.timerBar.tick(this);
		if (this.ticksUntilSwitch < 0) {
			if (this.roundState == RoundState.IRRITATER) {
				this.endIrritateredRound();
			} else {
				this.startIrritateredRound();
			}

			this.updateAll();
		}

		// Eliminate players that are out of bounds
		Iterator<PlayerEntry> iterator = this.players.iterator();
		while (iterator.hasNext()) {
			PlayerEntry entry = iterator.next();
			if (entry.isOutOfBounds()) {
				this.eliminate(entry, ".out_of_bounds", false);
				iterator.remove();
			}
		}

		this.checkForWin();
	}

	@Override
	public ActionResult onIrritaterCatchEntity(IrritaterEntity irritater, Entity target) {
		if (!this.isGameEnding() && target instanceof ServerPlayerEntity player) {
			PlayerEntry entry = this.getEntryFromPlayer(player);

			if (entry != null) {
				entry.setIrritatered(true);
				entry.update();

				this.roundState = RoundState.IRRITATER;
			}
		}

		return ActionResult.PASS;
	}

	@Override
	public PlayerOfferResult onOfferPlayer(PlayerOffer offer) {
		return offer.accept(this.world, this.map.getSpawnPos()).and(() -> {
			this.setSpectator(offer.player());
		});
	}

	@Override
	public ActionResult onDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		return ActionResult.FAIL;
	}

	@Override
	public ActionResult onDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry == null || this.isGameEnding()) {
			IrritaterRunActivePhase.spawnAtCenter(this.world, this.map, player);
		} else {
			this.eliminate(entry, true);
			this.setRandomIrritatered();
		}
		return ActionResult.FAIL;
	}

	@Override
	public void onRemovePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry != null && !this.isGameEnding()) {
			this.eliminate(entry, true);

			// Restore irritatered balance
			if (entry.isIrritatered()) {
				this.setRandomIrritatered();
			}
		}
	}

	// Utilities
	/**
	 * Starts an irritatered round by {@linkplain IrritaterRunActivePhase#setRandomIrritatered making a random player irritatered}.
	 */
	private void startIrritateredRound() {
		this.rounds += 1;
		this.ticksUntilSwitch = this.getRoundTicks();

		IrritaterEntity entity = new IrritaterEntity(IrritaterRunEntityTypes.IRRITATER, world);

		Vec3d spawnPos = map.getSpawnPos();
		entity.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

		PlayerEntry entry = this.getRandomPlayer();

		if (entry != null) {
			entity.setTarget(entry.getPlayer());
		}

		world.spawnEntity(entity);

		this.roundState = RoundState.IRRITATER_CATCHING;
	}

	/**
	 * Ends an irritatered round by removing players that are irritatered.
	 */
	private void endIrritateredRound() {
		this.gameSpace.getPlayers().playSound(this.config.getDestroySound());
		this.ticksUntilSwitch = 20 * 4;

		Iterator<PlayerEntry> irritateredIterator = this.players.iterator();
		while (irritateredIterator.hasNext()) {
			PlayerEntry entry = irritateredIterator.next();
			if (entry.isIrritatered()) {
				this.eliminate(entry, ".destroyed", false);
				irritateredIterator.remove();
			}
		}

		this.roundState = RoundState.BREAK;
	}

	/**
	 * Attempts to determine a winner and starts game closure if one is found.
	 */
	private void checkForWin() {
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.ticksUntilClose = this.config.getTicksUntilClose().get(this.world.getRandom());
		}
	}

	/**
	 * Updates all entries in {@link IrritaterRunActivePhase#players}.
	 */
	private void updateAll() {
		for (PlayerEntry entry : this.players) {
			entry.update();
		}
	}

	private int getRoundTicks() {
		return Math.max(20, 300 - (this.rounds * 20));
	}

	private PlayerEntry getRandomPlayer() {
		if (this.players.isEmpty()) return null;
		return this.players.get(this.world.getRandom().nextInt(this.players.size()));
	}

	private void setRandomIrritatered() {
		PlayerEntry entry = this.getRandomPlayer();
		if (entry != null) {
			entry.setIrritatered(true);
			entry.update();
		}
	}

	private boolean isGameEnding() {
		return this.ticksUntilClose >= 0;
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			return this.players.iterator().next().getWinningMessage();
		}
		return Text.translatable("text.irritaterrun.no_winners", this.rounds).formatted(Formatting.GOLD);
	}

	private void setSpectator(ServerPlayerEntity player) {
		player.changeGameMode(GameMode.SPECTATOR);
	}

	private PlayerEntry getEntryFromPlayer(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player.equals(entry.getPlayer())) {
				return entry;
			}
		}
		return null;
	}

	private void eliminate(PlayerEntry entry, String suffix, boolean remove) {
		Text message = Text.translatable("text.irritaterrun.eliminated" + suffix, entry.getPlayer().getDisplayName()).formatted(Formatting.RED);
		this.gameSpace.getPlayers().sendMessage(message);

		if (remove) {
			this.players.remove(entry);
		}
		this.setSpectator(entry.getPlayer());
	}

	private void eliminate(PlayerEntry entry, boolean remove) {
		this.eliminate(entry, "", remove);
	}

	public float getTimerBarPercent() {
		return this.ticksUntilSwitch / (float) this.getRoundTicks();
	}

	public int getRounds() {
		return this.rounds;
	}

	public IrritaterRunMap getMap() {
		return this.map;
	}

	public ServerWorld getWorld() {
		return this.world;
	}

	public List<PlayerEntry> getPlayers() {
		return this.players;
	}

	public IrritaterArmorSet getArmorSet() {
		return this.armorSet;
	}

	public static void spawn(ServerWorld world, Vec3d pos, float yaw, ServerPlayerEntity player) {
		player.teleport(world, pos.getX(), pos.getY(), pos.getZ(), yaw, 0);
	}

	public static void spawnAtCenter(ServerWorld world, IrritaterRunMap map, ServerPlayerEntity player) {
		IrritaterRunActivePhase.spawn(world, map.getSpawnPos(), 0, player);
	}
}