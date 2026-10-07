package com.ntmit.entity;

import com.hbm.blocks.fluid.CoriumFinite;
import com.hbm.blocks.machine.rbmk.RBMKDebris;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.threading.PacketThreading;
import com.hbm.packet.toclient.AuxParticlePacketNT;
import com.hbm.particle.helper.HbmEffectNT;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemTooling;
import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.lib.RbmkJumpHandler;
import com.ntmit.particle.ParticleNTMITSmoke;
import com.ntmit.particle.ParticleNTMITSteam;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.item.EntityFallingBlock;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;

public class EntityLidPlate extends Entity implements IEntityAdditionalSpawnData {

	public int count;
	public int[] states = new int[0];
	public byte[] offsets = new byte[0];
	public int[] tints = new int[0];
	public float[] lifts = new float[0];
	public byte[] rodColors = new byte[0];
	public float roll;
	public float prevRoll;
	public float tumble;
	public float prevTumble;
	public float spin;
	public boolean landed;
	public boolean blasted;
	public boolean wrecked;
	public int age;
	public double pushY;
	public double groundY;
	private static final DataParameter<Boolean> PLATE_BLASTED = EntityDataManager.<Boolean>createKey(EntityLidPlate.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> PLATE_LANDED = EntityDataManager.<Boolean>createKey(EntityLidPlate.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> PLATE_WRECKED = EntityDataManager.<Boolean>createKey(EntityLidPlate.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Boolean> PLATE_RED = EntityDataManager.<Boolean>createKey(EntityLidPlate.class, DataSerializers.BOOLEAN);
	private static final DataParameter<Float> PLATE_ROLL = EntityDataManager.<Float>createKey(EntityLidPlate.class, DataSerializers.FLOAT);
	private static final DataParameter<Float> PLATE_TUMBLE = EntityDataManager.<Float>createKey(EntityLidPlate.class, DataSerializers.FLOAT);
	private static final DataParameter<Integer> PLATE_AGE = EntityDataManager.<Integer>createKey(EntityLidPlate.class, DataSerializers.VARINT);
	private int smokeLandTicks;
	private int errorTicks;
	private boolean blastSeen;
	private boolean ntmitRedSteam;
	private int ntmitCoreMinX;
	private int ntmitCoreMaxX;
	private int ntmitCoreMinZ;
	private int ntmitCoreMaxZ;
	private boolean ntmitCoreSet;

	public boolean isSpecialPlate() {
		return this.dataManager.get(PLATE_RED).booleanValue();
	}

	public void setCoreBounds(int minX, int maxX, int minZ, int maxZ) {
		this.ntmitCoreMinX = Math.min(minX, maxX);
		this.ntmitCoreMaxX = Math.max(minX, maxX);
		this.ntmitCoreMinZ = Math.min(minZ, maxZ);
		this.ntmitCoreMaxZ = Math.max(minZ, maxZ);
		this.ntmitCoreSet = true;
	}

	public EntityLidPlate(World world) {
		super(world);
		this.setSize(RbmkJumpHandler.PLATE_HITBOX_WIDTH, RbmkJumpHandler.PLATE_HITBOX_HEIGHT);
		this.ignoreFrustumCheck = true;
	}

	public void setRedSteam(boolean red) {
		if (this.world != null && this.world.isRemote) return;
		this.ntmitRedSteam = red;
		this.dataManager.set(PLATE_RED, Boolean.valueOf(red));
	}

	public void configure(List<BlockPos> positions, List<IBlockState> blockStates, BlockPos center, List<Integer> tints, List<Float> lifts, List<Byte> rodColors) {
		this.count = Math.min(positions.size(), blockStates.size());
		this.states = new int[this.count];
		this.offsets = new byte[this.count * 3];
		this.tints = new int[this.count];
		this.lifts = new float[this.count];
		this.rodColors = new byte[this.count];
		for (int i = 0; i < this.count; i++) {
			Vec3i offset = positions.get(i).subtract(center);
			this.offsets[i * 3] = (byte) offset.getX();
			this.offsets[i * 3 + 1] = (byte) offset.getY();
			this.offsets[i * 3 + 2] = (byte) offset.getZ();
			this.states[i] = Block.getStateId(blockStates.get(i));
			this.tints[i] = tints.get(i);
			this.lifts[i] = lifts.get(i);
			this.rodColors[i] = rodColors.get(i);
		}
		this.pushY = this.posY;
		this.groundY = this.posY - RbmkJumpHandler.PLATE_EMBED_DEPTH;
		this.sanitize();
		this.applySize();
	}

	private void applySize() {
		this.setSize(RbmkJumpHandler.PLATE_HITBOX_WIDTH, RbmkJumpHandler.PLATE_HITBOX_HEIGHT);
	}

	private void sanitize() {
		if (this.count <= 1) return;
		int before = this.count;
		boolean[] drop = new boolean[this.count];
		for (int i = 0; i < this.count; i++) {
			if (drop[i]) continue;
			for (int j = i + 1; j < this.count; j++) {
				if (drop[j]) continue;
				if (this.offsets[i * 3] != this.offsets[j * 3]) continue;
				if (this.offsets[i * 3 + 2] != this.offsets[j * 3 + 2]) continue;
				if (this.offsets[i * 3 + 1] >= this.offsets[j * 3 + 1]) {
					drop[j] = true;
				} else {
					drop[i] = true;
					break;
				}
			}
		}
		int kept = 0;
		for (int i = 0; i < this.count; i++) {
			if (!drop[i]) kept++;
		}
		if (kept == this.count) return;
		int[] states = new int[kept];
		byte[] offsets = new byte[kept * 3];
		int[] tints = new int[kept];
		float[] lifts = new float[kept];
		byte[] colors = new byte[kept];
		int k = 0;
		for (int i = 0; i < this.count; i++) {
			if (drop[i]) continue;
			states[k] = this.states[i];
			offsets[k * 3] = this.offsets[i * 3];
			offsets[k * 3 + 1] = this.offsets[i * 3 + 1];
			offsets[k * 3 + 2] = this.offsets[i * 3 + 2];
			tints[k] = this.tints[i];
			lifts[k] = this.lifts[i];
			colors[k] = this.rodColors[i];
			k++;
		}
		this.count = kept;
		this.states = states;
		this.offsets = offsets;
		this.tints = tints;
		this.lifts = lifts;
		this.rodColors = colors;
		RbmkJumpHandler.diag("plate sanitized entries " + before + " -> " + kept);
	}

	public net.minecraft.util.math.AxisAlignedBB crushBox() {
		int minX = 0;
		int maxX = 0;
		int minZ = 0;
		int maxZ = 0;
		for (int i = 0; i < this.count; i++) {
			minX = Math.min(minX, this.offsets[i * 3]);
			maxX = Math.max(maxX, this.offsets[i * 3]);
			minZ = Math.min(minZ, this.offsets[i * 3 + 2]);
			maxZ = Math.max(maxZ, this.offsets[i * 3 + 2]);
		}
		return new net.minecraft.util.math.AxisAlignedBB(this.posX + (double) minX, this.posY, this.posZ + (double) minZ, this.posX + (double) maxX + 1.0D, this.posY + (double) RbmkJumpHandler.PLATE_HITBOX_HEIGHT + 0.5D, this.posZ + (double) maxZ + 1.0D);
	}

	public boolean isRod(int index) {
		return index >= 0 && index < this.tints.length && this.tints[index] != 0xFFFFFF;
	}

	public float lift(int index) {
		return index >= 0 && index < this.lifts.length ? this.lifts[index] : 0.0F;
	}

	public byte rodColor(int index) {
		return index >= 0 && index < this.rodColors.length ? this.rodColors[index] : (byte) -1;
	}

	private int stuckTicks;
	private boolean homeSet;
	private double homeX;
	private double homeZ;
	private double homeRadius;

	public BlockPos offset(int index) {
		return new BlockPos(this.offsets[index * 3], this.offsets[index * 3 + 1], this.offsets[index * 3 + 2]);
	}

	@Override
	protected void entityInit() {
		this.dataManager.register(PLATE_BLASTED, Boolean.FALSE);
		this.dataManager.register(PLATE_LANDED, Boolean.FALSE);
		this.dataManager.register(PLATE_WRECKED, Boolean.FALSE);
		this.dataManager.register(PLATE_RED, Boolean.FALSE);
		this.dataManager.register(PLATE_ROLL, Float.valueOf(0.0F));
		this.dataManager.register(PLATE_TUMBLE, Float.valueOf(0.0F));
		this.dataManager.register(PLATE_AGE, Integer.valueOf(0));
	}

	@Override
	public void onUpdate() {
		this.age++;
		this.prevRoll = this.roll;
		this.prevTumble = this.tumble;
		if (!Double.isFinite(this.posX) || !Double.isFinite(this.posY) || !Double.isFinite(this.posZ) || !Double.isFinite(this.motionX) || !Double.isFinite(this.motionY) || !Double.isFinite(this.motionZ)) {
			this.setDead();
			return;
		}
		try {
			if (this.world.isRemote) {
				this.landed = this.dataManager.get(PLATE_LANDED).booleanValue();
				this.wrecked = this.dataManager.get(PLATE_WRECKED).booleanValue();
				this.blasted = this.dataManager.get(PLATE_BLASTED).booleanValue();
				this.roll = this.dataManager.get(PLATE_ROLL).floatValue();
				this.tumble = this.dataManager.get(PLATE_TUMBLE).floatValue();
				this.age = this.dataManager.get(PLATE_AGE).intValue();
			} else {
				if (!this.landed && this.age > 1) {
					this.roll += this.spin;
					this.tumble += this.spin * 0.7F;
				}
				this.dataManager.set(PLATE_ROLL, Float.valueOf(this.roll));
				this.dataManager.set(PLATE_TUMBLE, Float.valueOf(this.tumble));
				this.dataManager.set(PLATE_AGE, Integer.valueOf(this.age));
			}
			if (!this.world.isRemote && !this.landed && this.age > 1) {
				if (this.ntmitRedSteam && !this.dataManager.get(PLATE_RED).booleanValue()) this.dataManager.set(PLATE_RED, Boolean.TRUE);
				this.motionY -= RbmkJumpHandler.PLATE_GRAVITY;
				if (this.motionY < -RbmkJumpHandler.PLATE_MAX_FALL) this.motionY = -RbmkJumpHandler.PLATE_MAX_FALL;
				this.motionX *= RbmkJumpHandler.PLATE_AIR_DRAG_XZ;
				this.motionZ *= RbmkJumpHandler.PLATE_AIR_DRAG_XZ;
				if (this.motionY < 0.0D) {
					this.motionX *= RbmkJumpHandler.PLATE_DESCEND_DRAG_XZ;
					this.motionZ *= RbmkJumpHandler.PLATE_DESCEND_DRAG_XZ;
				}
				if (this.motionY > 0.0D) this.pushPath();
				if (this.blasted && this.motionY > 0.0D) this.breakPath();
				this.noClip = true;
				double beforeY = this.posY;
				this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);
				if (this.homeSet) {
					double limit = this.homeRadius + RbmkJumpHandler.PLATE_BLAST_MARGIN;
					double dx = this.posX - this.homeX;
					double dz = this.posZ - this.homeZ;
					double d2 = dx * dx + dz * dz;
					if (d2 > limit * limit) {
						double d = Math.sqrt(d2);
						double nx = dx / d;
						double nz = dz / d;
						this.posX = this.homeX + nx * limit;
						this.posZ = this.homeZ + nz * limit;
						double outward = this.motionX * nx + this.motionZ * nz;
						if (outward > 0.0D) {
							this.motionX -= nx * outward;
							this.motionZ -= nz * outward;
						}
					}
				}
				if (this.ntmitCoreSet) {
					double limitX = net.minecraft.util.math.MathHelper.clamp(this.posX, (double) this.ntmitCoreMinX + 0.5D, (double) this.ntmitCoreMaxX + 0.5D);
					double limitZ = net.minecraft.util.math.MathHelper.clamp(this.posZ, (double) this.ntmitCoreMinZ + 0.5D, (double) this.ntmitCoreMaxZ + 0.5D);
					if (limitX != this.posX) {
						this.posX = limitX;
						this.motionX = 0.0D;
					}
					if (limitZ != this.posZ) {
						this.posZ = limitZ;
						this.motionZ = 0.0D;
					}
				}
				if (this.motionY <= 0.0D && this.posY <= this.groundY) {
					this.posY = this.groundY;
					this.land();
				} else if (this.onGround && this.motionY <= 0.0D) {
					this.land();
				} else if (this.motionY <= 0.0D) {
					if (Math.abs(this.posY - beforeY) < 0.02D) {
						if (++this.stuckTicks > RbmkJumpHandler.PLATE_STUCK_TICKS) this.land();
					} else {
						this.stuckTicks = 0;
					}
				}
			}
			if (!this.world.isRemote) this.hitPlayers();
			if (this.world.isRemote) {
				this.clientSteam();
				this.clientSmoke();
			}
		} catch (Throwable throwable) {
			if (++this.errorTicks < 200) {
				if (this.errorTicks % 40 == 1) RbmkJumpHandler.diag("plate update error: " + throwable);
			} else {
				RbmkJumpHandler.diag("plate update error: " + throwable);
				if (!this.world.isRemote) {
					this.fracture();
				}
				this.setDead();
			}
		}
		super.onUpdate();
		this.refreshBounds();
	}

	private void refreshBounds() {
		if (this.count <= 0) return;
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
		this.homeRadius = (double) Math.max(Math.max(maxX + 1, -minX), Math.max(maxZ + 1, -minZ));
		if (!this.homeSet) {
			this.homeSet = true;
			this.homeX = this.posX;
			this.homeZ = this.posZ;
		}
		this.setEntityBoundingBox(new AxisAlignedBB(
				this.posX + (double) minX, this.posY + (double) minY, this.posZ + (double) minZ,
				this.posX + (double) maxX + 1.0D, this.posY + (double) maxY + 1.0D, this.posZ + (double) maxZ + 1.0D));
	}



	private void hitPlayers() {
		if (this.motionY == 0.0D) return;
		DamageSource source = this.motionY > 0.0D ? RbmkJumpHandler.LID_ASCEND : RbmkJumpHandler.LID_DESCEND;
		List<EntityPlayer> players = this.world.getEntitiesWithinAABB(EntityPlayer.class, this.crushBox());
		for (EntityPlayer player : players) {
			if (player.isDead || player.isCreative()) continue;
			player.attackEntityFrom(source, RbmkJumpHandler.PLATE_DAMAGE);
		}
	}

	private void land() {
		this.landed = true;
		this.wrecked = true;
		this.dataManager.set(PLATE_LANDED, Boolean.TRUE);
		this.dataManager.set(PLATE_WRECKED, Boolean.TRUE);
		this.motionX = 0.0D;
		this.motionY = 0.0D;
		this.motionZ = 0.0D;
		this.setPosition(this.posX, this.posY - RbmkJumpHandler.PLATE_SINK, this.posZ);
		if (this.world.isRemote) return;
		try {
			this.crater();
			BlockPos base = new BlockPos(this.posX, this.posY, this.posZ);
			for (int i = 0; i < 64; i++) {
				double dx = this.world.rand.nextGaussian() * 1.5D;
				double dz = this.world.rand.nextGaussian() * 1.5D;
				double x = this.posX + 0.5D + dx;
				double z = this.posZ + 0.5D + dz;
				double y = (double) this.world.getHeight((int) Math.floor(x), (int) Math.floor(z)) + 0.1D;
				this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, x, y, z, dx * 0.12D, 0.35D + this.world.rand.nextDouble() * 0.35D, dz * 0.12D, Block.getStateId(this.world.getBlockState(new BlockPos(x, y - 1.5D, z))));
			}
			this.world.playSound(null, this.posX, this.posY, this.posZ, com.hbm.lib.HBMSoundHandler.blockDebris, SoundCategory.BLOCKS, 7.0F, 0.55F + this.world.rand.nextFloat() * 0.2F);
			if (this.blasted) RbmkJumpHandler.shakeLand(this.world, this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D);
			this.scatterParts();
		} catch (Throwable throwable) {
			RbmkJumpHandler.diag("plate land error: " + throwable);
		}
	}

	private void crater() {
		RbmkJumpHandler.plateImpact(this.world, this.posX, this.posY, this.posZ);
	}

	private void breakPath() {
		double dy = this.motionY;
		int steps = Math.max(1, (int) Math.ceil(Math.abs(dy)));
		double step = dy / (double) steps;
		for (int i = 0; i < this.count; i++) {
			Vec3i offset = this.offset(i);
			int x = (int) Math.floor(this.posX + (double) offset.getX());
			int z = (int) Math.floor(this.posZ + (double) offset.getZ());
			for (int s = 0; s <= steps; s++) {
				int y = (int) Math.floor(this.posY + (double) offset.getY() + step * (double) s);
				for (int up = 0; up <= 1; up++) {
					BlockPos pos = new BlockPos(x, y + up, z);
					if (!this.pushable(pos, this.world.getBlockState(pos), false, !this.blasted)) continue;
					this.world.destroyBlock(pos, false);
				}
			}
		}
	}

	private void pushPath() {
		double target = this.posY + Math.max(this.motionY, 0.0D) + 1.0D;
		double from = Math.min(this.pushY, this.posY);
		double span = target - from;
		if (span <= 0.0D) return;
		int steps = Math.max(1, (int) Math.ceil(span));
		double step = span / (double) steps;
		int budget = RbmkJumpHandler.PLATE_PUSH_PER_TICK;
		for (int s = 1; s <= steps; s++) {
			double base = from + step * (double) s;
			for (int i = 0; i < this.count; i++) {
				Vec3i offset = this.offset(i);
				BlockPos pos = new BlockPos((int) Math.floor(this.posX + (double) offset.getX()), (int) Math.floor(base + (double) offset.getY()), (int) Math.floor(this.posZ + (double) offset.getZ()));
				if (this.clearBlock(pos, budget > 0)) {
					if (budget > 0) budget--;
				}
			}
		}
		this.pushY = target;
	}

	private boolean clearBlock(BlockPos pos, boolean visual) {
		IBlockState state = this.world.getBlockState(pos);
		if (!this.pushable(pos, state, true, !this.blasted)) return false;
		this.world.setBlockToAir(pos);
		if (!visual || this.world.isRemote) return true;
		EntityFallingBlock falling = new EntityFallingBlock(this.world, (double) pos.getX() + 0.5D, (double) pos.getY() + 0.5D, (double) pos.getZ() + 0.5D, state);
		falling.motionY = 0.2D + this.world.rand.nextDouble() * 0.3D;
		falling.motionX = (this.world.rand.nextDouble() - 0.5D) * 0.4D;
		falling.motionZ = (this.world.rand.nextDouble() - 0.5D) * 0.4D;
		this.world.spawnEntity(falling);
		return true;
	}

	private boolean pushable(BlockPos pos, IBlockState state, boolean force, boolean keepWreck) {
		Block block = state.getBlock();
		if (block.isAir(state, this.world, pos)) return false;
		if (state.getMaterial().isLiquid()) return false;
		if (state.getBlockHardness(this.world, pos) < 0.0F) return false;
		if (keepWreck && (block instanceof RBMKDebris || block instanceof CoriumFinite)) return false;
		if (force) return true;
		try {
			return block.getExplosionResistance(null) <= RbmkJumpHandler.PLATE_PATH_MAX_RESISTANCE;
		} catch (Throwable throwable) {
			return false;
		}
	}

	private void scatterParts() {
		if (NTMITWorldRules.entitySummonDisabled(this.world)) return;
		for (int i = 0; i < RbmkJumpHandler.PLATE_SCATTER_COUNT; i++) {
			EntityRBMKDebris.DebrisType type = i % 3 == 0 ? EntityRBMKDebris.DebrisType.LID : (i % 3 == 1 ? EntityRBMKDebris.DebrisType.FUEL : EntityRBMKDebris.DebrisType.BLANK);
			double angle = this.world.rand.nextDouble() * Math.PI * 2.0D;
			double speed = 0.25D + this.world.rand.nextDouble() * 0.5D;
			EntityRBMKDebris debris = new EntityRBMKDebris(this.world, this.posX + 0.5D, this.posY + 1.0D, this.posZ + 0.5D, type);
			debris.motionX = Math.cos(angle) * speed;
			debris.motionY = 0.3D + this.world.rand.nextDouble() * 0.5D;
			debris.motionZ = Math.sin(angle) * speed;
			this.world.spawnEntity(debris);
		}
	}

	private void solidify() {
		Block smolder = Block.getBlockFromName(RbmkJumpHandler.SMOLDER_BLOCK);
		if (smolder == null) return;
		IBlockState state = smolder.getDefaultState();
		for (int i = 0; i < this.count; i++) {
			Vec3i offset = this.offset(i);
			BlockPos pos = new BlockPos(Math.floor(this.posX + (double) offset.getX()), Math.floor(this.posY + (double) offset.getY()), Math.floor(this.posZ + (double) offset.getZ()));
			IBlockState existing = this.world.getBlockState(pos);
			if (!existing.getBlock().isReplaceable(this.world, pos) && !existing.getBlock().isAir(existing, this.world, pos)) continue;
			this.world.setBlockState(pos, state, 3);
		}
	}

	@SideOnly(Side.CLIENT)
	private void clientSteam() {
	}

	@SideOnly(Side.CLIENT)
	private void clientSmoke() {
		if (this.dataManager.get(PLATE_BLASTED).booleanValue() && !this.blastSeen) {
			this.blastSeen = true;
			this.smokeLandTicks = 0;
		}
		if (!this.dataManager.get(PLATE_LANDED).booleanValue()) {
			if (this.blastSeen && this.age > 1) this.spawnSmoke(RbmkJumpHandler.PLATE_SMOKE_TRAIL_THIN, RbmkJumpHandler.PLATE_SMOKE_RISE);
			return;
		}
		if (this.smokeLandTicks++ < RbmkJumpHandler.PLATE_SMOKE_LAND_TICKS) {
			this.spawnSmoke(RbmkJumpHandler.PLATE_SMOKE_LAND_PER_TICK, 0.05F);
		}
	}

	private int[] footprint() {
		int minX = 0;
		int maxX = 0;
		int minZ = 0;
		int maxZ = 0;
		for (int i = 0; i < this.count; i++) {
			Vec3i offset = this.offset(i);
			if (i == 0) {
				minX = maxX = offset.getX();
				minZ = maxZ = offset.getZ();
			}
			minX = Math.min(minX, offset.getX());
			maxX = Math.max(maxX, offset.getX());
			minZ = Math.min(minZ, offset.getZ());
			maxZ = Math.max(maxZ, offset.getZ());
		}
		return new int[] { minX, maxX, minZ, maxZ };
	}

	@SideOnly(Side.CLIENT)
	private void spawnSteam() {
		if (this.count <= 0) return;
		int[] box = this.footprint();
		float scale = Math.max(RbmkJumpHandler.PLATE_STEAM_SCALE_MIN, (float) (box[1] - box[0] + box[3] - box[2]) / 4.0F);
		for (int i = 0; i < RbmkJumpHandler.PLATE_STEAM_PER_TICK; i++) {
			double x = this.posX + (double) box[0] + this.world.rand.nextDouble() * (double) (box[1] - box[0] + 1);
			double y = this.posY + 1.0D + this.world.rand.nextDouble() * 0.6D;
			double z = this.posZ + (double) box[2] + this.world.rand.nextDouble() * (double) (box[3] - box[2] + 1);
			ParticleNTMITSteam particle = new ParticleNTMITSteam(this.world, x, y, z);
			if (this.dataManager.get(PLATE_RED).booleanValue()) particle.setColor(RbmkJumpHandler.RED_STEAM_COLOR);
			particle.setCloudScale(scale, scale * RbmkJumpHandler.PLATE_STEAM_SCALE_RATIO);
			particle.setLife(RbmkJumpHandler.PLATE_STEAM_LIFE);
			particle.motionX = (this.world.rand.nextDouble() - 0.5D) * 0.3D;
			particle.motionY = 0.55D + this.world.rand.nextDouble() * 0.7D;
			particle.motionZ = (this.world.rand.nextDouble() - 0.5D) * 0.3D;
			Minecraft.getMinecraft().effectRenderer.addEffect(particle);
		}
	}

	@SideOnly(Side.CLIENT)
	private void spawnSmoke(int count, float rise) {
		if (this.count <= 0) return;
		int[] box = this.footprint();
		float scale = Math.max(RbmkJumpHandler.PLATE_SMOKE_SCALE_MIN, (float) (box[1] - box[0] + box[3] - box[2]) / 5.0F);
		for (int i = 0; i < count; i++) {
			double x = this.posX + (double) box[0] + this.world.rand.nextDouble() * (double) (box[1] - box[0] + 1);
			double y = this.posY + 1.0D + this.world.rand.nextDouble() * 0.5D;
			double z = this.posZ + (double) box[2] + this.world.rand.nextDouble() * (double) (box[3] - box[2] + 1);
			ParticleNTMITSmoke particle = new ParticleNTMITSmoke(this.world, x, y, z);
			if (this.dataManager.get(PLATE_RED).booleanValue()) particle.setColor(RbmkJumpHandler.DARK_RED_SMOKE_COLOR);
			particle.setCloudScale(scale, scale * RbmkJumpHandler.PLATE_SMOKE_SCALE_RATIO);
			particle.setLife(RbmkJumpHandler.PLATE_SMOKE_LIFE);
			particle.motionX = (this.world.rand.nextDouble() - 0.5D) * 0.14D;
			particle.motionY = (double) rise + this.world.rand.nextDouble() * 0.12D;
			particle.motionZ = (this.world.rand.nextDouble() - 0.5D) * 0.14D;
			Minecraft.getMinecraft().effectRenderer.addEffect(particle);
		}
	}

	public void blast() {
		if (this.world.isRemote) return;
		this.landed = false;
		this.blasted = true;
		this.dataManager.set(PLATE_BLASTED, Boolean.TRUE);
		this.dataManager.set(PLATE_LANDED, Boolean.FALSE);
		this.age = 0;
		if (this.dataManager.get(PLATE_RED).booleanValue()) {
			double angle = this.world.rand.nextDouble() * Math.PI * 2.0D;
			this.motionY = RbmkJumpHandler.PLATE_BLAST_VY * RbmkJumpHandler.PLATE_SIDE_VY_RATIO;
			this.motionX = Math.cos(angle) * RbmkJumpHandler.PLATE_SIDE_SPEED;
			this.motionZ = Math.sin(angle) * RbmkJumpHandler.PLATE_SIDE_SPEED;
		} else {
			this.motionY = RbmkJumpHandler.PLATE_BLAST_VY;
			this.motionX = (this.world.rand.nextDouble() - 0.5D) * RbmkJumpHandler.PLATE_BLAST_SPREAD * 2.0D;
			this.motionZ = (this.world.rand.nextDouble() - 0.5D) * RbmkJumpHandler.PLATE_BLAST_SPREAD * 2.0D;
		}
		this.spin = (this.world.rand.nextBoolean() ? 1.0F : -1.0F) * RbmkJumpHandler.PLATE_SPIN * (0.7F + this.world.rand.nextFloat() * 0.8F);
		for (int i = 0; i < 40; i++) {
			double dx = this.world.rand.nextGaussian();
			double dz = this.world.rand.nextGaussian();
			this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, this.posX + 0.5D + dx, this.posY + 0.5D + this.world.rand.nextDouble(), this.posZ + 0.5D + dz, dx * 0.2D, 0.4D, dz * 0.2D, Block.getStateId(this.world.getBlockState(new BlockPos(this.posX, this.posY - 0.5D, this.posZ))));
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, com.hbm.lib.HBMSoundHandler.blockDebris, SoundCategory.BLOCKS, 9.0F, 0.5F);
	}

