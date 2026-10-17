package com.ntmit.blocks;

import com.ntmit.lib.RefStrings;
import com.ntmit.tileentity.TileEntityITConsole;
import com.ntmit.tileentity.TileEntityITServerCompute;
import com.ntmit.tileentity.TileEntityITServerCore;
import com.ntmit.tileentity.TileEntityITServerHeatExchanger;
import com.ntmit.tileentity.TileEntityITSteamDrum;
import com.ntmit.tileentity.TileEntityITTestMultiblock;
import com.ntmit.tileentity.TileEntityRBMKGauge3x3;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModBlocks {

	public static final Block test_block = new BlockTest();
	public static final Block test_multiblock = new BlockTestMultiblock();

	public static final Block rbmk_gauge_3x3 = new BlockRBMKGauge3x3();

	public static final BlockITServerCore server_core = new BlockITServerCore();
	public static final BlockITServerCompute server_compute = new BlockITServerCompute();
	public static final BlockITServerHeatExchanger server_heat_exchanger = new BlockITServerHeatExchanger();

	public static final BlockITSteamDrum steam_drum = new BlockITSteamDrum();

	public static void preInit() {
		ForgeRegistries.ITEMS.register(new ItemBlock(test_block).setRegistryName(test_block.getRegistryName()));

		GameRegistry.registerTileEntity(TileEntityITConsole.class, new ResourceLocation(RefStrings.MODID, "test_block"));

		ForgeRegistries.ITEMS.register(new ItemBlock(test_multiblock).setRegistryName(test_multiblock.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityITTestMultiblock.class, new ResourceLocation(RefStrings.MODID, "test_multiblock"));

		ForgeRegistries.BLOCKS.register(rbmk_gauge_3x3);
		ForgeRegistries.ITEMS.register(new ItemBlock(rbmk_gauge_3x3).setRegistryName(rbmk_gauge_3x3.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityRBMKGauge3x3.class, new ResourceLocation(RefStrings.MODID, "rbmk_gauge_3x3"));

		ForgeRegistries.ITEMS.register(new ItemBlock(server_core).setRegistryName(server_core.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityITServerCore.class, new ResourceLocation(RefStrings.MODID, "server_core"));

		ForgeRegistries.ITEMS.register(new ItemBlock(server_compute).setRegistryName(server_compute.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityITServerCompute.class, new ResourceLocation(RefStrings.MODID, "server_compute"));

		ForgeRegistries.ITEMS.register(new ItemBlock(server_heat_exchanger).setRegistryName(server_heat_exchanger.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityITServerHeatExchanger.class, new ResourceLocation(RefStrings.MODID, "server_heat_exchanger"));

		ForgeRegistries.ITEMS.register(new ItemBlock(steam_drum).setRegistryName(steam_drum.getRegistryName()));
		GameRegistry.registerTileEntity(TileEntityITSteamDrum.class, new ResourceLocation(RefStrings.MODID, "steam_drum"));
	}
}
