package net.jmb19905.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.math.Axis;
import net.jmb19905.common.MoonController;
import net.jmb19905.client.SkyUtils;
import net.jmb19905.config.*;
import net.jmb19905.client.data.StarDataManager;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.*;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {
    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow @Nullable private VertexBuffer starBuffer;

    @Shadow private double prevCamZ;

    @Inject(at = @At("HEAD"), method = "renderSky")
    private void renderSky(Matrix4f p_202424_, Matrix4f p_254034_, float p_202426_, Camera p_202427_, boolean p_202428_, Runnable p_202429_, CallbackInfo ci) {
        SkyUtils.calculateStarLatitudeRotation(level, p_202427_.getPosition().z());
        if(StarDataManager.vanillaStarBuffer == null){
            StarDataManager.vanillaStarBuffer = starBuffer;
        }
        if(ClientConfig.starsType.get() == StarsType.CUSTOM){
            starBuffer = StarDataManager.INSTANCE.getStarBuffer();
        }
        else if(ClientConfig.starsType.get() == StarsType.VANILLA){
            starBuffer = StarDataManager.vanillaStarBuffer;
        }
    }



    @Inject(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F", shift = At.Shift.AFTER))
    private void renderSky$customStars(Matrix4f mat1, Matrix4f matrix, float f, Camera camera, boolean bl, Runnable runnable, CallbackInfo ci, @Local PoseStack stack) {
        if(ClientConfig.starsType.get() != StarsType.CUSTOM) return;
        assert this.level != null;
        float f11 = 1.0F - this.level.getRainLevel(f);
        float f10 = (float) (level.getStarBrightness(f) * f11 * ClientConfig.starBrightness.get());
        if (f10 > 0.0F) {
            stack.pushPose();
            stack.mulPose(Axis.XP.rotationDegrees(180.0F));
            stack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            if(ClientConfig.latitudeEffects.get() == LatitudeEffects.STARS_ONLY) {
                stack.mulPose(Axis.YP.rotationDegrees((float) (180 * SkyUtils.starLatitudeRotation(level, camera.getPosition().z()))));
            }
            stack.mulPose(Axis.ZP.rotationDegrees(-level.getTimeOfDay(f) * 360.0F));
            stack.mulPose(Axis.ZP.rotationDegrees((float) (-SkyUtils.yearRotation(level) * 360.0F)));
            RenderSystem.setShaderColor(f10, f10, f10, f10);
            FogRenderer.setupNoFog();
            assert starBuffer != null;
            starBuffer.bind();
            starBuffer.drawWithShader(stack.last().pose(), matrix, GameRenderer.getPositionColorShader());
            VertexBuffer.unbind();
            runnable.run();
            stack.popPose();
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexBuffer;drawWithShader(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lnet/minecraft/client/renderer/ShaderInstance;)V"), method = "renderSky")
    private void renderSky$skipVanillaStars(VertexBuffer buffer, Matrix4f p_254480_, Matrix4f p_254555_, ShaderInstance p_253993_) {
        if(!buffer.equals(starBuffer) || ClientConfig.starsType.get() == StarsType.VANILLA){
            buffer.drawWithShader(p_254480_, p_254555_, p_253993_);
        }
    }

    @Redirect(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DimensionSpecialEffects;getSunriseColor(FF)[F"), method = "renderSky")
    private float[] renderSky$getSunriseColor(DimensionSpecialEffects effects, float skyAngle, float partialTicks) {
        return SkyUtils.getSunriseColor(level, prevCamZ, partialTicks);
    }
    @Redirect(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 1), method = "renderSky")
    private void renderSky$sunriseRotationRemoveVanilla(PoseStack instance, Quaternionf p_254385_) {

    }
    @Inject(at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionf;)V", ordinal = 1), method = "renderSky")
    private void renderSky$sunriseRotation(Matrix4f mat, Matrix4f p_254034_, float f, Camera p_202427_, boolean p_202428_, Runnable p_202429_, CallbackInfo ci, @Local PoseStack stack) {
        stack.mulPose(Axis.ZP.rotationDegrees((float) SkyUtils.getSunriseColorRotation(level, prevCamZ, f)));
    }
    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMoonPhase()I"), method = "renderSky")
    private void renderSky$renderMoon$Pre(Matrix4f mat, Matrix4f p_254034_, float f, Camera p_202427_, boolean p_202428_, Runnable p_202429_, CallbackInfo ci, @Local PoseStack stack) {
        if (CommonConfig.moonOrbitType.get() == MoonOrbitType.VANILLA) return;
        assert level != null;
        stack.mulPose(Axis.XP.rotation(-MoonController.getInstance().getMoonOrbitPosition(level.getDayTime()) * Mth.TWO_PI));
        stack.mulPose(Axis.YP.rotationDegrees(90));
        //poseStack.mulPose(Axis.XP.rotationDegrees(15));
        int phase = this.level.getMoonPhase();
        if(phase == 4 && !ClientConfig.renderNewMoon.get()){
            RenderSystem.setShaderColor(0,0,0,0);
        }
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getStarBrightness(F)F"), method = "renderSky")
    private void renderSky$renderMoon$Post(Matrix4f mat, Matrix4f p_254034_, float f, Camera p_202427_, boolean p_202428_, Runnable p_202429_, CallbackInfo ci, @Local PoseStack stack) {
        assert level != null;
        stack.mulPose(Axis.XP.rotation(MoonController.getInstance().getMoonOrbitPosition(level.getDayTime()) * Mth.TWO_PI));
        stack.mulPose(Axis.YP.rotationDegrees(-90));
        //poseStack.mulPose(Axis.XP.rotationDegrees(-15));
    }

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"), method = "renderSky")
    private void renderSky$renderCelestial$Pre(Matrix4f mat, Matrix4f p_254034_, float f, Camera camera, boolean p_202428_, Runnable p_202429_, CallbackInfo ci, @Local PoseStack stack) {
        if(ClientConfig.latitudeEffects.get() == LatitudeEffects.ALL) {
            stack.mulPose(Axis.XP.rotationDegrees((float) (-180 * SkyUtils.starLatitudeRotation(level, camera.getPosition().z()))));
        }
    }
}