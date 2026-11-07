package com.ntmit.mixin;

import com.ntmit.lib.TurbinePaintAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentTE", remap = false)
public abstract class MixinModularTurbineComponentTEPaint implements TurbinePaintAccess {

	@Unique
	private int ntmit$paintColor = TurbinePaintAccess.NO_PAINT;

	@Override
	public int ntmit$getPaintColor() {
		return this.ntmit$paintColor;
	}

	@Override
	public void ntmit$setPaintColor(int color) {
		this.ntmit$paintColor = color < 0 ? TurbinePaintAccess.NO_PAINT : color & 0xFFFFFF;
	}
}
