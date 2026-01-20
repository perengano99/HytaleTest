package com.perengano99.hytaleplugin;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

public class GrassGrowthConfig {
	private boolean enabled = true;
	private GrowthRule[] rules;

	public boolean isEnabled() {
		return enabled;
	}

	public GrowthRule[] getRules() {
		return rules;
	}

	public static final BuilderCodec<GrassGrowthConfig> CODEC = BuilderCodec.builder(GrassGrowthConfig.class, GrassGrowthConfig::new)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
					(c, v, unused) -> c.enabled = v,
					(c, unused) -> c.enabled)
			.add()
			.append(new KeyedCodec<>("Rules", new ArrayCodec<>(GrowthRule.CODEC, GrowthRule[]::new)),
					(c, v, unused) -> c.rules = v,
					(c, unused) -> c.rules)
			.add()
			.build();

	public enum RuleType { SPREAD, CONVERSION }

	public enum LightSource { GENERAL, SKY }

	public static class GrowthRule {
		private RuleType type;
		private String[] targetBlocks;
		private String[] targetGroups;
		private GrowthConditions conditions;

		public RuleType getType() {
			return type;
		}

		public String[] getTargetBlocks() {
			return targetBlocks;
		}

		public String[] getTargetGroups() {
			return targetGroups;
		}

		public GrowthConditions getConditions() {
			return conditions;
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
				.append(new KeyedCodec<>("TargetGroups", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, unused) -> c.targetGroups = v,
						(c, unused) -> c.targetGroups)
				.add()
				.append(new KeyedCodec<>("Conditions", GrowthConditions.CODEC),
						(c, v, unused) -> c.conditions = v,
						(c, unused) -> c.conditions)
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

		public String[] getBiomes() {
			return biomes;
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
