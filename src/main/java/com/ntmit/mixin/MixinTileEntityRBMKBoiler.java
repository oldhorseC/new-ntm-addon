package com.ntmit.mixin;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.ntmit.fluids.NTMITFluids;
import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.lib.RbmkFeedback;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSteamCurve;
import com.ntmit.lib.RbmkSurge;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntityRBMKBoiler.class)
public abstract class MixinTileEntityRBMKBoiler {

	@Shadow(remap = false) public FluidTankNTM steam;
	@Shadow(remap = false) protected int consumption;
	@Shadow(remap = false) protected int output;

	@Unique private int ntmitSteamJumpCooldown;
	@Unique private int ntmitDiagTimer;
	@Unique private boolean ntmitOverheatBoost;

	@Inject(method = "<init>", at = @At("TAIL"), require = 1)
	private void ntmit$steamCapacity(CallbackInfo ci) {
		this.steam = new FluidTankNTM(Fluids.STEAM, RbmkJumpHandler.BOILER_STEAM_CAPACITY).withOwner((TileEntityRBMKBoiler) (Object) this);
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$syncSteamType(CallbackInfo ci) {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null) return;
		this.ntmitOverheatBoost = RbmkJumpHandler.hasOverheatingRodNear(world, boiler.getPos());
		if (this.steam.getMaxFill() != RbmkJumpHandler.BOILER_STEAM_CAPACITY) this.steam.changeTankSize(RbmkJumpHandler.BOILER_STEAM_CAPACITY);
		if (world.isRemote) return;
		if (this.steam.getFill() > 0) return;
		if (NTMITWorldRules.wetSteam(world)) {
			if (NTMITFluids.WETSTEAM != null && this.steam.getTankType() != NTMITFluids.WETSTEAM) this.steam.setTankType(NTMITFluids.WETSTEAM);
		} else if (NTMITFluids.isMixture(this.steam.getTankType())) {
			this.steam.setTankType(Fluids.STEAM);
		}
	}

	@Inject(method = "cyceCompressor", at = @At("RETURN"), require = 1, remap = false)
	private void ntmit$compressMixture(CallbackInfo ci) {
		if (!NTMITWorldRules.wetSteam(((TileEntityRBMKBoiler) (Object) this).getWorld())) return;
		FluidType next = NTMITFluids.compressed(this.steam.getTankType());
		if (next == null) return;
		this.steam.setTankType(next);
		this.steam.setFill(next == NTMITFluids.WETSTEAM ? Math.min(this.steam.getFill() * 1000, this.steam.getMaxFill()) : this.steam.getFill() / 10);
	}

	@Inject(method = "getHeatFromSteam", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
	private static void ntmit$heatOfMixture(FluidType type, CallbackInfoReturnable<Double> cir) {
		if (NTMITFluids.isMixture(type)) cir.setReturnValue(TileEntityRBMKBoiler.getHeatFromSteam(NTMITFluids.dryOf(type)));
	}

	@Inject(method = "getFactorFromSteam", at = @At("RETURN"), cancellable = true, require = 1, remap = false)
	private static void ntmit$factorOfMixture(FluidType type, CallbackInfoReturnable<Double> cir) {
		if (NTMITFluids.isMixture(type)) cir.setReturnValue(TileEntityRBMKBoiler.getFactorFromSteam(NTMITFluids.dryOf(type)) * NTMITFluids.WATER_BONUS);
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$steamFullJump(CallbackInfo ci) {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world != null && !world.isRemote) {
			RbmkJumpHandler.tickCJumps(world, boiler.getPos());
			RbmkJumpHandler.tickReactorBurst(world, boiler.getPos());
			int burstElapsed = RbmkJumpHandler.reactorBurstElapsed(world, boiler.getPos());
			if (++this.ntmitDiagTimer >= 40) {
				this.ntmitDiagTimer = 0;
				double fraction = RbmkSteamCurve.normalFraction(this.steam.getFill(), this.steam.getTankType());
			}
			if (burstElapsed > 0) {
				RbmkJumpHandler.pumpReactor(world, boiler.getPos(), burstElapsed);
			}
		}
		if (this.ntmitSteamJumpCooldown > 0) this.ntmitSteamJumpCooldown--;
		if (this.ntmitSteamJumpCooldown > 0) return;
		int cooldown = RbmkJumpHandler.steamFull(boiler, this.steam);
		if (cooldown > 0) this.ntmitSteamJumpCooldown = cooldown;
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$steamFeedback(CallbackInfo ci) {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return;
		float boost = RbmkFeedback.tankBoost(this.steam.getFill(), this.steam.getTankType()) + RbmkSurge.steamBoost(boiler);
		if (boost <= 0F) return;
		int extra = Math.round(this.output * boost);
		if (extra <= 0) return;
		this.steam.setFill(Math.min(this.steam.getMaxFill(), this.steam.getFill() + extra));
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$overheatWaterBoost(CallbackInfo ci) {
		if (!this.ntmitOverheatBoost) return;
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return;
		int extraWater = Math.min(boiler.feed.getFill(), this.consumption);
		if (extraWater <= 0) return;
		int extraSteam = Math.min(this.steam.getMaxFill() - this.steam.getFill(), this.output * 2);
		boiler.feed.setFill(boiler.feed.getFill() - extraWater);
		this.steam.setFill(this.steam.getFill() + extraSteam);
		this.consumption += extraWater;
		this.output += extraSteam;
		boiler.heat -= extraWater * RbmkJumpHandler.boilerHeatConsumption(world);
	}

	@Inject(method = "getConsoleData", at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$displayConsoleData(CallbackInfoReturnable<RBMKColumn> cir) {
		RBMKColumn column = cir.getReturnValue();
		if (!(column instanceof RBMKColumn.BoilerColumn)) return;
		RBMKColumn.BoilerColumn data = (RBMKColumn.BoilerColumn) column;
		data.maxSteam = RbmkJumpHandler.BOILER_DISPLAY_CAPACITY;
		data.steam = Math.min(data.steam, RbmkJumpHandler.BOILER_DISPLAY_CAPACITY);
	}

	@Redirect(method = {"update()V", "func_73660_a()V"}, at = @At(value = "FIELD", target = "Lcom/hbm/particle/helper/HbmEffectNT;RBMKSteam:Lcom/hbm/particle/helper/HbmEffectNT;"), require = 0, remap = false)
	private HbmEffectNT ntmit$redVentSteam() {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return HbmEffectNT.RBMKSteam;
		if (!RbmkJumpHandler.reactorHasDigammaRod(world, boiler.getPos())) return HbmEffectNT.RBMKSteam;
		return HbmEffectNT.Tower;
	}
}
