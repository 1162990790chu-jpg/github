package net.mcreator.speedapple;

import org.jetbrains.annotations.Nullable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.TickTask;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;

import net.mcreator.speedapple.init.SpeedappleModTabs;
import net.mcreator.speedapple.init.SpeedappleModItems;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.api.EnvType;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.Queue;
import java.util.PriorityQueue;
import java.util.Comparator;

import java.lang.invoke.MethodType;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandle;

import it.unimi.dsi.fastutil.ints.IntObjectPair;
import it.unimi.dsi.fastutil.ints.IntObjectImmutablePair;

public class SpeedappleMod implements ModInitializer {
	public static final Logger LOGGER = LogManager.getLogger(SpeedappleMod.class);
	public static final String MODID = "speedapple";
	// Persistent custom NBT backing store for Fabric procedure blocks on entities and block entities.
	public static final AttachmentType<CompoundTag> CUSTOM_NBT = AttachmentRegistry.createPersistent(Identifier.parse(MODID + ":mcreator_custom_nbt"), CompoundTag.CODEC);

	@Override
	public void onInitialize() {
		// Start of user code block mod constructor
		// End of user code block mod constructor
		LOGGER.info("Initializing SpeedappleMod");
		SpeedappleModTabs.load();
		SpeedappleModItems.load();
		tick();
		// Start of user code block mod init
		// End of user code block mod init
	}

	// Start of user code block mod methods
	// End of user code block mod methods
	private static final Queue<IntObjectPair<Runnable>> workToBeScheduled = new ConcurrentLinkedQueue<>();
	private static final PriorityQueue<TickTask> workQueue = new PriorityQueue<>(Comparator.comparingInt(TickTask::getTick));

	public static void queueServerWork(int delay, Runnable action) {
		workToBeScheduled.add(new IntObjectImmutablePair<>(delay, action));
	}

	private void tick() {
		ServerTickEvents.END_SERVER_TICK.register((server) -> {
			int currentTick = server.getTickCount();
			IntObjectPair<Runnable> work;
			while ((work = workToBeScheduled.poll()) != null) {
				workQueue.add(new TickTask(currentTick + work.leftInt(), work.right()));
			}
			while (!workQueue.isEmpty() && currentTick >= workQueue.peek().getTick()) {
				workQueue.poll().run();
			}
		});
	}

	private static Object minecraft;
	private static MethodHandle playerHandle;

	@Nullable
	public static Player clientPlayer() {
		if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			try {
				if (minecraft == null || playerHandle == null) {
					Class<?> minecraftClass = Class.forName("net.minecraft.client.Minecraft");
					minecraft = MethodHandles.publicLookup().findStatic(minecraftClass, "getInstance", MethodType.methodType(minecraftClass)).invoke();
					playerHandle = MethodHandles.publicLookup().findGetter(minecraftClass, "player", Class.forName("net.minecraft.client.player.LocalPlayer"));
				}
				return (Player) playerHandle.invoke(minecraft);
			} catch (Throwable e) {
				LOGGER.error("Failed to get client player", e);
				return null;
			}
		} else {
			return null;
		}
	}
}