package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.lib.RbmkSteamCurve;

import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(value = TileEntityRBMKBase.class, priority = 1100)
public class MixinTileEntityRBMKJumpControl {

	@Unique private static final int NT_FORCED_DAMAGE = 19200;

	@Unique private static Field ntmitDamageField;
	@Unique private static Field ntmitFallingField;
	@Unique private static Field ntmitHeightField;

	@Unique private int ntmitStoredDamage;
	@Unique private double ntmitTargetHeight;
	@Unique private boolean ntmitHasTarget;
	@Unique private boolean ntmitBoosting;
	@Unique private boolean ntmitCycleActive;

	@Inject(method = "leafia$jump", at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$controlJump(CallbackInfo ci) {
		this.ntmitBoosting = false;
		Field damageField = ntmitDamageField();
		Field fallingField = ntmitFallingField();
		if (damageField == null || fallingField == null) return;
		TileEntityRBMKBase te = (TileEntityRBMKBase) (Object) this;
		World world = te.getWorld();
		if (world == null || world.isRemote) return;
		if (RbmkJumpHandler.isNativeJump(world, te.getPos())) {
			this.ntmitBoosting = false;
			this.ntmitCycleActive = false;
			this.ntmitHasTarget = false;
			return;
		}
		try {
			int stored = damageField.getInt(this);
			boolean falling = fallingField.getBoolean(this);
			if (!this.ntmitCycleActive && stored <= 0) return;

			this.ntmitStoredDamage = stored;
			this.ntmitBoosting = true;

			if (this.ntmitCycleActive && !falling) {
				damageField.setInt(this, NT_FORCED_DAMAGE);
				return;
			}
			if (this.ntmitCycleActive) {
				this.ntmitCycleActive = false;
				this.ntmitHasTarget = false;
				int previous = (int) Math.round(this.ntmitTargetHeight * 1000.0D);
				this.ntmitStoredDamage = stored > previous ? stored : 0;
				damageField.setInt(this, this.ntmitStoredDamage);
				return;
			}

			Field heightField = ntmitHeightField();
			if (falling || (heightField != null && heightField.getDouble(this) > 0.0D)) {
				this.ntmitStoredDamage = 0;
				damageField.setInt(this, 0);
				return;
			}

			double target = Math.min(RbmkSteamCurve.damageToAmplitude(stored), RbmkJumpHandler.JUMP_HEIGHT_LIMIT);
			if (target <= 0.0D) {
				this.ntmitStoredDamage = 0;
				damageField.setInt(this, 0);
				return;
			}

			this.ntmitTargetHeight = target;
			this.ntmitHasTarget = true;
			this.ntmitCycleActive = true;
			damageField.setInt(this, NT_FORCED_DAMAGE);
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
		if (!this.ntmitHasTarget) return;
		Field heightField = ntmitHeightField();
		Field fallingField = ntmitFallingField();
		if (heightField == null || fallingField == null) return;
		try {
			double height = heightField.getDouble(this);
			if (height < this.ntmitTargetHeight) return;
			heightField.setDouble(this, this.ntmitTargetHeight);
			fallingField.setBoolean(this, true);
			this.ntmitHasTarget = false;
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
