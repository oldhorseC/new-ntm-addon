package com.ntmit.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntityBlastChunk extends Entity implements IEntityAdditionalSpawnData {

	public int count;
	public int[] states = new int[0];
	public byte[] offsets = new byte[0];
	public int pile;
	public float roll;
	public float prevRoll;
	public float tumble;
	public float prevTumble;
	public float spin;
	public int age;
	public boolean landed;
	public double thrust;
	public int thrustTicks;
	private int stuckTicks;

	private static final double GRAVITY = 0.08D;
	private static final double DRAG = 0.985D;
	private static final double MAX_FALL = 2.5D;
	private static final int LIFETIME = 400;
	private static final int STUCK_TICKS = 60;

	private static final int[][] ORDER = new int[][] {
			{ 0, 0, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 0, 0, 1 }, { 0, 0, -1 },
			{ 1, 0, 1 }, { 1, 0, -1 }, { -1, 0, 1 }, { -1, 0, -1 },
			{ 0, 1, 0 }, { 1, 1, 0 }, { -1, 1, 0 }, { 0, 1, 1 }, { 0, 1, -1 },
			{ 1, 1, 1 }, { 1, 1, -1 }, { -1, 1, 1 }, { -1, 1, -1 } };

	private static final DataParameter<Float> ROLL = EntityDataManager
			.<Float>createKey(EntityBlastChunk.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> TUMBLE = EntityDataManager
			.<Float>createKey(EntityBlastChunk.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> SPIN = EntityDataManager
			.<Float>createKey(EntityBlastChunk.class, DataSerializers.FLOAT);

	public EntityBlastChunk(World world) {
		super(world);
		this.setSize(1.0F, 1.0F);
	}

	public void configure(int baseX, int baseY, int baseZ, int[] states, int[] positions, int pile) {

		this.count = Math.min(states.length, positions.length / 3);
		this.states = new int[this.count];
		this.offsets = new byte[this.count * 3];
		for (int i = 0; i < this.count; i++) {
			this.states[i] = states[i];
			this.offsets[i * 3] = (byte) (positions[i * 3] - baseX);
			this.offsets[i * 3 + 1] = (byte) (positions[i * 3 + 1] - baseY);
			this.offsets[i * 3 + 2] = (byte) (positions[i * 3 + 2] - baseZ);
		}
		this.pile = Math.max(1, pile);
		this.setPosition((double) baseX, (double) baseY, (double) baseZ);
		this.refreshBounds();
	}

	public void setSpin(float spin) {
		this.spin = spin;
		if (this.world != null && !this.world.isRemote) this.dataManager.set(SPIN, Float.valueOf(spin));
	}

	public void setThrust(double thrust, int ticks) {
		this.thrust = thrust;
		this.thrustTicks = Math.max(0, ticks);
	}

	private void refreshBounds() {

		if (this.count <= 0) {
			this.setEntityBoundingBox(new AxisAlignedBB(this.posX, this.posY, this.posZ,
					this.posX + 1.0D, this.posY + 1.0D, this.posZ + 1.0D));
			return;
		}
		int minX = 0;
		int maxX = 0;
		int minY = 0;
		int maxY = 0;
		int minZ = 0;
		int maxZ = 0;
		for (int i = 0; i < this.count; i++) {
			minX = Math.min(minX, this.offsets[i * 3]);
			maxX = Math.max(maxX, this.offsets[i * 3]);
			minY = Math.min(minY, this.offsets[i * 3 + 1]);
			maxY = Math.max(maxY, this.offsets[i * 3 + 1]);
			minZ = Math.min(minZ, this.offsets[i * 3 + 2]);
			maxZ = Math.max(maxZ, this.offsets[i * 3 + 2]);
		}
		this.setEntityBoundingBox(new AxisAlignedBB(this.posX + (double) minX, this.posY + (double) minY,
				this.posZ + (double) minZ, this.posX + (double) maxX + 1.0D, this.posY + (double) maxY + 1.0D,
				this.posZ + (double) maxZ + 1.0D));
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(ROLL, Float.valueOf(0.0F));
		this.dataManager.register(TUMBLE, Float.valueOf(0.0F));
		this.dataManager.register(SPIN, Float.valueOf(0.0F));
	}

	@Override
	public void onUpdate() {

		this.age++;
		this.prevRoll = this.roll;
		this.prevTumble = this.tumble;
		if (!Double.isFinite(this.posX) || !Double.isFinite(this.posY) || !Double.isFinite(this.posZ)) {
			this.setDead();
			return;
		}
		if (this.world == null) {
			this.setDead();
			return;
		}
		if (this.world.isRemote) {
			this.roll = this.dataManager.get(ROLL).floatValue();
			this.tumble = this.dataManager.get(TUMBLE).floatValue();
			this.spin = this.dataManager.get(SPIN).floatValue();
			super.onUpdate();
			this.refreshBounds();
			return;
		}

		this.roll += this.spin;
		this.tumble += this.spin * 0.6F;
		this.dataManager.set(ROLL, Float.valueOf(this.roll));
		this.dataManager.set(TUMBLE, Float.valueOf(this.tumble));

		if (this.age > LIFETIME) {
			this.fracture();
			return;
		}

		this.motionY -= GRAVITY;
		if (this.thrustTicks > 0) {
			this.motionY += this.thrust;
			this.thrustTicks--;
		}
		this.motionX *= DRAG;
		this.motionZ *= DRAG;
		if (this.motionY < -MAX_FALL) this.motionY = -MAX_FALL;
		this.noClip = true;
		this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

		int bx = (int) Math.floor(this.posX);
		int bz = (int) Math.floor(this.posZ);
		if (!this.world.isBlockLoaded(new BlockPos(bx, 0, bz))) {
			this.fracture();
			return;
		}
		int ground = this.world.getHeight(bx, bz);
		if (this.motionY <= 0.0D && this.posY <= (double) ground + 0.15D) {
			this.land(bx, ground, bz);
			return;
		}
		if (this.motionX * this.motionX + this.motionZ * this.motionZ < 4.0E-4D && this.motionY <= 0.0D) {
			if (++this.stuckTicks > STUCK_TICKS) {
				this.land(bx, ground, bz);
				return;
			}
		} else {
			this.stuckTicks = 0;
		}

		super.onUpdate();
		this.refreshBounds();
	}

	private void land(int x, int y, int z) {

		this.landed = true;
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.setPosition((double) x, (double) y, (double) z);
		this.pileAt(x, y, z);
		this.setDead();
	}

	private void pileAt(int x, int y, int z) {

		if (this.count <= 0) return;
		int target = Math.max(1, Math.min(this.pile, this.count));
		int placed = 0;
		for (int i = 0; i < ORDER.length && placed < target; i++) {
			int[] order = ORDER[i];
			BlockPos pos = new BlockPos(x + order[0], y + order[1], z + order[2]);
			if (!this.world.isBlockLoaded(pos)) continue;
			IBlockState existing = this.world.getBlockState(pos);
			if (!existing.getBlock().isReplaceable(this.world, pos)) continue;
			IBlockState state = Block.getStateById(this.states[placed % this.count]);
			if (state == null) continue;
			if (state.getMaterial().isLiquid()) continue;
			if (state.getBlock().isAir(state, this.world, pos)) continue;
			this.world.setBlockState(pos, state, 3);
			placed++;
		}
		BlockPos below = new BlockPos(x, y - 1, z);
		int dust = this.world.isBlockLoaded(below) ? Block.getStateId(this.world.getBlockState(below)) : 0;
		for (int i = 0; i < 24; i++) {
			double dx = this.world.rand.nextGaussian() * 1.2D;
			double dz = this.world.rand.nextGaussian() * 1.2D;
			this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, this.posX + 0.5D + dx,
					this.posY + 0.4D, this.posZ + 0.5D + dz, dx * 0.15D,
					0.25D + this.world.rand.nextDouble() * 0.35D, dz * 0.15D, dust);
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, com.hbm.lib.HBMSoundHandler.blockDebris,
				SoundCategory.BLOCKS, 3.0F, 0.65F + this.world.rand.nextFloat() * 0.2F);
	}

	private void fracture() {

		int dust = 0;
		BlockPos pos = new BlockPos(this.posX, this.posY - 1.0D, this.posZ);
		if (this.world != null && this.world.isBlockLoaded(pos)) {
			dust = Block.getStateId(this.world.getBlockState(pos));
		}
		for (int i = 0; i < 16; i++) {
			double dx = this.world.rand.nextGaussian() * 1.5D;
			double dz = this.world.rand.nextGaussian() * 1.5D;
			this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, this.posX + 0.5D + dx,
					this.posY + 0.5D, this.posZ + 0.5D + dz, dx * 0.15D, 0.3D, dz * 0.15D, dust);
		}
		this.setDead();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {

		this.count = compound.getInteger("count");
		this.states = compound.getIntArray("states");
		this.offsets = compound.getByteArray("offsets");
		this.pile = compound.getInteger("pile");
		this.roll = compound.getFloat("roll");
		this.tumble = compound.getFloat("tumble");
		this.spin = compound.getFloat("spin");
		this.age = compound.getInteger("age");
		this.landed = compound.getBoolean("landed");
		this.thrust = compound.getDouble("thrust");
		this.thrustTicks = compound.getInteger("thrustTicks");
		this.count = Math.min(this.count, Math.min(this.states.length, this.offsets.length / 3));
		this.dataManager.set(ROLL, Float.valueOf(this.roll));
		this.dataManager.set(TUMBLE, Float.valueOf(this.tumble));
		this.dataManager.set(SPIN, Float.valueOf(this.spin));
		this.refreshBounds();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {

		compound.setInteger("count", this.count);
		compound.setIntArray("states", this.states);
		compound.setByteArray("offsets", this.offsets);
		compound.setInteger("pile", this.pile);
		compound.setFloat("roll", this.roll);
		compound.setFloat("tumble", this.tumble);
		compound.setFloat("spin", this.spin);
		compound.setInteger("age", this.age);
		compound.setBoolean("landed", this.landed);
		compound.setDouble("thrust", this.thrust);
		compound.setInteger("thrustTicks", this.thrustTicks);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {

		buffer.writeInt(this.count);
		buffer.writeInt(this.pile);
		for (int i = 0; i < this.count; i++) buffer.writeInt(this.states[i]);
		buffer.writeBytes(this.offsets);
		buffer.writeFloat(this.roll);
		buffer.writeFloat(this.tumble);
		buffer.writeFloat(this.spin);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {

		this.count = buffer.readInt();
		this.pile = buffer.readInt();
		this.states = new int[this.count];
		for (int i = 0; i < this.count; i++) this.states[i] = buffer.readInt();
		this.offsets = new byte[this.count * 3];
		buffer.readBytes(this.offsets);
		this.roll = buffer.readFloat();
		this.tumble = buffer.readFloat();
		this.spin = buffer.readFloat();
		this.prevRoll = this.roll;
		this.prevTumble = this.tumble;
		this.dataManager.set(ROLL, Float.valueOf(this.roll));
		this.dataManager.set(TUMBLE, Float.valueOf(this.tumble));
		this.dataManager.set(SPIN, Float.valueOf(this.spin));
		this.refreshBounds();
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
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
	public boolean canBeCollidedWith() {
		return false;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public void applyEntityCollision(Entity entity) {
	}
}
