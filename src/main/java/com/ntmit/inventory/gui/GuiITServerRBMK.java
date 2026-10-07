package com.ntmit.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.fluid.FluidType;
import com.hbm.inventory.fluid.Fluids;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.hbm.util.I18nUtil;
import com.hbm.util.SoundUtil;
import com.ntmit.lib.RefStrings;
import com.ntmit.tileentity.TileEntityITServerCore;
import com.ntmit.inventory.container.ContainerITServerRBMK;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SideOnly(Side.CLIENT)
public class GuiITServerRBMK extends GuiContainer {

	private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/server_gui_rbmk.png");

	private static final int X_SIZE = 244;
	private static final int Y_SIZE = 172;

	private static final ResourceLocation GRID_TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/server_gui_rbmk_grid.png");

	private static final int GRID_COLS = 15;
	private static final int CELL = 10;
	private static final int TYPE_V = 172;
	private static final int HEAT_V = 192;
	private static final int COLOUR_V = 202;

	private static final int GRID_X = 188;
	private static final int GRID_Y = 5;
	private static final int VIEW_W = 51;
	private static final int VIEW_H = 51;

	private static final int FIELD_PX = GRID_COLS * CELL;

	private static final int GRID_TEX_SIZE = 54;

	private static final int TILE = 50;

	private static final float MIN_ZOOM = 0.25F;
	private static final float MAX_ZOOM = 4.0F;
	private static final float ZOOM_STEP = 1.25F;

	private static final int GRID_BACKDROP = 0xFF101010;

	private static final int LIST_X = 8;
	private static final int LIST_Y = 48;
	private static final int LIST_ROW = 14;

	private static final int MODE_W = 96;
	private static final int MODE_ROWS = 4;

	private static final int NOTE_Y = 92;
	private static final int NOTE_LINES = 2;
	private static final int NOTE_COLOUR = 0xFF9E9E9E;
	private static final int NOTE_WARN_COLOUR = 0xFFFF8866;

	private static final int REGISTER_BUTTON_ID = 3;
	private static final int REGISTER_BUTTON_X = 48;
	private static final int REGISTER_BUTTON_Y = 7;
	private static final int REGISTER_BUTTON_W = 40;
	private static final int REGISTER_BUTTON_H = 18;

	private static final int SAMPLE_X = 110;
	private static final int SAMPLE_W = 74;

	private static final int SAMPLE_ROWS = 5;

	private static final int LIST_BACKDROP = 0xFF252525;
	private static final int LIST_BACKDROP_SEL = 0xFF27402A;
	private static final int LIST_BACKDROP_HOVER = 0xFF5A2424;
	private static final int LIST_TEXT = 0xFFCFCFCF;
	private static final int LIST_TEXT_SEL = 0xFFFFCC44;
	private static final int LIST_TEXT_HOVER = 0xFFFF9999;

	private static final String LIST_REMOVE = "右键=删除";

	private static final String LIST_PICK = "请选择枚举";

	private static final String LIST_NO_COLUMNS = "请先在网格里选择柱体";

	private static final long LIST_WARN_MILLIS = 3000L;

	private static final String NOTE_DUPLICATE = "不建议采用：与其它变量重复或非物理量";

	private static final int SEND_Y = LIST_Y + LIST_ROW;
	private static final int SIGNAL_Y = LIST_Y + LIST_ROW * 2;
	private static final int SIGNAL_W = 96;

	private static final String SEND_PICK = "请选择发送方式";

	private static final String SIGNAL_NONE = "请先选择发送方式";
	private static final String SIGNAL_ROR = "请输入ID";
	private static final String SIGNAL_MOUNT = "请输入变量名";
	private static final int SIGNAL_HINT_COLOUR = 0xFF707070;

	private static final int IMPORT_BUTTON_ID = 0;
	private static final int IMPORT_BUTTON_X = 27;
	private static final int IMPORT_BUTTON_Y = 7;
	private static final int IMPORT_BUTTON_SIZE = 18;

	private static final int SELECTION_U = 0;
	private static final int SELECTION_V = 192;

	private static final int STATUS_X = 8;
	private static final int STATUS_Y = 30;
	private static final int STATUS_W = 72;
	private static final int STATUS_COLOUR = 0xFF5555;

