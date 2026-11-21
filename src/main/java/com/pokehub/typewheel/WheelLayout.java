package com.pokehub.typewheel;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

/** Which blocks around the base block the big wheel fills (so the whole wheel is clickable and solid). */
public final class WheelLayout {
    public static final float CENTER_Y = 3.5F;
    public static final float RADIUS = 2.5F;
    /** {dx, dy} offsets of the filler blocks, relative to the base block. */
    public static final List<int[]> OFFSETS = new ArrayList<>();

    static {
        for (int dy = 0; dy <= 6; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                if (dx == 0 && dy == 0) continue;
                boolean in = dx == 0 || (dy == 0 && Math.abs(dx) <= 1);
                if (!in) {
                    double nx = Math.max(dx - 0.5, Math.min(0.0, dx + 0.5));
                    double ny = Math.max(dy, Math.min(CENTER_Y, dy + 1.0));
                    double ddx = nx, ddy = ny - CENTER_Y;
                    in = Math.sqrt(ddx * ddx + ddy * ddy) < RADIUS;
                }
                if (in) OFFSETS.add(new int[] {dx, dy});
            }
        }
    }

    private WheelLayout() {}

    public static Direction right(Direction facing) {
        return facing.getCounterClockWise();
    }

    public static List<BlockPos> partPositions(BlockPos base, Direction facing) {
        Direction r = right(facing);
        List<BlockPos> out = new ArrayList<>(OFFSETS.size());
        for (int[] o : OFFSETS) out.add(base.relative(r, o[0]).above(o[1]));
        return out;
    }

    /** Finds the base block of the wheel that a filler block belongs to, or null. */
    public static BlockPos findController(BlockGetter level, BlockPos part, Direction facing) {
        Direction r = right(facing);
        for (int[] o : OFFSETS) {
            BlockPos c = part.relative(r, -o[0]).below(o[1]);
            BlockState s = level.getBlockState(c);
            if (s.getBlock() instanceof TypeWheelBlock && s.getValue(TypeWheelBlock.FACING) == facing) return c;
        }
        return null;
    }
}
