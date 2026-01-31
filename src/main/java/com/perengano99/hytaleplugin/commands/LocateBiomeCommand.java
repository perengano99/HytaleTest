package com.perengano99.hytaleplugin.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.universe.world.worldgen.IWorldGen;
import com.hypixel.hytale.server.worldgen.biome.Biome;
import com.hypixel.hytale.server.worldgen.chunk.ChunkGenerator;
import com.hypixel.hytale.server.worldgen.chunk.ZoneBiomeResult;
import com.perengano99.hytaleplugin.WorldZones;

import javax.annotation.Nonnull;

public class LocateBiomeCommand extends AbstractPlayerCommand {
	
	RequiredArg<String> biomeArg = withRequiredArg("biome", "Nombre del bioma", ArgTypes.STRING);
	OptionalArg<String> zoneArg = withOptionalArg("zone", "Zona la partida", ArgTypes.STRING);
	
	public LocateBiomeCommand() {
		super("locateBiome", "Busca un bioma especifico!");
	}
	
	@Override
	protected void execute(@Nonnull CommandContext context, @Nonnull Store<EntityStore> store, @Nonnull Ref<EntityStore> ref, @Nonnull PlayerRef playerRef,
	                       @Nonnull World world) {
		IWorldGen worldGen = world.getChunkStore().getGenerator();
		if (worldGen instanceof ChunkGenerator generator) {
			int seed = (int) world.getWorldConfig().getSeed();
			Player player = store.getComponent(ref, Player.getComponentType());
			TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
			String biomeTarget = biomeArg.get(context).toLowerCase();
			// Validación de bioma
			if (!WorldZones.getValidBiomes().contains(biomeTarget)) {
				player.sendMessage(Message.raw("Bioma inválido: '" + biomeTarget + "'. Usa uno de la lista oficial."));
				return;
			}
			// Validación de zona (si está presente)
			if (zoneArg.provided(context)) {
				String zoneTarget = zoneArg.get(context).toLowerCase();
				if (!WorldZones.getValidZones().contains(zoneTarget)) {
					player.sendMessage(Message.raw("Zona inválida: '" + zoneTarget + "'. Usa una zona oficial."));
					return;
				}
			}
			
			// Configuración chingona
			long timeoutMs = 60000; // 2.5s antes de cortar
			long startTime = System.currentTimeMillis();
			int radius = 10240; // Radio de chunks a buscar
			final int STEP = 8; // Saltos de optimización (8 bloques)
			
			// Posición inicial (Jugador)
			int startChunkX = (int) transform.getPosition().getX() >> 5;
			int startChunkZ = (int) transform.getPosition().getZ() >> 5;
			
			// Variables para el espiral
			int x = 0, z = 0, dx = 0, dz = -1;
			int t;
			
			// Loop principal (Espiral)
			for (int i = 0; i < Math.pow(radius * 2, 2); i++) {
				
				// Timeout check
				if (System.currentTimeMillis() - startTime > timeoutMs) {
					player.sendMessage(Message.raw("Se acabó el tiempo compa. No encontré el bioma '" + biomeTarget + "'."));
					return;
				}
				
				int currentChunkX = startChunkX + x;
				int currentChunkZ = startChunkZ + z;
				
				// Loop interno del chunk (usando STEP para ir en chinga)
				for (int bx = 0; bx < 32; bx += STEP) {
					for (int bz = 0; bz < 32; bz += STEP) {
						int globalX = (currentChunkX << 5) + bx;
						int globalZ = (currentChunkZ << 5) + bz;
						
						// Consulta directa al generador (MATEMÁTICAS PURAS, sin cargar chunks)
						ZoneBiomeResult result = generator.getZoneBiomeResultAt(seed, globalX, globalZ);
						String zone = result.getZoneResult().getZone().name().toLowerCase();
						if (zoneArg.provided(context) && !zone.equalsIgnoreCase(zoneArg.get(context).toLowerCase()))
							continue;
						
						if (result.getBiome().getName().toLowerCase().equalsIgnoreCase(biomeTarget)) {
							player.sendMessage(Message.raw("Bioma '" + biomeTarget + "' encontrado en '" + zone + "': [ " + globalX + ", ~, " + globalZ + " ]"));
							return;
						}
					}
				}
				
				// Cálculo del siguiente paso en espiral
				if ((x == z) || ((x < 0) && (x == -z)) || ((x > 0) && (x == 1 - z))) {
					t  = dx;
					dx = -dz;
					dz = t;
				}
				x += dx;
				z += dz;
			}
			
			player.sendMessage(Message.raw("Terminé el radio de búsqueda y no salió nada."));
		}
	}
}