	public void fracture() {
		if (this.world.isRemote) return;
		try {
			for (int i = 0; i < this.count; i++) {
			if (NTMITWorldRules.entitySummonDisabled(this.world)) break;
			BlockPos offset = this.offset(i);
			double x = this.posX + offset.getX() + 0.5D;
			double y = this.posY + offset.getY() + 0.5D;
			double z = this.posZ + offset.getZ() + 0.5D;
			EntityRBMKDebris debris = new EntityRBMKDebris(this.world, x, y, z, EntityRBMKDebris.DebrisType.BLANK);
			debris.motionX = (this.world.rand.nextDouble() - 0.5D) * RbmkJumpHandler.PLATE_FRACTURE_SPEED * 2.0D;
			debris.motionY = 0.35D + this.world.rand.nextDouble() * RbmkJumpHandler.PLATE_FRACTURE_SPEED;
			debris.motionZ = (this.world.rand.nextDouble() - 0.5D) * RbmkJumpHandler.PLATE_FRACTURE_SPEED * 2.0D;
			this.world.spawnEntity(debris);
		}
		BlockPos base = new BlockPos(this.posX, this.posY + 1.0D, this.posZ);
		for (int i = 0; i < 40; i++) {
			double dx = this.world.rand.nextGaussian();
			double dz = this.world.rand.nextGaussian();
			this.world.spawnParticle(EnumParticleTypes.BLOCK_DUST, this.posX + 0.5D + dx, this.posY + 0.5D + this.world.rand.nextDouble(), this.posZ + 0.5D + dz, dx * 0.2D, 0.4D, dz * 0.2D, Block.getStateId(this.world.getBlockState(base)));
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, com.hbm.lib.HBMSoundHandler.blockDebris, SoundCategory.BLOCKS, 8.0F, 0.7F);
		} catch (Throwable throwable) {
			RbmkJumpHandler.diag("plate fracture error: " + throwable);
		}
		this.setDead();
	}

