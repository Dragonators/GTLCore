package org.gtlcore.gtlcore.mixin.stargatejourney.stargate;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.phys.AABB;
import net.povstalec.sgjourney.StargateJourney;
import net.povstalec.sgjourney.common.blocks.dhd.AbstractDHDBlock;
import net.povstalec.sgjourney.common.init.BlockInit;
import net.povstalec.sgjourney.common.structures.SGJourneyStructure;
import net.povstalec.sgjourney.common.structures.StargateStructure;

import committee.nova.mods.avaritia.init.registry.ModItems;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("all")
@Mixin(StargateStructure.class)
public abstract class StargateStructureMixin extends SGJourneyStructure {

    @Unique
    private static final ResourceLocation gTLCore$LANTEA = new ResourceLocation(StargateJourney.MODID, "lantea");
    @Unique
    private static final ResourceLocation gTLCore$ABYDOS = new ResourceLocation(StargateJourney.MODID, "abydos");
    @Unique
    private static final ResourceLocation gTLCore$CHULAK = new ResourceLocation(StargateJourney.MODID, "chulak");
    @Unique
    private static final ResourceLocation gTLCore$CAVUM_TENEBRAE = new ResourceLocation(StargateJourney.MODID, "cavum_tenebrae");

    public StargateStructureMixin(StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<ResourceLocation> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, Optional<Boolean> commonStargates) {
        super(config, startPool, startJigsawName, size, startHeight, projectStartToHeightmap, maxDistanceFromCenter, commonStargates);
    }

    @Shadow(remap = false)
    public static int getX(long seed) {
        throw new AssertionError();
    }

    @Shadow(remap = false)
    public static int getZ(long seed) {
        throw new AssertionError();
    }

    /**
     * @author Dragons
     * @reason Ban Dimensions
     */
    @Overwrite(remap = false)
    protected boolean extraSpawningChecks(Structure.GenerationContext context) {
        Holder<Biome> biome = context.biomeSource().getNoiseBiome(
                context.chunkPos().getMinBlockX() >> 2,
                0,
                context.chunkPos().getMinBlockZ() >> 2,
                context.randomState().sampler());

        if (gTLCore$isDimensionBiome(biome, context)) return false;

        ChunkPos chunkpos = context.chunkPos();
        long seed = context.seed();

        return chunkpos.x == getX(seed) && chunkpos.z == getZ(seed);
    }

    @Override
    public void afterPlace(WorldGenLevel level, StructureManager structureManager, ChunkGenerator chunkGenerator, RandomSource randomSource,
                           BoundingBox boundingBox, ChunkPos chunkPos, PiecesContainer piecesContainer) {
        super.afterPlace(level, structureManager, chunkGenerator, randomSource, boundingBox, chunkPos, piecesContainer);

        ResourceLocation dimensionLocation = level.getLevel().dimension().location();
        if (gTLCore$isTargetDimension(dimensionLocation)) {
            gTLCore$createDHDItemFrames(level, boundingBox, randomSource);
        }
    }

    @Unique
    private static boolean gTLCore$isTargetDimension(ResourceLocation dimensionLocation) {
        return dimensionLocation.equals(gTLCore$LANTEA) ||
                dimensionLocation.equals(gTLCore$ABYDOS) ||
                dimensionLocation.equals(gTLCore$CHULAK) ||
                dimensionLocation.equals(gTLCore$CAVUM_TENEBRAE);
    }

    @Unique
    private void gTLCore$createDHDItemFrames(WorldGenLevel level, BoundingBox boundingBox, RandomSource randomSource) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int x = boundingBox.minX(); x <= boundingBox.maxX(); x++) {
            for (int y = boundingBox.minY(); y <= boundingBox.maxY(); y++) {
                for (int z = boundingBox.minZ(); z <= boundingBox.maxZ(); z++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);

                    if (state.getBlock() instanceof AbstractDHDBlock) {
                        gTLCore$createItemFrameInFrontOfDHD(level, pos.immutable(), state, randomSource);
                    }
                }
            }
        }
    }

    @Unique
    private void gTLCore$createItemFrameInFrontOfDHD(WorldGenLevel level, BlockPos dhdPos, BlockState dhdState, RandomSource randomSource) {
        ItemStack displayItem = gTLCore$selectDisplayItemForDimension(level, randomSource);

        Direction dhdFacing = dhdState.getValue(AbstractDHDBlock.FACING);
        BlockPos itemFramePos = dhdPos.relative(dhdFacing);

        if (!level.getBlockState(itemFramePos).isAir()) {
            if (displayItem != null && !displayItem.isEmpty()) {
                level.setBlock(itemFramePos, Blocks.AIR.defaultBlockState(), 3);
                level.addFreshEntity(gTLCore$getItemEntity(level, itemFramePos, displayItem));
            }
            return;
        }

        AABB checkArea = new AABB(itemFramePos);
        List<ItemFrame> existingFrames = level.getEntitiesOfClass(ItemFrame.class, checkArea);
        if (!existingFrames.isEmpty()) {
            if (displayItem != null && !displayItem.isEmpty()) {
                level.addFreshEntity(gTLCore$getItemEntity(level, itemFramePos, displayItem));
            }
            return;
        }

        ItemFrame itemFrame = new ItemFrame(level.getLevel(), itemFramePos, Direction.UP);

        if (displayItem != null && !displayItem.isEmpty()) {
            itemFrame.setItem(displayItem, false);
            level.addFreshEntity(itemFrame);
        }
    }

    @Unique
    private static @NotNull ItemEntity gTLCore$getItemEntity(WorldGenLevel level, BlockPos itemFramePos, ItemStack displayItem) {
        double x = itemFramePos.getX() + 0.5;
        double y = itemFramePos.getY() + 0.5;
        double z = itemFramePos.getZ() + 0.5;

        ItemEntity itemEntity = new ItemEntity(level.getLevel(), x, y, z, displayItem);

        itemEntity.setUnlimitedLifetime();
        itemEntity.setInvulnerable(true);

        itemEntity.setDeltaMovement(0, 0, 0);
        itemEntity.setNoGravity(true);
        return itemEntity;
    }

    @Unique
    private ItemStack gTLCore$selectDisplayItemForDimension(WorldGenLevel level, RandomSource randomSource) {
        ResourceLocation dimensionLocation = level.getLevel().dimension().location();

        if (dimensionLocation.equals(gTLCore$LANTEA)) {
            return new ItemStack(BlockInit.UNIVERSE_STARGATE.get().asItem());
        } else if (dimensionLocation.equals(gTLCore$ABYDOS)) {
            return new ItemStack(ModItems.neutron_ring.get());
        } else if (dimensionLocation.equals(gTLCore$CHULAK)) {
            return new ItemStack(ModItems.infinity_umbrella.get());
        } else if (dimensionLocation.equals(gTLCore$CAVUM_TENEBRAE)) {
            return new ItemStack(ModItems.infinity_ring.get());
        } else {
            return null;
        }
    }

    @Unique
    private boolean gTLCore$isDimensionBiome(Holder<Biome> biome, Structure.GenerationContext context) {
        if (biome.is(BiomeTags.IS_OVERWORLD)) return true;

        if (biome.is(BiomeTags.IS_NETHER)) return true;

        if (biome.is(BiomeTags.IS_END)) return true;

        return false;
    }
}
