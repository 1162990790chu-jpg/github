package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerLevel;

import net.mcreator.speedapple.event.ItemEvents;

@Mixin(BoneMealItem.class)
public abstract class BoneMealItemMixin {
	@Inject(method = "useOn(Lnet/minecraft/world/item/context/UseOnContext;)Lnet/minecraft/world/InteractionResult;", at = @At("HEAD"), cancellable = true)
	public void useOn(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (context.getLevel() instanceof ServerLevel) {
			boolean result = ItemEvents.BONEMEAL_USED.invoker().onBonemealUsed(context.getClickedPos(), (Entity) context.getPlayer(), context.getItemInHand(), context.getLevel().getBlockState(context.getClickedPos()));
			if (!result)
				cir.setReturnValue(InteractionResult.FAIL);
		}
	}
}