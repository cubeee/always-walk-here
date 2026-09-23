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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Provides;
import javax.inject.Inject;

import joptsimple.internal.Strings;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Menu;
import net.runelite.api.MenuAction;
import net.runelite.api.MenuEntry;
import net.runelite.api.Scene;
import net.runelite.api.Tile;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.PostMenuSort;
import net.runelite.api.events.PreMapLoad;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.HotkeyListener;

import java.util.*;

@Slf4j
@PluginDescriptor(
	name = "Always Walk Here"
)
public class AlwaysWalkHerePlugin extends Plugin {

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private ConfigManager configManager;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private MarkedTileOverlay markedTileOverlay;

	@Inject
	private Gson gson;

	@Inject
	private KeyManager keyManager;

	@Inject
	private AlwaysWalkHereConfig config;

	private final List<WorldPoint> markedTilePoints = new ArrayList<>();

	private boolean showMenuOptions;

	private final HotkeyListener hotkeyListener = new HotkeyListener(() -> config.getKeybind()) {
		@Override
		public void hotkeyPressed() {
			showMenuOptions = true;
		}

		@Override
		public void hotkeyReleased() {
			showMenuOptions = false;
		}
	};

	@Override
	protected void startUp() {
		keyManager.registerKeyListener(hotkeyListener);
		overlayManager.add(markedTileOverlay);
		clientThread.invoke(() -> {
			if (client == null) {
				return;
			}
			loadTiles(client.getTopLevelWorldView().getScene());
		});
	}

	@Override
	protected void shutDown() {
		keyManager.unregisterKeyListener(hotkeyListener);
		overlayManager.remove(markedTileOverlay);
		markedTilePoints.clear();
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onPreMapLoad(PreMapLoad preMapLoad) {
		loadTiles(preMapLoad.getScene());
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onMenuOpened(MenuOpened event) {
		modifyMenu(true);
	}

	@Subscribe
	@SuppressWarnings("unused")
	public void onPostMenuSort(PostMenuSort event) {
		modifyMenu(false);
	}

	private void modifyMenu(boolean allowShowMenuOptions) {
		Menu menu = client.getMenu();
		MenuEntry[] menuEntries = menu.getMenuEntries();
		if (!hasWalkHereOption(menuEntries)) {
			return;
		}

		Tile selectedTile = client.getTopLevelWorldView().getSelectedSceneTile();
		if (selectedTile == null) {
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, selectedTile.getLocalLocation());
		if (worldPoint == null) {
			return;
		}
		boolean isMarked = markedTilePoints.contains(worldPoint);

		if (showMenuOptions && allowShowMenuOptions) {
			menu.createMenuEntry(1)
					.setOption(isMarked ? "Reset always walk here" : "Always walk here")
					.setType(MenuAction.RUNELITE)
					.onClick(e -> toggleTile(selectedTile));
		}

		if (isMarked) {
			MenuEntry[] sortedEntries = menu.getMenuEntries();
			Arrays.sort(sortedEntries, (a, b) -> {
				if (a.getType() == MenuAction.WALK) {
					return 1;
				} else if (b.getType() == MenuAction.WALK) {
					return -1;
				}
				return 0;
			});
			client.getMenu().setMenuEntries(sortedEntries);
		}
	}

	private boolean hasWalkHereOption(MenuEntry... menuEntries) {
		if (menuEntries == null) {
			return false;
		}
		for (MenuEntry menuEntry : menuEntries) {
			if (menuEntry.getType() == MenuAction.WALK) {
				return true;
			}
		}
		return false;
	}

	private void toggleTile(Tile tile) {
		if (tile == null) {
			return;
		}

		WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, tile.getLocalLocation());
		int regionId = worldPoint.getRegionID();

		MarkedTile markedTile = new MarkedTile(regionId, worldPoint.getRegionX(), worldPoint.getRegionY(), worldPoint.getPlane());
		Collection<MarkedTile> markedTiles = loadMarkedTiles(regionId);

		if (markedTiles.contains(markedTile)) {
			markedTilePoints.remove(worldPoint);
			markedTiles.remove(markedTile);
		} else {
			markedTilePoints.add(worldPoint);
			markedTiles.add(markedTile);
		}

		saveMarkedTiles(regionId, markedTiles);
	}

	private void loadTiles(Scene scene) {
		markedTilePoints.clear();
		int[] regionIds = scene.getMapRegions();
		if (regionIds == null || regionIds.length == 0) {
			return;
		}
		WorldView worldView = client.getWorldView(scene.getWorldViewId());
		if (worldView == null) {
			return;
		}
		for (int regionId : regionIds) {
			for (MarkedTile tile : loadMarkedTiles(regionId)) {
				WorldPoint worldPoint = WorldPoint.fromRegion(tile.getRegionId(), tile.getRegionX(), tile.getRegionY(), tile.getZ());
				Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(worldView, worldPoint);
				markedTilePoints.addAll(localWorldPoints);
			}
		}
        log.debug("Loaded marked tiles: {}", markedTilePoints.size());
	}

	private void saveMarkedTiles(int regionId, Collection<MarkedTile> markedTiles) {
		String key = Constants.CONFIG_REGION_PREFIX + regionId;
		if (markedTiles == null || markedTiles.isEmpty()) {
			configManager.unsetConfiguration(Constants.CONFIG_GROUP, key);
			return;
		}
		String json = gson.toJson(markedTiles);
		configManager.setConfiguration(Constants.CONFIG_GROUP, key, json);
        log.debug("Saved marked tiles {}: {}", regionId, json);
	}

	private Collection<MarkedTile> loadMarkedTiles(int regionId) {
		String key = Constants.CONFIG_REGION_PREFIX + regionId;
		String json = configManager.getConfiguration(Constants.CONFIG_GROUP, key);
		if (Strings.isNullOrEmpty(json)) {
			return new ArrayList<>();
		}
		return gson.fromJson(json, new TypeToken<List<MarkedTile>>(){}.getType());
	}

	@Provides
    AlwaysWalkHereConfig provideConfig(ConfigManager configManager) {
		return configManager.getConfig(AlwaysWalkHereConfig.class);
	}

	List<WorldPoint> getMarkedTilePoints() {
		return markedTilePoints;
	}

}
