package com.ntmit.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PacketNTMITRbmkDebug implements IMessage {

	public boolean enabled;

	public PacketNTMITRbmkDebug() { }

	public PacketNTMITRbmkDebug(boolean enabled) {
		this.enabled = enabled;
	}

	public static void register() {
		PacketNTMITSteam.NETWORK.registerMessage(Handler.class, PacketNTMITRbmkDebug.class, 2, Side.CLIENT);
	}

	public static void send(EntityPlayerMP player, boolean enabled) {
		if (player == null) return;
		PacketNTMITSteam.NETWORK.sendTo(new PacketNTMITRbmkDebug(enabled), player);
	}

	public static void sendToAll(boolean enabled) {
		PacketNTMITSteam.NETWORK.sendToAll(new PacketNTMITRbmkDebug(enabled));
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.enabled = buf.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeBoolean(this.enabled);
	}

	public static class Handler implements IMessageHandler<PacketNTMITRbmkDebug, IMessage> {

		@Override
		public IMessage onMessage(PacketNTMITRbmkDebug message, MessageContext ctx) {
			net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(
					() -> com.ntmit.proxy.ClientProxy.handleRbmkDebugPacket(message.enabled));
			return null;
		}
	}
}
