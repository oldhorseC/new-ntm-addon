package com.ntmit.fluids;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.render.misc.EnumSymbol;

import java.lang.reflect.Field;
import java.util.List;

public final class NTMITFluids {

	public static final int ID_BASE = 1600;
	public static final float WATER_BONUS = 1.15F;

	public static FluidType WETSTEAM;
	public static FluidType WETSTEAM_HOT;
	public static FluidType WETSTEAM_SUPERHOT;
	public static FluidType WETSTEAM_ULTRAHOT;

	private static boolean registered;

	private NTMITFluids() {
	}

	public static void register() {
		if (registered) return;
		registered = true;
		try {
			WETSTEAM = create("WETSTEAM", 0xA8BCC8, 100, 0, "steam");
			WETSTEAM_HOT = create("WETSTEAM_HOT", 0xB9AEB6, 300, 1, "hotsteam");
			WETSTEAM_SUPERHOT = create("WETSTEAM_SUPERHOT", 0xC49AA2, 450, 2, "superhotsteam");
			WETSTEAM_ULTRAHOT = create("WETSTEAM_ULTRAHOT", 0xCC858C, 600, 3, "ultrahotsteam");
			List<FluidType> order = metaOrder();
			order.add(WETSTEAM);
			order.add(WETSTEAM_HOT);
			order.add(WETSTEAM_SUPERHOT);
			order.add(WETSTEAM_ULTRAHOT);
		} catch (Throwable throwable) {
			WETSTEAM = null;
			WETSTEAM_HOT = null;
			WETSTEAM_SUPERHOT = null;
			WETSTEAM_ULTRAHOT = null;
		}
	}

	private static FluidType create(String name, int color, int temperature, int tier, String texture) {
		FluidType type = new FluidType(name, color, 4, 0, 0, EnumSymbol.NONE, texture, 0xFFFFFF, ID_BASE + tier, null);
		type.setTemp(temperature);
		type.addTraits(Fluids.GASEOUS, Fluids.UNSIPHONABLE);
		type.setFFNameOverride(texture);
		return type;
	}

	@SuppressWarnings("unchecked")
	private static List<FluidType> metaOrder() throws Exception {
		Field field = Fluids.class.getDeclaredField("metaOrder");
		field.setAccessible(true);
		return (List<FluidType>) field.get(null);
	}

	public static boolean isMixture(FluidType type) {
		return type != null && (type == WETSTEAM || type == WETSTEAM_HOT || type == WETSTEAM_SUPERHOT || type == WETSTEAM_ULTRAHOT);
	}

	public static FluidType dryOf(FluidType type) {
		if (type == WETSTEAM) return Fluids.STEAM;
		if (type == WETSTEAM_HOT) return Fluids.HOTSTEAM;
		if (type == WETSTEAM_SUPERHOT) return Fluids.SUPERHOTSTEAM;
		if (type == WETSTEAM_ULTRAHOT) return Fluids.ULTRAHOTSTEAM;
		return type;
	}

	public static FluidType mixtureOf(FluidType dry) {
		if (dry == Fluids.STEAM) return WETSTEAM;
		if (dry == Fluids.HOTSTEAM) return WETSTEAM_HOT;
		if (dry == Fluids.SUPERHOTSTEAM) return WETSTEAM_SUPERHOT;
		if (dry == Fluids.ULTRAHOTSTEAM) return WETSTEAM_ULTRAHOT;
		return dry;
	}

	public static FluidType compressed(FluidType type) {
		if (type == WETSTEAM) return WETSTEAM_HOT;
		if (type == WETSTEAM_HOT) return WETSTEAM_SUPERHOT;
		if (type == WETSTEAM_SUPERHOT) return WETSTEAM_ULTRAHOT;
		if (type == WETSTEAM_ULTRAHOT) return WETSTEAM;
		return null;
	}

	private static FluidType[] connectTypes;

	public static FluidType[] connectTypes() {
		if (connectTypes == null) {
			connectTypes = new FluidType[] {
				Fluids.NONE, Fluids.WATER,
				Fluids.STEAM, Fluids.HOTSTEAM, Fluids.SUPERHOTSTEAM, Fluids.ULTRAHOTSTEAM,
				WETSTEAM, WETSTEAM_HOT, WETSTEAM_SUPERHOT, WETSTEAM_ULTRAHOT
			};
		}
		return connectTypes;
	}
}