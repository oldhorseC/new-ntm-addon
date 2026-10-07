package com.ntmit.proxy;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class ServerProxy {

	public void registerRenderInfo() { }
	public void registerTileEntitySpecialRenderer() { }
	public void registerItemRenderer() { }
	public void registerEntityRenderer() { }
	public void registerBlockRenderer() { }

	public void preInit(FMLPreInitializationEvent evt) { }
	public void init(FMLInitializationEvent evt) { }
	public void postInit(FMLPostInitializationEvent evt) { }
	public void onLoadComplete(FMLLoadCompleteEvent event) { }
}
