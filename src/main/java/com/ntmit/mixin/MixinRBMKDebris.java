package com.ntmit.mixin;

import com.hbm.entity.projectile.EntityDebrisBase;
import com.hbm.entity.projectile.EntityRBMKDebris;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityDebrisBase.class, priority = 1000)
public abstract class MixinRBMKDebris {

	@Unique private int ntmitTicks;

	@Inject(method = {"func_70071_h_", "onUpdate"}, at = @At("HEAD"), remap = false)
	private void ntmit$startNoClip(CallbackInfo ci) {
		if (!(((Object) this) instanceof EntityRBMKDebris)) return;
		Entity entity = (Entity) (Object) this;
		if (this.ntmitTicks < 15) entity.noClip = true;
		this.ntmitTicks++;
	}

	@Inject(method = {"func_70071_h_", "onUpdate"}, at = @At("TAIL"), remap = false)
	private void ntmit$finishNoClip(CallbackInfo ci) {
		if (!(((Object) this) instanceof EntityRBMKDebris)) return;
		if (this.ntmitTicks != 15) return;
		Entity entity = (Entity) (Object) this;
		entity.noClip = false;
		if (entity.world == null || entity.world.isRemote) return;
		AxisAlignedBB box = entity.getEntityBoundingBox();
		boolean stuck = !entity.world.getCollisionBoxes(entity, box).isEmpty();
		if (!stuck) {
			BlockPos center = new BlockPos(entity.posX, entity.posY + (double) entity.height * 0.5D, entity.posZ);
			if (!entity.world.isBlockLoaded(center)) return;
			stuck = entity.world.getBlockState(center).getMaterial().blocksMovement();
		}
		if (!stuck) return;
		int x = MathHelper.floor(entity.posX);
		int z = MathHelper.floor(entity.posZ);
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		int top = -1;
		for (int y = entity.world.getActualHeight() - 1; y >= 0; y--) {
			pos.setPos(x, y, z);
			if (!entity.world.isBlockLoaded(pos)) continue;
			IBlockState state = entity.world.getBlockState(pos);
			if (!state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()) continue;
			top = y;
			break;
		}
		if (top < 0) return;
		entity.setPosition(entity.posX, (double) (top + 1), entity.posZ);
		entity.motionX = 0.0D;
		entity.motionY = 0.0D;
		entity.motionZ = 0.0D;
	}

	@Inject(method = {"func_70091_d", "move"}, at = @At("HEAD"), cancellable = true, remap = false)
	private void ntmit$noClipMove(MoverType type, double x, double y, double z, CallbackInfo ci) {
		if (!(((Object) this) instanceof EntityRBMKDebris)) return;
		Entity entity = (Entity) (Object) this;
		if (!entity.noClip) return;
		entity.setPosition(entity.posX + x, entity.posY + y, entity.posZ + z);
		ci.cancel();
	}
}
