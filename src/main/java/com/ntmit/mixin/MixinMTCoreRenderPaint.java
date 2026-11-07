package com.ntmit.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.ntmit.render.TurbinePaintTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.core.MTCoreRender", remap = false)
public abstract class MixinMTCoreRenderPaint {

	@Inject(method = "render", at = @At("HEAD"))
	private void ntmit$beginPaint(CallbackInfo ci, @Local(argsOnly = true, index = 1) Object tile) {
		TurbinePaintTextures.begin(TurbinePaintTextures.colorOf(tile));
	}

	@Inject(method = "render", at = @At("RETURN"))
	private void ntmit$endPaint(CallbackInfo ci) {
		TurbinePaintTextures.end();
	}
}
