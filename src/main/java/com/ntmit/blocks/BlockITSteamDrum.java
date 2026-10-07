package com.ntmit.blocks;

import com.hbm.blocks.BlockBase;
import com.hbm.blocks.ILookOverlay;
import com.hbm.blocks.network.FluidDuctBox;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.items.IDynamicModels;
import com.hbm.lib.ForgeDirection;
import com.hbm.lib.Library;
import com.hbm.render.model.DuctBakedModel;
import com.hbm.util.I18nUtil;
import com.ntmit.fluids.NTMITFluids;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.NTMITMod;
import com.ntmit.tileentity.TileEntityITSteamDrum;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.renderer.block.statemap.StateMapperBase;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelBakeEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.property.ExtendedBlockState;
import net.minecraftforge.common.property.IExtendedBlockState;
import net.minecraftforge.common.property.IUnlistedProperty;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

import com.hbm.items.machine.IItemFluidIdentifier;
import com.ntmit.render.ScaledBakedModel;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

public class BlockITSteamDrum extends BlockBase implements IDynamicModels, ILookOverlay {

	public static final int SIZE_LEVEL_META = 0;
	public static final float DRUM_SCALE = 1.16F;

	public BlockITSteamDrum() {
		super(Material.IRON);

		setRegistryName(new ResourceLocation(RefStrings.MODID, "steam_drum"));
		setTranslationKey(RefStrings.MODID + ".steam_drum");
		setCreativeTab(NTMITMod.tabITContent);
		setHardness(4F);
		setResistance(40F);

		setDefaultState(this.blockState.getBaseState().withProperty(FluidDuctBox.META, Integer.valueOf(SIZE_LEVEL_META)));

		IDynamicModels.INSTANCES.add(this);
	}

	@Override
	protected BlockStateContainer createBlockState() {
		return new ExtendedBlockState(this, new IProperty[] {FluidDuctBox.META}, new IUnlistedProperty[] {
			FluidDuctBox.CONN_NORTH, FluidDuctBox.CONN_SOUTH, FluidDuctBox.CONN_WEST, FluidDuctBox.CONN_EAST, FluidDuctBox.CONN_UP, FluidDuctBox.CONN_DOWN
		});
	}

	@Override
	public IBlockState getStateFromMeta(int meta) {
		return getDefaultState().withProperty(FluidDuctBox.META, Integer.valueOf(meta % 15));
	}

	@Override
	public int getMetaFromState(IBlockState state) {
		return state.getValue(FluidDuctBox.META).intValue();
	}

	@Override
	public IBlockState getExtendedState(IBlockState state, IBlockAccess world, BlockPos pos) {
		int mask = this.resolveMask(world, pos);
		IExtendedBlockState ext = (IExtendedBlockState) state;
		ext = ext.withProperty(FluidDuctBox.CONN_NORTH, Boolean.valueOf((mask & 1 << EnumFacing.NORTH.getIndex()) != 0));
		ext = ext.withProperty(FluidDuctBox.CONN_SOUTH, Boolean.valueOf((mask & 1 << EnumFacing.SOUTH.getIndex()) != 0));
		ext = ext.withProperty(FluidDuctBox.CONN_WEST, Boolean.valueOf((mask & 1 << EnumFacing.WEST.getIndex()) != 0));
		ext = ext.withProperty(FluidDuctBox.CONN_EAST, Boolean.valueOf((mask & 1 << EnumFacing.EAST.getIndex()) != 0));
		ext = ext.withProperty(FluidDuctBox.CONN_UP, Boolean.valueOf((mask & 1 << EnumFacing.UP.getIndex()) != 0));
		ext = ext.withProperty(FluidDuctBox.CONN_DOWN, Boolean.valueOf((mask & 1 << EnumFacing.DOWN.getIndex()) != 0));
		return ext;
	}

	protected int resolveMask(IBlockAccess world, BlockPos pos) {
		int mask = 0;
		for (EnumFacing dir : EnumFacing.VALUES) {
			BlockPos neighbor = pos.offset(dir);
			ForgeDirection side = ForgeDirection.getOrientation(dir.getIndex());
			for (FluidType type : NTMITFluids.connectTypes()) {
				if (type == null) continue;
				if (!Library.canConnectFluid(world, neighbor, side, type)) continue;
				mask |= 1 << dir.getIndex();
				break;
			}
		}
		return mask & 0x3F;
	}

