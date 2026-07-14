package com.UnsiredTracker;

import net.runelite.api.MenuAction;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayMenuEntry;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.PanelComponent;

import javax.inject.Inject;
import java.awt.*;

public class UnsiredTrackerOverlay extends Overlay
{
    private final UnsiredTrackerPlugin plugin;
    private final PanelComponent panelComponent = new PanelComponent();

    @Inject
    public UnsiredTrackerOverlay(UnsiredTrackerPlugin plugin)
    {
        this.plugin = plugin;

        setPosition(OverlayPosition.TOP_LEFT);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(OverlayPriority.HIGH);

        getMenuEntries().add(
                new OverlayMenuEntry(
                        MenuAction.RUNELITE_OVERLAY,
                        "Reset",
                        "All Stats"
                ));

        getMenuEntries().add(
                new OverlayMenuEntry(
                        MenuAction.RUNELITE_OVERLAY,
                        "Reset",
                        "Last Unsired KC"
                ));

        getMenuEntries().add(
                new OverlayMenuEntry(
                        MenuAction.RUNELITE_OVERLAY,
                        "Reset",
                        "Longest Dry"
                ));

        getMenuEntries().add(
                new OverlayMenuEntry(
                        MenuAction.RUNELITE_OVERLAY,
                        "Reset",
                        "Most Spooned"
                ));
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        panelComponent.getChildren().clear();

        if (plugin.getConfig().currentKC())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Current KC")
                            .right(String.valueOf(plugin.getCurrentSireKC()))
                            .build()
            );
        }

        if (plugin.getConfig().lastUnsiredKC())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Last Unsired")
                            .right(String.valueOf(plugin.getLastUnsiredKC()))
                            .build()
            );
        }

        if (plugin.getConfig().killsSinceLastUnsired())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Current Dry")
                            .right(String.valueOf(plugin.getKillsSinceLastUnsired()))
                            .build()
            );
        }

        if (plugin.getConfig().dryStreak())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Longest Dry")
                            .right(String.valueOf(plugin.getDryStreak()))
                            .build()
            );
        }

        if (plugin.getConfig().bestStreak())
        {
            panelComponent.getChildren().add(
                    LineComponent.builder()
                            .left("Most Spooned")
                            .right(String.valueOf(plugin.getBestStreak()))
                            .build()
            );
        }

        return panelComponent.render(graphics);
    }
}