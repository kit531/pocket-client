package com.pocketclient;

import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.ParticleStatus;

/** One-key "low-end phone" preset. Pressing the key again restores the settings you had. */
public final class Performance {
	private Performance() {
	}

	/** @return true if the preset is now active. */
	public static boolean toggle(Minecraft mc) {
		Options o = mc.options;
		Config c = Config.get();

		if (!c.perfActive || c.backup == null) {
			Config.Backup b = new Config.Backup();
			b.renderDistance = o.renderDistance().get();
			b.simulationDistance = o.simulationDistance().get();
			b.biomeBlend = o.biomeBlendRadius().get();
			b.entityDistanceScaling = o.entityDistanceScaling().get();
			b.entityShadows = o.entityShadows().get();
			b.particles = o.particles().get().name();
			b.clouds = o.cloudStatus().get().name();
			b.graphics = o.graphicsMode().get().name();
			c.backup = b;

			o.renderDistance().set(Math.min(b.renderDistance, 8));
			o.simulationDistance().set(Math.min(b.simulationDistance, 6));
			o.biomeBlendRadius().set(0);
			o.entityDistanceScaling().set(0.75);
			o.entityShadows().set(false);
			o.particles().set(ParticleStatus.DECREASED);
			o.cloudStatus().set(CloudStatus.OFF);
			o.graphicsMode().set(GraphicsStatus.FAST);
			c.perfActive = true;
		} else {
			Config.Backup b = c.backup;
			o.renderDistance().set(b.renderDistance);
			o.simulationDistance().set(b.simulationDistance);
			o.biomeBlendRadius().set(b.biomeBlend);
			o.entityDistanceScaling().set(b.entityDistanceScaling);
			o.entityShadows().set(b.entityShadows);
			o.particles().set(ParticleStatus.valueOf(b.particles));
			o.cloudStatus().set(CloudStatus.valueOf(b.clouds));
			o.graphicsMode().set(GraphicsStatus.valueOf(b.graphics));
			c.perfActive = false;
			c.backup = null;
		}

		o.save();
		Config.save();
		return c.perfActive;
	}
}
