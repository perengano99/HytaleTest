package com.perengano99.hytaleplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.component.Store;
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
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.spawning.util.LightRangePredicate;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.perengano99.hytaleplugin.config.ExposureBlockerModelsConfig;
import com.perengano99.hytaleplugin.config.GrassGrowthConfig.GrowthConditions;
import com.perengano99.hytaleplugin.config.GrassGrowthConfig.GrowthRule;
import com.perengano99.hytaleplugin.config.GrassGrowthConfig.RuleType;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PedoProcedure extends TickProcedure {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final Random random = new Random();
	
	public static final BuilderCodec<PedoProcedure> CODEC = BuilderCodec.builder(PedoProcedure.class, PedoProcedure::new, TickProcedure.BASE_CODEC)
			.append(new KeyedCodec<>("Rules", new ArrayCodec<>(Codec.STRING, String[]::new)),
					(c, v, unused) -> {
						c.ruleKeys = List.of(v);
						c.loadRulesFromKeys();
					},
					(c, unused) -> c.ruleKeys.toArray(new String[0]))
			.add()
			.build();
	
	
	// Campos cache para no buscar en cada regla.
	private List<GrowthRule> rules = new ArrayList<>();
	private List<String> ruleKeys = new ArrayList<>();
	private int light = -1, sky = -1;
	private int skyExposure = -1;
	private int waterNearby = -1;
	private String biome = "-1";
	
	
	//	@Override
	public BlockTickStrategy onTick(@Nonnull World world, WorldChunk wc, int x, int y, int z, int blockId) {
		if (!HytaleDevPlugin.getGrassGrowthConfig().isEnabled()) return BlockTickStrategy.IGNORED;
		
		IWorldGen worldGen = world.getChunkStore().getGenerator();
		if (worldGen instanceof ChunkGenerator generator) {
			reset();
			
			Store<EntityStore> store = world.getEntityStore().getStore();
			WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
			
			byte light = LightRangePredicate.calculateLightValue(wc.getBlockChunk(), x, y, z, worldTimeResource.getSunlightFactor());
			byte skyLight = (byte) (wc.getBlockChunk().getSkyLight(x, y, z) * worldTimeResource.getSunlightFactor());
			
			for (GrowthRule rule : rules) {
				if (random.nextFloat() > rule.getChance()) continue;
				if (!checkRuleConditions(rule.getConditions(), world, wc, x, y, z, generator, worldTimeResource)) continue;
				
				if (execute(rule, world, wc, x, y, z, BlockType.getAssetMap().getAsset(blockId).getId()))
					return BlockTickStrategy.CONTINUE;
			}
		}
		return BlockTickStrategy.IGNORED;
	}
	
	private void reset() {
		light       = -1;
		sky         = -1;
		skyExposure = -1;
		waterNearby = -1;
		biome       = "-1";
	}
	
	private boolean execute(GrowthRule rule, World world, WorldChunk wc, int x, int y, int z, String block) {
		switch(rule.getType()) {
			case RuleType.CONVERSION:
				if (rule.getTargetBlocks().isEmpty()) {
					LOGGER.atWarning().log("No target blocks were provided for Conversion Rule");
					return false;
				}
				
				String targetBlock = rule.getTargetBlocks().get(random.nextInt(rule.getTargetBlocks().size()));
				int index = BlockType.getAssetMap().getIndex(targetBlock);
				if (index == Integer.MIN_VALUE) {
					LOGGER.atWarning().log("Unknown target block! " + targetBlock);
					return false;
				}
				world.setBlock(x, y, z, targetBlock);
				return true;
			case RuleType.SPREAD:
				if (rule.getTargetBlocks().isEmpty()) {
					LOGGER.atWarning().log("No target blocks were provided for Spread Rule");
					return false;
				}
				
				int dx = random.nextInt(3) - 1;
				int dy = random.nextInt(3) - 1;
				int dz = random.nextInt(3) - 1;
				// No permitir esparcir en (0,0,0), (0,1,0) ni (0,-1,0)
				if (dx == 0 && dz == 0) return false;
				
				int nx = x + dx, ny = y + dy, nz = z + dz;
				int neighborId = world.getBlock(nx, ny, nz);
				BlockType neighbor = BlockType.getAssetMap().getAsset(neighborId);
				if (rule.getTargetBlocks().contains(neighbor.getId())) {
					if (isCovered(wc, nx, ny, nz)) return false;
					world.setBlock(nx, ny, nz, block);
					return true;
				}
				break;
		}
		return false;
	}
	
	private boolean checkRuleConditions(GrowthConditions conditions, World world, WorldChunk wc, int x, int y, int z, ChunkGenerator generator,
	                                    WorldTimeResource worldTimeResource) {
		boolean pass = true;
		
		// Check Light
		if (conditions.getMinLight() > 0) {
			if (light == -1) getLight(wc, x, y, z, worldTimeResource);
			
			pass = switch(conditions.getLightSource()) {
				case GENERAL -> light >= conditions.getMinLight();
				case SKY -> sky >= conditions.getMinLight();
			};
		}
		
		// Check Sky Exposure
		if (pass && conditions.isNeedsSkyExposure()) {
			if (skyExposure == -1) skyExposure = isCovered(wc, x, y, z) ? 0 : 1;
			
			pass = skyExposure == 1;
		}
		
		// Check Water Nearby
		if (pass && conditions.isNeedsNearbyWater()) {
			int radius = HytaleDevPlugin.getGrassGrowthConfig().getWaterNearbyRadius();
			if (waterNearby == -1) waterNearby = isWaterNearby(wc, x, y, z, radius) ? 1 : 0;
			
			pass = waterNearby == 1;
		}
		
		// Check Biomes
		if (pass) {
			if (biome.equalsIgnoreCase("-1")) biome = getBiome(world, generator, x, y, z);
			
			pass = conditions.isExcludeBiomes() != conditions.getBiomes().contains(biome);
		}
		
		return pass;
	}
	
	private void getLight(WorldChunk wc, int x, int y, int z, WorldTimeResource worldTimeResource) {
		byte light = LightRangePredicate.calculateLightValue(wc.getBlockChunk(), x, y, z, worldTimeResource.getSunlightFactor());
		byte skyLight = (byte) (wc.getBlockChunk().getSkyLight(x, y, z) * worldTimeResource.getSunlightFactor());
		
		this.light = LightRangePredicate.lightToPrecentage(light);
		this.sky   = LightRangePredicate.lightToPrecentage(skyLight);
	}
	
	private boolean isCovered(WorldChunk wc, int x, int y, int z) {
		BlockType upType = wc.getBlockType(x, y + 1, z);
		
		if (upType.getId().equalsIgnoreCase("Empty")) return false;
		
		// Si encima hay cualquier fluido, esta cubierto.
		// getFluidId esta marcado para remover, pero no encontre otra forma de conseguir el fluido.
		if (!Fluid.getAssetMap().getAsset(wc.getFluidId(x, y + 1, z)).getId().equalsIgnoreCase("Empty")) return true;
		
		String id = upType.getId();
		DrawType drawType = upType.getDrawType();
		ExposureBlockerModelsConfig cfg = HytaleDevPlugin.getExposureBlockerModelsConfig();
		
		if (drawType == DrawType.Cube || drawType == DrawType.CubeWithModel) return !cfg.getBlacklist().contains(id);
		
		if (cfg.getRotativeModelsList().contains(id)) {
			// getRotationIndex esta marcado para remover, pero no encontre otra forma de conseguir la rotacion.
			int rotation = wc.getRotationIndex(x, y + 1, z);
			if (RotationTuple.get(rotation).pitch() == Rotation.None) return true;
		}
		return cfg.getModelsWitheList().contains(id);
	}
	
	private boolean isWaterNearby(WorldChunk wc, int x, int y, int z, int radius) {
		for (int i = x - radius; i <= x + radius; i++) {
			for (int j = y - radius; j <= y + radius; j++) {
				for (int k = z - radius; k <= z + radius; k++) {
					double dist = Math.sqrt(Math.pow(i - x, 2) + Math.pow(j - y, 2) + Math.pow(k - z, 2));
					if (dist > radius) continue;
					// getFluidId esta marcado para remover, pero no encontre otra forma de conseguir el fluido.
					if (Fluid.getAssetMap().getAsset(wc.getFluidId(i, j, k)).getId().equalsIgnoreCase("Water")) return true;
				}
			}
		}
		return false;
	}
	
	private String getBiome(World world, ChunkGenerator generator, int x, int y, int z) {
		int seed = (int) world.getWorldConfig().getSeed();
		ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
		return result.getBiome().getName();
	}
	
	public BlockTickStrategy onTickTest(@Nonnull World world, WorldChunk wc, int x, int y, int z, int blockId) {
		
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
			
			BlockType blockType = world.getBlockType(x, y + 1, z);
			if (blockType == null) {
				LOGGER.atInfo().log("El bloque superior es nulo");
			} else {
				int rotation = wc.getRotationIndex(x, y + 1, z);
				LOGGER.atInfo().log("El bloque superior es: " + blockType.getId() + " | " + RotationTuple.get(rotation).pitch());
			}
			
			
			Fluid fluid = Fluid.getAssetMap().getAsset(wc.getFluidId(x, y + 1, z));
			LOGGER.atInfo().log("El fluido es: " + (fluid == null ? "nulo" : fluid.getId()));
		}
		
		return BlockTickStrategy.CONTINUE;
	}
	
	private void loadRulesFromKeys() {
		this.rules = new ArrayList<>();
		var config = HytaleDevPlugin.getGrassGrowthConfig();
		for (String key : ruleKeys) {
			GrowthRule rule = config.getRule(key);
			if (rule != null) this.rules.add(rule);
		}
	}
}
