package io.github.haykam821.irritaterrun.game;

import io.github.haykam821.irritaterrun.game.phase.IrritaterRunActivePhase;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class PlayerEntry {
	private final ServerPlayerEntity player;
	private final IrritaterRunActivePhase phase;
	private boolean irritatered = false;

	public PlayerEntry(ServerPlayerEntity player, IrritaterRunActivePhase phase) {
		this.player = player;
		this.phase = phase;
	}

	public ServerPlayerEntity getPlayer() {
		return this.player;
	}

	public boolean isIrritatered() {
		return this.irritatered;
	}

	public void setIrritatered(boolean irritatered) {
		this.irritatered = irritatered;
	}

	/**
	 * Attempts to transfer this entry's {@linkplain PlayerEntry#irritatered} status to another entry.
	 * @param target the entry to transfer the irritatered status to
	 * @return whether the transfer was successful
	 */
	public boolean attemptTransferTo(PlayerEntry target) {
		// Players that are not irritatered should not be able to transfer the irritater
		if (!this.irritatered) return false;

		this.setIrritatered(false);
		this.update();

		target.setIrritatered(true);
		target.sendReceivedMessage();
		target.update();

		return true;
	}

	public Text getWinningMessage() {
		return Text.translatable("text.irritaterrun.win", this.player.getDisplayName(), this.phase.getRounds()).formatted(Formatting.GOLD);
	}

	private Text getReceivedMessage() {
		Text playerName = this.player.getDisplayName().copy().formatted(Formatting.RED);
		return Text.translatable("text.irritaterrun.received", playerName);
	}

	private void sendActionBar(Text message) {
		this.player.sendMessage(message, true);
	}

	private void sendReceivedMessage() {
		Text message = this.getReceivedMessage();
		for (PlayerEntry player : this.phase.getPlayers()) {
			player.sendActionBar(message);
		}
	}

	public boolean isOutOfBounds() {
		return !this.phase.getMap().getInnerBox().contains(this.player.getPos());
	}

	private void updateInventory() {
		player.currentScreenHandler.sendContentUpdates();
		player.playerScreenHandler.onContentChanged(player.getInventory());
	}

	public void update() {
		player.changeGameMode(GameMode.ADVENTURE);

		// Inventory
		player.getInventory().clear();
		if (this.irritatered) {
			this.player.getInventory().armor.set(3, this.phase.getArmorSet().getHelmet());
			this.player.getInventory().armor.set(2, this.phase.getArmorSet().getChestplate());
			this.player.getInventory().armor.set(1, this.phase.getArmorSet().getLeggings());
			this.player.getInventory().armor.set(0, this.phase.getArmorSet().getBoots());
		}
		this.updateInventory();
	}
}
