package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;

import net.minecraft.client.particle.Particle;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.reactors.rbmk.effects.ParticleJumpingRBMK", priority = 1000)
public abstract class MixinParticleJumpingRBMK {

	@Shadow(remap = false) private TileEntityRBMKBase rbmk;

	private boolean ntmit$dead() {
		try {
			return this.rbmk == null || this.rbmk.isInvalid();
		} catch (Throwable throwable) {
			return true;
		}
	}

	@Inject(method = "onUpdate", at = @At("HEAD"), cancellable = true)
	private void ntmit$guardUpdate(CallbackInfo ci) {
		if (!this.ntmit$dead()) return;
		try {
			((Particle) (Object) this).setExpired();
		} catch (Throwable throwable) {
		}
		ci.cancel();
	}

	@Inject(method = "renderParticle", at = @At("HEAD"), cancellable = true)
	private void ntmit$guardRender(CallbackInfo ci) {
		if (this.ntmit$dead()) ci.cancel();
	}
}