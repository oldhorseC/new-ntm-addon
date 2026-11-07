package com.ntmit.mixin;

import com.hbm.api.block.IToolable;
import com.ntmit.lib.TurbinePaintInteraction;
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
@Mixin(targets = "com.leafia.contents.machines.misc.modular_turbine.ModularTurbineBlockBase", remap = false)
public abstract class MixinModularTurbineBlockBasePaint {

	@Inject(method = "onScrew(Lnet/minecraft/world/World;Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/EnumFacing;FFFLnet/minecraft/util/EnumHand;Lcom/hbm/api/block/IToolable$ToolType;)Z",
			at = @At("HEAD"), cancellable = true)
	private void ntmit$paintTurbine(World world, EntityPlayer player, BlockPos pos, EnumFacing side,
			float hitX, float hitY, float hitZ, EnumHand hand, IToolable.ToolType tool,
			CallbackInfoReturnable<Boolean> cir) {
		if (tool != IToolable.ToolType.SCREWDRIVER) return;
		if (!TurbinePaintInteraction.tryPaint(this, world, player, pos, hand)) return;
		cir.setReturnValue(Boolean.TRUE);
		cir.cancel();
	}
}

