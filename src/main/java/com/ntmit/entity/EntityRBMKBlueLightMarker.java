package com.ntmit.entity;

import com.ntmit.lib.RbmkJumpHandler;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

public class EntityRBMKBlueLightMarker extends EntityLiving {

	private int markerTicks;

	public EntityRBMKBlueLightMarker(World world) {
		super(world);
		this.setSize(0.1F, 0.1F);
		this.setSilent(true);
		this.setInvisible(true);
		this.setNoGravity(true);
		this.noClip = true;
		this.ignoreFrustumCheck = true;
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		if (this.world.isRemote) return;
		this.markerTicks++;
		if (this.markerTicks == 1) {
			EntityRBMKBlueLight light = new EntityRBMKBlueLight(this.world, this.posX, this.posY, this.posZ, 15.0F, this.posY + 8192.0D);
			light.maxAge = 600;
			this.world.spawnEntity(light);
			this.setDead();
		}
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean isEntityInvulnerable(DamageSource source) {
		return true;
	}
}
