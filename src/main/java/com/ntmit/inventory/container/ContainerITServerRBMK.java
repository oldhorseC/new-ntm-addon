package com.ntmit.inventory.container;

import com.hbm.items.tool.ItemRBMKTool;
import com.ntmit.items.ModItems;
import com.ntmit.tileentity.TileEntityITServerCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerITServerRBMK extends Container {

	public static final int CONNECTOR_SLOT_INDEX = 0;
	public static final int CONNECTOR_SLOT_X = 8;
	public static final int CONNECTOR_SLOT_Y = 8;

	public static final int HOTBAR_X = 41;
	public static final int HOTBAR_Y = 174;

	private final TileEntityITServerCore core;

	public ContainerITServerRBMK(InventoryPlayer invPlayer, TileEntityITServerCore core) {
		this.core = core;

		this.addSlotToContainer(new SlotItemHandler(core.getRbmkToolSlot(), 0, CONNECTOR_SLOT_X, CONNECTOR_SLOT_Y));

		for (int col = 0; col < 9; col++) {
			this.addSlotToContainer(new Slot(invPlayer, col, HOTBAR_X + col * 18, HOTBAR_Y));
		}
	}

	public TileEntityITServerCore getCore() {
		return this.core;
	}

	public ItemStack getRbmkToolStack() {
		return this.core.getRbmkToolStack();
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = this.inventorySlots.get(index);
		if (slot == null || !slot.getHasStack()) return ItemStack.EMPTY;

		ItemStack stack = slot.getStack();
		ItemStack copy = stack.copy();

		if (index == CONNECTOR_SLOT_INDEX) {
			if (!this.mergeItemStack(stack, CONNECTOR_SLOT_INDEX + 1, this.inventorySlots.size(), true)) return ItemStack.EMPTY;
		} else if ((stack.getItem() instanceof ItemRBMKTool || stack.getItem() == ModItems.server_connector) && !this.inventorySlots.get(CONNECTOR_SLOT_INDEX).getHasStack()) {
			if (!this.mergeItemStack(stack, CONNECTOR_SLOT_INDEX, CONNECTOR_SLOT_INDEX + 1, false)) return ItemStack.EMPTY;
		} else {
			return ItemStack.EMPTY;
		}

		if (stack.isEmpty()) {
			slot.putStack(ItemStack.EMPTY);
		} else {
			slot.onSlotChange(stack, copy);
		}

		return copy;
	}
}
