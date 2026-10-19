package dev.tabletopfoods;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.player.ItemEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TabletopFoods implements ModInitializer {
    public static final String MOD_ID = "tabletopfoods";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    private static final Identifier FOOD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "placed_food");
    public static final EntityType<PlacedFood> PLACED_FOOD = Registry.register(
        BuiltInRegistries.ENTITY_TYPE, FOOD_ID,
        EntityType.Builder.<PlacedFood>of(PlacedFood::new, MobCategory.MISC)
            .sized(0.72F, 0.7F).clientTrackingRange(8).updateInterval(20).noLootTable()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, FOOD_ID)));

    @Override
    public void onInitialize() {
        ItemEvents.USE_ON.register(FoodPlacement::useOn);
        LOGGER.info("Tabletop Foods ready: persistent food placement and sculpted food models.");
    }
}
