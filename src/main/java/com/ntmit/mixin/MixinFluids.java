package com.ntmit.mixin;

import com.hbm.inventory.fluid.Fluids;
import com.ntmit.fluids.NTMITFluids;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Fluids.class)
public class MixinFluids {

	@Inject(method = "init", at = @At("RETURN"), require = 1, remap = false)
	private static void ntmit$registerMixtures(CallbackInfo ci) {
		NTMITFluids.register();
	}
}