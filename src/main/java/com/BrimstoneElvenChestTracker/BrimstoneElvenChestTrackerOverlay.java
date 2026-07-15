package com.BrimstoneElvenChestTracker;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.Locale;
import javax.inject.Inject;
import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class BrimstoneElvenChestTrackerOverlay extends Overlay
{
	private final BrimstoneElvenChestTrackerPlugin plugin;
	private final PanelComponent panelComponent = new PanelComponent();

	@Inject
	public BrimstoneElvenChestTrackerOverlay(
		BrimstoneElvenChestTrackerPlugin plugin)
	{
		this.plugin = plugin;

		setPosition(OverlayPosition.TOP_LEFT);
		setLayer(OverlayLayer.ABOVE_WIDGETS);
		setPriority(OverlayPriority.HIGH);

		/*
		 * These right-click options reset only the chest represented by the
		 * currently visible overlay.
		 */
		getMenuEntries().add(
			new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				"Reset",
				"All Stats"
			)
		);

		getMenuEntries().add(
			new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				"Reset",
				"Last Clog"
			)
		);

		getMenuEntries().add(
			new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				"Reset",
				"Last Dupe"
			)
		);

		getMenuEntries().add(
			new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				"Reset",
				"Longest Dry"
			)
		);

		getMenuEntries().add(
			new OverlayMenuEntry(
				MenuAction.RUNELITE_OVERLAY,
				"Reset",
				"Most Spooned"
			)
		);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (plugin.getActiveChest() == null)
		{
			return null;
		}

		panelComponent.getChildren().clear();

		panelComponent.getChildren().add(
			TitleComponent.builder()
				.text(plugin.getActiveChestTitle())
				.build()
		);

		if (plugin.getConfig().showTotalOpened())
		{
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left("Total Opened")
					.right(formatNumber(plugin.getActiveTotalOpened()))
					.build()
			);
		}

		if (plugin.getConfig().showLastClog())
		{
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left("Last Clog")
					.right(formatOptionalNumber(plugin.getActiveLastClog()))
					.build()
			);
		}

		if (plugin.getConfig().showLastDupe())
		{
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left("Last Dupe")
					.right(formatOptionalNumber(plugin.getActiveLastDupe()))
					.build()
			);
		}

		if (plugin.getConfig().showLongestDry())
		{
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left("Longest Dry")
					.right(formatOptionalNumber(plugin.getActiveLongestDry()))
					.build()
			);
		}

		if (plugin.getConfig().showMostSpooned())
		{
			panelComponent.getChildren().add(
				LineComponent.builder()
					.left("Most Spooned")
					.right(formatOptionalNumber(plugin.getActiveMostSpooned()))
					.build()
			);
		}

		return panelComponent.render(graphics);
	}

	private String formatNumber(int number)
	{
		return String.format(Locale.US, "%,d", number);
	}

	private String formatOptionalNumber(int number)
	{
		if (number <= 0)
		{
			return "-";
		}

		return formatNumber(number);
	}
}
