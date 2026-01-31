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
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.spawning.util.LightRangePredicate;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PedoProcedure extends TickProcedure {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static final Random random = new Random();
	
	public static final BuilderCodec<PedoProcedure> CODEC = BuilderCodec.builder(PedoProcedure.class, PedoProcedure::new, TickProcedure.BASE_CODEC)
			.append(new KeyedCodec<>("Rules", new ArrayCodec<>(Codec.STRING, String[]::new)),
					(c, v, unused) -> c.ruleKeys = List.of(v),
					(c, unused) -> c.ruleKeys.toArray(new String[0]))
			.add()
			.build();
	
	
	private List<String> ruleKeys = new ArrayList<>();
	private int light = -1, sky = -1;
	private int skyExposure = -1;
	private int waterNearby = -1;
	private String biome = "-1";
	
	
	public static void registerTickHandlers() {
		BlockType bt = BlockType.getAssetMap().getAsset("Example_Block");
		
		try {
			Field tickProcedureField = BlockType.class.getDeclaredField("tickProcedure");
			tickProcedureField.setAccessible(true);
			tickProcedureField.set(bt, new PedoProcedure());
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	//	@Override
	public BlockTickStrategy onTick(@Nonnull World world, WorldChunk wc, int x, int y, int z, int blockId) {
//		var config = HytaleDevPlugin.getGrassGrowthConfig();
//		if (!config.isEnabled()) return BlockTickStrategy.CONTINUE;

//		LOGGER.atInfo().log("KAWATE");
//
//		IWorldGen worldGen = world.getChunkStore().getGenerator();
//		if (worldGen instanceof ChunkGenerator generator) {
//			reset();
//			Store<EntityStore> store = world.getEntityStore().getStore();
//			WorldTimeResource worldTimeResource = store.getResource(WorldTimeResource.getResourceType());
//			String sourceBlock = BlockType.getAssetMap().getAsset(blockId).getId();
//
//			List<GrowthRule> rules = List.of();
//			for (GrowthRule rule : rules) {
//				if (random.nextFloat() > rule.getChance()) continue;
//				if (rule.getTargetBlocks().isEmpty()) {
//					LOGGER.atWarning().log("No target blocks were provided for an rule");
//					continue;
//				}
//
//				// Spread Action
//				if (rule.getType() == RuleType.SPREAD) {
//					int dx = random.nextInt(3) - 1;
//					int dy = random.nextInt(3) - 1;
//					int dz = random.nextInt(3) - 1;
//					// No permitir esparcir en (0,0,0), (0,1,0) ni (0,-1,0)
//					if (dx == 0 && dz == 0) continue;
//
//					int nx = x + dx, ny = y + dy, nz = z + dz;
//					BlockType neighbor = BlockType.getAssetMap().getAsset(world.getBlock(nx, ny, nz));
//
//					if (!rule.getTargetBlocks().contains(neighbor.getId()) || isCovered(wc, nx, ny, nz))
//						continue;
//					if (checkRuleConditions(rule.getConditions(), world, wc, x, y, z, generator, worldTimeResource))
//						continue;
//
//					world.setBlock(nx, ny, nz, sourceBlock);
//					LOGGER.atInfo().log("Se esparció un bloque! de " + neighbor.getId() + " a " + sourceBlock);
//				}
//				// Conversion Action
//				else {
//					String targetBlock = rule.getTargetBlocks().get(random.nextInt(rule.getTargetBlocks().size()));
//					int index = BlockType.getAssetMap().getIndex(targetBlock);
//					if (index == Integer.MIN_VALUE) {
//						LOGGER.atWarning().log("Unknown target block in an Rule: " + targetBlock);
//						continue;
//					}
//					if (checkRuleConditions(rule.getConditions(), world, wc, x, y, z, generator, worldTimeResource))
//						continue;
//
//					world.setBlock(x, y, z, targetBlock);
//					LOGGER.atInfo().log("Se convirtió un bloque! de " + sourceBlock + " a " + targetBlock);
//				}
//				break;
//			}
//		}
		return BlockTickStrategy.CONTINUE;
	}
	
	private void reset() {
		light       = -1;
		sky         = -1;
		skyExposure = -1;
		waterNearby = -1;
		biome       = "-1";
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
//		ExposureBlockerModelsConfig cfg = HytaleDevPlugin.getExposureBlockerModelsConfig();
		
//		if (drawType == DrawType.Cube || drawType == DrawType.CubeWithModel) return !cfg.getBlacklist().contains(id);
		
//		if (cfg.getRotativeModelsList().contains(id)) {
			// getRotationIndex esta marcado para remover, pero no encontre otra forma de conseguir la rotacion.
//			int rotation = wc.getRotationIndex(x, y + 1, z);
//			if (RotationTuple.get(rotation).pitch() == Rotation.None) return true;
//		}
//		return cfg.getModelsWitheList().contains(id);
		return false;
	}
	
	private boolean isWaterNearby(WorldChunk wc, int x, int y, int z, int radius) {
		int radiusSq = radius * radius; // Pre-calcula el cuadrado
		
		for (int i = x - radius; i <= x + radius; i++) {
			for (int j = y - radius; j <= y + radius; j++) {
				for (int k = z - radius; k <= z + radius; k++) {
					// Distancia euclidiana al cuadrado (más rápido que sqrt)
					double distSq = (i - x) * (i - x) + (j - y) * (j - y) + (k - z) * (k - z);
					if (distSq > radiusSq) continue;
					
					// getFluidId es rápido, úsalo sin miedo aunque esté deprecated por ahora
					if (Fluid.getAssetMap().getAsset(wc.getFluidId(i, j, k)).getId().equalsIgnoreCase("Water"))
						return true;
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
	
}
