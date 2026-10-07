package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControl;
import com.ntmit.lib.RbmkSurge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.hbm.handler.neutron.RBMKNeutronHandler$RBMKNeutronStream", priority = 1000)
public abstract class MixinRBMKNeutronStream {

	@Redirect(method = "runStreamInteraction(Lnet/minecraft/world/World;Lcom/hbm/handler/neutron/NeutronNodeWorld$StreamWorld;)V", at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKControl;getMult()D"), require = 1, remap = false)
	private double ntmit$surgeTransmittance(TileEntityRBMKControl rod) {
		return RbmkSurge.applyTransmittance(rod, rod.getMult());
	}
}