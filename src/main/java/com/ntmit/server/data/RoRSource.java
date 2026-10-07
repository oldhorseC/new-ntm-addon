package com.ntmit.server.data;

import com.hbm.api.redstoneoverradio.IRORInfo;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.inventory.control_panel.types.DataValue;
import com.hbm.inventory.control_panel.types.DataValueString;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RoRSource implements IServerSource {

	private final TileEntity host;

	public RoRSource(TileEntity host) {
		this.host = host;
	}

	@Override
	public boolean isValid(World world) {
		return world.getTileEntity(host.getPos()) == host;
	}

	@Override
	public Map<String, DataValue> listFields(World world) {
		if (!isValid(world)) return Collections.emptyMap();

		Map<String, DataValue> fields = new LinkedHashMap<>();
		for (String entry : ((IRORInfo) this.host).getFunctionInfo()) {
			if (entry == null || entry.isEmpty()) continue;
			fields.put(entry, new DataValueString(this.valueOfEntry(entry)));
		}
		return fields;
	}

	@Override
	public DataValue read(World world, String field) {
		if (!isValid(world)) return null;
		return new DataValueString(this.valueOfEntry(field));
	}

	private String valueOfEntry(String entry) {
		if (!entry.startsWith(IRORInfo.PREFIX_VALUE)) return "(function)";
		return this.valueOf(entry);
	}

	private String valueOf(String name) {
		if (this.host instanceof IRORValueProvider provider) {
			String value = provider.provideRORValue(name);
			return value == null ? "" : value;
		}
		return "";
	}
}
