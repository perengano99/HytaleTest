package com.perengano99.hytaleplugin;

import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;


@SuppressWarnings("unused")
public class HytaleDevPlugin extends JavaPlugin {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static HytaleDevPlugin instance;
	
	private final Config<GrassGrowthConfig> grassGrowthConfig;
	
	public HytaleDevPlugin(JavaPluginInit init) {
		super(init);
		instance = this;
		LOGGER.atInfo().log("Plugin inicializado!");
		grassGrowthConfig = withConfig("GrassGrowthConfig", GrassGrowthConfig.CODEC);
		LOGGER.atInfo().log("Config cargada!");
	}
	
	public static GrassGrowthConfig getGrassGrowthConfig() {
		assert instance != null;
		return instance.grassGrowthConfig.get();
	}
	
	@Override
	protected void setup() {
		LOGGER.atInfo().log("Hello, Hytale! The development environment is up and running.");
		
		
		//		int dirtId = BlockType.getAssetMap().getIndex("Soil_Dirt");
		//		this.getEntityStoreRegistry().registerSystem(new GlobalUpdateSystem());
		TickProcedure.CODEC.register("pedo_procedure", PedoProcedure.class, PedoProcedure.CODEC);
		// Desactivado por el momento.
		// getEventRegistry().registerGlobal(EventPriority.NORMAL, ChunkPreLoadProcessEvent.class, this::tickingPreviousBlocks);
		getCommandRegistry().registerCommand(new LocateBiomeCommand());
		
		LOGGER.atInfo().log("Ticking CARGADO!");
	}
	
	private void tickingPreviousBlocks(@Nonnull ChunkPreLoadProcessEvent event) {
		if (!isEnabled() || event.isNewlyGenerated()) return; // BlockTickPlugin ya gestiona chunks nuevos.
		
		BlockChunk bc = event.getChunk().getBlockChunk();
		Holder<ChunkStore> holder = event.getHolder();
		
		ChunkColumn column = holder.getComponent(ChunkColumn.getComponentType());
		if (column == null) return;
		
		Holder<ChunkStore>[] sections = column.getSectionHolders();
		if (sections == null) return;
		
		int targetId = BlockType.getAssetMap().getIndex("Soil_Grass");
		boolean modified = false;
		
		for (Holder<ChunkStore> sectionHolder : sections) {
			BlockSection section = sectionHolder.ensureAndGetComponent(BlockSection.getComponentType());
			if (section.isSolidAir()) continue;
			
			for (int i = 0; i < 32768; i++) {
				if (section.get(i) == targetId) {
					section.setTicking(i, true);
					modified = true;
				}
			}
		}
		
		if (modified)
			bc.markNeedsSaving();
	}
}