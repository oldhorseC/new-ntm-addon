package com.ntmit.mixin;

import com.ntmit.lib.ChunkLoadGuard;

import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public abstract class MixinWorldLoadGuard {

	@Inject(method = {"getBlockState(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;", "func_180495_p(Lnet/minecraft/util/math/BlockPos;)Lnet/minecraft/block/state/IBlockState;"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$guardGetBlockState(BlockPos pos, CallbackInfoReturnable<IBlockState> cir) {
		if (!ChunkLoadGuard.active()) return;
		if (((World) (Object) this).isBlockLoaded(pos, false)) return;
		cir.setReturnValue(Blocks.AIR.getDefaultState());
	}

	@Inject(method = {"setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z", "func_180501_a(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;I)Z"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$guardSetBlockState(BlockPos pos, IBlockState state, int flags, CallbackInfoReturnable<Boolean> cir) {
		if (!ChunkLoadGuard.active()) return;
		if (((World) (Object) this).isBlockLoaded(pos, false)) return;
		cir.setReturnValue(Boolean.FALSE);
	}

	@Inject(method = {"isBlockPowered(Lnet/minecraft/util/math/BlockPos;)Z", "func_175640_z(Lnet/minecraft/util/math/BlockPos;)Z"}, at = @At("HEAD"), cancellable = true, require = 1, remap = false)
	private void ntmit$guardIsBlockPowered(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (!ChunkLoadGuard.active()) return;
		if (((World) (Object) this).isBlockLoaded(pos, false)) return;
		cir.setReturnValue(Boolean.FALSE);
	}
}
