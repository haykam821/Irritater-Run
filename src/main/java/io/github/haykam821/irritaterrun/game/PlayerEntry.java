package io.github.haykam821.irritaterrun.game;

import io.github.haykam821.irritaterrun.game.phase.IrritaterRunActivePhase;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
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
		return new TranslatableText("text.irritaterrun.win", this.player.getDisplayName(), this.phase.getRounds()).formatted(Formatting.GOLD);
	}

	private Text getReceivedMessage() {
		Text playerName = this.player.getDisplayName().shallowCopy().formatted(Formatting.RED);
		return new TranslatableText("text.irritaterrun.received", playerName);
	}

	private void sendActionBar(Text message) {
		TitleS2CPacket packet = new TitleS2CPacket(TitleS2CPacket.Action.ACTIONBAR, message);
		this.player.networkHandler.sendPacket(packet);
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
		player.playerScreenHandler.onContentChanged(player.inventory);
		player.updateCursorStack();
	}

	public void update() {
		player.setGameMode(GameMode.ADVENTURE);

		// Inventory
		player.inventory.clear();
		if (this.irritatered) {
			this.player.inventory.armor.set(3, this.phase.getArmorSet().getHelmet());
			this.player.inventory.armor.set(2, this.phase.getArmorSet().getChestplate());
			this.player.inventory.armor.set(1, this.phase.getArmorSet().getLeggings());
			this.player.inventory.armor.set(0, this.phase.getArmorSet().getBoots());
		}
		this.updateInventory();
	}

	public void spawn() {
		this.update();
		IrritaterRunActivePhase.spawn(this.phase.getWorld(), this.phase.getMap(), player);
	}
}
