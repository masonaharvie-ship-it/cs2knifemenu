package com.cs2knifemenu.mixin;

import com.cs2knifemenu.KnifeViewRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * VERSION-SENSITIVE: replaces the first-person hand/item rendering with the knife while it is active.
 * (If the game crashes on startup with "InvalidInjectionException", the method parameters below
 * differ in your Minecraft version - see README "Version notes".)
 */
@Mixin(ItemInHandRenderer.class)
public class ItemInHandRendererMixin {
    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    private void cs2knifemenu$renderKnife(float partialTick, PoseStack poseStack, SubmitNodeCollector collector,
                                          LocalPlayer player, int light, CallbackInfo ci) {
        if (KnifeViewRenderer.render(poseStack, collector, light)) ci.cancel();
    }
}
