package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKRod;
import com.ntmit.lib.RbmkFeedback;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSurge;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityRBMKRod.class, priority = 1000)
public abstract class MixinTileEntityRBMKRod {

	@Unique private int ntmitMeltTicks;
	@Unique private int ntmitFlameTick;
	@Unique private boolean ntmitDigammaFired;

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKRod;meltdown()V"), cancellable = true, require = 0, remap = false)
	private void ntmit$interceptMelt(CallbackInfo ci) {
		ci.cancel();
	}

	@Redirect(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/util/ParticleUtil;spawnGasFlame(Lnet/minecraft/world/World;DDDDDD)V"), require = 0, remap = false)
	private void ntmit$interceptStockFlames(World world, double x, double y, double z, double mX, double mY, double mZ) {
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$overheatMelt(CallbackInfo ci) {
		TileEntityRBMKRod rod = (TileEntityRBMKRod) (Object) this;
		World world = rod.getWorld();
		if (world == null || world.isRemote) return;
		if (RbmkJumpHandler.isDigammaRod(rod)) {
			RbmkJumpHandler.markDigammaRod(world, rod.getPos());
			if (!this.ntmitDigammaFired && RbmkJumpHandler.digammaColumnOverheat(rod)) {
				this.ntmitDigammaFired = true;
				RbmkJumpHandler.digammaBurst(world, rod.getPos());
			}
			return;
		}
		if (!RbmkJumpHandler.rodAtMeltingPoint(rod) || !RbmkJumpHandler.steamNotYetAtBurst(rod)) {
			this.ntmitMeltTicks = 0;
			return;
		}
		int height = Math.max(RBMKDials.getColumnHeight(world), 1);
		this.ntmitMeltTicks++;
		if (this.ntmitFlameTick++ % RbmkJumpHandler.OVERHEAT_FLAME_INTERVAL == 0) RbmkJumpHandler.overheatFlames(world, rod.getPos(), height);
		if (this.ntmitMeltTicks >= RbmkJumpHandler.OVERHEAT_MELT_TICKS) {
			this.ntmitMeltTicks = 0;
			RbmkJumpHandler.moltenColumn(world, rod.getPos(), height);
		}
	}

	@ModifyArg(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/items/machine/ItemRBMKRod;burn(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;D)D", ordinal = 0), index = 2, require = 1, remap = false)
	private double ntmit$overheatReactivityCurve(double inFlux) {
		return RbmkJumpHandler.overheatReactivity((TileEntityRBMKRod) (Object) this, inFlux);
	}

	@ModifyArg(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/items/machine/ItemRBMKRod;burn(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;D)D", ordinal = 1), index = 2, require = 1, remap = false)
	private double ntmit$overheatReactivityType(double inFlux) {
		return RbmkJumpHandler.overheatReactivity((TileEntityRBMKRod) (Object) this, inFlux);
	}

	@ModifyArg(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/TileEntityRBMKRod;spreadFlux(DD)V"), index = 0, require = 1, remap = false)
	private double ntmit$surgeFlux(double original) {
		TileEntityRBMKRod rod = (TileEntityRBMKRod) (Object) this;
		return RbmkSurge.applyFlux(rod, original);
	}

	@ModifyArg(method = {"update()V", "func_73660_a()V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/items/machine/ItemRBMKRod;provideHeat(Lnet/minecraft/world/World;Lnet/minecraft/item/ItemStack;DD)D"), index = 3, require = 1, remap = false)
	private double ntmit$feedbackBurn(double original) {
		TileEntityRBMKRod rod = (TileEntityRBMKRod) (Object) this;
		World world = rod.getWorld();
		if (world == null || world.isRemote) return original;
		float boost = RbmkFeedback.burnBoost(world, rod.getPos());
		if (boost <= 0F) return original;
		return original + boost;
	}
}