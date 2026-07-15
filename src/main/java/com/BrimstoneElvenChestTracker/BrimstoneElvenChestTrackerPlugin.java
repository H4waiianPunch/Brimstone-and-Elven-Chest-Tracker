package com.BrimstoneElvenChestTracker;

import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.events.OverlayMenuClicked;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;

@Slf4j
@PluginDescriptor(
	name = "Brimstone and Elven Chest Tracker",
	description = "Tracks Brimstone and Elven Crystal Chest counts and collection log armour drops.",
	tags = {
		"brimstone",
		"elven",
		"crystal",
		"chest",
		"collection log",
		"clog"
	}
)
public class BrimstoneElvenChestTrackerPlugin extends Plugin
{
	private static final String CONFIG_GROUP =
		BrimstoneElvenChestTrackerConfig.GROUP;

	private static final int BRIMSTONE_CHEST_REGION = 5179;
	private static final int ELVEN_CHEST_REGION = 13151;

	/*
	 * RuneScape's player inventory container ID.
	 */
	private static final int INVENTORY_CONTAINER_ID = 93;

	/*
	 * Inventory changes and chat messages may arrive in slightly different
	 * orders. The plugin waits this many ticks before classifying a reward.
	 */
	private static final int EVENT_SETTLE_TICKS = 2;

	private static final Pattern BRIMSTONE_COUNT_PATTERN = Pattern.compile(
		"You have opened the Brimstone chest ([\\d,]+) times\\.?",
		Pattern.CASE_INSENSITIVE
	);

	private static final Pattern ELVEN_COUNT_PATTERN = Pattern.compile(
		"You have opened the crystal chest ([\\d,]+) times\\.?",
		Pattern.CASE_INSENSITIVE
	);

	private static final Pattern COLLECTION_LOG_PATTERN = Pattern.compile(
		"New item added to your collection log: (.+)",
		Pattern.CASE_INSENSITIVE
	);

	/*
	 * Item IDs for the ten tracked collection log items.
	 */
	private static final int MYSTIC_HAT_DUSK = 23047;
	private static final int MYSTIC_ROBE_TOP_DUSK = 23050;
	private static final int MYSTIC_ROBE_BOTTOM_DUSK = 23053;
	private static final int MYSTIC_GLOVES_DUSK = 23056;
	private static final int MYSTIC_BOOTS_DUSK = 23059;

	private static final int DRAGONSTONE_FULL_HELM = 24034;
	private static final int DRAGONSTONE_PLATEBODY = 24037;
	private static final int DRAGONSTONE_PLATELEGS = 24040;
	private static final int DRAGONSTONE_BOOTS = 24043;
	private static final int DRAGONSTONE_GAUNTLETS = 24046;

	private static final Map<Integer, TrackedItem> TRACKED_ITEMS_BY_ID =
		new HashMap<>();

	private static final Map<String, TrackedItem> TRACKED_ITEMS_BY_NAME =
		new HashMap<>();

	static
	{
		registerTrackedItem(
			MYSTIC_HAT_DUSK,
			"Mystic hat (dusk)",
			ChestType.BRIMSTONE
		);

		registerTrackedItem(
			MYSTIC_ROBE_TOP_DUSK,
			"Mystic robe top (dusk)",
			ChestType.BRIMSTONE
		);

		registerTrackedItem(
			MYSTIC_ROBE_BOTTOM_DUSK,
			"Mystic robe bottom (dusk)",
			ChestType.BRIMSTONE
		);

		registerTrackedItem(
			MYSTIC_GLOVES_DUSK,
			"Mystic gloves (dusk)",
			ChestType.BRIMSTONE
		);

		registerTrackedItem(
			MYSTIC_BOOTS_DUSK,
			"Mystic boots (dusk)",
			ChestType.BRIMSTONE
		);

		registerTrackedItem(
			DRAGONSTONE_FULL_HELM,
			"Dragonstone full helm",
			ChestType.ELVEN
		);

		registerTrackedItem(
			DRAGONSTONE_PLATEBODY,
			"Dragonstone platebody",
			ChestType.ELVEN
		);

		registerTrackedItem(
			DRAGONSTONE_PLATELEGS,
			"Dragonstone platelegs",
			ChestType.ELVEN
		);

		registerTrackedItem(
			DRAGONSTONE_GAUNTLETS,
			"Dragonstone gauntlets",
			ChestType.ELVEN
		);

		registerTrackedItem(
			DRAGONSTONE_BOOTS,
			"Dragonstone boots",
			ChestType.ELVEN
		);
	}

