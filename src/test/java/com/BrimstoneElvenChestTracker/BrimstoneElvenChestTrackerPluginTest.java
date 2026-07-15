package com.BrimstoneElvenChestTracker;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class BrimstoneElvenChestTrackerPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(BrimstoneElvenChestTrackerPlugin.class);
		RuneLite.main(args);
	}
}
