package com.ntmit.lib;

import com.hbm.blocks.ModBlocks;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNT;
import com.hbm.handler.neutron.NeutronNodeWorld;
import com.hbm.handler.neutron.NeutronStream;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemRBMKRod;
import com.hbm.lib.HBMSoundHandler;
import com.hbm.packet.threading.ThreadedPacket;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControl;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControlManual;
import com.hbm.util.DelayedTick;
import com.hbm.util.ParticleUtil;
import com.ntmit.entity.EntityLidPlate;
import com.ntmit.packet.PacketNTMITSteam;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RbmkJumpHandler {

	public static final String LEAFIA_MODID = "leafia";
	public static final String DAMAGE_FIELD = "leafia$damage";
	public static final int BOILER_STEAM_CAPACITY = 2000000;
	public static final int BOILER_DISPLAY_CAPACITY = 1000000;
	public static final int BOILER_DISPLAY_MAX_VALUE = 1900000;
	public static final int BOILER_WARN_ORANGE = 1200000;
	public static final int BOILER_WARN_RED = 1500000;
	public static final int STEAM_JUMP_THRESHOLD = 1200000;
	public static final int STEAM_VENT_THRESHOLD = 1600000;
	public static final int VENT_INTERVAL = 10;
	public static final int PRESSURE_BURST_THRESHOLD = 2000000;
	public static final int STEAM_JUMP_TARGET_MIN = 160;
	public static final int STEAM_JUMP_TARGET_MAX = 1100;
	public static final float STEAM_JUMP_CURVE = 4.0F;
	public static final int STEAM_JUMP_MIN_DAMAGE = 1200;
	public static final int STEAM_JUMP_FORCED_DAMAGE = 19200;
	public static final int STEAM_JUMP_PERIOD_START = 40;
	public static final int STEAM_JUMP_PERIOD_MIN = 2;
	public static final int STEAM_JUMP_MAX_PER_TICK = 4;
	public static final double JUMP_HEIGHT_LIMIT = 1.0D;

	public static final boolean PLATE_ENABLED = true;
	public static final double PLATE_LAUNCH_VY = 2.0D;
	public static final double PLATE_BLAST_MARGIN = 7.0D;
	public static final double PLATE_LAUNCH_SPREAD = 0.06D;
	public static final double PLATE_AIR_DRAG_XZ = 0.97D;
	public static final double PLATE_DESCEND_DRAG_XZ = 0.88D;
	public static final int PLATE_TOP_SCAN = 16;
	public static final double PLATE_GRAVITY = 0.08D;
	public static final double PLATE_DRAG = 0.995D;
	public static final float PLATE_SPIN = 6.0F;
	public static final float PLATE_SINK = 0.45F;
	public static final double PLATE_FRACTURE_SPEED = 0.55D;
	public static final int PLATE_LIFETIME = 600;
	public static final int PLATE_WRECK_LIFETIME = 24000;
	public static final float PLATE_DARKEN = 0.3F;
	public static final float SPECIAL_PLATE_R = 0.62F;
	public static final float SPECIAL_PLATE_G = 0.09F;
	public static final float SPECIAL_PLATE_B = 0.09F;
	public static final float PLATE_SMOLDER_RATIO = 0.6F;
	public static final int PLATE_SCATTER_COUNT = 12;
	public static final double PLATE_BLAST_VY = 4.25D;
	public static final double PLATE_BLAST_SPREAD = 0.05D;
	public static final double PLATE_MAX_FALL = 2.5D;
	public static final float NEIGHBOUR_RATIO = 0.75F;
	public static final int NEIGHBOUR_RADIUS = 1;
	public static final int STEAM_JUMP_COOLDOWN_START = 20;
	public static final int STEAM_JUMP_COOLDOWN_MIN = 2;

	public static final int REASIM_STEAM_CAPACITY = 80000;
	public static final int REASIM_JUMP_THRESHOLD = 48000;
	public static final int REASIM_VENT_THRESHOLD = 64000;
	public static final int REASIM_BURST_THRESHOLD = 79900;
	public static final int REASIM_JUMP_TARGET_MIN = STEAM_JUMP_TARGET_MIN;
	public static final int REASIM_JUMP_TARGET_MAX = STEAM_JUMP_TARGET_MAX;
	public static final float REASIM_JUMP_CURVE = STEAM_JUMP_CURVE;
	public static final int REASIM_JUMP_COOLDOWN_START = STEAM_JUMP_COOLDOWN_START;
	public static final int REASIM_JUMP_COOLDOWN_MIN = STEAM_JUMP_COOLDOWN_MIN;
	public static final float REASIM_NEIGHBOUR_RATIO = NEIGHBOUR_RATIO;
	public static final String SMOLDER_BLOCK = "leafia:pribris_smoke";
	public static final String BURNING_BLOCK = "hbm:pribris_burning";
	public static final float PLATE_BURNING_CHANCE = 0.0F;
	public static final float PLATE_PATH_MAX_RESISTANCE = 40.0F;
	public static final int PLATE_PUSH_PER_TICK = 320;
	public static final int PLATE_IMPACT_RADIUS = 4;
	public static final int PLATE_IMPACT_MAX_PER_TICK = 2;
	public static final int PLATE_IMPACT_SMALL_RADIUS = 1;
	public static final float PLATE_IMPACT_MAX_HARDNESS = 50.0F;
	public static final float PLATE_IMPACT_EXPLOSION = 3.0F;
	public static final int PLATE_IMPACT_RUBBLE = 12;
	public static final int PLATE_STUCK_TICKS = 10;
	public static final double PLATE_EMBED_DEPTH = 0.25D;
	public static final int PLATE_DISMANTLE_DEBRIS = 48;
	public static final int PLATE_QUENCH_RADIUS = 12;
	public static final float PLATE_HITBOX_WIDTH = 3.0F;
	public static final float PLATE_HITBOX_PADDING = 1.0F;
	public static final float PLATE_HITBOX_HEIGHT = 0.8F;
	public static final float PLATE_DAMAGE = 1000.0F;
	public static final int PLATE_SMOKE_TRAIL_THIN = 1;
	public static final DamageSource LID_ASCEND = new DamageSource("ntmitLidAscend").setDamageBypassesArmor();
	public static final DamageSource LID_DESCEND = new DamageSource("ntmitLidDescend").setDamageBypassesArmor();
	public static final int PLATE_STEAM_TICKS = 10;
	public static final int PLATE_STEAM_LIFE = 20;
	public static final int PLATE_STEAM_PER_TICK = 16;
	public static final int PLATE_STEAM_LAYERS = 6;
	public static final float PLATE_STEAM_EXPAND = 0.45F;
	public static final float PLATE_STEAM_SCALE_MIN = 1.2F;
	public static final float PLATE_STEAM_SCALE_RATIO = 2.5F;
	public static final int PLATE_SMOKE_LAND_TICKS = 0;
	public static final int PLATE_SMOKE_TRAIL_PER_TICK = 3;
	public static final int PLATE_SMOKE_LAND_PER_TICK = 4;
	public static final int PLATE_SMOKE_LIFE = 40;
	public static final float PLATE_SMOKE_SCALE_MIN = 1.0F;
	public static final float PLATE_SMOKE_SCALE_RATIO = 3.0F;
	public static final float PLATE_SMOKE_RISE = 0.22F;
	public static final float SHAKE_RANGE_JUMP = 250.0F;
	public static final float SHAKE_RANGE_BLAST = 400.0F;
	private static final String[] SHAKE_JUMP = new String[] { "type=smooth", "preset=EXPLOSION", "range=250", "bloomDulling*10000", "blurDulling*100" };
	private static final String[] SHAKE_BLAST = new String[] { "type=smooth", "preset=EXPLOSION", "range=400", "intensity*1.75", "speed*1.25", "duration*1.2", "blurDulling*3.0", "bloomDulling*10000" };
	private static final float SHAKE_RANGE_LAND = 250.0F;
	private static final String[] SHAKE_LAND = new String[] { "type=smooth", "preset=EXPLOSION", "range=250", "intensity*0.9", "bloomDulling*10000", "blurDulling*100" };
	public static final String DEBRIS_BLOCK = "leafia:pribris_smoke";
	public static final float BURST_RADIUS = 5.0F;
	public static final float MUSH_SCALE_MIN = 2.0F;
	public static final float MUSH_SCALE_MAX = 6.0F;
	public static final int STEAM_WAVES = 3;
	public static final int STEAM_WAVE_INTERVAL = 8;
	public static final int STEAM_PER_COLUMN = 2;
	public static final int STEAM_RISE = 4;
	public static final int RISE_STEAM_DELAY = 20;
	public static final int RISE_STEAM_TICKS = 40;
	public static final int RISE_STEAM_COLOR = 0xFFFFFF;
	public static final float RISE_STEAM_ALPHA = 0.3F;
	public static final float RISE_STEAM_PARTICLE_LIFT = 0.4F;
	public static final float RISE_STEAM_SCALE_MIN = 2.0F;
	public static final float RISE_STEAM_SCALE_CAP = 16.0F;
	public static final float RISE_STEAM_SCALE_MAX_RATIO = 3.0F;
	public static final float RISE_STEAM_LIFE_BASE = 9.0F;
	public static final int RISE_STEAM_LIFE_CAP = 60;
	public static final int RISE_STEAM_MIN_LIFE = 6;
	public static final float BURST_BOOM_VOLUME = 5.0F;
	public static final float BURST_BOOM_PITCH = 0.55F;
	public static final float BURST_STEAM_VOLUME = 1.8F;
	public static final float BURST_STEAM_PITCH = 1.0F;
	public static final int AFTERMATH_DELAY = 90;
	public static final int MAX_COMPONENTS = 4096;
	public static final float BURST_RATIO = 1.0F;
	public static final int BURST_DEBRIS_BONUS = 4;
	public static final float BURST_DEBRIS_RATIO = 0.18F;
	public static final int MAX_BURST_DEBRIS = 512;
	public static final int MAX_COLUMN_FILL = 8192;
	public static final int CACHE_CLEAN_RADIUS = 24;
	public static final int MAX_DAMAGE = 1100;
	public static boolean allowMeltdown = false;
	public static final int OVERHEAT_MELT_TICKS = 100;
	public static final double OVERHEAT_NEUTRON_MULT = 2.5D;
	public static final int OVERHEAT_FLAME_INTERVAL = 5;
	public static final int OVERHEAT_FLAME_COUNT = 8;
	public static final int OVERHEAT_FLAME_AGE = 40;
	public static final int OVERHEAT_CORIUM_THROW = 10;

	private RbmkJumpHandler() { }

	public static boolean isLeafiaLoaded() {
		return Loader.isModLoaded(LEAFIA_MODID);
	}

	private static Field lookupField(Class<?> type, String name) {
		for (Class<?> current = type; current != null; current = current.getSuperclass()) {
			try {
				Field field = current.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (Throwable throwable) {
			}
		}
		return null;
	}

	private static final java.util.Set<String> FIELD_WARNED = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

	private static Field damageField(TileEntityRBMKBase te) {
		if (te == null) return null;
		Field field = lookupField(te.getClass(), DAMAGE_FIELD);
		if (field == null && FIELD_WARNED.add("damage:" + te.getClass().getName())) {
			diag("WARN leafia damage field \"" + DAMAGE_FIELD + "\" not found on " + te.getClass().getName());
		}
		return field;
	}

	public static boolean pump(TileEntityRBMKBase te, int target) {
		if (te == null || target <= 0 || !isLeafiaLoaded()) return false;
		Field field = damageField(te);
		if (field == null) return false;
		try {
			int clamped = Math.min(target, MAX_DAMAGE);
			if (field.getInt(te) >= clamped) return false;
			field.setInt(te, clamped);
			return true;
		} catch (Throwable throwable) {
			return false;
		}
	}

	public static int readDamage(TileEntityRBMKBase te) {
		if (te == null || !isLeafiaLoaded()) return -1;
		Field field = damageField(te);
		if (field == null) return -1;
		try {
			return field.getInt(te);
		} catch (Throwable throwable) {
			return -1;
		}
	}

	public static boolean pumpForce(TileEntityRBMKBase te, int value) {
		if (te == null || !isLeafiaLoaded()) return false;
		Field field = damageField(te);
		if (field == null) return false;
		try {
			field.setInt(te, Math.max(0, Math.min(value, MAX_DAMAGE)));
			return true;
		} catch (Throwable throwable) {
			return false;
		}
	}

	public static void diag(String message) {
		System.out.println("[ntm-it][diag] " + message);
	}

	public static final int BURST_DELAY_TICKS = 600;
	public static final int BURST_REACTIVITY_DELAY_TICKS = 10;
	public static final int STEAM_CURVE_START = 400000;
	public static final int STEAM_CURVE_LINEAR_END = 800000;
	public static final int STEAM_CURVE_KNEE = 1300000;
	public static final int STEAM_CURVE_STEEP = 1600000;
	public static final int BURST_AZ5_DELAY_TICKS = 40;
	public static final double AZ5_RADIUS = 32.0D;

	private static World az5World;
	private static BlockPos az5Pos;
	private static long az5Tick = Long.MIN_VALUE;
	private static int az5Dim = Integer.MIN_VALUE;

	public static void az5Press(World world, BlockPos pos) {
		if (world == null || world.isRemote || pos == null) return;
		az5World = world;
		az5Pos = pos;
		az5Dim = world.provider.getDimension();
		az5Tick = world.getTotalWorldTime();
		diag("AZ-5 pressed at " + pos);
	}

	public static boolean az5ForcesBurst(World world, BlockPos pos) {
		if (world == null || pos == null) return false;
		if (az5World != world || az5Pos == null) return false;
		if (az5Dim != world.provider.getDimension()) return false;
		long elapsed = world.getTotalWorldTime() - az5Tick;
		if (elapsed < BURST_AZ5_DELAY_TICKS) return false;
		if (elapsed > BURST_DELAY_TICKS) {
			az5Consume();
			return false;
		}
		return pos.distanceSq(az5Pos) <= AZ5_RADIUS * AZ5_RADIUS;
	}

	public static void az5Consume() {
		az5World = null;
		az5Pos = null;
		az5Tick = Long.MIN_VALUE;
	}

	public static void steamBurst(TileEntityRBMKBoiler boiler) {
		if (boiler == null) return;
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return;
		if (NTMITWorldRules.steamExplosionDisabled(world)) return;
		try {
			if (reactorHasDigammaRod(world, boiler.getPos())) {
				digammaReactorBurst(world, boiler.getPos());
				return;
			}
			pressureBurst(boiler);
		} catch (Throwable throwable) {
			diag("pressure burst error: " + throwable);
			throwable.printStackTrace();
		}
	}

	public static final String CURSED_TOREX_CLASS = "com.custom_hbm.contents.torex.LCETorex";
	public static final String CURSED_FALLOUT_INTERFACE = "com.leafia.overwrite_contents.interfaces.IMixinEntityNukeExploisonMK5";
	public static final String CURSED_ADVANCEMENTS_CLASS = "com.leafia.init.AddonAdvancements";
	public static final int DIGAMMA_NUKE_RADIUS = 50;
	public static final float DIGAMMA_TOREX_SCALE = 50.0F;
	public static final double DIGAMMA_RADIUS_FALLOFF = 20.0D;
	public static final double DIGAMMA_LEVEL_MAX = 9.99999999D;
	public static final int RED_STEAM_COLOR = 0xFFFF2020;
	public static final double PLATE_SIDE_SPEED = 1.6D;
	public static final double PLATE_SIDE_VY_RATIO = 0.2D;
	public static final double PLATE_SIDE_SPREAD = 0.35D;
	public static void clearCaches() {
		ZEROED_REACTORS.clear();
		DIGAMMA_BURST_TICKS.clear();
		DIGAMMA_SCAN_CACHE.clear();
		DIGAMMA_MARKS.clear();
		JUMP_COLUMNS.clear();
		JUMP_NEXT.clear();
		lastBurstWorld = null;
		lastBurstTick = Long.MIN_VALUE;
		reasimBurstWorld = null;
		reasimBurstTick = Long.MIN_VALUE;
		scramWorld = null;
		scramTick = Long.MIN_VALUE;
		scramPressTick = Long.MIN_VALUE;
		scramLevels = 0.0D;
		impactTick = Long.MIN_VALUE;
		impactDim = Integer.MIN_VALUE;
		impactUsed = 0;
		az5World = null;
		az5Pos = null;
		az5Tick = Long.MIN_VALUE;
		az5Dim = Integer.MIN_VALUE;
		redBurstWorld = null;
		redBurstPos = null;
		redBurstTick = Long.MIN_VALUE;
		diag("caches cleared (world unload)");
	}

	public static final int DIGAMMA_ZEROING_COOLDOWN_TICKS = 1200;
	private static final java.util.Map<String, Long> ZEROED_REACTORS = new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static final java.util.Map<String, Long> DIGAMMA_BURST_TICKS = new java.util.concurrent.ConcurrentHashMap<String, Long>();
	private static World redBurstWorld;
	private static BlockPos redBurstPos;
	private static long redBurstTick = Long.MIN_VALUE;

	private static String reactorKey(World world, int[] bounds) {
		return worldIdentity(world) + ":" + ((bounds[0] + bounds[1]) / 2) + ":" + ((bounds[2] + bounds[3]) / 2);
	}

	public static String worldIdentity(World world) {
		if (world == null) return "null";
		String name = "?";
		try {
			name = world.getWorldInfo() == null ? "?" : world.getWorldInfo().getWorldName();
		} catch (Throwable throwable) {
		}
		return world.provider.getDimension() + "|" + name;
	}

	private static void forceRedBurst(World world, BlockPos pos) {
		if (world == null || pos == null) return;
		redBurstWorld = world;
		redBurstPos = pos;
		redBurstTick = world.getTotalWorldTime();
	}

	private static boolean consumeRedBurst(World world, BlockPos origin) {
		if (world == null || origin == null) return false;
		if (redBurstWorld != world || redBurstPos == null) return false;
		if (!redBurstPos.equals(origin) || redBurstTick != world.getTotalWorldTime()) return false;
		redBurstWorld = null;
		redBurstPos = null;
		redBurstTick = Long.MIN_VALUE;
		return true;
	}

	public static void digammaBurst(World world, BlockPos rodPos) {
		digammaReactorBurst(world, rodPos);
	}

	public static void digammaReactorBurst(World world, BlockPos anyPos) {
		if (world == null || world.isRemote || anyPos == null) return;
		List<Component> components = gatherComponents(world, anyPos);
		if (components.isEmpty()) return;
		int[] bounds = bounds(components, anyPos);
		String key = reactorKey(world, bounds);
		BlockPos center = new BlockPos((bounds[0] + bounds[1]) / 2, anyPos.getY(), (bounds[2] + bounds[3]) / 2);
		TileEntityRBMKBase origin = null;
		for (Component component : components) {
			if (!component.pos.equals(center)) continue;
			if (!world.isBlockLoaded(component.pos)) continue;
			TileEntity te = world.getTileEntity(component.pos);
			if (te instanceof TileEntityRBMKBase base) origin = base;
		}
		if (origin == null) {
			for (Component component : components) {
				if (!world.isBlockLoaded(component.pos)) continue;
				TileEntity te = world.getTileEntity(component.pos);
				if (te instanceof TileEntityRBMKBase base) {
					origin = base;
					break;
				}
			}
		}
		boolean recent = false;
		Long lastBurst = DIGAMMA_BURST_TICKS.get(key);
		if (lastBurst != null) recent = world.getTotalWorldTime() - lastBurst.longValue() < (long) BURST_COOLDOWN;
		diag("digamma reactor burst center=" + center + " key=" + key + " origin=" + (origin == null ? "none" : origin.getPos()) + " zeroing deferred to second explosion");
		if (origin != null && !recent) {
			DIGAMMA_BURST_TICKS.put(key, world.getTotalWorldTime());
			try {
				forceRedBurst(world, origin.getPos());
				pressureBurst(origin, true);
			} catch (Throwable throwable) {
				diag("digamma burst error: " + throwable);
				throwable.printStackTrace();
			}
		}
	}

	public static void digammaZeroingPayload(World world, BlockPos center, int[] bounds) {
		if (world == null || world.isRemote || bounds == null || center == null) return;
		String key = reactorKey(world, bounds);
		long now = world.getTotalWorldTime();
		Long next = ZEROED_REACTORS.get(key);
		if (next != null && next.longValue() > now) {
			diag("digamma zeroing on cooldown for reactor " + key + " left=" + (next.longValue() - now));
			return;
		}
		ZEROED_REACTORS.put(key, Long.valueOf(now + (long) DIGAMMA_ZEROING_COOLDOWN_TICKS));
		diag("digamma zeroing payload fires at " + center);
		digammaZeroing(world, center);
	}

	private static final Set<String> ROD_NAMES_SEEN = Collections.synchronizedSet(new HashSet<String>());

	public static boolean isDigammaRod(TileEntityRBMKRod rod) {
		if (rod == null || rod.inventory == null) return false;
		net.minecraft.item.ItemStack stack = rod.inventory.getStackInSlot(0);
		if (stack.isEmpty()) return false;
		net.minecraft.item.Item item = stack.getItem();
		if (item == com.hbm.items.ModItems.rbmk_fuel_drx) return true;
		net.minecraft.util.ResourceLocation name = item.getRegistryName();
		if (name == null) return false;
		if (ROD_NAMES_SEEN.add(name.toString())) diag("rod fuel item seen: " + name);
		String path = name.getPath();
		return path.contains("digamma") || path.contains("dgomega") || path.contains("drx");
	}

	public static final int DIGAMMA_SCAN_INTERVAL = 20;
	private static final java.util.Map<String, long[]> DIGAMMA_SCAN_CACHE = new java.util.concurrent.ConcurrentHashMap<String, long[]>();

	public static boolean reactorHasDigammaRod(World world, BlockPos origin) {
		return reactorHasDigammaRod(world, origin, false);
	}

	public static boolean reactorHasDigammaRod(World world, BlockPos origin, boolean force) {
		if (world == null || origin == null || world.isRemote) return false;
		if (digammaMarkNear(world, origin)) return true;
		String key = world.provider.getDimension() + ":" + origin.getX() + ":" + origin.getY() + ":" + origin.getZ();
		long now = world.getTotalWorldTime();
		if (!force) {
			long[] cached = DIGAMMA_SCAN_CACHE.get(key);
			if (cached != null && now - cached[0] < (long) DIGAMMA_SCAN_INTERVAL) return cached[1] != 0L;
		}
		boolean found = scanDigammaRod(world, origin);
		DIGAMMA_SCAN_CACHE.put(key, new long[] { now, found ? 1L : 0L });
		return found;
	}

	public static final int DIGAMMA_MARK_TICKS = 400;
	public static final int DIGAMMA_MARK_RANGE = 96;
	private static final java.util.Map<Integer, java.util.Map<Long, Long>> DIGAMMA_MARKS = new java.util.concurrent.ConcurrentHashMap<Integer, java.util.Map<Long, Long>>();

	public static void markDigammaRod(World world, BlockPos pos) {
		if (world == null || pos == null || world.isRemote) return;
		int dimension = world.provider.getDimension();
		java.util.Map<Long, Long> marks = DIGAMMA_MARKS.get(Integer.valueOf(dimension));
		if (marks == null) {
			marks = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
			DIGAMMA_MARKS.put(Integer.valueOf(dimension), marks);
		}
		marks.put(Long.valueOf(pos.toLong()), Long.valueOf(world.getTotalWorldTime() + (long) DIGAMMA_MARK_TICKS));
	}

	private static boolean digammaMarkNear(World world, BlockPos origin) {
		java.util.Map<Long, Long> marks = DIGAMMA_MARKS.get(Integer.valueOf(world.provider.getDimension()));
		if (marks == null || marks.isEmpty()) return false;
		long now = world.getTotalWorldTime();
		for (java.util.Map.Entry<Long, Long> entry : marks.entrySet()) {
			if (entry.getValue().longValue() < now) {
				marks.remove(entry.getKey());
				continue;
			}
			BlockPos pos = BlockPos.fromLong(entry.getKey().longValue());
			if (Math.abs(pos.getX() - origin.getX()) <= DIGAMMA_MARK_RANGE && Math.abs(pos.getZ() - origin.getZ()) <= DIGAMMA_MARK_RANGE) return true;
		}
		return false;
	}

	private static boolean scanDigammaRod(World world, BlockPos origin) {
		Set<BlockPos> seen = new HashSet<BlockPos>();
		Deque<BlockPos> queue = new ArrayDeque<BlockPos>();
		queue.add(origin);
		seen.add(origin);
		int visited = 0;
		while (!queue.isEmpty() && visited < MAX_COMPONENTS) {
			BlockPos pos = queue.poll();
			if (!world.isBlockLoaded(pos)) continue;
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityRBMKBase)) continue;
			visited++;
			if (te instanceof TileEntityRBMKRod rod && isDigammaRod(rod)) return true;
			for (EnumFacing facing : EnumFacing.values()) {
				BlockPos next = pos.offset(facing);
				if (!seen.add(next)) continue;
				if (!world.isBlockLoaded(next)) continue;
				if (world.getTileEntity(next) instanceof TileEntityRBMKBase) queue.add(next);
			}
		}
		return false;
	}

	public static void digammaZeroing(World world, BlockPos pos) {
		if (world == null || world.isRemote || pos == null) return;
		double x = (double) pos.getX() + 0.5D;
		double y = (double) pos.getY() + 0.5D;
		double z = (double) pos.getZ() + 0.5D;
		digammaDose(world, x, y, z);
		try {
			Class<?> torex = Class.forName(CURSED_TOREX_CLASS);
			Method method = torex.getMethod("statFacDigamma", World.class, double.class, double.class, double.class, float.class);
			method.invoke(null, world, x, y, z, DIGAMMA_TOREX_SCALE);
		} catch (Throwable throwable) {
			diag("digamma torex unavailable: " + throwable);
		}
		try {
			com.hbm.entity.logic.EntityNukeExplosionMK5 blast = com.hbm.entity.logic.EntityNukeExplosionMK5.statFac(world, DIGAMMA_NUKE_RADIUS, x, y, z);
			digammaFallout(blast);
			world.spawnEntity(blast);
		} catch (Throwable throwable) {
			diag("digamma nuke error: " + throwable);
		}
		diag("digamma zeroing at " + pos);
	}

	public static void digammaDose(World world, double x, double y, double z) {
		try {
			net.minecraft.util.math.Vec3d center = new net.minecraft.util.math.Vec3d(x, y, z);
			for (Object object : new java.util.ArrayList<Object>(world.loadedEntityList)) {
				if (!(object instanceof net.minecraft.entity.EntityLivingBase)) continue;
				net.minecraft.entity.EntityLivingBase living = (net.minecraft.entity.EntityLivingBase) object;
				double distance = new net.minecraft.util.math.Vec3d(living.posX, living.posY, living.posZ).distanceTo(center);
				double dg = com.hbm.capability.HbmLivingProps.getDigamma(living);
				double level = Math.max(dg, Math.min((double) DIGAMMA_NUKE_RADIUS - distance / DIGAMMA_RADIUS_FALLOFF, DIGAMMA_LEVEL_MAX));
				com.hbm.capability.HbmLivingProps.setDigamma(living, level);
				if (living instanceof net.minecraft.entity.player.EntityPlayer) {
					net.minecraft.entity.player.EntityPlayer player = (net.minecraft.entity.player.EntityPlayer) living;
					if (level > dg) digammaAchievement(player);
					if (distance <= (double) DIGAMMA_NUKE_RADIUS && player instanceof net.minecraft.entity.player.EntityPlayerMP serverPlayer) {
						LidAchievements.grant(serverPlayer, LidAchievements.DIGAMMA_ZEROING);
					}
				}
			}
		} catch (Throwable throwable) {
			diag("digamma dose error: " + throwable);
		}
	}

	private static void digammaFallout(com.hbm.entity.logic.EntityNukeExplosionMK5 blast) {
		if (blast == null) return;
		try {
			Class<?> fallout = Class.forName(CURSED_FALLOUT_INTERFACE);
			if (fallout.isInstance(blast)) fallout.getMethod("setDigammaFallout").invoke(blast);
		} catch (Throwable throwable) {
			diag("digamma fallout unavailable: " + throwable);
		}
	}

	private static void digammaAchievement(net.minecraft.entity.player.EntityPlayer player) {
		try {
			Class<?> advancements = Class.forName(CURSED_ADVANCEMENTS_CLASS);
			Field field = lookupField(advancements, "dgomega");
			Object advancement = field == null ? null : field.get(null);
			if (advancement != null) com.hbm.main.AdvancementManager.grantAchievement(player, (net.minecraft.advancements.Advancement) advancement);
		} catch (Throwable throwable) {
		}
	}

	public static final int JUMP_FAST_TICKS = 200;
	public static final int JUMP_RAMP_TICKS = BURST_DELAY_TICKS - JUMP_FAST_TICKS;
	public static final int JUMP_PERIOD_SLOW = 60;
	public static final int JUMP_PERIOD_RAMP_END = 8;
	public static final int JUMP_PERIOD_FAST = 3;
	public static final int JUMP_DAMAGE_MIN = 600;
	public static final int JUMP_DAMAGE_MAX = 1000;
	public static final int JUMP_ORDER_REFRESH = 40;
	public static final float JUMP_THRESHOLD_RATIO = 1.0F;
	private static final java.util.Map<Long, Long> JUMP_NEXT = new java.util.concurrent.ConcurrentHashMap<Long, Long>();
	private static final java.util.Map<String, Object[]> JUMP_COLUMNS = new java.util.concurrent.ConcurrentHashMap<String, Object[]>();

	private static int jumpPeriod(int elapsed) {
		if (elapsed >= JUMP_RAMP_TICKS) return JUMP_PERIOD_FAST;
		float progress = (float) elapsed / (float) JUMP_RAMP_TICKS;
		return Math.round(lerp((float) JUMP_PERIOD_SLOW, (float) JUMP_PERIOD_RAMP_END, progress * progress));
	}

	private static int jumpBatch(int total, int elapsed) {
		if (total <= 1) return 1;
		float progress = Math.min(1.0F, (float) elapsed / (float) JUMP_RAMP_TICKS);
		int batch = (int) Math.ceil((double) total * (0.35D + 0.65D * (double) progress * (double) progress));
		return MathHelper.clamp(batch, 1, total);
	}

	@SuppressWarnings("unchecked")
	private static List<BlockPos> collectReactorColumns(World world, BlockPos origin) {
		String key = world.provider.getDimension() + ":" + origin.getX() + ":" + origin.getY() + ":" + origin.getZ();
		long now = world.getTotalWorldTime();
		Object[] cached = JUMP_COLUMNS.get(key);
		if (cached != null && now - ((Long) cached[0]).longValue() < (long) JUMP_ORDER_REFRESH) return (List<BlockPos>) cached[1];
		List<BlockPos> columns = new ArrayList<BlockPos>();
		Set<BlockPos> seen = new HashSet<BlockPos>();
		Deque<BlockPos> queue = new ArrayDeque<BlockPos>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && columns.size() < MAX_COMPONENTS) {
			BlockPos pos = queue.poll();
			if (!world.isBlockLoaded(pos)) continue;
			if (!(world.getTileEntity(pos) instanceof TileEntityRBMKBase)) continue;
			columns.add(pos);
			for (EnumFacing facing : EnumFacing.values()) {
				BlockPos next = pos.offset(facing);
				if (!seen.add(next)) continue;
				if (!world.isBlockLoaded(next)) continue;
				if (world.getTileEntity(next) instanceof TileEntityRBMKBase) queue.add(next);
			}
		}
		JUMP_COLUMNS.put(key, new Object[] { Long.valueOf(now), columns });
		return columns;
	}

	public static int jumpTargetDamage(World world) {
		int span = Math.max(1, JUMP_DAMAGE_MAX - JUMP_DAMAGE_MIN + 1);
		return JUMP_DAMAGE_MIN + world.rand.nextInt(span);
	}

	public static final int DARK_RED_SMOKE_COLOR = 0x8B0000;

	public static void jumpWholeReactor(World world, BlockPos origin) {
		if (world == null || world.isRemote || origin == null) return;
		List<BlockPos> columns = collectReactorColumns(world, origin);
		if (columns.isEmpty()) return;
		long now = world.getTotalWorldTime();
		List<BlockPos> order = new ArrayList<BlockPos>(columns);
		Collections.shuffle(order, world.rand);
		int limit = Math.max(1, Math.round((float) order.size() * JUMP_THRESHOLD_RATIO));
		int pumped = 0;
		for (BlockPos pos : order) {
			if (pumped >= limit) break;
			long key = pos.toLong();
			Long next = JUMP_NEXT.get(key);
			if (next != null && next.longValue() > now) continue;
			JUMP_NEXT.put(Long.valueOf(key), Long.valueOf(now + (long) JUMP_PERIOD_RAMP_END));
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityRBMKBase base)) continue;
			if (pumpForce(base, jumpTargetDamage(world))) pumped++;
		}
		if (pumped > 0) diag("reactor random jump pumped=" + pumped + "/" + order.size());
	}

	public static void pumpReactor(World world, BlockPos origin, int elapsed) {
		if (world == null || world.isRemote || origin == null || elapsed < 0) return;
		List<BlockPos> columns = collectReactorColumns(world, origin);
		if (columns.isEmpty()) return;
		long now = world.getTotalWorldTime();
		boolean special = reactorHasDigammaRod(world, origin);
		int period = jumpPeriod(elapsed);
		int batch = jumpBatch(columns.size(), elapsed);
		if (special) {
			period = Math.max(2, period * 3 / 4);
			batch = MathHelper.clamp(batch * 3 / 2, 1, columns.size());
		}
		List<BlockPos> order = new ArrayList<BlockPos>(columns);
		Collections.shuffle(order, world.rand);
		int pumped = 0;
		for (BlockPos pos : order) {
			if (pumped >= batch) break;
			long key = pos.toLong();
			Long next = JUMP_NEXT.get(key);
			if (next != null && next.longValue() > now) continue;
			JUMP_NEXT.put(Long.valueOf(key), Long.valueOf(now + (long) period));
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityRBMKBase base)) continue;
			int span = Math.max(1, JUMP_DAMAGE_MAX - JUMP_DAMAGE_MIN + 1);
			if (pumpForce(base, JUMP_DAMAGE_MIN + world.rand.nextInt(span))) pumped++;
		}
		if (pumped > 0) diag("reactor jump elapsed=" + elapsed + " period=" + period + " batch=" + batch + " pumped=" + pumped + " total=" + columns.size());
	}

	public static float steamCurve(double value, double capacity) {
		if (capacity <= 0.0D) return 0.0F;
		double u = value / capacity;
		double s = (double) STEAM_CURVE_START / (double) BOILER_STEAM_CAPACITY;
		double l = (double) STEAM_CURVE_LINEAR_END / (double) BOILER_STEAM_CAPACITY;
		double k = (double) STEAM_CURVE_KNEE / (double) BOILER_STEAM_CAPACITY;
		double t = (double) STEAM_CURVE_STEEP / (double) BOILER_STEAM_CAPACITY;
		if (u <= s) return 0.0F;
		if (u <= l) return (float) (0.2D * (u - s) / (l - s));
		if (u <= k) return (float) (0.2D + 0.3D * Math.pow((u - l) / (k - l), 1.5D));
		if (u <= t) return (float) (0.5D + 0.25D * Math.pow((u - k) / (t - k), 2.5D));
		double tail = Math.min((u - t) / (1.0D - t), 1.0D);
		return (float) Math.min(1.0D, 0.75D + 0.25D * Math.pow(tail, 4.0D));
	}

	private static World scramWorld;
	private static long scramTick = Long.MIN_VALUE;
	private static double scramLevels;
	private static long scramPressTick = Long.MIN_VALUE;

	public static void pollControlRods(World world, BlockPos origin) {
		if (world == null || world.isRemote || origin == null) return;
		long now = world.getTotalWorldTime();
		if (scramWorld != world) {
			scramWorld = world;
			scramTick = now;
			scramLevels = 0.0D;
		}
		if (scramTick == now) return;
		scramTick = now;
		double sum = 0.0D;
		int found = 0;
		for (int dx = -NEIGHBOUR_RADIUS; dx <= NEIGHBOUR_RADIUS; dx++) {
			for (int dz = -NEIGHBOUR_RADIUS; dz <= NEIGHBOUR_RADIUS; dz++) {
				BlockPos scan = new BlockPos(origin.getX() + dx, origin.getY(), origin.getZ() + dz);
				if (!world.isBlockLoaded(scan)) continue;
				TileEntity te = world.getTileEntity(scan);
				if (!(te instanceof TileEntityRBMKControl control)) continue;
				sum += (double) control.level;
				found++;
			}
		}
		if (found > 0 && sum < scramLevels - 0.0005D) scramPressTick = now;
		scramLevels = sum;
	}

	public static boolean scramForcesBurst(World world, BlockPos pos) {
		if (world == null || scramWorld != world) return false;
		long elapsed = world.getTotalWorldTime() - scramPressTick;
		if (elapsed < (long) BURST_REACTIVITY_DELAY_TICKS) return false;
		if (elapsed > (long) BURST_DELAY_TICKS) {
			scramPressTick = Long.MIN_VALUE;
			return false;
		}
		return true;
	}

	public static void scramConsume() {
		scramPressTick = Long.MIN_VALUE;
	}

	public static int steamFull(TileEntityRBMKBoiler boiler, FluidTankNTM steam) {
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return 0;
		if (steam == null || steam.getMaxFill() <= 0) return 0;
		if (steam.getFill() < STEAM_JUMP_THRESHOLD) return 0;
		float ramp = steamCurve(steam.getFill(), BOILER_STEAM_CAPACITY);
		int target = Math.round(MathHelper.clamp(lerp(STEAM_JUMP_TARGET_MIN, STEAM_JUMP_TARGET_MAX, ramp), 0.0F, MAX_DAMAGE));
		if (steam.getFill() >= STEAM_VENT_THRESHOLD && world.getTotalWorldTime() % VENT_INTERVAL == 0L) emitJet(boiler);
		pump(boiler, target);
		jumpWholeReactor(world, boiler.getPos());
		return Math.round(lerp(STEAM_JUMP_COOLDOWN_START, STEAM_JUMP_COOLDOWN_MIN, ramp));
	}

	private static World reasimBurstWorld;
	private static long reasimBurstTick = Long.MIN_VALUE;
	private static long impactTick = Long.MIN_VALUE;
	private static int impactDim = Integer.MIN_VALUE;
	private static int impactUsed;
	private static World lastBurstWorld;
	private static long lastBurstTick = Long.MIN_VALUE;
	public static final int BURST_COOLDOWN = 20;

	public static int reasimSteamJump(TileEntityRBMKBase te) {
		if (te == null || !isLeafiaLoaded()) return 0;
		World world = te.getWorld();
		if (world == null || world.isRemote) return 0;
		int steam = te.reasimSteam;
		if (steam < REASIM_JUMP_THRESHOLD) return 0;
		float ramp = steamCurve(steam, REASIM_STEAM_CAPACITY);
		int target = Math.round(MathHelper.clamp(lerp(REASIM_JUMP_TARGET_MIN, REASIM_JUMP_TARGET_MAX, ramp), 0.0F, MAX_DAMAGE));
		if (steam >= REASIM_VENT_THRESHOLD && world.getTotalWorldTime() % VENT_INTERVAL == 0L) emitJet(te);
		pump(te, target);
		jumpWholeReactor(world, te.getPos());
		return Math.round(lerp(REASIM_JUMP_COOLDOWN_START, REASIM_JUMP_COOLDOWN_MIN, ramp));
	}

	public static void reasimBurst(TileEntityRBMKBase te, World world) {
		if (NTMITWorldRules.steamExplosionDisabled(world)) return;
		long time = world.getTotalWorldTime();
		if (reasimBurstWorld == world && reasimBurstTick == time) return;
		reasimBurstWorld = world;
		reasimBurstTick = time;
		te.reasimSteam = 0;
		try {
			if (reactorHasDigammaRod(world, te.getPos())) {
				digammaReactorBurst(world, te.getPos());
				return;
			}
			pressureBurst(te);
		} catch (Throwable throwable) {
			diag("reasim burst error: " + throwable);
			throwable.printStackTrace();
		}
	}

	public static float lerp(float from, float to, float progress) {
		return from + (to - from) * progress;
	}

	public static int levelTint(float level) {
		int r = Math.round(255.0F * (1.0F - level));
		int g = Math.round(255.0F * level);
		return (r & 0xFF) | ((g & 0xFF) << 8) | (0x50 << 16);
	}

	public static float randomRodLevel(World world, float original) {
		float ceiling = MathHelper.clamp(original, 0.0F, 1.0F);
		if (ceiling <= 0.0F) return 0.0F;
		float first = world.rand.nextFloat();
		float second = world.rand.nextFloat();
		return ceiling * Math.max(first, second);
	}

	public static byte readRodColor(TileEntity controlTe) {
		Field field = lookupField(controlTe.getClass(), "color");
		if (field == null) return -1;
		try {
			Object value = field.get(controlTe);
			if (value instanceof Enum) return (byte) ((Enum<?>) value).ordinal();
		} catch (Throwable throwable) {
		}
		return -1;
	}

	public static void emitJet(TileEntityRBMKBase te) {
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		BlockPos pos = te.getPos();
		double top = (double) (pos.getY() + RBMKDials.getColumnHeight(world));
		double x = (double) pos.getX() + 0.25D + (double) world.rand.nextInt(2) * 0.5D;
		double z = (double) pos.getZ() + 0.25D + (double) world.rand.nextInt(2) * 0.5D;
		boolean digamma = reactorHasDigammaRod(world, pos);
		if (!digamma) spawnSteam(world, x, top, z);
		if (jetDiag++ % 20 == 0) diag("vent jet particle=" + (digamma ? "SUPPRESSED(digamma)" : "WHITE") + " at " + pos);
		world.playSound(null, x, top, z, HBMSoundHandler.steamEngineOperate, SoundCategory.BLOCKS, 2.0F, 1.0F + world.rand.nextFloat() * 0.25F);
	}

	private static int jetDiag;

	public static final int LID_RED_STEAM_PER_TICK = 8;
	public static final int VENT_RED_STEAM_COUNT = 4;

	public static final float DIGAMMA_CLOUD_SCALE = 4.0F;
	public static final float DIGAMMA_CLOUD_SCALE_RATIO = 3.0F;
	public static final int DIGAMMA_CLOUD_LIFE = 60;
	public static final float DIGAMMA_CLOUD_RISE = 0.7F;
	public static final int DIGAMMA_CLOUD_COUNT = 96;

	public static void sendDigammaSteamBurst(World world, int[] bounds, List<Component> components, int height) {
		if (world == null || world.isRemote || bounds == null) return;
		double cx = ((double) bounds[0] + (double) bounds[1] + 1.0D) / 2.0D;
		double cz = ((double) bounds[2] + (double) bounds[3] + 1.0D) / 2.0D;
		float spread = Math.max(2.0F, (float) Math.max(bounds[1] - bounds[0] + 1, bounds[3] - bounds[2] + 1) / 2.0F);
		int top = 0;
		if (components != null) {
			for (Component component : components) top = Math.max(top, component.pos.getY());
		}
		double y = (double) (top + height + 1);
		sendRedSteam(world, cx, y, cz, spread, DIGAMMA_CLOUD_COUNT);
		for (int wave = 1; wave < STEAM_WAVES; wave++) {
			final double wy = y + (double) wave * 1.5D;
			final double fx = cx;
			final double fz = cz;
			final float fs = spread;
			DelayedTick.scheduleWorldStart(world, wave * STEAM_WAVE_INTERVAL, delayed -> sendRedSteam(delayed, fx, wy, fz, fs, DIGAMMA_CLOUD_COUNT));
		}
		diag("digamma steam burst at " + cx + "," + y + "," + cz + " spread=" + spread);
	}

	public static void sendSteam(World world, double x, double y, double z, float spread, int count, float base, float max, int life, int color, float rise) {
		PacketNTMITSteam.send(world, x, y, z, spread, count, base, max, life, color, rise);
	}

	private static void explodeFootprint(World world, int[] bounds, int baseY, int height, boolean red) {
		if (world == null || bounds == null) return;
		float radius = red ? BURST_RADIUS * 1.5F : BURST_RADIUS * 0.75F;
		int y = Math.min(baseY + Math.max(height, 1), baseY + 6);
		int count = 0;
		for (int x = bounds[0]; x <= bounds[1]; x += 3) {
			for (int z = bounds[2]; z <= bounds[3]; z += 3) {
				if (count++ >= 96) {
					diag("footprint explosion points=" + count + " radius=" + radius + " (capped)");
					return;
				}
				new ExplosionNT(world, null, (double) x + 0.5D, (double) y + 0.5D, (double) z + 0.5D, radius).addAttrib(ExplosionNT.ExAttrib.ALLDROP).addAttrib(ExplosionNT.ExAttrib.NOSOUND).addAttrib(ExplosionNT.ExAttrib.NOPARTICLE).explode();
			}
		}
		diag("footprint explosion points=" + count + " radius=" + radius);
	}

	public static void sendRedSteam(World world, double x, double y, double z, float spread, int count) {
		sendSteam(world, x, y, z, spread, count, DIGAMMA_CLOUD_SCALE, DIGAMMA_CLOUD_SCALE * DIGAMMA_CLOUD_SCALE_RATIO, DIGAMMA_CLOUD_LIFE, RED_STEAM_COLOR, DIGAMMA_CLOUD_RISE);
	}

	public static void lidSteam(World world, EntityLidPlate plate, int[] bounds, int color) {
		if (world == null || world.isRemote || plate == null || bounds == null) return;
		if (!plate.isEntityAlive()) return;
		double width = (double) (bounds[1] - bounds[0] + 1);
		double depth = (double) (bounds[3] - bounds[2] + 1);
		float scale = Math.max(PLATE_STEAM_SCALE_MIN, (float) (width + depth - 2.0D) / 4.0F);
		sendSteam(world, plate.posX, plate.posY + 1.0D, plate.posZ, (float) Math.max(width, depth) / 2.0F, PLATE_STEAM_PER_TICK, scale, scale * PLATE_STEAM_SCALE_RATIO, PLATE_STEAM_LIFE, color, 0.75F);
	}

	public static void spawnPlate(World world, List<BlockPos> positions, List<IBlockState> states, int[] bounds, List<Integer> tints, List<Float> lifts, List<Byte> rodColors, boolean sideways) {
		BlockPos center = new BlockPos((bounds[0] + bounds[1]) / 2, positions.get(0).getY(), (bounds[2] + bounds[3]) / 2);
		Block smolder = Block.getBlockFromName(SMOLDER_BLOCK);
		Block burning = Block.getBlockFromName(BURNING_BLOCK);
		IBlockState smolderState = smolder == null ? null : smolder.getDefaultState();
		IBlockState burningState = burning == null ? null : burning.getDefaultState();
		List<IBlockState> render = new ArrayList<>(states);
		for (int i = 0; i < render.size(); i++) {
			if (tints.get(i) != 0xFFFFFF) continue;
			if (world.rand.nextFloat() >= PLATE_SMOLDER_RATIO) continue;
			IBlockState replacement = smolderState;
			if (burningState != null && world.rand.nextFloat() < PLATE_BURNING_CHANCE) replacement = burningState;
			if (replacement != null) render.set(i, replacement);
		}
		EntityLidPlate plate = new EntityLidPlate(world);
		plate.setPosition((double) center.getX(), (double) center.getY(), (double) center.getZ());
		plate.configure(positions, render, center, tints, lifts, rodColors);
		plate.setCoreBounds(bounds[0], bounds[1], bounds[2], bounds[3]);
		boolean special = sideways || reactorHasDigammaRod(world, center, true);
		if (special) diag("lid special launch for digamma reactor");
		plate.setRedSteam(special);
		int lidSteamColor = special ? RED_STEAM_COLOR : RISE_STEAM_COLOR;
		plate.motionY = PLATE_LAUNCH_VY;
		plate.motionX = (world.rand.nextDouble() - 0.5D) * PLATE_LAUNCH_SPREAD;
		plate.motionZ = (world.rand.nextDouble() - 0.5D) * PLATE_LAUNCH_SPREAD;
		diag("lid launch upward special=" + special);
		for (int k = 0; k < PLATE_STEAM_TICKS; k++) {
			final int step = k;
			DelayedTick.scheduleWorldStart(world, step, delayed -> lidSteam(delayed, plate, bounds, lidSteamColor));
		}
		diag("lid launch special=" + special + " color=" + lidSteamColor);
		plate.spin = (world.rand.nextBoolean() ? 1.0F : -1.0F) * PLATE_SPIN * (0.7F + world.rand.nextFloat() * 0.6F);
		world.spawnEntity(plate);
	}

	public static void spawnSteam(World world, double x, double y, double z) {
		PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(HbmEffectNT.RBMKSteam, new NBTTagCompound(), x, y, z), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, 200.0D));
	}

	public static void spawnSteamPuff(World world, double x, double y, double z, float base, float max, int life, int color) {
		NBTTagCompound data = new NBTTagCompound();
		data.setInteger("color", color);
		data.setFloat("alpha", RISE_STEAM_ALPHA);
		data.setFloat("lift", RISE_STEAM_PARTICLE_LIFT);
		data.setFloat("base", base);
		data.setFloat("max", max);
		data.setInteger("life", life);
		data.setBoolean("noWind", true);
		PacketThreading.createAllAroundThreadedPacket(new AuxParticlePacketNT(HbmEffectNT.Tower, data, x, y, z), new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, 200.0D));
	}

	public static void scheduleSteamRise(World world, BlockPos origin, List<BlockPos> tops, int[] bounds, int columnHeight, int color) {
		if (world == null || world.isRemote || origin == null || bounds == null) return;
		int baseY = origin.getY() + Math.max(columnHeight, 1) + 1;
		if (tops != null) {
			for (BlockPos top : tops) {
				if (top.getY() + 1 > baseY) baseY = top.getY() + 1;
			}
		}
		int[] box = bounds;
		float scale = riseScale(box);
		int interval = Math.max(1, Math.round((float) Math.sqrt((double) scale)));
		for (int k = 0; k < RISE_STEAM_TICKS; k += interval) {
			final int step = k;
			final float size = scale;
			final int y = baseY;
			DelayedTick.scheduleWorldStart(world, RISE_STEAM_DELAY + k, delayed -> steamRiseTick(delayed, box, y, size, step, color));
		}
	}

	public static float riseScale(int[] bounds) {
		float scale = (float) ((bounds[1] - bounds[0]) + (bounds[3] - bounds[2])) / 2.0F / 2.0F;
		return MathHelper.clamp(scale, RISE_STEAM_SCALE_MIN, RISE_STEAM_SCALE_CAP);
	}

	private static void steamRiseTick(World world, int[] bounds, int baseY, float scale, int step, int color) {
		if (world == null || world.isRemote || bounds == null) return;
		int life = (int) (RISE_STEAM_LIFE_BASE * (float) Math.pow((double) scale, 0.75D)) + world.rand.nextInt(10);
		life = Math.min(life, RISE_STEAM_TICKS - step + 2);
		life = MathHelper.clamp(life, RISE_STEAM_MIN_LIFE, RISE_STEAM_LIFE_CAP);
		double x = (double) bounds[0] + world.rand.nextDouble() * (double) (bounds[1] - bounds[0] + 1);
		double z = (double) bounds[2] + world.rand.nextDouble() * (double) (bounds[3] - bounds[2] + 1);
		double y = (double) baseY + world.rand.nextDouble() * 0.5D;
		spawnSteamPuff(world, x, y, z, scale, scale * RISE_STEAM_SCALE_MAX_RATIO, life, color);
	}

	public static void pumpNeighbours(World world, BlockPos pos, int target) {
		for (int dx = -NEIGHBOUR_RADIUS; dx <= NEIGHBOUR_RADIUS; dx++) {
			for (int dz = -NEIGHBOUR_RADIUS; dz <= NEIGHBOUR_RADIUS; dz++) {
				if (dx == 0 && dz == 0) continue;
				BlockPos scan = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
				if (!world.isBlockLoaded(scan)) continue;
				TileEntity te = world.getTileEntity(scan);
				if (!(te instanceof TileEntityRBMKBase)) continue;
				pump((TileEntityRBMKBase) te, target);
			}
		}
	}

	public static void pressureBurst(TileEntityRBMKBase boiler) {
		pressureBurst(boiler, false);
	}

	public static void pressureBurst(TileEntityRBMKBase boiler, boolean forced) {
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return;
		long now = world.getTotalWorldTime();
		if (!forced && lastBurstWorld == world && now - lastBurstTick < BURST_COOLDOWN) return;
		lastBurstWorld = world;
		lastBurstTick = now;
		BlockPos origin = boiler.getPos();
		List<Component> components = gatherComponents(world, origin);
		List<Component> columnSpace = gatherColumnSpace(world, components);
		diag("pressureBurst at " + origin + " components=" + components.size() + " columnSpace=" + columnSpace.size());
		int height = Math.max(RBMKDials.getColumnHeight(world), 1);
		int[] bounds = bounds(components, origin);
		boolean red = consumeRedBurst(world, origin);
		if (!red && reactorHasDigammaRod(world, origin, true)) {
			red = true;
			diag("special burst: digamma rod found in reactor, all steam goes red");
		}
		int steamColor = red ? RED_STEAM_COLOR : RISE_STEAM_COLOR;
		List<BlockPos> tops = new ArrayList<>();
		List<IBlockState> topStates = new ArrayList<>();
		List<Integer> topTints = new ArrayList<>();
		List<Float> topLifts = new ArrayList<>();
		List<Byte> topRodColors = new ArrayList<>();
		if (PLATE_ENABLED) {
			Set<BlockPos> seenTops = new HashSet<>();
			for (Component component : components) {
				if (!world.isBlockLoaded(component.pos)) continue;
				BlockPos top = null;
				for (int i = 1; i <= PLATE_TOP_SCAN; i++) {
					BlockPos pos = component.pos.up(i);
					if (!world.isBlockLoaded(pos)) break;
					if (world.isAirBlock(pos)) break;
					top = pos;
				}
				if (top == null || !seenTops.add(top)) continue;
				int tint = 0xFFFFFF;
				float lift = 0.0F;
				byte rodColor = -1;
				TileEntity controlTe = world.getTileEntity(component.pos);
				if (controlTe instanceof TileEntityRBMKControl) {
					lift = randomRodLevel(world, (float) MathHelper.clamp(((TileEntityRBMKControl) controlTe).level, 0.0D, 1.0D));
					tint = levelTint(lift);
					rodColor = controlTe instanceof TileEntityRBMKControlManual ? readRodColor(controlTe) : -1;
					if (controlTe instanceof TileEntityRBMKControlManual && rodColor < 0) rodColor = -2;
				}
				tops.add(top);
				topStates.add(world.getBlockState(top));
				topTints.add(tint);
				topLifts.add(lift);
				topRodColors.add(rodColor);
			}
		}
		Block debris = smolderBlock();
		if (debris != null) {
			scorch(world, origin, bounds, components, debris, height);
			scatterBurstDebris(world, bounds);
		}
		if (red) sendDigammaSteamBurst(world, bounds, components, height);
		new ExplosionNT(world, null, (double) origin.getX() + 0.5D, (double) origin.getY() + 0.5D, (double) origin.getZ() + 0.5D, BURST_RADIUS).addAttrib(ExplosionNT.ExAttrib.ALLDROP).addAttrib(ExplosionNT.ExAttrib.NOSOUND).addAttrib(ExplosionNT.ExAttrib.NOPARTICLE).explode();
		explodeFootprint(world, bounds, origin.getY(), height, red);
		cleanNeutronCache(world, origin);
		double centerX = (double) ((bounds[0] + bounds[1]) / 2) + 0.5D;
		double centerZ = (double) ((bounds[2] + bounds[3]) / 2) + 0.5D;
		double centerY = (double) origin.getY() + 1.0D;
		world.playSound(null, centerX, centerY, centerZ, HBMSoundHandler.rbmk_explosion, SoundCategory.BLOCKS, BURST_BOOM_VOLUME, BURST_BOOM_PITCH);
		world.playSound(null, centerX, centerY, centerZ, HBMSoundHandler.steamEngineOperate, SoundCategory.BLOCKS, BURST_STEAM_VOLUME, BURST_STEAM_PITCH + world.rand.nextFloat() * 0.25F);
		shakeCamera(world, centerX, centerY, centerZ, SHAKE_RANGE_JUMP, SHAKE_JUMP);
		Set<BlockPos> lifted = new HashSet<>();
		if (PLATE_ENABLED && !tops.isEmpty()) {
			for (BlockPos pos : tops) {
				if (!world.isBlockLoaded(pos)) continue;
				world.setBlockToAir(pos);
				lifted.add(pos);
			}
			spawnPlate(world, tops, topStates, bounds, topTints, topLifts, topRodColors, red);
		}
		diag("plate lifted=" + lifted.size());
		DelayedTick.scheduleWorldStart(world, AFTERMATH_DELAY, delayed -> aftermathBurst(delayed, origin, debris, components, columnSpace, lifted));
		scheduleSteamRise(world, origin, tops, bounds, height, steamColor);
	}

	public static void plateImpact(World world, double x, double y, double z) {
		if (world == null || world.isRemote) return;
		try {
			long time = world.getTotalWorldTime();
			int dimension = world.provider.getDimension();
			if (impactTick != time || impactDim != dimension) {
				impactTick = time;
				impactDim = dimension;
				impactUsed = 0;
			}
			boolean heavy = impactUsed < PLATE_IMPACT_MAX_PER_TICK;
			impactUsed++;
			int radius = heavy ? PLATE_IMPACT_RADIUS : PLATE_IMPACT_SMALL_RADIUS;
			int rSq = radius * radius;
			int cx = (int) Math.floor(x);
			int cy = (int) Math.floor(y);
			int cz = (int) Math.floor(z);
			int broken = 0;
			for (int dx = -radius; dx <= radius; dx++) {
				for (int dy = -radius; dy <= radius; dy++) {
					for (int dz = -radius; dz <= radius; dz++) {
						if (dx * dx + dy * dy + dz * dz > rSq) continue;
						if (meteorDamage(world, new BlockPos(cx + dx, cy + dy, cz + dz))) broken++;
					}
				}
			}
			if (heavy) {
				world.createExplosion(null, x, y, z, PLATE_IMPACT_EXPLOSION, false);
				ExplosionLarge.spawnRubble(world, x, y, z, PLATE_IMPACT_RUBBLE);
				ExplosionLarge.spawnParticles(world, x, y + 1.0D, z, 12);
			}
			diag("plate impact at " + cx + "," + cy + "," + cz + " broken=" + broken + " heavy=" + heavy);
		} catch (Throwable throwable) {
			diag("plate impact error: " + throwable);
		}
	}

	public static boolean meteorDamage(World world, BlockPos pos) {
		if (!world.isBlockLoaded(pos)) return false;
		IBlockState state = world.getBlockState(pos);
		Block block = state.getBlock();
		if (block.isAir(state, world, pos)) return false;
		if (state.getMaterial().isLiquid()) return false;
		float hardness = state.getBlockHardness(world, pos);
		if (hardness < 0.0F || hardness > PLATE_IMPACT_MAX_HARDNESS) return false;
		if (hardness <= 0.3F || block == Blocks.LEAVES || block == Blocks.LOG) {
			world.setBlockToAir(pos);
			return true;
		}
		if (world.rand.nextInt(3) == 0) {
			if (world.rand.nextInt(8) == 0) {
				EntityFallingBlock falling = new EntityFallingBlock(world, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, state);
				falling.motionY = 0.3D + world.rand.nextDouble() * 0.4D;
				falling.motionX = (world.rand.nextDouble() - 0.5D) * 0.5D;
				falling.motionZ = (world.rand.nextDouble() - 0.5D) * 0.5D;
				world.spawnEntity(falling);
			}
			world.setBlockToAir(pos);
			return true;
		}
		if (world.rand.nextInt(6) == 1) {
			if (block == Blocks.DIRT) {
				world.setBlockState(pos, ModBlocks.dirt_dead.getDefaultState(), 3);
			} else if (block == Blocks.SAND) {
				world.setBlockState(pos, (world.rand.nextInt(2) == 1 ? Blocks.SANDSTONE : Blocks.GRAVEL).getDefaultState(), 3);
			} else if (block == Blocks.STONE) {
				world.setBlockState(pos, Blocks.COBBLESTONE.getDefaultState(), 3);
			} else if (block == Blocks.GRASS) {
				world.setBlockState(pos, ModBlocks.waste_earth.getDefaultState(), 3);
			}
		}
		return false;
	}

	public static void shakeCamera(World world, double x, double y, double z, float range, String[] args) {
		if (world == null || world.isRemote) return;
		if (!Loader.isModLoaded(LEAFIA_MODID)) return;
		try {
			Class<?> packetClass = Class.forName("com.leafia.CommandLeaf$ShakecamPacket");
			Object packet = packetClass.getConstructor(String[].class).newInstance((Object) args);
			packetClass.getMethod("setPos", BlockPos.class).invoke(packet, new BlockPos(x, y, z));
			PacketThreading.createSendToAllTrackingThreadedPacket((ThreadedPacket) packet, new NetworkRegistry.TargetPoint(world.provider.getDimension(), x, y, z, (double) range));
		} catch (Throwable throwable) {
			diag("shake packet error: " + throwable);
		}
	}

	public static void shakeLand(World world, double x, double y, double z) {
		shakeCamera(world, x, y, z, SHAKE_RANGE_LAND, SHAKE_LAND);
	}

	public static void scorch(World world, BlockPos origin, int[] bounds, List<Component> components, Block debris, int height) {
		IBlockState debrisState = debris.getDefaultState();
		List<Integer> distances = new ArrayList<>();
		for (Component component : components) {
			distances.add(distanceSq(component.pos, origin));
		}
		Collections.sort(distances);
		int limit = distances.get(Math.min(distances.size() - 1, (int) ((float) distances.size() * BURST_RATIO)));
		for (Component component : components) {
			if (distanceSq(component.pos, origin) > limit) continue;
			int distFromEdge = Math.min(Math.min(component.pos.getX() - bounds[0], bounds[1] - component.pos.getX()), Math.min(component.pos.getZ() - bounds[2], bounds[3] - component.pos.getZ()));
			int reduce = MathHelper.clamp(distFromEdge + 1, 1, height);
			if (world.rand.nextInt(3) == 0) reduce = Math.min(reduce + 1, height);
			for (int i = 0; i <= height; i++) {
				BlockPos pos = component.pos.up(i);
				if (!world.isBlockLoaded(pos)) break;
				if (i <= height + 1 - reduce) {
					world.setBlockState(pos, debrisState, 3);
				} else {
					world.setBlockToAir(pos);
				}
			}
		}
	}

	public static int distanceSq(BlockPos a, BlockPos b) {
		int dx = a.getX() - b.getX();
		int dz = a.getZ() - b.getZ();
		return dx * dx + dz * dz;
	}

	public static final float STEAM_WAVE_SCALE = 3.0F;
	public static final float STEAM_WAVE_SCALE_RATIO = 3.0F;
	public static final int STEAM_WAVE_LIFE = 45;
	public static final int STEAM_WAVE_COUNT = 6;
	public static final float VENT_STEAM_SCALE = 2.0F;
	public static final int VENT_STEAM_LIFE = 45;

	public static void steamWave(World world, List<Component> components, int height, int wave, int color) {
		if (world == null) return;
		for (Component component : components) {
			for (int i = 0; i < STEAM_WAVE_COUNT; i++) {
				double x = (double) component.pos.getX() + 0.5D + (world.rand.nextDouble() - 0.5D) * 1.5D;
				double z = (double) component.pos.getZ() + 0.5D + (world.rand.nextDouble() - 0.5D) * 1.5D;
				double y = (double) (component.pos.getY() + height + 1 + wave) + world.rand.nextDouble() * STEAM_RISE;
				if (color == RISE_STEAM_COLOR) {
					spawnSteam(world, x, y, z);
				} else {
					spawnSteamPuff(world, x, y, z, STEAM_WAVE_SCALE, STEAM_WAVE_SCALE * STEAM_WAVE_SCALE_RATIO, STEAM_WAVE_LIFE, color);
				}
			}
		}
	}

	public static int[] bounds(List<Component> components, BlockPos origin) {
		int minX = origin.getX();
		int maxX = origin.getX();
		int minZ = origin.getZ();
		int maxZ = origin.getZ();
		for (Component component : components) {
			minX = Math.min(minX, component.pos.getX());
			maxX = Math.max(maxX, component.pos.getX());
			minZ = Math.min(minZ, component.pos.getZ());
			maxZ = Math.max(maxZ, component.pos.getZ());
		}
		return new int[]{minX, maxX, minZ, maxZ};
	}

	public static void aftermathBurst(World world, BlockPos origin, Block debris, List<Component> components, List<Component> columnSpace, Set<BlockPos> lifted) {
		if (world == null) return;
		diag("aftermathBurst at " + origin + " components=" + components.size());
		shakeCamera(world, (double) origin.getX() + 0.5D, (double) origin.getY() + 1.0D, (double) origin.getZ() + 0.5D, SHAKE_RANGE_BLAST, SHAKE_BLAST);
		int[] box = bounds(components, origin);
		for (EntityLidPlate plate : world.getEntitiesWithinAABB(EntityLidPlate.class, new AxisAlignedBB(origin).grow(64.0D))) {
			try {
				if (plateEdgeDistance(plate, box) >= PLATE_BLAST_MARGIN) continue;
				plate.blast();
			} catch (Throwable throwable) {
				diag("plate blast error: " + throwable);
			}
		}
		boolean specialReactor = reactorHasDigammaRod(world, origin, true);
		if (specialReactor) diag("second explosion is a digamma zeroing (no restore, no meltdown)");
		TileEntityRBMKBase anchor = null;
		if (!specialReactor) {
		for (Component component : components) {
			if (!world.isBlockLoaded(component.pos)) continue;
			if (debris != null && world.getBlockState(component.pos).getBlock() != debris) continue;
			world.setBlockState(component.pos, component.state, 3);
			TileEntity restored = world.getTileEntity(component.pos);
			if (restored instanceof TileEntityRBMKBoiler restoredBoiler) restoredBoiler.steam.setFill(0);
			if (restored instanceof TileEntityRBMKBase restoredBase) {
				restoredBase.reasimSteam = 0;
				restoredBase.reasimWater = 0;
			}
			TileEntity te = restored;
			if (!(te instanceof TileEntityRBMKBase)) continue;
			te.readFromNBT(component.nbt);
			if (anchor == null || component.pos.equals(origin)) anchor = (TileEntityRBMKBase) te;
		}
		for (Component component : columnSpace) {
			if (lifted.contains(component.pos)) continue;
			if (!world.isBlockLoaded(component.pos)) continue;
			if (debris != null && world.getBlockState(component.pos).getBlock() != debris) continue;
			world.setBlockState(component.pos, component.state, 3);
			if (component.nbt == null) continue;
			TileEntity te = world.getTileEntity(component.pos);
			if (te == null) continue;
			te.readFromNBT(component.nbt);
		}
		}
		if (anchor != null && !specialReactor) {
			try {
				allowMeltdown = true;
				anchor.meltdown();
			} catch (Throwable throwable) {
				diag("aftermath meltdown error: " + throwable);
				throwable.printStackTrace();
			} finally {
				allowMeltdown = false;
			}
		}
		if (specialReactor) {
			sendDigammaSteamBurst(world, box, components, Math.max(RBMKDials.getColumnHeight(world), 1));
			digammaZeroingPayload(world, new BlockPos((box[0] + box[1]) / 2, origin.getY(), (box[2] + box[3]) / 2), box);
		}
		int columnHeight = Math.max(RBMKDials.getColumnHeight(world), 1);
		wreckReactor(world, origin, box, columnHeight);
		smokeColumn(world, origin, box, columnHeight);
		smotherFire(world, origin);
		cleanNeutronCache(world, origin);
	}

	private static double plateEdgeDistance(EntityLidPlate plate, int[] box) {
		AxisAlignedBB plateBox = plate.getEntityBoundingBox();
		double dx = Math.max(Math.max((double) box[0] - plateBox.maxX, plateBox.minX - (double) box[1] - 1.0D), 0.0D);
		double dz = Math.max(Math.max((double) box[2] - plateBox.maxZ, plateBox.minZ - (double) box[3] - 1.0D), 0.0D);
		return Math.sqrt(dx * dx + dz * dz);
	}

	public static void wreckReactor(World world, BlockPos origin, int[] bounds, int height) {
		if (world == null || world.isRemote) return;
		Block wreck = ModBlocks.pribris;
		if (wreck == null) return;
		IBlockState state = wreck.getDefaultState();
		int minY = origin.getY();
		int maxY = origin.getY() + Math.max(height, 1) + 1;
		int converted = 0;
		for (int x = bounds[0]; x <= bounds[1]; x++) {
			for (int z = bounds[2]; z <= bounds[3]; z++) {
				for (int y = minY; y <= maxY; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					if (!world.isBlockLoaded(pos)) continue;
					Block block = world.getBlockState(pos).getBlock();
					if (block == Blocks.FIRE) {
						world.setBlockToAir(pos);
						continue;
					}
					boolean rbmk = block instanceof com.hbm.blocks.machine.rbmk.RBMKBase;
					if (!rbmk) {
						TileEntity te = world.getTileEntity(pos);
						rbmk = te instanceof TileEntityRBMKBase;
					}
					if (!rbmk) continue;
					world.setBlockState(pos, state, 3);
					converted++;
				}
			}
		}
		diag("wreck sweep converted=" + converted);
	}

	public static Block smolderBlock() {
		Block block = Block.getBlockFromName(SMOLDER_BLOCK);
		if (block == null) block = Block.getBlockFromName("hbm:pribris_smoke");
		return block;
	}

	private static Block smokeBlockFor(World world, BlockPos origin) {
		if (reactorHasDigammaRod(world, origin)) {
			Block digamma = Block.getBlockFromName("hbm:pribris_digamma");
			if (digamma != null) return digamma;
		}
		return smolderBlock();
	}

	public static void smokeColumn(World world, BlockPos origin, int[] bounds, int height) {
		if (world == null || world.isRemote) return;
		Block smoke = smokeBlockFor(world, origin);
		if (smoke == null) return;
		int avgX = (bounds[0] + bounds[1]) / 2;
		int avgZ = (bounds[2] + bounds[3]) / 2;
		int limit = Math.max(height, 1);
		int top = origin.getY() + limit + 1;
		float scale = (float) (bounds[1] - bounds[0] + bounds[3] - bounds[2]) / 2.0F / 2.0F;
		IBlockState smokeState = smoke.getDefaultState();
		BlockPos target = null;
		BlockPos highest = null;
		for (int y = top; y >= 0; y--) {
			BlockPos pos = new BlockPos(avgX, y, avgZ);
			if (!world.isBlockLoaded(pos)) break;
			Block block = world.getBlockState(pos).getBlock();
			if (block != Blocks.AIR && highest == null) highest = pos;
			boolean wreck = block instanceof com.hbm.blocks.machine.rbmk.RBMKDebris || block instanceof com.hbm.blocks.fluid.CoriumFinite;
			if (wreck) {
				target = pos;
				break;
			}
		}
		if (target == null) target = highest == null ? new BlockPos(avgX, origin.getY() + limit, avgZ) : highest.up();
		if (!world.isBlockLoaded(target)) return;
		world.setBlockState(target, smokeState, 3);
		TileEntity te = world.getTileEntity(target);
		if (te == null) return;
		try {
			te.getClass().getMethod("setScale", float.class).invoke(te, scale);
		} catch (Throwable throwable) {
		}
		diag("smoke column at " + target + " scale=" + scale);
	}

	public static final double DIGAMMA_MELTDOWN_HEAT = 2000.0D;

	public static boolean digammaColumnOverheat(TileEntityRBMKRod rod) {
		if (rod == null) return false;
		return rod.heat >= DIGAMMA_MELTDOWN_HEAT;
	}

	public static boolean rodAtMeltingPoint(TileEntityRBMKRod rod) {
		if (rod == null) return false;
		ItemStack stack = rod.inventory.getStackInSlot(0);
		if (stack.isEmpty()) return false;
		if (!(stack.getItem() instanceof ItemRBMKRod fuel)) return false;
		return ItemRBMKRod.getHullHeat(stack) > fuel.meltingPoint;
	}

	public static double overheatReactivity(TileEntityRBMKRod rod, double inFlux) {
		if (!rodAtMeltingPoint(rod)) return inFlux;
		if (!steamNotYetAtBurst(rod)) return inFlux;
		return inFlux * OVERHEAT_NEUTRON_MULT;
	}

	public static boolean steamNotYetAtBurst(TileEntityRBMKBase column) {
		if (column == null) return false;
		World world = column.getWorld();
		if (world == null || world.isRemote) return false;
		int reasim = column.reasimSteam;
		double tank = 0D;
		BlockPos pos = column.getPos();
		for (int dx = -NEIGHBOUR_RADIUS; dx <= NEIGHBOUR_RADIUS; dx++) {
			for (int dz = -NEIGHBOUR_RADIUS; dz <= NEIGHBOUR_RADIUS; dz++) {
				BlockPos scan = new BlockPos(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
				if (!world.isBlockLoaded(scan)) continue;
				TileEntity te = world.getTileEntity(scan);
				if (te instanceof TileEntityRBMKBase base) reasim = Math.max(reasim, base.reasimSteam);
				if (te instanceof TileEntityRBMKBoiler boiler) tank = Math.max(tank, (double) boiler.steam.getFill());
			}
		}
		return reasim < REASIM_BURST_THRESHOLD && tank < (double) PRESSURE_BURST_THRESHOLD;
	}

	public static void overheatFlames(World world, BlockPos base, int height) {
		if (world == null || world.isRemote) return;
		double x = (double) base.getX() + 0.5D;
		double z = (double) base.getZ() + 0.5D;
		double y = (double) base.getY() + (double) height + 0.5D;
		for (int i = 0; i < OVERHEAT_FLAME_COUNT; i++) {
			double fx = x + (world.rand.nextDouble() - 0.5D) * 0.6D;
			double fz = z + (world.rand.nextDouble() - 0.5D) * 0.6D;
			ParticleUtil.spawnGasFlame(world, fx, y, fz, 0.0D, 0.2D, 0.0D);
		}
	}

	public static void moltenColumn(World world, BlockPos base, int height) {
		if (world == null || world.isRemote) return;
		diag("overheat meltdown at " + base + " height=" + height);
		if (!world.isBlockLoaded(base)) return;
		Block corium = ModBlocks.corium_block;
		if (corium == null) return;
		TileEntity te = world.getTileEntity(base);
		if (te instanceof TileEntityRBMKRod rod) rod.inventory.setStackInSlot(0, ItemStack.EMPTY);
		IBlockState state = corium.getDefaultState();
		int limit = Math.max(height, 1);
		for (int i = 0; i <= limit; i++) {
			BlockPos pos = base.up(i);
			if (!world.isBlockLoaded(pos)) continue;
			world.setBlockState(pos, state, 3);
		}
		BlockPos cap = base.up(limit + 1);
		if (world.isBlockLoaded(cap) && world.getBlockState(cap).getBlock() instanceof com.hbm.blocks.machine.rbmk.RBMKBase) world.setBlockState(cap, state, 3);
		if (NTMITWorldRules.entitySummonDisabled(world)) return;
		Block chunk = ModBlocks.block_corium;
		if (chunk == null) return;
		IBlockState chunkState = chunk.getDefaultState();
		double x = (double) base.getX() + 0.5D;
		double y = (double) base.getY() + (double) limit + 0.5D;
		double z = (double) base.getZ() + 0.5D;
		for (int i = 0; i < OVERHEAT_CORIUM_THROW; i++) {
			double angle = world.rand.nextDouble() * Math.PI * 2.0D;
			double speed = 0.15D + world.rand.nextDouble() * 0.35D;
			EntityFallingBlock piece = new EntityFallingBlock(world, x, y, z, chunkState);
			piece.motionX = Math.cos(angle) * speed;
			piece.motionZ = Math.sin(angle) * speed;
			piece.motionY = 0.45D + world.rand.nextDouble() * 0.55D;
			world.spawnEntity(piece);
		}
	}

	public static void smotherFire(World world, BlockPos origin) {
		Block burning = Block.getBlockFromName(BURNING_BLOCK);
		Block smoke = smolderBlock();
		IBlockState smokeState = smoke == null ? Blocks.AIR.getDefaultState() : smoke.getDefaultState();
		int radius = PLATE_QUENCH_RADIUS;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = -6; dy <= 20; dy++) {
					BlockPos pos = new BlockPos(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!world.isBlockLoaded(pos)) continue;
					Block block = world.getBlockState(pos).getBlock();
					if (block == Blocks.FIRE) {
						world.setBlockToAir(pos);
					} else if (burning != null && block == burning) {
						world.setBlockState(pos, smokeState, 3);
					}
				}
			}
		}
	}

	public static void quenchFire(World world, BlockPos origin) {
		int radius = PLATE_QUENCH_RADIUS;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				for (int dy = -6; dy <= 20; dy++) {
					BlockPos pos = new BlockPos(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
					if (!world.isBlockLoaded(pos)) continue;
					if (world.getBlockState(pos).getBlock() != Blocks.FIRE) continue;
					world.setBlockToAir(pos);
				}
			}
		}
	}

	public static List<Component> gatherColumnSpace(World world, List<Component> components) {
		List<Component> space = new ArrayList<>();
		int height = Math.max(RBMKDials.getColumnHeight(world), 1);
		for (Component component : components) {
			for (int i = 1; i <= height && space.size() < MAX_COLUMN_FILL; i++) {
				BlockPos pos = component.pos.up(i);
				if (!world.isBlockLoaded(pos)) break;
				TileEntity te = world.getTileEntity(pos);
				space.add(new Component(pos, world.getBlockState(pos), te == null ? null : te.writeToNBT(new NBTTagCompound())));
			}
		}
		return space;
	}

	public static void scatterBurstDebris(World world, int[] bounds) {
		Block smolder = Block.getBlockFromName(SMOLDER_BLOCK);
		if (smolder == null) return;
		IBlockState state = smolder.getDefaultState();
		int cx = (bounds[0] + bounds[1]) / 2;
		int cz = (bounds[2] + bounds[3]) / 2;
		int span = Math.max(bounds[1] - bounds[0], bounds[3] - bounds[2]) / 2;
		int radius = span + BURST_DEBRIS_BONUS;
		int placed = 0;
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				if (placed >= MAX_BURST_DEBRIS) return;
				int dist2 = dx * dx + dz * dz;
				if (dist2 > radius * radius) continue;
				if (dist2 < span * span) continue;
				if (world.rand.nextFloat() > BURST_DEBRIS_RATIO) continue;
				int x = cx + dx;
				int z = cz + dz;
				BlockPos column = new BlockPos(x, 0, z);
				if (!world.isBlockLoaded(column)) continue;
				int y = world.getHeight(x, z);
				BlockPos pos = new BlockPos(x, y, z);
				IBlockState existing = world.getBlockState(pos);
				if (!existing.getBlock().isAir(existing, world, pos) && !existing.getBlock().isReplaceable(world, pos)) continue;
				world.setBlockState(pos, state, 3);
				placed++;
			}
		}
	}

	public static List<Component> gatherComponents(World world, BlockPos origin) {
		List<Component> components = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		queue.add(origin);
		seen.add(origin);
		while (!queue.isEmpty() && components.size() < MAX_COMPONENTS) {
			BlockPos pos = queue.poll();
			if (!world.isBlockLoaded(pos)) continue;
			TileEntity te = world.getTileEntity(pos);
			if (!(te instanceof TileEntityRBMKBase)) continue;
			components.add(new Component(pos, world.getBlockState(pos), te.writeToNBT(new NBTTagCompound())));
			for (EnumFacing facing : EnumFacing.values()) {
				BlockPos next = pos.offset(facing);
				if (!seen.add(next)) continue;
				if (!world.isBlockLoaded(next)) continue;
				if (world.getTileEntity(next) instanceof TileEntityRBMKBase) queue.add(next);
			}
		}
		return components;
	}

	public static void cleanNeutronCache(World world, BlockPos origin) {
		NeutronNodeWorld.getOrAddWorld(world).removeAllStreamsOfType(NeutronStream.NeutronType.RBMK);
		for (int dx = -CACHE_CLEAN_RADIUS; dx <= CACHE_CLEAN_RADIUS; dx++) {
			for (int dz = -CACHE_CLEAN_RADIUS; dz <= CACHE_CLEAN_RADIUS; dz++) {
				for (int dy = -4; dy <= 16; dy++) {
					NeutronNodeWorld.removeNode(world, new BlockPos(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz));
				}
			}
		}
	}

	public static class Component {

		public final BlockPos pos;
		public final IBlockState state;
		public final NBTTagCompound nbt;

		public Component(BlockPos pos, IBlockState state, NBTTagCompound nbt) {
			this.pos = pos;
			this.state = state;
			this.nbt = nbt;
		}
	}
}
