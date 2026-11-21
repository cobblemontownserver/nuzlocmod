package com.pokehub.typewheel.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.pokehub.typewheel.TypeWheelBlock;
import com.pokehub.typewheel.TypeWheelBlockEntity;
import com.pokehub.typewheel.TypeWheelMod;
import com.pokehub.typewheel.WheelLayout;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Draws the stand, the spinning 18-type disc and the pointer. All sizes are in blocks. */
public class TypeWheelRenderer implements BlockEntityRenderer<TypeWheelBlockEntity> {
    private static final ResourceLocation TEX = TypeWheelMod.id("textures/entity/type_wheel.png");
    private static final float S = 1024.0F;
    // colour swatches in the corners of the texture: {u0, v0, u1, v1}
    private static final float[] DARK = sw(0, 0);
    private static final float[] GOLD = sw(904, 0);
    private static final float[] RED = sw(0, 904);
    private static final float[] BACK = sw(904, 904);

    private static final float CY = WheelLayout.CENTER_Y;
    private static final float R = WheelLayout.RADIUS;
    private static final float ZF = 0.25F;
    private static final float ZB = 0.02F;
    private static final int N = 36;

    private static float[] sw(int x, int y) {
        return new float[] {(x + 20) / S, (y + 20) / S, (x + 100) / S, (y + 100) / S};
    }

