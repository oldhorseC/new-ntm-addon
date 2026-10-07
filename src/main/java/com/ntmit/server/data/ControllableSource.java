package com.ntmit.server.data;

import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.IControllable;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.Map;

public class ControllableSource implements IServerSource {

	private final IControllable host;

	public ControllableSource(IControllable host) {
		this.host = host;
	}

	@Override
	public boolean isValid(World world) {
		if (host.getControlWorld() != world) return false;
		return world.getTileEntity(host.getControlPos()) == host;
	}

	@Override
	public DataValue read(World world, String field) {
		if (!isValid(world)) return null;
		return host.getQueryData().get(field);
	}

	@Override
	public Map<String, DataValue> listFields(World world) {
		if (!isValid(world)) return Collections.emptyMap();
		return host.getQueryData();
	}
}
