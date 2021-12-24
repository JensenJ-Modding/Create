package com.simibubi.create.content.contraptions.components.motor;

import com.simibubi.create.AllShapes;
import com.simibubi.create.AllTileEntities;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import com.simibubi.create.content.contraptions.solver.ConstantSpeedRule;
import com.simibubi.create.content.contraptions.solver.HalfShaftConnectionRule;
import com.simibubi.create.content.contraptions.solver.KineticSolver;
import com.simibubi.create.content.contraptions.solver.SolverBlock;
import com.simibubi.create.foundation.block.ITE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class CreativeMotorBlock extends DirectionalKineticBlock implements ITE<CreativeMotorTileEntity>, SolverBlock {

	public CreativeMotorBlock(Properties properties) {
		super(properties);
	}

	@Override
	public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {
		return AllShapes.MOTOR_BLOCK.get(state.getValue(FACING));
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction preferred = getPreferredFacing(context);
		if ((context.getPlayer() != null && context.getPlayer()
			.isShiftKeyDown()) || preferred == null)
			return super.getStateForPlacement(context);
		return defaultBlockState().setValue(FACING, preferred);
	}

	// IRotate:

	@Override
	public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
		return face == state.getValue(FACING);
	}

	@Override
	public Axis getRotationAxis(BlockState state) {
		return state.getValue(FACING)
			.getAxis();
	}

	@Override
	public boolean hideStressImpact() {
		return true;
	}

	@Override
	public boolean isPathfindable(BlockState state, BlockGetter reader, BlockPos pos, PathComputationType type) {
		return false;
	}

	@Override
	public Class<CreativeMotorTileEntity> getTileEntityClass() {
		return CreativeMotorTileEntity.class;
	}

	@Override
	public BlockEntityType<? extends CreativeMotorTileEntity> getTileEntityType() {
		return AllTileEntities.MOTOR.get();
	}

	@Override
	public void created(KineticSolver solver, Level level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		Direction to = state.getValue(FACING);
		int speed = getTileEntityOptional(level, pos).map(te -> te.generatedSpeed.getValue()).orElse(0);

		solver.addRule(pos, new HalfShaftConnectionRule(to));
		solver.addRule(pos, new ConstantSpeedRule(speed));
	}
}
