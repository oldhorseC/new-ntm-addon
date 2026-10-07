package com.ntmit.mixin;

import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.hbm.blocks.machine.MachineFan$TileEntityFan")
public abstract class MixinTileEntityFan {

	@Redirect(method = "onLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isBlockPowered(Lnet/minecraft/util/math/BlockPos;)Z"), require = 0)
	private boolean ntmit$safePowered(World world, BlockPos pos) {
		if (world == null || pos == null) return false;
		for (EnumFacing facing : EnumFacing.values()) {
			if (!world.isBlockLoaded(pos.offset(facing))) return false;
		}
		return world.isBlockPowered(pos);
	}
}
