package com.ntmit.lib;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RbmkFeedback {

	public static final float STEAM_BOOST = 0.40F;
	public static final float BURN_BOOST = 0.40F;
	public static final int BURN_SCAN_RADIUS = 1;

	private RbmkFeedback() { }

	public static final double CURVE_STEEP = 3.0D;

	public static float shape(float progress) {
		if (progress <= 0F) return 0F;
		if (progress >= 1F) return 1F;
		return (float) ((Math.exp(CURVE_STEEP * (double) progress) - 1.0D) / (Math.exp(CURVE_STEEP) - 1.0D));
	}

	public static float progress(double value, double trigger, double steep, double mult, double full) {
		if (value <= trigger) return 0F;
		if (full <= trigger) return 1F;
		if (value < steep) {
			return EARLY_SHARE * shape((float) ((value - trigger) / (steep - trigger)));
		}
		if (value < mult) {
			return EARLY_SHARE + (STEADY_SHARE - EARLY_SHARE) * shape((float) ((value - steep) / (mult - steep)));
		}
		if (value >= full || full <= mult) return 1F;
		return STEADY_SHARE + (1F - STEADY_SHARE) * (float) Math.pow((value - mult) / (full - mult), POWER_SEGMENT);
	}

	public static final double TANK_TRIGGER = 400000D;
	public static final double TANK_STEEP = 1200000D;
	public static final double TANK_MULT = 1500000D;
	public static final int REASIM_TRIGGER = 16000;
	public static final int REASIM_STEEP = 48000;
	public static final int REASIM_MULT = 60000;
	public static final float EARLY_SHARE = 0.10F;
	public static final float STEADY_SHARE = 0.25F;
	public static final double POWER_SEGMENT = 3.0D;

	public static float tankBoost(double fill) {
		return tankProgress(fill) * STEAM_BOOST;
	}

	public static float reasimBoost(int steam) {
		return reasimProgress(steam) * STEAM_BOOST;
	}

	public static float tankProgress(double fill) {
		return progress(fill, TANK_TRIGGER, TANK_STEEP, TANK_MULT, RbmkJumpHandler.PRESSURE_BURST_THRESHOLD);
	}

	public static float reasimProgress(int steam) {
		return progress(steam, REASIM_TRIGGER, REASIM_STEEP, REASIM_MULT, RbmkJumpHandler.REASIM_BURST_THRESHOLD);
	}

	public static boolean late(double fill, int reasim) {
		return fill >= TANK_MULT || reasim >= REASIM_MULT;
	}

	public static final class Cell {
		public final double tank;
		public final int reasim;

		public Cell(double tank, int reasim) {
			this.tank = tank;
			this.reasim = reasim;
		}

		public float progress() {
			return Math.max(tankProgress(this.tank), reasimProgress(this.reasim));
		}
	}

	public static Cell cell(World world, BlockPos pos) {
		if (world == null || world.isRemote) return null;
		double tank = 0D;
		int reasim = 0;
		for (int dx = -BURN_SCAN_RADIUS; dx <= BURN_SCAN_RADIUS; dx++) {
			for (int dz = -BURN_SCAN_RADIUS; dz <= BURN_SCAN_RADIUS; dz++) {
				TileEntity te = world.isBlockLoaded(new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz)) ? world.getTileEntity(new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz)) : null;
				if (te instanceof TileEntityRBMKBoiler boiler) tank = Math.max(tank, boiler.steam.getFill());
				if (te instanceof TileEntityRBMKBase base) reasim = Math.max(reasim, base.reasimSteam);
			}
		}
		return new Cell(tank, reasim);
	}

	public static double cellTank(World world, BlockPos pos) {
		Cell cell = cell(world, pos);
		return cell == null ? 0D : cell.tank;
	}

	public static int cellReasim(World world, BlockPos pos) {
		Cell cell = cell(world, pos);
		return cell == null ? 0 : cell.reasim;
	}

	public static float cellProgress(World world, BlockPos pos) {
		Cell cell = cell(world, pos);
		return cell == null ? 0F : cell.progress();
	}

	public static float burnBoost(World world, BlockPos pos) {
		return cellProgress(world, pos) * BURN_BOOST;
	}
}