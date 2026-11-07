package com.ntmit.mixin;

import com.ntmit.lib.ChunkLoadGuard;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Collection;

@Mixin(Chunk.class)
public abstract class MixinChunkLoadGuard {

	@Inject(method = "onLoad", at = @At("HEAD"), remap = false)
	private void ntmit$enterLoad(CallbackInfo ci) {
		ChunkLoadGuard.enter();
	}

	@Inject(method = "onLoad", at = @At("RETURN"), remap = false)
	private void ntmit$exitLoad(CallbackInfo ci) {
		ChunkLoadGuard.exit();
	}

	@Redirect(method = "onLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;addTileEntities(Ljava/util/Collection;)V"), require = 0, remap = false)
	private void ntmit$copyTileEntities(World world, Collection<TileEntity> tiles) {
		world.addTileEntities(new ArrayList<TileEntity>(tiles));
	}

	@Redirect(method = "onLoad", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;func_175700_a(Ljava/util/Collection;)V"), require = 0, remap = false)
	private void ntmit$copyTileEntitiesSrg(World world, Collection<TileEntity> tiles) {
		world.addTileEntities(new ArrayList<TileEntity>(tiles));
	}
}
