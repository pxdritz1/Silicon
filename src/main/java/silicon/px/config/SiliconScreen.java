package silicon.px.config;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.CommonComponents;
import silicon.px.SiliconConfig;

import java.util.List;

public class SiliconScreen extends Screen {
	private final Screen parent;
	private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
	private final boolean wasEnabled = SiliconConfig.enabled;
	private final boolean wasMergeRandom = SiliconConfig.mergeRandomTextures;
	private final boolean wasFast = SiliconConfig.fastMode;
	private final int wasTolerance = SiliconConfig.tolerance;

	public SiliconScreen(Screen parent) {
		super(Component.translatable("silicon.config.title"));
		this.parent = parent;
	}

	@Override
	protected void init() {
		layout.addTitleHeader(title, font);

		LinearLayout body = layout.addToContents(LinearLayout.vertical().spacing(8));
		body.addChild(CycleButton.onOffBuilder(SiliconConfig.enabled)
				.create(0, 0, 210, 20, Component.translatable("silicon.config.enabled"), (button, value) -> SiliconConfig.enabled = value))
				.setTooltip(Tooltip.create(Component.translatable("silicon.config.enabled.tooltip")));
		body.addChild(CycleButton.onOffBuilder(SiliconConfig.mergeRandomTextures)
				.create(0, 0, 210, 20, Component.translatable("silicon.config.mergeRandomTextures"), (button, value) -> SiliconConfig.mergeRandomTextures = value))
				.setTooltip(Tooltip.create(Component.translatable("silicon.config.mergeRandomTextures.tooltip")));
		body.addChild(CycleButton.onOffBuilder(SiliconConfig.fastMode)
				.create(0, 0, 210, 20, Component.translatable("silicon.config.fastMode"), (button, value) -> SiliconConfig.fastMode = value))
				.setTooltip(Tooltip.create(Component.translatable("silicon.config.fastMode.tooltip")));
		body.addChild(CycleButton.builder(value -> value == 0 ? Component.translatable("silicon.config.tolerance.exact") : Component.literal(String.valueOf(value)), SiliconConfig.tolerance)
				.withValues(List.of(0, 1, 2, 4, 8))
				.create(0, 0, 210, 20, Component.translatable("silicon.config.tolerance"), (button, value) -> SiliconConfig.tolerance = value))
				.setTooltip(Tooltip.create(Component.translatable("silicon.config.tolerance.tooltip")));

		layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> onClose()).width(200).build());
		layout.visitWidgets(this::addRenderableWidget);
		repositionElements();
	}

	@Override
	protected void repositionElements() {
		layout.arrangeElements();
	}

	@Override
	public void onClose() {
		SiliconConfig.save();
		// chunks only pick up the new settings when they're rebuilt
		if (wasEnabled != SiliconConfig.enabled || wasMergeRandom != SiliconConfig.mergeRandomTextures || wasFast != SiliconConfig.fastMode
				|| wasTolerance != SiliconConfig.tolerance)
			Minecraft.getInstance().levelRenderer.allChanged();
		minecraft.setScreen(parent);
	}
}
