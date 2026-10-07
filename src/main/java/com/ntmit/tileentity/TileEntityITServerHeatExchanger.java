package com.ntmit.tileentity;

import com.hbm.api.fluidmk2.IFluidConnectorMK2;
import com.hbm.api.fluidmk2.IFluidStandardReceiverMK2;
import com.hbm.api.fluidmk2.IFluidStandardSenderMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Heatable;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.hbm.util.I18nUtil;
import com.ntmit.blocks.BlockITServerColumn;
import com.ntmit.server.ServerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TileEntityITServerHeatExchanger extends TileEntityLoadedBase implements ITickable, IHeatSource, IFluidStandardReceiverMK2, IFluidStandardSenderMK2, IFluidConnectorMK2 {

	public int heat;

	public int maxHeat = ServerConfig.heatExchangerMaxHeat;

	public int transferRate = ServerConfig.heatExchangerTransferRate;

	public final FluidTankNTM in = new FluidTankNTM(Fluids.WATER, ServerConfig.heatExchangerTankSize);
	public final FluidTankNTM out = new FluidTankNTM(Fluids.STEAM, ServerConfig.heatExchangerTankSize * 100);

	private int coolingRate;

	public boolean isOn;

	private long supplyHeat;
	private long supplyTicks;

	private boolean coolant;

	private DirPos[] fluidPos;

	private final List<BlockPos> groupMembers = new ArrayList<>();
	private BlockPos masterPos;
	private int groupSize = 1;
	private long lastGroupScan = Long.MIN_VALUE;

	@Override
	public void update() {
		if (world.isRemote) return;

		if (!this.isMaster()) {
			this.isOn = false;
			this.coolingRate = 0;
			return;
		}

		if (this.fluidPos == null) this.fluidPos = buildFluidPos();

		if (this.in.getTankType() != Fluids.NONE) {
			for (DirPos dirPos : this.fluidPos) {
				this.trySubscribe(this.in.getTankType(), world, dirPos);
			}
		}

		this.setupTanks();

		this.pullHeat();

		this.isOn = false;
		this.coolingRate = this.tryConvert();

		if (this.out.getFill() > 0) {
			for (DirPos dirPos : this.fluidPos) {
				this.tryProvide(this.out, world, dirPos);
			}
		}

		if (ServerConfig.debugHeatLog) {
			if (this.world.getTotalWorldTime() % 100L == 0L) {
				this.logHeatState();
				this.supplyHeat = 0;
				this.supplyTicks = 0;
			}
			this.supplyTicks++;
		}

		this.coolant = this.in.getTankType() != Fluids.NONE;
		if (this.world.getTotalWorldTime() % ServerConfig.groupRefreshInterval == 0L) {
			this.networkPackNT(20);
		}

		if (this.coolingRate > 0 || this.heat > 0) this.markDirty();
	}

	private void refreshGroupIfStale() {
		if (this.world == null) return;

		long scan = this.world.getTotalWorldTime() / Math.max(1, ServerConfig.groupRefreshInterval);
		if (scan == this.lastGroupScan) return;
		this.lastGroupScan = scan;

		this.groupMembers.clear();

		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		seen.add(this.pos);
		queue.add(this.pos);

		BlockPos best = this.pos;

		while (!queue.isEmpty() && this.groupMembers.size() < ServerConfig.maxHeatExchangerGroup) {
			BlockPos node = queue.poll();
			this.groupMembers.add(node);

			if (lowest(node, best)) best = node;

			for (ForgeDirection face : ALL_FACES) {
				BlockPos next = BlockITServerColumn.offset(node, face);
				if (!seen.add(next) || !this.world.isBlockLoaded(next)) continue;
				if (this.world.getTileEntity(next) instanceof TileEntityITServerHeatExchanger) queue.add(next);
			}
		}

		this.masterPos = best;
		this.groupSize = this.groupMembers.size();
	}

	private static boolean lowest(BlockPos candidate, BlockPos current) {
		if (candidate.getX() != current.getX()) return candidate.getX() < current.getX();
		if (candidate.getY() != current.getY()) return candidate.getY() < current.getY();
		return candidate.getZ() < current.getZ();
	}

	public boolean isMaster() {
		this.refreshGroupIfStale();
		return this.pos.equals(this.masterPos);
	}

	public boolean isGroupMember() {
		return !this.isMaster();
	}

	public int getGroupSize() {
		this.refreshGroupIfStale();
		return this.groupSize;
	}

	public TileEntityITServerHeatExchanger getMaster() {
		this.refreshGroupIfStale();
		if (this.pos.equals(this.masterPos)) return this;

		TileEntity tile = this.world == null ? null : this.world.getTileEntity(this.masterPos);
		if (tile instanceof TileEntityITServerHeatExchanger master && master != this) return master;

		return this;
	}

	protected void setupTanks() {
		if (this.in.getTankType().hasTrait(FT_Heatable.class)) {
			FT_Heatable trait = this.in.getTankType().getTrait(FT_Heatable.class);

			if (trait != null && trait.getEfficiency(FT_Heatable.HeatingType.BOILER) > 0D) {
				FT_Heatable.HeatingStep step = trait.getFirstStep();

				if (step != null && step.amountReq > 0) {
					this.out.setTankType(step.typeProduced);
					this.out.changeTankSize(this.in.getMaxFill() * step.amountProduced / step.amountReq);
					return;
				}
			}
		}

		this.in.setTankType(Fluids.NONE);
		this.out.setTankType(Fluids.NONE);
	}

	protected int tryConvert() {
		if (!this.in.getTankType().hasTrait(FT_Heatable.class)) return 0;

		FT_Heatable trait = this.in.getTankType().getTrait(FT_Heatable.class);
		if (trait == null || trait.getEfficiency(FT_Heatable.HeatingType.BOILER) <= 0D) return 0;

		FT_Heatable.HeatingStep step = trait.getFirstStep();
		if (step == null || step.amountReq <= 0 || step.amountProduced <= 0 || step.heatReq <= 0) return 0;

		int inputOps = this.in.getFill() / step.amountReq;
		int outputOps = (this.out.getMaxFill() - this.out.getFill()) / step.amountProduced;
		int heatOps = this.heat / step.heatReq;
		int ops = Math.min(inputOps, Math.min(outputOps, heatOps));
		if (ops <= 0) return 0;

		this.in.setFill(this.in.getFill() - step.amountReq * ops);
		this.out.setFill(this.out.getFill() + step.amountProduced * ops);
		this.heat -= step.heatReq * ops;
		this.isOn = true;
		return step.heatReq * ops;
	}

	private static final ForgeDirection[] HORIZONTAL_FACES = {ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST};

	private void pullHeat() {
		this.refreshGroupIfStale();

		Set<BlockPos> sources = new HashSet<>();
		for (BlockPos member : this.groupMembers) {
			for (ForgeDirection side : HORIZONTAL_FACES) {
				BlockPos neighbour = BlockITServerColumn.offset(member, side);
				TileEntity tile = this.world.isBlockLoaded(neighbour) ? this.world.getTileEntity(neighbour) : null;
				if (tile instanceof IHeatSource && !(tile instanceof TileEntityITServerHeatExchanger)) sources.add(neighbour);
			}
		}

		for (BlockPos sourcePos : sources) {
			if (this.heat >= this.maxHeat) return;
			TileEntity sourceTile = this.world.isBlockLoaded(sourcePos) ? this.world.getTileEntity(sourcePos) : null;
			if (!(sourceTile instanceof IHeatSource source)) continue;

			int moved = Math.min(Math.min(this.transferRate, source.getHeatStored()), this.maxHeat - this.heat);
			if (moved <= 0) continue;

			source.useUpHeat(moved);
			this.heat += moved;
			this.supplyHeat += moved;
		}
	}

	public boolean hasCoolant() {
		return this.getMaster().coolant;
	}

	public FluidType getInputType() {
		return this.in.getTankType();
	}

	public void setInputType(FluidType type) {
		this.in.setTankType(type);
		this.setupTanks();
		this.coolant = this.in.getTankType() != Fluids.NONE;
		this.markDirty();
	}

	public int getCoolingRate() {
		return this.getMaster().coolingRate;
	}

	public ForgeDirection getFacing() {
		return BlockITServerColumn.facingOf(world, pos);
	}

	private static final ForgeDirection[] ALL_FACES = {ForgeDirection.DOWN, ForgeDirection.UP, ForgeDirection.NORTH, ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.EAST};

	private DirPos[] buildFluidPos() {
		DirPos[] dirs = new DirPos[ALL_FACES.length];
		for (int i = 0; i < ALL_FACES.length; i++) {
			ForgeDirection dir = ALL_FACES[i];
			dirs[i] = new DirPos(pos.getX() + dir.offsetX, pos.getY() + dir.offsetY, pos.getZ() + dir.offsetZ, dir);
		}
		return dirs;
	}

	@Override
	public FluidTankNTM[] getReceivingTanks() {
		return new FluidTankNTM[] {this.getMaster().in};
	}

	@Override
	public FluidTankNTM[] getAllTanks() {
		TileEntityITServerHeatExchanger master = this.getMaster();
		return new FluidTankNTM[] {master.in, master.out};
	}

	@Override
	public FluidTankNTM[] getSendingTanks() {
		return new FluidTankNTM[] {this.getMaster().out};
	}

	@Override
	public boolean canConnect(FluidType type, ForgeDirection dir) {
		return true;
	}

	@Override
	public int getHeatStored() {
		return this.heat;
	}

	@Override
	public void useUpHeat(int amount) {
		this.heat = Math.max(0, this.heat - amount);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.heat);
		buf.writeInt(this.coolingRate);
		buf.writeBoolean(this.coolant);
		buf.writeBoolean(this.isOn);
		this.in.serialize(buf);
		this.out.serialize(buf);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.heat = buf.readInt();
		this.coolingRate = buf.readInt();
		this.coolant = buf.readBoolean();
		this.isOn = buf.readBoolean();
		this.in.deserialize(buf);
		this.out.deserialize(buf);
	}

	private void logHeatState() {
		FT_Heatable.HeatingStep step = null;
		if (this.in.getTankType().hasTrait(FT_Heatable.class)) {
			FT_Heatable trait = this.in.getTankType().getTrait(FT_Heatable.class);
			if (trait != null) step = trait.getFirstStep();
		}

	}

	private String statusKey() {
		if (this.in.getTankType() == Fluids.NONE) return "no_fluid_type";
		if (this.in.getFill() <= 0) return "empty";
		if (!this.in.getTankType().hasTrait(FT_Heatable.class)) return "not_heatable";

		FT_Heatable trait = this.in.getTankType().getTrait(FT_Heatable.class);
		if (trait == null || trait.getEfficiency(FT_Heatable.HeatingType.BOILER) <= 0D) return "not_heatable";

		FT_Heatable.HeatingStep step = trait.getFirstStep();
		if (step == null || step.heatReq <= 0) return "not_heatable";
		if (this.out.getFill() >= this.out.getMaxFill()) return "output_full";
		if (this.heat < step.heatReq) return "heating_up";

		return this.isOn ? "boiling" : "idle";
	}

	public void getDiagData(NBTTagCompound nbt) {
		this.refreshGroupIfStale();

		TileEntityITServerHeatExchanger master = this.getMaster();
		if (master != this && master.isMaster()) {
			master.getDiagData(nbt);
			nbt.setInteger("members", master.getGroupSize());
			return;
		}

		nbt.setString("status", I18nUtil.resolveKey("hud.ntm-it.server.status." + this.statusKey()));
		nbt.setString("in_fluid", this.in.getTankType().getLocalizedName());
		nbt.setInteger("in", this.in.getFill());
		nbt.setString("out_fluid", this.out.getTankType().getLocalizedName());
		nbt.setInteger("out", this.out.getFill());
		nbt.setInteger("heat", this.heat);
		nbt.setInteger("members", this.getGroupSize());
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);

		this.heat = Math.min(nbt.getInteger("heat"), this.maxHeat);
		this.in.readFromNBT(nbt, "in");
		this.out.readFromNBT(nbt, "out");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", this.heat);
		this.in.writeToNBT(nbt, "in");
		this.out.writeToNBT(nbt, "out");
		return nbt;
	}
}