	@Inject
	private Client client;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private BrimstoneElvenChestTrackerOverlay overlay;

	@Inject
	private ConfigManager configManager;

	@Inject
	private BrimstoneElvenChestTrackerConfig config;

	/*
	 * Each chest has its own independently saved statistics.
	 */
	private final ChestStats brimstoneStats = new ChestStats();
	private final ChestStats elvenStats = new ChestStats();

	private ChestType activeChest;
	private boolean overlayAdded;
	private int gameTickCounter;

	private Map<Integer, Integer> previousTrackedInventory = new HashMap<>();
	private boolean inventorySnapshotInitialized;

	private final List<PendingInventoryDrop> pendingInventoryDrops =
		new ArrayList<>();

	private final List<PendingClogMessage> pendingClogMessages =
		new ArrayList<>();

	@Override
	protected void startUp()
	{
		loadProfileData();
		resetRuntimeTracking();
	}

	@Override
	protected void shutDown()
	{
		removeOverlay();

		activeChest = null;
		previousTrackedInventory.clear();
		pendingInventoryDrops.clear();
		pendingClogMessages.clear();
		inventorySnapshotInitialized = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			/*
			 * Reload the values belonging to the currently logged-in
			 * RuneScape profile.
			 */
			loadProfileData();

			inventorySnapshotInitialized = false;
			previousTrackedInventory.clear();
			pendingInventoryDrops.clear();
			pendingClogMessages.clear();
		}
		else if (
			event.getGameState() == GameState.LOGIN_SCREEN ||
			event.getGameState() == GameState.HOPPING
		)
		{
			removeOverlay();
			activeChest = null;
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		gameTickCounter++;

		processSettledDropEvents();
		initializeInventorySnapshotIfNeeded();
		updateOverlayVisibility();
	}

	/**
	 * Handles the one-use Apply checkboxes in Setup Settings.
	 *
	 * After a value is applied, its checkbox is immediately changed back to
	 * false so it behaves like a button.
	 */
	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (!CONFIG_GROUP.equals(event.getGroup()))
		{
			return;
		}

		if (!"true".equalsIgnoreCase(event.getNewValue()))
		{
			return;
		}

		String key = event.getKey();

