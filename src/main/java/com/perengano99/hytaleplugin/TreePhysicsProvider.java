package com.perengano99.hytaleplugin;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.modules.collision.BlockCollisionProvider;
import com.hypixel.hytale.server.core.modules.collision.BlockContactData;
import com.hypixel.hytale.server.core.modules.collision.BlockData;
import com.hypixel.hytale.server.core.modules.collision.BlockTracker;
import com.hypixel.hytale.server.core.modules.collision.IBlockCollisionConsumer;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.perengano99.hytaleplugin.Component.FallingTreeEntityBlock;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Gestor de física para un árbol completo en caída.
 * Contiene toda la lógica de actualización de física, rotación y colisiones con bloques.
 * El sistema solo encuentra los componentes y llama a los métodos de actualización.
 */
public class TreePhysicsProvider implements IBlockCollisionConsumer {
	
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
	// Constantes de física
	private static final float ANGULAR_ACCELERATION = 0.8f; // radianes/segundo²
	
	// Estado del árbol
	private float currentAngle = 0f;
	private float angularVelocity = 0.5f;
	
	// Datos compartidos
	@Nonnull
	private Vector3d pivotPoint;
	
	@Nonnull
	private Vector3d direction;
	
	// Referencias del árbol
	@Nonnull
	private Ref<EntityStore> rootRef;
	
	@Nonnull
	private List<Ref<EntityStore>> childrenRefs;
	
	// Estado de física
	private boolean isActive = true;
	private boolean isResting = false;
	
	// Detección de colisiones con bloques
	@Nonnull
	private BlockCollisionProvider blockCollisionProvider;
	
	@Nonnull
	private BlockTracker triggerTracker;
	
	private boolean hasCollided = false;
	
	@Nullable
	private Vector3d collisionPoint;
	
	@Nullable
	private Vector3d collisionNormal;
	
	// Almacenar posición anterior del ROOT para calcular movimiento
	@Nullable
	private Vector3d previousRootPosition;
	
	public TreePhysicsProvider(@Nonnull Ref<EntityStore> rootRef, @Nonnull Vector3d pivotPoint, @Nonnull Vector3d direction) {
		this.rootRef = rootRef;
		this.pivotPoint = pivotPoint.clone();
		this.direction = direction.normalize();
		this.childrenRefs = new ArrayList<>();
		this.currentAngle = 0f;
		this.angularVelocity = 0.5f;
		
		// Inicializar collision providers
		this.blockCollisionProvider = new BlockCollisionProvider();
		this.blockCollisionProvider.setRequestedCollisionMaterials(6);
		this.blockCollisionProvider.setReportOverlaps(true);
		this.triggerTracker = new BlockTracker();
	}
	
	/**
	 * Añade una referencia de hijo a este proveedor de física.
	 */
	public void addChild(@Nonnull Ref<EntityStore> childRef) {
		childrenRefs.add(childRef);
	}
	
	/**
	 * Obtiene todas las referencias de hijos.
	 */
	@Nonnull
	public List<Ref<EntityStore>> getChildren() {
		return childrenRefs;
	}
	
	/**
	 * Actualiza la física y posición del ROOT.
	 * Contiene toda la lógica de cálculo de ángulos y rotación.
	 */
	public void updateRoot(float dt, FallingTreeEntityBlock rootEntity, TransformComponent rootTransform, @Nullable World world, @Nullable BoundingBox boundingBox) {
		// Actualizar la física
		updatePhysics(dt);
		
		// Detectar colisiones si el mundo y bounding box están disponibles
		if (world != null && boundingBox != null && isActive) {
			detectCollisions(rootEntity, rootTransform, world, boundingBox);
		}
		
		// Calcular rotación basada en la dirección y el ángulo
		Vector3f rootRotation = calculateRotation(direction, currentAngle);
		
		// Actualizar posición y rotación del ROOT
		rootTransform.setPosition(pivotPoint);
		rootTransform.setRotation(rootRotation);
	}
	
