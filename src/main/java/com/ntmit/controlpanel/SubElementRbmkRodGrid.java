package com.ntmit.controlpanel;

import com.hbm.inventory.control_panel.GuiControlEdit;
import com.hbm.inventory.control_panel.controls.configs.SubElementBaseConfig;
import com.hbm.inventory.control_panel.types.DataValue;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Map;

@SideOnly(Side.CLIENT)
public class SubElementRbmkRodGrid extends SubElementBaseConfig {

	private static final int[] TRANSFORM = {18, 69, 88, 88};

	private GuiTextField widthField;
	private GuiTextField lengthField;
	private String widthText;
	private String lengthText;

	public SubElementRbmkRodGrid(GuiControlEdit gui, Map<String, DataValue> map) {
		super(gui, map);
		this.widthText = readText(map, "width", ControlRbmkRodGrid.DEFAULT_WIDTH);
		this.lengthText = readText(map, "length", ControlRbmkRodGrid.DEFAULT_LENGTH);
	}

	private static String readText(Map<String, DataValue> map, String key, float def) {
		DataValue v = map.get(key);
		float n = v == null ? def : v.getNumber();
		return Float.toString(Math.round(n * 100.0F) / 100.0F);
	}

	private static float parse(String text, float def) {
		try {
			return Math.max(ControlRbmkRodGrid.MIN_SIZE,
					Math.min(ControlRbmkRodGrid.MAX_SIZE, Float.parseFloat(text.trim())));
		} catch (NumberFormatException e) {
			return def;
		}
	}

	@Override
	public void fillConfigs(Map<String, DataValue> configs) {
		SubElementRbmkRodGrid.putFloatConfig(configs, "width",
				parse(this.widthText, ControlRbmkRodGrid.DEFAULT_WIDTH));
		SubElementRbmkRodGrid.putFloatConfig(configs, "length",
				parse(this.lengthText, ControlRbmkRodGrid.DEFAULT_LENGTH));
	}

	@Override
	public void initGui() {
		int cX = this.gui.width / 2;
		this.widthField = new GuiTextField(this.gui.currentButtonId(), Minecraft.getMinecraft().fontRenderer,
				cX - 10, this.gui.getGuiTop() + 50, 60, 18);
		this.widthField.setText(this.widthText);
		this.lengthField = new GuiTextField(this.gui.currentButtonId(), Minecraft.getMinecraft().fontRenderer,
				cX + 60, this.gui.getGuiTop() + 50, 60, 18);
		this.lengthField.setText(this.lengthText);
		super.initGui();
	}

	@Override
	protected void update() {
		super.update();
		this.widthField.updateCursorCounter();
		this.lengthField.updateCursorCounter();
		this.widthText = this.widthField.getText();
		this.lengthText = this.lengthField.getText();
	}

	@Override
	protected void drawScreen() {
		this.widthField.drawTextBox();
		this.lengthField.drawTextBox();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int button) {
		super.mouseClicked(mouseX, mouseY, button);
		this.widthField.mouseClicked(mouseX, mouseY, button);
		this.lengthField.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	protected void keyTyped(char typedChar, int keyCode) {
		super.keyTyped(typedChar, keyCode);
		this.widthField.textboxKeyTyped(typedChar, keyCode);
		this.lengthField.textboxKeyTyped(typedChar, keyCode);
	}

	@Override
	public void enableButtons(boolean enable) {
		this.widthField.setEnabled(enable);
		this.widthField.setVisible(enable);
		this.lengthField.setEnabled(enable);
		this.lengthField.setVisible(enable);
	}

	@Override
	public int[] getPreviewTransform() {
		return TRANSFORM;
	}
}

