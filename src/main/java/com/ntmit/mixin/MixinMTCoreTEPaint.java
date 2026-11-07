package com.ntmit.mixin;

import com.ntmit.lib.TurbinePaintAccess;
import com.ntmit.packet.PacketNTMITTurbinePaint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.core.MTCoreTE", remap = false)
public abstract class MixinMTCoreTEPaint implements TurbinePaintAccess {

	@Unique
	private int ntmit$paintColor = TurbinePaintAccess.NO_PAINT;

	@Override
	public int ntmit$getPaintColor() {
		return this.ntmit$paintColor;
	}

	@Override
	public void ntmit$setPaintColor(int color) {
		this.ntmit$paintColor = color < 0 ? TurbinePaintAccess.NO_PAINT : color & 0xFFFFFF;
	}

	@Inject(method = "assembleFromNBT_internal", at = @At("HEAD"))
	private void ntmit$readPaint(NBTTagCompound compound, CallbackInfoReturnable<Boolean> cir) {
		if (compound.hasKey(TurbinePaintAccess.NBT_KEY)) {
			this.ntmit$paintColor = compound.getInteger(TurbinePaintAccess.NBT_KEY) & 0xFFFFFF;
		}
	}

	@Inject(method = "writeAssemblyToNBT", at = @At("RETURN"))
	private void ntmit$writePaint(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
		NBTTagCompound out = cir.getReturnValue();
		if (out == null) out = compound;
		if (out == null) return;
		if (this.ntmit$paintColor >= 0) {
			out.setInteger(TurbinePaintAccess.NBT_KEY, this.ntmit$paintColor);
		} else {
			out.removeTag(TurbinePaintAccess.NBT_KEY);
		}
	}

	@Inject(method = "onPlayerValidate", at = @At("TAIL"))
	private void ntmit$syncPaint(EntityPlayer player, CallbackInfo ci) {
		if (!(player instanceof EntityPlayerMP)) return;
		PacketNTMITTurbinePaint.sendTo((EntityPlayerMP) player, ((TileEntity) (Object) this).getPos(),
				this.ntmit$paintColor);
	}
}
