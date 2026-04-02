package net.hearthian.wetsand.compat;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.hearthian.wetsand.blocks.SoakedBlock;
import net.hearthian.wetsand.blocks.Wettable;
import net.hearthian.wetsand.blocks.WettableBlock;
import net.hearthian.wetsand.blocks.WettableFallingBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.NotNull;

import static net.hearthian.wetsand.WetSand.LOGGER;
import static net.hearthian.wetsand.WetSand.MOD_ID;

public class BoPCompat {

    public static Block MOIST_WHITE_SAND;
    public static Block WET_WHITE_SAND;
    public static Block SOAKED_WHITE_SAND;

    public static Block MOIST_BLACK_SAND;
    public static Block WET_BLACK_SAND;
    public static Block SOAKED_BLACK_SAND;

    public static Block MOIST_ORANGE_SAND;
    public static Block WET_ORANGE_SAND;
    public static Block SOAKED_ORANGE_SAND;

    public static void init() {
        LOGGER.info("Biomes O' Plenty detected — registering BoP sand variants");

        Block bopWhiteSand  = getBoP("white_sand");
        Block bopBlackSand  = getBoP("black_sand");
        Block bopOrangeSand = getBoP("orange_sand");

        // --- White sand ---
        MOIST_WHITE_SAND  = new WettableFallingBlock(Wettable.HumidityLevel.MOIST,  props(bopWhiteSand,  "moist_white_sand"));
        WET_WHITE_SAND    = new WettableBlock(       Wettable.HumidityLevel.WET,    props(bopWhiteSand,  "wet_white_sand").mapColor(MapColor.COLOR_LIGHT_GRAY));
        SOAKED_WHITE_SAND = new SoakedBlock(                                         props(bopWhiteSand,  "soaked_white_sand").mapColor(MapColor.COLOR_LIGHT_GRAY));

        // --- Black sand ---
        MOIST_BLACK_SAND  = new WettableFallingBlock(Wettable.HumidityLevel.MOIST,  props(bopBlackSand,  "moist_black_sand"));
        WET_BLACK_SAND    = new WettableBlock(       Wettable.HumidityLevel.WET,    props(bopBlackSand,  "wet_black_sand").mapColor(MapColor.COLOR_BLACK));
        SOAKED_BLACK_SAND = new SoakedBlock(                                         props(bopBlackSand,  "soaked_black_sand").mapColor(MapColor.COLOR_BLACK));

        // --- Orange sand ---
        MOIST_ORANGE_SAND  = new WettableFallingBlock(Wettable.HumidityLevel.MOIST, props(bopOrangeSand, "moist_orange_sand"));
        WET_ORANGE_SAND    = new WettableBlock(       Wettable.HumidityLevel.WET,   props(bopOrangeSand, "wet_orange_sand").mapColor(MapColor.TERRACOTTA_ORANGE));
        SOAKED_ORANGE_SAND = new SoakedBlock(                                        props(bopOrangeSand, "soaked_orange_sand").mapColor(MapColor.TERRACOTTA_ORANGE));

        // Register all 9 blocks + items
        registerBlockItem("moist_white_sand",   MOIST_WHITE_SAND);
        registerBlockItem("wet_white_sand",     WET_WHITE_SAND);
        registerBlockItem("soaked_white_sand",  SOAKED_WHITE_SAND);
        registerBlockItem("moist_black_sand",   MOIST_BLACK_SAND);
        registerBlockItem("wet_black_sand",     WET_BLACK_SAND);
        registerBlockItem("soaked_black_sand",  SOAKED_BLACK_SAND);
        registerBlockItem("moist_orange_sand",  MOIST_ORANGE_SAND);
        registerBlockItem("wet_orange_sand",    WET_ORANGE_SAND);
        registerBlockItem("soaked_orange_sand", SOAKED_ORANGE_SAND);

        // Register humidity chains so Wettable logic picks them up
        Wettable.registerCompatChain(bopWhiteSand,  MOIST_WHITE_SAND,  WET_WHITE_SAND,  SOAKED_WHITE_SAND);
        Wettable.registerCompatChain(bopBlackSand,  MOIST_BLACK_SAND,  WET_BLACK_SAND,  SOAKED_BLACK_SAND);
        Wettable.registerCompatChain(bopOrangeSand, MOIST_ORANGE_SAND, WET_ORANGE_SAND, SOAKED_ORANGE_SAND);

        // Add to creative tab after each BoP base block
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.NATURAL_BLOCKS).register(content -> {
            content.addAfter(bopWhiteSand,   MOIST_WHITE_SAND,  WET_WHITE_SAND,  SOAKED_WHITE_SAND);
            content.addAfter(bopBlackSand,   MOIST_BLACK_SAND,  WET_BLACK_SAND,  SOAKED_BLACK_SAND);
            content.addAfter(bopOrangeSand,  MOIST_ORANGE_SAND, WET_ORANGE_SAND, SOAKED_ORANGE_SAND);
        });
    }

    // -------------------------------------------------------------------------

    private static Block getBoP(String path) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("biomesoplenty", path));
    }

    private static BlockBehaviour.Properties props(Block base, String id) {
        return BlockBehaviour.Properties.ofFullCopy(base)
            .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, id)));
    }

    private static void registerBlockItem(String path, Block block) {
        ResourceKey<@NotNull Item>  itemKey  = ResourceKey.create(Registries.ITEM,  Identifier.fromNamespaceAndPath(MOD_ID, path));
        ResourceKey<@NotNull Block> blockKey = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, path));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM,  itemKey,  new BlockItem(block, new Item.Properties().useBlockDescriptionPrefix().setId(itemKey)));
    }
}