package com.github.cubeee.alwayswalkhere;

import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Keybind;

import java.awt.Color;

@ConfigGroup(Constants.CONFIG_GROUP)
public interface AlwaysWalkHereConfig extends Config {
	@ConfigItem(
		keyName = "keybind",
		name = "Keybind",
		description = "Hold this key to get the menu action on tiles"
	)
	default Keybind getKeybind() {
		return Keybind.NOT_SET;
	}

	@ConfigItem(
		keyName = "tileColor",
		name = "Tile color",
		description = "Color used for marked tiles"
	)
	@Alpha
	default Color getTileColor() {
		return new Color(0, 112, 0, 50);
	}

	@ConfigItem(
		keyName = "borderWidth",
		name = "Border width",
		description = "Marked tile border width"
	)
	@Alpha
	default double getBorderWidth() {
		return 2;
	}
}
