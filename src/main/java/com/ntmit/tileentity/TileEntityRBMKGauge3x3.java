package com.ntmit.tileentity;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.network.RTTYSystem;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityRBMKGauge3x3 extends TileEntityLoadedBase implements ITickable, IGUIProvider, IControlReceiver {

	public static final int UNITS = 9;
	public static final int COLUMNS = 3;

	public final GaugeUnit[] units = new GaugeUnit[UNITS];

	public TileEntityRBMKGauge3x3() {
		for (int i = 0; i < UNITS; i++) {
			this.units[i] = new GaugeUnit(i);
		}
	}

	@Override
	public void update() {
		if (this.world == null) return;

		if (this.world.isRemote) {

			for (GaugeUnit unit : this.units) {
				unit.updateClient();
			}
		} else {
			for (GaugeUnit unit : this.units) {
				unit.update(this.world);
			}

			this.networkPackNT(50);
		}
	}

	public static class GaugeUnit {

		private static final int[] DEFAULT_COLORS = new int[]{0x800000, 0x804000, 0x808000, 0x000080};

		public boolean active = false;
		public boolean polling = false;
		public int color = 0x800000;
		public String label = "";
		public String channel = "";
		public long min = 0L;
		public long max = 100L;

		public long value = 0L;
		public double renderValue = 0L;
		public double lastRenderValue = 0L;

		public GaugeUnit(int index) {
			this.label = Integer.toString(index + 1);
			this.color = DEFAULT_COLORS[index % DEFAULT_COLORS.length];
		}

		public void update(World world) {
			if (!this.active || this.channel == null || this.channel.isEmpty()) return;

			RTTYSystem.RTTYChannel chan = RTTYSystem.listen(world, this.channel);
			if (chan != null && chan.timeStamp < world.getTotalWorldTime() - 1L) chan = null;

			if (chan != null && chan.signal != null) {
				try {
					this.value = Long.parseLong(chan.signal.toString());
				} catch (NumberFormatException e) {

				}
			} else if (this.polling) {
				this.value = 0L;
			}
		}

		@SideOnly(Side.CLIENT)
		public void updateClient() {
			this.lastRenderValue = this.renderValue;
			this.renderValue += ((double) this.value - this.renderValue) * 0.1D;
		}

		public void writeToNBT(NBTTagCompound nbt, int index) {
			nbt.setBoolean("active" + index, this.active);
			nbt.setBoolean("polling" + index, this.polling);
			nbt.setInteger("color" + index, this.color);
			nbt.setString("label" + index, this.label);
			nbt.setString("channel" + index, this.channel);
			nbt.setLong("min" + index, this.min);
			nbt.setLong("max" + index, this.max);
		}

		public void readFromNBT(NBTTagCompound nbt, int index) {
			this.active = nbt.getBoolean("active" + index);
			this.polling = nbt.getBoolean("polling" + index);
			this.color = nbt.getInteger("color" + index);
			this.label = nbt.getString("label" + index);
			this.channel = nbt.getString("channel" + index);
			this.min = nbt.getLong("min" + index);
			this.max = nbt.getLong("max" + index);
		}

		public void serialize(ByteBuf buf) {
			buf.writeBoolean(this.active);
			buf.writeBoolean(this.polling);
			buf.writeInt(this.color);
			writeString(buf, this.label);
			writeString(buf, this.channel);
			buf.writeLong(this.min);
			buf.writeLong(this.max);
			buf.writeLong(this.value);
		}

		public void deserialize(ByteBuf buf) {
			this.active = buf.readBoolean();
			this.polling = buf.readBoolean();
			this.color = buf.readInt();
			this.label = readString(buf);
			this.channel = readString(buf);
			this.min = buf.readLong();
			this.max = buf.readLong();
			this.value = buf.readLong();
		}
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new com.ntmit.inventory.gui.GuiRBMKGauge3x3(this);
	}

	@Override
	public void receiveControl(net.minecraft.entity.player.EntityPlayerMP player, NBTTagCompound data) {
		int active = data.getInteger("active");
		int polling = data.getInteger("polling");

		for (int i = 0; i < UNITS; i++) {
			GaugeUnit unit = this.units[i];
			unit.active = (active & 1 << i) != 0;
			unit.polling = (polling & 1 << i) != 0;

			unit.color = MathHelper.clamp(data.getInteger("color" + i), 0, 0xFFFFFF);
			unit.label = data.getString("label" + i);
			unit.channel = data.getString("channel" + i);
			unit.min = data.getLong("min" + i);
			unit.max = data.getLong("max" + i);
		}

		this.markDirty();
		this.networkPackNT(20);
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return true;
	}

	@Override
	public void serialize(ByteBuf buf) {
		for (GaugeUnit unit : this.units) {
			unit.serialize(buf);
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		for (GaugeUnit unit : this.units) {
			unit.deserialize(buf);
		}
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		for (int i = 0; i < UNITS; i++) {
			this.units[i].readFromNBT(nbt, i);
		}
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		for (int i = 0; i < UNITS; i++) {
			this.units[i].writeToNBT(nbt, i);
		}
		return nbt;
	}

	private static void writeString(ByteBuf buf, String text) {
		byte[] bytes = (text == null ? "" : text).getBytes(java.nio.charset.StandardCharsets.UTF_8);
		buf.writeInt(bytes.length);
		buf.writeBytes(bytes);
	}

	private static String readString(ByteBuf buf) {
		byte[] bytes = new byte[buf.readInt()];
		buf.readBytes(bytes);
		return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
	}
}
