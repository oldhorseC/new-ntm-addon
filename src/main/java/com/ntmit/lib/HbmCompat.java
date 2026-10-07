package com.ntmit.lib;

import net.minecraftforge.fml.common.Loader;

public class HbmCompat {

	public static final String HBM_MODID = com.hbm.Tags.MODID;

	private HbmCompat() { }

	public static boolean isHbmLoaded() {
		return Loader.isModLoaded(HBM_MODID);
	}
}