	/**
	 * Actualiza la posición y rotación de un CHILD basándose en el estado actual del ROOT.
	 */
	public void updateChild(FallingTreeEntityBlock childEntity, TransformComponent childTransform, @Nullable World world, @Nullable BoundingBox boundingBox) {
		// Detectar colisiones para este CHILD si el mundo y bounding box están disponibles
		if (world != null && boundingBox != null && isActive) {
			detectCollisions(childEntity, childTransform, world, boundingBox);
		}
		
		// Calcular rotación
		Vector3f rotation = calculateRotation(direction, currentAngle);
		
		// Calcular nueva posición rotada
		Vector3d newChildPos = rotateAroundPivot(
			childEntity.getOffsetFromPivot(),
			pivotPoint,
			direction,
			currentAngle
		);
		
		// Aplicar posición y rotación
		childTransform.setPosition(newChildPos);
		childTransform.setRotation(rotation);
	}
	
	/**
	 * Detecta colisiones con bloques del mundo.
	 * Solo el ROOT detecta colisiones; cuando impacta, todo el árbol se detiene.
	 */
	private void detectCollisions(FallingTreeEntityBlock entity, TransformComponent transform, @Nonnull World world, @Nonnull BoundingBox boundingBox) {
//		// Solo el ROOT detecta colisiones
//		if (!entity.isRoot()) {
//			return;
//		}
		
		hasCollided = false;
		collisionPoint = null;
		collisionNormal = null;
		
		Vector3d currentPos = transform.getPosition();
		// Movimiento pequeño hacia abajo para detectar colisiones
		Vector3d movement = new Vector3d(0, -0.1, 0);
		
		// Realizar raycast de colisiones con bloques
		blockCollisionProvider.cast(
			world,
			boundingBox.getBoundingBox(),
			currentPos,
			movement,
			this,
			triggerTracker,
			1.0 // maxRelativeDistance
		);
		
		// Si hay colisión, detener la caída
		if (hasCollided) {
			isActive = false;
			isResting = true;
			angularVelocity = 0;
		}
	}
	
	/**
	 * Implementación de IBlockCollisionConsumer.onCollision()
	 * Se invoca para cada bloque que colisiona durante el raycast.
	 */
	@Nonnull
	@Override
	public IBlockCollisionConsumer.Result onCollision(
			int blockX,
			int blockY,
			int blockZ,
			@Nonnull Vector3d direction,
			@Nonnull BlockContactData contactData,
			@Nonnull BlockData blockData,
			@Nonnull Box collider
	) {
		BlockMaterial blockMaterial = blockData.getBlockType().getMaterial();
		
		// Solo detectar colisiones con bloques sólidos
		if (blockMaterial == BlockMaterial.Solid && !contactData.isOverlapping()) {
			// Registrar la colisión
			hasCollided = true;
			collisionPoint = new Vector3d(contactData.getCollisionPoint());
			collisionNormal = new Vector3d(contactData.getCollisionNormal());
			
			// Detener búsqueda de más colisiones
			return IBlockCollisionConsumer.Result.STOP;
		}
		
		// Continuar buscando más colisiones
		return IBlockCollisionConsumer.Result.CONTINUE;
	}
	
	@Nonnull
	@Override
	public IBlockCollisionConsumer.Result probeCollisionDamage(
			int blockX, int blockY, int blockZ, Vector3d direction, BlockContactData collisionData, BlockData blockData
	) {
		return IBlockCollisionConsumer.Result.CONTINUE;
	}
	
	@Override
	public void onCollisionDamage(int blockX, int blockY, int blockZ, Vector3d direction, BlockContactData collisionData, BlockData blockData) {
	}
	
	@Nonnull
	@Override
	public IBlockCollisionConsumer.Result onCollisionSliceFinished() {
		return IBlockCollisionConsumer.Result.CONTINUE;
	}
	
	@Override
	public void onCollisionFinished() {
	}
	
	/**
	 * Actualiza la física del árbol completo.
	 * Calcula el nuevo ángulo y velocidad angular basado en la aceleración.
	 * La rotación continúa hasta que hay colisión.
	 */
	private void updatePhysics(float dt) {
		if (!isActive) {
			isResting = true;
			return;
		}
		
		// Aplicar aceleración angular
		float newAngularVel = angularVelocity + ANGULAR_ACCELERATION * dt;
		angularVelocity = newAngularVel;
		
		// Integración: deltaAngle = v*dt + 0.5*a*dt²
		float deltaAngle = angularVelocity * dt + 0.5f * ANGULAR_ACCELERATION * dt * dt;
		float newAngle = currentAngle + deltaAngle;
		
		currentAngle = newAngle;
	}
	
