package dev.tabletopfoods;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Runs at the item-use stage, after normal chest/workbench interactions. */
public final class FoodPlacement {
    private FoodPlacement() {}

    public static boolean canPlace(ItemStack stack) {
        // Carrots, potatoes and berries are also planting BlockItems; they are still placeable food.
        return !stack.isEmpty() && stack.has(DataComponents.FOOD)
            && stack.has(DataComponents.CONSUMABLE) && !stack.is(Items.CAKE);
    }

    /** ItemEvents.USE_ON uses null to continue vanilla; PASS would also replace vanilla useOn. */
    public static InteractionResult useOn(UseOnContext context) {
        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !canPlace(stack) || context.getClickedFace() != Direction.UP)
            return null;
        Level level = context.getLevel();
        BlockPos support = context.getClickedPos();
        // Sneaking keeps native planting where that plant can actually survive.
        // This does not intercept sneaking placement on chests or other unsuitable surfaces.
        if (context.isSecondaryUseActive() && stack.getItem() instanceof BlockItem plant
            && plant.getBlock().defaultBlockState().canSurvive(level, support.above()))
            return null;
        if (player.isSpectator() || !player.mayBuild()
            || !player.mayUseItemAt(support.above(), Direction.UP, stack)
            || !level.mayInteract(player, support)) return InteractionResult.FAIL;
        double height = surfaceHeight(level, support);
        if (!Double.isFinite(height) || Math.abs(context.getClickLocation().y - height) > 0.04)
            return null;

        PlacedFood food = new PlacedFood(TabletopFoods.PLACED_FOOD, level);
        food.setPos(support.getX() + 0.5, height + 0.003, support.getZ() + 0.5);
        food.setYRot(Math.round(player.getYRot() / 90F) * 90F);
        food.setOldPosAndRot();
        food.setSupport(support);
        food.setFood(stack.copyWithCount(1));
        AABB bounds = food.getBoundingBox().deflate(0.006);
        if (level.getBlockCollisions(food, bounds).iterator().hasNext()
            || !level.getEntitiesOfClass(PlacedFood.class, bounds).isEmpty())
            return InteractionResult.FAIL;
        // The client predicts only the action; the server owns entities and inventory.
        if (!level.isClientSide()) {
            if (!level.addFreshEntity(food)) return InteractionResult.FAIL;
            if (!player.getAbilities().instabuild) stack.shrink(1);
            level.playSound(null, support, SoundEvents.ITEM_FRAME_PLACE, SoundSource.BLOCKS, 0.7F, 1.2F);
            level.gameEvent(player, GameEvent.ENTITY_PLACE, food.position());
        }
        return InteractionResult.SUCCESS;
    }

    public static double surfaceHeight(Level level, BlockPos support) {
        VoxelShape shape = level.getBlockState(support).getCollisionShape(level, support);
        if (shape.isEmpty()) return Double.NaN;
        double localTop = shape.max(Direction.Axis.Y, 0.5, 0.5);
        return Double.isFinite(localTop) ? support.getY() + localTop : Double.NaN;
    }
}
