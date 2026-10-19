package dev.randomvillagerskins.mixin;

import dev.randomvillagerskins.client.HumanVillagerRenderer;
import dev.randomvillagerskins.client.HumanVillagerRenderState;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMixin {
    @Shadow private Map<EntityType<?>, EntityRenderer<?, ?>> renderers;

    // Vanilla sends every AvatarRenderState to a player renderer, including NPC states.
    @Inject(method = "getRenderer(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
        at = @At("HEAD"), cancellable = true)
    private void keepVillagerRenderer(EntityRenderState state, CallbackInfoReturnable<EntityRenderer<?, ?>> callback) {
        if (state instanceof HumanVillagerRenderState) {
            EntityRenderer<?, ?> renderer = renderers.get(state.entityType);
            if (renderer instanceof HumanVillagerRenderer) callback.setReturnValue(renderer);
        }
    }
}
