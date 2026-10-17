package com.ntmit.entity;

import com.ntmit.lib.RbmkJumpHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityRBMKBlueLight extends Entity implements IEntityAdditionalSpawnData {

	public double baseY;
	public double topY;
	public float width;
	public int age;
	public int maxAge = Integer.MAX_VALUE;
	public int fadeOutTicks;
	public float timeAlpha = 1.0F;
	public boolean timeLocked;
	public double capturedDistance;

	public EntityRBMKBlueLight(World world) {
		super(world);
		this.setSize(0.1F, 0.1F);
		this.ignoreFrustumCheck = true;
		this.timeAlpha = world.isDaytime() ? 0.0F : 1.0F;
	}

	public EntityRBMKBlueLight(World world, double x, double y, double z, float width, double topY) {
		this(world);
		this.setPosition(x, y, z);
		this.baseY = y;
		this.topY = topY;
		this.width = width;
	}

	@Override
	public void setPosition(double x, double y, double z) {
		super.setPosition(x, y, z);
		if (this.width <= 0.0F) {
			this.baseY = y;
			this.topY = y + 8192.0D;
			this.width = 15.0F;
		}
	}

	@Override
	protected void entityInit() {
	}

	@Override
	public void onUpdate() {
		super.onUpdate();
		this.age++;
		if (!this.timeLocked && this.age % 20 == 0) this.timeAlpha = this.world.isDaytime() ? 0.0F : 1.0F;
		if (!this.world.isRemote && this.age >= this.maxAge) this.setDead();
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	@Override
	public boolean isInRangeToRender3d(double x, double y, double z) {
		return true;
	}

	@Override
	public boolean isEntityInvulnerable(net.minecraft.util.DamageSource source) {
		return true;
	}

	@Override
	public boolean attackEntityFrom(net.minecraft.util.DamageSource source, float amount) {
		return false;
	}

	@Override
	public void setFire(int seconds) {
	}

	@Override
	public boolean isBurning() {
		return false;
	}

	@Override
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(this.posX - (double) this.width * 0.5D, this.baseY, this.posZ - (double) this.width * 0.5D,
				this.posX + (double) this.width * 0.5D, this.topY, this.posZ + (double) this.width * 0.5D);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeDouble(this.baseY);
		buffer.writeDouble(this.topY);
		buffer.writeFloat(this.width);
		buffer.writeInt(this.maxAge);
		buffer.writeInt(this.fadeOutTicks);
		buffer.writeFloat(this.timeAlpha);
		buffer.writeBoolean(this.timeLocked);
		buffer.writeDouble(this.capturedDistance);
	}

	@Override
	public void readSpawnData(ByteBuf additionalData) {
		this.baseY = additionalData.readDouble();
		this.topY = additionalData.readDouble();
		this.width = additionalData.readFloat();
		this.maxAge = additionalData.readInt();
		this.fadeOutTicks = additionalData.readInt();
		this.timeAlpha = additionalData.readFloat();
		this.timeLocked = additionalData.readBoolean();
		this.capturedDistance = additionalData.readDouble();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		this.baseY = compound.getDouble("baseY");
		this.topY = compound.getDouble("topY");
		this.width = compound.getFloat("width");
		this.age = compound.getInteger("age");
		this.maxAge = compound.getInteger("maxAge");
		this.fadeOutTicks = compound.getInteger("fadeOutTicks");
		this.timeAlpha = compound.getFloat("timeAlpha");
		this.timeLocked = compound.getBoolean("timeLocked");
		this.capturedDistance = compound.getDouble("capturedDistance");
		if (this.maxAge <= 0) this.maxAge = Integer.MAX_VALUE;
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		compound.setDouble("baseY", this.baseY);
		compound.setDouble("topY", this.topY);
		compound.setFloat("width", this.width);
		compound.setInteger("age", this.age);
		compound.setInteger("maxAge", this.maxAge);
		compound.setInteger("fadeOutTicks", this.fadeOutTicks);
		compound.setFloat("timeAlpha", this.timeAlpha);
		compound.setBoolean("timeLocked", this.timeLocked);
		compound.setDouble("capturedDistance", this.capturedDistance);
	}
}