	/**
	 * Rota el offset alrededor del pivote en 3D usando la fórmula de Rodrigues.
	 */
	private Vector3d rotateAroundPivot(Vector3d offset, Vector3d pivot, Vector3d direction, float angle) {
		// Normalizar la dirección
		Vector3d dir = direction.normalize();
		
		// Vector "arriba" inicial (Y+)
		Vector3d upVector = new Vector3d(0, 1, 0);
		
		// Calcular eje de rotación: up × direction
		Vector3d rotationAxis = new Vector3d(
			upVector.y * dir.z - upVector.z * dir.y,
			upVector.z * dir.x - upVector.x * dir.z,
			upVector.x * dir.y - upVector.y * dir.x
		);
		
		// Normalizar el eje de rotación
		double axisLength = rotationAxis.length();
		if (axisLength > 1e-6) {
			rotationAxis.scale(1.0 / axisLength);
		} else {
			rotationAxis = new Vector3d(1, 0, 0);
		}
		
		// Fórmula de Rodrigues: v_rot = v*cos(θ) + (k × v)*sin(θ) + k*(k·v)*(1-cos(θ))
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		double oneMinusCos = 1.0 - cosAngle;
		
		// Calcular k × offset (producto cruz)
		Vector3d crossProduct = new Vector3d(
			rotationAxis.y * offset.z - rotationAxis.z * offset.y,
			rotationAxis.z * offset.x - rotationAxis.x * offset.z,
			rotationAxis.x * offset.y - rotationAxis.y * offset.x
		);
		
		// Calcular k · offset (producto punto)
		double dotProduct = rotationAxis.x * offset.x + rotationAxis.y * offset.y + rotationAxis.z * offset.z;
		
		// Aplicar rotación de Rodrigues
		Vector3d rotatedOffset = new Vector3d(
			offset.x * cosAngle + crossProduct.x * sinAngle + rotationAxis.x * dotProduct * oneMinusCos,
			offset.y * cosAngle + crossProduct.y * sinAngle + rotationAxis.y * dotProduct * oneMinusCos,
			offset.z * cosAngle + crossProduct.z * sinAngle + rotationAxis.z * dotProduct * oneMinusCos
		);
		
		// Retornar posición final: pivot + offset rotado
		return new Vector3d(
			pivot.x + rotatedOffset.x,
			pivot.y + rotatedOffset.y,
			pivot.z + rotatedOffset.z
		);
	}
	
	/**
	 * Calcula la rotación 3D que alinea el modelo con la dirección de caída.
	 */
	private Vector3f calculateRotation(Vector3d direction, float angle) {
		// Normalizar la dirección
		Vector3d dir = direction.normalize();
		
		// Yaw: rotación alrededor del eje Y
		float yaw = (float) Math.atan2(dir.x, dir.z) - (float) Math.PI / 2;
		
		// Pitch: rotación alrededor del eje X
		float pitch = (float) Math.asin(-dir.y);
		
		// Roll: rotación del ángulo de caída
		float roll = -angle;
		
		return new Vector3f(pitch, yaw, roll);
	}
	
	// Getters
	public float getCurrentAngle() {
		return currentAngle;
	}
	
	public float getAngularVelocity() {
		return angularVelocity;
	}
	
	public void setAngularVelocity(float velocity) {
		this.angularVelocity = velocity;
	}
	
	@Nonnull
	public Vector3d getPivotPoint() {
		return pivotPoint;
	}
	
	@Nonnull
	public Vector3d getDirection() {
		return direction;
	}
	
	public boolean isResting() {
		return isResting;
	}
	
	public boolean isActive() {
		return isActive;
	}
	
	public void setActive(boolean active) {
		this.isActive = active;
	}
	
	@Nonnull
	public Ref<EntityStore> getRootRef() {
		return rootRef;
	}
	
	public boolean hasCollided() {
		return hasCollided;
	}
	
	@Nullable
	public Vector3d getCollisionPoint() {
		return collisionPoint;
	}
	
	@Nullable
	public Vector3d getCollisionNormal() {
		return collisionNormal;
	}
	
	/**
	 * Clona el proveedor de física.
	 */
	public TreePhysicsProvider clone() {
		TreePhysicsProvider cloned = new TreePhysicsProvider(rootRef, pivotPoint, direction);
		cloned.currentAngle = this.currentAngle;
		cloned.angularVelocity = this.angularVelocity;
		cloned.isActive = this.isActive;
		cloned.isResting = this.isResting;
		cloned.hasCollided = this.hasCollided;
		cloned.childrenRefs = new ArrayList<>(this.childrenRefs);
		return cloned;
	}
}





