package com.perengano99.hytaleplugin;

import com.hypixel.hytale.assetstore.AssetMap;
import com.hypixel.hytale.assetstore.map.BlockTypeAssetMap;
import com.hypixel.hytale.builtin.beds.sleep.systems.world.UpdateWorldSlumberSystem;
import com.hypixel.hytale.builtin.blocktick.BlockTickPlugin;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;


@SuppressWarnings("unused")
public class HytaleDevPlugin extends JavaPlugin {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	public HytaleDevPlugin(JavaPluginInit init) {
		super(init);
	}
	
	@Override
	protected void setup() {
		LOGGER.atInfo().log("Hello, Hytale! The development environment is up and running.");
		
		int dirtId = BlockType.getAssetMap().getIndex("Soil_Dirt");
//		BlockType.getAssetMap().getAsset(dirtId).getTickProcedure().
		this.getEntityStoreRegistry().registerSystem(new GlobalUpdateSystem());
		
	}
}