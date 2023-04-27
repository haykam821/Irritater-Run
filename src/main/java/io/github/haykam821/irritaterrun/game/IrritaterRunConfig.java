package io.github.haykam821.irritaterrun.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.haykam821.irritaterrun.game.map.IrritaterRunMapConfig;
import net.minecraft.SharedConstants;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.intprovider.ConstantIntProvider;
import net.minecraft.util.math.intprovider.IntProvider;
import xyz.nucleoid.plasmid.game.common.config.PlayerConfig;

public class IrritaterRunConfig {
	public static final Codec<IrritaterRunConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			IrritaterRunMapConfig.CODEC.fieldOf("map").forGetter(IrritaterRunConfig::getMapConfig),
			PlayerConfig.CODEC.fieldOf("players").forGetter(IrritaterRunConfig::getPlayerConfig),
			IntProvider.NON_NEGATIVE_CODEC.optionalFieldOf("ticks_until_close", ConstantIntProvider.create(SharedConstants.TICKS_PER_SECOND * 5)).forGetter(IrritaterRunConfig::getTicksUntilClose),
			SoundEvent.CODEC.optionalFieldOf("destroy_sound", SoundEvents.ENTITY_LIGHTNING_BOLT_THUNDER).forGetter(IrritaterRunConfig::getDestroySound),
			Codec.INT.optionalFieldOf("armor_color", 0xFF0000).forGetter(IrritaterRunConfig::getArmorColor)
		).apply(instance, IrritaterRunConfig::new);
	});

	private final IrritaterRunMapConfig mapConfig;
	private final PlayerConfig playerConfig;
	private final IntProvider ticksUntilClose;
	private final SoundEvent destroySound;
	private final int armorColor;

	public IrritaterRunConfig(IrritaterRunMapConfig mapConfig, PlayerConfig playerConfig, IntProvider ticksUntilClose, SoundEvent destroySound, int armorColor) {
		this.mapConfig = mapConfig;
		this.playerConfig = playerConfig;
		this.ticksUntilClose = ticksUntilClose;
		this.destroySound = destroySound;
		this.armorColor = armorColor;
	}

	public IrritaterRunMapConfig getMapConfig() {
		return this.mapConfig;
	}

	public PlayerConfig getPlayerConfig() {
		return this.playerConfig;
	}

	public IntProvider getTicksUntilClose() {
		return this.ticksUntilClose;
	}

	public SoundEvent getDestroySound() {
		return this.destroySound;
	}

	public int getArmorColor() {
		return this.armorColor;
	}
}