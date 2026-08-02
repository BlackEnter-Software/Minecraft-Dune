package com.blackenter.minecraftdune.entity;

import com.blackenter.minecraftdune.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.animal.Rabbit;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import org.jetbrains.annotations.Nullable;

/**
 * The first Muad'dib mouse implementation deliberately subclasses vanilla Rabbit.
 *
 * This provides proven hopping locomotion, panic behavior, breeding, temptation,
 * avoidance and navigation while the mod's own desert-specific AI is developed.
 */
public class MuaddibMouseEntity extends Rabbit {
    public MuaddibMouseEntity(
            EntityType<? extends MuaddibMouseEntity> entityType,
            Level level
    ) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Rabbit.createAttributes();
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
