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
			
			Vector3d pivotPos = new Vector3d(x + 0.5, y + 1.5, z + 0.5);
			
			// Dirección de caída aleatoria (permitiendo diagonales)
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
			
			// 1. Crear entidad ROOT
			Holder<EntityStore> rootHolder = FallingTreeEntityBlock.createRoot(
				TreeBlockType.TRUNK,
				btype.getId(),
				pivotPos,
				fallDirection
			);
			
			rootHolder.addComponent(DespawnComponent.getComponentType(),
				DespawnComponent.despawnInSeconds(store.getResource(TimeResource.getResourceType()), 30));
			rootHolder.addComponent(NetworkId.getComponentType(),
				new NetworkId(store.getExternalData().takeNextNetworkId()));
			
			Ref<EntityStore> rootRef = store.addEntity(rootHolder, AddReason.SPAWN);
			FallingTreeEntityBlock rootEntity = rootHolder.getComponent(
				HytaleDevPlugin.getFallingTreeEntityBlockComponentType()
			);
			
			// Crear el TreePhysicsProvider para gestionar la física del árbol completo
			TreePhysicsProvider physicsProvider = new TreePhysicsProvider(rootRef, pivotPos, fallDirection);
			
			// Asignar el provider al ROOT
			if (rootEntity != null) {
				rootEntity.setTreePhysicsProvider(physicsProvider);
			}
			
			// 2. Crear entidades CHILD - Tronco principal
			for (int i = 2; i < 8; i++) {
				Vector3d childPos = new Vector3d(x + 0.5, y + i + 0.5, z + 0.5);
				
				Holder<EntityStore> childHolder = FallingTreeEntityBlock.createChild(
					TreeBlockType.TRUNK,
					btype.getId(),
					childPos,
					fallDirection,
					rootRef,
					pivotPos
				);
				
				childHolder.addComponent(DespawnComponent.getComponentType(),
					DespawnComponent.despawnInSeconds(store.getResource(TimeResource.getResourceType()), 30));
				childHolder.addComponent(NetworkId.getComponentType(),
					new NetworkId(store.getExternalData().takeNextNetworkId()));
				
				Ref<EntityStore> childRef = store.addEntity(childHolder, AddReason.SPAWN);
				
				// Agregar al provider
				physicsProvider.addChild(childRef);
				
				FallingTreeEntityBlock childEntity = childHolder.getComponent(
					HytaleDevPlugin.getFallingTreeEntityBlockComponentType()
				);
				if (childEntity != null) {
					childEntity.setTreePhysicsProvider(physicsProvider);
				}
			}
			
			// Ramas hacia los lados (simulando copa del árbol)
			for (int ramo = 0; ramo < 3; ramo++) {
				for (int i = 4; i < 7; i++) {
					for (int j = 1; j <= 3; j++) {
						Vector3d childPos;
						if (ramo == 0) {
							childPos = new Vector3d(x + 0.5 - j, y + i + 0.5, z + 0.5 - j);
						} else if (ramo == 1) {
							childPos = new Vector3d(x + 0.5 - j, y + i + 0.5, z + 0.5 + j);
						} else {
							childPos = new Vector3d(x + 0.5 + j, y + i + 0.5, z + 0.5 - j);
						}
						
						Holder<EntityStore> childHolder = FallingTreeEntityBlock.createChild(
							TreeBlockType.TRUNK,
							btype.getId(),
							childPos,
							fallDirection,
							rootRef,
							pivotPos
						);
						
						childHolder.addComponent(DespawnComponent.getComponentType(),
							DespawnComponent.despawnInSeconds(store.getResource(TimeResource.getResourceType()), 30));
						childHolder.addComponent(NetworkId.getComponentType(),
							new NetworkId(store.getExternalData().takeNextNetworkId()));
						
						Ref<EntityStore> childRef = store.addEntity(childHolder, AddReason.SPAWN);
						
						// Agregar al provider
						physicsProvider.addChild(childRef);
						
						FallingTreeEntityBlock childEntity = childHolder.getComponent(
							HytaleDevPlugin.getFallingTreeEntityBlockComponentType()
						);
						if (childEntity != null) {
							childEntity.setTreePhysicsProvider(physicsProvider);
						}
					}
				}
			}
		});
	}
}
