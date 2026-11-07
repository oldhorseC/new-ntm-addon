package com.ntmit.mixin;

import com.ntmit.lib.ModularTurbineRenderBounds;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE", remap = false)
public abstract class MixinMTCoreTE {

    public AxisAlignedBB getRenderBoundingBox() {
        return ModularTurbineRenderBounds.forCore((TileEntity) (Object) this);
    }

    public AxisAlignedBB func_145835_a() {
        return ModularTurbineRenderBounds.forCore((TileEntity) (Object) this);
    }
}