		switch (key)
		{
			case "applyBrimstoneLastClog":
				applyLastClogBaseline(
					ChestType.BRIMSTONE,
					config.baselineBrimstoneLastClog()
				);
				resetOneUseConfigControl(key);
				break;

			case "applyBrimstoneLastDupe":
				applyLastDupeBaseline(
					ChestType.BRIMSTONE,
					config.baselineBrimstoneLastDupe()
				);
				resetOneUseConfigControl(key);
				break;

			case "applyElvenLastClog":
				applyLastClogBaseline(
					ChestType.ELVEN,
					config.baselineElvenLastClog()
				);
				resetOneUseConfigControl(key);
				break;

			case "applyElvenLastDupe":
				applyLastDupeBaseline(
					ChestType.ELVEN,
					config.baselineElvenLastDupe()
				);
				resetOneUseConfigControl(key);
				break;

			// =============================================================
			// TESTING ONLY - COMMENTED OUT FOR NORMAL USE
			// =============================================================
			//
			// Uncomment these cases together with the testing ConfigItems in
			// BrimstoneElvenChestTrackerConfig.java and the testing methods
			// lower in this class.
			//
//			 case "simulateBrimstoneOpen":
//			 	simulateChestOpen(ChestType.BRIMSTONE);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
//			 case "simulateBrimstoneClog":
//			 	simulateTrackedDrop(ChestType.BRIMSTONE, true);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
//			 case "simulateBrimstoneDupe":
//			 	simulateTrackedDrop(ChestType.BRIMSTONE, false);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
//			 case "simulateElvenOpen":
//			 	simulateChestOpen(ChestType.ELVEN);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
//			 case "simulateElvenClog":
//			 	simulateTrackedDrop(ChestType.ELVEN, true);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
//			 case "simulateElvenDupe":
//			 	simulateTrackedDrop(ChestType.ELVEN, false);
//			 	resetOneUseConfigControl(key);
//			 	break;
//
			default:
				break;
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		String cleanMessage = Text.removeTags(event.getMessage()).trim();

		Matcher brimstoneMatcher =
			BRIMSTONE_COUNT_PATTERN.matcher(cleanMessage);

		if (brimstoneMatcher.find())
		{
			updateChestCount(
				ChestType.BRIMSTONE,
				brimstoneMatcher.group(1)
			);

			return;
		}

		Matcher elvenMatcher =
			ELVEN_COUNT_PATTERN.matcher(cleanMessage);

		if (elvenMatcher.find())
		{
			updateChestCount(
				ChestType.ELVEN,
				elvenMatcher.group(1)
			);

			return;
		}

		Matcher clogMatcher =
			COLLECTION_LOG_PATTERN.matcher(cleanMessage);

		if (!clogMatcher.find())
		{
			return;
		}

		String normalizedItemName =
			normalizeItemName(clogMatcher.group(1));

		TrackedItem trackedItem =
			TRACKED_ITEMS_BY_NAME.get(normalizedItemName);

		if (trackedItem == null)
		{
			return;
		}

		pendingClogMessages.add(
			new PendingClogMessage(
				trackedItem.chestType,
				trackedItem.normalizedName,
				gameTickCounter
			)
		);
	}

	@Subscribe
	public void onItemContainerChanged(ItemContainerChanged event)
	{
		if (event.getContainerId() != INVENTORY_CONTAINER_ID)
		{
			return;
		}

		ItemContainer inventory = event.getItemContainer();

		if (inventory == null)
		{
			return;
		}

		Map<Integer, Integer> currentTrackedInventory =
			buildTrackedInventorySnapshot(inventory);

		if (!inventorySnapshotInitialized)
		{
			/*
			 * The first inventory event establishes the starting state so
			 * existing items are not treated as newly received drops.
			 */
			previousTrackedInventory = currentTrackedInventory;
			inventorySnapshotInitialized = true;
			return;
		}

		ChestType chestAtCurrentLocation = getChestAtPlayerLocation();

		if (chestAtCurrentLocation != null)
		{
			for (
				Map.Entry<Integer, TrackedItem> entry :
				TRACKED_ITEMS_BY_ID.entrySet()
			)
			{
				int itemId = entry.getKey();
				TrackedItem trackedItem = entry.getValue();

				if (trackedItem.chestType != chestAtCurrentLocation)
				{
					continue;
				}

				int previousQuantity =
					previousTrackedInventory.getOrDefault(itemId, 0);

				int currentQuantity =
					currentTrackedInventory.getOrDefault(itemId, 0);

				int quantityAdded = currentQuantity - previousQuantity;

				for (int i = 0; i < quantityAdded; i++)
				{
					pendingInventoryDrops.add(
						new PendingInventoryDrop(
							trackedItem.chestType,
							trackedItem.normalizedName,
							gameTickCounter
						)
					);
				}
			}
		}

		/*
		 * Always update the snapshot, including when outside a chest region.
		 */
		previousTrackedInventory = currentTrackedInventory;
	}

	@Subscribe
	public void onOverlayMenuClicked(OverlayMenuClicked event)
	{
		if (event.getOverlay() != overlay)
		{
			return;
		}

		if (!"Reset".equals(event.getEntry().getOption()))
		{
			return;
		}

		String target = event.getEntry().getTarget();

		switch (target)
		{
			case "All Stats":
				resetActiveChestAllStats();
				break;

			case "Last Clog":
				resetActiveChestLastClog();
				break;

			case "Last Dupe":
				resetActiveChestLastDupe();
				break;

			case "Longest Dry":
				resetActiveChestLongestDry();
				break;

			case "Most Spooned":
				resetActiveChestMostSpooned();
				break;

			default:
				break;
		}
	}

