package com.BrimstoneElvenChestTracker;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Range;

@ConfigGroup(BrimstoneElvenChestTrackerConfig.GROUP)
public interface BrimstoneElvenChestTrackerConfig extends Config
{
	/*
	 * This group name is used for RuneLite's visible plugin configuration
	 * and for the profile-specific statistics saved by the plugin.
	 */
	String GROUP = "brimstoneelvenchesttracker";

	@ConfigSection(
		name = "Display Settings",
		description = "Choose which chest statistics are shown on the overlay.",
		position = 0
	)
	String displaySection = "displaySection";

	@ConfigSection(
		name = "Setup Settings",
		description = "Enter previously known chest counts for your latest collection log items and duplicates.",
		position = 1
	)
	String setupSection = "setupSection";

	@ConfigItem(
		keyName = "showTotalOpened",
		name = "Total Opened",
		description = "Show the total number of times the current chest has been opened.",
		section = displaySection,
		position = 0
	)
	default boolean showTotalOpened()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLastClog",
		name = "Last Clog",
		description = "Show the chest count where the most recent new collection log item was obtained.",
		section = displaySection,
		position = 1
	)
	default boolean showLastClog()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLastDupe",
		name = "Last Dupe",
		description = "Show the chest count where the most recent duplicate tracked item was obtained.",
		section = displaySection,
		position = 2
	)
	default boolean showLastDupe()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showLongestDry",
		name = "Longest Dry",
		description = "Show the largest number of chests between tracked armour drops.",
		section = displaySection,
		position = 3
	)
	default boolean showLongestDry()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showMostSpooned",
		name = "Most Spooned",
		description = "Show the smallest number of chests between tracked armour drops.",
		section = displaySection,
		position = 4
	)
	default boolean showMostSpooned()
	{
		return true;
	}

	/*
	 * BRIMSTONE SETUP
	 *
	 * The user enters the chest count and then checks the matching Apply box.
	 * The plugin processes it once and immediately unchecks the Apply box.
	 *
	 * Entering 0 and applying it intentionally clears that individual value.
	 */

	@Range(min = 0)
	@ConfigItem(
		keyName = "baselineBrimstoneLastClog",
		name = "Brimstone Last Clog",
		description = "Chest count where your most recent new Brimstone collection log item was obtained. Enter 0 to clear it.",
		section = setupSection,
		position = 0
	)
	default int baselineBrimstoneLastClog()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "applyBrimstoneLastClog",
		name = "Apply Brimstone Last Clog",
		description = "Apply the Brimstone Last Clog value above.",
		section = setupSection,
		position = 1
	)
	default boolean applyBrimstoneLastClog()
	{
		return false;
	}

	@Range(min = 0)
	@ConfigItem(
		keyName = "baselineBrimstoneLastDupe",
		name = "Brimstone Last Dupe",
		description = "Chest count where your most recent duplicate Brimstone armour item was obtained. Enter 0 to clear it.",
		section = setupSection,
		position = 2
	)
	default int baselineBrimstoneLastDupe()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "applyBrimstoneLastDupe",
		name = "Apply Brimstone Last Dupe",
		description = "Apply the Brimstone Last Dupe value above.",
		section = setupSection,
		position = 3
	)
	default boolean applyBrimstoneLastDupe()
	{
		return false;
	}

	/*
	 * ELVEN SETUP
	 */

	@Range(min = 0)
	@ConfigItem(
		keyName = "baselineElvenLastClog",
		name = "Elven Last Clog",
		description = "Chest count where your most recent new Elven collection log item was obtained. Enter 0 to clear it.",
		section = setupSection,
		position = 4
	)
	default int baselineElvenLastClog()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "applyElvenLastClog",
		name = "Apply Elven Last Clog",
		description = "Apply the Elven Last Clog value above.",
		section = setupSection,
		position = 5
	)
	default boolean applyElvenLastClog()
	{
		return false;
	}

	@Range(min = 0)
	@ConfigItem(
		keyName = "baselineElvenLastDupe",
		name = "Elven Last Dupe",
		description = "Chest count where your most recent duplicate Dragonstone armour item was obtained. Enter 0 to clear it.",
		section = setupSection,
		position = 6
	)
	default int baselineElvenLastDupe()
	{
		return 0;
	}

	@ConfigItem(
		keyName = "applyElvenLastDupe",
		name = "Apply Elven Last Dupe",
		description = "Apply the Elven Last Dupe value above.",
		section = setupSection,
		position = 7
	)
	default boolean applyElvenLastDupe()
	{
		return false;
	}

	// =====================================================================
	// TESTING ONLY - COMMENTED OUT FOR NORMAL USE
	// =====================================================================
	//
	// Uncomment this section and the matching ConfigChanged cases/methods in
	// BrimstoneElvenChestTrackerPlugin.java when simulated events are needed.
	//
//	 @ConfigSection(
//	 	name = "Testing - Brimstone",
//	 	description = "Testing controls for the Brimstone Chest.",
//	 	position = 2,
//	 	closedByDefault = true
//	 )
//	 String brimstoneTestingSection = "brimstoneTestingSection";
//
//	 @ConfigSection(
//	 	name = "Testing - Elven",
//	 	description = "Testing controls for the Elven Crystal Chest.",
//	 	position = 3,
//	 	closedByDefault = true
//	 )
//	 String elvenTestingSection = "elvenTestingSection";
//
//	 @ConfigItem(
//	 	keyName = "simulateBrimstoneOpen",
//	 	name = "Simulate Chest Open",
//	 	description = "Add one to the Brimstone Chest total.",
//	 	section = brimstoneTestingSection,
//	 	position = 0
//	 )
//	 default boolean simulateBrimstoneOpen()
//	 {
//	 	return false;
//	 }
//
//	 @ConfigItem(
//	 	keyName = "simulateBrimstoneClog",
//	 	name = "Simulate New Clog",
//	 	description = "Add one Brimstone opening and record a new clog.",
//	 	section = brimstoneTestingSection,
//	 	position = 1
//	 )
//	 default boolean simulateBrimstoneClog()
//	 {
//	 	return false;
//	 }
//
//	 @ConfigItem(
//	 	keyName = "simulateBrimstoneDupe",
//	 	name = "Simulate New Dupe",
//	 	description = "Add one Brimstone opening and record a duplicate.",
//	 	section = brimstoneTestingSection,
//	 	position = 2
//	 )
//	 default boolean simulateBrimstoneDupe()
//	 {
//	 	return false;
//	 }
//
//	 @ConfigItem(
//	 	keyName = "simulateElvenOpen",
//	 	name = "Simulate Chest Open",
//	 	description = "Add one to the Elven Crystal Chest total.",
//	 	section = elvenTestingSection,
//	 	position = 0
//	 )
//	 default boolean simulateElvenOpen()
//	 {
//	 	return false;
//	 }
//
//	 @ConfigItem(
//	 	keyName = "simulateElvenClog",
//	 	name = "Simulate New Clog",
//	 	description = "Add one Elven opening and record a new clog.",
//	 	section = elvenTestingSection,
//	 	position = 1
//	 )
//	 default boolean simulateElvenClog()
//	 {
//	 	return false;
//	 }
//
//	 @ConfigItem(
//	 	keyName = "simulateElvenDupe",
//	 	name = "Simulate New Dupe",
//	 	description = "Add one Elven opening and record a duplicate.",
//	 	section = elvenTestingSection,
//	 	position = 2
//	 )
//	 default boolean simulateElvenDupe()
//	 {
//	 	return false;
//	 }
}
