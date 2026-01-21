package com.perengano99.hytaleplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;
import com.hypixel.hytale.codec.codecs.map.MapCodec;

import java.util.*;

public class GrassGrowthConfig {
	
	private boolean enabled = true;
	private int waterNearbyRadius = 4;
	private Map<String, GrowthRule> rules = Map.of();
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public GrowthRule getRule(String rule) {
		return rules.get(rule);
	}
	
	public Map<String, GrowthRule> getRules() {
		return rules;
	}
	
	public int getWaterNearbyRadius() {
		return waterNearbyRadius;
	}
	
	public static final BuilderCodec<GrassGrowthConfig> CODEC = BuilderCodec.builder(GrassGrowthConfig.class, GrassGrowthConfig::new)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
					(c, v, unused) -> c.enabled = v,
					(c, unused) -> c.enabled)
			.add()
			.append(new KeyedCodec<>("WaterNearbyRadius", Codec.INTEGER),
					(c, v, unused) -> c.waterNearbyRadius = v,
					(c, unused) -> c.waterNearbyRadius)
			.add()
			.append(new KeyedCodec<>("Rules", new MapCodec<>(GrowthRule.CODEC, HashMap::new)),
					(c, v, unused) -> c.rules = v,
					(c, i) -> c.rules)
			.add()
			.build();
	
	public enum RuleType { SPREAD, CONVERSION }
	
	public enum LightSource { GENERAL, SKY }
	
	public static class GrowthRule {
		
		private RuleType type;
		private String[] targetBlocks;
		private GrowthConditions conditions;
		private float chance;
		
		public RuleType getType() {
			return type;
		}
		
		public List<String> getTargetBlocks() {
			return List.of(targetBlocks);
		}
		
		
		public GrowthConditions getConditions() {
			return conditions;
		}
		
		public float getChance() {
			return chance;
		}
		
		public static final BuilderCodec<GrowthRule> CODEC = BuilderCodec.builder(GrowthRule.class, GrowthRule::new)
				.append(new KeyedCodec<>("Type", Codec.STRING),
						(c, v, unused) -> c.type = RuleType.valueOf(v),
						(c, unused) -> c.type.name())
				.add()
				.append(new KeyedCodec<>("TargetBlocks", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, unused) -> c.targetBlocks = v,
						(c, unused) -> c.targetBlocks)
				.add()
				.append(new KeyedCodec<>("Conditions", GrowthConditions.CODEC),
						(c, v, unused) -> c.conditions = v,
						(c, unused) -> c.conditions)
				.add()
				.append(new KeyedCodec<>("Chance", Codec.FLOAT),
						(c, v, i) -> c.chance = v,
						(c, i) -> c.chance)
				.add()
				.build();
	}
	
	public static class GrowthConditions {
		
		private int minLight = 0;
		private LightSource lightSource = LightSource.SKY;
		private boolean needsSkyExposure = true;
		private boolean needsNearbyWater = false;
		private String[] biomes = {};
		private boolean excludeBiomes = false;
		
		public int getMinLight() {
			return minLight;
		}
		
		public LightSource getLightSource() {
			return lightSource;
		}
		
		public boolean isNeedsSkyExposure() {
			return needsSkyExposure;
		}
		
		public boolean isNeedsNearbyWater() {
			return needsNearbyWater;
		}
		
		public Set<String> getBiomes() {
			return Set.of(biomes);
		}
		
		public boolean isExcludeBiomes() {
			return excludeBiomes;
		}
		
		public static final BuilderCodec<GrowthConditions> CODEC = BuilderCodec.builder(GrowthConditions.class, GrowthConditions::new)
				.append(new KeyedCodec<>("MinLight", Codec.INTEGER),
						(c, v, unused) -> c.minLight = v,
						(c, unused) -> c.minLight)
				.add()
				.append(new KeyedCodec<>("LightSource", Codec.STRING),
						(c, v, unused) -> c.lightSource = LightSource.valueOf(v),
						(c, unused) -> c.lightSource.name())
				.add()
				.append(new KeyedCodec<>("NeedsSkyExposure", Codec.BOOLEAN),
						(c, v, unused) -> c.needsSkyExposure = v,
						(c, unused) -> c.needsSkyExposure)
				.add()
				.append(new KeyedCodec<>("NeedsNearbyWater", Codec.BOOLEAN),
						(c, v, unused) -> c.needsNearbyWater = v,
						(c, unused) -> c.needsNearbyWater)
				.add()
				.append(new KeyedCodec<>("Biomes", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, unused) -> c.biomes = v,
						(c, unused) -> c.biomes)
				.add()
				.append(new KeyedCodec<>("ExcludeBiomes", Codec.BOOLEAN),
						(c, v, unused) -> c.excludeBiomes = v,
						(c, unused) -> c.excludeBiomes)
				.add()
				.build();
	}
}
