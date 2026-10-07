package com.ntmit.tileentity;

import com.hbm.api.tile.IHeatSource;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmit.blocks.BlockITServerColumn;
import com.ntmit.server.ServerConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

public class TileEntityITServerCompute extends TileEntityLoadedBase implements ITickable, IHeatSource {

	public int heat;

	public int maxHeat = ServerConfig.maxHeat;

	@Override
	public void update() {
		if (world.isRemote) return;

		if (heat > maxHeat) {
			heat = maxHeat;
		}

		if (this.world.getTotalWorldTime() % ServerConfig.groupRefreshInterval == 0L) {
			this.networkPackNT(20);
		}
	}

	public ForgeDirection getFacing() {
		return BlockITServerColumn.facingOf(world, pos);
	}

	public int getComputePoints() {
		return ServerConfig.computePoints;
	}

	public void getDiagData(NBTTagCompound nbt) {
		nbt.setInteger("compute_points", this.getComputePoints());
		nbt.setInteger("heat", this.heat);
	}

	@Override
	public int getHeatStored() {
		return heat;
	}

	@Override
	public void useUpHeat(int amount) {
		heat = Math.max(0, heat - amount);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.heat);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.heat = buf.readInt();
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		heat = nbt.getInteger("heat");
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("heat", heat);
		return nbt;
	}
}
