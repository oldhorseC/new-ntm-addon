package com.ntmit.lib;

import net.minecraft.util.math.Vec3d;

public final class SkyTint {

	private static final int DURATION = 25;
	private static int ticks;

	private SkyTint() { }

	public static void trigger() {
		ticks = DURATION;
	}

	public static void tick() {
		if (ticks > 0) ticks--;
	}

	public static Vec3d apply(Vec3d original) {
		if (ticks <= 0 || original == null) return original;
		int elapsed = DURATION - ticks;
		Vec3d bright = new Vec3d(0.45D, 0.72D, 1.0D);
		Vec3d deep = new Vec3d(0.015D, 0.06D, 0.25D);
		if (elapsed <= 10) return lerp(bright, deep, (double) elapsed / 10.0D);
		return lerp(deep, original, (double) (elapsed - 10) / 15.0D);
	}

	private static Vec3d lerp(Vec3d from, Vec3d to, double t) {
		return new Vec3d(
				from.x + (to.x - from.x) * t,
				from.y + (to.y - from.y) * t,
				from.z + (to.z - from.z) * t);
	}
}