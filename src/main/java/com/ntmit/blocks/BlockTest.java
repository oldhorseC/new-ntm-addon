package com.ntmit.blocks;

import com.hbm.blocks.BlockBase;
import com.hbm.items.tool.ItemRBMKTool;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.ModGuiHandler;
import com.ntmit.main.NTMITMod;
import com.ntmit.tileentity.TileEntityITConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn.ColumnType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockHorizontal;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;

public class BlockTest extends BlockBase {

	public static final PropertyDirection FACING = BlockHorizontal.FACING;

	public BlockTest() {
		super(Material.IRON);

		setRegistryName(new ResourceLocation(RefStrings.MODID, "test_block"));
		setTranslationKey(RefStrings.MODID + ".test_block");
		setCreativeTab(NTMITMod.tabITContent);
		setHardness(3F);
		setResistance(30F);

		setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH));
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new BlockStateContainer(this, FACING);
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		EnumFacing facing = EnumFacing.byIndex(meta);
		if (facing.getAxis() == EnumFacing.Axis.Y) facing = EnumFacing.NORTH;
		return getDefaultState().withProperty(FACING, facing);
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FACING).getIndex();
	}

	@Override
	public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		return getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileEntityITConsole();
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
		super.onBlockPlacedBy(world, pos, state, placer, stack);

		if (!world.isRemote) {
			TileEntity te = world.getTileEntity(pos);
			if (te instanceof TileEntityITConsole console) {
				console.setTarget(pos.getX(), pos.getY(), pos.getZ());
			}
		}
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, Block blockIn, BlockPos fromPos) {
		super.neighborChanged(state, world, pos, blockIn, fromPos);

		if (world.isRemote) return;

		int power = world.getRedstonePowerFromNeighbors(pos);
		if (power <= 0 || power > 15) return;

		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityITConsole console)) return;

		NBTTagCompound control = new NBTTagCompound();
		control.setDouble("level", (15D - power) / 14D);

		for (int j = 0; j < console.columns.length; j++) {
			if (console.columns[j] != null && console.columns[j].type == ColumnType.CONTROL)
				control.setInteger("sel_" + j, j);
		}

		console.receiveControl(control);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityITConsole console)) return false;

		ItemStack held = player.getHeldItem(hand);

		if (held.getItem() instanceof ItemRBMKTool && held.hasTagCompound()) {
			NBTTagCompound tag = held.getTagCompound();
			if (tag.hasKey("posX") && tag.hasKey("posY") && tag.hasKey("posZ")) {
				if (!world.isRemote) {
					console.setTarget(tag.getInteger("posX"), tag.getInteger("posY"), tag.getInteger("posZ"));
					player.sendMessage(new TextComponentTranslation(held.getItem().getTranslationKey() + ".set")
							.setStyle(new Style().setColor(TextFormatting.YELLOW)));
				}
				return true;
			}
		}

		if (player.isSneaking()) {
			if (!world.isRemote) {
				console.setTarget(pos.getX(), pos.getY(), pos.getZ());
				player.sendMessage(new TextComponentTranslation("chat." + RefStrings.MODID + ".console_bind",
						pos.getX(), pos.getY(), pos.getZ()).setStyle(new Style().setColor(TextFormatting.YELLOW)));
			}
			return true;
		}

		if (world.isRemote) {
			FMLNetworkHandler.openGui(player, NTMITMod.instance, ModGuiHandler.ID_CONSOLE, world, pos.getX(), pos.getY(), pos.getZ());
		}

		return true;
	}
}
