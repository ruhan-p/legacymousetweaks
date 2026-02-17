package com.infloat.legacymousetweaks;

import com.infloat.legacymousetweaks.config.ConfigManager;
import net.fabricmc.api.ModInitializer;

public class LegacyMouseTweaks implements ModInitializer {
    @Override
    public void onInitialize() {
        ConfigManager.load();
    }
}
