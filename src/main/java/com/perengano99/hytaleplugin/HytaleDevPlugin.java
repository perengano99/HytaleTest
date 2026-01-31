package com.perengano99.hytaleplugin;

import com.hypixel.hytale.builtin.blocktick.BlockTickPlugin;
import com.hypixel.hytale.builtin.buildertools.prefabeditor.PrefabDirtySystems.BlockBreakDirtySystem;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.BlockNeighbor;
import com.hypixel.hytale.server.core.asset.type.blocktick.config.TickProcedure;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.event.events.ecs.BreakBlockEvent;
import com.hypixel.hytale.server.core.event.events.ecs.PlaceBlockEvent;
import com.hypixel.hytale.server.core.modules.interaction.BlockHarvestUtils;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.BreakBlockInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.data.TreeCollector;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.PluginBase;
import com.hypixel.hytale.server.core.plugin.PluginInit;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.ChunkColumn;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.events.ChunkPreLoadProcessEvent;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;
import com.perengano99.hytaleplugin.blocktick.NaturalGrassTick;
import com.perengano99.hytaleplugin.commands.LocateBiomeCommand;
import com.perengano99.hytaleplugin.commands.ReloadCommand;
import com.perengano99.hytaleplugin.commands.SyncAssetsCommand;
import com.perengano99.hytaleplugin.config.PluginConfig;
import com.perengano99.hytaleplugin.interaction.BlockInfoGetterInteraction;
import com.sun.source.tree.Tree;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.nio.file.Path;


@SuppressWarnings("unused")
public class HytaleDevPlugin extends JavaPlugin {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	private static HytaleDevPlugin instance;
	private static final Path pluginDataDirectory = Path.of("mods/HytaleDevPlugin");
	
	//	private final Config<GrassGrowthConfig> grassGrowthConfig;
	//	private final Config<ExposureBlockerModelsConfig> exposureBlockerModelsConfig;
	private final Config<PluginConfig> pluginConfig;
	
	public HytaleDevPlugin(JavaPluginInit init) {
		super(init);
		// Replace data directory to one more aesthetic.
		try {
			Field initDataDirectoryField = PluginInit.class.getDeclaredField("dataDirectory");
			initDataDirectoryField.setAccessible(true);
			initDataDirectoryField.set(init, pluginDataDirectory);
			
			Field baseDataDirectoryField = PluginBase.class.getDeclaredField("dataDirectory");
			baseDataDirectoryField.setAccessible(true);
			baseDataDirectoryField.set(this, pluginDataDirectory);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		pluginConfig = withConfig("config", PluginConfig.CODEC);
		
		instance = this;
		LOGGER.atInfo().log("=======================================================");
		LOGGER.atInfo().log("¡Hytale Dev Plugin Creado!");
		LOGGER.atInfo().log("=======================================================");
	}
	
	public static PluginConfig getConfig() {
		assert instance != null;
		return instance.pluginConfig.get();
		PlaceBlockEvent
	}
	
	public static void reloadConfigs() {
		assert instance != null;
		
		instance.pluginConfig.load();
		LOGGER.atInfo().log("Plugin Reloaded!");
	}
	
	@Override
	protected void setup() {
		LOGGER.atInfo().log("Hello, Hytale! The development environment is up and running.");
		try {
			//			grassGrowthConfig.save();
			//			exposureBlockerModelsConfig.save();
			pluginConfig.save();
		} catch (Exception _) {}
		
		//		int dirtId = BlockType.getAssetMap().getIndex("Soil_Dirt");
		//		this.getEntityStoreRegistry().registerSystem(new GlobalUpdateSystem());
		TickProcedure.CODEC.register("pedo_procedure", PedoProcedure.class, PedoProcedure.CODEC);
		
		// Desactivado por el momento.
		// getEventRegistry().registerGlobal(EventPriority.NORMAL, ChunkPreLoadProcessEvent.class, this::tickingPreviousBlocks);
		getCommandRegistry().registerCommand(new LocateBiomeCommand());
		getCommandRegistry().registerCommand(new ReloadCommand());
		
		// WARNING: DEVELOPMENT-ONLY COMMAND! Remove or disable before production builds.
		// This command allows execution of Gradle tasks from the server and poses a security risk.
		// DO NOT include in production or public releases.
		getCommandRegistry().registerCommand(new SyncAssetsCommand());
		
		getCodecRegistry(Interaction.CODEC).register("block_info_getter", BlockInfoGetterInteraction.class, BlockInfoGetterInteraction.CODEC);
		
		LOGGER.atInfo().log("Ticking CARGADO!");
		
		
	}
	
	@Override
	protected void start0() {
		super.start0();
		
		NaturalGrassTick.registerTickHandlers();
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
	
	//	private <T> Config<T> createConfig(List<Config<?>> configs, String name, BuilderCodec<T> codec) {
	//		Config<T> config = new Config<>(new Path() {}, name, codec);
	//	}
}