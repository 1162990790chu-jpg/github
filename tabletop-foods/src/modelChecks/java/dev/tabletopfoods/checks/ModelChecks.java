package dev.tabletopfoods.checks;

import com.google.gson.*;
import net.minecraft.world.food.Foods;
import net.minecraft.world.food.FoodProperties;
import java.nio.file.*;
import java.util.*;

/** Validates coverage against the actual Minecraft version, plus geometry and model references. */
public final class ModelChecks {
    static void require(boolean test, String message) { if (!test) throw new AssertionError(message); }
    static JsonObject object(Path path) throws Exception { return JsonParser.parseString(Files.readString(path)).getAsJsonObject(); }
    public static void main(String[] args) throws Exception {
        Path resources = Path.of(args[0]);
        JsonArray catalog = JsonParser.parseString(Files.readString(resources.resolve("tabletopfoods-food-models.json"))).getAsJsonArray();
        Set<String> actual = new HashSet<>();
        for (JsonElement entry : catalog) require(actual.add(entry.getAsJsonObject().get("id").getAsString()), "Duplicate catalog item");
        int vanilla = 0;
        for (var field : Foods.class.getDeclaredFields()) {
            if (field.getType() != FoodProperties.class) continue;
            String id = "minecraft:" + field.getName().toLowerCase(Locale.ROOT);
            require(actual.contains(id), "Vanilla food is missing: " + id);
            vanilla++;
        }
        for (String id : List.of("speedapple:speed_apple", "speedapple:night_vision_carrot", "speedapple:jump_gummy"))
            require(actual.contains(id), "Companion food is missing: " + id);
        require(actual.size() == vanilla + 3, "Unexpected catalog coverage");
        int elements = 0;
        for (String id : actual) {
            String[] parts = id.split(":");
            JsonObject item = object(resources.resolve("assets/" + parts[0] + "/items/" + parts[1] + ".json")).getAsJsonObject("model");
            require(item.get("type").getAsString().equals("minecraft:model"), "Not a model item: " + id);
            String reference = item.get("model").getAsString();
            require(reference.equals("tabletopfoods:item/" + parts[0] + "/" + parts[1]), "Invalid model reference: " + id);
            JsonObject model = object(resources.resolve("assets/tabletopfoods/models/item/" + parts[0] + "/" + parts[1] + ".json"));
            require(!model.get("parent").getAsString().contains("generated"), "Flat generated model: " + id);
            JsonObject textures = model.getAsJsonObject("textures");
            for (JsonElement texture : textures.asMap().values())
                require(texture.getAsString().startsWith("minecraft:item/"), "26.2 food model must use the item atlas consistently: " + id);
            JsonArray geometry = model.getAsJsonArray("elements");
            require(geometry.size() >= 3, "Food needs a sculpted model: " + id);
            for (JsonElement box : geometry) {
                JsonObject shape = box.getAsJsonObject();
                for (int axis = 0; axis < 3; axis++) {
                    double from = shape.getAsJsonArray("from").get(axis).getAsDouble();
                    double to = shape.getAsJsonArray("to").get(axis).getAsDouble();
                    require(from >= 0 && to <= 16 && to > from, "Invalid solid geometry: " + id);
                }
                require(shape.getAsJsonObject("faces").size() == 6, "Open food geometry: " + id);
                for (JsonElement face : shape.getAsJsonObject("faces").asMap().values()) {
                    JsonObject definition = face.getAsJsonObject();
                    String texture = definition.get("texture").getAsString();
                    require(texture.startsWith("#") && textures.has(texture.substring(1)), "Unresolved face texture: " + id);
                    JsonArray uv = definition.getAsJsonArray("uv");
                    for (JsonElement value : uv) require(value.getAsDouble() >= 0 && value.getAsDouble() <= 16, "UV outside texture: " + id);
                    require(uv.get(2).getAsDouble() > uv.get(0).getAsDouble() && uv.get(3).getAsDouble() > uv.get(1).getAsDouble(), "Empty palette UV");
                }
                elements++;
            }
        }
        System.out.println("Model checks passed: " + vanilla + " vanilla foods + 3 companion foods; " + elements + " solid elements, all model and texture references valid.");
    }
}
