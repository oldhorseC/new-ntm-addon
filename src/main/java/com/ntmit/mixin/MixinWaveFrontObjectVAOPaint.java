package com.ntmit.mixin;

import com.ntmit.render.TurbinePaintTextures;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.hbm.render.loader.WaveFrontObjectVAO", remap = false)
public abstract class MixinWaveFrontObjectVAOPaint {

	@Inject(method = "renderPart", at = @At("HEAD"))
	private void ntmit$preparePart(String part, CallbackInfo ci) {
		TurbinePaintTextures.preparePart(part);
	}

	@Inject(method = "renderAll", at = @At("HEAD"))
	private void ntmit$prepareAll(CallbackInfo ci) {
		TurbinePaintTextures.prepareAll();
	}
}
