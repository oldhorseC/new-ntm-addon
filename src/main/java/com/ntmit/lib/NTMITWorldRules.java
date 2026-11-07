package com.ntmit.lib;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;

public class NTMITWorldRules {

	public static final String WET_STEAM = "ntmitWetSteam";
	public static final String DISABLE_STEAM_EXPLOSION = "dialDisableSteamExplosion";
	public static final String DISABLE_ENTITY_SUMMON = "dialDisableEntitySummon";
	public static final String RBMK_DEBUG = "ntmitRbmkDebug";
	public static final String MOBILE_MODE = "ntmitMobileMode";

	@SubscribeEvent
	public void onWorldLoad(WorldEvent.Load event) {
		World world = event.getWorld();
		if (world == null || world.isRemote) return;
		GameRules rules = world.getGameRules();
		for (String key : new String[] {WET_STEAM, DISABLE_STEAM_EXPLOSION, DISABLE_ENTITY_SUMMON, RBMK_DEBUG, MOBILE_MODE}) {
			if (!rules.getString(key).isEmpty()) continue;
			rules.setOrCreateGameRule(key, "false");
		}
	}

	@SubscribeEvent
	public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
		EntityPlayer player = event.player;
		if (player == null || player.world == null || player.world.isRemote) return;
		if (!(player instanceof EntityPlayerMP)) return;
		com.ntmit.packet.PacketNTMITRbmkDebug.send((EntityPlayerMP) player, rbmkDebug(player.world));
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

	public static boolean rbmkDebug(World world) {
		if (world == null) return false;
		return world.getGameRules().getBoolean(RBMK_DEBUG);
	}

	public static void setRbmkDebug(World world, boolean value) {
		if (world == null) return;
		world.getGameRules().setOrCreateGameRule(RBMK_DEBUG, Boolean.toString(value));
	}

	public static boolean mobileMode(World world) {
		if (world == null) return false;
		return world.getGameRules().getBoolean(MOBILE_MODE);
	}

	public static void setMobileMode(World world, boolean value) {
		if (world == null) return;
		world.getGameRules().setOrCreateGameRule(MOBILE_MODE, Boolean.toString(value));
	}
}