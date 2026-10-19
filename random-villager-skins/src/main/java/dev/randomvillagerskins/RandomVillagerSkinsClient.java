package dev.randomvillagerskins;

import dev.randomvillagerskins.client.HumanVillagerRenderer;
import dev.randomvillagerskins.client.SkinLibrary;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class RandomVillagerSkinsClient implements ClientModInitializer {
    public static final String MOD_ID = "randomvillagerskins";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final SkinLibrary SKINS = new SkinLibrary(
        FabricLoader.getInstance().getConfigDir().resolve(MOD_ID).resolve("skins"));

    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(EntityTypes.VILLAGER, HumanVillagerRenderer::new);
        ClientTickEvents.END_CLIENT_TICK.register(client -> SKINS.reloadIfNeeded(client.getTextureManager()));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("villagerskins").then(literal("reload").executes(context -> {
                SKINS.requestReload();
                context.getSource().sendFeedback(Component.literal("已请求重载村民皮肤，稍后生效。"));
                return 1;
            }))));
        LOGGER.info("Random human villager renderer registered; skins are assigned by villager UUID.");
    }
}
