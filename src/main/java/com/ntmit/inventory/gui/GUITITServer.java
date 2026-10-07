package com.ntmit.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.I18nUtil;
import com.hbm.util.SoundUtil;
import com.ntmit.inventory.container.ContainerITServer;
import com.ntmit.items.ItemITServerConnector;
import com.ntmit.lib.RefStrings;
import com.ntmit.tileentity.TileEntityITServerCore;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SideOnly(Side.CLIENT)
public class GUITITServer extends GuiInfoContainer {

	public static final int GUI_WIDTH = 176;
	public static final int GUI_HEIGHT = 222;

	private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/server_gui.png");

	private static final int PANEL_OFFSET_X = -1;
	private static final int PANEL_OFFSET_Y = -1;

	private static final int POWER_BAR_X = 8;
	private static final int POWER_BAR_Y = 60;
	private static final int POWER_BAR_W = 16;
	private static final int POWER_BAR_H = 52;

	private static final int POWER_FULL_U = 176;
	private static final int POWER_FULL_V = 0;

	private static final int HEAT_BAR_X = 27;
	private static final int HEAT_BAR_Y = 60;
	private static final int HEAT_BAR_W = 16;
	private static final int HEAT_BAR_H = 52;

	private static final int HEAT_FULL_U = 192;
	private static final int HEAT_FULL_V = 0;

	private static final int BATTERY_X = ContainerITServer.BATTERY_SLOT_X;
	private static final int BATTERY_Y = ContainerITServer.BATTERY_SLOT_Y;
	private static final int BATTERY_SIZE = 18;

	private static final int BUTTON_X = 8;
	private static final int BUTTON_Y_1 = 16;
	private static final int BUTTON_Y_2 = 38;
	private static final int BUTTON_W = 36;
	private static final int BUTTON_H = 20;

	private static final int IMPORT_BUTTON_X = 46;
	private static final int IMPORT_BUTTON_Y = 113;
	private static final int IMPORT_BUTTON_W = 20;
	private static final int IMPORT_BUTTON_H = 20;
	private static final int IMPORT_BUTTON_ID = 2;

	private static final int STRIP_U = 0;
	private static final int STRIP_V = 222;
	private static final int STRIP_W = 91;
	private static final int STRIP_H = 18;
	private static final int STRIP_X = 68;
	private static final int STRIP_Y = 4;

	private static final int CHANNEL_FIELD_X = STRIP_X + 20;
	private static final int CHANNEL_FIELD_Y = STRIP_Y + 6;
	private static final int CHANNEL_FIELD_W = 51;
	private static final int CHANNEL_FIELD_H = 14;

	private static final int CHANNEL_SAVE_X = STRIP_X + 73;
	private static final int CHANNEL_SAVE_Y = STRIP_Y + 6;
	private static final int CHANNEL_SAVE_W = 17;
	private static final int CHANNEL_SAVE_H = 14;

	private static final int TEST_BUTTON_1_ID = 0;
	private static final int TEST_BUTTON_2_ID = 1;

	private static final int DEBUG_TEXT_X = 68;
	private static final int DEBUG_POS_Y = 24;
	private static final int DEBUG_DEVICE_Y = 34;
	private static final int DEBUG_LIST_Y = 48;
	private static final int DEBUG_LINE_H = 10;
	private static final int DEBUG_LIST_MAX = 9;
	private static final int DEBUG_TEXT_COLOUR = 0x203020;

	private static final int DEBUG_VALUE_WIDTH = 42;
	private static final int DEBUG_VALUE_COLOUR = 0x203060;

	private final TileEntityITServerCore core;

	private String importedPos = "";
	private String importedDevice = "";
	private List<String> importedClasses = Collections.emptyList();

	private int debugScroll;

	private String lastDrawnText = "";
	private int debugLogCooldown;

	private GuiTextField channelField;

