package com.ntmit.lib;

import com.hbm.api.block.IToolable;
import com.hbm.blocks.BlockDummyable;
import com.hbm.items.tool.ItemTooling;
import com.ntmit.packet.PacketNTMITTurbinePaint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class TurbinePaintInteraction {

	private TurbinePaintInteraction() {
	}

	public static boolean tryPaint(Object block, World world, EntityPlayer player, BlockPos pos, EnumHand screwHand) {
		if (!(block instanceof BlockDummyable)) return false;
		if (world == null || player == null || pos == null || screwHand == null) return false;
		if (!player.isSneaking()) return false;
		EnumHand paintHand = screwHand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
		int color = TurbinePaintColors.colorOf(player.getHeldItem(paintHand));
		if (color < 0) {
			return false;
		}
		if (world.isRemote) return true;
		TileEntity target = ((BlockDummyable) block).findCoreTE(world, pos);
		if (target == null) target = world.getTileEntity(pos);
		if (target == null) return false;
		TurbinePaintStore.set(target, color);
		target.markDirty();
		PacketNTMITTurbinePaint.sendAround(world, target.getPos(), color);
		return true;
	}

	public static EnumHand screwdriverHand(EntityPlayer player, EnumHand hand) {
		if (player == null || hand == null) return null;
		if (isScrewdriver(player.getHeldItem(hand))) return hand;
		EnumHand other = hand == EnumHand.MAIN_HAND ? EnumHand.OFF_HAND : EnumHand.MAIN_HAND;
		if (isScrewdriver(player.getHeldItem(other))) return other;
		return null;
	}

	public static boolean isScrewdriver(ItemStack stack) {
		if (stack == null || stack.isEmpty()) return false;
		return stack.getItem() instanceof ItemTooling
				&& ((ItemTooling) stack.getItem()).getType() == IToolable.ToolType.SCREWDRIVER;
	}
}