	/**
	 * Applies a manually entered Last Clog chest count.
	 *
	 * The value is saved immediately. The internal Last Rare Drop value is
	 * recalculated using whichever is newer: Last Clog or Last Dupe.
	 */
	private void applyLastClogBaseline(
		ChestType chestType,
		int chestCount)
	{
		ChestStats stats = getStats(chestType);

		stats.lastClog = Math.max(0, chestCount);
		recalculateLastRareDropFromKnownDrops(stats);
		saveChestStats(chestType);

//		log.info(
//			"Applied {} Last Clog baseline: {}",
//			chestType.displayName,
//			stats.lastClog
//		);
	}

	/**
	 * Applies a manually entered Last Dupe chest count.
	 *
	 * The value is saved immediately. The internal Last Rare Drop value is
	 * recalculated using whichever is newer: Last Clog or Last Dupe.
	 */
	private void applyLastDupeBaseline(
		ChestType chestType,
		int chestCount)
	{
		ChestStats stats = getStats(chestType);

		stats.lastDupe = Math.max(0, chestCount);
		recalculateLastRareDropFromKnownDrops(stats);
		saveChestStats(chestType);

//		log.info(
//			"Applied {} Last Dupe baseline: {}",
//			chestType.displayName,
//			stats.lastDupe
//		);
	}

	/**
	 * Last Rare Drop is an internal value used to calculate future gaps.
	 *
	 * Because both a new clog and a duplicate count as a tracked armour drop,
	 * the latest known tracked drop is the higher of Last Clog and Last Dupe.
	 */
	private void recalculateLastRareDropFromKnownDrops(
		ChestStats stats)
	{
		stats.lastRareDrop = Math.max(
			stats.lastClog,
			stats.lastDupe
		);
	}

	/**
	 * Turns an Apply checkbox back off after it has been processed.
	 */
	private void resetOneUseConfigControl(String key)
	{
		configManager.setConfiguration(
			CONFIG_GROUP,
			key,
			false
		);
	}

	// =====================================================================
	// TESTING ONLY - COMMENTED OUT FOR NORMAL USE
	// =====================================================================
	//
	// Uncomment these methods together with the matching ConfigItems and the
	// matching ConfigChanged switch cases when simulated events are needed.
	//
//	 private void simulateChestOpen(ChestType chestType)
//	 {
//	 	ChestStats stats = getStats(chestType);
//
//	 	stats.totalOpened++;
//	 	stats.lastCountMessageTick = gameTickCounter;
//
//	 	saveChestTotalOpened(chestType);
//
//	 	log.info(
//	 		"TEST: Simulated {} opening. Total opened is now {}.",
//	 		chestType.displayName,
//	 		stats.totalOpened
//	 	);
//	 }
//
//	 private void simulateTrackedDrop(
//	 	ChestType chestType,
//	 	boolean newCollectionLogItem)
//	 {
//	 	ChestStats stats = getStats(chestType);
//
//	 	stats.totalOpened++;
//	 	stats.lastCountMessageTick = gameTickCounter;
//
//	 	saveChestTotalOpened(chestType);
//	 	recordTrackedDrop(chestType, newCollectionLogItem);
//
//	 	log.info(
//	 		"TEST: Simulated {} {} at chest count {}.",
//	 		chestType.displayName,
//	 		newCollectionLogItem
//	 			? "new collection log item"
//	 			: "duplicate",
//	 		stats.totalOpened
//	 	);
//	 }

