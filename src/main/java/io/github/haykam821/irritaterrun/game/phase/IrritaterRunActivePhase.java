package io.github.haykam821.irritaterrun.game.phase;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import io.github.haykam821.irritaterrun.game.IrritaterArmorSet;
import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import io.github.haykam821.irritaterrun.game.IrritaterRunTimerBar;
import io.github.haykam821.irritaterrun.game.PlayerEntry;
import io.github.haykam821.irritaterrun.game.map.IrritaterRunMap;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameMode;
import xyz.nucleoid.plasmid.game.GameCloseReason;
import xyz.nucleoid.plasmid.game.GameLogic;
import xyz.nucleoid.plasmid.game.GameSpace;
import xyz.nucleoid.plasmid.game.event.GameCloseListener;
import xyz.nucleoid.plasmid.game.event.GameOpenListener;
import xyz.nucleoid.plasmid.game.event.GameTickListener;
import xyz.nucleoid.plasmid.game.event.PlayerAddListener;
import xyz.nucleoid.plasmid.game.event.PlayerDamageListener;
import xyz.nucleoid.plasmid.game.event.PlayerDeathListener;
import xyz.nucleoid.plasmid.game.event.PlayerRemoveListener;
import xyz.nucleoid.plasmid.game.rule.GameRule;
import xyz.nucleoid.plasmid.game.rule.RuleResult;
import xyz.nucleoid.plasmid.widget.GlobalWidgets;

public class IrritaterRunActivePhase {
	private final ServerWorld world;
	private final GameSpace gameSpace;
	private final IrritaterRunMap map;
	private final IrritaterRunConfig config;
	private final List<PlayerEntry> players;
	private final IrritaterRunTimerBar timerBar;
	private final IrritaterArmorSet armorSet;
	private boolean singleplayer = false;
	private boolean opened = false;
	private int rounds = 0;
	private int ticksUntilSwitch = this.getRoundTicks();
	private boolean irritaterRound = false;

	public IrritaterRunActivePhase(GameSpace gameSpace, IrritaterRunMap map, IrritaterRunConfig config, GlobalWidgets widgets) {
		this.world = gameSpace.getWorld();
		this.gameSpace = gameSpace;
		this.map = map;
		this.config = config;
		this.players = gameSpace.getPlayers().stream().map(player -> {
			return new PlayerEntry(player, this);
		}).collect(Collectors.toList());
		this.timerBar = new IrritaterRunTimerBar(widgets);
		this.armorSet = new IrritaterArmorSet(config.getArmorColor());
	}

	public static void setRules(GameLogic game) {
		game.setRule(GameRule.BLOCK_DROPS, RuleResult.DENY);
		game.setRule(GameRule.CRAFTING, RuleResult.DENY);
		game.setRule(GameRule.FALL_DAMAGE, RuleResult.DENY);
		game.setRule(GameRule.HUNGER, RuleResult.DENY);
		game.setRule(GameRule.INTERACTION, RuleResult.DENY);
		game.setRule(GameRule.PORTALS, RuleResult.DENY);
		game.setRule(GameRule.PVP, RuleResult.ALLOW);
	}

	public static void open(GameSpace gameSpace, IrritaterRunMap map, IrritaterRunConfig config) {
		gameSpace.openGame(game -> {
			GlobalWidgets widgets = new GlobalWidgets(game);
			IrritaterRunActivePhase phase = new IrritaterRunActivePhase(gameSpace, map, config, widgets);

			IrritaterRunActivePhase.setRules(game);

			// Listeners
			game.on(GameCloseListener.EVENT, phase::close);
			game.on(GameOpenListener.EVENT, phase::open);
			game.on(GameTickListener.EVENT, phase::tick);
			game.on(PlayerAddListener.EVENT, phase::addPlayer);
			game.on(PlayerDamageListener.EVENT, phase::onPlayerDamage);
			game.on(PlayerDeathListener.EVENT, phase::onPlayerDeath);
			game.on(PlayerRemoveListener.EVENT, phase::removePlayer);
		});
	}

	private void close() {
		this.timerBar.remove();
	}

