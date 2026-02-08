package com.perengano99.hytaleplugin.Component;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentModel;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollision;
import com.hypixel.hytale.server.core.modules.entity.hitboxcollision.HitboxCollisionConfig;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perengano99.hytaleplugin.HytaleDevPlugin;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

/**
 * Componente que representa un bloque de árbol en caída.
 * Contiene solo datos de identificación y referencias.
 * Toda la lógica de física está en TreePhysicsProvider.
 */
public class FallingTreeEntityBlock implements Component<EntityStore> {
	
	private static final HytaleLogger LOGGER = HytaleLogger.getLogger();
	private static final String missingTexture = "Items/FallingBlocks/Pedo_Block.png";
	
	public enum TreeBlockType { TRUNK, BRANCH, LEAVES }
	
	public static final BuilderCodec<FallingTreeEntityBlock> CODEC = BuilderCodec.builder(FallingTreeEntityBlock.class, FallingTreeEntityBlock::new)
			.append(new KeyedCodec<>("BlockId", Codec.STRING),
					(c, v, i) -> c.blockId = v,
					(c, i) -> c.blockId)
			.add()
			.append(new KeyedCodec<>("Transform", TransformComponent.CODEC),
					(c, v, i) -> c.transform = v,
					(c, i) -> c.transform)
			.add()
			.append(new KeyedCodec<>("Type", Codec.STRING),
					(c, v, i) -> c.type = TreeBlockType.valueOf(v),
					(c, i) -> c.type.name())
			.add()
			.append(new KeyedCodec<>("Dir", Vector3d.CODEC),
					(c, v, i) -> c.direction = v,
					(c, i) -> c.direction)
			.add()
			.append(new KeyedCodec<>("IsRoot", Codec.BOOLEAN),
					(c, v, i) -> c.isRoot = v,
					(c, i) -> c.isRoot)
			.add()
			.build();
	
	// Datos de identificación
	protected String blockId;
	protected TreeBlockType type;
	protected TransformComponent transform;
	protected Vector3d direction;
	protected boolean isRoot;
	
	// Referencias para estructura del árbol
	protected @Nullable Ref<EntityStore> rootEntity;
	protected Vector3d offsetFromPivot;
	
	// Referencia al proveedor de física del árbol
	protected @Nullable Object treePhysicsProvider; // Object para evitar dependencias circulares
	
	public FallingTreeEntityBlock() {}
	
	public FallingTreeEntityBlock(String blockId, TreeBlockType type, TransformComponent transform, Vector3d direction, boolean isRoot) {
		this.blockId   = blockId;
		this.type      = type;
		this.transform = transform;
		this.direction = direction;
		this.isRoot    = isRoot;
	}
	
	// Getters básicos para identificación
	public boolean isRoot() {
		return isRoot;
	}
	
	public String getBlockId() {
		return blockId;
	}
	
	public TreeBlockType getType() {
		return type;
	}
	
	public TransformComponent getTransform() {
		return transform;
	}
	
	public Vector3d getDirection() {
		return direction;
	}
	
	public @Nullable Ref<EntityStore> getRootEntity() {
		return rootEntity;
	}
	
	public Vector3d getOffsetFromPivot() {
		return offsetFromPivot;
	}
	
	// Gestión del proveedor de física
	public @Nullable Object getTreePhysicsProvider() {
		return treePhysicsProvider;
	}
	
	public void setTreePhysicsProvider(@Nullable Object provider) {
		this.treePhysicsProvider = provider;
	}
	
	// Métodos de creación de entidades
	public static Holder<EntityStore> createRoot(TreeBlockType type, String blockId, Vector3d position, Vector3d direction) {
		TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
		
		Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
		FallingTreeEntityBlock entity = new FallingTreeEntityBlock(blockId, type, transform, direction, true);
		
		// Crear modelo
		ModelAsset modelAsset = getModelAsset(type);
		Model model = Model.createScaledModel(modelAsset, 1);
		resolveTexture(type, blockId, model);
		
		holder.addComponent(HytaleDevPlugin.getFallingTreeEntityBlockComponentType(), entity);
		addHolders(holder, model, transform);
		return holder;
	}
	
	public static Holder<EntityStore> createChild(TreeBlockType type, String blockId, Vector3d position, Vector3d direction,
	                                              Ref<EntityStore> rootRef, Vector3d pivotPos) {
		TransformComponent transform = new TransformComponent(position, new Vector3f(0, 0, 0));
		
		Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
		FallingTreeEntityBlock entity = new FallingTreeEntityBlock(blockId, type, transform, direction, false);
		
		// Inicializar referencias de estructura
		entity.rootEntity = rootRef;
		entity.offsetFromPivot = position.subtract(pivotPos);
		
		// Crear modelo
		ModelAsset modelAsset = getModelAsset(type);
		Model model = Model.createScaledModel(modelAsset, 1);
		resolveTexture(type, blockId, model);
		
		holder.addComponent(HytaleDevPlugin.getFallingTreeEntityBlockComponentType(), entity);
		addHolders(holder, model, transform);
		return holder;
	}
	
	private static void addHolders(Holder<EntityStore> holder, Model model, TransformComponent transform) {
		// ...existing code...
		holder.addComponent(TransformComponent.getComponentType(), transform);
		holder.addComponent(PersistentModel.getComponentType(), new PersistentModel(model.toReference()));
		holder.addComponent(ModelComponent.getComponentType(), new ModelComponent(model));
		holder.addComponent(BoundingBox.getComponentType(), new BoundingBox(model.getBoundingBox()));
		holder.addComponent(HitboxCollision.getComponentType(), new HitboxCollision(HitboxCollisionConfig.getAssetMap().getAsset("HardCollision")));
		holder.addComponent(PhysicsValues.getComponentType(), model.getPhysicsValues());
		holder.ensureComponent(Velocity.getComponentType());
		holder.ensureComponent(UUIDComponent.getComponentType());
	}
	
	private static ModelAsset getModelAsset(TreeBlockType type) {
		// Por el momento, solo se implementa TRUNK
		// BRANCH y LEAVES serán implementados más adelante
		return ModelAsset.getAssetMap().getAsset("Falling_Block");
	}
	
	private static void resolveTexture(TreeBlockType type, String blockId, Model model) {
		try {
			String texture = "Items/FallingBlocks/" + blockId + ".png";
			//Si no se encuentra la textura dentro de "Items/FallingBlocks/" se debe usar missingTexture.
			
			Field texField = Model.class.getDeclaredField("texture");
			texField.setAccessible(true);
			
			texField.set(model, texture);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Override
	public Component<EntityStore> clone() {
		FallingTreeEntityBlock cloned = new FallingTreeEntityBlock(blockId, type, transform.clone(), direction.clone(), isRoot);
		
		if (!isRoot) {
			cloned.rootEntity = rootEntity;
			cloned.offsetFromPivot = offsetFromPivot != null ? offsetFromPivot.clone() : null;
		}
		
		return cloned;
	}
}


