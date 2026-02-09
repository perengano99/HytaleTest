package com.perengano99.hytaleplugin.interaction;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.DespawnComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.modules.time.TimeResource;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perengano99.hytaleplugin.Component.FallingTreeEntityBlock;
import com.perengano99.hytaleplugin.Component.FallingTreeEntityBlock.TreeBlockType;
import com.perengano99.hytaleplugin.HytaleDevPlugin;
import com.perengano99.hytaleplugin.TreePhysicsProvider;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class FallingBlockSpawnInteraction extends SimpleInstantInteraction {
	
	public static final BuilderCodec<FallingBlockSpawnInteraction> CODEC = BuilderCodec.builder(
			FallingBlockSpawnInteraction.class,
			FallingBlockSpawnInteraction::new,
			FallingBlockSpawnInteraction.ABSTRACT_CODEC
	                                                                                           ).build();
	
	@Override
	protected void firstRun(@Nonnull InteractionType type, @Nonnull InteractionContext context, @Nonnull CooldownHandler cooldownHandler) {
		CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
		assert commandBuffer != null;
		
		World world = commandBuffer.getExternalData().getWorld();
		
		world.execute(() -> {
			Store<EntityStore> store = world.getEntityStore().getStore();
			
			BlockPosition targetBlockPos = context.getTargetBlock();
			if (targetBlockPos == null) return;
			
			int x = targetBlockPos.x;
			int y = targetBlockPos.y;
			int z = targetBlockPos.z;
			
			BlockType btype = world.getBlockType(x, y, z);
			if (btype == null) return;
			
			// El pivote será la base del árbol (justo encima del bloque clickeado)
			Vector3d pivotPos = new Vector3d(x + 0.5, y + 2, z + 0.5);
			
			// Dirección de caída aleatoria
			double randomAngle = Math.random() * Math.PI * 2;
			double randomDiagonal = Math.random();
			
			Vector3d fallDirection = new Vector3d(
					Math.cos(randomAngle) * randomDiagonal,
					0,
					Math.sin(randomAngle) * randomDiagonal
			);
			
			if (fallDirection.length() > 1e-6) {
				fallDirection = fallDirection.normalize();
			} else {
				fallDirection = new Vector3d(0, 0, 1);
			}
			// Definir la estructura del árbol (Offsets relativos al pivote)
			List<Vector3d> treeStructure = new ArrayList<>();
			
			// --- Tronco Central (Altura 5) ---
			for (int h = 1; h <= 5; h++) {
				treeStructure.add(new Vector3d(0, h, 0));
			}
			
			// --- Ramas Bajas (Cruz en altura 3) ---
			treeStructure.add(new Vector3d(1, 3, 0));
			treeStructure.add(new Vector3d(-1, 3, 0));
			treeStructure.add(new Vector3d(0, 3, 1));
			treeStructure.add(new Vector3d(0, 3, -1));
			
			// --- Ramas Medias (Diagonales en altura 4) ---
			treeStructure.add(new Vector3d(1, 4, 1));
			treeStructure.add(new Vector3d(-1, 4, -1));
			treeStructure.add(new Vector3d(1, 4, -1));
			treeStructure.add(new Vector3d(-1, 4, 1));
			// Extensión de ramas medias
			treeStructure.add(new Vector3d(2, 4, 0));
			treeStructure.add(new Vector3d(-2, 4, 0));
			
			// --- Copa (Cruz pequeña en altura 5) ---
			treeStructure.add(new Vector3d(1, 5, 0));
			treeStructure.add(new Vector3d(-1, 5, 0));
			treeStructure.add(new Vector3d(0, 5, 1));
			treeStructure.add(new Vector3d(0, 5, -1));
			treeStructure.add(new Vector3d(0, 6, 0)); // Punta
			
			// Calcular el centro de masa (centroide) del árbol completo
			// El ROOT está en el pivote (offset 0,0,0 relativo al pivote)
			// Sumamos todos los offsets incluyendo el ROOT
			Vector3d sumOffsets = new Vector3d(0, 0, 0); // Offset del ROOT
			for (Vector3d v : treeStructure) {
				sumOffsets.add(v);
			}
			
			// Promedio: Suma / Cantidad total de bloques (Root + Hijos)
			int totalBlocks = treeStructure.size() + 1; // +1 por el ROOT
			Vector3d centerOfMassOffset = new Vector3d(
					sumOffsets.x / totalBlocks,
					sumOffsets.y / totalBlocks,
					sumOffsets.z / totalBlocks
			);
			
			// 1. Crear entidad ROOT (La base del tronco)
			Holder<EntityStore> rootHolder = FallingTreeEntityBlock.createRoot(
					TreeBlockType.TRUNK,
					btype.getId(),
					pivotPos,
					fallDirection
			                                                                  );
			
			setupEntityComponents(store, rootHolder);
			
			Ref<EntityStore> rootRef = store.addEntity(rootHolder, AddReason.SPAWN);
			FallingTreeEntityBlock rootEntity = rootHolder.getComponent(HytaleDevPlugin.getFallingTreeEntityBlockComponentType());
			
			// Crear el TreePhysicsProvider
			TreePhysicsProvider physicsProvider = new TreePhysicsProvider(rootRef, pivotPos, fallDirection, centerOfMassOffset);
			
			if (rootEntity != null) {
				rootEntity.setTreePhysicsProvider(physicsProvider);
			}
			
			
			// 2. Generar entidades CHILD basadas en la estructura
			for (Vector3d offset : treeStructure) {
				// Calcular posición absoluta en el mundo
				Vector3d childPos = pivotPos.clone().add(offset);
				
				Holder<EntityStore> childHolder = FallingTreeEntityBlock.createChild(
						TreeBlockType.TRUNK, // Usamos TRUNK para todo para ver la estructura de madera
						btype.getId(),
						childPos,
						fallDirection,
						rootRef,
						pivotPos);
				
				setupEntityComponents(store, childHolder);
				
				Ref<EntityStore> childRef = store.addEntity(childHolder, AddReason.SPAWN);
				
				// Agregar al provider
				physicsProvider.addChild(childRef);
				
				FallingTreeEntityBlock childEntity = childHolder.getComponent(HytaleDevPlugin.getFallingTreeEntityBlockComponentType());
				if (childEntity != null) {
					childEntity.setTreePhysicsProvider(physicsProvider);
				}
			}
		});
	}
	
	// Helper para no repetir código de componentes comunes
	private void setupEntityComponents(Store<EntityStore> store, Holder<EntityStore> holder) {
		holder.addComponent(DespawnComponent.getComponentType(),
				DespawnComponent.despawnInSeconds(store.getResource(TimeResource.getResourceType()), 5)); // 60 segs para admirar el desastre
		holder.addComponent(NetworkId.getComponentType(),
				new NetworkId(store.getExternalData().takeNextNetworkId()));
	}
}