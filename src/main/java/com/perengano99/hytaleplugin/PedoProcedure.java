package com.perengano99.hytaleplugin;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.spawning.util.LightRangePredicate;
import com.hypixel.hytale.server.worldgen.BiomeDataSystem;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;

import javax.annotation.Nonnull;

public class PedoProcedure extends TickProcedure {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	public static final BuilderCodec<PedoProcedure> CODEC = BuilderCodec.builder(PedoProcedure.class, PedoProcedure::new, TickProcedure.BASE_CODEC).
			//addField().
					build();
	
	@Override
	public BlockTickStrategy onTick(@Nonnull World world, WorldChunk wc, int x, int y, int z, int blockId) {
		
		IWorldGen worldGen = world.getChunkStore().getGenerator();
		if (worldGen instanceof ChunkGenerator generator) {
			
			int seed = (int) world.getWorldConfig().getSeed();
			ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
			
			Biome biome = result.getBiome();
			LOGGER.atInfo().log("El bioma es: \"" + biome.getName() + "\" | Zona: \"" + result.getZoneResult().getZone().name() + "\"");
			
			Store<EntityStore> store = world.getEntityStore().getStore();
			WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
			
			byte light = LightRangePredicate.calculateLightValue(wc.getBlockChunk(), x, y, z, worldTimeResource.getSunlightFactor());
			byte skyLight = (byte) (wc.getBlockChunk().getSkyLight(x, y, z) * worldTimeResource.getSunlightFactor());
			LOGGER.atInfo().log(
					"El porcentaje de luz es: " + LightRangePredicate.lightToPrecentage(light) + ", la luz del cielo es: " + LightRangePredicate.lightToPrecentage(skyLight));
		}
		
		return BlockTickStrategy.CONTINUE;
	}
}
