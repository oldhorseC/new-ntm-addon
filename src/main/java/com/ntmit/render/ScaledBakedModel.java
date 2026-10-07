package com.ntmit.render;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@SideOnly(Side.CLIENT)
public class ScaledBakedModel implements IBakedModel {

	private final IBakedModel delegate;
	private final float scale;

	public ScaledBakedModel(IBakedModel delegate, float scale) {
		this.delegate = delegate;
		this.scale = scale;
	}

	@Override
	public List<BakedQuad> getQuads(@Nullable IBlockState state, @Nullable EnumFacing side, long rand) {
		List<BakedQuad> quads = this.delegate.getQuads(state, side, rand);
		if (quads == null || quads.isEmpty()) return quads;
		List<BakedQuad> result = new ArrayList<>(quads.size());
		for (BakedQuad quad : quads) result.add(this.scaleQuad(quad));
		return result;
	}

	private BakedQuad scaleQuad(BakedQuad quad) {
		int[] data = quad.getVertexData();
		int stride = data.length / 4;
		int[] copy = new int[data.length];
		System.arraycopy(data, 0, copy, 0, data.length);
		for (int vertex = 0; vertex < 4; vertex++) {
			int offset = vertex * stride;
			if (offset + 2 >= copy.length) return quad;
			for (int axis = 0; axis < 3; axis++) {
				float value = Float.intBitsToFloat(copy[offset + axis]);
				value = 0.5F + (value - 0.5F) * this.scale;
				if (value < 0F) value = 0F;
				if (value > 1F) value = 1F;
				copy[offset + axis] = Float.floatToRawIntBits(value);
			}
		}
		return new BakedQuad(copy, quad.getTintIndex(), quad.getFace(), quad.getSprite(), quad.shouldApplyDiffuseLighting(), quad.getFormat());
	}

	@Override
	public boolean isAmbientOcclusion() {
		return this.delegate.isAmbientOcclusion();
	}

	@Override
	public boolean isGui3d() {
		return this.delegate.isGui3d();
	}

	@Override
	public boolean isBuiltInRenderer() {
		return this.delegate.isBuiltInRenderer();
	}

	@Override
	public TextureAtlasSprite getParticleTexture() {
		return this.delegate.getParticleTexture();
	}

	@Override
	public ItemCameraTransforms getItemCameraTransforms() {
		return this.delegate.getItemCameraTransforms();
	}

	@Override
	public ItemOverrideList getOverrides() {
		return this.delegate.getOverrides();
	}
}