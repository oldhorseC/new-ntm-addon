package com.ntmit.server.data;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORInteractive;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.control_panel.IControllable;
import com.hbm.tileentity.machine.rbmk.TileEntityRBMKConsole;
import net.minecraft.tileentity.TileEntity;

public final class ServerSources {

	private ServerSources() {
	}

	public static IServerSource of(TileEntity tile) {

		if (tile instanceof TileEntityRBMKConsole console) return new RBMKConsoleSource(console);

		IServerSource ror = tile instanceof IRORInfo ? new RoRSource(tile) : null;
		IServerSource panel = tile instanceof IControllable controllable ? new ControllableSource(controllable) : null;

		if (ror == null) return panel;
		if (panel == null) return ror;
		return new MergedSource(ror, panel);
	}

	public static String describe(TileEntity tile) {
		if (tile == null) return "no tile entity";

		StringBuilder out = new StringBuilder(tile.getClass().getName());
		if (tile instanceof TileEntityRBMKConsole) out.append(" [RBMK console]");
		else if (tile instanceof IRORValueProvider) out.append(" [IRORValueProvider]");
		else if (tile instanceof IRORInteractive) out.append(" [IRORInteractive]");
		else if (tile instanceof IRORInfo) out.append(" [IRORInfo]");
		if (tile instanceof IControllable) out.append(" [IControllable]");
		return out.toString();
	}
}

