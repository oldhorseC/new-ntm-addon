package com.ntmit.main;

import com.ntmit.blocks.ModBlocks;
import com.ntmit.creativetabs.TabITContent;
import com.ntmit.items.ModItems;
import com.ntmit.lib.RefStrings;
import com.ntmit.proxy.ServerProxy;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import org.apache.logging.log4j.Logger;

@Mod(modid = RefStrings.MODID, name = RefStrings.NAME, version = RefStrings.VERSION,
		dependencies = NTMITMod.DEPENDENCIES)
public class NTMITMod {

	public static final String DEPENDENCIES = "required-after:hbm;required-after:mixinbooter";

	@Mod.Instance(RefStrings.MODID)
	public static NTMITMod instance;

	@SidedProxy(clientSide = RefStrings.CLIENTSIDE, serverSide = RefStrings.SERVERSIDE)
	public static ServerProxy proxy;

	public static Logger logger;

	public static CreativeTabs tabITContent = new TabITContent(CreativeTabs.getNextID());

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		logger = event.getModLog();
		logger.info("{} {} preInit", RefStrings.NAME, RefStrings.VERSION);

		proxy.registerRenderInfo();
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.ntmit.lib.NTMITWorldRules());
		net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new com.ntmit.lib.LidAchievements());
		NetworkRegistry.INSTANCE.registerGuiHandler(instance, new ModGuiHandler());
		ModItems.preInit();
		ModBlocks.preInit();
		net.minecraftforge.fml.common.registry.EntityRegistry.registerModEntity(
				new net.minecraft.util.ResourceLocation(RefStrings.MODID, "lid_plate"),
				com.ntmit.entity.EntityLidPlate.class, "lid_plate", 0, instance, 160, 1, true);
		com.ntmit.controlpanel.NtmitControlPanel.register();
		proxy.preInit(event);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		proxy.init(event);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		proxy.postInit(event);
	}

	@Mod.EventHandler
	public void onLoadComplete(FMLLoadCompleteEvent event) {
		proxy.onLoadComplete(event);
	}
}
