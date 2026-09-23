package com.github.cubeee.alwayswalkhere;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AlwaysWalkHerePluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AlwaysWalkHerePlugin.class);
		RuneLite.main(args);
	}
}
