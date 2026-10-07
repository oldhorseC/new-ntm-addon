package com.ntmit.mixin;

import zone.rong.mixinbooter.IEarlyMixinLoader;
import zone.rong.mixinbooter.MixinLoader;

import java.util.Collections;
import java.util.List;

@MixinLoader
public class NtmitEarlyMixinLoader implements IEarlyMixinLoader {

    @Override
    public List<String> getMixinConfigs() {
        return Collections.singletonList("mixins.ntm-it.early.json");
    }
}