	private void open() {
		this.opened = true;
		this.singleplayer = this.players.size() == 1;

 		for (PlayerEntry entry : this.players) {
			entry.spawn();
		}
		this.setRandomIrritatered();
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

	private void tick() {
		this.ticksUntilSwitch -= 1;
		this.timerBar.tick(this);
		if (this.ticksUntilSwitch < 0) {
			if (this.irritaterRound) {
				this.gameSpace.getPlayers().sendSound(this.config.getDestroySound());
				this.ticksUntilSwitch = 20 * 4;

				Iterator<PlayerEntry> irritateredIterator = this.players.iterator();
				while (irritateredIterator.hasNext()) {
					PlayerEntry entry = irritateredIterator.next();
					if (entry.isIrritatered()) {
						this.eliminate(entry, ".destroyed", false);
						irritateredIterator.remove();
					}
				}
			} else {
				this.rounds += 1;
				this.ticksUntilSwitch = this.getRoundTicks();

				this.setRandomIrritatered();
			}
			this.irritaterRound = !this.irritaterRound;

			for (PlayerEntry entry : this.players) {
				entry.update();
			}
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

		// Determine a winner
		if (this.players.size() < 2) {
			if (this.players.size() == 1 && this.singleplayer) return;
			
			Text endingMessage = this.getEndingMessage();
			for (ServerPlayerEntity player : this.gameSpace.getPlayers()) {
				player.sendMessage(endingMessage, false);
			}

			this.gameSpace.close(GameCloseReason.FINISHED);
		}
	}

	private Text getEndingMessage() {
		if (this.players.size() == 1) {
			return this.players.iterator().next().getWinningMessage();
		}
		return new TranslatableText("text.irritaterrun.no_winners", this.rounds).formatted(Formatting.GOLD);
	}

	private void setSpectator(PlayerEntity player) {
		player.setGameMode(GameMode.SPECTATOR);
	}

	private PlayerEntry getEntryFromPlayer(ServerPlayerEntity player) {
		for (PlayerEntry entry : this.players) {
			if (player.equals(entry.getPlayer())) {
				return entry;
			}
		}
		return null;
	}

	private void addPlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry == null || !this.players.contains(entry)) {
			this.setSpectator(player);
		} else if (this.opened) {
			this.eliminate(entry, true);
		}
	}

	private void removePlayer(ServerPlayerEntity player) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		this.eliminate(entry, true);
		this.setRandomIrritatered();
	}

	private void eliminate(PlayerEntry entry, String suffix, boolean remove) {
		Text message = new TranslatableText("text.irritaterrun.eliminated" + suffix, entry.getPlayer().getDisplayName()).formatted(Formatting.RED);
		this.gameSpace.getPlayers().sendMessage(message);

		if (remove) {
			this.players.remove(entry);
		}
		this.setSpectator(entry.getPlayer());
	}

	private void eliminate(PlayerEntry entry, boolean remove) {
		this.eliminate(entry, "", remove);
	}

	private ActionResult onPlayerDamage(ServerPlayerEntity player, DamageSource source, float amount) {
		if (!(source.getAttacker() instanceof ServerPlayerEntity)) return ActionResult.FAIL;
		PlayerEntry attackerEntry = this.getEntryFromPlayer((ServerPlayerEntity) source.getAttacker());
		if (attackerEntry != null && attackerEntry.isIrritatered()) {
			PlayerEntry entry = this.getEntryFromPlayer((ServerPlayerEntity) source.getAttacker());
			if (entry != null) {
				attackerEntry.setIrritatered(false);
				attackerEntry.update();

				entry.setIrritatered(true);
				entry.update();
			}
		}
		return ActionResult.FAIL;
	}

	private ActionResult onPlayerDeath(ServerPlayerEntity player, DamageSource source) {
		PlayerEntry entry = this.getEntryFromPlayer(player);
		if (entry == null) {
			IrritaterRunActivePhase.spawn(this.world, this.map, player);
		} else {
			this.eliminate(entry, true);
			this.setRandomIrritatered();
		}
		return ActionResult.FAIL;
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

	public IrritaterArmorSet getArmorSet() {
		return this.armorSet;
	}

	public static void spawn(ServerWorld world, IrritaterRunMap map, ServerPlayerEntity player) {
		Vec3d center = map.getInnerBox().getCenter();
		player.teleport(world, center.getX(), map.getPlatform().getMin().getY() + 1, center.getZ(), 0, 0);
	}
}