	public GUITITServer(InventoryPlayer invPlayer, TileEntityITServerCore core) {
		super(new ContainerITServer(invPlayer, core));

		this.core = core;
		this.xSize = GUI_WIDTH;
		this.ySize = GUI_HEIGHT;
	}

	@Override
	public void initGui() {
		super.initGui();

		this.buttonList.add(new GuiButton(TEST_BUTTON_1_ID, this.guiLeft + PANEL_OFFSET_X + BUTTON_X, this.guiTop + PANEL_OFFSET_Y + BUTTON_Y_1, BUTTON_W, BUTTON_H, "testbutton_1"));
		this.buttonList.add(new GuiButton(TEST_BUTTON_2_ID, this.guiLeft + PANEL_OFFSET_X + BUTTON_X, this.guiTop + PANEL_OFFSET_Y + BUTTON_Y_2, BUTTON_W, BUTTON_H, "testbutton_2"));

		this.buttonList.add(new GuiButton(IMPORT_BUTTON_ID, this.guiLeft + PANEL_OFFSET_X + IMPORT_BUTTON_X, this.guiTop + PANEL_OFFSET_Y + IMPORT_BUTTON_Y, IMPORT_BUTTON_W, IMPORT_BUTTON_H, ">"));

		this.channelField = new GuiTextField(0, this.fontRenderer, this.guiLeft + PANEL_OFFSET_X + CHANNEL_FIELD_X, this.guiTop + PANEL_OFFSET_Y + CHANNEL_FIELD_Y, CHANNEL_FIELD_W, CHANNEL_FIELD_H);
		this.channelField.setMaxStringLength(24);
		this.channelField.setEnableBackgroundDrawing(false);
		this.channelField.setText(this.core.getConnectorChannel());
	}

	private boolean isStripVisible() {
		return !this.importedPos.isEmpty();
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);

		if (button.id == IMPORT_BUTTON_ID) {
			this.importConnector();
		}

