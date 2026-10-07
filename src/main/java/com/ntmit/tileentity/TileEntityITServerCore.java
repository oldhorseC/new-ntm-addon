package com.ntmit.tileentity;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyReceiverMK2;
import com.hbm.api.tile.IHeatSource;
import com.hbm.inventory.control_panel.ControlEvent;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValueFloat;
import com.hbm.inventory.control_panel.types.DataValueString;
import com.hbm.interfaces.IControlReceiver;
import com.hbm.inventory.control_panel.IControllable;
import com.hbm.items.tool.ItemRBMKTool;
import com.hbm.items.tool.ItemMultiDetonator;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKBase;
import com.hbm.tileentity.network.RTTYSystem;
import com.hbm.lib.Library;
import com.hbm.lib.DirPos;
import com.hbm.lib.ForgeDirection;
import com.hbm.tileentity.IGUIProvider;
import com.hbm.tileentity.TileEntityLoadedBase;
import com.ntmit.blocks.BlockITServerColumn;
import com.ntmit.inventory.container.ContainerITServer;
import com.ntmit.inventory.container.ContainerITServerRBMK;
import com.ntmit.inventory.gui.GUITITServer;
import com.ntmit.inventory.gui.GuiITServerRBMK;
import com.ntmit.items.ItemITServerConnector;
import com.ntmit.items.ModItems;
import com.ntmit.main.ModGuiHandler;
import com.ntmit.main.NTMITMod;
import com.ntmit.server.ServerConfig;
import com.ntmit.server.data.IServerSource;
import com.ntmit.server.data.ServerSources;
import net.minecraft.entity.player.EntityPlayerMP;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLNetworkHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.ItemStackHandler;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class TileEntityITServerCore extends TileEntityLoadedBase implements ITickable, IEnergyReceiverMK2, IHeatSource, IControllable, IGUIProvider, IControlReceiver {

	public static final int SERVER_VERSION = 1;

	public static final String FIELD_COMPUTE = "server.compute";
	public static final String FIELD_COMPUTE_UNITS = "server.compute_units";
	public static final String FIELD_HEAT = "server.heat";
	public static final String FIELD_MAX_HEAT = "server.max_heat";
	public static final String FIELD_COOLING = "server.cooling";
	public static final String FIELD_POWER = "server.power";
	public static final String FIELD_MAX_POWER = "server.max_power";
	public static final String FIELD_STATE = "server.state";
	public static final String FIELD_DATA_CLASSES = "server.data_classes";

	public enum State {
		RUNNING, THROTTLED, HALTED, UNDER_POWER;

		public String getTranslationKey() {
			return "gui.ntm-it.server.state." + name().toLowerCase(Locale.US);
		}
	}

	public long power;
	public long maxPower = ServerConfig.maxPower;

	public final ItemStackHandler batterySlot = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return !stack.isEmpty() && stack.getItem() instanceof IBatteryItem;
		}
	};

	public final ItemStackHandler connectorSlot = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return !stack.isEmpty() && (stack.getItem() == ModItems.server_connector || stack.getItem() instanceof ItemRBMKTool);
		}

		@Override
		protected void onContentsChanged(int slot) {
			markDirty();
		}
	};

	private Map<String, String> connectorValues = new LinkedHashMap<>();
	private String connectorDevice = "";

	private String lastConnectorSignature = "";

	public final ItemStackHandler rbmkToolSlot = new ItemStackHandler(1) {
		@Override
		public boolean isItemValid(int slot, ItemStack stack) {
			return !stack.isEmpty() && (stack.getItem() instanceof ItemRBMKTool
					|| stack.getItem() == ModItems.server_connector
					|| stack.getItem() instanceof ItemMultiDetonator);
		}

		@Override
		protected void onContentsChanged(int slot) {

			markDirty();
		}
	};

	public static final int RBMK_GRID = 15;
	public static final int RBMK_RADIUS = 7;

	private BlockPos rbmkTarget = null;

	private RBMKColumn[] rbmkColumns = new RBMKColumn[RBMK_GRID * RBMK_GRID];

	private String rbmkImportError = "";
	private boolean rbmkImportAsked = false;

	public enum RbmkDebugMode {
		COLUMN_HEAT("HEAT", "热量", "第一根被选柱体的热量", false),
		COLUMN_MAX_HEAT("MAX", "单柱上限", "该柱体的热量上限；同型柱体都一样", true),
		COLUMN_TYPE("TYPE", "柱体类型", "柱体类型编号，不是物理量", true),
		SELECTED("NUM", "选中数量", "当前选中的柱体根数，调试用", true),
		TOTAL_HEAT("SUM", "总热量", "所有被选柱体的热量之和", false),
		AVG_HEAT("AVG", "平均热量", "所有被选柱体热量的平均值", false),
		PEAK_HEAT("PEAK", "最高热量", "被选柱体中最热的那一根的热量", false),
		MODERATED("MOD", "慢化", "柱体是否被慢化：0 否，1 是", false),
		REASIM_WATER("RW", "水冷通量", "柱体水冷通道里的水量", false),
		REASIM_STEAM("RS", "蒸汽通量", "柱体蒸汽通道里的蒸汽量", false),
		FUEL_ENRICHMENT("ENR", "燃料富集", "燃料柱体的富集度；原始值 0~1，对外显示/广播按百分比 ×100", false),
		FUEL_XENON("XEN", "氙毒", "燃料柱体积攒的氙毒", false),
		FUEL_HEAT("FH", "燃料热量", "燃料柱体自身的实际热量", false),
		ROD_LEVEL("ROD", "控制棒深度", "控制棒插入深度；原始值 0~1，对外显示/广播按百分比 ×100", false),
		BOILER_WATER("BW", "锅炉水量", "锅炉柱体里的水量", false),
		BOILER_STEAM("BS", "锅炉蒸汽", "锅炉柱体里的蒸汽量", false),
		COOLER_CRYO("CRY", "低温工质", "冷却柱体里的低温流体量", false),
		COOLER_HOT("HOT", "高温工质", "冷却柱体里的高温流体量", false),
		HEATER_WATER("HW", "预热水量", "预热器柱体里的水量", false),
		HEATER_STEAM("HS", "预热蒸汽", "预热器柱体里的蒸汽量", false),
		OUTGASSER_PROGRESS("PRG", "排气进度", "排气柱体已完成的进度", false),
		OUTGASSER_FLUX("FLX", "消耗通量", "排气柱体消耗掉的中子通量", false),
		OUTGASSER_GAS("GAS", "排气存量", "排气柱体已产出的气体量", false);

		public final String label;

		public final String name;

		public final String note;

		public final boolean duplicate;

		RbmkDebugMode(String label, String name, String note, boolean duplicate) {
			this.label = label;
			this.name = name;
			this.note = note;
			this.duplicate = duplicate;
		}

		public boolean isFraction() {
			return this == ROD_LEVEL || this == FUEL_ENRICHMENT;
		}

		/**
		 * 分数型模式的对外口径：统一乘 100 变成百分比，其余模式原样返回。
		 * 仅用于 RTTY 广播与 .text 文本；CC/OC 查询仍按 "raw + .display" 双键给出，
		 * 存储值与协议格式都不因此改变。
		 */
		public double displayValue(double raw) {
			return this.isFraction() ? raw * 100.0D : raw;
		}
	}

	public static final int SEND_ROR = 0;
	public static final int SEND_MOUNT = 1;

	public static String sendLabel(int send) {
		return send == SEND_MOUNT ? "挂载" : "ROR";
	}

	public static final class RbmkSample {

		public final int mode;
		public final int[] cols;

		public final int send;
		public final String signal;

		double value;

		RbmkSample(int mode, int[] cols, int send, String signal) {
			this.mode = mode;
			this.cols = cols;
			this.send = send;
			this.signal = signal == null ? "" : signal;
		}

		boolean sameAs(RbmkSample other) {
			if (other == null || other.mode != this.mode || other.send != this.send) return false;
			if (!this.signal.equals(other.signal) || this.cols.length != other.cols.length) return false;
			for (int i = 0; i < this.cols.length; i++) {
				if (this.cols[i] != other.cols[i]) return false;
			}
			return true;
		}
	}

	private List<RbmkSample> rbmkSamples = new ArrayList<>();

	public List<RbmkSample> getRbmkSamples() {
		return this.rbmkSamples;
	}

	public int getRbmkSend() {
		return this.rbmkSend;
	}

	public String getRbmkSignal() {
		return this.rbmkSignal;
	}

	public static final String DEBUG_CHANNEL = "debug";

	private double rbmkDebugValue;
	private int rbmkDebugMode;

	private int rbmkSend = SEND_ROR;
	private String rbmkSignal = "debug";

	private int rbmkDebugSeq;

	private int rbmkActiveSample = -1;

	private boolean rbmkResumePending = false;

	private boolean rbmkFirstTick = true;

	private final class DebugQueryMap extends LinkedHashMap<String, DataValue> {

		private long lastLog;

		@Override
		public DataValue get(Object key) {
			DataValue value = super.get(key);

			long now = System.currentTimeMillis();
			if (now - this.lastLog >= 1000L) {
				this.lastLog = now;
			}

			return value;
		}
	}

	private int[] rbmkDebugCols = new int[0];
	private boolean rbmkDebugActive = false;

	public static final String DEBUG_PANEL_NAME = "debug";
	private boolean rbmkDebugRegistered = false;

	public static final int MODE_COLLECT = 0;
	public static final int MODE_DISPATCH = 1;

	private String connectorChannel = "";
	private int connectorMode = MODE_COLLECT;

	private List<String> connectorSelected = Collections.emptyList();

	public int heat;
	public int maxHeat = ServerConfig.maxHeat;

	public int tick;

	private State state = State.RUNNING;

	private int compute;
	private final List<TileEntityITServerCompute> computeUnits = new ArrayList<>();

	private final List<TileEntityITServerHeatExchanger> heatTargets = new ArrayList<>();
	private int cooling;

	private DirPos[] conPos;

	private int computeUnitCount;
	private int heatTargetCount;
	private boolean coolantPresent;

	@Override
	public Container provideContainer(int ID, EntityPlayer player, World world, int x, int y, int z) {

		if (ID == ModGuiHandler.ID_SERVER_RBMK) return new ContainerITServerRBMK(player.inventory, this);
		return ID == ModGuiHandler.ID_SERVER ? new ContainerITServer(player.inventory, this) : null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen provideGUI(int ID, EntityPlayer player, World world, int x, int y, int z) {
		if (ID == ModGuiHandler.ID_SERVER_RBMK) return new GuiITServerRBMK(player.inventory, this);
		return ID == ModGuiHandler.ID_SERVER ? new GUITITServer(player.inventory, this) : null;
	}

	@Override
	public void update() {
		if (world.isRemote) return;

		this.tick++;

		if (this.rbmkResumePending || this.rbmkFirstTick) {
			this.rbmkResumePending = false;
			this.rbmkFirstTick = false;
			if (!this.rbmkSamples.isEmpty()) this.resumeSamples();
		}

		if (this.conPos == null) this.conPos = buildConPos();
		for (DirPos con : this.conPos) {
			this.trySubscribe(world, con);
		}

		this.dischargeBattery();

		long cost = this.getPowerCost();
		boolean paid = this.power >= cost;
		this.power = Math.max(0L, this.power - cost);

		if (this.tick % ServerConfig.groupRefreshInterval == 0) {
			this.refreshGroup();
			this.networkPackNT(20);
		}

		if (this.tick % ServerConfig.connectorPollInterval == 0) {
			this.pollConnector();
			this.networkPackNT(20);
		}

		if (this.rbmkTarget != null && this.tick % 10 == 0) {
			this.rescanRbmk();
			this.networkPackNT(20);
		}

		if (this.rbmkTarget != null) {
			this.refreshDebugReading();
			if (this.rbmkDebugActive) this.sendDebugReading();
			this.refreshSamples();

			if (this.tick % 2 == 0) this.networkPackNT(20);
		}

		this.cooling = 0;
		for (TileEntityITServerHeatExchanger exchanger : this.heatTargets) {
			if (exchanger.isGroupMember()) continue;
			this.cooling += exchanger.getCoolingRate();
		}

		int ceiling = (int) (this.maxHeat * 1.5D);
		if (paid && this.heat < ceiling) {
			this.heat = Math.min(ceiling, this.heat + (int) Math.round(ServerConfig.heatPerTick(this.compute)));
		}

		this.state = this.evaluateState();

		this.markDirty();
	}

	private DirPos[] buildConPos() {
		return new DirPos[] {
				new DirPos(pos.getX() + ForgeDirection.DOWN.offsetX, pos.getY() + ForgeDirection.DOWN.offsetY, pos.getZ() + ForgeDirection.DOWN.offsetZ, ForgeDirection.DOWN),
				new DirPos(pos.getX() + ForgeDirection.UP.offsetX, pos.getY() + ForgeDirection.UP.offsetY, pos.getZ() + ForgeDirection.UP.offsetZ, ForgeDirection.UP),
				new DirPos(pos.getX() + ForgeDirection.NORTH.offsetX, pos.getY() + ForgeDirection.NORTH.offsetY, pos.getZ() + ForgeDirection.NORTH.offsetZ, ForgeDirection.NORTH),
				new DirPos(pos.getX() + ForgeDirection.SOUTH.offsetX, pos.getY() + ForgeDirection.SOUTH.offsetY, pos.getZ() + ForgeDirection.SOUTH.offsetZ, ForgeDirection.SOUTH),
				new DirPos(pos.getX() + ForgeDirection.WEST.offsetX, pos.getY() + ForgeDirection.WEST.offsetY, pos.getZ() + ForgeDirection.WEST.offsetZ, ForgeDirection.WEST),
				new DirPos(pos.getX() + ForgeDirection.EAST.offsetX, pos.getY() + ForgeDirection.EAST.offsetY, pos.getZ() + ForgeDirection.EAST.offsetZ, ForgeDirection.EAST)
		};
	}

	private State evaluateState() {
		if (this.heat >= this.maxHeat * 1.5D || this.power <= 0) return State.HALTED;
		if (this.heat >= this.maxHeat) return State.THROTTLED;
		if (this.power < getPowerCost()) return State.UNDER_POWER;
		return State.RUNNING;
	}

	public long getPowerCost() {
		return Math.round(ServerConfig.powerDraw(this.compute)) + (long) activePollTasks() * ServerConfig.powerPerTask;
	}

	private int activePollTasks() {
		return 0;
	}

	private void refreshGroup() {
		this.computeUnits.clear();
		this.heatTargets.clear();
		this.compute = 0;

		Set<BlockPos> visited = new HashSet<>();
		Deque<BlockPos> queue = new ArrayDeque<>();
		visited.add(this.pos);
		queue.add(this.pos);

		while (!queue.isEmpty() && this.computeUnits.size() < ServerConfig.computeSlots) {
			BlockPos node = queue.poll();

			for (ForgeDirection side : BlockITServerColumn.sidesOf(BlockITServerColumn.facingOf(world, node))) {
				BlockPos neighbour = BlockITServerColumn.offset(node, side);

				if (!world.isBlockLoaded(neighbour) || !visited.add(neighbour)) continue;

				TileEntity tile = world.isBlockLoaded(neighbour) ? world.getTileEntity(neighbour) : null;
				if (!(tile instanceof TileEntityITServerCompute unit)) continue;

				this.computeUnits.add(unit);
				this.compute += unit.getComputePoints();
				if (this.computeUnits.size() >= ServerConfig.computeSlots) break;
				queue.add(neighbour);
			}
		}

		collectHeatTarget(this.pos);
		for (TileEntityITServerCompute unit : this.computeUnits) {
			collectHeatTarget(unit.getPos());
		}

		this.computeUnitCount = this.computeUnits.size();
		this.heatTargetCount = this.heatTargets.size();

		this.coolantPresent = false;
		for (TileEntityITServerHeatExchanger exchanger : this.heatTargets) {
			if (exchanger.hasCoolant()) {
				this.coolantPresent = true;
				break;
			}
		}
	}

	private void collectHeatTarget(BlockPos unitPos) {
		ForgeDirection back = BlockITServerColumn.facingOf(world, unitPos).getOpposite();
		BlockPos targetPos = BlockITServerColumn.offset(unitPos, back);

		if (!world.isBlockLoaded(targetPos)) return;

		TileEntity tile = world.isBlockLoaded(targetPos) ? world.getTileEntity(targetPos) : null;
		if (tile instanceof TileEntityITServerHeatExchanger exchanger) {
			this.heatTargets.add(exchanger);
		}
	}

	@Override
	public long getPower() {
		return this.power;
	}

	@Override
	public void setPower(long power) {
		this.power = power;
	}

	@Override
	public long getMaxPower() {
		return this.maxPower;
	}

	@Override
	public long getReceiverSpeed() {
		return Math.max(1L, Math.min(this.maxPower, getPowerCost() * 20L));
	}

	@Override
	public IEnergyReceiverMK2.ConnectionPriority getPriority() {
		return IEnergyReceiverMK2.ConnectionPriority.HIGH;
	}

	public ItemStackHandler getBatterySlot() {
		return this.batterySlot;
	}

	public ItemStackHandler getConnectorSlot() {
		return this.connectorSlot;
	}

	public ItemStack getConnectorStack() {
		return this.connectorSlot.getStackInSlot(0);
	}

	public ItemStack getRbmkToolStack() {
		return this.rbmkToolSlot.getStackInSlot(0);
	}

	/**
	 * RBMK 工具槽里若放着 HBM 的「多发起爆器」，返回它绑定过的柱体坐标。
	 *
	 * <p>绑定数据存在物品自身的 NBT（xValues / yValues / zValues），
	 * 而该槽的 NBT 已随本 TE 同步到客户端，因此两端调用都可用。</p>
	 */
	public List<BlockPos> getBoundRbmkPositions() {
		ItemStack stack = this.getRbmkToolStack();
		if (stack.isEmpty() || !(stack.getItem() instanceof ItemMultiDetonator)) return Collections.emptyList();

		int[][] locs = ItemMultiDetonator.getLocations(stack);
		if (locs == null || locs.length < 3 || locs[0] == null) return Collections.emptyList();

		List<BlockPos> list = new ArrayList<>(locs[0].length);
		for (int i = 0; i < locs[0].length; i++) {
			list.add(new BlockPos(locs[0][i], locs[1][i], locs[2][i]));
		}
		return list;
	}

	public ItemStackHandler getRbmkToolSlot() {
		return this.rbmkToolSlot;
	}

	private void rescanRbmk() {
		if (this.rbmkTarget == null) return;

		if (this.scanLayerAt(this.rbmkTarget.getY()) > 0) return;
		if (this.scanLayerAt(this.rbmkTarget.getY() - 1) > 0) return;
		this.scanLayerAt(this.rbmkTarget.getY() + 1);
	}

	private int scanLayerAt(int y) {
		int found = 0;

		for (int index = 0; index < this.rbmkColumns.length; index++) {
			BlockPos pos = new BlockPos(this.rbmkTarget.getX() + rbmkX(index), y, this.rbmkTarget.getZ() + rbmkZ(index));
			TileEntity tile = this.world.isBlockLoaded(pos) ? this.world.getTileEntity(pos) : null;
			RBMKColumn column = tile instanceof TileEntityRBMKBase base ? base.getConsoleData() : null;
			this.rbmkColumns[index] = column;
			if (column != null) found++;
		}

		return found;
	}

	public static int rbmkX(int index) {
		return index % RBMK_GRID - RBMK_RADIUS;
	}

	public static int rbmkZ(int index) {
		return index / RBMK_GRID - RBMK_RADIUS;
	}

	private boolean importRbmkTarget() {

		ItemStack stack = this.getRbmkToolStack();

		this.rbmkImportAsked = true;
		this.rbmkImportError = "";

		NBTTagCompound nbt = stack.getTagCompound();

		if (stack.getItem() instanceof ItemRBMKTool && nbt != null && nbt.hasKey("posX")) {
			this.rbmkTarget = new BlockPos(nbt.getInteger("posX"), nbt.getInteger("posY"), nbt.getInteger("posZ"));
		} else if (!stack.isEmpty() && ItemITServerConnector.isBound(stack)
				&& ItemITServerConnector.getDimension(stack) == this.world.provider.getDimension()) {
			this.rbmkTarget = ItemITServerConnector.getPos(stack);
		} else {
			this.rbmkImportError = stack.isEmpty() ? "slotempty" : "notlinked";
			return false;
		}

		this.rescanRbmk();
		this.debugRbmkScan(stack, this.rbmkTarget);

		if (!hasRbmkColumns()) this.rbmkImportError = "nocolumns";

		this.markDirty();
		this.networkPackNT(20);
		return true;
	}

	private boolean hasRbmkColumns() {
		for (RBMKColumn column : this.rbmkColumns) {
			if (column != null) return true;
		}
		return false;
	}

	public boolean getRbmkImportAsked() {
		return this.rbmkImportAsked;
	}

	public String getRbmkImportError() {
		return this.rbmkImportError;
	}

	private void debugRbmkScan(ItemStack stack, BlockPos target) {
		int[] perLayer = new int[3];
		for (int dy = -1; dy <= 1; dy++) {
			for (int index = 0; index < this.rbmkColumns.length; index++) {
				BlockPos pos = new BlockPos(target.getX() + rbmkX(index), target.getY() + dy, target.getZ() + rbmkZ(index));
				if (this.world.isBlockLoaded(pos) && this.world.getTileEntity(pos) instanceof TileEntityRBMKBase) perLayer[dy + 1]++;
			}
		}

		int found = 0;
		StringBuilder sample = new StringBuilder();
		for (int index = 0; index < this.rbmkColumns.length; index++) {
			RBMKColumn column = this.rbmkColumns[index];
			if (column == null) continue;
			found++;
			if (found <= 4) sample.append(" [i=").append(index).append(' ').append(column.type).append(" heat=").append((int) column.heat).append(']');
		}

	}

	public BlockPos getRbmkTarget() {
		return this.rbmkTarget;
	}

	public RBMKColumn[] getRbmkColumns() {
		return this.rbmkColumns;
	}

	private void pollConnector() {
		Map<String, String> found = new LinkedHashMap<>();

		ItemStack stack = this.getConnectorStack();
		if (stack.isEmpty() || !ItemITServerConnector.isBound(stack)) {
			this.publishConnector(found, "");
			return;
		}

		if (ItemITServerConnector.getDimension(stack) != this.world.provider.getDimension()) {
			this.publishConnector(found, "");
			return;
		}

		BlockPos pos = ItemITServerConnector.getPos(stack);
		if (!this.world.isBlockLoaded(pos)) {
			this.publishConnector(found, "");
			return;
		}

		IServerSource source = ServerSources.of(this.world.getTileEntity(pos));
		if (source == null) {
			this.publishConnector(found, "");
			return;
		}

		for (Map.Entry<String, DataValue> entry : source.listFields(this.world).entrySet()) {
			found.put(entry.getKey(), entry.getValue() == null ? "-" : entry.getValue().toString());
		}

		this.publishConnector(found, ItemITServerConnector.getDeviceType(stack));
	}

	private void publishConnector(Map<String, String> found, String device) {
		this.connectorValues = found;
		this.connectorDevice = device;

		String signature = String.join(",", found.keySet());
		if (!signature.equals(this.lastConnectorSignature)) {
			this.lastConnectorSignature = signature;
		}
	}

	public Map<String, String> getConnectorValues() {
		return this.connectorValues;
	}

	public String getConnectorDevice() {
		return this.connectorDevice;
	}

	public String getConnectorChannel() {
		return this.connectorChannel;
	}

	public int getConnectorMode() {
		return this.connectorMode;
	}

	public List<String> getConnectorSelected() {
		return this.connectorSelected;
	}

	@Override
	public boolean hasPermission(EntityPlayer player) {
		return player.getDistanceSq(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) < 256.0D;
	}

	@Override
	public void receiveControl(EntityPlayerMP player, NBTTagCompound data) {
		if (data.getBoolean("openRbmkPage")) {
			FMLNetworkHandler.openGui(player, NTMITMod.instance, ModGuiHandler.ID_SERVER_RBMK, player.world, this.pos.getX(), this.pos.getY(), this.pos.getZ());
			return;
		}

		if (data.hasKey("channel")) {
			String channel = data.getString("channel").trim();
			this.connectorChannel = channel.length() > 24 ? channel.substring(0, 24) : channel;
		}
		if (data.hasKey("mode")) {
			this.connectorMode = data.getInteger("mode") == MODE_DISPATCH ? MODE_DISPATCH : MODE_COLLECT;
		}
		if (data.hasKey("selected")) {
			List<String> selected = new ArrayList<>();
			NBTTagList list = data.getTagList("selected", 8);
			for (int i = 0; i < list.tagCount(); i++) {
				selected.add(list.getStringTagAt(i));
			}
			this.connectorSelected = selected;
		}

		if (data.getBoolean("importRbmk")) {
			boolean found = this.importRbmkTarget();

		}

		if (data.getBoolean("rbmkDebug")) {
			this.debugBroadcast(data.getInteger("mode"), data.getIntArray("cols"));
		}

		if (data.getBoolean("rbmkRegister")) {
			this.debugRegister(data.getInteger("mode"), data.getIntArray("cols"),
					data.getInteger("send"), data.getString("signal"));
		}

		if (data.hasKey("rbmkSampleRemove")) {
			this.debugUnregister(data.getInteger("rbmkSampleRemove"));
		}

		if (data.hasKey("rbmkSampleLoad")) {
			this.debugLoadSample(data.getInteger("rbmkSampleLoad"));
		}

		if (data.getBoolean("rbmkSampleQuery")) {
			this.rbmkDebugSeq++;
			this.networkPackNT(20);
		}

		this.markDirty();
		this.networkPackNT(20);
	}

	private void debugBroadcast(int modeOrdinal, int[] selected) {
		this.rbmkDebugMode = Math.floorMod(modeOrdinal, RbmkDebugMode.values().length);
		this.rbmkDebugCols = selected;
		this.rbmkDebugActive = true;

		this.refreshDebugReading();
		if (this.rbmkDebugActive) this.sendDebugReading();
	}

	private void debugRegister(int modeOrdinal, int[] selected, int send, String signal) {

		this.rbmkDebugMode = Math.floorMod(modeOrdinal, RbmkDebugMode.values().length);
		this.rbmkDebugCols = selected;
		this.rbmkDebugRegistered = true;
		this.rbmkSend = send == SEND_MOUNT ? SEND_MOUNT : SEND_ROR;
		this.rbmkSignal = signal == null ? "" : signal;
		this.rbmkActiveSample = this.rememberSample(this.rbmkDebugMode, selected, this.rbmkSend, this.rbmkSignal);

		this.networkPackNT(20);
	}

	private int rememberSample(int mode, int[] cols, int send, String signal) {
		RbmkSample sample = new RbmkSample(mode, cols.clone(), send, signal);
		for (int i = 0; i < this.rbmkSamples.size(); i++) {

			if (this.rbmkSamples.get(i).sameAs(sample)) return i;
		}

		this.rbmkSamples.add(sample);
		this.markDirty();
		return this.rbmkSamples.size() - 1;
	}

	private void debugUnregister(int index) {
		if (index < 0 || index >= this.rbmkSamples.size()) return;

		RbmkSample removed = this.rbmkSamples.remove(index);

		if (this.rbmkActiveSample == index) this.rbmkActiveSample = -1;
		else if (this.rbmkActiveSample > index) this.rbmkActiveSample--;

		this.markDirty();
		this.networkPackNT(20);
	}

	private void resumeSamples() {
		int index = this.rbmkActiveSample;
		if (index < 0 || index >= this.rbmkSamples.size()) index = 0;

		RbmkSample sample = this.rbmkSamples.get(index);
		this.rbmkActiveSample = index;
		this.rbmkDebugMode = Math.floorMod(sample.mode, RbmkDebugMode.values().length);
		this.rbmkDebugCols = sample.cols.clone();
		this.rbmkSend = sample.send;
		this.rbmkSignal = sample.signal;
		this.rbmkDebugActive = true;
		this.rbmkDebugRegistered = true;

		if (this.rbmkTarget != null) this.importRbmkTarget();

		this.refreshDebugReading();
		if (this.rbmkDebugActive) this.sendDebugReading();
		this.networkPackNT(20);

	}

	private void debugLoadSample(int index) {
		if (index < 0 || index >= this.rbmkSamples.size()) return;

		RbmkSample sample = this.rbmkSamples.get(index);
		this.rbmkActiveSample = index;
		this.rbmkDebugMode = Math.floorMod(sample.mode, RbmkDebugMode.values().length);
		this.rbmkDebugCols = sample.cols.clone();
		this.rbmkSend = sample.send;
		this.rbmkSignal = sample.signal;
		this.rbmkDebugActive = true;
		this.rbmkDebugRegistered = true;

		this.refreshDebugReading();
		if (this.rbmkDebugActive) this.sendDebugReading();
		this.networkPackNT(20);

	}

	private void sendDebugReading() {
		if (this.rbmkSend == SEND_MOUNT) return;

		if (this.isActiveRegistered()) return;

		String channel = this.rbmkSignal == null || this.rbmkSignal.isEmpty() ? DEBUG_CHANNEL : this.rbmkSignal;
		RbmkDebugMode active = RbmkDebugMode.values()[Math.floorMod(this.rbmkDebugMode, RbmkDebugMode.values().length)];
		RTTYSystem.broadcast(this.world, channel, Math.round(active.displayValue(this.rbmkDebugValue)));
	}

	private boolean isActiveRegistered() {
		RbmkSample active = new RbmkSample(this.rbmkDebugMode, this.rbmkDebugCols, this.rbmkSend, this.rbmkSignal);
		for (RbmkSample sample : this.rbmkSamples) {
			if (sample.sameAs(active)) return true;
		}
		return false;
	}

	private final Map<String, Double> rbmkPublished = new LinkedHashMap<>();

	private final Map<String, Boolean> rbmkPublishedFraction = new LinkedHashMap<>();

	private void refreshSamples() {
		for (RbmkSample sample : this.rbmkSamples) {
			sample.value = this.readDebugValue(sample.mode, sample.cols);

			if (sample.send == SEND_ROR) {
				String channel = sample.signal.isEmpty() ? DEBUG_CHANNEL : sample.signal;

				RbmkDebugMode sampleMode = RbmkDebugMode.values()[Math.floorMod(sample.mode, RbmkDebugMode.values().length)];
				long sent = Math.round(sampleMode.displayValue(sample.value));
				RTTYSystem.broadcast(this.world, channel, sent);
			} else if (!sample.signal.isEmpty()) {
				boolean fraction = RbmkDebugMode.values()[Math.floorMod(sample.mode, RbmkDebugMode.values().length)].isFraction();
				this.rbmkPublished.put(sample.signal, sample.value);
				this.rbmkPublishedFraction.put(sample.signal, fraction);
			}
		}
	}


	private void refreshDebugReading() {
		double value = this.readDebugValue(this.rbmkDebugMode, this.rbmkDebugCols);
		this.rbmkDebugValue = value;

		if (this.rbmkSend == SEND_MOUNT && !this.rbmkSignal.isEmpty()) {
			boolean fraction = RbmkDebugMode.values()[Math.floorMod(this.rbmkDebugMode, RbmkDebugMode.values().length)].isFraction();
			this.rbmkPublished.put(this.rbmkSignal, value);
			this.rbmkPublishedFraction.put(this.rbmkSignal, fraction);
		}
		this.rbmkDebugSeq++;

	}

	private double readDebugValue(int modeOrdinal, int[] columns) {
		RbmkDebugMode mode = RbmkDebugMode.values()[Math.floorMod(modeOrdinal, RbmkDebugMode.values().length)];

		double value = 0.0D;
		int count = 0;
		boolean taken = false;

		for (int index : columns) {
			if (index < 0 || index >= this.rbmkColumns.length) continue;

			RBMKColumn column = this.rbmkColumns[index];
			if (column == null) continue;
			count++;

			switch (mode) {
				case COLUMN_HEAT:
					if (!taken) { value = column.heat; taken = true; }
					break;
				case COLUMN_MAX_HEAT:
					if (!taken) { value = column.maxHeat; taken = true; }
					break;
				case COLUMN_TYPE:
					if (!taken) { value = column.type.ordinal(); taken = true; }
					break;
				case TOTAL_HEAT:
				case AVG_HEAT:
					value += column.heat;
					break;
				case PEAK_HEAT:
					value = Math.max(value, column.heat);
					break;
				case SELECTED:
					break;
				case MODERATED:
					if (!taken) { value = column.moderated ? 1 : 0; taken = true; }
					break;
				case REASIM_WATER:
					if (!taken) { value = column.reasimWater; taken = true; }
					break;
				case REASIM_STEAM:
					if (!taken) { value = column.reasimSteam; taken = true; }
					break;
				case FUEL_ENRICHMENT:
					if (!taken && column instanceof RBMKColumn.FuelColumn) { value = ((RBMKColumn.FuelColumn) column).enrichment; taken = true; }
					break;
				case FUEL_XENON:
					if (!taken && column instanceof RBMKColumn.FuelColumn) { value = ((RBMKColumn.FuelColumn) column).xenon; taken = true; }
					break;
				case FUEL_HEAT:
					if (!taken && column instanceof RBMKColumn.FuelColumn) { value = ((RBMKColumn.FuelColumn) column).c_heat; taken = true; }
					break;
				case ROD_LEVEL:
					if (!taken && column instanceof RBMKColumn.ControlColumn) { value = ((RBMKColumn.ControlColumn) column).level; taken = true; }
					break;
				case BOILER_WATER:
					if (!taken && column instanceof RBMKColumn.BoilerColumn) { value = ((RBMKColumn.BoilerColumn) column).water; taken = true; }
					break;
				case BOILER_STEAM:
					if (!taken && column instanceof RBMKColumn.BoilerColumn) { value = ((RBMKColumn.BoilerColumn) column).steam; taken = true; }
					break;
				case COOLER_CRYO:
					if (!taken && column instanceof RBMKColumn.CoolerColumn) { value = ((RBMKColumn.CoolerColumn) column).cryo; taken = true; }
					break;
				case COOLER_HOT:
					if (!taken && column instanceof RBMKColumn.CoolerColumn) { value = ((RBMKColumn.CoolerColumn) column).hot; taken = true; }
					break;
				case HEATER_WATER:
					if (!taken && column instanceof RBMKColumn.HeaterColumn) { value = ((RBMKColumn.HeaterColumn) column).water; taken = true; }
					break;
				case HEATER_STEAM:
					if (!taken && column instanceof RBMKColumn.HeaterColumn) { value = ((RBMKColumn.HeaterColumn) column).steam; taken = true; }
					break;
				case OUTGASSER_PROGRESS:
					if (!taken && column instanceof RBMKColumn.OutgasserColumn) { value = ((RBMKColumn.OutgasserColumn) column).progress; taken = true; }
					break;
				case OUTGASSER_FLUX:
					if (!taken && column instanceof RBMKColumn.OutgasserColumn) { value = ((RBMKColumn.OutgasserColumn) column).usedFlux; taken = true; }
					break;
				case OUTGASSER_GAS:
					if (!taken && column instanceof RBMKColumn.OutgasserColumn) { value = ((RBMKColumn.OutgasserColumn) column).gas; taken = true; }
					break;
			}
		}

		if (mode == RbmkDebugMode.SELECTED) value = count;
		else if (mode == RbmkDebugMode.AVG_HEAT && count > 0) value /= count;

		return value;
	}


	public ItemStack getBatteryStack() {
		return this.batterySlot.getStackInSlot(0);
	}

	private void dischargeBattery() {
		ItemStack stack = this.getBatteryStack();
		if (stack.isEmpty() || !(stack.getItem() instanceof IBatteryItem battery)) return;
		if (this.power >= this.maxPower) return;

		long moved = Math.min(Math.min(battery.getDischargeRate(stack), battery.getCharge(stack)), this.maxPower - this.power);
		if (moved <= 0L) return;

		battery.dischargeBattery(stack, moved);
		this.power += moved;
	}

	@Override
	public int getHeatStored() {
		return this.heat;
	}
	@Override
	public void useUpHeat(int amount) {
		this.heat = Math.max(0, this.heat - amount);
	}

	@Override
	public Map<String, DataValue> getQueryData() {
		Map<String, DataValue> map = new DebugQueryMap();

		RbmkDebugMode active = RbmkDebugMode.values()[Math.floorMod(this.rbmkDebugMode, RbmkDebugMode.values().length)];
		map.put(DEBUG_PANEL_NAME, new DataValueFloat(active.isFraction()
				? (float) this.rbmkDebugValue
				: asSevenSegmentNumber(this.rbmkDebugValue)));
		if (active.isFraction()) {
			map.put(DEBUG_PANEL_NAME + ".display", new DataValueFloat(asSevenSegmentNumber(this.rbmkDebugValue * 100.0D)));
		} else {
			map.put(DEBUG_PANEL_NAME + ".raw", new DataValueFloat((float) this.rbmkDebugValue));
		}

		map.put(DEBUG_PANEL_NAME + ".text", new DataValueString(active.label + " " + Library.getShortNumber(Math.round(active.displayValue(this.rbmkDebugValue))) + " #" + this.rbmkDebugSeq));

		for (Map.Entry<String, Double> entry : this.rbmkPublished.entrySet()) {
			boolean fraction = Boolean.TRUE.equals(this.rbmkPublishedFraction.get(entry.getKey()))
					|| this.isFractionName(entry.getKey());
			map.put(entry.getKey(), new DataValueFloat(fraction
					? (float) (double) entry.getValue()
					: asSevenSegmentNumber(entry.getValue())));

			if (fraction) {

				map.put(entry.getKey() + ".display", new DataValueFloat(asSevenSegmentNumber(entry.getValue() * 100.0D)));
			} else {
				map.put(entry.getKey() + ".raw", new DataValueFloat((float) (double) entry.getValue()));
			}

			double shownValue = fraction ? entry.getValue() * 100.0D : entry.getValue();
			map.put(entry.getKey() + ".text", new DataValueString(Library.getShortNumber(Math.round(shownValue)) + " #" + this.rbmkDebugSeq));
		}
		return map;
	}

	private boolean isFractionName(String name) {
		for (RbmkSample sample : this.rbmkSamples) {
			if (!sample.signal.equals(name)) continue;
			return RbmkDebugMode.values()[Math.floorMod(sample.mode, RbmkDebugMode.values().length)].isFraction();
		}
		return false;
	}

	private static float asSevenSegmentNumber(double value) {
		long whole = Math.round(value);
		if (whole <= 0L) return 0.0f;

		String digits = Long.toString(whole);
		if (digits.length() > 15) return (float) whole;

		try {
			return (float) Long.parseLong(digits, 16);
		} catch (NumberFormatException e) {
			return (float) whole;
		}
	}

	@Override
	public List<String> getInEvents() {
		return Collections.emptyList();
	}

	@Override
	public List<String> getOutEvents() {
		return Collections.emptyList();
	}

	@Override
	public void receiveEvent(BlockPos source, ControlEvent event) {

	}

	@Override
	public BlockPos getControlPos() {
		return this.pos;
	}

	@Override
	public World getControlWorld() {
		return this.world;
	}

	public int getCompute() {
		return this.compute;
	}

	public int getComputeSlots() {
		return ServerConfig.computeSlots;
	}

	public int getConnectedComputeUnits() {
		return this.computeUnitCount;
	}

	public int getCooling() {
		return this.cooling;
	}

	public int getHeatTargetCount() {
		return this.heatTargetCount;
	}

	public boolean hasCoolant() {
		return this.coolantPresent;
	}

	public int getHeat() {
		return this.heat;
	}

	public int getMaxHeat() {
		return this.maxHeat;
	}

	public State getState() {
		return this.state;
	}

	public ForgeDirection getFacing() {
		return BlockITServerColumn.facingOf(world, pos);
	}

	public void getDiagData(NBTTagCompound nbt) {

		nbt.setString("state", this.state.name());
		nbt.setInteger("compute", this.compute);
		nbt.setInteger("compute_units", this.computeUnitCount);
		nbt.setInteger("heat", this.heat);
		nbt.setLong("power", this.power);
	}

	@Override
	public void serialize(ByteBuf buf) {
		buf.writeInt(this.tick);
		buf.writeLong(this.power);
		buf.writeInt(this.heat);
		buf.writeInt(this.compute);
		buf.writeInt(this.computeUnitCount);
		buf.writeInt(this.cooling);
		buf.writeInt(this.heatTargetCount);
		buf.writeBoolean(this.coolantPresent);
		buf.writeByte(this.state.ordinal());

		writeString(buf, this.connectorDevice);
		buf.writeInt(this.connectorValues.size());
		for (Map.Entry<String, String> entry : this.connectorValues.entrySet()) {
			writeString(buf, entry.getKey());
			writeString(buf, entry.getValue());
		}

		writeString(buf, this.connectorChannel);
		buf.writeInt(this.connectorMode);

		buf.writeInt(this.connectorSelected.size());
		for (String name : this.connectorSelected) {
			writeString(buf, name);
		}

		buf.writeLong(this.rbmkTarget == null ? Long.MIN_VALUE : this.rbmkTarget.toLong());
		for (RBMKColumn column : this.rbmkColumns) {
			RBMKColumn.writeToBuf(buf, column);
		}

		writeString(buf, this.rbmkImportError);
		buf.writeBoolean(this.rbmkImportAsked);

		buf.writeDouble(this.rbmkDebugValue);
		buf.writeInt(this.rbmkDebugMode);
		buf.writeInt(this.rbmkDebugSeq);

		buf.writeInt(this.rbmkSamples.size());
		for (RbmkSample sample : this.rbmkSamples) {
			buf.writeInt(sample.mode);
			buf.writeInt(sample.send);
			writeString(buf, sample.signal);
			buf.writeInt(sample.cols.length);
			for (int index : sample.cols) {
				buf.writeInt(index);
			}
		}

		buf.writeInt(this.rbmkSend);
		writeString(buf, this.rbmkSignal);
	}

	@Override
	public void deserialize(ByteBuf buf) {
		this.tick = buf.readInt();
		this.power = buf.readLong();
		this.heat = buf.readInt();
		this.compute = buf.readInt();
		this.computeUnitCount = buf.readInt();
		this.cooling = buf.readInt();
		this.heatTargetCount = buf.readInt();
		this.coolantPresent = buf.readBoolean();
		this.state = State.values()[buf.readByte()];

		Map<String, String> received = new LinkedHashMap<>();
		this.connectorDevice = readString(buf);
		int count = buf.readInt();
		for (int i = 0; i < count; i++) {
			received.put(readString(buf), readString(buf));
		}


		this.connectorValues = received;

		this.connectorChannel = readString(buf);
		this.connectorMode = buf.readInt();

		List<String> selected = new ArrayList<>();
		int selectedCount = buf.readInt();
		for (int i = 0; i < selectedCount; i++) {
			selected.add(readString(buf));
		}
		this.connectorSelected = selected;

		long packedTarget = buf.readLong();
		this.rbmkTarget = packedTarget == Long.MIN_VALUE ? null : BlockPos.fromLong(packedTarget);

		RBMKColumn[] receivedGrid = new RBMKColumn[this.rbmkColumns.length];
		for (int i = 0; i < receivedGrid.length; i++) {
			receivedGrid[i] = RBMKColumn.readFromBuf(buf);
		}
		this.rbmkColumns = receivedGrid;

		this.rbmkImportError = readString(buf);
		this.rbmkImportAsked = buf.readBoolean();

		this.rbmkDebugValue = buf.readDouble();
		this.rbmkDebugMode = buf.readInt();
		this.rbmkDebugSeq = buf.readInt();

		List<RbmkSample> samples = new ArrayList<>();
		int sampleCount = buf.readInt();
		for (int i = 0; i < sampleCount; i++) {
			int mode = buf.readInt();
			int send = buf.readInt();
			String signal = readString(buf);
			int[] cols = new int[buf.readInt()];
			for (int c = 0; c < cols.length; c++) {
				cols[c] = buf.readInt();
			}
			samples.add(new RbmkSample(mode, cols, send, signal));
		}
		this.rbmkSamples = samples;
		this.rbmkSend = buf.readInt();
		this.rbmkSignal = readString(buf);

		for (RbmkSample sample : samples) {
			if (sample.send != SEND_MOUNT || sample.signal.isEmpty()) continue;
			this.rbmkPublished.putIfAbsent(sample.signal, 0.0D);
			this.rbmkPublishedFraction.putIfAbsent(sample.signal,
					RbmkDebugMode.values()[Math.floorMod(sample.mode, RbmkDebugMode.values().length)].isFraction());
		}
	}

	private static void writeString(ByteBuf buf, String text) {
		byte[] bytes = (text == null ? "" : text).getBytes(StandardCharsets.UTF_8);
		buf.writeShort(bytes.length);
		buf.writeBytes(bytes);
	}

	private static String readString(ByteBuf buf) {
		byte[] bytes = new byte[buf.readShort()];
		buf.readBytes(bytes);
		return new String(bytes, StandardCharsets.UTF_8);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		this.tick = nbt.getInteger("tick");
		this.power = nbt.getLong("power");

		this.heat = Math.min(nbt.getInteger("heat"), (int) (this.maxHeat * 1.5D));
		if (nbt.hasKey("battery")) this.batterySlot.deserializeNBT(nbt.getCompoundTag("battery"));
		if (nbt.hasKey("connector")) this.connectorSlot.deserializeNBT(nbt.getCompoundTag("connector"));
		if (nbt.hasKey("rbmkTool")) this.rbmkToolSlot.deserializeNBT(nbt.getCompoundTag("rbmkTool"));
		if (nbt.hasKey("rbmkTarget")) this.rbmkTarget = BlockPos.fromLong(nbt.getLong("rbmkTarget"));
		this.connectorChannel = nbt.getString("connectorChannel");
		this.connectorMode = nbt.getInteger("connectorMode");

		List<String> selected = new ArrayList<>();
		NBTTagList selectedList = nbt.getTagList("connectorSelected", 8);
		for (int i = 0; i < selectedList.tagCount(); i++) {
			selected.add(selectedList.getStringTagAt(i));
		}
		this.connectorSelected = selected;

		List<RbmkSample> samples = new ArrayList<>();
		NBTTagList sampleList = nbt.getTagList("rbmkSamples", 10);
		for (int i = 0; i < sampleList.tagCount(); i++) {
			NBTTagCompound entry = sampleList.getCompoundTagAt(i);
			samples.add(new RbmkSample(entry.getInteger("mode"), entry.getIntArray("cols"),
					entry.getInteger("send"), entry.getString("signal")));
		}
		this.rbmkSamples = samples;
		this.rbmkActiveSample = nbt.getInteger("rbmkActiveSample");
		this.rbmkSend = nbt.getInteger("rbmkSend");
		this.rbmkSignal = nbt.getString("rbmkSignal");

		NBTTagList publishedList = nbt.getTagList("rbmkPublished", 10);
		for (int i = 0; i < publishedList.tagCount(); i++) {
			NBTTagCompound tag = publishedList.getCompoundTagAt(i);
			this.rbmkPublished.put(tag.getString("name"), tag.getDouble("value"));
		}

		NBTTagList fractionList = nbt.getTagList("rbmkPublishedFraction", 10);
		for (int i = 0; i < fractionList.tagCount(); i++) {
			NBTTagCompound tag = fractionList.getCompoundTagAt(i);
			this.rbmkPublishedFraction.put(tag.getString("name"), tag.getBoolean("fraction"));
		}

		if (!this.rbmkSamples.isEmpty()) this.rbmkResumePending = true;

	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		nbt.setInteger("server_version", SERVER_VERSION);
		nbt.setInteger("tick", this.tick);
		nbt.setLong("power", this.power);
		nbt.setInteger("heat", this.heat);
		nbt.setTag("battery", this.batterySlot.serializeNBT());
		nbt.setTag("connector", this.connectorSlot.serializeNBT());
		nbt.setTag("rbmkTool", this.rbmkToolSlot.serializeNBT());
		if (this.rbmkTarget != null) nbt.setLong("rbmkTarget", this.rbmkTarget.toLong());
		nbt.setString("connectorChannel", this.connectorChannel);
		nbt.setInteger("connectorMode", this.connectorMode);

		NBTTagList selectedList = new NBTTagList();
		for (String name : this.connectorSelected) {
			selectedList.appendTag(new NBTTagString(name));
		}
		nbt.setTag("connectorSelected", selectedList);

		NBTTagList sampleList = new NBTTagList();
		for (RbmkSample sample : this.rbmkSamples) {
			NBTTagCompound entry = new NBTTagCompound();
			entry.setInteger("mode", sample.mode);
			entry.setInteger("send", sample.send);
			entry.setString("signal", sample.signal);
			entry.setIntArray("cols", sample.cols);
			sampleList.appendTag(entry);
		}
		nbt.setTag("rbmkSamples", sampleList);
		nbt.setInteger("rbmkActiveSample", this.rbmkActiveSample);
		nbt.setInteger("rbmkSend", this.rbmkSend);
		nbt.setString("rbmkSignal", this.rbmkSignal);

		NBTTagList publishedList = new NBTTagList();
		for (Map.Entry<String, Double> entry : this.rbmkPublished.entrySet()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("name", entry.getKey());
			tag.setDouble("value", entry.getValue());
			publishedList.appendTag(tag);
		}
		nbt.setTag("rbmkPublished", publishedList);

		NBTTagList fractionList = new NBTTagList();
		for (Map.Entry<String, Boolean> entry : this.rbmkPublishedFraction.entrySet()) {
			NBTTagCompound tag = new NBTTagCompound();
			tag.setString("name", entry.getKey());
			tag.setBoolean("fraction", entry.getValue());
			fractionList.appendTag(tag);
		}
		nbt.setTag("rbmkPublishedFraction", fractionList);
		return nbt;
	}
}