	@Override
	public boolean processInitialInteract(EntityPlayer player, EnumHand hand) {
		if (this.world.isRemote || this.isDead) return true;
		ItemStack held = player.getHeldItem(hand);
		if (!held.isEmpty() && held.getItem() instanceof ItemTooling && ((ItemTooling) held.getItem()).getType() == com.hbm.api.block.IToolable.ToolType.SCREWDRIVER) {
			this.dismantle(player);
			return true;
		}
		this.fracture();
		return true;
	}

	@Override
	public net.minecraft.util.EnumActionResult applyPlayerInteraction(EntityPlayer player, Vec3d vec, EnumHand hand) {
		this.processInitialInteract(player, hand);
		return net.minecraft.util.EnumActionResult.SUCCESS;
	}

	private void dismantle(EntityPlayer player) {
		int bars = Math.min(this.count, RbmkJumpHandler.PLATE_DISMANTLE_DEBRIS);
		int given = bars;
		while (bars > 0) {
			int stack = Math.min(bars, 64);
			bars -= stack;
			ItemStack loot = new ItemStack(ModItems.debris_metal, stack);
			if (player == null || !player.inventory.addItemStackToInventory(loot)) {
				this.world.spawnEntity(new net.minecraft.entity.item.EntityItem(this.world, this.posX + 0.5D, this.posY + 1.0D, this.posZ + 0.5D, loot));
			}
		}
		this.world.playSound(null, this.posX, this.posY, this.posZ, com.hbm.lib.HBMSoundHandler.blockDebris, SoundCategory.BLOCKS, 6.0F, 0.9F);
		RbmkJumpHandler.diag("plate dismantled at " + this.getPosition() + " bars=" + given);
		this.setDead();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound compound) {
		this.count = compound.getInteger("count");
		this.states = compound.getIntArray("states");
		this.offsets = compound.getByteArray("offsets");
		this.tints = compound.getIntArray("tints");
		int[] liftBits = compound.getIntArray("lifts");
		this.lifts = new float[liftBits.length];
		for (int i = 0; i < liftBits.length; i++) this.lifts[i] = Float.intBitsToFloat(liftBits[i]);
		this.rodColors = compound.getByteArray("rodColors");
		this.landed = compound.getBoolean("landed");
		this.blasted = compound.getBoolean("blasted");
		this.wrecked = compound.getBoolean("wrecked");
		this.age = compound.getInteger("age");
		this.roll = compound.getFloat("roll");
		this.tumble = compound.getFloat("tumble");
		this.spin = compound.getFloat("spin");
		this.pushY = compound.getDouble("pushY");
		this.groundY = compound.hasKey("groundY") ? compound.getDouble("groundY") : this.posY;
		this.dataManager.set(PLATE_BLASTED, Boolean.valueOf(this.blasted));
		this.dataManager.set(PLATE_LANDED, Boolean.valueOf(this.landed));
		this.dataManager.set(PLATE_WRECKED, Boolean.valueOf(this.wrecked));
		this.dataManager.set(PLATE_ROLL, Float.valueOf(this.roll));
		this.dataManager.set(PLATE_TUMBLE, Float.valueOf(this.tumble));
		this.dataManager.set(PLATE_AGE, Integer.valueOf(this.age));
		this.count = Math.min(this.count, Math.min(this.states.length, Math.min(this.tints.length, Math.min(this.lifts.length, Math.min(this.rodColors.length, this.offsets.length / 3)))));
		this.sanitize();
		this.applySize();
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound compound) {
		compound.setInteger("count", this.count);
		compound.setIntArray("states", this.states);
		compound.setByteArray("offsets", this.offsets);
		compound.setIntArray("tints", this.tints);
		int[] liftBits = new int[this.lifts.length];
		for (int i = 0; i < liftBits.length; i++) liftBits[i] = Float.floatToRawIntBits(this.lifts[i]);
		compound.setIntArray("lifts", liftBits);
		compound.setByteArray("rodColors", this.rodColors);
		compound.setBoolean("landed", this.landed);
		compound.setBoolean("blasted", this.blasted);
		compound.setBoolean("wrecked", this.wrecked);
		compound.setInteger("age", this.age);
		compound.setFloat("roll", this.roll);
		compound.setFloat("tumble", this.tumble);
		compound.setFloat("spin", this.spin);
		compound.setDouble("pushY", this.pushY);
		compound.setDouble("groundY", this.groundY);
	}

