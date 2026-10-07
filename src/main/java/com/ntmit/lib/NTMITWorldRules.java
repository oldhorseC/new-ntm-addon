package com.ntmit.lib;

import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class NTMITWorldRules {

	public static final String WET_STEAM = "ntmitWetSteam";
	public static final String DISABLE_STEAM_EXPLOSION = "dialDisableSteamExplosion";
	public static final String DISABLE_ENTITY_SUMMON = "dialDisableEntitySummon";

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		World world = event.getWorld();
		if (world == null || world.isRemote) return;
		GameRules rules = world.getGameRules();
		for (String key : new String[] {WET_STEAM, DISABLE_STEAM_EXPLOSION, DISABLE_ENTITY_SUMMON}) {
			if (!rules.getString(key).isEmpty()) continue;
			rules.setOrCreateGameRule(key, "false");
		}
	}

	@SubscribeEvent
	public void onWorldUnload(WorldEvent.Unload event) {
		com.ntmit.lib.RbmkJumpHandler.clearCaches();
	}

	public static boolean wetSteam(World world) {
		if (world == null || world.isRemote) return false;
		return world.getGameRules().getBoolean(WET_STEAM);
	}

	public static boolean steamExplosionDisabled(World world) {
		if (world == null) return false;
		return world.getGameRules().getBoolean(DISABLE_STEAM_EXPLOSION);
	}

	public static boolean entitySummonDisabled(World world) {
		if (world == null) return false;
		return world.getGameRules().getBoolean(DISABLE_ENTITY_SUMMON);
	}
}