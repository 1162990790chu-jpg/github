package dev.tabletopfoods.client;

import dev.tabletopfoods.TabletopFoods;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class TabletopFoodsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EntityRendererRegistry.register(TabletopFoods.PLACED_FOOD, PlacedFoodRenderer::new);
    }
}
