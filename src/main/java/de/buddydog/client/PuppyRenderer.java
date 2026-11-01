package de.buddydog.client;

import com.mojang.blaze3d.vertex.PoseStack;

import de.buddydog.PuppyEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.animal.wolf.Wolf;

/**
 * Nutzt das normale Wolf-Modell. Beim Kommando "Platz" wird der sitzende Hund
 * zusaetzlich abgeflacht, damit er so aussieht, als wuerde er flach auf dem Boden liegen.
 */
public class PuppyRenderer extends WolfRenderer {

    public PuppyRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void extractRenderState(Wolf entity, WolfRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        // Die Pose "SLEEPING" nutzen wir hier nur als Merker fuer "liegt" (nur im Renderer).
        if (entity instanceof PuppyEntity puppy && puppy.isLying()) {
            state.pose = Pose.SLEEPING;
        }
    }

    @Override
    protected void setupRotations(WolfRenderState state, PoseStack poseStack, float bodyRot, float scale) {
        super.setupRotations(state, poseStack, bodyRot, scale);
        if (state.pose == Pose.SLEEPING) {
            poseStack.scale(1.0F, 0.6F, 1.0F);
        }
    }
}
