package com.ntmit.lib;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
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

	public static float tankBoost(double fill, FluidType type) {
		return tankProgress(fill, type) * STEAM_BOOST;
	}

	public static float tankBoost(double fill) {
		return tankBoost(fill, Fluids.STEAM);
	}

	public static float reasimBoost(int steam) {
		return reasimProgress(steam) * STEAM_BOOST;
	}

	public static float tankProgress(double fill, FluidType type) {
		return (float) RbmkSteamCurve.feedback(RbmkSteamCurve.normalFraction(fill, type));
	}

	public static float tankProgress(double fill) {
		return tankProgress(fill, Fluids.STEAM);
	}

	public static float reasimProgress(int steam) {
		return (float) RbmkSteamCurve.feedback(RbmkSteamCurve.reasimFraction(steam));
	}

	public static boolean late(double fill, FluidType type, int reasim) {
		double s = Math.max(RbmkSteamCurve.normalFraction(fill, type), RbmkSteamCurve.reasimFraction(reasim));
		return RbmkSteamCurve.late(s);
	}

	public static boolean late(double fill, int reasim) {
		return late(fill, Fluids.STEAM, reasim);
	}

	public static final class Cell {
		public final double tank;
		public final int reasim;
		public final FluidType tankType;
		public final double tankFraction;
		public final double reasimFraction;

		public Cell(double tank, int reasim, FluidType tankType, double tankFraction, double reasimFraction) {
			this.tank = tank;
			this.reasim = reasim;
			this.tankType = tankType;
			this.tankFraction = tankFraction;
			this.reasimFraction = reasimFraction;
		}

		public float progress() {
			return (float) RbmkSteamCurve.feedback(Math.max(this.tankFraction, this.reasimFraction));
		}

		public boolean late() {
			return RbmkSteamCurve.late(Math.max(this.tankFraction, this.reasimFraction));
		}
	}

	public static Cell cell(World world, BlockPos pos) {
		if (world == null || world.isRemote) return null;
		double tank = 0D;
		FluidType tankType = Fluids.STEAM;
		double tankFraction = 0D;
		int reasim = 0;
		double reasimFraction = 0D;
		for (int dx = -BURN_SCAN_RADIUS; dx <= BURN_SCAN_RADIUS; dx++) {
			for (int dz = -BURN_SCAN_RADIUS; dz <= BURN_SCAN_RADIUS; dz++) {
				BlockPos scan = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
				if (!world.isBlockLoaded(scan)) continue;
				TileEntity te = world.getTileEntity(scan);
				if (te instanceof TileEntityRBMKBoiler boiler) {
					double fraction = RbmkSteamCurve.normalFraction(boiler.steam.getFill(), boiler.steam.getTankType());
					if (fraction > tankFraction) {
						tankFraction = fraction;
						tank = boiler.steam.getFill();
						tankType = boiler.steam.getTankType();
					}
				}
				if (te instanceof TileEntityRBMKBase base) {
					double fraction = RbmkSteamCurve.reasimFraction(base.reasimSteam);
					if (base.reasimSteam > reasim) reasim = base.reasimSteam;
					if (fraction > reasimFraction) reasimFraction = fraction;
				}
			}
		}
		return new Cell(tank, reasim, tankType, tankFraction, reasimFraction);
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
