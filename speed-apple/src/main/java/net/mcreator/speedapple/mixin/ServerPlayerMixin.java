package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@ModifyExpressionValue(method = "drop(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Inventory;removeFromSelected(Z)Lnet/minecraft/world/item/ItemStack;"))
	private ItemStack drop(ItemStack removed, boolean all) {
		ServerPlayer self = (ServerPlayer) (Object) this;
		return removed;
	}
}