package com.ntmit.packet;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class PacketNTMITTurbinePaint implements IMessage {

	public BlockPos pos;
	public int color;

	public PacketNTMITTurbinePaint() {
	}

	public PacketNTMITTurbinePaint(BlockPos pos, int color) {
		this.pos = pos;
		this.color = color;
	}

	public static void register() {
		PacketNTMITSteam.NETWORK.registerMessage(Handler.class, PacketNTMITTurbinePaint.class, 3, Side.CLIENT);
	}

	public static void sendAround(World world, BlockPos pos, int color) {
		if (world == null || world.isRemote || pos == null) return;
		try {
			PacketNTMITSteam.NETWORK.sendToAllAround(new PacketNTMITTurbinePaint(pos, color),
					new NetworkRegistry.TargetPoint(world.provider.getDimension(),
							pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 256.0D));
		} catch (Throwable throwable) {
		}
	}

	public static void sendTo(EntityPlayerMP player, BlockPos pos, int color) {
		if (player == null || pos == null) return;
		PacketNTMITSteam.NETWORK.sendTo(new PacketNTMITTurbinePaint(pos, color), player);
	}

	@Override
	public void fromBytes(ByteBuf buf) {
		this.pos = BlockPos.fromLong(buf.readLong());
		this.color = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeLong(this.pos == null ? 0L : this.pos.toLong());
		buf.writeInt(this.color);
	}

	public static class Handler implements IMessageHandler<PacketNTMITTurbinePaint, IMessage> {

		@Override
		public IMessage onMessage(PacketNTMITTurbinePaint message, MessageContext ctx) {
			net.minecraft.client.Minecraft.getMinecraft().addScheduledTask(
					() -> com.ntmit.proxy.ClientProxy.handleTurbinePaintPacket(message));
			return null;
		}
	}
}
