package com.ntmit.mixin;

import com.hbm.inventory.gui.GUIRBMKConsole;
import com.hbm.tileentity.machine.rbmk.RBMKColumn;
import com.ntmit.lib.RbmkJumpHandler;

import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GUIRBMKConsole.class, priority = 1000)
public abstract class MixinGUIRBMKConsole {

	@Redirect(method = {"drawScreen(IIF)V", "func_73863_a(IIF)V"}, at = @At(value = "INVOKE", target = "Lcom/hbm/tileentity/machine/rbmk/RBMKColumn;getFancyStats()Ljava/util/List;"), require = 1, remap = false)
	private List<String> ntmit$boilerStats(RBMKColumn col) {
		List<String> stats = col.getFancyStats();
		if (!(col instanceof RBMKColumn.BoilerColumn)) return stats;
		RBMKColumn.BoilerColumn boiler = (RBMKColumn.BoilerColumn) col;
		List<String> out = new ArrayList<>();
		for (String line : stats) {
			if (line.startsWith(TextFormatting.WHITE.toString())) continue;
			out.add(line);
		}
		if (boiler.steam >= RbmkJumpHandler.BOILER_DISPLAY_MAX_VALUE) {
			out.add(TextFormatting.RED + "ERROR");
		} else {
			out.add(TextFormatting.WHITE + String.valueOf(boiler.steam) + "/" + RbmkJumpHandler.BOILER_DISPLAY_CAPACITY + "mB");
			if (boiler.steam >= RbmkJumpHandler.BOILER_WARN_RED) {
				out.add(TextFormatting.RED + I18n.format("gui.ntm-it.boiler.warning"));
			} else if (boiler.steam >= RbmkJumpHandler.BOILER_WARN_ORANGE) {
				out.add(TextFormatting.GOLD + I18n.format("gui.ntm-it.boiler.warning"));
			} else if (boiler.steam >= RbmkJumpHandler.BOILER_DISPLAY_CAPACITY) {
				out.add(TextFormatting.YELLOW + I18n.format("gui.ntm-it.boiler.caution"));
			}
		}
		return out;
	}
}
