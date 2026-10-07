package com.ntmit.mixin;

import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBoiler;
import com.ntmit.fluids.NTMITFluids;
import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.lib.RbmkFeedback;
import com.ntmit.lib.RbmkJumpHandler;
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
	@Shadow(remap = false) protected int output;

	@Unique private int ntmitSteamJumpCooldown;
	@Unique private int ntmitDiagTimer;
	@Unique private int ntmitBurstDelay;

	@Inject(method = "<init>", at = @At("TAIL"), require = 1)
	private void ntmit$steamCapacity(CallbackInfo ci) {
		this.steam = new FluidTankNTM(Fluids.STEAM, RbmkJumpHandler.BOILER_STEAM_CAPACITY).withOwner((TileEntityRBMKBoiler) (Object) this);
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$syncSteamType(CallbackInfo ci) {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null) return;
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
		if (++this.ntmitDiagTimer >= 40) {
			this.ntmitDiagTimer = 0;
			int diagDamage = RbmkJumpHandler.readDamage((TileEntityRBMKBoiler) (Object) this);
			int diagSpan = RbmkJumpHandler.STEAM_JUMP_TARGET_MAX - RbmkJumpHandler.STEAM_JUMP_TARGET_MIN;
			float diagRamp = diagSpan > 0 ? Math.min(1.0F, Math.max(0.0F, (float) (diagDamage - RbmkJumpHandler.STEAM_JUMP_TARGET_MIN) / (float) diagSpan)) : 0.0F;
			RbmkJumpHandler.diag("boiler " + ((TileEntityRBMKBoiler) (Object) this).getPos() + " fill=" + this.steam.getFill() + " damage=" + diagDamage + " ramp=" + diagRamp + " cooldown=" + this.ntmitSteamJumpCooldown);
		}
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world != null && !world.isRemote) {
			RbmkJumpHandler.pollControlRods(world, boiler.getPos());
			boolean locked = this.ntmitBurstDelay > 0 && RbmkJumpHandler.reactorHasDigammaRod(world, boiler.getPos());
			if (this.ntmitBurstDelay > 0) RbmkJumpHandler.pumpReactor(world, boiler.getPos(), this.ntmitBurstDelay);
			if (this.steam.getFill() >= RbmkJumpHandler.burstThreshold(this.steam.getTankType())) {
				if (RbmkJumpHandler.az5ForcesBurst(world, boiler.getPos())) {
					RbmkJumpHandler.az5Consume();
					this.ntmitBurstDelay = 0;
					RbmkJumpHandler.steamBurst(boiler);
				} else if (this.ntmitBurstDelay > 0 && RbmkJumpHandler.scramForcesBurst(world, boiler.getPos())) {
					RbmkJumpHandler.scramConsume();
					this.ntmitBurstDelay = 0;
					RbmkJumpHandler.steamBurst(boiler);
				} else if (++this.ntmitBurstDelay >= RbmkJumpHandler.BURST_DELAY_TICKS) {
					this.ntmitBurstDelay = 0;
					RbmkJumpHandler.steamBurst(boiler);
				}
			} else if (locked) {
				if (++this.ntmitBurstDelay >= RbmkJumpHandler.BURST_DELAY_TICKS) {
					this.ntmitBurstDelay = 0;
					RbmkJumpHandler.steamBurst(boiler);
				}
			} else {
				this.ntmitBurstDelay = 0;
			}
		}
		if (this.ntmitSteamJumpCooldown > 0) this.ntmitSteamJumpCooldown--;
		if (this.ntmitSteamJumpCooldown > 0) return;
		int cooldown = RbmkJumpHandler.steamFull((TileEntityRBMKBoiler) (Object) this, this.steam);
		if (cooldown > 0) this.ntmitSteamJumpCooldown = cooldown;
	}

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$steamFeedback(CallbackInfo ci) {
		TileEntityRBMKBoiler boiler = (TileEntityRBMKBoiler) (Object) this;
		World world = boiler.getWorld();
		if (world == null || world.isRemote) return;
		float boost = RbmkFeedback.tankBoost(this.steam.getFill()) + RbmkSurge.steamBoost(boiler);
		if (boost <= 0F) return;
		int extra = Math.round(this.output * boost);
		if (extra <= 0) return;
		this.steam.setFill(Math.min(this.steam.getMaxFill(), this.steam.getFill() + extra));
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
