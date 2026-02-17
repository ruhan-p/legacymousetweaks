package com.infloat.legacymousetweaks.client;

import io.github.prospector.modmenu.api.ModMenuApi;

import java.util.function.Function;

public class LegacyMouseTweaksModMenuApi implements ModMenuApi {
    @Override
    public String getModId() {
        return "modid";
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Function getConfigScreenFactory() {
        return parent -> new MouseTweaksConfigScreen((net.minecraft.client.gui.screen.Screen) parent);
    }
}
