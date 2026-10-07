package com.ntmit.items;

import net.minecraft.item.Item;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class ModItems {

	public static final List<Item> ALL_ITEMS = new ArrayList<>();

	public static final Item server_connector = new ItemITServerConnector("server_connector");

	public static void preInit() {
		for (Item item : ALL_ITEMS) {
			ForgeRegistries.ITEMS.register(item);
		}
	}
}
