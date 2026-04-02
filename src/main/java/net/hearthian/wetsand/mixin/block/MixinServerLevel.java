package net.hearthian.wetsand.mixin.block;

import net.hearthian.wetsand.blocks.Wettable;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public class MixinServerLevel {

    @Inject(method = "tickChunk", at = @At("TAIL"))
    private void wetsand$tickCompatBlocks(LevelChunk chunk, int randomTickSpeed, CallbackInfo ci) {
        ServerLevel world = (ServerLevel) (Object) this;
        LevelChunkSection[] sections = chunk.getSections();

        for (int sectionIndex = 0; sectionIndex < sections.length; sectionIndex++) {
            LevelChunkSection section = sections[sectionIndex];
            if (section.hasOnlyAir()) continue;

            // Calculate the actual bottom Y of this section from the chunk's min build height
            int sectionBottomY = world.getMinY() + (sectionIndex * 16);

            for (int i = 0; i < randomTickSpeed; i++) {
                int x = chunk.getPos().getMinBlockX() + world.random.nextInt(16);
                int z = chunk.getPos().getMinBlockZ() + world.random.nextInt(16);
                int y = sectionBottomY + world.random.nextInt(16);

                BlockPos pos = new BlockPos(x, y, z);
                BlockState state = world.getBlockState(pos);
                Block block = state.getBlock();

                if (!Wettable.COMPAT_INCREASES.containsKey(block)) continue;
                int currentLevel = 0;
                if (block instanceof Wettable wettable) {
                    currentLevel = wettable.getHumidityLevel().ordinal();
                }
                final int finalCurrentLevel = currentLevel;

                BlockPos.findClosestMatch(pos, Wettable.HUMIDITY_RANGE, Wettable.HUMIDITY_RANGE, (conditionPos) -> {
                    if (world.getFluidState(conditionPos).is(Fluids.WATER) ||
                        world.getFluidState(conditionPos).is(Fluids.FLOWING_WATER)) {
                        int distance = conditionPos.distChessboard(pos);
                        if ((Wettable.HUMIDITY_RANGE - finalCurrentLevel) >= distance) {
                            Block next = Wettable.COMPAT_INCREASES.get(block);
                            if (next != null) {
                                world.setBlockAndUpdate(pos, next.defaultBlockState());
                            }
                            return true;
                        }
                    }
                    return false;
                });
            }
        }
    }
}