package com.perengano99.hytaleplugin.blocktick;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.DrawType;
import com.hypixel.hytale.server.core.asset.type.blocktick.BlockTickStrategy;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.spawning.util.LightRangePredicate;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.perengano99.hytaleplugin.HytaleDevPlugin;
import com.perengano99.hytaleplugin.config.NaturalGrassConfig;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Random;
import java.util.Set;

public class NaturalGrassTick extends TickProcedure {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	private static final Set<String> grassBlocks = Set.of("Soil_Grass", "Soil_Grass_Burnt", "Soil_Grass_Dry", "Soil_Grass_Cold", "Soil_Grass_Deep", "Soil_Grass_Wet",
			"Soil_Grass_Full", "Soil_Grass_Sunny");
	
	public static void registerTickHandlers() {
		try {
			Field tickProcedureField = BlockType.class.getDeclaredField("tickProcedure");
			tickProcedureField.setAccessible(true);
			
			for (String grassBlock : grassBlocks) {
				BlockType bt = BlockType.getAssetMap().getAsset(grassBlock);
				tickProcedureField.set(bt, new NaturalGrassTick());
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public BlockTickStrategy onTick(@Nonnull World world, WorldChunk wc, int x, int y, int z, int blockId) {
		var config = HytaleDevPlugin.getConfig().getGrassConfig();
		if (!config.isEnabled() || getRandom().nextFloat() > config.getProcessChance() / 100) return BlockTickStrategy.CONTINUE;
		
		String block = BlockType.getAssetMap().getAsset(blockId).getId();
		
		if (grassBlocks.contains(block)) {
			if (config.grassExposureDecay() && isCovered(config, wc, x, y, z)) {
				world.setBlock(x, y, z, getDirtFromGrass(block));
				return BlockTickStrategy.CONTINUE;
			}
			
			if (!block.equals("Soil_Grass_Dry")) {
				if (config.grassZone2Dry() && !block.equals("Soil_Grass_Wet")) {
					if (world.getChunkStore().getGenerator() instanceof ChunkGenerator generator) {
						int seed = (int) world.getWorldConfig().getSeed();
						ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
						if (result.getZoneResult().getZone().name().startsWith("Zone2_")) {
							world.setBlock(x, y, z, "Soil_Grass_Dry");
							return BlockTickStrategy.CONTINUE;
						}
					}
				}
				
				WorldTimeResource worldTimeResource = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
				if (getLight(wc, x, y, z, worldTimeResource) < config.getMinSkyPercent()) return BlockTickStrategy.CONTINUE;
				
				int dx = getRandom().nextInt(3) - 1;
				int dy = getRandom().nextInt(3) - 1;
				int dz = getRandom().nextInt(3) - 1;
				if (dx == 0 && dz == 0) return BlockTickStrategy.CONTINUE;
				
				int nx = x + dx, ny = y + dy, nz = z + dz;
				String neighbor = BlockType.getAssetMap().getAsset(world.getBlock(nx, ny, nz)).getId();
				if (canSpread(block, neighbor) && getLight(wc, nx, ny, nz, worldTimeResource) >= config.getMinSkyPercent() / 1.5f && !isCovered(config, wc, nx, ny, nz))
					world.setBlock(nx, ny, nz, block);
			}
		}
		return BlockTickStrategy.CONTINUE;
	}
	
	private boolean isCovered(NaturalGrassConfig cfg, WorldChunk wc, int x, int y, int z) {
		BlockType upType = wc.getBlockType(x, y + 1, z);
		if (upType.getId().equalsIgnoreCase("Empty")) return false;
		
		// Si encima hay cualquier fluido, esta cubierto.
		// getFluidId esta marcado para remover, pero no encontre otra forma de conseguir el fluido.
		if (!Fluid.getAssetMap().getAsset(wc.getFluidId(x, y + 1, z)).getId().equalsIgnoreCase("Empty")) return true;
		
		String id = upType.getId();
		DrawType drawType = upType.getDrawType();
		
		if (drawType == DrawType.Cube || drawType == DrawType.CubeWithModel) return !cfg.getBlacklist().contains(id);
		
		if (cfg.getRotativeModelsList().contains(id)) {
			// getRotationIndex esta marcado para remover, pero no encontre otra forma de conseguir la rotacion.
			int rotation = wc.getRotationIndex(x, y + 1, z);
			if (RotationTuple.get(rotation).pitch() == Rotation.None) return true;
		}
		return cfg.getModelsWitheList().contains(id);
	}
	
	private int getLight(WorldChunk wc, int x, int y, int z, WorldTimeResource worldTimeResource) {
		byte skyLight = (byte) (wc.getBlockChunk().getSkyLight(x, y, z) * worldTimeResource.getSunlightFactor());
		return LightRangePredicate.lightToPrecentage(skyLight);
	}
	
	private String getDirtFromGrass(String grass) {
		return switch(grass) {
			case "Soil_Grass_Burnt" -> "Soil_Dirt_Burnt";
			case "Soil_Grass_Cold" -> "Sol_Dirt_Cold";
			case "Soil_Grass_Dry" -> "Soil_Dirt_Dry";
			default -> "Soil_Dirt";
		};
	}
	
	private boolean canSpread(String grass, String block) {
		switch(grass) {
			case "Soil_Grass_Burnt":
				if (block.equals("Soil_Dirt_Burnt"))
					return true;
			case "Soil_Grass_Cold": {
				if (block.equals("Soil_Dirt_Cold"))
					return true;
			}
		}
		return block.equals("Soil_Dirt");
	}
}
