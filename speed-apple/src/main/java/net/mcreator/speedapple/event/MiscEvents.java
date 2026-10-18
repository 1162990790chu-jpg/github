package net.mcreator.speedapple.event;

import net.minecraft.commands.CommandSourceStack;

import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.event.Event;

import com.mojang.brigadier.ParseResults;

public class MiscEvents {
	public static final Event<CommandExecute> COMMAND_EXECUTE = EventFactory.createArrayBacked(CommandExecute.class, (callbacks) -> (results) -> {
		for (CommandExecute event : callbacks) {
			boolean result = event.onCommandExecuted(results);
			if (!result) {
				return false;
			}
		}
		return true;
	});

	@FunctionalInterface
	public interface CommandExecute {
		boolean onCommandExecuted(ParseResults<CommandSourceStack> results);
	}
}