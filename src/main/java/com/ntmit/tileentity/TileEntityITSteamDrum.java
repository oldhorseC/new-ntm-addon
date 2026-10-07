package com.ntmit.tileentity;

import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.I18nUtil;
import com.ntmit.blocks.BlockITSteamDrum;
import com.ntmit.fluids.NTMITFluids;
import com.ntmit.lib.NTMITWorldRules;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.state.IBlockState;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ITickable;

public class TileEntityITSteamDrum extends TileEntityLoadedBase implements ITickable, IFluidStandardReceiverMK2, IFluidStandardSenderMK2 {

	public static final int TANK_SIZE = 64000;
	public static final int RATE = 1000;
	public static final int STEAM_PER_MIXTURE = 870;

	public final FluidTankNTM in = new FluidTankNTM(Fluids.NONE, TANK_SIZE);
	public final FluidTankNTM outSteam = new FluidTankNTM(Fluids.NONE, TANK_SIZE * 2);
	public final FluidTankNTM outWater = new FluidTankNTM(Fluids.WATER, TANK_SIZE);

	private DirPos[] ports;
	private int lastConverted;
	private boolean active;

	@Override
	public void update() {
		if (world.isRemote) return;
		if (!NTMITWorldRules.wetSteam(this.world)) {
			this.active = false;
			this.lastConverted = 0;
			return;
		}
		if (this.ports == null) this.ports = this.buildPorts();

		FluidType mixture = this.in.getTankType();
		if (NTMITFluids.isMixture(mixture)) {
			for (DirPos port : this.ports) this.trySubscribe(mixture, world, port);
			this.convert();
			if (this.outSteam.getFill() > 0) {
				for (DirPos port : this.ports) this.tryProvide(this.outSteam, world, port);
			}
			if (this.outWater.getFill() > 0) {
				for (DirPos port : this.ports) this.tryProvide(this.outWater, world, port);
			}
		} else {
			this.active = false;
			this.lastConverted = 0;
			if (this.in.getFill() > 0) {
				for (DirPos port : this.ports) this.trySubscribe(mixture, world, port);
			}
		}

		if (this.world.getTotalWorldTime() % 20L == 0L) this.networkPackNT(50);
	}

	private void convert() {
		FluidType mixture = this.in.getTankType();
		FluidType dry = NTMITFluids.dryOf(mixture);
		if (this.outSteam.getTankType() != dry) {
			if (this.outSteam.getFill() > 0) {
				this.active = false;
				return;
			}
			this.outSteam.setTankType(dry);
		}

		int batch = Math.min(this.in.getFill(), RATE);
		int steamRoom = this.outSteam.getMaxFill() - this.outSteam.getFill();
		int waterRoom = this.outWater.getMaxFill() - this.outWater.getFill();
		int denom = 1000 - STEAM_PER_MIXTURE;
		if (steamRoom <= 0 || waterRoom <= 0) {
			this.active = false;
			return;
		}
		batch = Math.min(batch, steamRoom * 1000 / STEAM_PER_MIXTURE);
		batch = Math.min(batch, waterRoom * 1000 / denom);
		if (batch <= 0) {
			this.active = false;
			return;
		}

		int steam = batch * STEAM_PER_MIXTURE / 1000;
		int water = batch - steam;
		this.in.setFill(this.in.getFill() - batch);
		this.outSteam.setFill(this.outSteam.getFill() + steam);
		this.outWater.setFill(this.outWater.getFill() + water);
		this.lastConverted = batch;
		this.active = true;
		this.markDirty();
	}

	private DirPos[] buildPorts() {
		DirPos[] dirs = new DirPos[6];
		for (EnumFacing dir : EnumFacing.VALUES) {
			dirs[dir.getIndex()] = new DirPos(this.pos.offset(dir), ForgeDirection.getOrientation(dir.getIndex()));
		}
		return dirs;
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return new FluidTankNTM[] {this.in};
	}

	@Override
	public FluidTankNTM[] getSendingTanks() {
		return new FluidTankNTM[] {this.outSteam, this.outWater};
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		return new FluidTankNTM[] {this.in, this.outSteam, this.outWater};
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		return true;
	}

	public boolean setInputType(FluidType type) {
		if (type == null || type == Fluids.NONE) return false;
		if (this.in.getFill() > 0) return false;
		if (this.in.getTankType() == type) return false;
		this.in.setTankType(type);
		this.active = false;
		this.lastConverted = 0;
		this.markDirty();
		return true;
	}

	public void getDiagData(NBTTagCompound nbt) {
		nbt.setString("status", I18nUtil.resolveKey("hud.ntm-it.steam_drum.status." + this.statusKey()));
		nbt.setString("in_fluid", this.in.getTankType().getLocalizedName());
		nbt.setInteger("in", this.in.getFill());
		nbt.setString("out_fluid", this.outSteam.getTankType().getLocalizedName());
		nbt.setInteger("out", this.outSteam.getFill());
		nbt.setString("out2_fluid", this.outWater.getTankType().getLocalizedName());
		nbt.setInteger("out2", this.outWater.getFill());
		nbt.setInteger("rate", this.lastConverted);
	}

	private String statusKey() {
		if (!NTMITFluids.isMixture(this.in.getTankType())) return "no_mixture";
		if (this.in.getFill() <= 0) return "empty";
		if (this.outSteam.getFill() >= this.outSteam.getMaxFill()) return "output_full";
		return this.active ? "separating" : "idle";
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.lastConverted);
		buf.writeBoolean(this.active);
		this.in.serialize(buf);
		this.outSteam.serialize(buf);
		this.outWater.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.lastConverted = buf.readInt();
		this.active = buf.readBoolean();
		this.in.deserialize(buf);
		this.outSteam.deserialize(buf);
		this.outWater.deserialize(buf);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.in.readFromNBT(nbt, "in");
		this.outSteam.readFromNBT(nbt, "outSteam");
		this.outWater.readFromNBT(nbt, "outWater");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		this.in.writeToNBT(nbt, "in");
		this.outSteam.writeToNBT(nbt, "outSteam");
		this.outWater.writeToNBT(nbt, "outWater");
		return nbt;
	}
}