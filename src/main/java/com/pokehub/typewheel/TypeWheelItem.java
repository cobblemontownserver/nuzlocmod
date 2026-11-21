package com.pokehub.typewheel;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class TypeWheelItem extends BlockItem {
    public TypeWheelItem(Block block, Item.Properties props) {
        super(block, props);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext ctx, BlockState state) {
        if (!super.canPlace(ctx, state)) return false;
        Direction facing = state.getValue(TypeWheelBlock.FACING);
        for (BlockPos p : WheelLayout.partPositions(ctx.getClickedPos(), facing)) {
            if (!ctx.getLevel().getBlockState(p).canBeReplaced()) {
                Player pl = ctx.getPlayer();
                if (pl != null && !ctx.getLevel().isClientSide) {
                    pl.displayClientMessage(Component.literal("Not enough room: the wheel needs a clear space 5 blocks wide and 7 blocks tall."), true);
                }
                return false;
            }
        }
        return true;
    }
}
