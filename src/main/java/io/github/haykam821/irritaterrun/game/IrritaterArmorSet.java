package io.github.haykam821.irritaterrun.game;

import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import xyz.nucleoid.plasmid.util.ItemStackBuilder;

public final class IrritaterArmorSet {
	private static final Text NAME = new TranslatableText("text.irritaterrun.armor").formatted(Formatting.RED);

	private final int color;

	public IrritaterArmorSet(int color) {
		this.color = color;
	}

	public ItemStack getHelmet() {
		return getArmorStack(Items.LEATHER_HELMET, this.color);
	}

	public ItemStack getChestplate() {
		return getArmorStack(Items.LEATHER_CHESTPLATE, this.color);
	}

	public ItemStack getLeggings() {
		return getArmorStack(Items.LEATHER_LEGGINGS, this.color);
	}

	public ItemStack getBoots() {
		return getArmorStack(Items.LEATHER_BOOTS, this.color);
	}

	private static ItemStack getArmorStack(ItemConvertible item, int color) {
		return ItemStackBuilder.of(item)
			.addEnchantment(Enchantments.BINDING_CURSE, 1)
			.setUnbreakable()
			.setName(NAME)
			.setColor(color)
			.build();
	}
}
