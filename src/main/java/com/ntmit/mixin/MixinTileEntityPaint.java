package com.ntmit.mixin;

import com.ntmit.lib.TurbinePaintAccess;
import com.ntmit.lib.TurbinePaintStore;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TileEntity.class)
public abstract class MixinTileEntityPaint {

	@Inject(method = "writeToNBT", at = @At("RETURN"))
	private void ntmit$writePaint(NBTTagCompound compound, CallbackInfoReturnable<NBTTagCompound> cir) {
		if (!((Object) this instanceof TileEntity)) return;
		NBTTagCompound out = cir.getReturnValue();
		if (out == null) out = compound;
		if (out == null) return;
		int color = TurbinePaintStore.get((TileEntity) (Object) this);
		if (color >= 0) {
			out.setInteger(TurbinePaintAccess.NBT_KEY, color);
		} else {
			out.removeTag(TurbinePaintAccess.NBT_KEY);
		}
	}

	@Inject(method = "readFromNBT", at = @At("HEAD"))
	private void ntmit$readPaint(NBTTagCompound compound, CallbackInfo ci) {
		if (!((Object) this instanceof TileEntity)) return;
		TurbinePaintStore.set((TileEntity) (Object) this,
				compound.hasKey(TurbinePaintAccess.NBT_KEY)
						? compound.getInteger(TurbinePaintAccess.NBT_KEY)
						: TurbinePaintAccess.NO_PAINT);
	}
}
