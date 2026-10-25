package com.pocketclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Settings, stored in config/pocketclient.json (edit the file, then restart the game).
 * Toggles for HUD / sprint / performance are also saved here when you press their keys.
 */
public final class Config {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Config instance = new Config();

	// What to show
	public boolean hud = true;
	public boolean showFps = true;
	public boolean showPing = true;
	public boolean showCps = true;
	public boolean showCoords = false;
	public boolean showKeystrokes = true;

	// Gameplay helper
	public boolean toggleSprint = true;

	// Layout. Scale < 1 keeps the HUD small on phones that run a high GUI scale.
	public float hudScale = 0.8f;
	public int infoX = 4;
	public int infoY = 4;
	public int keystrokesX = 6;
	/** -1 = vertically centered on the left edge. */
	public int keystrokesY = -1;

	// Performance preset state (so toggling it off restores your old settings)
	public boolean perfActive = false;
	public Backup backup = null;

	public static final class Backup {
		public int renderDistance;
		public int simulationDistance;
		public int biomeBlend;
		public double entityDistanceScaling;
		public boolean entityShadows;
		public String particles;
		public String clouds;
		public String graphics;
	}

	public static Config get() {
		return instance;
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("pocketclient.json");
	}

	public static void load() {
		Path file = file();
		try {
			if (Files.exists(file)) {
				Config loaded = GSON.fromJson(Files.readString(file), Config.class);
				if (loaded != null) {
					instance = loaded;
				}
			}
		} catch (Exception e) {
			System.err.println("[pocketclient] Could not read config, using defaults: " + e);
		}
		save(); // writes defaults / adds any new fields
	}

	public static void save() {
		try {
			Files.writeString(file(), GSON.toJson(instance));
		} catch (IOException e) {
			System.err.println("[pocketclient] Could not save config: " + e);
		}
	}
}
