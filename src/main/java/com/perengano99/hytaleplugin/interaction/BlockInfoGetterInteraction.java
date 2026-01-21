package com.perengano99.hytaleplugin.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.spawning.util.LightRangePredicate;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;

import javax.annotation.Nonnull;

public class BlockInfoGetterInteraction extends SimpleInstantInteraction {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	public static BuilderCodec<BlockInfoGetterInteraction> CODEC = BuilderCodec.builder(BlockInfoGetterInteraction.class, BlockInfoGetterInteraction::new,
			BlockInfoGetterInteraction.ABSTRACT_CODEC).build();
	
	@Override
	protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		assert commandBuffer != null;
		
		World world = commandBuffer.getExternalData().getWorld();
		
		int x, y, z;
		String blockName = "";
		
		BlockPosition targetBlockPos = context.getTargetBlock();
		if (targetBlockPos != null) {
			x = targetBlockPos.x;
			y = targetBlockPos.y;
			z = targetBlockPos.z;
		} else {
			Ref<EntityStore> ownerRef = context.getOwningEntity();
			Vector3d playerPos = commandBuffer.getComponent(ownerRef, TransformComponent.getComponentType()).getPosition();
			
			x = (int) playerPos.x;
			y = (int) playerPos.y - 1;
			z = (int) playerPos.z;
			
			blockName = "AbovePlayer: ";
		}
		BlockType btype = world.getBlockType(x, y, z);
		blockName += btype.getId();
		
		String info = "\n" + blockName + " | cords: [" + x + ", " + y + ", " + z + "]";
		
		IWorldGen worldGen = world.getChunkStore().getGenerator();
		if (worldGen instanceof ChunkGenerator generator) {
			int seed = (int) world.getWorldConfig().getSeed();
			ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, x, z);
			
			info += "\nZone: " + result.getZoneResult().getZone().name() + " | Biome: " + result.getBiome().getName();
		}
		
		long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
		WorldChunk chunk = world.getChunk(chunkIndex);
		WorldTimeResource worldTimeResource = world.getEntityStore().getStore().getResource(WorldTimeResource.getResourceType());
		
		byte light = LightRangePredicate.calculateLightValue(chunk.getBlockChunk(), x, y, z, worldTimeResource.getSunlightFactor());
		byte skyLight = (byte) (chunk.getBlockChunk().getSkyLight(x, y, z) * worldTimeResource.getSunlightFactor());
		
		info += "\nGlobal Light: " + LightRangePredicate.lightToPrecentage(light) + " | Sky Light: " + LightRangePredicate.lightToPrecentage(skyLight);
		
		int rotIndex = chunk.getRotationIndex(x, y, z);
		RotationTuple rotation = RotationTuple.get(rotIndex);
		info += "\nRotation: [" + rotation.pitch().getDegrees() + ", " + rotation.yaw().getDegrees() + ", " + rotation.roll().getDegrees() + "]";
		
		LOGGER.atInfo().log(info);
	}
}
