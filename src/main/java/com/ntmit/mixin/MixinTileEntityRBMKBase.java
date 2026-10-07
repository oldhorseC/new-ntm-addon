package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.lib.RbmkFeedback;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSurge;

import net.minecraft.world.World;

import java.lang.reflect.Field;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityRBMKBase.class, priority = 1000)
public abstract class MixinTileEntityRBMKBase {

	@Unique private int ntmitReasimJumpCooldown;
	@Unique private int ntmitSteamBefore;
	@Unique private int ntmitBurstDelay;
	@Unique private static Field ntmitLeafiaDamageField;

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$capLeafiaDamage(CallbackInfo ci) {
		Field field = ntmitLeafiaDamageField();
		if (field == null) return;
		try {
			int damage = field.getInt(this);
			if (damage > RbmkJumpHandler.MAX_DAMAGE) field.setInt(this, RbmkJumpHandler.MAX_DAMAGE);
		} catch (Throwable throwable) {
		}
	}

	@Inject(method = "meltdown", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$gateMeltdown(CallbackInfo ci) {
		if (RbmkJumpHandler.allowMeltdown) return;
		RbmkJumpHandler.diag("meltdown vetoed at " + ((TileEntityRBMKBase) (Object) this).getPos());
		ci.cancel();
	}

	@Inject(method = "spawnDebris", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$gateDebris(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		if (NTMITWorldRules.entitySummonDisabled(te.getWorld())) ci.cancel();
	}

	@Unique private static Field ntmitLeafiaDamageField() {
		if (ntmitLeafiaDamageField == null) {
			try {
				ntmitLeafiaDamageField = TileEntityRBMKBase.class.getField(RbmkJumpHandler.DAMAGE_FIELD);
			} catch (Throwable throwable) {
				return null;
			}
		}
		return ntmitLeafiaDamageField;
	}

	@ModifyConstant(method = "boilWater", constant = @Constant(intValue = 16000), require = 1, remap = false)
	private int ntmit$reasimSteamCapacity(int original) {
		return RbmkJumpHandler.REASIM_STEAM_CAPACITY;
	}

	@Inject(method = "boilWater", at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$reasimSteamJump(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		this.ntmitSteamBefore = te.reasimSteam;
		boolean locked = this.ntmitBurstDelay > 0 && RbmkJumpHandler.reactorHasDigammaRod(world, te.getPos());
		if (this.ntmitBurstDelay > 0) RbmkJumpHandler.pumpReactor(world, te.getPos(), this.ntmitBurstDelay);
		if (te.reasimSteam >= RbmkJumpHandler.REASIM_BURST_THRESHOLD) {
			RbmkJumpHandler.pollControlRods(world, te.getPos());
			if (RbmkJumpHandler.az5ForcesBurst(world, te.getPos())) {
				RbmkJumpHandler.az5Consume();
				this.ntmitBurstDelay = 0;
				RbmkJumpHandler.reasimBurst(te, world);
			} else if (this.ntmitBurstDelay > 0 && RbmkJumpHandler.scramForcesBurst(world, te.getPos())) {
				RbmkJumpHandler.scramConsume();
				this.ntmitBurstDelay = 0;
				RbmkJumpHandler.reasimBurst(te, world);
			} else if (++this.ntmitBurstDelay >= RbmkJumpHandler.BURST_DELAY_TICKS) {
				this.ntmitBurstDelay = 0;
				RbmkJumpHandler.reasimBurst(te, world);
			}
		} else if (locked) {
			if (++this.ntmitBurstDelay >= RbmkJumpHandler.BURST_DELAY_TICKS) {
				this.ntmitBurstDelay = 0;
				RbmkJumpHandler.reasimBurst(te, world);
			}
		} else {
			this.ntmitBurstDelay = 0;
		}
		if (this.ntmitReasimJumpCooldown > 0) {
			this.ntmitReasimJumpCooldown--;
			return;
		}
		int cooldown = RbmkJumpHandler.reasimSteamJump(te);
		if (cooldown > 0) this.ntmitReasimJumpCooldown = cooldown;
	}

	@Inject(method = "boilWater", at = @At("RETURN"), require = 1, remap = false)
	private void ntmit$reasimFeedback(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		float boost = RbmkFeedback.reasimBoost(this.ntmitSteamBefore) + RbmkSurge.steamBoost(te);
		if (boost <= 0F) return;
		int produced = te.reasimSteam - this.ntmitSteamBefore;
		if (produced <= 0) return;
		int extra = Math.round(produced * boost);
		if (extra <= 0) return;
		te.reasimSteam = Math.min(RbmkJumpHandler.REASIM_STEAM_CAPACITY, te.reasimSteam + extra);
	}
}