package com.ntmit.server.data;

import com.hbm.inventory.control_panel.types.DataValue;
import net.minecraft.world.World;

import java.util.Map;

public interface IServerSource {

	boolean isValid(World world);

	DataValue read(World world, String field);

	Map<String, DataValue> listFields(World world);
}
