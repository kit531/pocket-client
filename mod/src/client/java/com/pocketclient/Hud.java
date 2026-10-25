package com.pocketclient;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.LocalPlayer;

import java.util.Locale;

/**
 * Draws the info lines (FPS / ping / CPS / coords) and the keystrokes block.
 *
 * Render-only: nothing here changes gameplay. The text is rebuilt a few times a second
 * instead of every frame, so the per-frame cost is just drawing, with no string building
 * or text measuring (less garbage for the GC on phones).
 */
public final class Hud {
	private static final int BG = 0x80000000;
	private static final int BG_DOWN = 0xB0FFFFFF;
	private static final int FG = 0xFFFFFFFF;
	private static final int FG_DOWN = 0xFF000000;

	private static final long REFRESH_MS = 200L;
	private static final int MAX_LINES = 5; // fps, ping, cps, coords, facing

	// W, A, S, D, LMB, RMB, SPACE
	private static final String[] KEY_LABELS = {"W", "A", "S", "D", "LMB", "RMB", "SPACE"};
	private static final int[] keyWidths = new int[KEY_LABELS.length];

	private static final String[] lines = new String[MAX_LINES];
	private static final int[] lineWidths = new int[MAX_LINES];
	private static int lineCount = 0;
	private static long lastRefresh = 0L;

	private static long windowStart = System.currentTimeMillis();
	private static int frames = 0;
	private static int fps = 0;

	private Hud() {
	}

	public static void render(GuiGraphics g) {
		// Count rendered HUD frames ourselves so we don't depend on a Minecraft FPS getter.
		long now = System.currentTimeMillis();
		frames++;
		if (now - windowStart >= 1000L) {
			fps = (int) (frames * 1000L / (now - windowStart));
			frames = 0;
			windowStart = now;
		}

		Minecraft mc = Minecraft.getInstance();
		Config c = Config.get();
		LocalPlayer player = mc.player;
		if (!c.hud || player == null || mc.options.hideGui) {
			return;
		}

		if (now - lastRefresh >= REFRESH_MS) {
			refresh(mc, c, player, now);
		}

		PoseStack pose = g.pose();
		pose.pushPose();
		pose.scale(c.hudScale, c.hudScale, 1.0f);
		drawInfo(g, mc.font, c);
		if (c.showKeystrokes) {
			drawKeystrokes(g, mc, c);
		}
		pose.popPose();
	}

	private static void refresh(Minecraft mc, Config c, LocalPlayer player, long now) {
		Font font = mc.font;
		int n = 0;

		if (c.showFps) {
			n = put(font, n, fps + " FPS");
		}
		if (c.showPing) {
			int ping = -1;
			ClientPacketListener connection = mc.getConnection();
			if (connection != null) {
				PlayerInfo info = connection.getPlayerInfo(player.getUUID());
				if (info != null) {
					ping = info.getLatency();
				}
			}
			n = put(font, n, (ping < 0 ? "--" : String.valueOf(ping)) + " ms");
		}
		if (c.showCps) {
			n = put(font, n, Cps.left() + " | " + Cps.right() + " CPS");
		}
		if (c.showCoords) {
			String facing = player.getDirection().getName();
			facing = facing.substring(0, 1).toUpperCase(Locale.ROOT) + facing.substring(1);
			n = put(font, n, String.format(Locale.ROOT, "%.1f %.1f %.1f", player.getX(), player.getY(), player.getZ()));
			n = put(font, n, facing);
		}
		lineCount = n;

		for (int i = 0; i < KEY_LABELS.length; i++) {
			keyWidths[i] = font.width(KEY_LABELS[i]);
		}
		lastRefresh = now;
	}

	private static int put(Font font, int n, String text) {
		lines[n] = text;
		lineWidths[n] = font.width(text);
		return n + 1;
	}

	private static void drawInfo(GuiGraphics g, Font font, Config c) {
		int x = c.infoX;
		int y = c.infoY;
		for (int i = 0; i < lineCount; i++) {
			g.fill(x - 2, y - 2, x + lineWidths[i] + 2, y + 10, BG);
			g.drawString(font, lines[i], x, y, FG, true);
			y += 13;
		}
	}

	private static void drawKeystrokes(GuiGraphics g, Minecraft mc, Config c) {
		Options o = mc.options;
		Font font = mc.font;

		int cell = 18;
		int gap = 2;
		int wide = cell * 3 + gap * 2;
		int mouseH = 14;
		int spaceH = 12;
		int totalH = cell + gap + cell + gap + mouseH + gap + spaceH;

		int x0 = c.keystrokesX;
		int scaledHeight = (int) (g.guiHeight() / c.hudScale);
		int y0 = c.keystrokesY >= 0 ? c.keystrokesY : (scaledHeight - totalH) / 2;

		int row1 = y0 + cell + gap;
		int row2 = row1 + cell + gap;
		int row3 = row2 + mouseH + gap;
		int half = (wide - gap) / 2;

		key(g, font, 0, x0 + cell + gap, y0, cell, cell, o.keyUp.isDown());
		key(g, font, 1, x0, row1, cell, cell, o.keyLeft.isDown());
		key(g, font, 2, x0 + cell + gap, row1, cell, cell, o.keyDown.isDown());
		key(g, font, 3, x0 + (cell + gap) * 2, row1, cell, cell, o.keyRight.isDown());
		key(g, font, 4, x0, row2, half, mouseH, o.keyAttack.isDown());
		key(g, font, 5, x0 + half + gap, row2, half, mouseH, o.keyUse.isDown());
		key(g, font, 6, x0, row3, wide, spaceH, o.keyJump.isDown());
	}

	private static void key(GuiGraphics g, Font font, int label, int x, int y, int w, int h, boolean down) {
		g.fill(x, y, x + w, y + h, down ? BG_DOWN : BG);
		g.drawString(font, KEY_LABELS[label], x + (w - keyWidths[label]) / 2, y + (h - 8) / 2 + 1, down ? FG_DOWN : FG, false);
	}
}
