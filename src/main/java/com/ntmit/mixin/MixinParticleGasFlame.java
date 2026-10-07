package com.ntmit.mixin;

import com.hbm.particle.ParticleGasFlame;
import com.ntmit.particle.FuelTintCache;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
@Mixin(value = ParticleGasFlame.class, priority = 1000)
public abstract class MixinParticleGasFlame {

	private static Field ntmitRed;
	private static Field ntmitGreen;
	private static Field ntmitBlue;
	private static Field ntmitWorld;
	private static Field ntmitPosX;
	private static Field ntmitPosY;
	private static Field ntmitPosZ;
	private static boolean ntmitResolved;

	@Inject(method = "updateColor", at = @At("TAIL"), require = 0, remap = false)
	private void ntmit$fuelTint(CallbackInfo ci) {
		if (!ntmitResolve(this)) return;
		try {
			World world = (World) ntmitWorld.get(this);
			int tint = FuelTintCache.tintAt(world, ntmitPosX.getDouble(this), ntmitPosY.getDouble(this), ntmitPosZ.getDouble(this));
			if (tint == 0xFFFFFF) return;
			ntmitRed.setFloat(this, (float) ((tint >> 16) & 0xFF) / 255.0F);
			ntmitGreen.setFloat(this, (float) ((tint >> 8) & 0xFF) / 255.0F);
			ntmitBlue.setFloat(this, (float) (tint & 0xFF) / 255.0F);
		} catch (Throwable throwable) {
		}
	}

	private static boolean ntmitResolve(Object particle) {
		if (ntmitResolved) return ntmitRed != null && ntmitGreen != null && ntmitBlue != null && ntmitWorld != null && ntmitPosX != null && ntmitPosY != null && ntmitPosZ != null;
		ntmitResolved = true;
		Class<?> type = particle.getClass();
		ntmitRed = ntmitField(type, "particleRed");
		ntmitGreen = ntmitField(type, "particleGreen");
		ntmitBlue = ntmitField(type, "particleBlue");
		ntmitWorld = ntmitField(type, "world");
		ntmitPosX = ntmitField(type, "posX");
		ntmitPosY = ntmitField(type, "posY");
		ntmitPosZ = ntmitField(type, "posZ");
		return ntmitRed != null && ntmitGreen != null && ntmitBlue != null && ntmitWorld != null && ntmitPosX != null && ntmitPosY != null && ntmitPosZ != null;
	}

	private static Field ntmitField(Class<?> type, String name) {
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			try {
				Field field = c.getDeclaredField(name);
				field.setAccessible(true);
				return field;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}
}

