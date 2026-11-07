package com.ntmit.lib;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class TurbinePaintEventHandler {

	@SubscribeEvent
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		EntityPlayer player = event.getEntityPlayer();
		EnumHand hand = event.getHand();
		EnumHand screwHand = TurbinePaintInteraction.screwdriverHand(player, hand);
		if (screwHand == null) return;
		Block block = event.getWorld().getBlockState(event.getPos()).getBlock();
		if (!TurbinePaintInteraction.tryPaint(block, event.getWorld(), player, event.getPos(), screwHand)) return;
		event.setCanceled(true);
	}
}
