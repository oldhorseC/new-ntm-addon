package com.ntmit.creativetabs;

import com.ntmit.blocks.ModBlocks;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TabITContent extends CreativeTabs {

	public TabITContent(int index) {
		super(index, "tabITContent");
	}

	@Override
	@SideOnly(Side.CLIENT)
	public ItemStack createIcon() {
		if (ModBlocks.test_block != null) {
			return new ItemStack(ModBlocks.test_block);
		}
		return new ItemStack(Items.IRON_PICKAXE);
	}
}
