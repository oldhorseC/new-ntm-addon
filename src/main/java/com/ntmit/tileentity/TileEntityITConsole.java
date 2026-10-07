package com.ntmit.tileentity;

import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.lib.Library;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.RBMKColumn.ColumnType;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole.ScreenType;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlManual;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlManual.RBMKColor;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;
import com.hbm.util.BobMathUtil;
import com.hbm.util.BufferUtil;
import com.hbm.util.EnumUtil;
import com.hbm.util.I18nUtil;
import com.ntmit.inventory.gui.GUITestConsole;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TileEntityITConsole extends TileEntityLoadedBase implements IControlReceiver, IGUIProvider, ITickable {

	public static final int fluxDisplayBuffer = 60;
	public int[] fluxBuffer = new int[fluxDisplayBuffer];

	public RBMKColumn[] columns = new RBMKColumn[15 * 15];
	public RBMKScreen[] screens = new RBMKScreen[6];
	private int targetX;
	private int targetY;
	private int targetZ;
	private byte rotation;

	public TileEntityITConsole() {
		for (int i = 0; i < screens.length; i++) {
			screens[i] = new RBMKScreen();
		}
	}

	@Override
	public void update() {
		if (!world.isRemote) {
			if (this.world.getTotalWorldTime() % 10 == 0) {
				rescan();
				prepareScreenInfo();
			}
			networkPackNT(50);
		}
	}

	private void rescan() {
		double flux = 0;

		for (int index = 0; index < columns.length; index++) {
			final int rx = getXFromIndex(index);
			final int rz = getZFromIndex(index);

			TileEntity te = world.isBlockLoaded(new BlockPos(targetX + rx, targetY, targetZ + rz)) ? world.getTileEntity(new BlockPos(targetX + rx, targetY, targetZ + rz)) : null;

			if (te instanceof TileEntityRBMKBase rbmk) {

			com.hbm.tileentity.machine.rbmk.RBMKColumn col = rbmk.getConsoleData();
			NBTTagCompound data = new NBTTagCompound();
			data.setDouble("heat", col.heat);
			data.setDouble("maxHeat", col.maxHeat);
			data.setBoolean("moderated", col.moderated);
			data.setInteger("reasimWater", col.reasimWater);
			data.setInteger("reasimSteam", col.reasimSteam);
			data.setInteger("indicator", col.indicator);
			columns[index] = new RBMKColumn(col.type, data);
				columns[index].data.setDouble("heat", rbmk.heat);
				columns[index].data.setDouble("maxHeat", rbmk.maxHeat());
				if (rbmk.isModerated()) columns[index].data.setBoolean("moderated", true);

				if (te instanceof TileEntityRBMKRod fuel) {
					flux += fuel.lastFluxQuantity;
				}
			} else {
				columns[index] = null;
			}
		}

		for (int i = 0; i < this.fluxBuffer.length - 1; i++) {
			this.fluxBuffer[i] = this.fluxBuffer[i + 1];
		}
		this.fluxBuffer[this.fluxBuffer.length - 1] = (int) flux;
	}

	@SuppressWarnings("incomplete-switch")
	private void prepareScreenInfo() {
		for (RBMKScreen screen : this.screens) {
			if (screen.type == ScreenType.NONE) {
				screen.display = null;
				continue;
			}

			double value = 0;
			int count = 0;

			for (Integer i : screen.columns) {
				RBMKColumn col = this.columns[i];
				if (col == null) continue;

				switch (screen.type) {
					case COL_TEMP:
						count++;
						value += col.data.getDouble("heat");
						break;
					case FUEL_DEPLETION:
						if (col.data.hasKey("enrichment")) {
							count++;
							value += (100D - (col.data.getDouble("enrichment") * 100D));
						}
						break;
					case FUEL_POISON:
						if (col.data.hasKey("xenon")) {
							count++;
							value += col.data.getDouble("xenon");
						}
						break;
					case FUEL_TEMP:
						if (col.data.hasKey("c_heat")) {
							count++;
							value += col.data.getDouble("c_heat");
						}
						break;
					case ROD_EXTRACTION:
						if (col.data.hasKey("level")) {
							count++;
							value += col.data.getDouble("level") * 100;
						}
						break;
				}
			}

			double result = value / (double) count;
			String text = ((int) (result * 10)) / 10D + "";

			text = switch (screen.type) {
				case COL_TEMP -> "rbmk.screen.temp=" + text + "°C";
				case FUEL_DEPLETION -> "rbmk.screen.depletion=" + text + "%";
				case FUEL_POISON -> "rbmk.screen.xenon=" + text + "%";
				case FUEL_TEMP -> "rbmk.screen.core=" + text + "°C";
				case ROD_EXTRACTION -> "rbmk.screen.rod=" + text + "%";
				default -> text;
			};

			screen.display = text;
		}
	}

	@Override
	public void serialize(ByteBuf buf) {
		if (this.world.getTotalWorldTime() % 10 == 0) {
			buf.writeBoolean(true);

			for (RBMKColumn column : this.columns) {
				if (column == null || column.type == null) {
					buf.writeByte(-1);
				} else {
					buf.writeByte((byte) column.type.ordinal());
					BufferUtil.writeNBT(buf, column.data);
				}
			}

			BufferUtil.writeIntArray(buf, fluxBuffer);

			for (RBMKScreen screen : this.screens) {
				// BufferUtil.writeString -> utf8Bytes -> seq.length()：传 null 会抛 NullPointerException，
				// 而 screen.display 的默认值就是 null，因此这里必须写成空串（与 HBM 官方 RBMKConsole 的
				// display.isEmpty() ? null : display 约定保持一致）。
				BufferUtil.writeString(buf, screen.display == null ? "" : screen.display);
			}
		} else {
			buf.writeBoolean(false);
			for (RBMKScreen screen : screens) {
				buf.writeByte((byte) screen.type.ordinal());
			}
		}
	}

	@Override
	public void deserialize(ByteBuf buf) {
		if (buf.readBoolean()) {
			for (int i = 0; i < this.columns.length; i++) {
				byte ordinal = buf.readByte();
				if (ordinal == -1) {
					this.columns[i] = null;
				} else {
					this.columns[i] = new RBMKColumn(ColumnType.values()[ordinal], BufferUtil.readNBT(buf));
				}
			}

			this.fluxBuffer = BufferUtil.readIntArray(buf);

			for (RBMKScreen screen : this.screens) {
				String display = BufferUtil.readString(buf);
				screen.display = display.isEmpty() ? null : display;
			}
		} else {
			for (RBMKScreen screen : this.screens) {
				screen.type = ScreenType.values()[buf.readByte()];
			}
		}
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return new Vec3d(pos.getX() - player.posX, pos.getY() - player.posY, pos.getZ() - player.posZ).length() < 20;
	}

	/** The same entry point without a player, for calls from inside this mod such as the trial block. */
	public void receiveControl(NBTTagCompound data) {
		this.receiveControl(null, data);
	}

	@Override
	public void receiveControl(net.minecraft.entity.player.EntityPlayerMP player, NBTTagCompound data) {
		if (data.getBoolean("ntmit_az5")) com.ntmit.lib.RbmkJumpHandler.az5Press(this.world, this.getPos());
		if (data.hasKey("level")) {
			Set<String> keys = data.getKeySet();
			for (String key : keys) {
				if (key.startsWith("sel_")) {
					int index = data.getInteger(key);
					int x = getXFromIndex(index);
					int z = getZFromIndex(index);

					TileEntity te = world.isBlockLoaded(new BlockPos(targetX + x, targetY, targetZ + z)) ? world.getTileEntity(new BlockPos(targetX + x, targetY, targetZ + z)) : null;
					if (te instanceof TileEntityRBMKControlManual rod) {
						rod.startingLevel = rod.level;
						rod.setTarget(MathHelper.clamp(data.getDouble("level"), 0, 1));
						te.markDirty();
					}
				}
			}
		}

		if (data.hasKey("toggle")) {
			int slot = data.getByte("toggle");
			int next = this.screens[slot].type.ordinal() + 1;
			ScreenType type = ScreenType.values()[next % ScreenType.values().length];
			this.screens[slot].type = type;
		}

		if (data.hasKey("id")) {
			int slot = data.getByte("id");
			List<Integer> list = new ArrayList<>();

			for (int i = 0; i < 15 * 15; i++) {
				if (data.getBoolean("s" + i)) {
					list.add(i);
				}
			}

			Integer[] cols = list.toArray(new Integer[0]);
			this.screens[slot].columns = cols;
		}

		if (data.hasKey("assignColor")) {
			int color = data.getByte("assignColor");
			int[] cols = data.getIntArray("cols");

			for (int i : cols) {
				int x = getXFromIndex(i);
				int z = getZFromIndex(i);

				TileEntity te = world.isBlockLoaded(new BlockPos(targetX + x, targetY, targetZ + z)) ? world.getTileEntity(new BlockPos(targetX + x, targetY, targetZ + z)) : null;

				if (te instanceof TileEntityRBMKControlManual rod) {
					rod.color = EnumUtil.grabEnumSafely(RBMKColor.class, color);
					te.markDirty();
				}
			}
		}

		if (data.hasKey("compressor")) {
			int[] cols = data.getIntArray("cols");

			for (int i : cols) {
				int x = getXFromIndex(i);
				int z = getZFromIndex(i);

				TileEntity te = world.isBlockLoaded(new BlockPos(targetX + x, targetY, targetZ + z)) ? world.getTileEntity(new BlockPos(targetX + x, targetY, targetZ + z)) : null;

				if (te instanceof TileEntityRBMKBoiler boiler) {
					boiler.cyceCompressor();
				}
			}
		}
	}

	public void setTarget(int x, int y, int z) {
		this.targetX = x;
		this.targetY = y;
		this.targetZ = z;
		this.markDirty();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.targetX = nbt.getInteger("tX");
		this.targetY = nbt.getInteger("tY");
		this.targetZ = nbt.getInteger("tZ");

		for (int i = 0; i < this.screens.length; i++) {
			this.screens[i].type = ScreenType.values()[nbt.getByte("t" + i)];
			this.screens[i].columns = Arrays.stream(nbt.getIntArray("s" + i)).boxed().toArray(Integer[]::new);
		}
		rotation = nbt.getByte("rotation");
	}

	@Override
	public @NotNull NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);

		nbt.setInteger("tX", this.targetX);
		nbt.setInteger("tY", this.targetY);
		nbt.setInteger("tZ", this.targetZ);

		for (int i = 0; i < this.screens.length; i++) {
			nbt.setByte("t" + i, (byte) this.screens[i].type.ordinal());
			nbt.setIntArray("s" + i, Arrays.stream(this.screens[i].columns).mapToInt(Integer::intValue).toArray());
		}
		nbt.setByte("rotation", rotation);

		return nbt;
	}

	public void rotate() {
		rotation = (byte) ((rotation + 1) % 4);
	}

	public int getXFromIndex(int col) {
		final int i = col % 15 - 7;
		final int j = col / 15 - 7;
		return switch (rotation) {
			case 0 ->
					i;
			case 1 ->
					-j;
			case 2 ->
					-i;
			case 3 ->
					j;
			default -> i;
		};
	}

	public int getZFromIndex(int col) {
		final int i = col % 15 - 7;
		final int j = col / 15 - 7;
		return switch (rotation) {
			case 0 ->
					j;
			case 1 ->
					i;
			case 2 ->
					-j;
			case 3 ->
					-i;
			default -> j;
		};
	}

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		return new GUITestConsole(player.inventory, this);
	}

	public static class RBMKColumn {
		public ColumnType type;
		public NBTTagCompound data;

		public RBMKColumn(ColumnType type) {
			this.type = type;
		}

		RBMKColumn(ColumnType type, NBTTagCompound data) {
			this.type = type;
			this.data = (data != null) ? data : new NBTTagCompound();
		}

		@SuppressWarnings("incomplete-switch")
		@SideOnly(Side.CLIENT)
		public List<String> getFancyStats() {
			if (this.data == null) return null;

			List<String> stats = new ArrayList<>();
			stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.heat", ((int) ((this.data.getDouble("heat") * 10D)) / 10D) + "°C"));

			switch (this.type) {
				case FUEL:
				case FUEL_SIM:
					if (this.data.hasKey("rod_name"))
						stats.add("§3" + I18n.format("rbmk.rod.name") + " " + I18n.format(this.data.getString("rod_name") + ".name"));
					else stats.add("§3" + I18n.format("rbmk.rod.name"));
					stats.add(TextFormatting.GREEN + I18nUtil.resolveKey("rbmk.rod.depletion", ((int) (((1D - this.data.getDouble("enrichment")) * 100000)) / 1000D) + "%"));
					stats.add(TextFormatting.DARK_PURPLE + I18nUtil.resolveKey("rbmk.rod.xenon", ((int) (((this.data.getDouble("xenon")) * 1000D)) / 1000D) + "%"));
					stats.add(TextFormatting.DARK_RED + I18nUtil.resolveKey("rbmk.rod.coreTemp", ((int) ((this.data.getDouble("c_coreHeat") * 10D)) / 10D) + "°C"));
					stats.add(TextFormatting.RED + I18nUtil.resolveKey("rbmk.rod.skinTemp", ((int) ((this.data.getDouble("c_heat") * 10D)) / 10D) + "°C", ((int) ((this.data.getDouble("c_maxHeat") * 10D)) / 10D) + "°C"));
					break;

				case BOILER:
					stats.add(TextFormatting.BLUE + I18nUtil.resolveKey("rbmk.boiler.water", this.data.getInteger("water"), this.data.getInteger("maxWater")));
					stats.add(TextFormatting.WHITE + I18nUtil.resolveKey("rbmk.boiler.steam", this.data.getInteger("steam"), this.data.getInteger("maxSteam")));
					stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.boiler.type", Fluids.fromID(this.data.getShort("type")).getLocalizedName()));
					break;
				case CONTROL:
					if (this.data.hasKey("color")) {
						short col = this.data.getShort("color");
						if (col >= 0 && col < RBMKColor.values().length) {
							stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.control." + RBMKColor.values()[col].name().toLowerCase(Locale.US)));
						}
					}

				case CONTROL_AUTO:
					stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.control.level", ((int) ((this.data.getDouble("level") * 100D))) + "%"));
					break;

				case HEATEX:
					stats.add(TextFormatting.BLUE + Fluids.fromID(this.data.getShort("type")).getLocalizedName() + " " + this.data.getInteger("water") + "/" + this.data.getInteger("maxWater") + "mB");
					stats.add(TextFormatting.RED + Fluids.fromID(this.data.getShort("hottype")).getLocalizedName() + " " + this.data.getInteger("steam") + "/" + this.data.getInteger("maxSteam") + "mB");
					break;
				case COOLER:
					stats.add(TextFormatting.AQUA + I18nUtil.resolveKey("rbmk.cooler.cooling", this.data.getInteger("cooled") * 20));
					stats.add(TextFormatting.DARK_AQUA + I18nUtil.resolveKey("rbmk.cooler.cryo", this.data.getInteger("cryo")));
					break;
				case OUTGASSER:
					double flux = this.data.getDouble("usedFlux");
					double progress = this.data.getDouble("progress");
					double maxProgress = this.data.getDouble("maxProgress");
					int eta = 0;
					if (flux > 0) eta = (int) ((maxProgress - progress) / flux);
					stats.add(TextFormatting.GOLD + I18nUtil.resolveKey("rbmk.outgasser.eta", BobMathUtil.toDate(BobMathUtil.ticksToDate(eta, 72000))));
					stats.add(TextFormatting.AQUA + I18nUtil.resolveKey("rbmk.outgasser.flux", Library.getShortNumber((long) flux)));
					stats.add(TextFormatting.DARK_AQUA + I18nUtil.resolveKey("rbmk.outgasser.progress", Library.getShortNumber((long) progress), Library.getShortNumber((long) maxProgress), Library.getPercentage(progress / maxProgress)));
					stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.outgasser.gas", this.data.getInteger("gas"), this.data.getInteger("maxGas")));
					break;
			}

			if (data.getBoolean("moderated")) {
				stats.add(TextFormatting.YELLOW + I18nUtil.resolveKey("rbmk.moderated"));
			}

			return stats;
		}
	}

	public static class RBMKScreen {
		public ScreenType type = ScreenType.NONE;
		public Integer[] columns = new Integer[0];
		public String display = null;

		RBMKScreen() {
		}

		RBMKScreen(ScreenType type, Integer[] columns, String display) {
			this.type = type;
			this.columns = columns;
			this.display = display;
		}
	}
}
