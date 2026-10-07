package com.ntmit.blocks;

import com.hbm.blocks.machine.rbmk.RBMKGauge;
import com.ntmit.main.NTMITMod;
import com.ntmit.tileentity.TileEntityRBMKGauge3x3;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

public class BlockRBMKGauge3x3 extends RBMKGauge {

	public BlockRBMKGauge3x3() {

		super("rbmk_gauge_3x3");

		com.hbm.blocks.ModBlocks.ALL_BLOCKS.remove(this);

		this.setCreativeTab(NTMITMod.tabITContent);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileEntityRBMKGauge3x3();
	}
}
