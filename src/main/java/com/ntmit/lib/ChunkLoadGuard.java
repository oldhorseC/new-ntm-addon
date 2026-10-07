package com.ntmit.lib;

public class ChunkLoadGuard {

	private static int depth;

	public static void enter() {
		depth++;
	}

	public static void exit() {
		if (depth > 0) depth--;
	}

	public static boolean active() {
		return depth > 0;
	}
}
