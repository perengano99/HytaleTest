package com.perengano99.hytaleplugin;

import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChain;
import com.hypixel.hytale.protocol.packets.interaction.SyncInteractionChains;
import com.hypixel.hytale.server.core.auth.PlayerAuthentication;
import com.hypixel.hytale.server.core.io.PacketHandler;
import com.hypixel.hytale.server.core.io.adapter.PacketWatcher;

public class PacketListener implements PacketWatcher {
	private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
	
    @Override
    public void accept(PacketHandler packetHandler, Packet packet) {
        if (packet.getId() != 290) {
            return;
        }
        SyncInteractionChains interactionChains = (SyncInteractionChains) packet;
        SyncInteractionChain[] updates = interactionChains.updates;

        for (SyncInteractionChain item : updates) {
            PlayerAuthentication playerAuthentication = packetHandler.getAuth();
            String uuid = playerAuthentication.getUuid().toString();
            InteractionType interactionType = item.interactionType;
            if(interactionType == InteractionType.Use){
                // code here
	            LOGGER.atInfo().log("Se utilizo la F?");
            }
        }
    }
}