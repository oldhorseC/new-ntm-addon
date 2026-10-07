package com.ntmit.lib;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RbmkSurge {

	public static final double TEMP_RATIO = 0.8D;
	public static final float STEAM_BOOST = 2.0F;
	public static final double ADD_NEUTRON = 50.0D;
	public static final double NEUTRON_RATIO = 2.0D;
	public static final double ADD_TRANSMITTANCE = 0.02D;
	public static final double TRANSMITTANCE_RATIO = 2.0D;
	public static final double TRANSMITTANCE_MIN = 0.02D;
	public static final float OVERHEAT_MULTIPLIER = 2.0F;

	private RbmkSurge() { }

	public static float progress(TileEntityRBMKBase te) {
		if (te == null) return 0F;
		World world = te.getWorld();
		if (world == null || world.isRemote) return 0F;
		double max = te.maxHeat();
		if (max <= 0D) return 0F;
		double start = max * TEMP_RATIO;
		double heat = te.heat;
		if (heat <= start) return 0F;
		return RbmkFeedback.shape((float) Math.min(1D, (heat - start) / (max - start)));
	}

	public static boolean overTemp(TileEntityRBMKBase te) {
		return te != null && te.heat >= te.maxHeat();
	}

	public static float steamProgress(TileEntityRBMKBase te) {
		if (te == null) return 0F;
		World world = te.getWorld();
		if (world == null || world.isRemote) return 0F;
		return Math.max(RbmkFeedback.reasimProgress(te.reasimSteam), RbmkFeedback.cellProgress(world, te.getPos()));
	}

	public static boolean late(TileEntityRBMKBase te) {
		if (te == null) return false;
		World world = te.getWorld();
		if (world == null || world.isRemote) return false;
		if (overTemp(te)) return true;
		BlockPos pos = te.getPos();
		return RbmkFeedback.late(RbmkFeedback.cellTank(world, pos), Math.max(te.reasimSteam, RbmkFeedback.cellReasim(world, pos)));
	}

	public static float channelProgress(TileEntityRBMKBase te) {
		if (te == null) return 0F;
		World world = te.getWorld();
		if (world == null || world.isRemote) return 0F;
		RbmkFeedback.Cell cell = RbmkFeedback.cell(world, te.getPos());
		float p = RbmkFeedback.reasimProgress(te.reasimSteam);
		if (cell != null) p = Math.max(p, cell.progress());
		return Math.max(p, progress(te));
	}

	public static double applyFlux(TileEntityRBMKBase te, double base) {
		if (te == null) return base;
		World world = te.getWorld();
		if (world == null || world.isRemote) return base;
		RbmkFeedback.Cell cell = RbmkFeedback.cell(world, te.getPos());
		float p = progress(te);
		if (cell != null) p = Math.max(p, Math.max(RbmkFeedback.reasimProgress(te.reasimSteam), cell.progress()));
		if (p <= 0F) return base;
		boolean over = overTemp(te);
		double mult = over ? OVERHEAT_MULTIPLIER : 1.0D;
		if (over || (cell != null && RbmkFeedback.late(cell.tank, Math.max(te.reasimSteam, cell.reasim)))) {
			return base * (1.0D + NEUTRON_RATIO * (double) p) * mult;
		}
		return base + ADD_NEUTRON * (double) p * mult;
	}

	public static double applyTransmittance(TileEntityRBMKBase te, double base) {
		if (te == null) return base;
		World world = te.getWorld();
		if (world == null || world.isRemote) return base;
		RbmkFeedback.Cell cell = RbmkFeedback.cell(world, te.getPos());
		float p = progress(te);
		if (cell != null) p = Math.max(p, Math.max(RbmkFeedback.reasimProgress(te.reasimSteam), cell.progress()));
		if (p <= 0F) return base;
		boolean over = overTemp(te);
		double mult = over ? OVERHEAT_MULTIPLIER : 1.0D;
		if (over || (cell != null && RbmkFeedback.late(cell.tank, Math.max(te.reasimSteam, cell.reasim)))) {
			return Math.min(1.0D, Math.max(base, TRANSMITTANCE_MIN) * (1.0D + TRANSMITTANCE_RATIO * (double) p) * mult);
		}
		return Math.min(1.0D, base + ADD_TRANSMITTANCE * (double) p * mult);
	}

	public static boolean active(TileEntityRBMKBase te) {
		return progress(te) > 0F;
	}

	public static double surgePoint(TileEntityRBMKBase te) {
		if (te == null) return 0D;
		return te.maxHeat() * TEMP_RATIO;
	}

	public static float steamBoost(TileEntityRBMKBase te) {
		return channelProgress(te) * STEAM_BOOST;
	}
}