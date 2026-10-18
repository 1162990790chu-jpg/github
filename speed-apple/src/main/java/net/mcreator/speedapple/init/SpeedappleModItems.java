/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.speedapple.init;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;

import net.mcreator.speedapple.item.SpeedAppleItem;
import net.mcreator.speedapple.SpeedappleMod;

import java.util.function.Function;

public class SpeedappleModItems {
	public static Item SPEED_APPLE;

	public static void load() {
		SPEED_APPLE = register("speed_apple", SpeedAppleItem::new);
	}

	// Start of user code block custom items
	// End of user code block custom items
	private static <I extends Item> I register(String name, Function<Item.Properties, ? extends I> supplier) {
		return (I) Items.registerItem(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(SpeedappleMod.MODID, name)), (Function<Item.Properties, Item>) supplier);
	}
}