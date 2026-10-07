package com.ntmit.mixin;

import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityRBMKConsole.class, priority = 1000)
public abstract class MixinTileEntityRBMKConsole {

	@Inject(method = "receiveControl(Lnet/minecraft/entity/player/EntityPlayerMP;Lnet/minecraft/nbt/NBTTagCompound;)V", at = @At("HEAD"), require = 0, remap = false)
	private void ntmit$detectAz5(EntityPlayerMP player, NBTTagCompound data, CallbackInfo ci) {
		if (data == null) return;
		if (!data.getBoolean("ntmit_az5") && !data.getBoolean("fuckingstopalldamnautocontrolrods")) return;
		TileEntityRBMKConsole console = (TileEntityRBMKConsole) (Object) this;
		RbmkJumpHandler.az5Press(console.getWorld(), console.getPos());
	}
}
