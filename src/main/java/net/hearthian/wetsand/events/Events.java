package net.hearthian.wetsand.events;

import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.hearthian.wetsand.blocks.Wettable;
import net.hearthian.wetsand.utils.BrushableBlockEntityAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class Events {
    public static void registerDry() {
        UseBlockCallback.EVENT.register((player, world, hand, blockHit) -> {
            BlockState state = world.getBlockState(blockHit.getBlockPos());

            if (!player.isSpectator()
                && world instanceof ServerLevel serverWorld
                && state.is(TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("wet-sand", "wettable")))
            ) {
                if (player.getItemInHand(hand).is(Items.GLASS_BOTTLE) && state.getBlock() instanceof Wettable wettableBlock) {
                    BlockPos pos = blockHit.getBlockPos();
                    wettableBlock.getDecreasedHumidityState(state).ifPresent(blockState -> {
                        BlockEntity entity = serverWorld.getBlockEntity(pos);

                        player.getMainHandItem().shrink(1);
                        player.addItem(Items.POTION.getDefaultInstance());
                        serverWorld.setBlockAndUpdate(pos, blockState);
//                        serverWorld.setBlockState(pos, blockState, 2, 0);
//                        entity.cancelRemoval();

                        if (entity instanceof BrushableBlockEntity BrushableBlockEntity) {
                            BlockEntity entity2 = serverWorld.getBlockEntity(pos);
                            if (entity2 instanceof BrushableBlockEntity brushableBlockEntity2) {
                                ((BrushableBlockEntityAccessor) brushableBlockEntity2).wet_sand$setItem(BrushableBlockEntity.getItem());
//                                brushableBlockEntity2.markDirty();
//                                serverWorld.updateListeners(pos, state, state, Block.NOTIFY_ALL);
                            }
                        }
                    });
                }
            }

            return InteractionResult.PASS;
        });
    }
}