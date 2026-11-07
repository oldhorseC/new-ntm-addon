package com.ntmit.lib;

import com.hbm.items.food.ItemCrayon;
import com.hbm.items.machine.ItemChemicalDye;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDye;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;

public final class TurbinePaintColors {

	private TurbinePaintColors() {
	}

	public static int colorOf(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return TurbinePaintAccess.NO_PAINT;
		Item item = stack.getItem();
		int meta = stack.getMetadata();
		if (item instanceof ItemChemicalDye || item instanceof ItemCrayon) {
			ItemChemicalDye.EnumChemDye[] values = ItemChemicalDye.EnumChemDye.values();
			if (meta < 0 || meta >= values.length) return TurbinePaintAccess.NO_PAINT;
			return values[meta].color & 0xFFFFFF;
		}
		if (item == Items.DYE || item instanceof ItemDye) {
			if (meta < 0 || meta >= ItemDye.DYE_COLORS.length) return TurbinePaintAccess.NO_PAINT;
			return ItemDye.DYE_COLORS[meta] & 0xFFFFFF;
		}
		return oreDictionaryColor(stack);
	}

	private static int oreDictionaryColor(ItemStack stack) {
		for (int id : OreDictionary.getOreIDs(stack)) {
			String name = OreDictionary.getOreName(id);
			if (name == null) continue;
			if (name.equals("dye")) {
				int meta = stack.getMetadata();
				if (meta >= 0 && meta < ItemDye.DYE_COLORS.length) {
					return ItemDye.DYE_COLORS[meta] & 0xFFFFFF;
				}
				continue;
			}
			if (!name.startsWith("dye") || name.length() <= 3) continue;
			String colorName = name.substring(3);
			for (EnumDyeColor color : EnumDyeColor.values()) {
				if (color.getName().equalsIgnoreCase(colorName)) {
					return color.getColorValue() & 0xFFFFFF;
				}
			}
		}
		return TurbinePaintAccess.NO_PAINT;
	}
}

