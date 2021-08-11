package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.DyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.event.RegistryEvent;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Locale;

/**
 * Created 11/30/2020 by SuperMartijn642
 */
public enum ScarecrowType {

    PRIMITIVE;

    private static final VoxelShape PRIMITIVE_SHAPE = VoxelShapes.or(
        VoxelShapes.create(7.5 / 16d, 0, 7.5 / 16d, 8.5 / 16d, 26 / 16d, 8.5 / 16d),
        VoxelShapes.create(4 / 16d, 9 / 16d, 6 / 16d, 12 / 16d, 22 / 16d, 10 / 16d),
        VoxelShapes.create(4 / 16d, 21 / 16d, 4 / 16d, 12 / 16d, 29 / 16d, 12 / 16d));

    private static final VoxelShape[] PRIMITIVE_SHAPES_BOTTOM = new VoxelShape[4];
    private static final VoxelShape[] PRIMITIVE_SHAPES_TOP = new VoxelShape[4];

    static{
        PRIMITIVE_SHAPES_BOTTOM[Direction.NORTH.getHorizontalIndex()] = PRIMITIVE_SHAPE;
        PRIMITIVE_SHAPES_BOTTOM[Direction.EAST.getHorizontalIndex()] = rotateShape(Direction.NORTH, Direction.EAST, PRIMITIVE_SHAPE);
        PRIMITIVE_SHAPES_BOTTOM[Direction.SOUTH.getHorizontalIndex()] = rotateShape(Direction.NORTH, Direction.SOUTH, PRIMITIVE_SHAPE);
        PRIMITIVE_SHAPES_BOTTOM[Direction.WEST.getHorizontalIndex()] = rotateShape(Direction.NORTH, Direction.WEST, PRIMITIVE_SHAPE);
        for(int i = 0; i < 4; i++)
            PRIMITIVE_SHAPES_TOP[i] = PRIMITIVE_SHAPES_BOTTOM[i].withOffset(0, -1, 0);
    }

    /**
     * Credits to wyn_price
     * @see <a href="https://forums.minecraftforge.net/topic/74979-1144-rotate-voxel-shapes/?do=findComment&comment=391969">Minecraft Forge forum post</a>
     */
    public static VoxelShape rotateShape(Direction from, Direction to, VoxelShape shape){
        VoxelShape[] buffer = new VoxelShape[]{shape, VoxelShapes.empty()};

        int times = (to.getHorizontalIndex() - from.getHorizontalIndex() + 4) % 4;
        for(int i = 0; i < times; i++){
            buffer[0].forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> buffer[1] = VoxelShapes.or(buffer[1], VoxelShapes.create(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            buffer[0] = buffer[1];
            buffer[1] = VoxelShapes.empty();
        }

        return buffer[0];
    }

    public final EnumMap<DyeColor,ScarecrowBlock> blocks = new EnumMap<>(DyeColor.class);
    public TileEntityType<? extends ScarecrowTile> tileTileEntityType;
    private final EnumMap<DyeColor,BlockItem> items = new EnumMap<>(DyeColor.class);

    public void registerBlock(RegistryEvent.Register<Block> e){
        switch(this){
            case PRIMITIVE:
                Arrays.stream(DyeColor.values()).forEach(color -> this.blocks.put(color, new ScarecrowBlock(this, color)));
        }
        this.blocks.values().forEach(e.getRegistry()::register);
    }

    public void registerTileType(RegistryEvent.Register<TileEntityType<?>> e){
        this.tileTileEntityType = TileEntityType.Builder.create(this::createTileEntity, this.blocks.values().toArray(new Block[0])).build(null);
        this.tileTileEntityType.setRegistryName(this.name().toLowerCase(Locale.ROOT) + "_tile");
        e.getRegistry().register(this.tileTileEntityType);
    }

    public void registerItem(RegistryEvent.Register<Item> e){
        this.blocks.forEach((color, block) -> {
            BlockItem item = new BlockItem(block, new Item.Properties().group(ItemGroup.DECORATIONS));
            item.setRegistryName(this.getRegistryName(color));
            this.items.put(color, item);
        });
        this.items.values().forEach(e.getRegistry()::register);
    }

    public String getRegistryName(DyeColor color){
        return (color == DyeColor.PURPLE ? this.name().toLowerCase(Locale.ROOT) : color.getTranslationKey()) + "_scarecrow";
    }

    public Block.Properties getBlockProperties(DyeColor color){
        switch(this){
            case PRIMITIVE:
                return Block.Properties.create(Material.WOOL, color).sound(SoundType.CLOTH).harvestTool(ToolType.AXE).hardnessAndResistance(0.5f);
        }
        return Block.Properties.create(Material.AIR);
    }

    public VoxelShape getBlockShape(Direction facing, boolean bottom){
        switch(this){
            case PRIMITIVE:
                return bottom ? PRIMITIVE_SHAPES_BOTTOM[facing.getHorizontalIndex()] : PRIMITIVE_SHAPES_TOP[facing.getHorizontalIndex()];
        }
        return VoxelShapes.fullCube();
    }

    public ScarecrowTile createTileEntity(){
        switch(this){
            case PRIMITIVE:
                break;
        }
        return new ScarecrowTile(this);
    }

    public RenderType getRenderLayer(){
        switch(this){
            case PRIMITIVE:
                return RenderType.getTranslucent();
        }
        return RenderType.getSolid();
    }

    public boolean is2BlocksHigh(){
        return this == PRIMITIVE;
    }

}