	/**
	 * Parses and saves the exact count reported by the chest.
	 *
	 * This works both when opening the chest and when using its Check option.
	 */
	private void updateChestCount(
		ChestType chestType,
		String formattedCount)
	{
		try
		{
			int chestCount = Integer.parseInt(
				formattedCount
					.replace(",", "")
					.trim()
			);

			ChestStats stats = getStats(chestType);

			stats.totalOpened = chestCount;
			stats.lastCountMessageTick = gameTickCounter;

			saveChestTotalOpened(chestType);

//			log.debug(
//				"Updated {} total opened to {}",
//				chestType,
//				chestCount
//			);
		}
		catch (NumberFormatException exception)
		{
//			log.warn(
//				"Unable to parse chest count from '{}'",
//				formattedCount,
//				exception
//			);
		}
	}

	/**
	 * Waits for inventory and collection-log events to settle, then decides
	 * whether each tracked item was a new collection log item or a duplicate.
	 */
	private void processSettledDropEvents()
	{
		List<PendingInventoryDrop> readyInventoryDrops =
			new ArrayList<>();

		Iterator<PendingInventoryDrop> inventoryIterator =
			pendingInventoryDrops.iterator();

		while (inventoryIterator.hasNext())
		{
			PendingInventoryDrop drop = inventoryIterator.next();

			if (
				gameTickCounter - drop.detectedTick >=
				EVENT_SETTLE_TICKS
			)
			{
				readyInventoryDrops.add(drop);
				inventoryIterator.remove();
			}
		}

		List<PendingClogMessage> readyClogMessages =
			new ArrayList<>();

		Iterator<PendingClogMessage> clogIterator =
			pendingClogMessages.iterator();

		while (clogIterator.hasNext())
		{
			PendingClogMessage clogMessage = clogIterator.next();

			if (
				gameTickCounter - clogMessage.detectedTick >=
				EVENT_SETTLE_TICKS
			)
			{
				readyClogMessages.add(clogMessage);
				clogIterator.remove();
			}
		}

		boolean[] usedClogMessages =
			new boolean[readyClogMessages.size()];

		for (PendingInventoryDrop inventoryDrop : readyInventoryDrops)
		{
			if (!hasRecentChestCountMessage(
				inventoryDrop.chestType,
				inventoryDrop.detectedTick))
			{
				continue;
			}

			int matchingClogIndex = findMatchingClogMessage(
				inventoryDrop,
				readyClogMessages,
				usedClogMessages
			);

			if (matchingClogIndex >= 0)
			{
				usedClogMessages[matchingClogIndex] = true;
				recordTrackedDrop(inventoryDrop.chestType, true);
			}
			else
			{
				recordTrackedDrop(inventoryDrop.chestType, false);
			}
		}

		/*
		 * A collection log message is authoritative on its own. This fallback
		 * handles a missed or unusually ordered inventory event.
		 */
		for (int i = 0; i < readyClogMessages.size(); i++)
		{
			if (usedClogMessages[i])
			{
				continue;
			}

			PendingClogMessage clogMessage =
				readyClogMessages.get(i);

			if (hasRecentChestCountMessage(
				clogMessage.chestType,
				clogMessage.detectedTick))
			{
				recordTrackedDrop(clogMessage.chestType, true);
			}
		}
	}

	private int findMatchingClogMessage(
		PendingInventoryDrop inventoryDrop,
		List<PendingClogMessage> clogMessages,
		boolean[] usedClogMessages)
	{
		for (int i = 0; i < clogMessages.size(); i++)
		{
			if (usedClogMessages[i])
			{
				continue;
			}

			PendingClogMessage clogMessage = clogMessages.get(i);

			if (
				clogMessage.chestType == inventoryDrop.chestType &&
				clogMessage.normalizedItemName.equals(
					inventoryDrop.normalizedItemName
				) &&
				Math.abs(
					clogMessage.detectedTick -
					inventoryDrop.detectedTick
				) <= 1
			)
			{
				return i;
			}
		}

		return -1;
	}

	private boolean hasRecentChestCountMessage(
		ChestType chestType,
		int itemEventTick)
	{
		ChestStats stats = getStats(chestType);

		if (stats.lastCountMessageTick == Integer.MIN_VALUE)
		{
			return false;
		}

		return Math.abs(
			stats.lastCountMessageTick - itemEventTick
		) <= 1;
	}

