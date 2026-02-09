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
	private static final float GRAVITY = 20.0f; // Gravedad ajustada para objetos pesados (árboles)
	private static final float AIR_RESISTANCE = 0.995f; // Fricción del aire muy leve
	private static final float TORQUE_FACTOR = 0.05f; // Factor para controlar velocidad de rotación (más bajo = más lento)
	
	// Estado del árbol
	private float currentAngle = 0f;
	private float angularVelocity = 0.0f; // Empieza quieto, la gravedad lo moverá
	private float massFactor; // Distancia al centro de masa
	private float verticalVelocity = 0.0f; // Velocidad de caída del pivote
	
	// Sistema de APOYO
	private boolean isSupported = false; // ¿El árbol está apoyado en un bloque sólido?
	@Nullable private Vector3d supportContactPoint = null; // Punto de contacto de apoyo
	@Nullable private Vector3d supportContactNormal = null; // Normal del bloque de apoyo
	
	// Datos compartidos
	@Nonnull private Vector3d pivotPoint;
	@Nonnull private Vector3d direction;
	
	// Referencias
	@Nonnull private Ref<EntityStore> rootRef;
	@Nonnull private List<Ref<EntityStore>> childrenRefs;
	
	// Estado
	private boolean isActive = true;
	private boolean isResting = false;
	
	// Colisiones
	@Nonnull private BlockCollisionProvider blockCollisionProvider;
	@Nonnull private BlockTracker triggerTracker;
	
	// Estado de colisión TEMPORAL por entidad (se resetea en cada tick)
	private boolean tempHasCollided = false;
	@Nullable private Vector3d tempCollisionPoint;
	@Nullable private Vector3d tempCollisionNormal;
	private double tempCollisionStart = 1.0;
	
	// Contador de bloques que colisionaron este tick
	private int collidingBlocksCount = 0;
	private int thisTickSupportingBlocks = 0; // Contador de apoyo detectado ESTE tick
	private int processedChildrenThisTick = 0; // Cuenta cuántos hijos se han procesado
	private static final int MIN_COLLISIONS_TO_REST = 3; // Mínimo de bloques colisionando para considerar reposo
	
	
	public TreePhysicsProvider(@Nonnull Ref<EntityStore> rootRef, @Nonnull Vector3d pivotPoint, @Nonnull Vector3d direction, @Nonnull Vector3d centerOfMassOffset) {
		this.rootRef      = rootRef;
		this.pivotPoint   = pivotPoint.clone();
		this.direction    = direction.normalize();
		this.childrenRefs = new ArrayList<>();
		
		// Configuración inicial
		this.currentAngle    = 0.05f; // Le damos un empujoncito inicial pequeño (aprox 3 grados) para que la gravedad actúe
		this.angularVelocity = 0.0f;
		
		// Calcular "brazo de palanca" (Distancia del pivote al centro de masa)
		// Simplificación: Usamos la magnitud del vector offset
		this.massFactor = (float) centerOfMassOffset.length();
		
		this.blockCollisionProvider = new BlockCollisionProvider();
		this.blockCollisionProvider.setRequestedCollisionMaterials(6);
		this.blockCollisionProvider.setReportOverlaps(true);
		this.triggerTracker = new BlockTracker();
	}
	
	public void addChild(@Nonnull Ref<EntityStore> childRef) {
		childrenRefs.add(childRef);
	}
	
	@Nonnull
	public List<Ref<EntityStore>> getChildren() {
		return childrenRefs;
	}
	
	public void updateRoot(float dt, FallingTreeEntityBlock rootEntity, TransformComponent rootTransform, @Nullable World world, @Nullable BoundingBox boundingBox) {
		// Resetear contadores de colisión THIS TICK
		collidingBlocksCount = 0;
		processedChildrenThisTick = 0;
		thisTickSupportingBlocks = 0;  // Contar apoyo ESTE tick
		
		// PRIMERO: Detectar colisiones del ROOT
		if (world != null && boundingBox != null && isActive) {
			boolean rootCollided = detectCollisions(rootEntity, rootTransform, world, boundingBox);
			if (rootCollided) {
				collidingBlocksCount++;
				
				// Si colisiona, verificar si es un bloque de apoyo (normal hacia arriba)
				if (tempCollisionNormal != null && tempCollisionNormal.y > 0.7) {
					thisTickSupportingBlocks++;
					supportContactPoint = tempCollisionPoint != null ? new Vector3d(tempCollisionPoint) : null;
					supportContactNormal = new Vector3d(tempCollisionNormal);
					LOGGER.atInfo().log("[ROOT COLLISION] APOYO DETECTADO - Normal: (%f, %f, %f)",
						supportContactNormal.x, supportContactNormal.y, supportContactNormal.z);
				}
			}
		}
		
		// SEGUNDO: Establecer estado de apoyo basado en apoyo detectado THIS TICK
		isSupported = thisTickSupportingBlocks > 0;
		
		// TERCERO: Actualizar física
		updatePhysics(dt);
		
		Vector3f rootRotation = calculateRotation(direction, currentAngle);
		rootTransform.setPosition(pivotPoint);
		rootTransform.setRotation(rootRotation);
	}
	
	public void updateChild(FallingTreeEntityBlock childEntity, TransformComponent childTransform, @Nullable World world, @Nullable BoundingBox boundingBox) {
		// Incrementar contador de hijos procesados
		processedChildrenThisTick++;
		
		// Calcular nueva posición rotada
		Vector3f rotation = calculateRotation(direction, currentAngle);
		Vector3d newChildPos = rotateAroundPivot(childEntity.getOffsetFromPivot(), pivotPoint, direction, currentAngle);
		
		// Detectar colisión ANTES de aplicar la posición
		if (world != null && boundingBox != null && isActive) {
			boolean childCollided = detectCollisions(childEntity, childTransform, world, boundingBox);
			if (childCollided) {
				collidingBlocksCount++;
				
				// Verificar si es un bloque de apoyo (normal apunta hacia arriba)
				if (tempCollisionNormal != null && tempCollisionNormal.y > 0.7) {
					thisTickSupportingBlocks++;  // Usar el contador THIS TICK
					
					// Guardar el punto de contacto más bajo (mejor apoyo)
					if (supportContactPoint == null && tempCollisionPoint != null) {
						supportContactPoint = new Vector3d(tempCollisionPoint);
					} else if (supportContactPoint != null && tempCollisionPoint != null && tempCollisionPoint.y > supportContactPoint.y) {
						supportContactPoint = new Vector3d(tempCollisionPoint);
					}
					supportContactNormal = new Vector3d(tempCollisionNormal);
					
					LOGGER.atInfo().log("[CHILD COLLISION] APOYO DETECTADO - thisTickSupports=%d",
						thisTickSupportingBlocks);
				}
				
				// Ajustar posición para no atravesar
				if (tempCollisionNormal != null) {
					newChildPos.add(
							tempCollisionNormal.x * 0.1,
							tempCollisionNormal.y * 0.1,
							tempCollisionNormal.z * 0.1
					);
				}
			}
		}
		
		// Aplicar transformaciones
		childTransform.setPosition(newChildPos);
		childTransform.setRotation(rotation);
	}
	
	/**
	 * Detecta colisiones para una entidad individual.
	 *
	 * @return true si hubo colisión, false en caso contrario
	 */
	private boolean detectCollisions(FallingTreeEntityBlock entity, TransformComponent transform, @Nonnull World world, @Nonnull BoundingBox boundingBox) {
		// Resetear estado temporal de colisión
		tempHasCollided     = false;
		tempCollisionPoint  = null;
		tempCollisionNormal = null;
		tempCollisionStart  = 1.0;
		
		Vector3d currentPos = transform.getPosition();
		
		// Calcular movimiento predicho para el próximo tick
		// Usamos un vector de movimiento CONSTANTE hacia abajo para siempre detectar el suelo
		// Más componentes horizontales basados en la dirección de caída y rotación actual
		double horizontalSpeed = Math.max(Math.abs(angularVelocity) * 2.0, 0.5); // Mínimo 0.5
		double verticalSpeed = Math.max(verticalVelocity, 1.0); // Mínimo 1.0 para siempre buscar hacia abajo
		
		Vector3d movement = new Vector3d(
				direction.x * horizontalSpeed,  // Movimiento horizontal en dirección de caída
				-verticalSpeed,                 // Siempre buscar hacia abajo (mínimo 1 bloque)
				direction.z * horizontalSpeed   // Movimiento horizontal en dirección de caída
		);
		
		// Log para debug: verificar que se está ejecutando
		LOGGER.atInfo().log("Detectando colisión - Pos: (%f, %f, %f), Movement: (%f, %f, %f)",
				currentPos.x, currentPos.y, currentPos.z,
				movement.x, movement.y, movement.z);
		
		// Realizar el cast de colisión (SIEMPRE, sin optimización que podría fallar)
		blockCollisionProvider.cast(world, boundingBox.getBoundingBox(), currentPos, movement, this, triggerTracker, 1.0);
		
		LOGGER.atInfo().log("Resultado detección: hasCollided=%b", tempHasCollided);
		
		return tempHasCollided;
	}
	

	/**
	 * Implementación de IBlockCollisionConsumer.onCollision()
	 * Se invoca para cada bloque que colisiona durante el raycast.
	 */
	@Nonnull
	@Override
	public IBlockCollisionConsumer.Result onCollision(int blockX, int blockY, int blockZ, @Nonnull Vector3d direction, @Nonnull BlockContactData contactData,
	                                                  @Nonnull BlockData blockData, @Nonnull Box collider) {
		// Verificar que el bloque sea sólido
		if (blockData.getBlockType() == null) {
			return IBlockCollisionConsumer.Result.CONTINUE;
		}
		
		BlockMaterial blockMaterial = blockData.getBlockType().getMaterial();
		
		// Solo considerar colisiones con bloques sólidos
		if (blockMaterial == BlockMaterial.Solid) {
			// Verificar que no sea un overlap (ya dentro del bloque)
			if (!contactData.isOverlapping()) {
				// Verificar que estamos acercándonos al bloque, no alejándonos
				double surfaceAlignment = direction.dot(contactData.getCollisionNormal());
				if (surfaceAlignment < 0.0) {
					// Registrar la colisión más cercana
					double collisionStart = contactData.getCollisionStart();
					if (collisionStart < tempCollisionStart) {
						tempHasCollided     = true;
						tempCollisionPoint  = new Vector3d(contactData.getCollisionPoint());
						tempCollisionNormal = new Vector3d(contactData.getCollisionNormal());
						tempCollisionStart  = collisionStart;
					}
				}
			}
		}
		
		// Continuar buscando más colisiones para encontrar la más cercana
		return IBlockCollisionConsumer.Result.CONTINUE;
	}
	
	@Nonnull
	@Override
	public IBlockCollisionConsumer.Result probeCollisionDamage(int blockX, int blockY, int blockZ, Vector3d direction, BlockContactData collisionData, BlockData blockData) {
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
	 * Aplica aceleración gravitacional real y torque para rotación.
	 */
	private void updatePhysics(float dt) {
		if (!isActive) {
			isResting = true;
			return;
		}
		
		// 1. GRAVEDAD REAL: Aceleración constante hacia abajo
		// v = v0 + a*t  (la velocidad aumenta con el tiempo)
		verticalVelocity += GRAVITY * dt;
		
		// Aplicar caída al pivote (desplazamiento con aceleración)
		pivotPoint.y -= verticalVelocity * dt;
		
		// 2. Calcular Torque (Fuerza de giro) con factor de control
		// Torque = Gravedad * Distancia_CoM * sin(Angulo) * TORQUE_FACTOR
		// El TORQUE_FACTOR controla qué tan rápido rota
		float torque = (float) (GRAVITY * massFactor * Math.sin(currentAngle) * TORQUE_FACTOR);
		
		// 3. Actualizar Velocidad Angular (más controlada)
		angularVelocity += torque * dt;
		
		// 4. Aplicar resistencia del aire
		angularVelocity *= AIR_RESISTANCE;
		
		// 5. Actualizar Ángulo (sin límites artificiales)
		currentAngle += angularVelocity * dt;
	}
	
	/**
	 * Rota el offset alrededor del pivote en 3D usando la fórmula de Rodrigues.
	 */
	private Vector3d rotateAroundPivot(Vector3d offset, Vector3d pivot, Vector3d direction, float angle) {
		// ... (Mismo código de Rodrigues que tenías) ...
		Vector3d dir = direction.normalize();
		Vector3d upVector = new Vector3d(0, 1, 0);
		Vector3d rotationAxis = new Vector3d(upVector.y * dir.z - upVector.z * dir.y, upVector.z * dir.x - upVector.x * dir.z, upVector.x * dir.y - upVector.y * dir.x);
		double axisLength = rotationAxis.length();
		if (axisLength > 1e-6) {
			rotationAxis.scale(1.0 / axisLength);
		} else {
			rotationAxis = new Vector3d(1, 0, 0);
		}
		double cosAngle = Math.cos(angle);
		double sinAngle = Math.sin(angle);
		double oneMinusCos = 1.0 - cosAngle;
		Vector3d crossProduct = new Vector3d(rotationAxis.y * offset.z - rotationAxis.z * offset.y, rotationAxis.z * offset.x - rotationAxis.x * offset.z,
				rotationAxis.x * offset.y - rotationAxis.y * offset.x);
		double dotProduct = rotationAxis.x * offset.x + rotationAxis.y * offset.y + rotationAxis.z * offset.z;
		Vector3d rotatedOffset = new Vector3d(offset.x * cosAngle + crossProduct.x * sinAngle + rotationAxis.x * dotProduct * oneMinusCos,
				offset.y * cosAngle + crossProduct.y * sinAngle + rotationAxis.y * dotProduct * oneMinusCos,
				offset.z * cosAngle + crossProduct.z * sinAngle + rotationAxis.z * dotProduct * oneMinusCos);
		return new Vector3d(pivot.x + rotatedOffset.x, pivot.y + rotatedOffset.y, pivot.z + rotatedOffset.z);
	}
	
	/**
	 * Calcula la rotación 3D que alinea el modelo con la dirección de caída.
	 */
	private Vector3f calculateRotation(Vector3d direction, float angle) {
		Vector3d dir = direction.normalize();
		float yaw = (float) Math.atan2(dir.x, dir.z) - (float) Math.PI / 2;
		float pitch = (float) Math.asin(-dir.y);
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
	
	public int getCollidingBlocksCount() {
		return collidingBlocksCount;
	}
	
	/**
	 * Clona el proveedor de física.
	 */
	public TreePhysicsProvider clone() {
		//		TreePhysicsProvider cloned = new TreePhysicsProvider(rootRef, pivotPoint, direction);
		//		cloned.currentAngle    = this.currentAngle;
		//		cloned.angularVelocity = this.angularVelocity;
		//		cloned.isActive        = this.isActive;
		//		cloned.isResting       = this.isResting;
		//		cloned.hasCollided     = this.hasCollided;
		//		cloned.childrenRefs    = new ArrayList<>(this.childrenRefs);
		return null;
	}
}





