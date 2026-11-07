package com.ntmit.mixin;

import com.ntmit.render.TurbinePaintTextures;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureManager.class)
public abstract class MixinTextureManagerPaint {

	@Inject(method = "bindTexture", at = @At("HEAD"))
	private void ntmit$recordTurbineBase(ResourceLocation texture, CallbackInfo ci) {
		TurbinePaintTextures.recordBase(texture);
	}
}
