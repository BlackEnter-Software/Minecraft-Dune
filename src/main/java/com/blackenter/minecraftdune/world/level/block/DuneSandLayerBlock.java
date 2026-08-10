package com.blackenter.minecraftdune.world.level.block;

import com.blackenter.minecraftdune.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DuneSandLayerBlock extends Block {
    public static final int MAX_LAYERS = 15;
    public static final IntegerProperty LAYERS = IntegerProperty.create("layers", 1, MAX_LAYERS);

    private static final VoxelShape[] SHAPES = createShapes();

    public DuneSandLayerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LAYERS, 1));
    }

    private static VoxelShape[] createShapes() {
        VoxelShape[] shapes = new VoxelShape[MAX_LAYERS + 1];
        shapes[0] = Block.box(0.0, 0.0, 0.0, 16.0, 0.0, 16.0);
        for (int layers = 1; layers <= MAX_LAYERS; layers++) {
            shapes[layers] = Block.box(0.0, 0.0, 0.0, 16.0, layers, 16.0);
        }
        return shapes;
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return SHAPES[state.getValue(LAYERS) - 1];
    }

    @Override
    protected VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos position) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected VoxelShape getVisualShape(
            BlockState state,
            BlockGetter level,
            BlockPos position,
            CollisionContext context
    ) {
        return SHAPES[state.getValue(LAYERS)];
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return pathComputationType == PathComputationType.LAND && state.getValue(LAYERS) < 9;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos position) {
        BlockPos below = position.below();
        BlockState belowState = level.getBlockState(below);
        return Block.isFaceFull(belowState.getCollisionShape(level, below), Direction.UP);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos position,
            BlockPos neighborPosition
    ) {
        return state.canSurvive(level, position)
                ? super.updateShape(state, direction, neighborState, level, position, neighborPosition)
                : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        if (existing.is(this)) {
            int nextLayers = existing.getValue(LAYERS) + 1;
            return nextLayers > MAX_LAYERS
                    ? ModBlocks.SAND.get().defaultBlockState()
                    : existing.setValue(LAYERS, nextLayers);
        }
        return defaultBlockState();
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context.getItemInHand().is(asItem())) {
            return context.replacingClickedOnBlock()
                    ? context.getClickedFace() == Direction.UP
                    : true;
        }
        return super.canBeReplaced(state, context);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LAYERS);
    }
}
