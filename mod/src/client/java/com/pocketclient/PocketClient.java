package com.pocketclient;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/**
 * Server-safe by design.
 *
 * What this mod does NOT do: it never sets the player's position, velocity or rotation, never
 * sends or edits network packets, and has no reach / hitbox / timer / fly features. The server
 * sees exactly what a vanilla client would send.
 *
 * What it touches:
 *  - the HUD (render only),
 *  - the state of the vanilla Sprint key (auto-sprint), so Minecraft's own rules still decide
 *    whether you can actually sprint (hunger, blindness, using an item, sneaking, ...),
 *  - video options in the optional performance preset.
 */
public class PocketClient implements ClientModInitializer {
	private static final String CATEGORY = "key.categories.pocketclient";

	// F6-F8 are free in vanilla and can be mapped to on-screen buttons in Zalith/Pojav.
	private KeyMapping toggleHud;
	private KeyMapping toggleSprint;
	private KeyMapping togglePerformance;

	/** True while the Sprint key is being held down by us (not by the player). */
	private boolean sprintHeldByUs = false;

	@Override
	public void onInitializeClient() {
		Config.load();

		toggleHud = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.pocketclient.toggle_hud", GLFW.GLFW_KEY_F6, CATEGORY));
		toggleSprint = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.pocketclient.toggle_sprint", GLFW.GLFW_KEY_F7, CATEGORY));
		togglePerformance = KeyBindingHelper.registerKeyBinding(
				new KeyMapping("key.pocketclient.toggle_performance", GLFW.GLFW_KEY_F8, CATEGORY));

		HudRenderCallback.EVENT.register((graphics, tickDelta) -> Hud.render(graphics));
		ClientTickEvents.END_CLIENT_TICK.register(this::onTick);
	}

	private void onTick(Minecraft mc) {
		LocalPlayer player = mc.player;
		Config c = Config.get();

		while (toggleHud.consumeClick()) {
			c.hud = !c.hud;
			Config.save();
			notice(player, "HUD", c.hud);
		}
		while (toggleSprint.consumeClick()) {
			c.toggleSprint = !c.toggleSprint;
			Config.save();
			notice(player, "Auto-sprint", c.toggleSprint);
		}
		while (togglePerformance.consumeClick()) {
			boolean on = Performance.toggle(mc);
			notice(player, "Performance preset", on);
		}

		tickAutoSprint(mc, c);
	}

	/**
	 * Same effect as the vanilla "Toggle Sprint" accessibility option: while Forward is held we
	 * hold the Sprint key. Whether the player really sprints is still decided by vanilla code.
	 */
	private void tickAutoSprint(Minecraft mc, Config c) {
		KeyMapping sprint = mc.options.keySprint;
		boolean want = c.toggleSprint
				&& mc.player != null
				&& mc.screen == null
				&& mc.options.keyUp.isDown();

		if (want) {
			if (!sprint.isDown()) {
				sprint.setDown(true);
				sprintHeldByUs = true;
			}
		} else if (sprintHeldByUs) {
			sprint.setDown(false);
			sprintHeldByUs = false;
		}
	}

	private static void notice(LocalPlayer player, String label, boolean on) {
		if (player != null) {
			player.displayClientMessage(Component.literal(label + ": " + (on ? "ON" : "OFF")), true);
		}
	}
}
