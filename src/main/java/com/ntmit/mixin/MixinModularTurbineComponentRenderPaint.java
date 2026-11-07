package com.ntmit.mixin;

import com.ntmit.render.TurbinePaintTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.ModularTurbineComponentRender", remap = false)
public abstract class MixinModularTurbineComponentRenderPaint {

	@Redirect(method = "render", at = @At(value = "INVOKE",
			target = "Lcom/leafia/transformer/LeafiaGls;func_179124_c(FFF)V"))
	private static void ntmit$paintFlywheel(float red, float green, float blue) {
		TurbinePaintTextures.applyFlywheelColor(red, green, blue);
	}
}
