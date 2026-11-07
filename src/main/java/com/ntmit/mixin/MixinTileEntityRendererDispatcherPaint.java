package com.ntmit.mixin;

import com.ntmit.render.TurbinePaintTextures;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TileEntityRendererDispatcher.class)
public abstract class MixinTileEntityRendererDispatcherPaint {

	@Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("HEAD"))
	private void ntmit$beginPaint(TileEntity tile, double x, double y, double z, float partialTicks,
			int destroyStage, float alpha, CallbackInfo ci) {
		TurbinePaintTextures.begin(TurbinePaintTextures.colorOf(tile));
	}

	@Inject(method = "render(Lnet/minecraft/tileentity/TileEntity;DDDFIF)V", at = @At("RETURN"))
	private void ntmit$endPaint(TileEntity tile, double x, double y, double z, float partialTicks,
			int destroyStage, float alpha, CallbackInfo ci) {
		TurbinePaintTextures.end();
	}
}
