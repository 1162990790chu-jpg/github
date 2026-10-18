package net.mcreator.speedapple.mixin;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.Mixin;

import net.minecraft.commands.Commands;
import net.minecraft.commands.CommandSourceStack;

import net.mcreator.speedapple.event.MiscEvents;

import com.mojang.brigadier.ParseResults;

@Mixin(Commands.class)
public abstract class CommandsMixin {
	@Inject(method = "performCommand(Lcom/mojang/brigadier/ParseResults;Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
	public void performCommand(ParseResults<CommandSourceStack> parseResults, String string, CallbackInfo ci) {
		boolean result = MiscEvents.COMMAND_EXECUTE.invoker().onCommandExecuted(parseResults);
		if (!result)
			ci.cancel();
	}
}