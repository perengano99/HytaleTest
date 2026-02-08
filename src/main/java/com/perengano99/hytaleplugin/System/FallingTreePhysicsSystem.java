package com.perengano99.hytaleplugin.System;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perengano99.hytaleplugin.Component.FallingTreeEntityBlock;
import com.perengano99.hytaleplugin.HytaleDevPlugin;
import com.perengano99.hytaleplugin.TreePhysicsProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Sistema de física para árboles en caída.
 * Solo se encarga de encontrar los componentes y llamar a los métodos de actualización del provider.
 * Toda la lógica de cálculo está en TreePhysicsProvider.
 */
public class FallingTreePhysicsSystem extends EntityTickingSystem<EntityStore> {
	
	@Override
	public void tick(float dt, int index, @Nonnull ArchetypeChunk<EntityStore> chunk, @Nonnull Store<EntityStore> store, @Nonnull CommandBuffer<EntityStore> commandBuffer) {
		FallingTreeEntityBlock entity = chunk.getComponent(index, HytaleDevPlugin.getFallingTreeEntityBlockComponentType());
		if (entity == null) return;
		
		TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
		if (transform == null) return;
		
		// Obtener el TreePhysicsProvider
		Object providerObj = entity.getTreePhysicsProvider();
		if (!(providerObj instanceof TreePhysicsProvider)) return;
		
		TreePhysicsProvider provider = (TreePhysicsProvider) providerObj;
		
		// Obtener World y BoundingBox para detectar colisiones
		World world = commandBuffer.getExternalData().getWorld();
		BoundingBox boundingBox = chunk.getComponent(index, BoundingBox.getComponentType());
		
		// Procesar ROOT: el provider actualizará física, posición, rotación y colisiones
		if (entity.isRoot()) {
			provider.updateRoot(dt, entity, transform, world, boundingBox);
		}
		// Procesar CHILD: el provider actualizará posición, rotación y colisiones basándose en el estado del ROOT
		else {
			provider.updateChild(entity, transform, world, boundingBox);
		}
	}
	
	@Nullable
	@Override
	public Query<EntityStore> getQuery() {
		return Query.and(HytaleDevPlugin.getFallingTreeEntityBlockComponentType());
	}
}




