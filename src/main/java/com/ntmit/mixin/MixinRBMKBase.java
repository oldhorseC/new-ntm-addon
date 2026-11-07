package com.ntmit.mixin;

import com.hbm.blocks.machine.rbmk.RBMKBase;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RBMKBase.class)
public class MixinRBMKBase {

	@Inject(method = {"breakBlock", "func_180429_a"}, at = @At("HEAD"), require = 1, remap = false)
	private void ntmit$onBreak(World world, BlockPos pos, IBlockState state, CallbackInfo ci) {
		RbmkJumpHandler.onRbmkBroken(world, pos);
	}
}
