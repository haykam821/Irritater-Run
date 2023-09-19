package io.github.haykam821.irritaterrun.entity;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import eu.pb4.polymer.core.api.entity.PolymerEntity;
import eu.pb4.polymer.core.api.utils.PolymerUtils;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.VirtualEntityUtils;
import eu.pb4.polymer.virtualentity.api.attachment.EntityAttachment;
import eu.pb4.polymer.virtualentity.api.elements.InteractionElement;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import eu.pb4.polymer.virtualentity.api.elements.MobAnchorElement;
import eu.pb4.polymer.virtualentity.api.tracker.EntityTrackedData;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntityPassengersSetS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import xyz.nucleoid.stimuli.EventInvokers;
import xyz.nucleoid.stimuli.Stimuli;

public class IrritaterEntity extends Entity implements PolymerEntity {
	private static final String TARGET_KEY = "target";

	private static final double MOVEMENT_SPEED = 0.8;

	private static final float HEAD_OFFSET_Y = 30 / 16f;
	private static final Vector3fc HEAD_OFFSET = new Vector3f(0, HEAD_OFFSET_Y, 0);

	private static final String HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTRiMmNiZmUxZmQ0ZDMxMjM0NjEwODFhZDQ2MGFjYjZjMDM0NWJlZDNmM2NlOTZkNDc1YjVmNThmN2I5MDMwYiJ9fX0=";
	private static final ItemStack HEAD_STACK = PolymerUtils.createPlayerHead(HEAD_TEXTURE);

	private final ElementHolder holder;
	private final EntityAttachment attachment;

	private final ItemDisplayElement head = new ItemDisplayElement(HEAD_STACK);
	private final InteractionElement interaction = InteractionElement.redirect(this);
	private final MobAnchorElement rideAnchor = new MobAnchorElement();

	private Entity target;
	private UUID targetUuid;

	public IrritaterEntity(EntityType<? extends IrritaterEntity> type, World world) {
		super(type, world);

		this.holder = new ElementHolder() {
			@Override
			protected void notifyElementsOfPositionUpdate(Vec3d newPos, Vec3d delta) {
				IrritaterEntity.this.rideAnchor.notifyMove(this.currentPos, newPos, delta);
			}

			@Override
			public Vec3d getPos() {
				return this.getAttachment().getPos();
			}
		};

		this.head.setTranslation(HEAD_OFFSET);
		this.head.setInterpolationDuration(0);

		this.interaction.setWidth(this.getWidth());
		this.interaction.setHeight(this.getHeight());

		VirtualEntityUtils.addVirtualPassenger(this, this.head.getEntityId(), this.interaction.getEntityId());

		this.holder.addElement(this.head);
		this.holder.addElement(this.interaction);
		this.holder.addElement(this.rideAnchor);

		this.attachment = EntityAttachment.of(this.holder, this);
	}

	public void setTarget(Entity target) {
		this.target = target;
		this.targetUuid = target.getUuid();

		this.updateTargetTracking(target, false);
	}

	public void updateTargetTracking(Entity target, boolean move) {
		double theta = MathHelper.atan2(target.getZ() - this.getZ(), target.getX() - this.getX());

		double x = move ? Math.cos(theta) * MOVEMENT_SPEED : 0;
		double z = move ? Math.sin(theta) * MOVEMENT_SPEED : 0;

		float yaw = (float) (MathHelper.DEGREES_PER_RADIAN * theta) - 90;

		this.refreshPositionAndAngles(this.getX() + x, this.getY(), this.getZ() + z, yaw, 0);

		float rotationY = (float) -theta - MathHelper.PI / 2;
		this.head.setLeftRotation(new Quaternionf().rotateY(rotationY));

		this.interaction.setYaw(yaw);

		if (move) {
			this.head.startInterpolation();
		}
	}

	private Entity getTarget() {
		if (this.target != null && !this.target.isRemoved()) {
			return this.target;
		} else if (this.targetUuid != null && this.getWorld() instanceof ServerWorld world) {
			this.target = world.getEntity(this.targetUuid);
			return this.target;
		}

		return this.target;
	}

	@Override
	public void tick() {
		super.tick();

		this.attachment.tick();
		this.holder.tick();

		Entity target = this.getTarget();

		if (target != null) {
			if (this.getBoundingBox().intersects(target.getBoundingBox())) {
				try (EventInvokers invokers = Stimuli.select().forEntity(this)) {
					ActionResult result = invokers.get(IrritaterCatchEntityEvent.EVENT).onIrritaterCatchEntity(this, target);

					if (result != ActionResult.FAIL) {
						this.discard();
					}
				}
			} else {
				this.updateTargetTracking(target, true);
			}
		}
	}

	@Override
	public void remove(RemovalReason reason) {
		super.remove(reason);

		this.holder.destroy();
		this.attachment.destroy();
	}

	@Override
	protected void initDataTracker() {
		return;
	}

	@Override
	protected void readCustomDataFromNbt(NbtCompound nbt) {
		if (this.targetUuid != null) {
			nbt.putUuid(TARGET_KEY, this.targetUuid);
		}
	}

	@Override
	protected void writeCustomDataToNbt(NbtCompound nbt) {
		if (nbt.containsUuid(TARGET_KEY)) {
			this.target = null;
			this.targetUuid = nbt.getUuid(TARGET_KEY);
		}
	}

	@Override
	public EntityType<?> getPolymerEntityType(ServerPlayerEntity player) {
		return EntityType.ARMOR_STAND;
	}

	@Override
	public void onEntityPacketSent(Consumer<Packet<?>> consumer, Packet<?> packet) {
		if (packet instanceof EntityPassengersSetS2CPacket passengersSetPacket) {
			IntList passengers = IntList.of(passengersSetPacket.getPassengerIds());
			packet = VirtualEntityUtils.createRidePacket(this.rideAnchor.getEntityId(), passengers);
		}

		consumer.accept(packet);
	}

	@Override
	public void modifyRawTrackedData(List<DataTracker.SerializedEntry<?>> data, ServerPlayerEntity player, boolean initial) {
		data.add(DataTracker.SerializedEntry.of(EntityTrackedData.FLAGS, (byte) (1 << EntityTrackedData.INVISIBLE_FLAG_INDEX)));
		data.add(DataTracker.SerializedEntry.of(ArmorStandEntity.ARMOR_STAND_FLAGS, (byte) (ArmorStandEntity.SMALL_FLAG | ArmorStandEntity.MARKER_FLAG)));
	}
}
