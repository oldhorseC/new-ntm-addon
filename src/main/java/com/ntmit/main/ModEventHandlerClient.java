package com.ntmit.main;

import com.hbm.util.I18nUtil;
import com.ntmit.blocks.BlockITServerColumn;
import com.ntmit.blocks.ModBlocks;
import com.ntmit.tileentity.TileEntityITServerCompute;
import com.ntmit.tileentity.TileEntityITServerCore;
import com.ntmit.tileentity.TileEntityITServerHeatExchanger;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.init.Items;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SideOnly(Side.CLIENT)
public class ModEventHandlerClient {

    @SubscribeEvent
    public void onRenderWorldLast(RenderWorldLastEvent event) {
        com.ntmit.render.RbmkDebugRender.render(event);
    }

    @SubscribeEvent
    public void registerModels(ModelRegistryEvent event) {
        registerBlockModel(ModBlocks.test_block, 0);
        registerBlockModel(ModBlocks.test_multiblock, 0);
    }

    private void registerBlockModel(Block block, int meta) {
        Item item = Item.getItemFromBlock(block);
        if (item == Items.AIR) return;

        ModelLoader.setCustomModelResourceLocation(item, meta,
                new ModelResourceLocation(block.getRegistryName(), "inventory"));
    }

    @SubscribeEvent
    public void onRenderOverlayPre(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        World world = mc.world;
        RayTraceResult hit = mc.objectMouseOver;
        if (world == null || hit == null || hit.typeOfHit != RayTraceResult.Type.BLOCK) return;
        if (!world.isBlockLoaded(hit.getBlockPos())) return;
		if (!(world.getBlockState(hit.getBlockPos()).getBlock() instanceof BlockITServerColumn column)) return;

        BlockPos core = column.findCore(world, hit.getBlockPos());
        if (core == null) return;

        TileEntity tile = world.isBlockLoaded(core) ? world.getTileEntity(core) : null;
        NBTTagCompound diag = diagnosticDump(tile);
        if (diag == null) return;

        String[] keys = diag.getKeySet().toArray(new String[0]);
        Arrays.sort(keys);

        ScaledResolution resolution = event.getResolution();
        FontRenderer font = mc.fontRenderer;
        int pX = resolution.getScaledWidth() / 2 + 8;
        int pZ = resolution.getScaledHeight() / 2;

        String title = I18nUtil.resolveKey("hud.ntm-it.server.title");
        font.drawString(title, pX + 1, pZ - 19, 0x006000);
        font.drawString(title, pX, pZ - 20, 0x00FF00);

        String name = I18nUtil.resolveKey(column.getTranslationKey());
        font.drawString(name, pX + 1, pZ - 9, 0x606000);
        font.drawString(name, pX, pZ - 10, 0xFFFF00);

        int lineY = pZ;
        for (String key : keys) {
            if (DIAG_HIDDEN.contains(key)) continue;
            String label = I18nUtil.resolveKey("hud.ntm-it.server." + key);
            font.drawString(label + ": " + diag.getTag(key), pX, lineY, colourOf(tile, key));
            lineY += 10;
        }
    }

    private static final Set<String> DIAG_HIDDEN = new HashSet<>(Arrays.asList("x", "y", "z", "id"));

    private static NBTTagCompound diagnosticDump(TileEntity tile) {
        if (tile == null) return null;

        NBTTagCompound diag = new NBTTagCompound();
        if (tile instanceof TileEntityITServerCore core) {
            core.getDiagData(diag);
        } else if (tile instanceof TileEntityITServerCompute compute) {
            compute.getDiagData(diag);
        } else if (tile instanceof TileEntityITServerHeatExchanger exchanger) {
            exchanger.getDiagData(diag);
        } else {
            return null;
        }

        return diag;
    }

    private static int colourOf(TileEntity tile, String key) {
        if (tile instanceof TileEntityITServerHeatExchanger exchanger) {

            TileEntityITServerHeatExchanger master = exchanger.getMaster();
            if ("in_fluid".equals(key)) return fluidColour(master.in.getTankType().getTint());
            if ("out_fluid".equals(key)) return fluidColour(master.out.getTankType().getTint());
        }

        return 0xFFFFFF;
    }

    private static int fluidColour(int tint) {
        return tint == 0 ? 0xFFFFFF : tint;
    }
}
