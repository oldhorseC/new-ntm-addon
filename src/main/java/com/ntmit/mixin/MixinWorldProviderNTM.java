package com.ntmit.mixin;

import com.hbm.world.WorldProviderNTM;
import com.ntmit.lib.SkyTint;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WorldProviderNTM.class)
public abstract class MixinWorldProviderNTM {

	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
	private void ntmit$skyTint(Entity camera, float partialTicks, CallbackInfoReturnable<Vec3d> cir) {
		Vec3d tinted = SkyTint.apply(cir.getReturnValue());
		if (tinted != null) cir.setReturnValue(tinted);
	}
}