package com.corgifeatures;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

/**
 * Launches a real RuneLite client with the plugin loaded. Run this class's
 * main method to test the plugin in-game.
 */
public class CorgiFeaturesPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(CorgiFeaturesPlugin.class);
		RuneLite.main(args);
	}
}
