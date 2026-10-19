package dev.tabletopfoods.client;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState;

public final class PlacedFoodRenderState extends EntityRenderState {
    public final ItemStackRenderState food = new ItemStackRenderState();
    public float yaw;
}
