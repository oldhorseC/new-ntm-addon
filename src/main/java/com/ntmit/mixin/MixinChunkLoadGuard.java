package com.ntmit.mixin;

import com.ntmit.lib.ChunkLoadGuard;

import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Chunk.class)
public abstract class MixinChunkLoadGuard {

	@Inject(method = "onLoad", at = @At("HEAD"), remap = false)
	private void ntmit$enterLoad(CallbackInfo ci) {
		ChunkLoadGuard.enter();
	}

	@Inject(method = "onLoad", at = @At("RETURN"), remap = false)
	private void ntmit$exitLoad(CallbackInfo ci) {
		ChunkLoadGuard.exit();
	}
}
