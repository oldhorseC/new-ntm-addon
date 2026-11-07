package com.ntmit.mixin;

import com.hbm.lib.HBMSoundHandler;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.WorldServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * RBMK 融毁时 HBM 会用 volume = 50 的 rbmk_explosion 做全图广播。本模组已按冲击波速度对每个玩家单独排期传声，
 * 因此这里把这条原始广播拦掉，避免"瞬间全图响一遍"后再叠一次。
 */
@Mixin(WorldServer.class)
public abstract class MixinWorldServerPlaySound {

	@Inject(method = "playSound(Lnet/minecraft/entity/player/EntityPlayer;DDDLnet/minecraft/util/SoundEvent;Lnet/minecraft/util/SoundCategory;FF)V", at = @At("HEAD"), cancellable = true, require = 0)
	private void ntmit$blockRbmkMeltdownSound(EntityPlayer player, double x, double y, double z, SoundEvent sound,
			SoundCategory category, float volume, float pitch, CallbackInfo ci) {
		if (sound == HBMSoundHandler.rbmk_explosion && volume >= 50.0F) {
			ci.cancel();
		}
	}
}
