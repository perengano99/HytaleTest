package com.perengano99.hytaleplugin.config;

import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class PluginConfig {
	
	public static final BuilderCodec<PluginConfig> CODEC = BuilderCodec.builder(PluginConfig.class, PluginConfig::new)
			.append(new KeyedCodec<>("NaturalGrassGrowth", NaturalGrassConfig.CODEC),
					(c, v, i) -> c.grassConfig = v,
					(c, i) -> c.grassConfig)
			.add()
			.build();
	
	private NaturalGrassConfig grassConfig = new NaturalGrassConfig();
	
	public NaturalGrassConfig getGrassConfig() {
		return grassConfig;
	}
}
