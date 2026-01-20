package com.perengano99.hytaleplugin.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.array.ArrayCodec;

import java.util.Set;

public class ExposureBlockerModelsConfig {
	
	public static final BuilderCodec<ExposureBlockerModelsConfig> CODEC = BuilderCodec.builder(ExposureBlockerModelsConfig.class, ExposureBlockerModelsConfig::new)
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
	
	public Set<String> getModelsWitheList() {
		return Set.of(modelsWhitelist);
	}
	
	public Set<String> getRotativeModelsList() {
		return Set.of(rotativeModelsList);
	}
	
	public Set<String> getBlacklist() {
		return Set.of(blacklist);
	}
}
