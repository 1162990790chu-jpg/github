package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;

import com.google.common.collect.Lists;

@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
	@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;", at = @At("HEAD"))
	public void assemble(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
		ItemStack itemStack3;
		ItemStack itemStack;
		ArrayList<ItemStack> list = Lists.newArrayList();
		for (int i = 0; i < input.ingredientCount(); ++i) {
			ItemStack itemStack2;
			itemStack = input.getItem(i);
			if (itemStack.isEmpty())
				continue;
			list.add(itemStack);
		}
	}
}