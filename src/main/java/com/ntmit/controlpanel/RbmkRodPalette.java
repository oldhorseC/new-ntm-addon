package com.ntmit.controlpanel;

public final class RbmkRodPalette {

	public static final int[] GROUP_COLORS = {
			0xFF5555,
			0xFFFF55,
			0x55FF55,
			0x5555FF,
			0xAA00AA,
	};

	public static final int UNASSIGNED = 0xFFFFFF;

	private RbmkRodPalette() {
	}

	public static int colorOf(int ordinal) {
		if (ordinal < 0 || ordinal >= GROUP_COLORS.length) return UNASSIGNED;
		return GROUP_COLORS[ordinal];
	}
}

