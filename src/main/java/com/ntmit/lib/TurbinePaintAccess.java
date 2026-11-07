package com.ntmit.lib;

public interface TurbinePaintAccess {

	String NBT_KEY = "ntmitPaintColor";
	int NO_PAINT = -1;

	int ntmit$getPaintColor();

	void ntmit$setPaintColor(int color);
}
