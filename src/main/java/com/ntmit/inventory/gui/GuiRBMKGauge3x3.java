package com.ntmit.inventory.gui;

import com.hbm.handler.threading.PacketThreading;
import com.hbm.inventory.gui.GUIScreenRBMKKeyPad;
import com.hbm.packet.toserver.NBTControlPacket;
import com.hbm.util.SoundUtil;
import com.ntmit.lib.RefStrings;
import com.ntmit.tileentity.TileEntityRBMKGauge3x3;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;

public class GuiRBMKGauge3x3 extends GuiScreen {

	private static final ResourceLocation TEXTURE = new ResourceLocation(RefStrings.MODID, "textures/gui/rbmk_gauge_3x3.png");

	private static final int TEX_W = 366;
	private static final int TEX_H = 250;

	private static final int[] CELL_X = { 8, 125, 242, 8, 125, 242, 8, 125, 242 };
	private static final int[] CELL_Y = { 24, 24, 24, 79, 79, 79, 134, 134, 134 };

	private static final int[] ACTIVE_X = { 82, 199, 316, 82, 199, 316, 82, 199, 316 };
	private static final int[] ACTIVE_Y = { 25, 25, 25, 80, 80, 80, 135, 135, 135 };
	private static final int[] POLLING_X = { 101, 218, 335, 101, 218, 335, 101, 218, 335 };
	private static final int[] POLLING_Y = { 24, 24, 24, 79, 79, 79, 134, 134, 134 };


	private static final int BOX_TEXT_X = 21;
	private static final int BOX_TEXT_Y = 6;
	private static final int BOX_TEXT_W = 36;
	private static final int BOX_TEXT_H = 12;
	private static final int COLOR_X = 0;
	private static final int COLOR_Y = 0;
	private static final int CHANNEL_X = 0;
	private static final int CHANNEL_Y = 18;
	private static final int LABEL_X = 0;
	private static final int LABEL_Y = 36;
	private static final int MAX_X = 57;
	private static final int MAX_Y = 18;
	private static final int MIN_X = 57;
	private static final int MIN_Y = 36;

	private static final int PANEL_W = 366;
	private static final int PANEL_H = 196;

	private static final int ACTIVE_U = 18;
	private static final int ACTIVE_V = 196;
	private static final int ACTIVE_W = 16;
	private static final int ACTIVE_H = 16;

	private static final int POLLING_U = 0;
	private static final int POLLING_V = 196;
	private static final int POLLING_W = 18;
	private static final int POLLING_H = 18;
	private static final int APPLY_X = 336;
	private static final int APPLY_Y = 4;
	private static final int APPLY_SIZE = 17;

	private final TileEntityRBMKGauge3x3 gauge;

	private final GuiTextField[] color = new GuiTextField[TileEntityRBMKGauge3x3.UNITS];
	private final GuiTextField[] label = new GuiTextField[TileEntityRBMKGauge3x3.UNITS];
	private final GuiTextField[] channel = new GuiTextField[TileEntityRBMKGauge3x3.UNITS];
	private final GuiTextField[] min = new GuiTextField[TileEntityRBMKGauge3x3.UNITS];
	private final GuiTextField[] max = new GuiTextField[TileEntityRBMKGauge3x3.UNITS];
	private final boolean[] active = new boolean[TileEntityRBMKGauge3x3.UNITS];
	private final boolean[] polling = new boolean[TileEntityRBMKGauge3x3.UNITS];

	private int guiLeft;
	private int guiTop;

	public GuiRBMKGauge3x3(TileEntityRBMKGauge3x3 gauge) {
		this.gauge = gauge;
	}

