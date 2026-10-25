package com.pocketclient.mixin;

import com.pocketclient.Cps;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onPress", at = @At("HEAD"))
	private void pocketclient$countClick(long window, int button, int action, int modifiers, CallbackInfo ci) {
		// action 1 = GLFW_PRESS. Ignore clicks made inside menus.
		if (action == 1 && Minecraft.getInstance().screen == null) {
			Cps.click(button);
		}
	}
}
