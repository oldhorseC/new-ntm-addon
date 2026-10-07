package com.ntmit.packet;

import com.ntmit.lib.RefStrings;

import io.netty.buffer.ByteBuf;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

public class PacketNTMITSteam implements IMessage {

	public static final SimpleNetworkWrapper NETWORK = NetworkRegistry.INSTANCE.newSimpleChannel(RefStrings.MODID + "_steam");

	public double x;
	public double y;
	public double z;
	public float spread;
	public int count;
	public float base;
	public float max;
	public int life;
	public int color;
	public float rise;

	public PacketNTMITSteam() { }

	public PacketNTMITSteam(double x, double y, double z, float spread, int count, float base, float max, int life, int color, float rise) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.spread = spread;
		this.count = count;
		this.base = base;
		this.max = max;
		this.life = life;
		this.color = color;
		this.rise = rise;
	}

	public static void register() {
		NETWORK.registerMessage(Handler.class, PacketNTMITSteam.class, 0, Side.CLIENT);
	}

	public static void send(World world, double x, double y, double z, float spread, int count, float base, float max, int life, int color, float rise) {
		if (world == null || world.isRemote) return;
		try {
			NETWORK.sendToAllAround(new PacketNTMITSteam(x, y, z, spread, count, base, max, life, color, rise), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, 200.0D));
		} catch (Throwable throwable) {
			System.out.println("[ntm-it][diag] steam packet error: " + throwable);
		}
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.x = buf.readDouble();
		this.y = buf.readDouble();
		this.z = buf.readDouble();
		this.spread = buf.readFloat();
		this.count = buf.readInt();
		this.base = buf.readFloat();
		this.max = buf.readFloat();
		this.life = buf.readInt();
		this.color = buf.readInt();
		this.rise = buf.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeDouble(this.x);
		buf.writeDouble(this.y);
		buf.writeDouble(this.z);
		buf.writeFloat(this.spread);
		buf.writeInt(this.count);
		buf.writeFloat(this.base);
		buf.writeFloat(this.max);
		buf.writeInt(this.life);
		buf.writeInt(this.color);
		buf.writeFloat(this.rise);
	}

	public static class Handler implements IMessageHandler<PacketNTMITSteam, IMessage> {

		@Override
		public IMessage onMessage(PacketNTMITSteam message, MessageContext ctx) {
			net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(() -> com.ntmit.proxy.ClientProxy.handleSteamPacket(message));
			return null;
		}
	}
}
