package com.perengano99.hytaleplugin.commands;

// ============================================================================
//  SyncAssetsCommand
//  --------------------------------------------------------------------------
//  DEVELOPMENT-ONLY COMMAND: DO NOT INCLUDE IN PRODUCTION BUILDS!
//  This command is intended solely for development workflows. It allows the
//  execution of Gradle asset synchronization tasks directly from the server.
//  This can pose a security risk and should be removed or disabled before
//  compiling or deploying to production environments.
//  --------------------------------------------------------------------------
//  Functionality:
//  - Allows developers to trigger Gradle tasks (syncSrcAssets or syncBuildAssets)
//    from within the running server, synchronizing assets between the source
//    project and the in-game resources.
//  - The command is asynchronous and streams Gradle output back to the user.
//  - Usage: /syncassets <side>   where <side> is either "src" or "build".
//  - "src": Syncs source resources to the in-game environment.
//  - "build": Syncs in-game resources back to the source project.
//  --------------------------------------------------------------------------
//  WARNING: This command executes external processes and exposes build tooling
//  to the server environment. It must NOT be present in production builds.
// ============================================================================

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.AbstractCommand;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.ParseResult;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.SingleArgumentType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.concurrent.CompletableFuture;

@Deprecated(forRemoval = true)
public class SyncAssetsCommand extends AbstractCommand {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	// Argument type for specifying which asset side to sync
	private static final SingleArgumentType<String> ASSET_TYPE_ARG = new SingleArgumentType<String>("Assets Side",
			"\"src\" -> syncs src resources to ingame.\n\"build\" -> syncs ingame resources to src project.") {
		@Nonnull
		public String parse(@Nonnull String input, ParseResult parseResult) {
			String inputLowerCase = input.toLowerCase();
			return switch(inputLowerCase) {
				case "src" -> "src";
				case "build" -> "build";
				default -> {
					parseResult.fail(Message.raw("Invalid side type: '" + inputLowerCase + "'. Use 'src' or 'build'."));
					yield "";
				}
			};
		}
	};
	
	// Required argument for the command
	RequiredArg<String> assetTypeArg = withRequiredArg("side", "The side to sync assets from.", ASSET_TYPE_ARG);
	
	public SyncAssetsCommand() {
		super("syncassets", "Synchronize project assets between source and build resources. DEVELOPMENT USE ONLY.");
	}
	
	/**
	 * Executes the syncassets command, running the appropriate Gradle task asynchronously.
	 * Streams Gradle output to the command sender in real time.
	 *
	 * @param context The command context
	 * @return CompletableFuture<Void> for async execution
	 */
	@Nullable
	@Override
	protected CompletableFuture<Void> execute(@Nonnull CommandContext context) {
		String sideArg = assetTypeArg.get(context);
		String task = sideArg.equals("src") ? "syncSrcAssets" : "syncBuildAssets";
		boolean isPlayer = context.isPlayer();
		
		send(isPlayer, context, "[SyncAssets] Starting asset synchronization using Gradle task: '" + task + "'...");
		send(isPlayer, context, "[SyncAssets] This command is for development purposes only. Do NOT use in production!");
		
		// Run the Gradle task asynchronously to avoid blocking the main server thread
		return CompletableFuture.runAsync(() -> {
			try {
				// The server runs in the 'run/' directory, so project root is '..'
				File projectRoot = new File("../");
				boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
				
				ProcessBuilder pb;
				if (isWindows) {
					// On Windows, use cmd /c to run the .bat script
					pb = new ProcessBuilder("cmd.exe", "/c", "gradlew.bat", task);
				} else {
					// On Linux/Mac, run the shell script directly
					pb = new ProcessBuilder("./gradlew", task);
				}
				
				pb.directory(projectRoot);
				pb.redirectErrorStream(true); // Merge error and standard output
				
				Process process = pb.start();
				
				// Stream Gradle output line by line to the command sender
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
					String line;
					while ((line = reader.readLine()) != null)
						send(isPlayer, context, "[Gradle Output] " + line);
				}
				
				int exitCode = process.waitFor();
				
				if (exitCode == 0)
					send(isPlayer, context, "[SyncAssets] Asset synchronization completed successfully.");
				else
					send(isPlayer, context, "[SyncAssets] Asset synchronization failed. Gradle exit code: " + exitCode);
			} catch (Exception e) {
				send(isPlayer, context, "[SyncAssets] Exception while executing Gradle: " + e.getMessage());
				e.printStackTrace();
			}
		});
	}
	
	private void send(boolean isPlayer, CommandContext context, String message) {
		if (isPlayer)
			LOGGER.atWarning().log(message);
		context.sendMessage(Message.raw("[SyncAssets] " + message));
	}
}