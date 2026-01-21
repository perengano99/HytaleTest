package com.perengano99.hytaleplugin.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.perengano99.hytaleplugin.HytaleDevPlugin;
import org.checkerframework.checker.nullness.compatqual.NonNullDecl;
import org.checkerframework.checker.nullness.compatqual.NullableDecl;

import java.util.concurrent.CompletableFuture;

public class ReloadCommand extends AbstractCommand {
	
	public ReloadCommand() {
		super("reloadTest", "Reload Hytale Dev Plugin");
	}
	
	@NullableDecl
	@Override
	protected CompletableFuture<Void> execute(@NonNullDecl CommandContext context) {
		HytaleDevPlugin.reloadConfigs();
		context.sendMessage(Message.raw("Plugin Reloaded!"));
		return CompletableFuture.completedFuture(null);
	}
}
