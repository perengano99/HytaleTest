package com.perengano99.hytaleplugin;


import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

public class HytaleDevPlugin extends JavaPlugin {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	public HytaleDevPlugin(JavaPluginInit init) {
		super(init);
	}
	
	@Override
	protected void setup() {
		LOGGER.atInfo().log("¡Qué onda, Orbis! El entorno de desarrollo E2E está vivo. [Ruka No Logro Esto]");
	}
}