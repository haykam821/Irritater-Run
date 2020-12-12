package io.github.haykam821.irritaterrun.game.map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class IrritaterRunMapConfig {
	public static final Codec<IrritaterRunMapConfig> CODEC = RecordCodecBuilder.create(instance -> {
		return instance.group(
			Codec.INT.fieldOf("x").forGetter(IrritaterRunMapConfig::getX),
			Codec.INT.fieldOf("z").forGetter(IrritaterRunMapConfig::getZ)
		).apply(instance, IrritaterRunMapConfig::new);
	});

	private final int x;
	private final int z;

	public IrritaterRunMapConfig(int x, int z) {
		this.x = x;
		this.z = z;
	}

	public int getX() {
		return this.x;
	}

	public int getZ() {
		return this.z;
	}
}