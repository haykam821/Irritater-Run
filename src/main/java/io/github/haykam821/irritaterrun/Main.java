package io.github.haykam821.irritaterrun;

import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import io.github.haykam821.irritaterrun.game.phase.IrritaterRunWaitingPhase;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;
import xyz.nucleoid.plasmid.game.GameType;

public class Main implements ModInitializer {
	private static final String MOD_ID = "irritaterrun";

	private static final Identifier IRRITATER_RUN_ID = new Identifier(MOD_ID, "irritater_run");
	public static final GameType<IrritaterRunConfig> IRRITATER_RUN_TYPE = GameType.register(IRRITATER_RUN_ID, IrritaterRunConfig.CODEC, IrritaterRunWaitingPhase::open);

	@Override
	public void onInitialize() {
		return;
	}
}