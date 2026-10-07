package com.ntmit.blocks;

import com.ntmit.lib.RefStrings;
import com.ntmit.main.ModGuiHandler;
import com.ntmit.main.NTMITMod;
import com.ntmit.tileentity.TileEntityITServerCore;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

import java.util.Locale;

public class BlockITServerCore extends BlockITServerColumn {

	public BlockITServerCore() {
		super("server_core");
	}

	@Override
	protected TileEntity createColumnTileEntity() {
		return new TileEntityITServerCore();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (world.isRemote) return true;

		BlockPos core = this.findCore(world, pos);
		if (core == null) return false;

		TileEntity tile = world.getTileEntity(core);
		if (!(tile instanceof TileEntityITServerCore server)) return false;

		if (player.isSneaking()) {
			sendStatus(player, server, core);
			return true;
		}

		FMLNetworkHandler.openGui(player, NTMITMod.instance, ModGuiHandler.ID_SERVER, world, core.getX(), core.getY(), core.getZ());
		return true;
	}

	private void sendStatus(EntityPlayer player, TileEntityITServerCore server, BlockPos core) {
		String facingName = server.getFacing().name().toLowerCase(Locale.US);
		String stateName = server.getState().name().toLowerCase(Locale.US);

		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.head", core.getX(), core.getY(), core.getZ()));
		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.facing." + facingName));
		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.compute",
				server.getCompute(), server.getComputeSlots(), server.getConnectedComputeUnits()));
		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.heat",
				server.getHeat(), server.getMaxHeat(), server.getCooling(), server.getHeatTargetCount()));
		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.state." + stateName));
		player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.power", server.getPower(), server.getMaxPower()));

		if (server.getHeatTargetCount() > 0 && !server.hasCoolant()) {
			player.sendMessage(statusLine("chat." + RefStrings.MODID + ".server.status.no_coolant"));
		}
	}

	private static ITextComponent statusLine(String key, Object... args) {
		return new TextComponentTranslation(key, args).setStyle(new Style().setColor(TextFormatting.YELLOW));
	}
}
