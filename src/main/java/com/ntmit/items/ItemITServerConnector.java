package com.ntmit.items;

import com.hbm.blocks.BlockDummyable;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.util.I18nUtil;
import com.ntmit.lib.RefStrings;
import com.ntmit.main.NTMITMod;
import com.ntmit.server.data.IServerSource;
import com.ntmit.server.data.ServerSources;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemITServerConnector extends Item {

	public static final String NBT_DIM = "dim";
	public static final String NBT_X = "posX";
	public static final String NBT_Y = "posY";
	public static final String NBT_Z = "posZ";
	public static final String NBT_DEVICE_TYPE = "deviceType";
	public static final String NBT_FIELDS = "fields";

	public ItemITServerConnector(String name) {
		this.setTranslationKey("item." + RefStrings.MODID + "." + name);
		this.setRegistryName(name);
		this.setMaxStackSize(1);
		this.setCreativeTab(NTMITMod.tabITContent);
		ModItems.ALL_ITEMS.add(this);
	}

	@Override
	public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos bpos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
		if (!player.isSneaking()) return EnumActionResult.PASS;
		if (world.isRemote) return EnumActionResult.SUCCESS;

		this.bind(player.getHeldItem(hand), player, world, bpos);
		return EnumActionResult.SUCCESS;
	}

	private void bind(ItemStack stack, EntityPlayer player, World world, BlockPos bpos) {
		TileEntity tile = world.getTileEntity(bpos);

		BlockPos target = bpos;
		if (world.getBlockState(bpos).getBlock() instanceof BlockDummyable dummyable) {
			int[] core = dummyable.findCore(world, bpos.getX(), bpos.getY(), bpos.getZ());
			if (core != null) {
				target = new BlockPos(core[0], core[1], core[2]);
				tile = world.getTileEntity(target);
			}
		}

		IServerSource source = ServerSources.of(tile);
		if (source == null) {
			say(player, "nodevice");
			return;
		}

		Map<String, DataValue> fields = source.listFields(world);
		if (fields.isEmpty()) {
			say(player, "nofields");
			return;
		}

		NBTTagCompound nbt = stack.hasTagCompound() ? stack.getTagCompound() : new NBTTagCompound();
		nbt.setInteger(NBT_DIM, world.provider.getDimension());
		nbt.setInteger(NBT_X, target.getX());
		nbt.setInteger(NBT_Y, target.getY());
		nbt.setInteger(NBT_Z, target.getZ());
		nbt.setString(NBT_DEVICE_TYPE, deviceTypeOf(world, target, tile));

		NBTTagList list = new NBTTagList();
		for (String name : fields.keySet()) list.appendTag(new NBTTagString(name));
		nbt.setTag(NBT_FIELDS, list);

		stack.setTagCompound(nbt);
		say(player, "bound", target.getX(), target.getY(), target.getZ(), fields.size());
	}

	private static String deviceTypeOf(World world, BlockPos pos, TileEntity tile) {
		ResourceLocation block = world.getBlockState(pos).getBlock().getRegistryName();
		return (block == null ? "?" : block.toString()) + "|" + (tile == null ? "?" : tile.getClass().getName());
	}

	public static boolean isBound(ItemStack stack) {
		return stack.hasTagCompound() && stack.getTagCompound().hasKey(NBT_X);
	}

	public static int getDimension(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getInteger(NBT_DIM) : 0;
	}

	public static BlockPos getPos(ItemStack stack) {
		if (!stack.hasTagCompound()) return BlockPos.ORIGIN;
		NBTTagCompound nbt = stack.getTagCompound();
		return new BlockPos(nbt.getInteger(NBT_X), nbt.getInteger(NBT_Y), nbt.getInteger(NBT_Z));
	}

	public static String getDeviceType(ItemStack stack) {
		return stack.hasTagCompound() ? stack.getTagCompound().getString(NBT_DEVICE_TYPE) : "";
	}

	public static List<String> getFields(ItemStack stack) {
		List<String> names = new ArrayList<>();
		if (!stack.hasTagCompound()) return names;
		NBTTagList list = stack.getTagCompound().getTagList(NBT_FIELDS, 8);
		for (int i = 0; i < list.tagCount(); i++) names.add(list.getStringTagAt(i));
		return names;
	}

	@Override
	public void addInformation(ItemStack stack, World world, List<String> tooltip, ITooltipFlag flag) {
		for (String line : I18nUtil.resolveKeyArray("item.ntm-it.server_connector.desc")) tooltip.add(TextFormatting.YELLOW + line);
		if (!isBound(stack)) return;

		BlockPos pos = getPos(stack);
		tooltip.add(TextFormatting.GRAY + getDeviceType(stack));
		tooltip.add(TextFormatting.GRAY + "D" + getDimension(stack) + " " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()
				+ " (" + getFields(stack).size() + ")");
	}

	private static void say(EntityPlayer player, String key, Object... args) {
		player.sendMessage(new TextComponentTranslation("item.ntm-it.server_connector." + key, args).setStyle(new Style().setColor(TextFormatting.YELLOW)));
	}
}
