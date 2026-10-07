package com.ntmit.controlpanel;

import com.hbm.inventory.control_panel.Control;
import com.hbm.inventory.control_panel.ControlRegistry;

public final class NtmitControlPanel {

	public static final String RBMK_ROD_GRID = "ntmit_rbmk_rod_grid";

	private NtmitControlPanel() {
	}

	public static void register() {
		if (ControlRegistry.isRegistered(RBMK_ROD_GRID)) return;

		Control prototype = new ControlRbmkRodGrid("NTM-IT RBMK Rod Grid", RBMK_ROD_GRID, null);
		ControlRegistry.addonControls.add(prototype);
		ControlRegistry.registry.put(RBMK_ROD_GRID, prototype);
	}
}

