package com.ntmit.items;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.NTMITMod;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ItemDyatlovSteamDevice extends Item {

	public ItemDyatlovSteamDevice(String name) {
		this.setTranslationKey("item." + RefStrings.MODID + "." + name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
		this.setCreativeTab(NTMITMod.tabITContent);
		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!(world.getBlockState(pos).getBlock() instanceof RBMKBase)) return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;
		BlockPos core = RbmkJumpHandler.reactorCore(world, pos);
		if (core == null) return EnumActionResult.PASS;
		RbmkJumpHandler.scheduleSteamExplosion(world, core);
		return EnumActionResult.SUCCESS;
	}
}