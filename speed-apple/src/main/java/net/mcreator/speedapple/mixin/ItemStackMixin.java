package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;

import net.mcreator.speedapple.event.BlockEvents;

@Mixin(ItemStack.class)
public abstract class ItemStackMixin {
	@Inject(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("TAIL"), cancellable = true)
	public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Player player = context.getPlayer();
		ItemStack copy = context.getItemInHand().copy();
		BlockState placedAgainst = player.level().getBlockState(context.getClickedPos().relative(context.getClickedFace()));
		if (!player.level().isEmptyBlock(context.getClickedPos().relative(context.getClickedFace()))) {
			boolean result = BlockEvents.BLOCK_MULTIPLACE.invoker().onMultiplaced(context.getClickedPos().relative(context.getClickedFace()), (Entity) player, placedAgainst, player.level().getBlockState(context.getClickedPos()));
			if (!result)
				cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}