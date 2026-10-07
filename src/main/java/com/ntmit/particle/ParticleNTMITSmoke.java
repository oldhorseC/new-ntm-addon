package com.ntmit.particle;

import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ParticleNTMITSmoke extends ParticleNTMITSteam {

	public ParticleNTMITSmoke(World world, double x, double y, double z) {
		super(world, x, y, z);
		float shade = 0.06F + world.rand.nextFloat() * 0.16F;
		this.particleRed = shade;
		this.particleGreen = shade;
		this.particleBlue = shade;
		this.alphaMod(0.5F);
	}
}