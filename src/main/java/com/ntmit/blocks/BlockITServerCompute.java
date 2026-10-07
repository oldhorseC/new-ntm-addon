package com.ntmit.blocks;

import com.ntmit.tileentity.TileEntityITServerCompute;
import net.minecraft.tileentity.TileEntity;

public class BlockITServerCompute extends BlockITServerColumn {

	public BlockITServerCompute() {
		super("server_compute");
	}

	@Override
	protected TileEntity createColumnTileEntity() {
		return new TileEntityITServerCompute();
	}
}
