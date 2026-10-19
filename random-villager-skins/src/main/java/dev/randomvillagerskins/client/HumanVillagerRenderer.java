package dev.randomvillagerskins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.randomvillagerskins.RandomVillagerSkinsClient;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.PlayerModelType;

/** Replaces client rendering only; the real entity remains a vanilla villager. */
public final class HumanVillagerRenderer extends MobRenderer<Villager, AvatarRenderState, PlayerModel> {
    private final PlayerModel wideModel;
    private final PlayerModel slimModel;

    public HumanVillagerRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel(context.bakeLayer(ModelLayers.PLAYER), false), 0.5F);
        wideModel = model;
        slimModel = new PlayerModel(context.bakeLayer(ModelLayers.PLAYER_SLIM), true);
        addLayer(new ItemInHandLayer<>(this));
        RandomVillagerSkinsClient.SKINS.requestReload();
    }

    @Override
    public AvatarRenderState createRenderState() {
        return new HumanVillagerRenderState();
    }

    @Override
    public void extractRenderState(Villager villager, AvatarRenderState state, float partialTick) {
        super.extractRenderState(villager, state, partialTick);
        HumanoidMobRenderer.extractHumanoidRenderState(villager, state, partialTick, itemModelResolver);
        // Preserve the villager's age flag when using the player render state.
        state.isBaby = villager.isBaby();
        state.skin = RandomVillagerSkinsClient.SKINS.skinFor(villager.getUUID());
        state.id = villager.getId();
        state.isSpectator = false;
        state.showHat = true;
        state.showJacket = true;
        state.showLeftPants = true;
        state.showRightPants = true;
        state.showLeftSleeve = true;
        state.showRightSleeve = true;
        state.showCape = false;
    }

    @Override
    public Identifier getTextureLocation(AvatarRenderState state) {
        return state.skin.body().texturePath();
    }

    @Override
    public void submit(AvatarRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        model = state.skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
        super.submit(state, poses, collector, camera);
    }

    @Override
    protected void scale(AvatarRenderState state, PoseStack poses) {
        float scale = 0.9375F * (state.isBaby ? 0.5F : 1.0F);
        poses.scale(scale, scale, scale);
    }
}
