package com.thewatcher.horror;

import com.thewatcher.gen.B;
import com.thewatcher.gen.BlockSink;
import com.thewatcher.gen.Realm;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.block.PointedDripstoneBlock;
import net.minecraft.block.StairsBlock;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.enums.Thickness;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/** Turns realm-generation blocks into real Minecraft blocks, and lets RealmGen build straight into a world. */
public final class RealmBlocks {
    private RealmBlocks() {}

    private static final BlockState[] STATES = new BlockState[B.values().length];

    public static BlockState state(B b) {
        BlockState s = STATES[b.ordinal()];
        if (s == null) STATES[b.ordinal()] = s = create(b);
        return s;
    }

    private static BlockState drip(Direction dir, Thickness t) {
        return Blocks.POINTED_DRIPSTONE.getDefaultState()
                .with(PointedDripstoneBlock.VERTICAL_DIRECTION, dir).with(PointedDripstoneBlock.THICKNESS, t);
    }

    private static BlockState create(B b) {
        return switch (b) {
            case AIR -> Blocks.AIR.getDefaultState();
            case STONE -> Blocks.STONE.getDefaultState();
            case BEDROCK -> Blocks.BEDROCK.getDefaultState();
            case WATER -> Blocks.WATER.getDefaultState();
            case YELLOW_TERRACOTTA -> Blocks.YELLOW_TERRACOTTA.getDefaultState();
            case WHITE_TERRACOTTA -> Blocks.WHITE_TERRACOTTA.getDefaultState();
            case SMOOTH_SANDSTONE -> Blocks.SMOOTH_SANDSTONE.getDefaultState();
            case SEA_LANTERN -> Blocks.SEA_LANTERN.getDefaultState();
            case REDSTONE_LAMP -> Blocks.REDSTONE_LAMP.getDefaultState();
            case BROWN_TERRACOTTA -> Blocks.BROWN_TERRACOTTA.getDefaultState();
            case RED_TERRACOTTA -> Blocks.RED_TERRACOTTA.getDefaultState();
            case BROWN_CARPET -> Blocks.BROWN_CARPET.getDefaultState();
            case IRON_BARS -> Blocks.IRON_BARS.getDefaultState();
            case CHAIR_N -> Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.NORTH);
            case CHAIR_E -> Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST);
            case CHAIR_S -> Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.SOUTH);
            case CHAIR_W -> Blocks.SPRUCE_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST);
            case DOOR_LOWER -> Blocks.DARK_OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                    .with(DoorBlock.OPEN, true).with(DoorBlock.HALF, DoubleBlockHalf.LOWER);
            case DOOR_UPPER -> Blocks.DARK_OAK_DOOR.getDefaultState().with(DoorBlock.FACING, Direction.NORTH)
                    .with(DoorBlock.OPEN, true).with(DoorBlock.HALF, DoubleBlockHalf.UPPER);
            case COBWEB -> Blocks.COBWEB.getDefaultState();
            case CRYING_OBSIDIAN -> Blocks.CRYING_OBSIDIAN.getDefaultState();
            case SHROOMLIGHT -> Blocks.SHROOMLIGHT.getDefaultState();
            case DIRT -> Blocks.DIRT.getDefaultState();
            case COARSE_DIRT -> Blocks.COARSE_DIRT.getDefaultState();
            case ROOTED_DIRT -> Blocks.ROOTED_DIRT.getDefaultState();
            case DEEPSLATE -> Blocks.DEEPSLATE.getDefaultState();
            case DARK_OAK_LOG -> Blocks.DARK_OAK_LOG.getDefaultState();
            case DARK_OAK_LOG_X -> Blocks.DARK_OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
            case DARK_OAK_LOG_Z -> Blocks.DARK_OAK_LOG.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z);
            case DEAD_BUSH -> Blocks.DEAD_BUSH.getDefaultState();
            case BONE_BLOCK -> Blocks.BONE_BLOCK.getDefaultState();
            case BONE_BLOCK_X -> Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.X);
            case BONE_BLOCK_Z -> Blocks.BONE_BLOCK.getDefaultState().with(PillarBlock.AXIS, Direction.Axis.Z);
            case SKELETON_SKULL -> Blocks.SKELETON_SKULL.getDefaultState();
            case COBBLED_DEEPSLATE_WALL -> Blocks.COBBLED_DEEPSLATE_WALL.getDefaultState();
            case SOUL_LANTERN -> Blocks.SOUL_LANTERN.getDefaultState();
            case SOUL_LANTERN_HANGING -> Blocks.SOUL_LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
            case POLISHED_BLACKSTONE_BRICKS -> Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
            case CHAIN -> Blocks.CHAIN.getDefaultState();
            case STONE_BRICK_WALL -> Blocks.STONE_BRICK_WALL.getDefaultState();
            case DEEPSLATE_BRICKS -> Blocks.DEEPSLATE_BRICKS.getDefaultState();
            case CRACKED_DEEPSLATE_BRICKS -> Blocks.CRACKED_DEEPSLATE_BRICKS.getDefaultState();
            case BROWN_MUSHROOM -> Blocks.BROWN_MUSHROOM.getDefaultState();
            case NETHER_WART_BLOCK -> Blocks.NETHER_WART_BLOCK.getDefaultState();
            case RED_MUSHROOM_BLOCK -> Blocks.RED_MUSHROOM_BLOCK.getDefaultState();
            case CRIMSON_HYPHAE -> Blocks.CRIMSON_HYPHAE.getDefaultState();
            case MAGMA_BLOCK -> Blocks.MAGMA_BLOCK.getDefaultState();
            case PEARLESCENT_FROGLIGHT -> Blocks.PEARLESCENT_FROGLIGHT.getDefaultState();
            case DRIP_DOWN_TIP -> drip(Direction.DOWN, Thickness.TIP);
            case DRIP_DOWN_FRUSTUM -> drip(Direction.DOWN, Thickness.FRUSTUM);
            case DRIP_DOWN_BASE -> drip(Direction.DOWN, Thickness.BASE);
            case DRIP_UP_TIP -> drip(Direction.UP, Thickness.TIP);
            case DRIP_UP_FRUSTUM -> drip(Direction.UP, Thickness.FRUSTUM);
            case DRIP_UP_BASE -> drip(Direction.UP, Thickness.BASE);
            case MUD -> Blocks.MUD.getDefaultState();
            case MANGROVE_LOG -> Blocks.MANGROVE_LOG.getDefaultState();
            case MANGROVE_ROOTS_WET -> Blocks.MANGROVE_ROOTS.getDefaultState().with(Properties.WATERLOGGED, true);
            case STONE_BRICKS -> Blocks.STONE_BRICKS.getDefaultState();
            case MOSSY_STONE_BRICKS -> Blocks.MOSSY_STONE_BRICKS.getDefaultState();
            case CRACKED_STONE_BRICKS -> Blocks.CRACKED_STONE_BRICKS.getDefaultState();
            case MOSSY_COBBLESTONE -> Blocks.MOSSY_COBBLESTONE.getDefaultState();
            case LILY_PAD -> Blocks.LILY_PAD.getDefaultState();
            case SEAGRASS -> Blocks.SEAGRASS.getDefaultState();
            case SPRUCE_FENCE -> Blocks.SPRUCE_FENCE.getDefaultState();
            case SPRUCE_FENCE_WET -> Blocks.SPRUCE_FENCE.getDefaultState().with(Properties.WATERLOGGED, true);
            case LANTERN -> Blocks.LANTERN.getDefaultState();
            case LANTERN_HANGING -> Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);
        };
    }

    /** A BlockSink that builds straight into a world. {@code prepare} makes sure chunks are decorated first. */
    public static BlockSink sink(ServerWorld world, Realm realm) {
        return new BlockSink() {
            private final BlockPos.Mutable m = new BlockPos.Mutable();

            @Override
            public void set(int x, int y, int z, B block) {
                world.setBlockState(m.set(x, y, z), state(block), Block.NOTIFY_LISTENERS);
            }

            @Override
            public B get(int x, int y, int z) {
                BlockState s = world.getBlockState(m.set(x, y, z));
                if (s.isAir()) return B.AIR;
                if (s.isOf(Blocks.WATER)) return B.WATER;
                return B.STONE;
            }

            @Override
            public void prepare(int x, int z) {
                Dimensions.ensureDecorated(world, realm, x >> 4, z >> 4);
            }
        };
    }
}
