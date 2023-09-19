package io.github.haykam821.irritaterrun.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.irritaterrun.Main;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class IrritaterRunEntityTypes {
	private static final Identifier IRRITATER_ID = new Identifier(Main.MOD_ID, "irritater");
	public static final EntityType<IrritaterEntity> IRRITATER = FabricEntityTypeBuilder.create(SpawnGroup.MISC, IrritaterEntity::new)
		.dimensions(EntityDimensions.fixed(8 / 16f, 30 / 16f))
		.build();

	private IrritaterRunEntityTypes() {
		return;
	}

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, IRRITATER_ID, IRRITATER);
		PolymerEntityUtils.registerType(IRRITATER);
	}
}