	@Override
	public void neighborChanged(IBlockState state, World world, BlockPos pos, net.minecraft.block.Block blockIn, BlockPos fromPos) {
		super.neighborChanged(state, world, pos, blockIn, fromPos);
		if (world != null && !world.isRemote) world.markBlockRangeForRenderUpdate(pos, pos);
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
	public boolean isFullCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean isBlockNormalCube(IBlockState state) {
		return false;
	}

	@Override
	public boolean shouldSideBeRendered(IBlockState state, IBlockAccess world, BlockPos pos, EnumFacing side) {
		return true;
	}

	private float[] limits(int meta) {
		float lower = 0.125F;
		float upper = 0.875F;
		float jLower = 0.0625F;
		float jUpper = 0.9375F;
		for (int i = 2; i < 13; i += 3) {
			if (meta <= i) continue;
			lower += 0.0625F;
			upper -= 0.0625F;
			jLower += 0.0625F;
			jUpper -= 0.0625F;
		}
		lower = 0.5F - (0.5F - lower) * DRUM_SCALE;
		upper = 0.5F + (upper - 0.5F) * DRUM_SCALE;
		return new float[] {lower, upper, jLower, jUpper};
	}

	private boolean[] connections(IBlockAccess world, BlockPos pos) {
		int connMask = this.resolveMask(world, pos);
		return new boolean[] {
			(connMask & 1 << EnumFacing.WEST.getIndex()) != 0,
			(connMask & 1 << EnumFacing.EAST.getIndex()) != 0,
			(connMask & 1 << EnumFacing.DOWN.getIndex()) != 0,
			(connMask & 1 << EnumFacing.UP.getIndex()) != 0,
			(connMask & 1 << EnumFacing.NORTH.getIndex()) != 0,
			(connMask & 1 << EnumFacing.SOUTH.getIndex()) != 0
		};
	}

	@Override
	public AxisAlignedBB getBoundingBox(IBlockState state, IBlockAccess source, BlockPos pos) {
		boolean[] conn = this.connections(source, pos);
		float[] lim = this.limits(this.getMetaFromState(state));
		float lower = lim[0];
		float upper = lim[1];
		float jLower = lim[2];
		float jUpper = lim[3];
		int mask = (conn[1] ? 32 : 0) + (conn[0] ? 16 : 0) + (conn[3] ? 8 : 0) + (conn[2] ? 4 : 0) + (conn[5] ? 2 : 0) + (conn[4] ? 1 : 0);
		if (mask == 0) return new AxisAlignedBB(jLower, jLower, jLower, jUpper, jUpper, jUpper);
		if (mask == 32 || mask == 16 || mask == 48) return new AxisAlignedBB(0.0D, lower, lower, 1.0D, upper, upper);
		if (mask == 8 || mask == 4 || mask == 12) return new AxisAlignedBB(lower, 0.0D, lower, upper, 1.0D, upper);
		if (mask == 2 || mask == 1 || mask == 3) return new AxisAlignedBB(lower, lower, 0.0D, upper, upper, 1.0D);
		return new AxisAlignedBB(conn[0] ? 0.0D : (double) lower, conn[2] ? 0.0D : (double) lower, conn[4] ? 0.0D : (double) lower, conn[1] ? 1.0D : (double) upper, conn[3] ? 1.0D : (double) upper, conn[5] ? 1.0D : (double) upper);
	}

	@Override
	public void addCollisionBoxToList(IBlockState state, World world, BlockPos pos, AxisAlignedBB entityBox, List<AxisAlignedBB> collidingBoxes, Entity entityIn, boolean isActualState) {
		boolean[] conn = this.connections(world, pos);
		float[] lim = this.limits(this.getMetaFromState(state));
		float lower = lim[0];
		float upper = lim[1];
		float jLower = lim[2];
		float jUpper = lim[3];
		int mask = (conn[1] ? 32 : 0) + (conn[0] ? 16 : 0) + (conn[3] ? 8 : 0) + (conn[2] ? 4 : 0) + (conn[5] ? 2 : 0) + (conn[4] ? 1 : 0);
		int count = 0;
		for (boolean c : conn) if (c) count++;
		if (mask == 0) {
			addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(jLower, jLower, jLower, jUpper, jUpper, jUpper));
			return;
		}
		if (mask == 32 || mask == 16 || mask == 48) {
			addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(0.0D, lower, lower, 1.0D, upper, upper));
			return;
		}
		if (mask == 8 || mask == 4 || mask == 12) {
			addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, 0.0D, lower, upper, 1.0D, upper));
			return;
		}
		if (mask == 2 || mask == 1 || mask == 3) {
			addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, lower, 0.0D, upper, upper, 1.0D));
			return;
		}
		if (count != 2) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(jLower, jLower, jLower, jUpper, jUpper, jUpper));
		if (conn[1]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(upper, lower, lower, 1.0D, upper, upper));
		if (conn[0]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(0.0D, lower, lower, lower, upper, upper));
		if (conn[3]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, upper, lower, upper, 1.0D, upper));
		if (conn[2]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, 0.0D, lower, upper, lower, upper));
		if (conn[5]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, lower, upper, upper, upper, 1.0D));
		if (conn[4]) addCollisionBoxToList(pos, entityBox, collidingBoxes, new AxisAlignedBB(lower, lower, 0.0D, upper, upper, lower));
	}

	@Override
	public boolean hasTileEntity(IBlockState state) {
		return true;
	}

	@Override
	public TileEntity createTileEntity(World world, IBlockState state) {
		return new TileEntityITSteamDrum();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerSprite(TextureMap map) {
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerModel() {
		for (int meta = 0; meta < 15; meta++) {
			ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), meta, new ModelResourceLocation(this.getRegistryName(), "meta=" + meta));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void bakeModel(ModelBakeEvent event) {
		for (int meta = 0; meta < 15; meta++) {
			DuctBakedModel baked = new DuctBakedModel(meta, false);
			ModelResourceLocation loc = new ModelResourceLocation(this.getRegistryName(), "meta=" + meta);
			event.getModelRegistry().putObject(loc, new ScaledBakedModel(baked, DRUM_SCALE));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public StateMapperBase getStateMapper(final ResourceLocation loc) {
		return new StateMapperBase() {
			@Override
			protected ModelResourceLocation getModelResourceLocation(IBlockState state) {
				return new ModelResourceLocation(loc, "meta=" + state.getValue(FluidDuctBox.META));
			}
		};
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void printHook(RenderGameOverlayEvent.Pre event, World world, BlockPos pos) {
		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityITSteamDrum drum)) return;

		ArrayList<String> text = new ArrayList<>();
		text.add(TextFormatting.RED + "<- " + TextFormatting.RESET + drum.in.getTankType().getLocalizedName() + ": " + drum.in.getFill() + "/" + drum.in.getMaxFill() + "mB");
		text.add(TextFormatting.GREEN + "-> " + TextFormatting.RESET + drum.outSteam.getTankType().getLocalizedName() + ": " + drum.outSteam.getFill() + "/" + drum.outSteam.getMaxFill() + "mB");
		text.add(TextFormatting.GREEN + "-> " + TextFormatting.RESET + drum.outWater.getTankType().getLocalizedName() + ": " + drum.outWater.getFill() + "/" + drum.outWater.getMaxFill() + "mB");
		ILookOverlay.printGeneric(event, I18nUtil.resolveKey(this.getTranslationKey() + ".name"), 0xFFFF00, 0x404000, text);
	}

	@Override
	public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		ItemStack held = player.getHeldItem(hand);
		if (held.isEmpty() || !(held.getItem() instanceof IItemFluidIdentifier)) return false;

		TileEntity te = world.getTileEntity(pos);
		if (!(te instanceof TileEntityITSteamDrum drum)) return false;
		if (world.isRemote) return true;

		FluidType type = ((IItemFluidIdentifier) held.getItem()).getType(world, pos.getX(), pos.getY(), pos.getZ(), held);
		return drum.setInputType(type);
	}
}