package com.ntmit.lib;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

public final class ModSoundEvents {

	public static final SoundEvent RBMK_FIRST_EXPLOSION = create("rbmk_first_explosion");

	private static SoundEvent create(String name) {
		ResourceLocation id = new ResourceLocation(RefStrings.MODID, name);
		SoundEvent event = new SoundEvent(id);
		event.setRegistryName(id);
		return event;
	}

	public static void register() {
		ForgeRegistries.SOUND_EVENTS.register(RBMK_FIRST_EXPLOSION);
	}

	private ModSoundEvents() {
	}
}
