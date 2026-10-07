package com.ntmit.blocks;

import com.hbm.blocks.BlockDummyable;
import com.hbm.lib.ForgeDirection;
import com.hbm.util.I18nUtil;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.NTMITMod;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public abstract class BlockITServerColumn extends BlockDummyable {

	public static final int COLUMN_HEIGHT = 3;

	public static final int META_CORE_MIN = BlockDummyable.offset + 2;

	protected BlockITServerColumn(String name) {

		super(Material.IRON, name, true);

		setTranslationKey(RefStrings.MODID + "." + name);

		setCreativeTab(NTMITMod.tabITContent);
		setHardness(3F);
		setResistance(30F);
	}

	@Override
	public int[] getDimensions() {
		return new int[] {COLUMN_HEIGHT - 1, 0, 0, 0, 0, 0};
	}

	@Override
	public int getOffset() {
		return 0;
	}

	@Override
	protected void fillSpace(World world, int x, int y, int z, ForgeDirection dir, int o) {
		super.fillSpace(world, x, y, z, dir, o);

		this.makeExtra(world, x, y + COLUMN_HEIGHT - 1, z);
	}

	@Override
	public ForgeDirection getDirModified(ForgeDirection dir) {
		if (dir == null || dir == ForgeDirection.UNKNOWN || dir.offsetY != 0) return ForgeDirection.NORTH;
		return dir;
	}

	@Override
	public EnumBlockRenderType getRenderType(IBlockState state) {
		return EnumBlockRenderType.MODEL;
	}

	@Override
	public boolean isOpaqueCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isBlockNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return !(world.getBlockState(pos.offset(side)).getBlock() instanceof BlockITServerColumn);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return meta >= META_CORE_MIN ? createColumnTileEntity() : null;
	}

	protected abstract TileEntity createColumnTileEntity();

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		super.addInformation(stack, world, tooltip, flag);

		String name = getRegistryName() == null ? "column" : getRegistryName().getPath();
		tooltip.add(TextFormatting.GRAY + I18nUtil.resolveKey("tooltip.ntm-it.server." + name + ".hint"));
		tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("tooltip.ntm-it.server." + name + ".faces"));
		tooltip.add(TextFormatting.DARK_GRAY + I18nUtil.resolveKey("tooltip.ntm-it.server.column"));
		tooltip.add(TextFormatting.DARK_AQUA + I18nUtil.resolveKey("tooltip.ntm-it.server.right_click"));
	}

	public static ForgeDirection facingOf(IBlockAccess world, BlockPos pos) {
		IBlockState state = world.getBlockState(pos);
		if (!(state.getBlock() instanceof BlockITServerColumn)) return ForgeDirection.NORTH;
		return facingOfMeta(state.getValue(META));
	}

	public static ForgeDirection facingOfMeta(int meta) {
		ForgeDirection dir = ForgeDirection.getOrientation(meta - BlockDummyable.offset);
		return dir == ForgeDirection.UNKNOWN || dir.offsetY != 0 ? ForgeDirection.NORTH : dir;
	}

	public static ForgeDirection[] sidesOf(ForgeDirection facing) {
		EnumFacing side = facing.toEnumFacing();
		return new ForgeDirection[] {ForgeDirection.getOrientation(side.rotateY()), ForgeDirection.getOrientation(side.rotateYCCW())};
	}

	public static BlockPos offset(BlockPos pos, ForgeDirection dir) {
		return pos.add(dir.offsetX, dir.offsetY, dir.offsetZ);
	}
}
