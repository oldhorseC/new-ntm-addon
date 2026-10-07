package com.ntmit.server.data;

import com.hbm.inventory.control_panel.types.DataValue;
import net.minecraft.world.World;

import java.util.LinkedHashMap;
import java.util.Map;

public class MergedSource implements IServerSource {

	private final IServerSource[] parts;

	public MergedSource(IServerSource... parts) {
		this.parts = parts;
	}

	@Override
	public boolean isValid(World world) {
		for (IServerSource part : this.parts) {
			if (part.isValid(world)) return true;
		}
		return false;
	}

	@Override
	public Map<String, DataValue> listFields(World world) {
		Map<String, DataValue> fields = new LinkedHashMap<>();
		for (IServerSource part : this.parts) {
			if (!part.isValid(world)) continue;
			for (Map.Entry<String, DataValue> entry : part.listFields(world).entrySet()) {
				fields.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		return fields;
	}

	@Override
	public DataValue read(World world, String field) {
		for (IServerSource part : this.parts) {
			DataValue value = part.read(world, field);
			if (value != null) return value;
		}
		return null;
	}
}
