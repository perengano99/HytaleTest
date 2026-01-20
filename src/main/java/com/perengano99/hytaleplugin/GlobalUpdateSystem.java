package com.perengano99.hytaleplugin;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.blackboard.view.combat.CombatViewSystems.Ticking;
import com.hypixel.hytale.server.spawning.local.LocalSpawnControllerSystem;
import it.unimi.dsi.fastutil.longs.LongSet;

public class GlobalUpdateSystem extends TickingSystem<EntityStore> {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	@Override
	public void tick(float dt, int index, Store<EntityStore> store) {
		int tickingChunks = 0;
		World world = store.getExternalData().getWorld();
		
		
		// Muy experimental y genuinamente no me gusta como estoy consiguiendo este ticking global de chunks,
		// especialmente pq se supone que los bloques ya estan haciendo ticking.
		
		LongSet loadedChunks = world.getChunkStore().getChunkIndexes();
		for (long chunkIndex : loadedChunks) {
			WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
			if (chunk != null)
				tickingChunks++;
		}
		
		LOGGER.atInfo().log("Hay '" + tickingChunks + "' chunks cargados de '"+ loadedChunks.size() +"'");
		// int dirtId = BlockType.getAssetMap().getIndex("Soil_Dirt");
		// LOGGER.atInfo().log("Tecnicamente, cada tick se muestra el ID: [" + dirtId + "] que representa \"Soil_Dirt\"");
	}
}