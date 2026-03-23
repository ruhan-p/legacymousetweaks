package com.infloat.legacymousetweaks.client;

import net.fabricmc.api.ClientModInitializer;
import net.legacyfabric.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.legacyfabric.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.input.Keyboard;

public class LegacyMouseTweaksClient implements ClientModInitializer {
    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.legacymousetweaks.open_config",
            Keyboard.KEY_M,
            "key.categories.legacymousetweaks"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfigKey.wasPressed()) {
                client.setScreen(new MouseTweaksConfigScreen(client.currentScreen));
            }
        });
    }
}
