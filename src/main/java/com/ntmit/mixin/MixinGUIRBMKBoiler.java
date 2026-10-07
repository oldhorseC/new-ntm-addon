package com.ntmit.mixin;

import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.gui.GUIRBMKBoiler;
import com.hbm.inventory.gui.GuiInfoContainer;
import com.hbm.inventory.gui.element.GUIElements;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(GUIRBMKBoiler.class)
public class MixinGUIRBMKBoiler {

	@Redirect(method = "drawGuiContainerBackgroundLayer(FII)V", at = @At(value = "INVOKE", target = "Lcom/hbm/inventory/fluid/tank/FluidTankNTM;getFill()I", ordinal = 1, remap = false), require = 1)
	private int ntmit$displayBarFill(FluidTankNTM tank) {
		return Math.min(tank.getFill(), RbmkJumpHandler.BOILER_DISPLAY_CAPACITY);
	}

	@Redirect(method = "drawGuiContainerBackgroundLayer(FII)V", at = @At(value = "INVOKE", target = "Lcom/hbm/inventory/fluid/tank/FluidTankNTM;getMaxFill()I", ordinal = 1, remap = false), require = 1)
	private int ntmit$displayBarMax(FluidTankNTM tank) {
		return RbmkJumpHandler.BOILER_DISPLAY_CAPACITY;
	}

	@Redirect(method = "drawScreen(IIF)V", at = @At(value = "INVOKE", target = "Lcom/hbm/inventory/fluid/tank/FluidTankNTM;renderTankInfo(Lcom/hbm/inventory/gui/GuiInfoContainer;IIIIII)V", ordinal = 1, remap = false), require = 1)
	private void ntmit$displayTooltip(FluidTankNTM tank, GuiInfoContainer gui, int mouseX, int mouseY, int x, int y, int width, int height) {
		if (x > mouseX || x + width <= mouseX || y >= mouseY || y + height < mouseY) return;
		int fill = tank.getFill();
		List<String> list = new ArrayList<>();
		list.add(tank.getTankType().getLocalizedName());
		if (fill >= RbmkJumpHandler.BOILER_DISPLAY_MAX_VALUE) {
			list.add(TextFormatting.RED + "ERROR");
		} else {
			list.add(Math.min(fill, RbmkJumpHandler.BOILER_DISPLAY_MAX_VALUE) + "/" + RbmkJumpHandler.BOILER_DISPLAY_CAPACITY + "mB");
			if (fill >= RbmkJumpHandler.BOILER_WARN_RED) {
				list.add(TextFormatting.RED + I18n.format("gui.ntm-it.boiler.warning"));
			} else if (fill >= RbmkJumpHandler.BOILER_WARN_ORANGE) {
				list.add(TextFormatting.GOLD + I18n.format("gui.ntm-it.boiler.warning"));
			} else if (fill >= RbmkJumpHandler.BOILER_DISPLAY_CAPACITY) {
				list.add(TextFormatting.YELLOW + I18n.format("gui.ntm-it.boiler.caution"));
			}
		}
		if (tank.getPressure() != 0) list.add(TextFormatting.RED + String.valueOf(tank.getPressure()));
		tank.getTankType().addInfo(list);
		GUIElements.drawHoveringTextFluid(list, mouseX, mouseY, gui.getFontRenderer(), gui.getItemRenderer(), gui.width, gui.height, tank.getTankType());
	}
}
