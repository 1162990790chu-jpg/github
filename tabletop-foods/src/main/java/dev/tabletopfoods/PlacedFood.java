package dev.tabletopfoods;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** A single real item, kept stationary on a supporting block until collected. */
public final class PlacedFood extends Entity {
    private static final EntityDataAccessor<ItemStack> FOOD = SynchedEntityData.defineId(
        PlacedFood.class, EntityDataSerializers.ITEM_STACK);
    private BlockPos support = BlockPos.ZERO;

    public PlacedFood(EntityType<? extends PlacedFood> type, Level level) {
        super(type, level);
        setNoGravity(true);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(FOOD, ItemStack.EMPTY);
    }

    public ItemStack getFood() { return entityData.get(FOOD); }
    public void setFood(ItemStack food) { entityData.set(FOOD, food.copyWithCount(1)); }
    public void setSupport(BlockPos pos) { support = pos.immutable(); }
    public BlockPos getSupport() { return support; }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        setFood(input.read("Food", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        support = input.read("Support", BlockPos.CODEC).orElseGet(() -> BlockPos.containing(getX(), getY() - 0.01, getZ()));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (!getFood().isEmpty()) output.store("Food", ItemStack.CODEC, getFood());
        output.store("Support", BlockPos.CODEC, support);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(level() instanceof ServerLevel server)) return;
        if (getFood().isEmpty()) { discard(); return; }
        if (tickCount % 10 == 0) {
            double top = FoodPlacement.surfaceHeight(server, support);
            if (!Double.isFinite(top) || Math.abs(top + 0.003 - getY()) > 0.025) dropFood(server);
        }
    }

    private boolean canCollect(Player player) {
        return !player.isSpectator() && player.mayBuild() && level().mayInteract(player, support);
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
        if (!player.getItemInHand(hand).isEmpty()) return InteractionResult.PASS;
        if (!canCollect(player)) return InteractionResult.FAIL;
        if (level() instanceof ServerLevel server && !isRemoved()) {
            ItemStack item = takeFood();
            if (!item.isEmpty()) {
                player.getInventory().add(item);
                if (!item.isEmpty()) player.drop(item, false);
                server.playSound(null, blockPosition(), SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.6F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /** Clear and discard before returning the stack, so repeated interactions cannot duplicate it. */
    private ItemStack takeFood() {
        if (isRemoved()) return ItemStack.EMPTY;
        ItemStack result = getFood().copy();
        setFood(ItemStack.EMPTY);
        discard();
        return result;
    }

    private void dropFood(ServerLevel server) {
        ItemStack item = takeFood();
        if (!item.isEmpty()) spawnAtLocation(server, item);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (isRemoved() || isInvulnerableToBase(source) || amount <= 0) return false;
        if (source.getEntity() instanceof Player player && !canCollect(player)) return false;
        dropFood(level);
        return true;
    }

    @Override public boolean isPickable() { return !isRemoved(); }
    @Override public boolean isAttackable() { return true; }
    @Override public boolean isPushable() { return false; }
    @Override public ItemStack getPickResult() { return getFood().copy(); }
}
