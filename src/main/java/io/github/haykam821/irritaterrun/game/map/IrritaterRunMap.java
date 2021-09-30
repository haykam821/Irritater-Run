package io.github.haykam821.irritaterrun.game.map;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import xyz.nucleoid.map_templates.BlockBounds;
import xyz.nucleoid.map_templates.MapTemplate;
import xyz.nucleoid.plasmid.game.world.generator.TemplateChunkGenerator;

public class IrritaterRunMap {
	private final MapTemplate template;
	private final BlockBounds platform;
	private final Box innerBox;
	private final Vec3d spawnPos;

	public IrritaterRunMap(MapTemplate template, BlockBounds platform) {
		this.template = template;
		this.platform = platform;
		this.innerBox = this.platform.asBox().expand(-1, -0.5, -1);

		Vec3d center = this.innerBox.getCenter();
		this.spawnPos = new Vec3d(center.getX(), this.platform.min().getY() + 1, center.getZ());
	}

	public BlockBounds getPlatform() {
		return this.platform;
	}

	public Box getInnerBox() {
		return this.innerBox;
	}

	public Vec3d getSpawnPos() {
		return this.spawnPos;
	}

	public ChunkGenerator createGenerator(MinecraftServer server) {
		return new TemplateChunkGenerator(server, this.template);
	}
}