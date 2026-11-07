package com.ntmit.mixin;

import com.ntmit.lib.TurbinePaintInteraction;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.core.MTCoreBlock", remap = false)
public abstract class MixinMTCoreBlockPaint {

	@Inject(method = "func_180639_a(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/state/IBlockState;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/EnumHand;Lnet/minecraft/util/EnumFacing;FFF)Z",
			at = @At("HEAD"), cancellable = true)
	private void ntmit$paintCore(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumHand hand,
			EnumFacing side, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
		EnumHand screwHand = TurbinePaintInteraction.screwdriverHand(player, hand);
		if (screwHand == null) return;
		if (!TurbinePaintInteraction.tryPaint(this, world, player, pos, screwHand)) return;
		cir.setReturnValue(Boolean.TRUE);
		cir.cancel();
	}
}
