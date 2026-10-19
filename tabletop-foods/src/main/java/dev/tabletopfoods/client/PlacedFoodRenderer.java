package dev.tabletopfoods.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.tabletopfoods.PlacedFood;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;

public final class PlacedFoodRenderer extends EntityRenderer<PlacedFood, PlacedFoodRenderState> {
    private final ItemModelResolver items;

    public PlacedFoodRenderer(EntityRendererProvider.Context context) {
        super(context);
        items = context.getItemModelResolver();
        shadowRadius = 0.22F;
    }

    @Override public PlacedFoodRenderState createRenderState() { return new PlacedFoodRenderState(); }

    @Override
    public void extractRenderState(PlacedFood food, PlacedFoodRenderState state, float partialTick) {
        super.extractRenderState(food, state, partialTick);
        items.updateForNonLiving(state.food, food.getFood(), ItemDisplayContext.GROUND, food);
        state.yaw = food.getYRot();
    }

    @Override
    public void submit(PlacedFoodRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poses, collector, camera);
        if (state.food.isEmpty()) return;
        poses.pushPose();
        poses.mulPose(Axis.YP.rotationDegrees(-state.yaw));
        // Ground transforms differ between resource packs; anchor the actual model's lowest point.
        poses.translate(0, -state.food.getModelBoundingBox().minY, 0);
        state.food.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poses.popPose();
    }
}
