package net.hearthian.wetsand.blocks;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class WettableBlock extends Block implements Wettable {
    public static final MapCodec<WettableBlock> CODEC = RecordCodecBuilder.mapCodec((instance) -> instance.group(HumidityLevel.CODEC.fieldOf("humidity_state").forGetter(Wettable::getHumidityLevel), propertiesCodec()).apply(instance, WettableBlock::new));
    private final HumidityLevel humidityLevel;

    public @NotNull MapCodec<WettableBlock> codec() {
        return CODEC;
    }

    public WettableBlock(HumidityLevel humidityLevel, BlockBehaviour.Properties settings) {
        super(settings);
        this.humidityLevel = humidityLevel;
    }

    @Override
    public void setPlacedBy(@NotNull net.minecraft.world.level.Level level, @NotNull BlockPos pos, 
                            @NotNull BlockState state,
                            @org.jetbrains.annotations.Nullable net.minecraft.world.entity.LivingEntity placer,
                            net.minecraft.world.item.ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        handleNetherPlacement(level, pos, state);
    }

    protected void randomTick(@NotNull BlockState state, @NotNull ServerLevel world, @NotNull BlockPos pos, @NotNull RandomSource random) {
//        LOGGER.info("GETS RANDOM TICK...");
        this.tickHumidity(state, world, pos);
    }

    protected boolean isRandomlyTicking(BlockState state) {
        return getIncreasedHumidityBlock(state.getBlock()).isPresent();
    }

    public HumidityLevel getHumidityLevel() {
        return this.humidityLevel;
    }
}