	private final TileEntityITServerCore core;

	private boolean importPressed = false;
	private long importTime = 0L;

	private final boolean[] selection = new boolean[TileEntityITServerCore.RBMK_GRID * TileEntityITServerCore.RBMK_GRID];

	private int debugMode = -1;

	private boolean modeListOpen = false;

	private int modeScroll = 0;

	private String listWarn = "";
	private long listWarnTime = 0L;

	private int sendType = -1;
	private boolean sendListOpen = false;

	private GuiTextField signalField;

	private float camX = (FIELD_PX - VIEW_W) / 2.0F;
	private float camY = (FIELD_PX - VIEW_H) / 2.0F;
	private float zoom = 1.0F;

	private boolean panning = false;
	private int panMouseX = 0;
	private int panMouseY = 0;
	private float panCamX = 0.0F;
	private float panCamY = 0.0F;

	public GuiITServerRBMK(InventoryPlayer invPlayer, TileEntityITServerCore core) {
		super(new ContainerITServerRBMK(invPlayer, core));
		this.core = core;
		this.xSize = X_SIZE;
		this.ySize = Y_SIZE;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.buttonList.add(new GuiButton(IMPORT_BUTTON_ID, this.guiLeft + IMPORT_BUTTON_X, this.guiTop + IMPORT_BUTTON_Y, IMPORT_BUTTON_SIZE, IMPORT_BUTTON_SIZE, ">"));
		this.buttonList.add(new GuiButton(REGISTER_BUTTON_ID, this.guiLeft + REGISTER_BUTTON_X, this.guiTop + REGISTER_BUTTON_Y, REGISTER_BUTTON_W, REGISTER_BUTTON_H, "+REG"));

		this.signalField = new GuiTextField(0, this.fontRenderer, this.guiLeft + LIST_X, this.guiTop + SIGNAL_Y,
				SIGNAL_W, LIST_ROW);
		this.signalField.setMaxStringLength(32);
		this.sendType = this.core.getRbmkSend();
		this.signalField.setText(this.core.getRbmkSignal());
		this.signalField.setEnabled(this.sendType >= 0);

		this.askForSamples();
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {
		if (this.signalField != null && this.signalField.isFocused() && this.signalField.textboxKeyTyped(typedChar, keyCode)) return;

		super.keyTyped(typedChar, keyCode);
	}

	@Override
	protected void actionPerformed(GuiButton button) throws IOException {
		super.actionPerformed(button);

		if (button.id == IMPORT_BUTTON_ID) {
			SoundUtil.playClickSound();
			this.importConnector();
		}
		if (button.id == REGISTER_BUTTON_ID) {
			SoundUtil.playClickSound();
			this.sendDebugRegistration();
		}
	}

	private int[] selectedColumns() {
		List<Integer> picked = new ArrayList<>();
		for (int index = 0; index < this.selection.length; index++) {
			if (this.selection[index]) picked.add(index);
		}

		int[] cols = new int[picked.size()];
		for (int i = 0; i < cols.length; i++) {
			cols[i] = picked.get(i);
		}
		return cols;
	}

	private void sendDebugRegistration() {
		if (!this.hasMode() || this.sendType < 0) return;

		int[] cols = this.selectedColumns();

		if (cols.length == 0) {
			this.listWarn = LIST_NO_COLUMNS;
			this.listWarnTime = System.currentTimeMillis();
			return;
		}

		String signal = this.signalField == null ? "" : this.signalField.getText();

		NBTTagCompound request = new NBTTagCompound();
		request.setBoolean("rbmkRegister", true);
		request.setInteger("mode", this.debugMode);
		request.setIntArray("cols", cols);
		request.setInteger("send", this.sendType);
		request.setString("signal", signal);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));

	}

	private void sendDebugReading() {
		if (!this.hasMode()) return;

		int[] cols = this.selectedColumns();

		NBTTagCompound request = new NBTTagCompound();
		request.setBoolean("rbmkDebug", true);
		request.setInteger("mode", this.debugMode);
		request.setIntArray("cols", cols);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));

	}

	private void askForSamples() {
		NBTTagCompound request = new NBTTagCompound();
		request.setBoolean("rbmkSampleQuery", true);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));
	}

	private boolean hasMode() {
		return this.debugMode >= 0 && this.debugMode < TileEntityITServerCore.RbmkDebugMode.values().length;
	}


	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);

		if (mouseButton == 0 && this.sendListOpen) {
			int option = this.sendOptionAt(mouseX, mouseY);
			this.sendListOpen = false;
			if (option >= 0) {
				this.sendType = option;
				if (this.signalField != null) this.signalField.setEnabled(true);
				SoundUtil.playClickSound();
				return;
			}
		}

		if (mouseButton == 0 && this.modeListOpen) {
			int option = this.modeOptionAt(mouseX, mouseY);
			this.modeListOpen = false;
			if (option >= 0) {
				this.debugMode = option;
				SoundUtil.playClickSound();
				this.sendDebugReading();
				return;
			}
		}

		boolean inSignal = this.signalField != null && this.signalField.mouseClicked(mouseX, mouseY, mouseButton);
		if (this.signalField != null && !inSignal) this.signalField.setFocused(false);
		if (inSignal) return;

		int sample = this.sampleRowAt(mouseX, mouseY);
		if (sample >= 0) {
			SoundUtil.playClickSound();
			if (mouseButton == 1) this.sendSampleRemoval(sample);
			if (mouseButton == 0) this.sendSampleLoad(sample);
			return;
		}

		if (mouseButton != 0) return;

		if (this.modeHeaderAt(mouseX, mouseY)) {
			this.modeListOpen = true;
			this.sendListOpen = false;
			SoundUtil.playClickSound();
			return;
		}

		if (this.sendHeaderAt(mouseX, mouseY)) {
			this.sendListOpen = true;
			this.modeListOpen = false;
			SoundUtil.playClickSound();
			return;
		}

		if (!this.inWindow(mouseX, mouseY)) return;

		this.panning = true;
		this.panMouseX = mouseX;
		this.panMouseY = mouseY;
		this.panCamX = this.camX;
		this.panCamY = this.camY;
	}

	@Override
	protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
		super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);

		if (!this.panning || clickedMouseButton != 0) return;

		this.camX = this.panCamX - (mouseX - this.panMouseX) / this.zoom;
		this.camY = this.panCamY - (mouseY - this.panMouseY) / this.zoom;
		this.clampCamera();
	}

	@Override
	public void handleMouseInput() throws IOException {
		super.handleMouseInput();

		int wheel = Mouse.getEventDWheel();
		if (wheel == 0) return;

		int mouseX = Mouse.getEventX() * this.width / Math.max(this.mc.displayWidth, 1);
		int mouseY = this.height - Mouse.getEventY() * this.height / Math.max(this.mc.displayHeight, 1) - 1;

		if (this.modeListOpen && this.inModePopup(mouseX, mouseY)) {
			this.modeScrollBy(wheel > 0 ? -1 : 1);
			return;
		}

		if (!this.inWindow(mouseX, mouseY)) return;

		float was = this.zoom;
		this.zoom = clamp(this.zoom * (wheel > 0 ? ZOOM_STEP : 1.0F / ZOOM_STEP), MIN_ZOOM, MAX_ZOOM);
		if (this.zoom == was) return;

		float localX = mouseX - this.guiLeft - GRID_X;
		float localY = mouseY - this.guiTop - GRID_Y;
		this.camX += localX / was - localX / this.zoom;
		this.camY += localY / was - localY / this.zoom;
		this.clampCamera();
	}

	@Override
	protected void mouseReleased(int mouseX, int mouseY, int state) {
		super.mouseReleased(mouseX, mouseY, state);

		if (!this.panning) return;
		this.panning = false;

		if (mouseX != this.panMouseX || mouseY != this.panMouseY) return;

		int index = this.cellAt(mouseX, mouseY);
		if (index < 0 || index >= this.selection.length) return;

		this.selection[index] = !this.selection[index];
		SoundUtil.playClickSound();
	}

	private void importConnector() {
		this.importPressed = true;
		this.importTime = System.currentTimeMillis();

		NBTTagCompound request = new NBTTagCompound();
		request.setBoolean("importRbmk", true);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.mc.getTextureManager().bindTexture(TEXTURE);
		this.drawTexturedModalRect(this.guiLeft, this.guiTop, 0, 0, X_SIZE, Y_SIZE);

		RBMKColumn[] columns = this.core.getRbmkColumns();
		this.drawWindow(columns);
		this.drawLists(mouseX, mouseY);

		String status = null;
		if (!this.listWarn.isEmpty() && System.currentTimeMillis() - this.listWarnTime < LIST_WARN_MILLIS) {
			status = this.listWarn;
		}
		if (this.importPressed) {
			if (this.core.getRbmkImportAsked()) {
				String error = this.core.getRbmkImportError();
				if (!error.isEmpty()) status = I18nUtil.resolveKey("gui.ntm-it.server.rbmk." + error);
			} else if (System.currentTimeMillis() - this.importTime > 1000L) {
				status = I18nUtil.resolveKey("gui.ntm-it.server.rbmk.notlinked");
			}
		}

		super.drawScreen(mouseX, mouseY, partialTicks);

		if (status != null) {
			this.fontRenderer.drawSplitString(status, this.guiLeft + STATUS_X, this.guiTop + STATUS_Y, STATUS_W, STATUS_COLOUR);
		}

		this.drawColumnInfo(columns, mouseX, mouseY);
	}

	private void drawWindow(RBMKColumn[] columns) {

		this.drawRect(this.guiLeft + GRID_X, this.guiTop + GRID_Y,
				this.guiLeft + GRID_X + VIEW_W, this.guiTop + GRID_Y + VIEW_H, GRID_BACKDROP);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		int scale = Math.max(this.mc.displayWidth / Math.max(this.width, 1), 1);
		GL11.glEnable(GL11.GL_SCISSOR_TEST);
		GL11.glScissor((this.guiLeft + GRID_X) * scale,
				this.mc.displayHeight - (this.guiTop + GRID_Y + VIEW_H) * scale,
				VIEW_W * scale, VIEW_H * scale);

		GlStateManager.pushMatrix();
		GlStateManager.translate((float) (this.guiLeft + GRID_X), (float) (this.guiTop + GRID_Y), 0.0F);
		GlStateManager.scale(this.zoom, this.zoom, 1.0F);
		GlStateManager.translate(-this.camX, -this.camY, 0.0F);

		this.drawGridOverlay();
		this.drawColumns(columns);

		GlStateManager.popMatrix();
		GL11.glDisable(GL11.GL_SCISSOR_TEST);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private boolean inWindow(int mouseX, int mouseY) {
		int x = mouseX - this.guiLeft - GRID_X;
		int y = mouseY - this.guiTop - GRID_Y;
		return x >= 0 && y >= 0 && x < VIEW_W && y < VIEW_H;
	}

	private void clampCamera() {
		this.camX = this.clampAxis(this.camX, VIEW_W / this.zoom);
		this.camY = this.clampAxis(this.camY, VIEW_H / this.zoom);
	}

	private float clampAxis(float cam, float visible) {
		float span = FIELD_PX - visible;
		float low = Math.min(span, 0.0F);
		float high = Math.max(span, 0.0F);
		return Math.max(low, Math.min(cam, high));
	}

	private void drawColumns(RBMKColumn[] columns) {

		this.mc.getTextureManager().bindTexture(TEXTURE);

		for (int i = 0; i < columns.length; i++) {
			RBMKColumn col = columns[i];
			if (col == null) continue;

			float x = CELL * (i % GRID_COLS);
			float y = CELL * (i / GRID_COLS);

			this.drawTexturedModalRect(x, y, col.type.offset, TYPE_V, CELL, CELL);
			int heat = clamp((int) Math.ceil((col.heat - 20.0D) * CELL / Math.max(col.maxHeat, 1.0D)), 0, CELL);
			this.drawTexturedModalRect(x, y + CELL - heat, 0, HEAT_V - heat, CELL, heat);

			this.drawColumnDetail(col, x, y);

			if (this.selection[i]) this.drawTexturedModalRect(x, y, SELECTION_U, SELECTION_V, CELL, CELL);
		}
	}

	private void drawGridOverlay() {
		this.mc.getTextureManager().bindTexture(GRID_TEXTURE);

		float right = this.camX + VIEW_W / this.zoom;
		float bottom = this.camY + VIEW_H / this.zoom;

		for (int y = (int) Math.floor(this.camY / TILE) * TILE; y < bottom; y += TILE) {
			for (int x = (int) Math.floor(this.camX / TILE) * TILE; x < right; x += TILE) {
				this.drawGridTile(x, y, 0, 0, TILE, TILE);
			}
		}
	}

	private void drawGridTile(float x, float y, int u, int v, int w, int h) {
		float f = 1.0F / GRID_TEX_SIZE;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buffer.pos(x, y + h, this.zLevel).tex(u * f, (v + h) * f).endVertex();
		buffer.pos(x + w, y + h, this.zLevel).tex((u + w) * f, (v + h) * f).endVertex();
		buffer.pos(x + w, y, this.zLevel).tex((u + w) * f, v * f).endVertex();
		buffer.pos(x, y, this.zLevel).tex(u * f, v * f).endVertex();
		tessellator.draw();
	}

	private void drawLists(int mouseX, int mouseY) {
		TileEntityITServerCore.RbmkDebugMode[] modes = TileEntityITServerCore.RbmkDebugMode.values();
		List<TileEntityITServerCore.RbmkSample> samples = this.core.getRbmkSamples();

		String header = this.hasMode() ? modeText(modes[this.debugMode]) : LIST_PICK;
		this.drawListRow(LIST_X, LIST_Y, MODE_W,
				!this.modeListOpen && this.modeHeaderAt(mouseX, mouseY), this.hasMode(), header);

		int hoveredSample = this.sampleRowAt(mouseX, mouseY);
		for (int i = 0; i < samples.size() && i < SAMPLE_ROWS; i++) {
			boolean hovered = hoveredSample == i;
			this.drawListRow(SAMPLE_X, LIST_Y + i * LIST_ROW, SAMPLE_W, hovered, false,
					hovered ? LIST_REMOVE : this.sampleLabel(samples.get(i)));
		}

		String sendHeader = this.sendType < 0 ? SEND_PICK : TileEntityITServerCore.sendLabel(this.sendType);
		this.drawListRow(LIST_X, SEND_Y, MODE_W,
				!this.sendListOpen && this.sendHeaderAt(mouseX, mouseY), this.sendType >= 0, sendHeader);

		this.signalField.drawTextBox();
		if (this.signalField.getText().isEmpty()) {
			this.fontRenderer.drawString(this.signalHint(), this.guiLeft + LIST_X + 4, this.guiTop + SIGNAL_Y + 4,
					this.sendType < 0 ? LIST_TEXT_HOVER : SIGNAL_HINT_COLOUR);
		}

		this.drawModeNote(modes);

		if (this.sendListOpen) {
			int hoveredSend = this.sendOptionAt(mouseX, mouseY);
			for (int i = 0; i <= 1; i++) {
				this.drawListRow(LIST_X, this.sendOptionY(i), MODE_W, hoveredSend == i, i == this.sendType,
						TileEntityITServerCore.sendLabel(i));
			}
		}

		if (!this.modeListOpen) return;

		int hoveredOption = this.modeOptionAt(mouseX, mouseY);
		for (int i = 0; i < MODE_ROWS && this.modeScroll + i < modes.length; i++) {
			int mode = this.modeScroll + i;
			this.drawListRow(LIST_X, this.modeOptionY(i), MODE_W, hoveredOption == mode, mode == this.debugMode,
					modeText(modes[mode]));
		}

		this.drawModeScrollBar(modes.length);
	}

	private static String modeText(TileEntityITServerCore.RbmkDebugMode mode) {
		return mode.label + " " + mode.name;
	}

	private void drawModeNote(TileEntityITServerCore.RbmkDebugMode[] modes) {
		if (!this.hasMode()) return;

		TileEntityITServerCore.RbmkDebugMode mode = modes[this.debugMode];
		this.fontRenderer.drawSplitString(mode.note, this.guiLeft + LIST_X, this.guiTop + NOTE_Y, MODE_W, NOTE_COLOUR);

		if (mode.duplicate) {
			this.fontRenderer.drawSplitString(NOTE_DUPLICATE, this.guiLeft + LIST_X,
					this.guiTop + NOTE_Y + NOTE_LINES * this.fontRenderer.FONT_HEIGHT, MODE_W, NOTE_WARN_COLOUR);
		}
	}

	private void drawModeScrollBar(int total) {
		if (total <= MODE_ROWS) return;

		int top = this.guiTop + this.modeOptionY(0);
		int height = MODE_ROWS * LIST_ROW - 1;
		int thumb = Math.max(6, height * MODE_ROWS / total);
		int travel = height - thumb;
		int offset = travel * this.modeScroll / (total - MODE_ROWS);

		int x = this.guiLeft + LIST_X + MODE_W - 2;
		this.drawRect(x, top, x + 1, top + height, LIST_BACKDROP);
		this.drawRect(x, top + offset, x + 1, top + offset + thumb, LIST_TEXT);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private void drawListRow(int x, int y, int w, boolean hovered, boolean selected, String text) {
		int backdrop = selected ? LIST_BACKDROP_SEL : (hovered ? LIST_BACKDROP_HOVER : LIST_BACKDROP);
		this.drawRect(this.guiLeft + x, this.guiTop + y, this.guiLeft + x + w, this.guiTop + y + LIST_ROW - 1, backdrop);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		this.fontRenderer.drawString(text, this.guiLeft + x + 2, this.guiTop + y + 3,
				selected ? LIST_TEXT_SEL : (hovered ? LIST_TEXT_HOVER : LIST_TEXT));
	}

	private String sampleLabel(TileEntityITServerCore.RbmkSample sample) {
		TileEntityITServerCore.RbmkDebugMode[] modes = TileEntityITServerCore.RbmkDebugMode.values();
		String label = modes[Math.floorMod(sample.mode, modes.length)].label;

		if (!sample.signal.isEmpty()) {
			return label + " " + TileEntityITServerCore.sendLabel(sample.send) + ":" + sample.signal;
		}

		BlockPos target = this.core.getRbmkTarget();
		if (target == null || sample.cols.length == 0) return label;

		int index = sample.cols[0];
		String coords = (target.getX() + TileEntityITServerCore.rbmkX(index)) + ","
				+ (target.getZ() + TileEntityITServerCore.rbmkZ(index));
		return label + " " + coords + (sample.cols.length > 1 ? " x" + sample.cols.length : "");
	}

	private boolean modeHeaderAt(int mouseX, int mouseY) {
		int localX = mouseX - this.guiLeft - LIST_X;
		int localY = mouseY - this.guiTop - LIST_Y;
		return localX >= 0 && localX < MODE_W && localY >= 0 && localY < LIST_ROW;
	}

	private int modeOptionAt(int mouseX, int mouseY) {
		int localX = mouseX - this.guiLeft - LIST_X;
		int localY = mouseY - this.guiTop - this.modeOptionY(0);
		if (localX < 0 || localX >= MODE_W || localY < 0) return -1;

		int row = this.modeScroll + localY / LIST_ROW;
		return row < TileEntityITServerCore.RbmkDebugMode.values().length ? row : -1;
	}

	private int modeOptionY(int index) {
		return LIST_Y + LIST_ROW * (index + 1);
	}

	private boolean inModePopup(int mouseX, int mouseY) {
		int localX = mouseX - this.guiLeft - LIST_X;
		int localY = mouseY - this.guiTop - this.modeOptionY(0);
		return localX >= 0 && localX < MODE_W && localY >= 0 && localY < MODE_ROWS * LIST_ROW;
	}

	private void modeScrollBy(int delta) {
		int rows = TileEntityITServerCore.RbmkDebugMode.values().length;
		this.modeScroll = clamp(this.modeScroll + delta, 0, Math.max(0, rows - MODE_ROWS));
	}

	private String signalHint() {
		if (this.sendType < 0) return SIGNAL_NONE;
		return this.sendType == TileEntityITServerCore.SEND_MOUNT ? SIGNAL_MOUNT : SIGNAL_ROR;
	}

	private boolean sendHeaderAt(int mouseX, int mouseY) {
		int localX = mouseX - this.guiLeft - LIST_X;
		int localY = mouseY - this.guiTop - SEND_Y;
		return localX >= 0 && localX < MODE_W && localY >= 0 && localY < LIST_ROW;
	}

	private int sendOptionAt(int mouseX, int mouseY) {
		int localX = mouseX - this.guiLeft - LIST_X;
		int localY = mouseY - this.guiTop - this.sendOptionY(0);
		if (localX < 0 || localX >= MODE_W || localY < 0) return -1;

		int row = localY / LIST_ROW;
		return row <= 1 ? row : -1;
	}

	private int sendOptionY(int index) {
		return SIGNAL_Y + LIST_ROW * (index + 1);
	}

	private int sampleRowAt(int mouseX, int mouseY) {
		int row = this.listRowAt(mouseX, mouseY, SAMPLE_X, SAMPLE_W);
		return row >= 0 && row < this.core.getRbmkSamples().size() ? row : -1;
	}

	private int listRowAt(int mouseX, int mouseY, int x, int w) {
		int localX = mouseX - this.guiLeft - x;
		int localY = mouseY - this.guiTop - LIST_Y;
		if (localX < 0 || localX >= w || localY < 0) return -1;

		return localY / LIST_ROW;
	}

	private void sendSampleRemoval(int index) {
		NBTTagCompound request = new NBTTagCompound();
		request.setInteger("rbmkSampleRemove", index);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));

	}

	private void sendSampleLoad(int index) {
		List<TileEntityITServerCore.RbmkSample> samples = this.core.getRbmkSamples();
		if (index < 0 || index >= samples.size()) return;

		TileEntityITServerCore.RbmkSample sample = samples.get(index);
		this.debugMode = Math.floorMod(sample.mode, TileEntityITServerCore.RbmkDebugMode.values().length);
		Arrays.fill(this.selection, false);
		for (int cell : sample.cols) {
			if (cell >= 0 && cell < this.selection.length) this.selection[cell] = true;
		}

		this.sendType = sample.send;
		if (this.signalField != null) {
			this.signalField.setText(sample.signal);
			this.signalField.setEnabled(true);
		}

		NBTTagCompound request = new NBTTagCompound();
		request.setInteger("rbmkSampleLoad", index);
		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(request, this.core.getPos()));

	}

	private void drawColumnDetail(RBMKColumn col, float x, float y) {

		switch (col.type) {
			case COOLER: {
				RBMKColumn.CoolerColumn cooler = (RBMKColumn.CoolerColumn) col;
				int cryo = clamp((int) Math.ceil(cooler.cryo * 8.0D / Math.max(cooler.maxCryo, 1)), 0, 8);
				if (cryo > 0) this.drawTexturedModalRect(x + 3, y + CELL - cryo - 1, 123, 191 - cryo, 4, cryo);
				break;
			}
			case CONTROL: {
				RBMKColumn.ControlColumn control = (RBMKColumn.ControlColumn) col;
				if (control.color > -1) this.drawTexturedModalRect(x, y, control.color * CELL, COLOUR_V, CELL, CELL);

			}
			case CONTROL_AUTO: {
				RBMKColumn.ControlColumn control = (RBMKColumn.ControlColumn) col;
				int level = clamp(8 - (int) Math.ceil(control.level * 8.0D), 0, 8);
				this.drawTexturedModalRect(x + 4, y + 1, 24, 183, 2, level);
				break;
			}
			case FUEL:
			case FUEL_SIM: {
				RBMKColumn.FuelColumn fuel = (RBMKColumn.FuelColumn) col;
				int heat = clamp((int) Math.ceil((fuel.c_heat - 20.0D) * 8.0D / Math.max(fuel.c_maxHeat, 1.0D)), 0, 8);
				int enrichment = clamp((int) Math.ceil(fuel.enrichment * 8.0D), 0, 8);
				int xenon = clamp((int) Math.ceil(fuel.xenon * 8.0D / 100.0D), 0, 8);
				this.drawTexturedModalRect(x + 1, y + CELL - heat - 1, 11, 191 - heat, 2, heat);
				this.drawTexturedModalRect(x + 4, y + CELL - enrichment - 1, 14, 191 - enrichment, 2, enrichment);
				this.drawTexturedModalRect(x + 7, y + CELL - xenon - 1, 17, 191 - xenon, 2, xenon);
				break;
			}
			case BOILER: {
				RBMKColumn.BoilerColumn boiler = (RBMKColumn.BoilerColumn) col;
				int water = clamp((int) Math.ceil(boiler.water * 8.0D / Math.max(boiler.maxWater, 1.0D)), 0, 8);
				int steam = clamp((int) Math.ceil(boiler.steam * 8.0D / Math.max(boiler.maxSteam, 1.0D)), 0, 8);
				this.drawTexturedModalRect(x + 1, y + CELL - water - 1, 41, 191 - water, 3, water);
				this.drawTexturedModalRect(x + 6, y + CELL - steam - 1, 46, 191 - steam, 3, steam);

				SteamGrade grade = SteamGrade.of(boiler.steamType);
				if (grade.dotV > -1) this.drawTexturedModalRect(x + 4, y + grade.dotY, 44, grade.dotV, 2, 2);
				break;
			}
			case HEATEX: {
				RBMKColumn.HeaterColumn heater = (RBMKColumn.HeaterColumn) col;
				int cold = clamp((int) Math.ceil(heater.water * 8.0D / Math.max(heater.maxWater, 1.0D)), 0, 8);
				int hot = clamp((int) Math.ceil(heater.steam * 8.0D / Math.max(heater.maxSteam, 1.0D)), 0, 8);
				this.drawTexturedModalRect(x + 1, y + CELL - cold - 1, 131, 191 - cold, 3, cold);
				this.drawTexturedModalRect(x + 6, y + CELL - hot - 1, 136, 191 - hot, 3, hot);
				break;
			}
			default:
				break;
		}
	}

	private int cellAt(int mouseX, int mouseY) {
		if (!this.inWindow(mouseX, mouseY)) return -1;

		float x = this.camX + (mouseX - this.guiLeft - GRID_X) / this.zoom;
		float y = this.camY + (mouseY - this.guiTop - GRID_Y) / this.zoom;

		int cellX = (int) Math.floor(x / CELL);
		int cellY = (int) Math.floor(y / CELL);
		if (cellX < 0 || cellY < 0 || cellX >= GRID_COLS || cellY >= GRID_COLS) return -1;

		return cellX + cellY * GRID_COLS;
	}

	private void drawColumnInfo(RBMKColumn[] columns, int mouseX, int mouseY) {
		BlockPos target = this.core.getRbmkTarget();
		if (target == null) return;

		int index = this.cellAt(mouseX, mouseY);
		if (index < 0 || index >= columns.length) return;

		RBMKColumn col = columns[index];
		if (col == null) return;

		List<String> lines = new ArrayList<>();
		lines.add(col.type.toString());
		lines.add(I18nUtil.resolveKey("gui.ntm-it.server.rbmk.pos",
				target.getX() + TileEntityITServerCore.rbmkX(index), target.getZ() + TileEntityITServerCore.rbmkZ(index)));
		lines.addAll(col.getFancyStats());

		this.drawHoveringText(lines, mouseX, mouseY);
	}

	private static int clamp(int value, int min, int max) {
		return value < min ? min : (value > max ? max : value);
	}

	private static float clamp(float value, float min, float max) {
		return value < min ? min : (value > max ? max : value);
	}

	private enum SteamGrade {
		NONE(-1, 0),
		STEAM(183, 1),
		HOTSTEAM(185, 3),
		SUPERHOTSTEAM(187, 5),
		ULTRAHOTSTEAM(189, 7);

		private final int dotV;
		private final int dotY;

		SteamGrade(int dotV, int dotY) {
			this.dotV = dotV;
			this.dotY = dotY;
		}

		private static SteamGrade of(short fluidId) {
			FluidType fluid = Fluids.fromID(fluidId);
			if (fluid == Fluids.STEAM) return STEAM;
			if (fluid == Fluids.HOTSTEAM) return HOTSTEAM;
			if (fluid == Fluids.SUPERHOTSTEAM) return SUPERHOTSTEAM;
			if (fluid == Fluids.ULTRAHOTSTEAM) return ULTRAHOTSTEAM;
			return NONE;
		}
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