    public TypeWheelRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public boolean shouldRenderOffScreen(TypeWheelBlockEntity be) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public void render(TypeWheelBlockEntity be, float partialTick, PoseStack ps, MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof TypeWheelBlock)) return;
        Direction facing = state.getValue(TypeWheelBlock.FACING);
        Level level = be.getLevel();
        int light = packedLight;
        if (level != null) light = LevelRenderer.getLightColor(level, be.getBlockPos().above(3).relative(facing));

        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEX));
        ps.pushPose();
        ps.translate(0.5F, 0.0F, 0.5F);
        ps.mulPose(Axis.YP.rotationDegrees(-facing.toYRot()));

        // stand
        box(vc, ps, -1.5F, 0.0F, -0.45F, 1.5F, 0.4F, 0.45F, DARK, light);
        box(vc, ps, -1.55F, 0.4F, -0.5F, 1.55F, 0.5F, 0.5F, GOLD, light);
        box(vc, ps, -0.25F, 0.5F, -0.3F, 0.25F, 6.45F, -0.02F, DARK, light);
        box(vc, ps, -0.2F, 6.2F, -0.3F, 0.2F, 6.45F, 0.46F, DARK, light);

        // pointer (fixed, tip down)
        pointer(vc, ps, light);

        // spinning wheel
        ps.pushPose();
        ps.translate(0.0F, CY, 0.0F);
        ps.mulPose(Axis.ZP.rotationDegrees(-be.getAngle(partialTick)));
        disc(vc, ps, light);
        for (int i = 0; i < 18; i++) {
            double a = Math.toRadians(i * 20.0);
            float x = (float) (2.395 * Math.sin(a));
            float y = (float) (2.395 * Math.cos(a));
            box(vc, ps, x - 0.05F, y - 0.05F, ZF, x + 0.05F, y + 0.05F, ZF + 0.1F, GOLD, light);
        }
        box(vc, ps, -0.28F, -0.28F, ZF, 0.28F, 0.28F, ZF + 0.1F, GOLD, light);
        ps.mulPose(Axis.ZP.rotationDegrees(45.0F));
        box(vc, ps, -0.28F, -0.28F, ZF, 0.28F, 0.28F, ZF + 0.1F, GOLD, light);
        ps.popPose();

        ps.popPose();
    }

    private static void disc(VertexConsumer vc, PoseStack ps, int light) {
        PoseStack.Pose pose = ps.last();
        float k = 0.995F;
        for (int i = 0; i < N; i++) {
            double a0 = Math.toRadians(i * 360.0 / N);
            double a1 = Math.toRadians((i + 1) * 360.0 / N);
            float s0 = (float) Math.sin(a0), c0 = (float) Math.cos(a0);
            float s1 = (float) Math.sin(a1), c1 = (float) Math.cos(a1);
            float x0 = R * s0, y0 = R * c0, x1 = R * s1, y1 = R * c1;
            // front (textured)
            v(vc, pose, 0, 0, ZF, 0.5F, 0.5F, 0, 0, 1, light);
            v(vc, pose, 0, 0, ZF, 0.5F, 0.5F, 0, 0, 1, light);
            v(vc, pose, x1, y1, ZF, 0.5F + 0.5F * k * s1, 0.5F - 0.5F * k * c1, 0, 0, 1, light);
            v(vc, pose, x0, y0, ZF, 0.5F + 0.5F * k * s0, 0.5F - 0.5F * k * c0, 0, 0, 1, light);
            // back
            v(vc, pose, 0, 0, ZB, BACK[0], BACK[1], 0, 0, -1, light);
            v(vc, pose, 0, 0, ZB, BACK[0], BACK[1], 0, 0, -1, light);
            v(vc, pose, x0, y0, ZB, BACK[2], BACK[1], 0, 0, -1, light);
            v(vc, pose, x1, y1, ZB, BACK[2], BACK[3], 0, 0, -1, light);
            // rim
            float nx = (s0 + s1) * 0.5F, ny = (c0 + c1) * 0.5F;
            v(vc, pose, x0, y0, ZF, GOLD[0], GOLD[1], nx, ny, 0, light);
            v(vc, pose, x1, y1, ZF, GOLD[2], GOLD[1], nx, ny, 0, light);
            v(vc, pose, x1, y1, ZB, GOLD[2], GOLD[3], nx, ny, 0, light);
            v(vc, pose, x0, y0, ZB, GOLD[0], GOLD[3], nx, ny, 0, light);
        }
    }

    private static void pointer(VertexConsumer vc, PoseStack ps, int light) {
        PoseStack.Pose pose = ps.last();
        float tx = 0.0F, ty = CY + 2.15F, lx = -0.3F, rx = 0.3F, top = CY + 2.75F, z0 = 0.36F, z1 = 0.46F;
        float[] c = RED;
        // front + back triangles
        v(vc, pose, tx, ty, z1, c[0], c[1], 0, 0, 1, light);
        v(vc, pose, tx, ty, z1, c[0], c[1], 0, 0, 1, light);
        v(vc, pose, rx, top, z1, c[2], c[1], 0, 0, 1, light);
        v(vc, pose, lx, top, z1, c[2], c[3], 0, 0, 1, light);
        v(vc, pose, tx, ty, z0, c[0], c[1], 0, 0, -1, light);
        v(vc, pose, tx, ty, z0, c[0], c[1], 0, 0, -1, light);
        v(vc, pose, lx, top, z0, c[2], c[1], 0, 0, -1, light);
        v(vc, pose, rx, top, z0, c[2], c[3], 0, 0, -1, light);
        // top
        quad(vc, pose, lx, top, z1, rx, top, z1, rx, top, z0, lx, top, z0, c, 0, 1, 0, light);
        // left and right slopes
        quad(vc, pose, tx, ty, z1, lx, top, z1, lx, top, z0, tx, ty, z0, c, -0.9F, -0.44F, 0, light);
        quad(vc, pose, rx, top, z1, tx, ty, z1, tx, ty, z0, rx, top, z0, c, 0.9F, -0.44F, 0, light);
    }

    private static void box(VertexConsumer vc, PoseStack ps, float x0, float y0, float z0, float x1, float y1, float z1, float[] c, int light) {
        PoseStack.Pose p = ps.last();
        quad(vc, p, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c, 0, 0, 1, light);   // front
        quad(vc, p, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, c, 0, 0, -1, light);  // back
        quad(vc, p, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c, -1, 0, 0, light);  // left
        quad(vc, p, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, c, 1, 0, 0, light);   // right
        quad(vc, p, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, c, 0, 1, 0, light);   // top
        quad(vc, p, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c, 0, -1, 0, light);  // bottom
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float[] uv, float nx, float ny, float nz, int light) {
        v(vc, p, ax, ay, az, uv[0], uv[3], nx, ny, nz, light);
        v(vc, p, bx, by, bz, uv[2], uv[3], nx, ny, nz, light);
        v(vc, p, cx, cy, cz, uv[2], uv[1], nx, ny, nz, light);
        v(vc, p, dx, dy, dz, uv[0], uv[1], nx, ny, nz, light);
    }

    private static void v(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float vv,
                          float nx, float ny, float nz, int light) {
        vc.addVertex(p, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, vv)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(p, nx, ny, nz);
    }
}
