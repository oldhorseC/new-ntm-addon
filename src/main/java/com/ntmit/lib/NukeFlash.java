package com.ntmit.lib;

public final class NukeFlash {

	private static final int DURATION = 10;
	private static int ticks;

	private NukeFlash() { }

	public static void trigger() {
		ticks = DURATION;
	}

	public static void tick() {
		if (ticks > 0) ticks--;
	}

	public static float alpha() {
		return ticks <= 0 ? 0.0F : (float) ticks / (float) DURATION;
	}
}