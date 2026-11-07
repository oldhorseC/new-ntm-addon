package com.ntmit.lib;

import net.minecraft.tileentity.TileEntity;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public final class TurbinePaintStore {

	private static final Map<TileEntity, Integer> COLORS = new WeakHashMap<TileEntity, Integer>();
	private static final Map<Class<?>, Field> CORE_FIELDS = new HashMap<Class<?>, Field>();

	private TurbinePaintStore() {
	}

	public static int get(TileEntity tile) {

		if (tile == null) return TurbinePaintAccess.NO_PAINT;
		Integer color = COLORS.get(tile);
		if (color != null) return color.intValue();
		if (tile instanceof TurbinePaintAccess) {
			return ((TurbinePaintAccess) tile).ntmit$getPaintColor();
		}
		return TurbinePaintAccess.NO_PAINT;
	}

	public static void set(TileEntity tile, int color) {

		if (tile == null) return;
		int value = color < 0 ? TurbinePaintAccess.NO_PAINT : color & 0xFFFFFF;
		if (value < 0) {
			COLORS.remove(tile);
		} else {
			COLORS.put(tile, Integer.valueOf(value));
		}
		if (tile instanceof TurbinePaintAccess) {
			((TurbinePaintAccess) tile).ntmit$setPaintColor(value);
		}
	}

	public static int getAny(Object tile) {

		if (!(tile instanceof TileEntity)) return TurbinePaintAccess.NO_PAINT;
		TileEntity entity = (TileEntity) tile;
		int color = get(entity);
		if (color >= 0) return color;
		Object core = core(entity);
		if (core instanceof TileEntity && core != entity) return get((TileEntity) core);
		return TurbinePaintAccess.NO_PAINT;
	}

	private static Object core(TileEntity tile) {

		Class<?> type = tile.getClass();
		Field field = CORE_FIELDS.get(type);
		if (field == null) {
			try {
				field = type.getField("core");
			} catch (Throwable throwable) {
				field = null;
			}
			CORE_FIELDS.put(type, field);
		}
		if (field == null) return null;
		try {
			return field.get(tile);
		} catch (Throwable throwable) {
			return null;
		}
	}
}