	@Override
	public void initGui() {
		super.initGui();
		this.guiLeft = (this.width - PANEL_W) / 2;
		this.guiTop = (this.height - PANEL_H) / 2;
		Keyboard.enableRepeatEvents(true);

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			TileEntityRBMKGauge3x3.GaugeUnit unit = this.gauge.units[i];
			int cx = this.guiLeft + cellX(i);
			int cy = this.guiTop + cellY(i);

			this.color[i] = field(cx + COLOR_X, cy + COLOR_Y, 6, String.format("%06X", unit.color));
			this.channel[i] = field(cx + CHANNEL_X, cy + CHANNEL_Y, 10, unit.channel);
			this.label[i] = field(cx + LABEL_X, cy + LABEL_Y, 15, unit.label);
			this.max[i] = field(cx + MAX_X, cy + MAX_Y, 6, Long.toString(unit.max));
			this.min[i] = field(cx + MIN_X, cy + MIN_Y, 6, Long.toString(unit.min));

			this.active[i] = unit.active;
			this.polling[i] = unit.polling;
		}
	}

	private GuiTextField field(int boxX, int boxY, int limit, String text) {
		GuiTextField box = new GuiTextField(0, this.fontRenderer,
				boxX + BOX_TEXT_X, boxY + BOX_TEXT_Y, BOX_TEXT_W, BOX_TEXT_H);

		GUIScreenRBMKKeyPad.setupTextFieldStandard(box, limit, text == null ? "" : text);
		return box;
	}

	private void blit(int x, int y, int u, int v, int w, int h) {
		this.mc.getTextureManager().bindTexture(TEXTURE);

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		float fu = 1.0F / TEX_W;
		float fv = 1.0F / TEX_H;

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuffer();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
		buffer.pos(x, y + h, this.zLevel).tex(u * fu, (v + h) * fv).endVertex();
		buffer.pos(x + w, y + h, this.zLevel).tex((u + w) * fu, (v + h) * fv).endVertex();
		buffer.pos(x + w, y, this.zLevel).tex((u + w) * fu, v * fv).endVertex();
		buffer.pos(x, y, this.zLevel).tex(u * fu, v * fv).endVertex();
		tessellator.draw();

		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
	}

	private static int cellX(int index) {
		return CELL_X[index];
	}

	private static int cellY(int index) {
		return CELL_Y[index];
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		this.blit(this.guiLeft, this.guiTop, 0, 0, PANEL_W, PANEL_H);

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			int cx = this.guiLeft + cellX(i);
			int cy = this.guiTop + cellY(i);

			if (this.active[i]) {
				this.blit(this.guiLeft + ACTIVE_X[i], this.guiTop + ACTIVE_Y[i], ACTIVE_U, ACTIVE_V, ACTIVE_W, ACTIVE_H);
			}
			if (this.polling[i]) {
				this.blit(this.guiLeft + POLLING_X[i], this.guiTop + POLLING_Y[i], POLLING_U, POLLING_V, POLLING_W, POLLING_H);
			}

			this.color[i].drawTextBox();
			this.label[i].drawTextBox();
			this.min[i].drawTextBox();
			this.max[i].drawTextBox();
			this.channel[i].drawTextBox();
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}


	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
		super.mouseClicked(mouseX, mouseY, mouseButton);
		if (mouseButton != 0) return;

		if (hit(mouseX, mouseY, this.guiLeft + APPLY_X, this.guiTop + APPLY_Y, APPLY_SIZE)) {
			SoundUtil.playClickSound();
			this.sendSetup();
			return;
		}

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			int cx = this.guiLeft + cellX(i);
			int cy = this.guiTop + cellY(i);

			if (hit(mouseX, mouseY, this.guiLeft + ACTIVE_X[i], this.guiTop + ACTIVE_Y[i], ACTIVE_W)) {
				this.active[i] = !this.active[i];
				SoundUtil.playClickSound();
				return;
			}
			if (hit(mouseX, mouseY, this.guiLeft + POLLING_X[i], this.guiTop + POLLING_Y[i], POLLING_W)) {
				this.polling[i] = !this.polling[i];
				SoundUtil.playClickSound();
				return;
			}
		}

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			this.color[i].mouseClicked(mouseX, mouseY, mouseButton);
			this.label[i].mouseClicked(mouseX, mouseY, mouseButton);
			this.min[i].mouseClicked(mouseX, mouseY, mouseButton);
			this.max[i].mouseClicked(mouseX, mouseY, mouseButton);
			this.channel[i].mouseClicked(mouseX, mouseY, mouseButton);
		}
	}

	private static boolean hit(int mouseX, int mouseY, int x, int y, int size) {
		return mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size;
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) throws IOException {

		if (keyCode == Keyboard.KEY_ESCAPE) {
			super.keyTyped(typedChar, keyCode);
			return;
		}

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			this.color[i].textboxKeyTyped(typedChar, keyCode);
			this.label[i].textboxKeyTyped(typedChar, keyCode);
			this.min[i].textboxKeyTyped(typedChar, keyCode);
			this.max[i].textboxKeyTyped(typedChar, keyCode);
			this.channel[i].textboxKeyTyped(typedChar, keyCode);
		}
	}

	private void sendSetup() {
		NBTTagCompound data = new NBTTagCompound();
		int activeMask = 0;
		int pollingMask = 0;

		for (int i = 0; i < TileEntityRBMKGauge3x3.UNITS; i++) {
			TileEntityRBMKGauge3x3.GaugeUnit unit = this.gauge.units[i];
			if (this.active[i]) activeMask |= 1 << i;
			if (this.polling[i]) pollingMask |= 1 << i;

			data.setString("label" + i, this.label[i].getText());
			data.setString("channel" + i, this.channel[i].getText());

			try {
				data.setInteger("color" + i, Integer.parseInt(this.color[i].getText().trim(), 16));
			} catch (NumberFormatException e) {
				data.setInteger("color" + i, unit.color);
			}
			try {
				data.setLong("min" + i, Long.parseLong(this.min[i].getText().trim()));
			} catch (NumberFormatException e) {
				data.setLong("min" + i, unit.min);
			}
			try {
				data.setLong("max" + i, Long.parseLong(this.max[i].getText().trim()));
			} catch (NumberFormatException e) {
				data.setLong("max" + i, unit.max);
			}
		}

		data.setInteger("active", activeMask);
		data.setInteger("polling", pollingMask);

		PacketThreading.createSendToServerThreadedPacket(new NBTControlPacket(data,
				this.gauge.getPos().getX(), this.gauge.getPos().getY(), this.gauge.getPos().getZ()));
	}

	@Override
	public void onGuiClosed() {
		super.onGuiClosed();
		Keyboard.enableRepeatEvents(false);
	}

	@Override
	public boolean doesGuiPauseGame() {
		return false;
	}
}
