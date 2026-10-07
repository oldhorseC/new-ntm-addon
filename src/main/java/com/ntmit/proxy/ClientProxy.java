package com.ntmit.proxy;

import com.ntmit.main.ModEventHandlerClient;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientProxy extends ServerProxy {

	@Override
	public void registerRenderInfo() {

		MinecraftForge.EVENT_BUS.register(new ModEventHandlerClient());

		ClientRegistry.bindTileEntitySpecialRenderer(com.ntmit.tileentity.TileEntityRBMKGauge3x3.class,
				new com.ntmit.render.RenderRBMKGauge3x3());

		net.minecraftforge.fml.client.registry.RenderingRegistry.registerEntityRenderingHandler(com.ntmit.entity.EntityLidPlate.class,
				manager -> new com.ntmit.render.RenderLidPlate(manager));
	}

	@Override
	public void registerTileEntitySpecialRenderer() { }

	@Override
	public void registerItemRenderer() { }

	@Override
	public void registerEntityRenderer() { }

	@Override
	public void registerBlockRenderer() { }

	@Override
	public void preInit(FMLPreInitializationEvent evt) {
		com.ntmit.packet.PacketNTMITSteam.register();
		super.preInit(evt);
	}

	@Override
	public void init(FMLInitializationEvent evt) {
		net.minecraftforge.client.ClientCommandHandler.instance.registerCommand(new com.ntmit.command.CommandNTMIT());
		super.init(evt);
	}

	@Override
	public void postInit(FMLPostInitializationEvent evt) {
		super.postInit(evt);
	}

	@Override
	public void onLoadComplete(FMLLoadCompleteEvent event) {
		super.onLoadComplete(event);
	}

	public static void handleSteamPacket(com.ntmit.packet.PacketNTMITSteam message) {
		net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
		net.minecraft.world.World world = mc.world;
		if (world == null) return;
		int count = Math.max(1, message.count);
		float spread = Math.max(0.5F, message.spread);
		for (int i = 0; i < count; i++) {
			double x = message.x + (world.rand.nextDouble() - 0.5D) * 2.0D * (double) spread;
			double z = message.z + (world.rand.nextDouble() - 0.5D) * 2.0D * (double) spread;
			double y = message.y + world.rand.nextDouble() * 0.75D;
			com.ntmit.particle.ParticleNTMITSteam particle = new com.ntmit.particle.ParticleNTMITSteam(world, x, y, z);
			particle.setColor(message.color & 0xFFFFFF);
			particle.setCloudScale(message.base, message.max);
			particle.setLife(message.life);
			particle.motionX = (world.rand.nextDouble() - 0.5D) * 0.25D;
			particle.motionY = (double) message.rise * (0.8D + world.rand.nextDouble() * 0.5D);
			particle.motionZ = (world.rand.nextDouble() - 0.5D) * 0.25D;
			mc.effectRenderer.addEffect(particle);
		}
	}
}
