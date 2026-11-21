package com.pokehub.typewheel;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The base block of the wheel. Holds the spin state and draws the whole wheel. */
public class TypeWheelBlock extends BaseEntityBlock {
    public static final MapCodec<TypeWheelBlock> CODEC = simpleCodec(TypeWheelBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    static final VoxelShape SHAPE_NS = Block.box(0, 0, 3, 16, 16, 13);
    static final VoxelShape SHAPE_EW = Block.box(3, 0, 0, 13, 16, 16);

    public TypeWheelBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    static VoxelShape shapeFor(Direction facing) {
        return facing.getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TypeWheelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TypeWheelMod.WHEEL_BE,
                level.isClientSide ? TypeWheelBlockEntity::clientTick : TypeWheelBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return spin(level, pos, player);
    }

    static InteractionResult spin(Level level, BlockPos controller, Player player) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(controller) instanceof TypeWheelBlockEntity be && player instanceof ServerPlayer sp) {
            be.trySpin(sp);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moved) {
        super.onPlace(state, level, pos, oldState, moved);
        if (level.isClientSide || oldState.is(this)) return;
        Direction f = state.getValue(FACING);
        BlockState part = TypeWheelMod.PART.defaultBlockState().setValue(TypeWheelPartBlock.FACING, f);
        for (BlockPos p : WheelLayout.partPositions(pos, f)) {
            if (level.getBlockState(p).canBeReplaced()) level.setBlock(p, part, 3);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            for (BlockPos p : WheelLayout.partPositions(pos, state.getValue(FACING))) {
                if (level.getBlockState(p).is(TypeWheelMod.PART)) level.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }
}
