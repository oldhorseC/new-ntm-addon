package com.ntmit.blocks;

import com.hbm.blocks.BlockBase;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.NTMITMod;
import com.ntmit.tileentity.TileEntityITTestMultiblock;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class BlockTestMultiblock extends BlockBase {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	public static final PropertyInteger ROLE = PropertyInteger.create("role", 0, 2);

	public static final int ROLE_BOTTOM = 0;
	public static final int ROLE_MIDDLE = 1;
	public static final int ROLE_TOP = 2;

	public static final int HEIGHT = 3;

	private static boolean dismantling = false;

	public BlockTestMultiblock() {
		super(Material.IRON);

		setRegistryName(new ResourceLocation(RefStrings.MODID, "test_multiblock"));
		setTranslationKey(RefStrings.MODID + ".test_multiblock");
		setCreativeTab(NTMITMod.tabITContent);
		setHardness(3F);
		setResistance(30F);

		setDefaultState(this.blockState.getBaseState()
				.withProperty(FACING, EnumFacing.NORTH)
				.withProperty(ROLE, ROLE_BOTTOM));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING, ROLE);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.byHorizontalIndex(meta & 3);
		int role = Math.min((meta >> 2) & 3, ROLE_TOP);
		return getDefaultState().withProperty(FACING, facing).withProperty(ROLE, role);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getHorizontalIndex() | (state.getValue(ROLE) << 2);
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);
		if (world.isRemote || !(placer instanceof EntityPlayer)) return;

		for (int i = 1; i < HEIGHT; i++) {
			BlockPos cell = pos.up(i);
			if (!world.getBlockState(cell).getBlock().isReplaceable(world, cell)) {
				world.setBlockToAir(pos);
				refund((EntityPlayer) placer, stack);
				return;
			}
		}

		EnumFacing facing = world.getBlockState(pos).getValue(FACING);

		for (int i = 0; i < HEIGHT; i++) {
			BlockPos cell = pos.up(i);
			IBlockState target = getDefaultState().withProperty(FACING, facing).withProperty(ROLE, i);
			if (world.getBlockState(cell) != target) world.setBlockState(cell, target, 3);
		}
	}

	private void refund(EntityPlayer player, ItemStack placed) {
		if (player.capabilities.isCreativeMode) return;

		ItemStack refunded = new ItemStack(this);
		ItemStack held = player.getHeldItemMainhand();

		if (placed != null && !held.isEmpty() && held.isItemEqual(placed) && held.getCount() < held.getMaxStackSize()) {
			held.grow(1);
		} else if (!player.inventory.addItemStackToInventory(refunded)) {
			player.dropItem(refunded, false);
		}
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return state.getValue(ROLE) == ROLE_BOTTOM;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return state.getValue(ROLE) == ROLE_BOTTOM ? new TileEntityITTestMultiblock() : null;
	}

	@Override
	public void breakBlock(World world, BlockPos pos, IBlockState state) {
		super.breakBlock(world, pos, state);

		if (world.isRemote || dismantling) return;

		int role = state.getValue(ROLE);
		BlockPos core = pos.down(role);

		if (role != ROLE_BOTTOM && world.getBlockState(core).getBlock() != this) return;

		dismantling = true;

		for (int i = 0; i < HEIGHT; i++) {
			BlockPos cell = core.up(i);
			if (cell.equals(pos)) continue;
			if (world.getBlockState(cell).getBlock() == this) world.setBlockToAir(cell);
		}

		dismantling = false;
	}
}
