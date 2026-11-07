package com.ntmit.lib;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

import java.lang.reflect.Field;
import java.util.List;

public final class ModularTurbineRenderBounds {

    private static final String CORE_CLASS = "com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE";
    private static final double CORE_CHUNK_MARGIN = 8.0D;

    private static Field componentsField;
    private static boolean componentsFieldMissing;

    private ModularTurbineRenderBounds() {
    }

    public static AxisAlignedBB forCore(TileEntity core) {
        AxisAlignedBB box = blockBox(core);
        if (box == null) return null;
        box = box.grow(CORE_CHUNK_MARGIN);

        List<?> components = components(core);
        if (components == null || components.isEmpty()) return box;

        for (int i = 0; i < components.size(); i++) {
            Object entry;
            try {
                entry = components.get(i);
            } catch (IndexOutOfBoundsException exception) {
                break;
            }
            if (!(entry instanceof TileEntity)) continue;
            AxisAlignedBB componentBox = ((TileEntity) entry).getRenderBoundingBox();
            if (componentBox == null || componentBox == TileEntity.INFINITE_EXTENT_AABB) continue;
            box = union(box, componentBox);
        }
        return box;
    }

    private static AxisAlignedBB blockBox(TileEntity tile) {
        if (tile == null) return null;
        BlockPos pos = tile.getPos();
        if (pos == null) return null;
        double x = pos.getX();
        double y = pos.getY();
        double z = pos.getZ();
        return new AxisAlignedBB(x, y, z, x + 1.0D, y + 1.0D, z + 1.0D);
    }

    private static List<?> components(TileEntity core) {
        if (componentsFieldMissing || core == null || !CORE_CLASS.equals(core.getClass().getName())) return null;
        try {
            if (componentsField == null) {
                componentsField = core.getClass().getField("components");
            }
            Object value = componentsField.get(core);
            return value instanceof List ? (List<?>) value : null;
        } catch (Exception exception) {
            componentsFieldMissing = true;
            return null;
        }
    }

    private static AxisAlignedBB union(AxisAlignedBB a, AxisAlignedBB b) {
        return new AxisAlignedBB(
                Math.min(a.minX, b.minX),
                Math.min(a.minY, b.minY),
                Math.min(a.minZ, b.minZ),
                Math.max(a.maxX, b.maxX),
                Math.max(a.maxY, b.maxY),
                Math.max(a.maxZ, b.maxZ)
        );
    }
}
