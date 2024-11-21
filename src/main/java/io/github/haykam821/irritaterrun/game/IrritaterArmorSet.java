package io.github.haykam821.irritaterrun.game;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.api.util.ItemStackBuilder;

public final class IrritaterArmorSet {
	private static final Text NAME = Text.translatable("text.irritaterrun.armor").formatted(Formatting.RED);

	private final int color;

	public IrritaterArmorSet(int color) {
		this.color = color;
	}

	public ItemStack getHelmet(RegistryWrapper.WrapperLookup registries) {
		return getArmorStack(registries, Items.LEATHER_HELMET, this.color);
	}

	public ItemStack getChestplate(RegistryWrapper.WrapperLookup registries) {
		return getArmorStack(registries, Items.LEATHER_CHESTPLATE, this.color);
	}

	public ItemStack getLeggings(RegistryWrapper.WrapperLookup registries) {
		return getArmorStack(registries, Items.LEATHER_LEGGINGS, this.color);
	}

	public ItemStack getBoots(RegistryWrapper.WrapperLookup registries) {
		return getArmorStack(registries, Items.LEATHER_BOOTS, this.color);
	}

	private static ItemStack getArmorStack(RegistryWrapper.WrapperLookup registries, ItemConvertible item, int color) {
		RegistryEntry<Enchantment> enchantment = registries
			.getOrThrow(RegistryKeys.ENCHANTMENT)
			.getOrThrow(Enchantments.BINDING_CURSE);

		return ItemStackBuilder.of(item)
			.addEnchantment(enchantment, 1)
			.setUnbreakable()
			.setName(NAME)
			.setDyeColor(color)
			.build();
	}
}
