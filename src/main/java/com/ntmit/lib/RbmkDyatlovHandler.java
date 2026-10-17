package com.ntmit.lib;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.hbm.items.tool.ItemDyatlov;
import com.ntmit.items.ItemDyatlovSteamDevice;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class RbmkDyatlovHandler {

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
		ItemStack stack = event.getItemStack();
		boolean meltdownTool = stack.getItem() instanceof ItemDyatlov;
		boolean steamTool = stack.getItem() instanceof ItemDyatlovSteamDevice;
		if (!meltdownTool && !steamTool) return;
		World world = event.getWorld();
		BlockPos pos = event.getPos();
		if (!(world.getBlockState(pos).getBlock() instanceof RBMKBase)) return;
		event.setCanceled(true);
		event.setUseBlock(Event.Result.DENY);
		event.setUseItem(Event.Result.DENY);
		if (world.isRemote) return;
		BlockPos core = RbmkJumpHandler.reactorCore(world, pos);
		if (core == null) return;
		if (meltdownTool) {
			RbmkJumpHandler.quickMeltdown(world, core);
		} else {
			RbmkJumpHandler.scheduleSteamExplosion(world, core);
		}
	}
}