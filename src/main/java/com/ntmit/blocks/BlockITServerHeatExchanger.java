package com.ntmit.blocks;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.FluidContainerRegistry;
import com.hbm.items.machine.IItemFluidIdentifier;
import com.ntmit.tileentity.TileEntityITServerHeatExchanger;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.init.SoundEvents;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.wrapper.InvWrapper;

public class BlockITServerHeatExchanger extends BlockITServerColumn {

	public BlockITServerHeatExchanger() {
		super("server_heat_exchanger");
	}

	@Override
	protected TileEntity createColumnTileEntity() {
		return new TileEntityITServerHeatExchanger();
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem(hand);
		if (held.isEmpty()) return false;
		if (world.isRemote) return true;

		BlockPos core = this.findCore(world, pos);
		if (core == null) return false;

		TileEntity tile = world.getTileEntity(core);
		if (!(tile instanceof TileEntityITServerHeatExchanger exchanger)) return false;

		if (held.getItem() instanceof IItemFluidIdentifier identifier) {
			FluidType type = identifier.getType(world, core.getX(), core.getY(), core.getZ(), held);
			exchanger.setInputType(type);
			player.sendMessage(new TextComponentTranslation(type.getConditionalName()));
			return true;
		}

		TileEntityITServerHeatExchanger master = exchanger.getMaster();
		InvWrapper slots = new InvWrapper(player.inventory);
		int slot = hand == EnumHand.OFF_HAND ? 40 : player.inventory.currentItem;

		if (master.in.loadTank(slot, slot, slots) || master.out.unloadTank(slot, slot, slots)) {
			world.playSound(null, pos, SoundEvents.ITEM_BUCKET_FILL, SoundCategory.PLAYERS, 0.5F, 1.0F);
			master.markDirty();
			return true;
		}

		if (held.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY, null) != null
				|| FluidContainerRegistry.getMaxFillCapacity(held) > 0
				|| FluidContainerRegistry.getFluidType(held) != Fluids.NONE) {
			return true;
		}

		return false;
	}
}
