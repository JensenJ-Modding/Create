package com.simibubi.create.foundation.behaviour.filtering;

import java.util.function.Consumer;
import java.util.function.Function;

import com.simibubi.create.AllPackets;
import com.simibubi.create.foundation.behaviour.base.IBehaviourType;
import com.simibubi.create.foundation.behaviour.base.SmartTileEntity;
import com.simibubi.create.foundation.behaviour.base.TileEntityBehaviour;
import com.simibubi.create.modules.logistics.item.filter.FilterItem;

import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.Vec3d;

public class FilteringBehaviour extends TileEntityBehaviour {

	public static IBehaviourType<FilteringBehaviour> TYPE = new IBehaviourType<FilteringBehaviour>() {
	};

	SlotPositioning slotPositioning;
	boolean showCount;
	Vec3d textShift;

	private ItemStack filter;
	public int count;
	private Consumer<ItemStack> callback;

	int scrollableValue;
	int ticksUntilScrollPacket;
	boolean forceClientState;

	public FilteringBehaviour(SmartTileEntity te) {
		super(te);
		filter = ItemStack.EMPTY;
		slotPositioning = new SlotPositioning(state -> Vec3d.ZERO, state -> Vec3d.ZERO);
		showCount = false;
		callback = stack -> {
		};
		textShift = Vec3d.ZERO;
		count = 0;
		ticksUntilScrollPacket = -1;
	}

	@Override
	public void writeNBT(CompoundNBT nbt) {
		nbt.put("Filter", getFilter().serializeNBT());
		nbt.putInt("FilterAmount", count);
		super.writeNBT(nbt);
	}

	@Override
	public void readNBT(CompoundNBT nbt) {
		filter = ItemStack.read(nbt.getCompound("Filter"));
		count = nbt.getInt("FilterAmount");
		if (nbt.contains("ForceScrollable")) {
			scrollableValue = count;
			ticksUntilScrollPacket = -1;
		}
		super.readNBT(nbt);
	}

	@Override
	public CompoundNBT writeToClient(CompoundNBT compound) {
		if (forceClientState) {
			compound.putBoolean("ForceScrollable", true);
			forceClientState = false;
		}
		return super.writeToClient(compound);
	}

	@Override
	public void tick() {
		super.tick();

		if (!getWorld().isRemote)
			return;
		if (ticksUntilScrollPacket == -1)
			return;
		if (ticksUntilScrollPacket > 0) {
			ticksUntilScrollPacket--;
			return;
		}

		AllPackets.channel.sendToServer(new FilteringCountUpdatePacket(getPos(), scrollableValue));
		ticksUntilScrollPacket = -1;
	}

	public FilteringBehaviour withCallback(Consumer<ItemStack> filterCallback) {
		callback = filterCallback;
		return this;
	}

	public FilteringBehaviour withSlotPositioning(SlotPositioning mapping) {
		this.slotPositioning = mapping;
		return this;
	}

	public FilteringBehaviour showCount() {
		showCount = true;
		return this;
	}

	public FilteringBehaviour moveText(Vec3d shift) {
		textShift = shift;
		return this;
	}
	
	@Override
	public void initialize() {
		super.initialize();
		scrollableValue = count;
	}

	public void setFilter(ItemStack stack) {
		filter = stack.copy();
		callback.accept(filter);

		if (filter.getItem() instanceof FilterItem)
			count = 0;
		else
			count = stack.getCount();
		forceClientState = true;
		
		tileEntity.markDirty();
		tileEntity.sendData();
	}

	public ItemStack getFilter() {
		return filter.copy();
	}

	public boolean isCountVisible() {
		return showCount && !getFilter().isEmpty();
	}

	public boolean test(ItemStack stack) {
		return filter.isEmpty() || FilterItem.test(stack, filter);
	}

	@Override
	public IBehaviourType<?> getType() {
		return TYPE;
	}

	public boolean testHit(Vec3d hit) {
		BlockState state = tileEntity.getBlockState();
		Vec3d offset = slotPositioning.offset.apply(state);
		if (offset == null)
			return false;
		Vec3d localHit = hit.subtract(new Vec3d(tileEntity.getPos()));
		return localHit.distanceTo(offset) < slotPositioning.scale / 2;
	}
	
	public int getAmount() {
		return count;
	}
	
	public boolean anyAmount() {
		return count == 0;
	}

	public static class SlotPositioning {
		Function<BlockState, Vec3d> offset;
		Function<BlockState, Vec3d> rotation;
		float scale;

		public SlotPositioning(Function<BlockState, Vec3d> offsetForState,
				Function<BlockState, Vec3d> rotationForState) {
			offset = offsetForState;
			rotation = rotationForState;
			scale = 1;
		}

		public SlotPositioning scale(float scale) {
			this.scale = scale;
			return this;
		}

	}

}
