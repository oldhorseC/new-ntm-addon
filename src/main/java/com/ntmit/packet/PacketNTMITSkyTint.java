package com.ntmit.packet;

import com.ntmit.lib.NukeFlash;
import com.ntmit.lib.SkyTint;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraft.world.World;

public class PacketNTMITSkyTint implements IMessage {

	public PacketNTMITSkyTint() { }

	public static void register() {
		PacketNTMITSteam.NETWORK.registerMessage(Handler.class, PacketNTMITSkyTint.class, 1, Side.CLIENT);
	}

	public static void send(World world, double x, double y, double z) {
		if (world == null || world.isRemote) return;
		try {
			PacketNTMITSteam.NETWORK.sendToAllAround(new PacketNTMITSkyTint(), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, 512.0D));
		} catch (Throwable throwable) {
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
	}

	@Override
	public void toBytes(ByteBuf buf) {
	}

	public static class Handler implements IMessageHandler<PacketNTMITSkyTint, IMessage> {

		@Override
		public IMessage onMessage(PacketNTMITSkyTint message, MessageContext ctx) {
			net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> {
				SkyTint.trigger();
				NukeFlash.trigger();
			});
			return null;
		}
	}
}