	/**
	 * Updates all rare-drop statistics.
	 *
	 * Longest Dry and Most Spooned are calculated between any of the five
	 * tracked armour rewards for that chest, including both new collection
	 * log items and duplicates.
	 */
	private void recordTrackedDrop(
		ChestType chestType,
		boolean newCollectionLogItem)
	{
		ChestStats stats = getStats(chestType);
		int chestCount = stats.totalOpened;

		if (chestCount <= 0)
		{
//			log.debug(
//				"Could not record {} drop because the chest count is unknown.",
//				chestType
//			);

			return;
		}

		/*
		 * Avoid counting the same reward twice if more than one relevant event
		 * is received for it.
		 */
		if (stats.lastRareDrop == chestCount)
		{
			if (newCollectionLogItem)
			{
				stats.lastClog = chestCount;
			}

			saveChestStats(chestType);
			return;
		}

		if (
			stats.lastRareDrop > 0 &&
			chestCount > stats.lastRareDrop
		)
		{
			int gap = chestCount - stats.lastRareDrop;

			if (gap > stats.longestDry)
			{
				stats.longestDry = gap;
			}

			if (
				stats.mostSpooned == 0 ||
				gap < stats.mostSpooned
			)
			{
				stats.mostSpooned = gap;
			}
		}

		stats.lastRareDrop = chestCount;

		if (newCollectionLogItem)
		{
			stats.lastClog = chestCount;
		}
		else
		{
			stats.lastDupe = chestCount;
		}

		saveChestStats(chestType);
	}

	private Map<Integer, Integer> buildTrackedInventorySnapshot(
		ItemContainer inventory)
	{
		Map<Integer, Integer> snapshot = new HashMap<>();

		for (Item item : inventory.getItems())
		{
			if (item == null)
			{
				continue;
			}

			if (!TRACKED_ITEMS_BY_ID.containsKey(item.getId()))
			{
				continue;
			}

			snapshot.merge(
				item.getId(),
				item.getQuantity(),
				Integer::sum
			);
		}

		return snapshot;
	}

	private void initializeInventorySnapshotIfNeeded()
	{
		if (
			inventorySnapshotInitialized ||
			client.getGameState() != GameState.LOGGED_IN
		)
		{
			return;
		}

		ItemContainer inventory =
			client.getItemContainer(INVENTORY_CONTAINER_ID);

		if (inventory == null)
		{
			return;
		}

		previousTrackedInventory =
			buildTrackedInventorySnapshot(inventory);

		inventorySnapshotInitialized = true;
	}

	private void updateOverlayVisibility()
	{
		ChestType chestAtPlayerLocation =
			getChestAtPlayerLocation();

		activeChest = chestAtPlayerLocation;

		if (activeChest != null && !overlayAdded)
		{
			overlayManager.add(overlay);
			overlayAdded = true;
		}
		else if (activeChest == null && overlayAdded)
		{
			removeOverlay();
		}
	}

	private ChestType getChestAtPlayerLocation()
	{
		Player player = client.getLocalPlayer();

		if (player == null)
		{
			return null;
		}

		WorldPoint location = player.getWorldLocation();

		if (location == null)
		{
			return null;
		}

		int regionId = location.getRegionID();

		if (regionId == BRIMSTONE_CHEST_REGION)
		{
			return ChestType.BRIMSTONE;
		}

		if (regionId == ELVEN_CHEST_REGION)
		{
			return ChestType.ELVEN;
		}

		return null;
	}

	private void removeOverlay()
	{
		if (overlayAdded)
		{
			overlayManager.remove(overlay);
			overlayAdded = false;
		}
	}

	private static void registerTrackedItem(
		int itemId,
		String itemName,
		ChestType chestType)
	{
		String normalizedName = normalizeItemName(itemName);

		TrackedItem trackedItem = new TrackedItem(
			itemId,
			normalizedName,
			chestType
		);

		TRACKED_ITEMS_BY_ID.put(itemId, trackedItem);
		TRACKED_ITEMS_BY_NAME.put(normalizedName, trackedItem);
	}