	@Override
	public void writeSpawnData(ByteBuf buffer) {
		buffer.writeInt(this.count);
		for (int i = 0; i < this.count; i++) buffer.writeInt(this.states[i]);
		for (int i = 0; i < this.count; i++) buffer.writeInt(this.tints[i]);
		for (int i = 0; i < this.count; i++) buffer.writeFloat(this.lifts[i]);
		buffer.writeBytes(this.rodColors);
		buffer.writeBytes(this.offsets);
		buffer.writeFloat(this.roll);
		buffer.writeFloat(this.tumble);
		buffer.writeFloat(this.spin);
	}

	@Override
	public void readSpawnData(ByteBuf buffer) {
		this.count = buffer.readInt();
		this.states = new int[this.count];
		for (int i = 0; i < this.count; i++) this.states[i] = buffer.readInt();
		this.tints = new int[this.count];
		for (int i = 0; i < this.count; i++) this.tints[i] = buffer.readInt();
		this.lifts = new float[this.count];
		for (int i = 0; i < this.count; i++) this.lifts[i] = buffer.readFloat();
		this.rodColors = new byte[this.count];
		buffer.readBytes(this.rodColors);
		this.offsets = new byte[this.count * 3];
		buffer.readBytes(this.offsets);
		this.roll = buffer.readFloat();
		this.tumble = buffer.readFloat();
		this.spin = buffer.readFloat();
		this.prevRoll = this.roll;
		this.prevTumble = this.tumble;
		this.dataManager.set(PLATE_ROLL, Float.valueOf(this.roll));
		this.dataManager.set(PLATE_TUMBLE, Float.valueOf(this.tumble));
		this.dataManager.set(PLATE_AGE, Integer.valueOf(this.age));
		this.applySize();
	}

	@Override
	public boolean isInRangeToRenderDist(double distance) {
		return true;
	}

	@Override
	public boolean isEntityInvulnerable(DamageSource source) {
		return true;
	}

	@Override
	public boolean attackEntityFrom(DamageSource source, float amount) {
		return false;
	}

	@Override
	public void setFire(int seconds) {
	}

	@Override
	public boolean canBeCollidedWith() {
		return true;
	}

	@Override
	public boolean canBePushed() {
		return false;
	}

	@Override
	public void applyEntityCollision(Entity entity) {
	}
}
