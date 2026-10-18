/*
 *	MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.speedapple.init;

import net.minecraft.world.item.CreativeModeTabs;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

public class SpeedappleModTabs {
	public static void load() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(tabData -> {
			tabData.accept(SpeedappleModItems.SPEED_APPLE);
		});
	}
}