	private static String normalizeItemName(String itemName)
	{
		String normalized = Text.removeTags(itemName)
			.trim()
			.toLowerCase(Locale.ENGLISH);

		while (
			normalized.endsWith(".") ||
			normalized.endsWith("!")
		)
		{
			normalized = normalized
				.substring(0, normalized.length() - 1)
				.trim();
		}

		return normalized;
	}

	private void resetRuntimeTracking()
	{
		gameTickCounter = 0;
		inventorySnapshotInitialized = false;
		previousTrackedInventory.clear();
		pendingInventoryDrops.clear();
		pendingClogMessages.clear();

		brimstoneStats.lastCountMessageTick =
			Integer.MIN_VALUE;

		elvenStats.lastCountMessageTick =
			Integer.MIN_VALUE;
	}

	/*
	 * PROFILE DATA LOADING AND SAVING
	 *
	 * Every displayed statistic and the internal lastRareDrop value are saved
	 * with setRSProfileConfiguration. They persist across plugin restarts,
	 * RuneLite restarts and logouts, while remaining separate per account.
	 */

	private void loadProfileData()
	{
		loadChestStats(
			ChestType.BRIMSTONE,
			brimstoneStats
		);

		loadChestStats(
			ChestType.ELVEN,
			elvenStats
		);
	}

	private void loadChestStats(
		ChestType chestType,
		ChestStats stats)
	{
		String prefix = chestType.configPrefix;

		stats.totalOpened =
			loadInteger(prefix + "TotalOpened");

		stats.lastClog =
			loadInteger(prefix + "LastClog");

		stats.lastDupe =
			loadInteger(prefix + "LastDupe");

		stats.lastRareDrop =
			loadInteger(prefix + "LastRareDrop");

		stats.longestDry =
			loadInteger(prefix + "LongestDry");

		stats.mostSpooned =
			loadInteger(prefix + "MostSpooned");
	}

	private int loadInteger(String key)
	{
		Integer savedValue =
			configManager.getRSProfileConfiguration(
				CONFIG_GROUP,
				key,
				Integer.class
			);

		return savedValue == null ? 0 : savedValue;
	}