		if (button.id == TEST_BUTTON_1_ID) {

			NBTTagCompound request = new NBTTagCompound();
			request.setBoolean("openRbmkPage", true);
			PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));
		}
	}

	private void sendChannel(int mode) {
		NBTTagCompound data = new NBTTagCompound();
		data.setString("channel", this.channelField == null ? "" : this.channelField.getText());
		data.setInteger("mode", mode);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data, this.core.getPos()));
	}

	@Override
	public void updateScreen() {
		super.updateScreen();
		if (this.channelField != null && this.isStripVisible()) this.channelField.updateCursorCounter();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (this.channelField != null && this.isStripVisible()) {
			if (this.channelField.isFocused() && keyCode == Keyboard.KEY_RETURN) {
				this.sendChannel(this.core.getConnectorMode());
				return;
			}
			if (this.channelField.textboxKeyTyped(typedChar, keyCode)) return;
		}

		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (!this.isStripVisible()) return;

		if (this.channelField != null) this.channelField.mouseClicked(mouseX, mouseY, mouseButton);

		if (this.inStrip(mouseX, mouseY, CHANNEL_SAVE_X, CHANNEL_SAVE_Y, CHANNEL_SAVE_W, CHANNEL_SAVE_H)) {
			SoundUtil.playClickSound();
			this.sendChannel(this.core.getConnectorMode());
		}
	}

	private boolean inStrip(int mouseX, int mouseY, int x, int y, int width, int height) {
		int left = this.guiLeft + PANEL_OFFSET_X + x;
		int top = this.guiTop + PANEL_OFFSET_Y + y;
		return mouseX >= left && mouseX < left + width && mouseY >= top && mouseY < top + height;
	}

	private void importConnector() {
		ItemStack stack = ((ContainerITServer) this.inventorySlots).getConnectorStack();

		if (stack.isEmpty() || !ItemITServerConnector.isBound(stack)) {
			this.importedPos = "";
			this.importedDevice = "";
			this.importedClasses = Collections.emptyList();
			return;
		}

		BlockPos pos = ItemITServerConnector.getPos(stack);
		this.importedPos = "D" + ItemITServerConnector.getDimension(stack) + " " + pos.getX() + "," + pos.getY() + "," + pos.getZ();
		this.importedDevice = ItemITServerConnector.getDeviceType(stack);
		this.importedClasses = ItemITServerConnector.getFields(stack);
		this.debugScroll = 0;
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();

		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) return;

		int max = Math.max(0, this.core.getConnectorValues().size() - DEBUG_LIST_MAX);
		if (max <= 0) return;

		this.debugScroll = Math.max(0, Math.min(max, this.debugScroll - Integer.signum(wheel)));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		List<String> drawn = new ArrayList<>();

		if (this.importedPos.isEmpty()) {

			if (!((ContainerITServer) this.inventorySlots).getConnectorStack().isEmpty()) {
				String hint = I18nUtil.resolveKey("item.ntm-it.server_connector.unbound");
				this.fontRenderer.drawString(hint, DEBUG_TEXT_X, DEBUG_POS_Y, DEBUG_TEXT_COLOUR);
				drawn.add(hint);
			}
			this.logDrawn(drawn);
			return;
		}

		this.fontRenderer.drawString(this.importedPos, DEBUG_TEXT_X, DEBUG_POS_Y, DEBUG_TEXT_COLOUR);
		drawn.add(this.importedPos);

		String device = this.debugDeviceLine();
		this.fontRenderer.drawString(device, DEBUG_TEXT_X, DEBUG_DEVICE_Y, DEBUG_TEXT_COLOUR);
		drawn.add(device);

		int width = GUI_WIDTH - 2 - DEBUG_TEXT_X;
		Map<String, String> values = this.core.getConnectorValues();
		List<String> names = new ArrayList<>(values.keySet());
		int maxScroll = Math.max(0, names.size() - DEBUG_LIST_MAX);
		if (this.debugScroll > maxScroll) this.debugScroll = maxScroll;

		int y = DEBUG_LIST_Y;
		for (int i = this.debugScroll; i < names.size() && i < this.debugScroll + DEBUG_LIST_MAX; i++) {
			String name = names.get(i);
			this.fontRenderer.drawString(this.truncate(name, width - DEBUG_VALUE_WIDTH), DEBUG_TEXT_X, y, DEBUG_TEXT_COLOUR);

			String value = values.get(name);
			if (value != null) {
				String shown = this.truncate(value, DEBUG_VALUE_WIDTH);
				this.fontRenderer.drawString(shown, GUI_WIDTH - 2 - this.fontRenderer.getStringWidth(shown), y, DEBUG_VALUE_COLOUR);
			}
			drawn.add(name + "=" + value);

			y += DEBUG_LINE_H;
		}

		if (maxScroll > 0) {
			String position = (this.debugScroll + 1) + "-" + Math.min(names.size(), this.debugScroll + DEBUG_LIST_MAX) + "/" + names.size();
			this.fontRenderer.drawString(position, DEBUG_TEXT_X, y, DEBUG_VALUE_COLOUR);
			drawn.add(position);
		}

		this.logDrawn(drawn);
	}

	private void logDrawn(List<String> drawn) {
		String text = String.join(" | ", drawn) + "   core=" + this.core.getConnectorValues();
		if (text.equals(this.lastDrawnText)) return;
		if (this.debugLogCooldown > 0) {
			this.debugLogCooldown--;
			return;
		}

		this.lastDrawnText = text;
		this.debugLogCooldown = 10;
	}

	private String debugDeviceLine() {
		int room = GUI_WIDTH - 2 - DEBUG_TEXT_X;
		String device = this.core.getConnectorDevice().isEmpty() ? this.importedDevice : this.core.getConnectorDevice();
		return this.truncate(device, room - 12) + " (" + this.core.getConnectorValues().size() + ")";
	}

	private String truncate(String text, int maxWidth) {
		if (this.fontRenderer.getStringWidth(text) <= maxWidth) return text;

		String cut = text;
		while (!cut.isEmpty() && this.fontRenderer.getStringWidth(cut + "..") > maxWidth) {
			cut = cut.substring(0, cut.length() - 1);
		}
		return cut + "..";
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
		this.mc.getTextureManager().bindTexture(TEXTURE);
		drawTexturedModalRect(this.guiLeft + PANEL_OFFSET_X, this.guiTop + PANEL_OFFSET_Y, 0, 0, GUI_WIDTH, GUI_HEIGHT);

		if (this.isStripVisible()) {
			drawTexturedModalRect(this.guiLeft + PANEL_OFFSET_X + STRIP_X, this.guiTop + PANEL_OFFSET_Y + STRIP_Y, STRIP_U, STRIP_V, STRIP_W, STRIP_H);
		}

		this.drawGauge(POWER_BAR_X, POWER_BAR_Y, POWER_BAR_W, POWER_BAR_H, POWER_FULL_U, POWER_FULL_V,
				fraction(this.core.getPower(), this.core.getMaxPower()));
		this.drawGauge(HEAT_BAR_X, HEAT_BAR_Y, HEAT_BAR_W, HEAT_BAR_H, HEAT_FULL_U, HEAT_FULL_V,
				fraction(this.core.getHeat(), this.core.getMaxHeat()));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);

		if (this.channelField != null && this.isStripVisible()) this.channelField.drawTextBox();

		this.drawElectricityInfo(this, mouseX, mouseY, this.guiLeft + PANEL_OFFSET_X + POWER_BAR_X, this.guiTop + PANEL_OFFSET_Y + POWER_BAR_Y, POWER_BAR_W, POWER_BAR_H,
				this.core.getPower(), this.core.getMaxPower());
		this.drawCustomInfo(mouseX, mouseY, this.guiLeft + PANEL_OFFSET_X + HEAT_BAR_X, this.guiTop + PANEL_OFFSET_Y + HEAT_BAR_Y, HEAT_BAR_W, HEAT_BAR_H,
				new String[] {I18nUtil.resolveKey("gui.ntm-it.server.heat", this.core.getHeat(), this.core.getMaxHeat())});

		this.drawCustomInfo(mouseX, mouseY, this.guiLeft + BATTERY_X, this.guiTop + BATTERY_Y, BATTERY_SIZE, BATTERY_SIZE, this.batteryLines());
	}

	private String[] batteryLines() {
		ItemStack stack = ((ContainerITServer) this.inventorySlots).getBatteryStack();
		if (stack.isEmpty()) {
			return new String[] {I18nUtil.resolveKey("gui.ntm-it.server.battery.empty")};
		}

		List<String> lines = new ArrayList<>();
		lines.add(stack.getDisplayName());

		NBTTagCompound tag = stack.getTagCompound();
		if (tag == null || tag.getSize() == 0) {
			lines.add(I18nUtil.resolveKey("gui.ntm-it.server.nbt.none"));
			return lines.toArray(new String[0]);
		}

		for (String key : tag.getKeySet()) {
			lines.add(I18nUtil.resolveKey("gui.ntm-it.server.nbt.entry", key, tag.getTag(key).toString()));
		}

		return lines.toArray(new String[0]);
	}

	private static float fraction(long value, long max) {
		if (max <= 0L) return 0F;
		return MathHelper.clamp((float) ((double) value / (double) max), 0F, 1F);
	}

	private void drawGauge(int x, int y, int w, int h, int u, int v, float fill) {
		int filled = (int) (h * fill);
		if (filled <= 0) return;

		drawTexturedModalRect(this.guiLeft + PANEL_OFFSET_X + x, this.guiTop + PANEL_OFFSET_Y + y + h - filled, u, v + h - filled, w, filled);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
