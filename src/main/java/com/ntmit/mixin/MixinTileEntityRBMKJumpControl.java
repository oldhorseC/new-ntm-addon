package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value = TileEntityRBMKBase.class, priority = 1100)
public class MixinTileEntityRBMKJumpControl {

	@Unique private static Field ntmitDamageField;
	@Unique private static Field ntmitFallingField;
	@Unique private static Field ntmitHeightField;
	@Unique private static boolean ntmitLogged;
	@Unique private static long ntmitBudgetTick = Long.MIN_VALUE;
	@Unique private static int ntmitBudgetUsed;

	@Unique private int ntmitStoredDamage;
	@Unique private int ntmitForcedDamage;
	@Unique private int ntmitJumpCooldown;
	@Unique private boolean ntmitBoosting;
	@Unique private boolean ntmitCycleActive;

	@Inject(method = "leafia$jump", at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$controlJump(CallbackInfo ci) {
		this.ntmitBoosting = false;
		Field damageField = ntmitDamageField();
		Field fallingField = ntmitFallingField();
		if (damageField == null || fallingField == null) return;
		World world = ((TileEntityRBMKBase) (Object) this).getWorld();
		if (world == null || world.isRemote) return;
		try {
			int stored = damageField.getInt(this);
			if (!this.ntmitCycleActive && this.ntmitJumpCooldown > 0) this.ntmitJumpCooldown--;
			int span = RbmkJumpHandler.STEAM_JUMP_TARGET_MAX - RbmkJumpHandler.STEAM_JUMP_TARGET_MIN;
			if (span <= 0) return;
			float ramp = (float) (stored - RbmkJumpHandler.STEAM_JUMP_TARGET_MIN) / (float) span;
			if (ramp <= 0.0F) {
				if (!this.ntmitCycleActive) return;
				ramp = 0.0F;
			}
			ramp = Math.min(ramp, 1.0F);
			boolean falling = fallingField.getBoolean(this);
			this.ntmitStoredDamage = stored;
			this.ntmitBoosting = true;
			if (this.ntmitCycleActive && !falling) {
				damageField.setInt(this, this.ntmitForcedDamage);
				return;
			}
			if (this.ntmitCycleActive) {
				this.ntmitCycleActive = false;
				damageField.setInt(this, 0);
				return;
			}
			if (this.ntmitJumpCooldown > 0) {
				damageField.setInt(this, 0);
				return;
			}
			long time = world.getTotalWorldTime();
			if (ntmitBudgetTick != time) {
				ntmitBudgetTick = time;
				ntmitBudgetUsed = 0;
			}
			if (ntmitBudgetUsed >= RbmkJumpHandler.STEAM_JUMP_MAX_PER_TICK) {
				damageField.setInt(this, 0);
				return;
			}
			ntmitBudgetUsed++;
			this.ntmitForcedDamage = Math.round(RbmkJumpHandler.lerp((float) RbmkJumpHandler.STEAM_JUMP_MIN_DAMAGE, (float) RbmkJumpHandler.STEAM_JUMP_FORCED_DAMAGE, ramp * ramp));
			this.ntmitCycleActive = true;
			this.ntmitJumpCooldown = Math.round(RbmkJumpHandler.lerp((float) RbmkJumpHandler.STEAM_JUMP_PERIOD_START, (float) RbmkJumpHandler.STEAM_JUMP_PERIOD_MIN, ramp));
			damageField.setInt(this, this.ntmitForcedDamage);
			if (!ntmitLogged) {
				ntmitLogged = true;
				RbmkJumpHandler.diag("jump control active, forcedDamage=" + this.ntmitForcedDamage + " period=" + this.ntmitJumpCooldown);
			}
		} catch (Throwable throwable) {
			this.ntmitBoosting = false;
		}
	}

	@Inject(method = "leafia$jump", at = @At("RETURN"), require = 1, remap = false)
	private void ntmit$restoreJump(CallbackInfo ci) {
		if (!this.ntmitBoosting) return;
		this.ntmitBoosting = false;
		Field field = ntmitDamageField();
		if (field == null) return;
		try {
			field.setInt(this, this.ntmitStoredDamage);
		} catch (Throwable throwable) {
		}
	}

	@Inject(method = "leafia$jump", at = @At("RETURN"), require = 1, remap = false)
	private void ntmit$limitJumpHeight(CallbackInfo ci) {
		Field heightField = ntmitHeightField();
		Field fallingField = ntmitFallingField();
		if (heightField == null || fallingField == null) return;
		try {
			if (heightField.getDouble(this) <= RbmkJumpHandler.JUMP_HEIGHT_LIMIT) return;
			heightField.setDouble(this, RbmkJumpHandler.JUMP_HEIGHT_LIMIT);
			fallingField.setBoolean(this, true);
		} catch (Throwable throwable) {
		}
	}

	@Inject(method = "leafia$jump", at = @At("RETURN"), require = 1, remap = false)
	private void ntmit$unstuckJump(CallbackInfo ci) {
		if (this.ntmitCycleActive) return;
		Field heightField = ntmitHeightField();
		Field fallingField = ntmitFallingField();
		Field damageField = ntmitDamageField();
		if (heightField == null || fallingField == null || damageField == null) return;
		try {
			if (heightField.getDouble(this) <= 0.0D) return;
			if (fallingField.getBoolean(this)) return;
			if (damageField.getInt(this) > 0) return;
			fallingField.setBoolean(this, true);
		} catch (Throwable throwable) {
		}
	}

	@Inject(method = "meltdown", at = @At("HEAD"), require = 0, cancellable = true, remap = false)
	private void ntmit$guardMeltdown(CallbackInfo ci) {
		if (!this.ntmitBoosting) return;
		this.ntmitBoosting = false;
		Field field = ntmitDamageField();
		if (field != null) {
			try {
				field.setInt(this, 0);
			} catch (Throwable throwable) {
			}
		}
		RbmkJumpHandler.diag("jump boost leak detected, meltdown vetoed");
		ci.cancel();
	}

	@Unique private static Field ntmitDamageField() {
		if (ntmitDamageField == null) {
			try {
				ntmitDamageField = TileEntityRBMKBase.class.getField(RbmkJumpHandler.DAMAGE_FIELD);
			} catch (Throwable throwable) {
				return null;
			}
		}
		return ntmitDamageField;
	}

	@Unique private static Field ntmitFallingField() {
		if (ntmitFallingField == null) {
			try {
				ntmitFallingField = TileEntityRBMKBase.class.getField("leafia$falling");
			} catch (Throwable throwable) {
				return null;
			}
		}
		return ntmitFallingField;
	}

	@Unique private static Field ntmitHeightField() {
		if (ntmitHeightField == null) {
			try {
				ntmitHeightField = TileEntityRBMKBase.class.getField("leafia$jumpHeight");
			} catch (Throwable throwable) {
				return null;
			}
		}
		return ntmitHeightField;
	}
}
