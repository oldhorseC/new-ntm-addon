package com.ntmit.lib;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.ntmit.fluids.NTMITFluids;

public final class RbmkSteamCurve {

	public static final int REASIM_CAPACITY = 80000;
	public static final int REASIM_Z_START = 4000;
	public static final int REASIM_A_START = 16000;
	public static final int REASIM_B_START = 40000;
	public static final int REASIM_C_START = 72000;

	public static final int STEAM_THRESHOLD = 2000000;
	public static final int HOTSTEAM_THRESHOLD = 1700000;
	public static final int SUPERHOTSTEAM_THRESHOLD = 1400000;
	public static final int ULTRAHOTSTEAM_THRESHOLD = 1100000;

	public static final double Z_START = 0.10D;
	public static final double A_START = 0.50D;
	public static final double B_START = 0.70D;
	public static final double C_START = 0.95D;

	public static final int A_PERIOD = 40;
	public static final int B_PERIOD = 20;
	public static final int C_PERIOD = 10;
	public static final int D_PERIOD = 5;

	public static final float A_AMPLITUDE = 0.30F;
	public static final float B_AMPLITUDE = 0.50F;
	public static final float C_AMPLITUDE = 0.60F;
	public static final float D_AMPLITUDE = 0.80F;

	public static final double F0 = 0.0D;
	public static final double K1 = 0.75D;
	public static final double K2 = 2.23606797749979D;
	public static final double K3 = 1.6D;
	public static final double F1 = F0 + 0.40D * K1;
	public static final double F2 = F1 + Math.pow(0.20D, 1.5D) * K2;

	public static final double A95 = 0.30D;
	public static final double BETA = 0.30D;
	public static final double K5 = 0.0005D;
	public static final double EXPLOSION_DELAY_SECONDS = 20.0D;

	public static final double LATE_FRACTION = B_START;

	private static final int[] REASIM_VALUES = { 0, REASIM_Z_START, REASIM_A_START, REASIM_B_START, REASIM_C_START, REASIM_CAPACITY };
	private static final double[] REASIM_FRACTIONS = { 0.0D, Z_START, A_START, B_START, C_START, 1.0D };

	public enum Zone {
		NONE, Z, A, B, C, D
	}

	private RbmkSteamCurve() {
	}

	public static int normalThreshold(FluidType type) {
		FluidType dry = type;
		if (NTMITFluids.isMixture(type)) dry = NTMITFluids.dryOf(type);
		if (dry == Fluids.ULTRAHOTSTEAM) return ULTRAHOTSTEAM_THRESHOLD;
		if (dry == Fluids.SUPERHOTSTEAM) return SUPERHOTSTEAM_THRESHOLD;
		if (dry == Fluids.HOTSTEAM) return HOTSTEAM_THRESHOLD;
		return STEAM_THRESHOLD;
	}

	public static double normalFraction(double fill, FluidType type) {
		int threshold = normalThreshold(type);
		return threshold <= 0 ? 0.0D : Math.max(0.0D, fill) / (double) threshold;
	}

	public static double reasimFraction(int steam) {
		if (steam <= 0) return 0.0D;
		for (int i = 0; i < REASIM_VALUES.length - 1; i++) {
			if (steam <= REASIM_VALUES[i + 1]) {
				double span = REASIM_VALUES[i + 1] - REASIM_VALUES[i];
				double progress = span <= 0 ? 0.0D : (steam - REASIM_VALUES[i]) / span;
				return REASIM_FRACTIONS[i] + (REASIM_FRACTIONS[i + 1] - REASIM_FRACTIONS[i]) * progress;
			}
		}
		return 1.0D;
	}

	public static Zone zone(double s) {
		if (s >= 1.0D) return Zone.D;
		if (s >= C_START) return Zone.C;
		if (s >= B_START) return Zone.B;
		if (s >= A_START) return Zone.A;
		if (s >= Z_START) return Zone.Z;
		return Zone.NONE;
	}

	public static double feedback(double s) {
		if (s < Z_START || s >= C_START) return 0.0D;
		if (s < A_START) return F0 + K1 * (s - Z_START);
		if (s < B_START) return F1 + K2 * Math.pow(s - A_START, 1.5D);
		return F2 + K3 * Math.pow(s - B_START, 2.0D);
	}

	public static double acceleration(double s, double t) {
		if (s < C_START) return 0.0D;
		if (s < 1.0D) return A95 + BETA * (s - C_START) / (1.0D - C_START);
		return A95 + BETA + K5 * t * t;
	}

	public static double amplitude(Zone zone, double s, double t) {
		switch (zone) {
			case A: return Math.min(feedback(s), A_AMPLITUDE);
			case B: return Math.min(feedback(s), B_AMPLITUDE);
			case C: return Math.min(acceleration(s, t), C_AMPLITUDE);
			case D: return Math.min(acceleration(1.0D, t), D_AMPLITUDE);
			default: return 0.0D;
		}
	}

	public static int period(Zone zone) {
		switch (zone) {
			case A: return A_PERIOD;
			case B: return B_PERIOD;
			case C: return C_PERIOD;
			case D: return D_PERIOD;
			default: return 0;
		}
	}

	public static int bPeriod(double s) {
		double progress = Math.max(0.0D, Math.min(1.0D, (s - B_START) / (C_START - B_START)));
		return (int) Math.round(B_PERIOD + (8.0D - B_PERIOD) * progress);
	}

	public static int cPeriod(double s) {
		double progress = Math.max(0.0D, Math.min(1.0D, (s - C_START) / (1.0D - C_START)));
		return (int) Math.round(C_PERIOD + (4.0D - C_PERIOD) * progress);
	}

	public static int cBudget(double s, int columns) {
		if (columns <= 1) return 1;
		double progress = Math.max(0.0D, Math.min(1.0D, (s - C_START) / (1.0D - C_START)));
		int max = Math.max(1, Math.min(9, columns));
		return 1 + (int) Math.round(progress * (max - 1));
	}

	public static boolean atBurst(double s) {
		return s >= 1.0D;
	}

	public static boolean late(double s) {
		return s >= LATE_FRACTION;
	}

	public static int amplitudeToDamage(double amplitude) {
		double clamped = Math.max(0.0D, Math.min(D_AMPLITUDE, amplitude));
		return (int) Math.round(clamped * 1000.0D);
	}

	public static double damageToAmplitude(int damage) {
		return Math.max(0, damage) / 1000.0D;
	}
}
