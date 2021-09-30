package io.github.haykam821.irritaterrun.game;

import io.github.haykam821.irritaterrun.game.phase.IrritaterRunActivePhase;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.game.common.GlobalWidgets;
import xyz.nucleoid.plasmid.game.common.widget.BossBarWidget;

public class IrritaterRunTimerBar {
	private static final Text TITLE = new TranslatableText("gameType.irritaterrun.irritater_run").formatted(Formatting.AQUA);

	private final BossBarWidget bar;

	public IrritaterRunTimerBar(GlobalWidgets widgets) {
		this.bar = widgets.addBossBar(TITLE, BossBar.Color.BLUE, BossBar.Style.PROGRESS);
	}

	public void tick(IrritaterRunActivePhase phase) {
		this.bar.setProgress(phase.getTimerBarPercent());
	}
}
