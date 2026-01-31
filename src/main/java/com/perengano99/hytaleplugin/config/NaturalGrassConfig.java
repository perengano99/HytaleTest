package com.perengano99.hytaleplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Set;

public class NaturalGrassConfig {
	
	public static final BuilderCodec<NaturalGrassConfig> CODEC = BuilderCodec.builder(NaturalGrassConfig.class, NaturalGrassConfig::new)
			.append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
					(c, v, i) -> c.enabled = v,
					(c, i) -> c.enabled)
			.add()
			.append(new KeyedCodec<>("SpreadChance", Codec.FLOAT),
					(c, v, i) -> c.processChance = Math.max(0, Math.min(100, v)),
					(c, i) -> c.processChance)
			.add()
			.append(new KeyedCodec<>("MinSkyLightPercent", Codec.FLOAT),
					(c, v, i) -> c.skyPercent = Math.max(0, Math.min(100, v)),
					(c, i) -> c.skyPercent)
			.add()
			.append(new KeyedCodec<>("GrassWithoutExposureDecay", Codec.BOOLEAN),
					(c, v, i) -> c.grassExposureDecay = v,
					(c, i) -> c.grassExposureDecay)
			.add()
			.append(new KeyedCodec<>("GrassDryInZone2", Codec.BOOLEAN),
					(c, v, i) -> c.grassZone2Dry = v,
					(c, i) -> c.grassZone2Dry)
			.add()
			.append(new KeyedCodec<>("ExposureBlockerModels", ExposureBlockerModelsConfig.CODEC),
					(c, v, i) -> c.exposureBlocker = v,
					(c, i) -> c.exposureBlocker)
			.add()
			.build();
	
	private boolean enabled = true;
	private boolean grassExposureDecay = true;
	private boolean grassZone2Dry = true;
	private float processChance = 0.05f;
	private float skyPercent = 60;
	private ExposureBlockerModelsConfig exposureBlocker = new ExposureBlockerModelsConfig();
	
	public boolean isEnabled() {
		return enabled;
	}
	
	public boolean grassExposureDecay() {
		return grassExposureDecay;
	}
	
	public boolean grassZone2Dry() {
		return grassZone2Dry;
	}
	
	public float getProcessChance() {
		return processChance;
	}
	
	public float getMinSkyPercent() {
		return skyPercent;
	}
	
	public Set<String> getModelsWitheList() {
		return Set.of(exposureBlocker.modelsWhitelist);
	}
	
	public Set<String> getRotativeModelsList() {
		return Set.of(exposureBlocker.rotativeModelsList);
	}
	
	public Set<String> getBlacklist() {
		return Set.of(exposureBlocker.blacklist);
	}
	
	private static class ExposureBlockerModelsConfig {
		
		public static final BuilderCodec<ExposureBlockerModelsConfig> CODEC = BuilderCodec.builder(
						ExposureBlockerModelsConfig.class, ExposureBlockerModelsConfig::new)
				.append(new KeyedCodec<>("ModelsWhitelist", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, i) -> c.modelsWhitelist = v,
						(c, i) -> c.modelsWhitelist)
				.add()
				.append(new KeyedCodec<>("RotativeModelsList", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, i) -> c.rotativeModelsList = v,
						(c, i) -> c.rotativeModelsList)
				.add()
				.append(new KeyedCodec<>("Blacklist", new ArrayCodec<>(Codec.STRING, String[]::new)),
						(c, v, i) -> c.blacklist = v,
						(c, i) -> c.blacklist)
				.add()
				.build();
		
		private String[] modelsWhitelist = {};
		private String[] rotativeModelsList = {};
		private String[] blacklist = {};
	}
}
