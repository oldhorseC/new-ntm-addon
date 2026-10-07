package com.ntmit.main;

import com.hbm.tileentity.IGUIProvider;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

public class ModGuiHandler implements IGuiHandler {

	public static final int ID_CONSOLE = 0;

	public static final int ID_SERVER = 1;

	public static final int ID_SERVER_RBMK = 2;

	public static final int ID_rbmk_gauge_3x3 = 3;

	@Override
	public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		if (te instanceof IGUIProvider provider) {
			return provider.provideContainer(ID, player, world, x, y, z);
		}
		return null;
	}

	@Override
	public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
		TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
		if (te instanceof IGUIProvider provider) {
			return provider.provideGUI(ID, player, world, x, y, z);
		}
		return null;
	}
}
