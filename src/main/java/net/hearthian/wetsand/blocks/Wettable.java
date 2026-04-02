package net.hearthian.wetsand.blocks;

import com.google.common.base.Suppliers;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.mojang.serialization.Codec;
import net.hearthian.wetsand.utils.BrushableBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;   // ADD
import java.util.Map;       // ADD
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static net.hearthian.wetsand.utils.initializer.*;

public interface Wettable {
    int HUMIDITY_RANGE = 3;

    Supplier<BiMap<Object, Object>> HUMIDITY_LEVEL_INCREASES = Suppliers.memoize(() -> ImmutableBiMap.builder()
        .put(Blocks.SAND, MOIST_SAND).put(MOIST_SAND, WET_SAND).put(WET_SAND, SOAKED_SAND)
        .put(Blocks.SUSPICIOUS_SAND, MOIST_SUSPICIOUS_SAND).put(MOIST_SUSPICIOUS_SAND, WET_SUSPICIOUS_SAND).put(WET_SUSPICIOUS_SAND, SOAKED_SUSPICIOUS_SAND)
        .put(Blocks.RED_SAND, MOIST_RED_SAND).put(MOIST_RED_SAND, WET_RED_SAND).put(WET_RED_SAND, SOAKED_RED_SAND)
        .build()
    );
    Supplier<BiMap<Object, Object>> HUMIDITY_LEVEL_DECREASES = Suppliers.memoize(() -> Objects.requireNonNull(HUMIDITY_LEVEL_INCREASES.get()).inverse());

    // ADD — secondary maps for soft-dependency compat mods
    Map<Block, Block> COMPAT_INCREASES = new HashMap<>();
    Map<Block, Block> COMPAT_DECREASES = new HashMap<>();

    // ADD — called by compat classes to register their chains at startup
    static void registerCompatChain(Block base, Block moist, Block wet, Block soaked) {
        COMPAT_INCREASES.put(base,  moist);
        COMPAT_INCREASES.put(moist, wet);
        COMPAT_INCREASES.put(wet,   soaked);
        COMPAT_DECREASES.put(moist,  base);
        COMPAT_DECREASES.put(wet,    moist);
        COMPAT_DECREASES.put(soaked, wet);
    }

    HumidityLevel getHumidityLevel();

    default Optional<BlockState> tryDrench(BlockState state, ServerLevel world, BlockPos pos) {
        int currentLevel = this.getHumidityLevel().ordinal();
        AtomicInteger maxHumidityLevel = new AtomicInteger(0);

        BlockPos.findClosestMatch(pos, HUMIDITY_RANGE, HUMIDITY_RANGE, (conditionPos) -> {
            if (world.getFluidState(conditionPos).is(Fluids.WATER) || world.getFluidState(conditionPos).is(Fluids.FLOWING_WATER)) {
                int distance = conditionPos.distChessboard(pos);
                if ((HUMIDITY_RANGE - currentLevel) >= distance) {
                    maxHumidityLevel.set(HUMIDITY_RANGE - distance + 1);
                    return true;
                }
            }
            return false;
        });

        BlockPos[] adjacent = { pos.north(), pos.south(), pos.south(), pos.east(), pos.west(), pos.above(), pos.below() };

        for (BlockPos conditionPos : adjacent) {
            if (world.getFluidState(conditionPos).is(Fluids.WATER)) {
                return this.getHumidityResult(state);
            }
            if (world.getBlockState(conditionPos).is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("wet-sand", "wettable")))) {
                if (world.getBlockState(conditionPos).getBlock() instanceof Wettable wettable) {
                    int humidityLevel = wettable.getHumidityLevel().ordinal();
                    if (humidityLevel > currentLevel && currentLevel < maxHumidityLevel.get()) {
                        return this.getHumidityResult(state);
                    }
                }
            }
        }

        return Optional.empty();
    }

    default void tickHumidity(BlockState state, ServerLevel world, BlockPos pos) {
        BlockEntity entity = world.getBlockEntity(pos);

        if (entity == null || (entity instanceof BrushableBlockEntity && state.getValue(BlockStateProperties.DUSTED) == 0)) {
            this.tryDrench(state, world, pos).ifPresent((drenched) -> {
                world.setBlockAndUpdate(pos, drenched);

                if (entity instanceof BrushableBlockEntity brushableBlockEntity) {
                    BlockEntity entity2 = world.getBlockEntity(pos);
                    if (entity2 instanceof BrushableBlockEntity brushableBlockEntity2) {
                        ((BrushableBlockEntityAccessor) brushableBlockEntity2).wet_sand$setItem(brushableBlockEntity.getItem());
                    }
                }
            });
        }
    }

    // MODIFY — now also checks the compat map
    static Optional<Block> getDecreasedHumidityBlock(Block block) {
        Block result = (Block) HUMIDITY_LEVEL_DECREASES.get().get(block);
        if (result == null) result = COMPAT_DECREASES.get(block);
        return Optional.ofNullable(result);
    }

    default Optional<BlockState> getDecreasedHumidityState(BlockState state) {
        return getDecreasedHumidityBlock(state.getBlock()).map((block) -> block.withPropertiesOf(state));
    }

    // MODIFY — now also checks the compat map
    default Optional<Block> getIncreasedHumidityBlock(Block block) {
        Block result = (Block) HUMIDITY_LEVEL_INCREASES.get().get(block);
        if (result == null) result = COMPAT_INCREASES.get(block);
        return Optional.ofNullable(result);
    }

    default Optional<BlockState> getHumidityResult(BlockState state) {
        return getIncreasedHumidityBlock(state.getBlock()).map((block) -> block.withPropertiesOf(state));
    }

    enum HumidityLevel implements StringRepresentable {
        UNAFFECTED("unaffected"),
        MOIST("moist"),
        WET("wet"),
        SOAKED("soaked");

        public static final Codec<HumidityLevel> CODEC = StringRepresentable.fromEnum(HumidityLevel::values);
        private final String id;

        HumidityLevel(final String id) {
            this.id = id;
        }

        public @NotNull String getSerializedName() {
            return this.id;
        }
    }

      default void handleNetherPlacement(net.minecraft.world.level.Level level, BlockPos pos, BlockState state) {
    if (!level.isClientSide() && level.dimension().equals(net.minecraft.world.level.Level.NETHER)) {
        Block current = state.getBlock();
        Block dryBlock = current;

        while (true) {
            Optional<Block> decreased = Wettable.getDecreasedHumidityBlock(dryBlock);
            if (decreased.isEmpty()) break;
            dryBlock = decreased.get();
        }

        if (current != dryBlock) {
            level.setBlock(pos, dryBlock.defaultBlockState(), 3);
            level.levelEvent(net.minecraft.world.level.block.LevelEvent.LAVA_FIZZ, pos, 0);
        }
    }
}
}