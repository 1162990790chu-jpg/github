package dev.tabletopfoods;

import com.google.gson.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Development-only test mod: excluded from both the distributable and its sources JAR. */
public final class DevelopmentChecks implements ModInitializer {
    private static final List<String> passed = new ArrayList<>();
    private static final BlockPos SUPPORT = new BlockPos(22, -61, 22);
    private static final AABB AREA = new AABB(15,-63,15,30,-55,30);
    @Override public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, access, environment) ->
            dispatcher.register(Commands.literal("tabletoptest")
                .then(Commands.literal("run").executes(context -> execute(context.getSource().getPlayerOrException(), "run")))
                .then(Commands.literal("gallery").executes(context -> execute(context.getSource().getPlayerOrException(), "gallery")))
                .then(Commands.literal("preview").executes(context -> execute(context.getSource().getPlayerOrException(), "preview")))
                .then(Commands.literal("status").executes(context -> execute(context.getSource().getPlayerOrException(), "status")))));
    }
    private static int execute(ServerPlayer player, String action) {
        try {
            return switch(action) {
                case "run" -> run(player);
                case "gallery" -> gallery(player, false);
                case "preview" -> gallery(player, true);
                default -> status(player);
            };
        } catch (Exception | AssertionError error) {
            TabletopFoods.LOGGER.error("DEVELOPMENT_CHECK_FAILED: {}", error.getMessage(), error);
            player.sendSystemMessage(Component.literal("测试未通过："+error.getMessage()));
            return 0;
        }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        passed.add(label); TabletopFoods.LOGGER.info("CHECK_PASS: {}", label);
    }
    private static List<String> catalog() throws Exception {
        try (var input = DevelopmentChecks.class.getResourceAsStream("/tabletopfoods-food-models.json")) {
            JsonArray entries = JsonParser.parseString(new String(Objects.requireNonNull(input).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonArray();
            List<String> result = new ArrayList<>();
            entries.forEach(e -> result.add(e.getAsJsonObject().get("id").getAsString())); return result;
        }
    }
    private static BlockHitResult hit(BlockPos support, double height) {
        return new BlockHitResult(new Vec3(support.getX()+0.5,height,support.getZ()+0.5),Direction.UP,support,false);
    }
    private static List<PlacedFood> foods(ServerLevel level) { return level.getEntitiesOfClass(PlacedFood.class,AREA); }
    private static int inventory(ServerPlayer player, ItemStack match) {
        int count=0; for(int i=0;i<player.getInventory().getContainerSize();i++) {
            ItemStack stack=player.getInventory().getItem(i); if(ItemStack.isSameItemSameComponents(stack,match))count+=stack.getCount();
        } return count;
    }
    private static int dropped(ServerLevel level, AABB area, net.minecraft.world.item.Item item) {
        return level.getEntitiesOfClass(ItemEntity.class,area).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
    }
    private static InteractionResult place(ServerPlayer player, BlockPos support) {
        return player.getMainHandItem().useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,hit(support,FoodPlacement.surfaceHeight(player.level(),support))));
    }
    private static void clear(ServerLevel level) {
        level.getEntitiesOfClass(PlacedFood.class,AREA).forEach(e->e.discard());
        level.getEntitiesOfClass(ItemEntity.class,AREA).forEach(e->e.discard());
        level.setBlock(SUPPORT.above(),Blocks.AIR.defaultBlockState(),3);
        level.setBlock(SUPPORT,Blocks.STONE.defaultBlockState(),3);
    }
    private static int run(ServerPlayer player) throws Exception {
        ServerLevel level=(ServerLevel)player.level(); GameType previous=player.gameMode.getGameModeForPlayer();
        boolean previousShift=player.isShiftKeyDown();
        List<ItemStack> saved=new ArrayList<>(); for(int i=0;i<player.getInventory().getContainerSize();i++) saved.add(player.getInventory().getItem(i).copy());
        passed.clear();
        try {
            player.getInventory().clearContent(); player.setGameMode(GameType.SURVIVAL); clear(level);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.STONE,2)); place(player,SUPPORT);
            check(level.getBlockState(SUPPORT.above()).is(Blocks.STONE)&&player.getMainHandItem().getCount()==1,"ordinary block placement is not intercepted");
            clear(level); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CAKE)); place(player,SUPPORT);
            check(level.getBlockState(SUPPORT.above()).is(Blocks.CAKE)&&foods(level).isEmpty(),"cake keeps native block placement");
            clear(level);
            Set<String> ids=new HashSet<>(catalog()); int count=0;
            for(var item:BuiltInRegistries.ITEM) {
                var id=BuiltInRegistries.ITEM.getKey(item);
                if(id.getNamespace().equals("minecraft")&&item.getDefaultInstance().has(DataComponents.FOOD)
                    && item.getDefaultInstance().has(DataComponents.CONSUMABLE)) {
                    check(ids.contains(id.toString()),"registry model coverage "+id); count++;
                }
            }
            check(count==40,"all 40 vanilla edible items found at runtime");
            check(!FoodPlacement.canPlace(Items.PUFFERFISH_BUCKET.getDefaultInstance()),"fish buckets are not player-consumable food");
            for(var item:List.of(Items.CARROT,Items.POTATO,Items.SWEET_BERRIES,Items.GLOW_BERRIES))check(FoodPlacement.canPlace(item.getDefaultInstance()),"plantable food supports placement "+item);
            ItemStack named=new ItemStack(Items.APPLE,3); named.set(DataComponents.CUSTOM_NAME,Component.literal("保存组件测试苹果")); named.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
            ItemStack expected=named.copyWithCount(1); player.setItemInHand(InteractionHand.MAIN_HAND,named);
            place(player,SUPPORT); check(named.getCount()==2&&foods(level).size()==1,"survival places one and consumes exactly one");
            PlacedFood first=foods(level).getFirst(); check(ItemStack.isSameItemSameComponents(first.getFood(),expected),"placement preserves complete item components");
            place(player,SUPPORT); check(named.getCount()==2&&foods(level).size()==1,"overlap rejected without consumption");
            var output=TagValueOutput.createWithContext(ProblemReporter.DISCARDING,level.registryAccess()); first.addAdditionalSaveData(output);
            PlacedFood restored=new PlacedFood(TabletopFoods.PLACED_FOOD,level);
            restored.readAdditionalSaveData(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),output.buildResult()));
            check(ItemStack.isSameItemSameComponents(restored.getFood(),expected)&&restored.getSupport().equals(SUPPORT),"entity save codec round trip preserves food components and support");
            player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY); int before=inventory(player,expected);
            first.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO); first.interact(player,InteractionHand.MAIN_HAND,Vec3.ZERO);
            check(first.isRemoved()&&inventory(player,expected)==before+1,"empty-hand collection succeeds once without duplication");
            clear(level); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD)); place(player,SUPPORT);
            var unsupported=foods(level).getFirst(); level.setBlock(SUPPORT,Blocks.AIR.defaultBlockState(),3);
            for(int i=0;i<20;i++)unsupported.tick();
            check(unsupported.isRemoved()&&dropped(level,AREA,Items.BREAD)==1,"removing support drops exactly one item");
            clear(level); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.APPLE)); place(player,SUPPORT);
            var attacked=foods(level).getFirst(); attacked.hurtServer(level,player.damageSources().playerAttack(player),1); attacked.hurtServer(level,player.damageSources().playerAttack(player),1);
            check(attacked.isRemoved()&&dropped(level,AREA,Items.APPLE)==1,"repeated attacks cannot duplicate food");
            clear(level); player.setGameMode(GameType.CREATIVE); ItemStack creative=new ItemStack(Items.CARROT,3); player.setItemInHand(InteractionHand.MAIN_HAND,creative); place(player,SUPPORT);
            check(creative.getCount()==3&&foods(level).size()==1,"creative placement does not consume food");
            clear(level); player.setGameMode(GameType.SPECTATOR); place(player,SUPPORT); check(foods(level).isEmpty(),"spectator cannot place food");
            player.setGameMode(GameType.SURVIVAL); level.setBlock(SUPPORT,Blocks.SMOOTH_STONE_SLAB.defaultBlockState().setValue(SlabBlock.TYPE,SlabType.BOTTOM),3); place(player,SUPPORT);
            check(foods(level).size()==1&&Math.abs(foods(level).getFirst().getY()-(SUPPORT.getY()+0.503))<0.001,"lower slab placement uses actual surface height");
            clear(level); level.setBlock(SUPPORT,Blocks.CHEST.defaultBlockState(),3); player.setShiftKeyDown(false);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.BREAD,2));
            var chestHit=hit(SUPPORT,FoodPlacement.surfaceHeight(level,SUPPORT));
            player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,chestHit);
            check(foods(level).isEmpty()&&player.getMainHandItem().getCount()==2&&player.containerMenu!=player.inventoryMenu,"normal chest interaction takes priority over food placement");
            player.closeContainer(); player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player,level,player.getMainHandItem(),InteractionHand.MAIN_HAND,chestHit);
            check(foods(level).size()==1&&player.getMainHandItem().getCount()==1,"sneaking places one food on a chest"); player.setShiftKeyDown(false);
            clear(level); level.setBlock(SUPPORT,Blocks.FARMLAND.defaultBlockState(),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT,2)); place(player,SUPPORT);
            check(foods(level).size()==1&&player.getMainHandItem().getCount()==1,"normal farmland click places a carrot as food");
            clear(level); level.setBlock(SUPPORT,Blocks.FARMLAND.defaultBlockState(),3); player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.CARROT,2)); place(player,SUPPORT);
            check(foods(level).isEmpty()&&level.getBlockState(SUPPORT.above()).is(Blocks.CARROTS)&&player.getMainHandItem().getCount()==1,"sneaking plants carrots normally");
            clear(level); level.setBlock(SUPPORT,Blocks.DIRT.defaultBlockState(),3);
            player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SWEET_BERRIES,2)); place(player,SUPPORT);
            check(foods(level).isEmpty()&&level.getBlockState(SUPPORT.above()).is(Blocks.SWEET_BERRY_BUSH)&&player.getMainHandItem().getCount()==1,"sneaking plants berry bushes normally"); player.setShiftKeyDown(false);
            clear(level); player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.COOKIE)); place(player,SUPPORT); var full=foods(level).getFirst();
            for(int i=0;i<36;i++)player.getInventory().setItem(i,new ItemStack(Items.STONE,64)); player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);
            AABB nearPlayer=player.getBoundingBox().inflate(4); int droppedBefore=dropped(level,nearPlayer,Items.COOKIE);
            full.interact(player,InteractionHand.OFF_HAND,Vec3.ZERO); check(full.isRemoved()&&dropped(level,nearPlayer,Items.COOKIE)==droppedBefore+1,"full inventory returns food as a nearby drop");
            player.getInventory().clearContent(); ItemStack honey=Items.HONEY_BOTTLE.getDefaultInstance(); ItemStack remainder=honey.finishUsingItem(level,player);
            check(remainder.is(Items.GLASS_BOTTLE),"normal honey consumption still returns glass bottle");
            ItemStack stew=Items.MUSHROOM_STEW.getDefaultInstance(); check(stew.finishUsingItem(level,player).is(Items.BOWL),"normal stew consumption still returns bowl");
            Files.writeString(FabricLoader.getInstance().getGameDir().resolve("tabletopfoods-test-results.json"),new GsonBuilder().setPrettyPrinting().create().toJson(Map.of("passed",passed,"total",passed.size(),"success",true)));
            TabletopFoods.LOGGER.info("ALL_RUNTIME_CHECKS_PASSED: {}",passed.size());
            player.sendSystemMessage(Component.literal("立体食物测试全部通过："+passed.size()+" 项")); return passed.size();
        } finally {
            player.closeContainer(); player.setShiftKeyDown(previousShift);
            clear(level); player.getInventory().clearContent(); for(int i=0;i<saved.size();i++)player.getInventory().setItem(i,saved.get(i)); player.setGameMode(previous);
        }
    }
    private static int gallery(ServerPlayer player, boolean preview) throws Exception {
        ServerLevel level=(ServerLevel)player.level();
        for(var e:level.getEntitiesOfClass(PlacedFood.class,new AABB(-2,-63,-2,17,-55,20)))if(e.entityTags().contains("tabletopfoods_gallery"))e.discard();
        List<String> list=preview?List.of("minecraft:apple","minecraft:bread","minecraft:carrot","minecraft:cooked_beef","minecraft:mushroom_stew","minecraft:salmon","minecraft:cookie","minecraft:honey_bottle","speedapple:jump_gummy"):catalog();
        int index=0; List<Object> records=new ArrayList<>(); int columns=preview?3:7;
        for(String id:list){int column=index%columns,row=index/columns; BlockPos support=new BlockPos(column*2,-60,row*2); level.setBlock(support,Blocks.QUARTZ_BLOCK.defaultBlockState(),3);
            ItemStack item=BuiltInRegistries.ITEM.getValue(Identifier.parse(id)).getDefaultInstance(); if(item.isEmpty())continue;
            PlacedFood food=new PlacedFood(TabletopFoods.PLACED_FOOD,level);food.setFood(item);food.setSupport(support);food.setPos(support.getX()+0.5,-58.997,support.getZ()+0.5);food.setYRot(0);food.setOldPosAndRot();food.addTag("tabletopfoods_gallery");level.addFreshEntity(food);
            records.add(Map.of("id",id,"uuid",food.getUUID().toString())); index++;
        }
        Files.writeString(FabricLoader.getInstance().getGameDir().resolve("tabletopfoods-gallery.json"),new Gson().toJson(records));
        TabletopFoods.LOGGER.info("GALLERY_READY: {} foods, {}",index,preview?"preview":"complete catalog");player.sendSystemMessage(Component.literal("模型展台已生成："+index)); return index;
    }
    private static int status(ServerPlayer player) throws Exception {
        ServerLevel level=(ServerLevel)player.level();JsonArray records=JsonParser.parseString(Files.readString(FabricLoader.getInstance().getGameDir().resolve("tabletopfoods-gallery.json"))).getAsJsonArray();int count=0;
        for(JsonElement entry:records){var obj=entry.getAsJsonObject();var entity=level.getEntity(UUID.fromString(obj.get("uuid").getAsString()));
            if(entity instanceof PlacedFood food && BuiltInRegistries.ITEM.getKey(food.getFood().getItem()).toString().equals(obj.get("id").getAsString()))count++;
        }
        check(count==records.size(),"saved gallery retains same entity UUIDs and food identities");TabletopFoods.LOGGER.info("PERSISTENCE_CHECK_PASSED: {}",count);player.sendSystemMessage(Component.literal("保存与重进验证通过："+count));return count;
    }
}
