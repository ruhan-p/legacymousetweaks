package com.infloat.legacymousetweaks.client;

import com.infloat.legacymousetweaks.config.ConfigManager;
import com.infloat.legacymousetweaks.config.MouseTweaksConfig;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

public class MouseTweaksConfigScreen extends Screen {
    private final Screen parent;

    public MouseTweaksConfigScreen(Screen parent) {
        this.parent = parent;
    }

    @Override
    public void init() {
        this.buttons.clear();
        int y = this.height / 4;

        this.buttons.add(new ButtonWidget(0, this.width / 2 - 100, y, 200, 20, ""));
        y += 24;
        this.buttons.add(new ButtonWidget(1, this.width / 2 - 100, y, 200, 20, ""));
        y += 24;
        this.buttons.add(new ButtonWidget(2, this.width / 2 - 100, y, 200, 20, ""));
        y += 24;
        this.buttons.add(new ButtonWidget(3, this.width / 2 - 100, y, 200, 20, ""));
        y += 24;
        this.buttons.add(new ButtonWidget(4, this.width / 2 - 100, y, 200, 20, ""));
        y += 24;
        this.buttons.add(new ButtonWidget(5, this.width / 2 - 100, y, 200, 20, ""));
        y += 28;
        this.buttons.add(new ButtonWidget(100, this.width / 2 - 100, y, 98, 20, "Done"));
        this.buttons.add(new ButtonWidget(101, this.width / 2 + 2, y, 98, 20, "Cancel"));

        this.lmt_updateButtonLabels();
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        MouseTweaksConfig config = ConfigManager.get();

        switch (button.id) {
            case 0:
                config.rmbTweak = !config.rmbTweak;
                break;
            case 1:
                config.lmbTweakWithItem = !config.lmbTweakWithItem;
                break;
            case 2:
                config.lmbTweakWithoutItem = !config.lmbTweakWithoutItem;
                break;
            case 3:
                config.wheelTweak = !config.wheelTweak;
                break;
            case 4:
                config.wheelSearchOrder = config.wheelSearchOrder == 1 ? 0 : 1;
                break;
            case 5:
                config.wheelScrollDirection = (config.wheelScrollDirection + 1) % 3;
                break;
            case 100:
                ConfigManager.save();
                this.client.setScreen(this.parent);
                return;
            case 101:
                ConfigManager.load();
                this.client.setScreen(this.parent);
                return;
            default:
                return;
        }

        this.lmt_updateButtonLabels();
    }

    @Override
    public void removed() {
        ConfigManager.save();
    }

    @Override
    public void render(int mouseX, int mouseY, float tickDelta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "Legacy Mouse Tweaks Config", this.width / 2, 20, 0xFFFFFF);
        super.render(mouseX, mouseY, tickDelta);
    }

    private void lmt_updateButtonLabels() {
        MouseTweaksConfig config = ConfigManager.get();
        for (ButtonWidget button : this.buttons) {
            switch (button.id) {
                case 0:
                    button.message = "RMB Tweak: " + lmt_onOff(config.rmbTweak);
                    break;
                case 1:
                    button.message = "LMB Tweak (With Item): " + lmt_onOff(config.lmbTweakWithItem);
                    break;
                case 2:
                    button.message = "LMB Tweak (Without Item): " + lmt_onOff(config.lmbTweakWithoutItem);
                    break;
                case 3:
                    button.message = "Wheel Tweak: " + lmt_onOff(config.wheelTweak);
                    break;
                case 4:
                    button.message = "Wheel Search Order: " + (config.wheelSearchOrder == 1 ? "Last to First" : "First to Last");
                    break;
                case 5:
                    button.message = "Wheel Direction: " + lmt_direction(config.wheelScrollDirection);
                    break;
                default:
                    break;
            }
        }
    }

    private static String lmt_onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String lmt_direction(int mode) {
        if (mode == 1) {
            return "Inverted";
        }
        if (mode == 2) {
            return "Position Aware";
        }
        return "Normal";
    }
}
