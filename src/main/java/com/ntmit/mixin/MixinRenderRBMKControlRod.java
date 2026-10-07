package com.ntmit.mixin;

import com.hbm.render.tileentity.RenderRBMKControlRod;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKControl;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderRBMKControlRod.class, priority = 1000)
public abstract class MixinRenderRBMKControlRod {

	@Inject(method = "render(Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKControl;DDDFIF)V", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
	private void ntmit$skipLiftedRod(TileEntityRBMKControl te, double x, double y, double z, float partialTicks, int destroyStage, float alpha, CallbackInfo ci) {
		if (te == null) return;
		World world = te.getWorld();
		if (world == null) return;
		int columnHeight = Math.max(RBMKDials.getColumnHeight(world), 1);
		BlockPos top = te.getPos().up(columnHeight);
		if (!world.isBlockLoaded(top)) return;
		if (world.isAirBlock(top)) ci.cancel();
	}
}
