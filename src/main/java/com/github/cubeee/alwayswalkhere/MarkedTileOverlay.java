/*
 * Copyright (c) 2026, cubeee <https://github.com/cubeee>
 * Copyright (c) 2026, WhatATopic <https://github.com/WhatATopic>
 * Copyright (c) 2018, TheLonelyDev <https://github.com/TheLonelyDev>
 * Copyright (c) 2018, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.github.cubeee.alwayswalkhere;

import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.*;

public class MarkedTileOverlay extends Overlay {

    private final AlwaysWalkHerePlugin plugin;
    private final AlwaysWalkHereConfig config;
    private final Client client;

    @Inject
    private MarkedTileOverlay(AlwaysWalkHerePlugin plugin, AlwaysWalkHereConfig config, Client client) {
        this.plugin = plugin;
        this.config = config;
        this.client = client;

        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> markedTiles = plugin.getMarkedTilePoints();
        if (markedTiles.isEmpty()) {
            return null;
        }

        Player player = client.getLocalPlayer();
        if (player == null) {
            return null;
        }
        WorldPoint playerLocation = player.getWorldLocation();
        if (playerLocation == null) {
            return null;
        }

        for (WorldPoint worldPoint : markedTiles) {
            if (worldPoint.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }
            drawTile(graphics, worldPoint, playerLocation);
        }

        return null;
    }

    private void drawTile(Graphics2D graphics, WorldPoint worldPoint, WorldPoint playerLocation) {
        if (worldPoint.distanceTo(playerLocation) >= Constants.MAX_DRAW_DISTANCE) {
            return;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(client, worldPoint);
        if (localPoint == null) {
            return;
        }

        Polygon polygon = Perspective.getCanvasTilePoly(client, localPoint);
        if (polygon != null) {
            OverlayUtil.renderPolygon(
                graphics,
                polygon,
                config.getTileColor(),
                config.getTileColor(),
                new BasicStroke((float) config.getBorderWidth())
            );
        }
    }
}
