package com.ntmit.particle;

import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;

import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;
import java.util.Map;

@SideOnly(Side.CLIENT)
public final class FuelTintCache {

	private static final int DIGAMMA_TINT = 0xFF2020;
	private static final Map<Long, int[]> CACHE = new HashMap<>();

	public static int tintAt(World world, double x, double y, double z) {
		if (world == null) return 0xFFFFFF;
		int bx = (int) Math.floor(x);
		int by = (int) Math.floor(y);
		int bz = (int) Math.floor(z);
		long key = (((long) bx) & 0x3FFFFFFL) << 38 | (((long) bz) & 0x3FFFFFFL) << 12 | (((long) by) & 0xFFFL);
		int now = (int) world.getTotalWorldTime();
		int[] hit = CACHE.get(key);
		if (hit != null && now - hit[1] <= 20) return hit[0];
		int tint = lookup(world, bx, by, bz);
		if (CACHE.size() > 4096) CACHE.clear();
		CACHE.put(key, new int[] {tint, now});
		return tint;
	}

	private static int lookup(World world, int bx, int by, int bz) {
		int span = Math.max(RBMKDials.getColumnHeight(world), 1) + 4;
		for (int i = 0; i <= span; i++) {
			BlockPos pos = new BlockPos(bx, by - i, bz);
			if (!world.isBlockLoaded(pos)) return 0xFFFFFF;
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityRBMKRod rod)) continue;
			if (rod.inventory == null) return 0xFFFFFF;
			ItemStack stack = rod.inventory.getStackInSlot(0);
			if (stack.isEmpty() || !(stack.getItem() instanceof ItemRBMKRod fuel)) return 0xFFFFFF;
			if (fuel.getRegistryName() != null && fuel.getRegistryName().getPath().contains("digamma")) return DIGAMMA_TINT;
			return fuel.colorTint;
		}
		return 0xFFFFFF;
	}

	private FuelTintCache() { }
}
