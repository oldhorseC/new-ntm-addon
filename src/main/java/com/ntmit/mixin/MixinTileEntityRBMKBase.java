package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.RBMKDials;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.lib.RbmkFeedback;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSurge;

import net.minecraft.util.math.MathHelper;
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
	@Unique private static Field ntmitLeafiaDamageField;

	@Inject(method = {"update()V", "func_73660_a()V"}, at = @At("TAIL"), require = 1, remap = false)
	private void ntmit$capLeafiaDamage(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world != null && !world.isRemote) {
			RbmkJumpHandler.tickCJumps(world, te.getPos());
			RbmkJumpHandler.tickReactorBurst(world, te.getPos());
		}
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

	@Inject(method = "boilWater", at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$boostReasimOverheat(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		if (!RbmkJumpHandler.reasimOverheatBoost(te)) return;
		if (te.heat < 100.0D) return;
		double heatConsumption = RbmkJumpHandler.boilerHeatConsumption(world);
		if (heatConsumption <= 0.0D) return;
		double availableHeat = (te.heat - 100.0D) / heatConsumption;
		double availableWater = te.reasimWater;
		double availableSpace = RbmkJumpHandler.REASIM_STEAM_CAPACITY - te.reasimSteam;
		double speed = MathHelper.clamp(RbmkJumpHandler.reasimBoilerSpeed(world), 0.0D, 1.0D);
		double normal = Math.floor(Math.min(availableHeat, Math.min(availableWater, availableSpace)) * speed);
		if (normal <= 0.0D) {
			ci.cancel();
			return;
		}
		int waterUsed = (int) Math.floor(Math.min(availableWater, normal * 2.0D));
		int steamProduced = (int) Math.floor(Math.min(availableSpace, normal * 3.0D));
		te.reasimWater -= waterUsed;
		te.reasimSteam += steamProduced;
		te.heat -= waterUsed * heatConsumption;
		ci.cancel();
	}

	@Inject(method = "boilWater", at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$reasimSteamJump(CallbackInfo ci) {
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		this.ntmitSteamBefore = te.reasimSteam;
		int elapsed = RbmkJumpHandler.reactorBurstElapsed(world, te.getPos());
		if (elapsed > 0) RbmkJumpHandler.pumpReactor(world, te.getPos(), elapsed);
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
