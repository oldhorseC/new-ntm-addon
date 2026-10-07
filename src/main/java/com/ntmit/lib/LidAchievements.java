package com.ntmit.lib;

import net.minecraft.advancements.Advancement;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LidAchievements {

	public static final ResourceLocation DIGAMMA_ZEROING = new ResourceLocation(RefStrings.MODID, "digamma_zeroing");
	public static final ResourceLocation LID_LAUNCH = new ResourceLocation(RefStrings.MODID, "lid_launch");
	public static final ResourceLocation LID_CRUSH = new ResourceLocation(RefStrings.MODID, "lid_crush");

	@SubscribeEvent
	public void onDeath(LivingDeathEvent event) {
		if (!(event.getEntityLiving() instanceof EntityPlayerMP player)) return;
		String type = event.getSource().getDamageType();
		ResourceLocation id = null;
		if ("ntmitLidAscend".equals(type)) id = LID_LAUNCH;
		if ("ntmitLidDescend".equals(type)) id = LID_CRUSH;
		if (id == null) return;
		grant(player, id);
	}

	public static void grant(EntityPlayerMP player, ResourceLocation id) {
		if (player == null || player.getServer() == null) return;
		Advancement advancement = player.getServer().getAdvancementManager().getAdvancement(id);
		if (advancement == null) return;
		player.getAdvancements().grantCriterion(advancement, "triggered");
	}
}
