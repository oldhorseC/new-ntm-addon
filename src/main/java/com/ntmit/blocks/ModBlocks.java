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

	/**
	 * 注意（2.6.1.0）：不要为 test_block / test_multiblock / server_* 调用
	 * ForgeRegistries.BLOCKS.register()。
	 * <p>
	 * 这些方块继承 com.hbm.blocks.BlockBase（server_* 经由 BlockITServerColumn ->
	 * BlockDummyable -> BlockBase），而 HBM 的 BlockBase / BlockDummyable / BlockContainerBakeableNormal /
	 * BlockFallingBase 构造器都会执行 ModBlocks.ALL_BLOCKS.add(this)；
	 * HBM 随后在 RegistryEvent.Register&lt;Block&gt; 事件里由 ModBlocks.registerBlocks()
	 * 遍历 ALL_BLOCKS 统一注册。我们再注册一次就会触发 FML 的
	 * "The object ... has been registered twice for the same name ..." 警告。
	 * <p>
	 * 例外：rbmk_gauge_3x3 的继承链是 RBMKGauge -> RBMKMiniPanelBase -> BlockContainer，
	 * 不在 HBM 的 ALL_BLOCKS 中，必须由我们自己注册。
	 * <p>
	 * ItemBlock（ITEMS 注册表）与 TileEntity 的注册仍然由我们负责。
	 */
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
