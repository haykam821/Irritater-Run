package io.github.haykam821.irritaterrun.entity;

import eu.pb4.polymer.core.api.entity.PolymerEntityUtils;
import io.github.haykam821.irritaterrun.Main;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

public final class IrritaterRunEntityTypes {
	private static final Identifier IRRITATER_ID = Main.identifier("irritater");
	private static final RegistryKey<EntityType<?>> IRRITATER_KEY = RegistryKey.of(RegistryKeys.ENTITY_TYPE, IRRITATER_ID);

	public static final EntityType<IrritaterEntity> IRRITATER = EntityType.Builder.create(IrritaterEntity::new, SpawnGroup.MISC)
		.dimensions(8 / 16f, 30 / 16f)
		.build(IRRITATER_KEY);

	private IrritaterRunEntityTypes() {
		return;
	}

	public static void register() {
		Registry.register(Registries.ENTITY_TYPE, IRRITATER_ID, IRRITATER);
		PolymerEntityUtils.registerType(IRRITATER);
	}
}
