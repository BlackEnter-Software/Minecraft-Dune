package com.blackenter.minecraftdune.entity;

import com.blackenter.minecraftdune.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

/**
 * A small desert rodent using Rabbit's proven hopping navigation and goals.
 * Its model supplies a kangaroo-mouse-specific hop and desert idle actions.
 */
public class MuaddibMouseEntity extends Rabbit {
    public final AnimationState idleAnimationState = new AnimationState();
    public final AnimationState sniffAnimationState = new AnimationState();
    public final AnimationState wiggleHeadAnimationState = new AnimationState();

    private int nextSpecialAnimationTick = 60;
    private int specialAnimationTicksRemaining;

    public MuaddibMouseEntity(
            EntityType<? extends MuaddibMouseEntity> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Rabbit.createAttributes();
    }

    @Override
    public void tick() {
        super.tick();

        if (level().isClientSide()) {
            updateClientAnimationStates();
        }
    }

    private void updateClientAnimationStates() {
        boolean standingStill = onGround()
                && getDeltaMovement().horizontalDistanceSqr() < 0.0004D;

        idleAnimationState.animateWhen(standingStill, tickCount);

        if (!standingStill) {
            stopSpecialAnimations();
            nextSpecialAnimationTick = Math.max(nextSpecialAnimationTick, 30);
            return;
        }

        if (specialAnimationTicksRemaining > 0) {
            specialAnimationTicksRemaining--;
            if (specialAnimationTicksRemaining == 0) {
                stopSpecialAnimations();
            }
            return;
        }

        if (--nextSpecialAnimationTick > 0) {
            return;
        }

        if (random.nextFloat() < 0.65F) {
            sniffAnimationState.start(tickCount);
            specialAnimationTicksRemaining = 16;
        } else {
            wiggleHeadAnimationState.start(tickCount);
            specialAnimationTicksRemaining = 20;
        }

        nextSpecialAnimationTick = 70 + random.nextInt(111);
    }

    private void stopSpecialAnimations() {
        sniffAnimationState.stop();
        wiggleHeadAnimationState.stop();
        specialAnimationTicksRemaining = 0;
    }

    @Nullable
    @Override
    public MuaddibMouseEntity getBreedOffspring(
            ServerLevel level,
            AgeableMob otherParent
    ) {
        return ModEntityTypes.MUADDIB_MOUSE.get().create(level);
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        if (level.getBlockState(pos.below()).is(BlockTags.SAND)) {
            return 10.0F;
        }

        return super.getWalkTargetValue(pos, level);
    }
}
