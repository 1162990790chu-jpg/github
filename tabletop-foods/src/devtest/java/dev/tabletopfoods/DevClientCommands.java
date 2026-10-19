package dev.tabletopfoods;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.Minecraft;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.PauseScreen;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class DevClientCommands implements ClientModInitializer {
    @Override public void onInitializeClient() {
        if (Boolean.getBoolean("tabletopfoods.automatedTest")) {
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                client.options.pauseOnLostFocus = false;
                if (client.gui.screen() instanceof PauseScreen) client.gui.setScreen(null);
            });
        }
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> dispatcher.register(
            literal("tabletopquit").executes(context -> { Minecraft.getInstance().stop(); return 1; })));
    }
}
