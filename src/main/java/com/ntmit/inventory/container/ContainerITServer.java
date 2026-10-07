package com.ntmit.inventory.container;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.inventory.slot.SlotBattery;
import com.ntmit.items.ModItems;
import com.ntmit.tileentity.TileEntityITServerCore;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

public class ContainerITServer extends Container {

	public static final int PLAYER_INV_X = 7;

	public static final int PLAYER_INV_Y = 139;

	public static final int BATTERY_SLOT_INDEX = 0;
	public static final int BATTERY_SLOT_X = 7;
	public static final int BATTERY_SLOT_Y = 113;

	public static final int CONNECTOR_SLOT_INDEX = 1;
	public static final int CONNECTOR_SLOT_X = 26;
	public static final int CONNECTOR_SLOT_Y = 113;

	private final TileEntityITServerCore core;

	public ContainerITServer(InventoryPlayer invPlayer, TileEntityITServerCore core) {
		this.core = core;

		this.addSlotToContainer(new SlotBattery(core.getBatterySlot(), 0, BATTERY_SLOT_X, BATTERY_SLOT_Y));

		this.addSlotToContainer(new SlotItemHandler(core.getConnectorSlot(), 0, CONNECTOR_SLOT_X, CONNECTOR_SLOT_Y));

		for (int row = 0; row < 3; row++) {
			for (int col = 0; col < 9; col++) {
				this.addSlotToContainer(new Slot(invPlayer, col + row * 9 + 9, PLAYER_INV_X + col * 18, PLAYER_INV_Y + row * 18));
			}
		}

		for (int col = 0; col < 9; col++) {
			this.addSlotToContainer(new Slot(invPlayer, col, PLAYER_INV_X + col * 18, PLAYER_INV_Y + 58));
		}
	}

	public TileEntityITServerCore getCore() {
		return this.core;
	}

	public ItemStack getBatteryStack() {
		return this.core.getBatteryStack();
	}

	public ItemStack getConnectorStack() {
		return this.core.getConnectorStack();
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
		boolean fromMachine = index == BATTERY_SLOT_INDEX || index == CONNECTOR_SLOT_INDEX;
		int firstPlayerSlot = CONNECTOR_SLOT_INDEX + 1;

		if (fromMachine) {
			if (!this.mergeItemStack(stack, firstPlayerSlot, this.inventorySlots.size(), true)) return ItemStack.EMPTY;
		} else if (stack.getItem() instanceof IBatteryItem && !this.inventorySlots.get(BATTERY_SLOT_INDEX).getHasStack()) {
			if (!this.mergeItemStack(stack, BATTERY_SLOT_INDEX, BATTERY_SLOT_INDEX + 1, false)) return ItemStack.EMPTY;
		} else if (stack.getItem() == ModItems.server_connector && !this.inventorySlots.get(CONNECTOR_SLOT_INDEX).getHasStack()) {
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
