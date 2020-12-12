package io.github.haykam821.irritaterrun.game.map;

import java.util.Iterator;

import io.github.haykam821.irritaterrun.game.IrritaterRunConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import xyz.nucleoid.plasmid.map.template.MapTemplate;
import xyz.nucleoid.plasmid.util.BlockBounds;

public class IrritaterRunMapBuilder {
	private static final BlockState FLOOR = Blocks.SMOOTH_SANDSTONE.getDefaultState();
	private static final BlockState FLOOR_OUTLINE = Blocks.OAK_PLANKS.getDefaultState();
	private static final BlockState WALL = Blocks.COBBLESTONE_WALL.getDefaultState();
	private static final BlockState WALL_TOP = Blocks.OAK_SLAB.getDefaultState();

	private final IrritaterRunConfig config;

	public IrritaterRunMapBuilder(IrritaterRunConfig config) {
		this.config = config;
	}

	public IrritaterRunMap create() {
		MapTemplate template = MapTemplate.createEmpty();
		IrritaterRunMapConfig mapConfig = this.config.getMapConfig();

		BlockBounds bounds = new BlockBounds(BlockPos.ORIGIN, new BlockPos(mapConfig.getX() + 1, 4, mapConfig.getZ() + 1));
		this.build(bounds, template, mapConfig);

		return new IrritaterRunMap(template, bounds);
	}

	private BlockState getBlockState(BlockPos pos, BlockBounds bounds, IrritaterRunMapConfig mapConfig) {
		int layer = pos.getY() - bounds.getMin().getY();
		boolean outline = pos.getX() == bounds.getMin().getX() || pos.getX() == bounds.getMax().getX() || pos.getZ() == bounds.getMin().getZ() || pos.getZ() == bounds.getMax().getZ();

		if (outline) {
			if (layer == 0) {
				return FLOOR_OUTLINE;
			} else if (layer == 1) {
				return WALL;
			} else if (layer == 2) {
				return WALL_TOP;
			}
		} else if (layer == 0) {
			return FLOOR;
		}

		return null;
	}

	public void build(BlockBounds bounds, MapTemplate template, IrritaterRunMapConfig mapConfig) {
		Iterator<BlockPos> iterator = bounds.iterator();
		while (iterator.hasNext()) {
			BlockPos pos = iterator.next();

			BlockState state = this.getBlockState(pos, bounds, mapConfig);
			if (state != null) {
				template.setBlockState(pos, state);
			}
		}
	}
}