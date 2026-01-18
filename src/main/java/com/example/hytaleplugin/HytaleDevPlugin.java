package com.example.hytaleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

/**
 * Main entry point for the Hytale plugin.
 * <p>
 * Hytale modding is based on a server-authoritative architecture where plugins run on the server
 * and can modify game logic, entities, and world generation. The client receives necessary assets
 * and data automatically.
 * <p>
 * This class extends {@link JavaPlugin}, which provides the lifecycle methods for the plugin.
 * <p>
 * <b>Resource Example:</b>
 * <br>
 * The project includes an example block definition at:
 * {@code src/main/resources/Server/Item/items/Example_Block.json}
 * <br>
 * This JSON file defines the properties of a custom block. To fully implement a custom block,
 * you would typically also need:
 * <ul>
 *     <li><b>Texture:</b> A PNG file in {@code src/main/resources/Client/textures/block/}</li>
 *     <li><b>Icon:</b> A PNG file in {@code src/main/resources/Client/textures/gui/icons/} (optional, often auto-generated)</li>
 *     <li><b>Model:</b> A JSON model file if it's not a standard cube.</li>
 * </ul>
 * Ensure your resource directory structure matches the game's expected format (e.g., separating Server and Client resources).
 */
@SuppressWarnings("unused")
public class HytaleDevPlugin extends JavaPlugin {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	public HytaleDevPlugin(JavaPluginInit init) {
		super(init);
	}
	
	@Override
	protected void setup() {
		LOGGER.atInfo().log("Hello, Hytale! The development environment is up and running.");
	}
}