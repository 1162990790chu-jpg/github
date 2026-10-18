package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;

import net.mcreator.speedapple.event.BlockEvents;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@Inject(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
	public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		BlockPlaceContext placeContext = new BlockPlaceContext(context);
		boolean result = BlockEvents.BLOCK_PLACE.invoker().onBlockPlaced(context.getClickedPos(), (Entity) placeContext.getPlayer(), ((BlockItem) placeContext.getItemInHand().getItem()).getBlock().defaultBlockState(),
				placeContext.getPlayer().level().getBlockState(context.getClickedPos()));
		if (!result)
			cir.setReturnValue(InteractionResult.FAIL);
	}
}