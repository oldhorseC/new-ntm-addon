package com.ntmit.server.data;

import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValueFloat;
import com.hbm.inventory.control_panel.types.DataValueString;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RBMKConsoleSource implements IServerSource {

	public static final String PREFIX = "VAL:";

	public static final String COLUMN_COUNT = PREFIX + "columncount";

	public static final String TOTAL_HEAT = PREFIX + "totalheat";

	private final TileEntityRBMKConsole console;

	public RBMKConsoleSource(TileEntityRBMKConsole console) {
		this.console = console;
	}

	@Override
	public boolean isValid(World world) {
		return world.getTileEntity(this.console.getPos()) == this.console;
	}

	@Override
	public Map<String, DataValue> listFields(World world) {
		if (!isValid(world)) return Collections.emptyMap();

		Map<String, DataValue> fields = new LinkedHashMap<>();
		fields.put(COLUMN_COUNT, new DataValueFloat(this.columnCount()));
		fields.put(TOTAL_HEAT, new DataValueFloat((float) this.totalHeat()));
		return fields;
	}

	@Override
	public DataValue read(World world, String field) {
		if (!isValid(world)) return null;

		if (COLUMN_COUNT.equals(field)) return new DataValueFloat(this.columnCount());
		if (TOTAL_HEAT.equals(field)) return new DataValueFloat((float) this.totalHeat());

		RBMKColumn column = this.findColumn(field);
		if (column == null) return null;

		String value = columnValue(column, nameOf(field));
		return value == null ? null : new DataValueString(value);
	}

	private int columnCount() {
		int count = 0;
		for (RBMKColumn column : this.console.columns) {
			if (column != null) count++;
		}
		return count;
	}

	private double totalHeat() {
		double total = 0.0D;
		for (RBMKColumn column : this.console.columns) {
			if (column != null) total += column.heat;
		}
		return total;
	}

	private RBMKColumn findColumn(String field) {
		String[] parts = field.split("\\.");
		if (parts.length != 3 || !parts[0].startsWith(PREFIX)) return null;

		int x;
		int z;
		try {
			x = Integer.parseInt(parts[1]);
			z = Integer.parseInt(parts[2]);
		} catch (NumberFormatException notCoordinates) {
			return null;
		}

		for (int i = 0; i < this.console.columns.length; i++) {
			if (this.console.columns[i] == null) continue;
			if (this.console.getPos().getX() + this.console.getXFromIndex(i) != x) continue;
			if (this.console.getPos().getZ() + this.console.getZFromIndex(i) != z) continue;
			return this.console.columns[i];
		}
		return null;
	}

	private static String nameOf(String field) {
		int dot = field.indexOf('.');
		String name = dot < 0 ? field : field.substring(0, dot);
		return name.substring(PREFIX.length());
	}

	private static String columnValue(RBMKColumn column, String name) {
		switch (name) {
			case "columnheat": return Integer.toString((int) column.heat);
			case "maxheat": return Integer.toString((int) column.maxHeat);
			case "moderated": return column.moderated ? "1" : "0";
			case "type": return column.type.name();
			default: break;
		}

		if (column instanceof RBMKColumn.FuelColumn fuel) {
			if (name.equals("rodheat")) return Integer.toString((int) fuel.c_heat);
			if (name.equals("depletion")) return Integer.toString((int) (100.0D - fuel.enrichment * 100.0D));
			// xenon 在 HBM 里本就是百分数量级（见 RBMKColumn.FuelColumn 的 tooltip：(int)(xenon * 1000) / 1000 之后直接配 "%"），
			// 所以这里不需要 ×100，保持原样即正确 —— 别再把它当分数"修"一遍。
			if (name.equals("xenon")) return Integer.toString((int) fuel.xenon);
		}
		if (column instanceof RBMKColumn.BoilerColumn boiler) {
			if (name.equals("water")) return Integer.toString(boiler.water);
			if (name.equals("steam")) return Integer.toString(boiler.steam);
		}
		if (column instanceof RBMKColumn.HeaterColumn heater) {
			if (name.equals("water")) return Integer.toString(heater.water);
			if (name.equals("steam")) return Integer.toString(heater.steam);
		}
		if (column instanceof RBMKColumn.CoolerColumn cooler && name.equals("cryo")) {
			return Integer.toString(cooler.cryo);
		}
		if (column instanceof RBMKColumn.ControlColumn control) {
			// 控制棒深度（level）在 HBM 里是 0~1 的分数，对外一律按百分比给出：
			// 与同文件 depletion 的写法、以及 HBM 自己 GUIRBMKConsole / 悬浮提示的 level*100 惯例一致。
			// 旧写法 (int) control.level 会把它压成 0 或 1，屏幕上永远是 0%。
			if (name.equals("level")) return Integer.toString((int) (control.level * 100.0D));
			if (name.equals("color")) return Integer.toString(control.color);
		}
		return null;
	}
}