	private void saveChestTotalOpened(ChestType chestType)
	{
		ChestStats stats = getStats(chestType);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			chestType.configPrefix + "TotalOpened",
			stats.totalOpened
		);
	}

	private void saveChestStats(ChestType chestType)
	{
		ChestStats stats = getStats(chestType);
		String prefix = chestType.configPrefix;

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "TotalOpened",
			stats.totalOpened
		);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "LastClog",
			stats.lastClog
		);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "LastDupe",
			stats.lastDupe
		);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "LastRareDrop",
			stats.lastRareDrop
		);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "LongestDry",
			stats.longestDry
		);

		configManager.setRSProfileConfiguration(
			CONFIG_GROUP,
			prefix + "MostSpooned",
			stats.mostSpooned
		);
	}

	private ChestStats getStats(ChestType chestType)
	{
		if (chestType == ChestType.BRIMSTONE)
		{
			return brimstoneStats;
		}

		return elvenStats;
	}

	private ChestStats getActiveStats()
	{
		if (activeChest == null)
		{
			return null;
		}

		return getStats(activeChest);
	}

	/*
	 * OVERLAY RESET ACTIONS
	 *
	 * These are the only plugin methods that intentionally clear saved stats.
	 * They run only when the user selects a Reset entry from the overlay menu.
	 */

	public void resetActiveChestAllStats()
	{
		ChestStats stats = getActiveStats();

		if (stats == null)
		{
			return;
		}

		stats.totalOpened = 0;
		stats.lastClog = 0;
		stats.lastDupe = 0;
		stats.lastRareDrop = 0;
		stats.longestDry = 0;
		stats.mostSpooned = 0;

		saveChestStats(activeChest);
	}

	public void resetActiveChestLastClog()
	{
		ChestStats stats = getActiveStats();

		if (stats == null)
		{
			return;
		}

		stats.lastClog = 0;
		recalculateLastRareDropFromKnownDrops(stats);
		saveChestStats(activeChest);
	}

	public void resetActiveChestLastDupe()
	{
		ChestStats stats = getActiveStats();

		if (stats == null)
		{
			return;
		}

		stats.lastDupe = 0;
		recalculateLastRareDropFromKnownDrops(stats);
		saveChestStats(activeChest);
	}

	public void resetActiveChestLongestDry()
	{
		ChestStats stats = getActiveStats();

		if (stats == null)
		{
			return;
		}

		stats.longestDry = 0;
		saveChestStats(activeChest);
	}

	public void resetActiveChestMostSpooned()
	{
		ChestStats stats = getActiveStats();

		if (stats == null)
		{
			return;
		}

		stats.mostSpooned = 0;
		saveChestStats(activeChest);
	}

	@Provides
	BrimstoneElvenChestTrackerConfig provideConfig(
		ConfigManager configManager)
	{
		return configManager.getConfig(
			BrimstoneElvenChestTrackerConfig.class
		);
	}

	public BrimstoneElvenChestTrackerConfig getConfig()
	{
		return config;
	}

	public ChestType getActiveChest()
	{
		return activeChest;
	}

	public String getActiveChestTitle()
	{
		if (activeChest == null)
		{
			return "";
		}

		return activeChest.displayName;
	}

	public int getActiveTotalOpened()
	{
		ChestStats stats = getActiveStats();
		return stats == null ? 0 : stats.totalOpened;
	}

	public int getActiveLastClog()
	{
		ChestStats stats = getActiveStats();
		return stats == null ? 0 : stats.lastClog;
	}

	public int getActiveLastDupe()
	{
		ChestStats stats = getActiveStats();
		return stats == null ? 0 : stats.lastDupe;
	}

	public int getActiveLongestDry()
	{
		ChestStats stats = getActiveStats();
		return stats == null ? 0 : stats.longestDry;
	}

	public int getActiveMostSpooned()
	{
		ChestStats stats = getActiveStats();
		return stats == null ? 0 : stats.mostSpooned;
	}

	public enum ChestType
	{
		BRIMSTONE(
			"Brimstone Chest",
			"brimstone"
		),

		ELVEN(
			"Elven Crystal Chest",
			"elven"
		);

		private final String displayName;
		private final String configPrefix;

		ChestType(
			String displayName,
			String configPrefix)
		{
			this.displayName = displayName;
			this.configPrefix = configPrefix;
		}
	}

	private static class ChestStats
	{
		private int totalOpened;
		private int lastClog;
		private int lastDupe;

		/*
		 * Saved internal value used to calculate the gap between tracked
		 * armour rewards.
		 */
		private int lastRareDrop;

		private int longestDry;
		private int mostSpooned;

		/*
		 * Runtime-only event-pairing value. This is not a user statistic and
		 * does not need to persist.
		 */
		private int lastCountMessageTick = Integer.MIN_VALUE;
	}

	private static class TrackedItem
	{
		@SuppressWarnings("unused")
		private final int itemId;

		private final String normalizedName;
		private final ChestType chestType;

		private TrackedItem(
			int itemId,
			String normalizedName,
			ChestType chestType)
		{
			this.itemId = itemId;
			this.normalizedName = normalizedName;
			this.chestType = chestType;
		}
	}

	private static class PendingInventoryDrop
	{
		private final ChestType chestType;
		private final String normalizedItemName;
		private final int detectedTick;

		private PendingInventoryDrop(
			ChestType chestType,
			String normalizedItemName,
			int detectedTick)
		{
			this.chestType = chestType;
			this.normalizedItemName = normalizedItemName;
			this.detectedTick = detectedTick;
		}
	}

	private static class PendingClogMessage
	{
		private final ChestType chestType;
		private final String normalizedItemName;
		private final int detectedTick;

		private PendingClogMessage(
			ChestType chestType,
			String normalizedItemName,
			int detectedTick)
		{
			this.chestType = chestType;
			this.normalizedItemName = normalizedItemName;
			this.detectedTick = detectedTick;
		}
	}
}
