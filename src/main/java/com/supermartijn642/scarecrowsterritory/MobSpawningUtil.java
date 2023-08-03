package com.supermartijn642.scarecrowsterritory;

import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.server.TicketManager;
import net.minecraft.world.spawner.WorldEntitySpawner;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Random;

/**
 * Created 1/14/2021 by SuperMartijn642
 */
public class MobSpawningUtil {

    private static final Field distanceManager;

    private static final Method getRandomPosWithin;
    private static final Method getSpawnList;
    private static final Method getSpawnList2;

    static{
        distanceManager = ReflectionUtil.findField(ServerChunkProvider.class, "field_217240_d");

        getRandomPosWithin = ReflectionUtil.findMethod(WorldEntitySpawner.class, "func_222262_a", World.class, Chunk.class);
        getSpawnList = ReflectionUtil.findMethod(WorldEntitySpawner.class, "getSpawnList", ChunkGenerator.class, EntityClassification.class, Random.class, BlockPos.class, World.class);
        getSpawnList2 = ReflectionUtil.findMethod(WorldEntitySpawner.class, "getSpawnList", ChunkGenerator.class, EntityClassification.class, Biome.SpawnListEntry.class, BlockPos.class, World.class);
    }

    /**
     * {@link ServerChunkProvider#distanceManager}
     */
    private static TicketManager getDistanceManager(ServerChunkProvider serverChunkProvider){
        try{
            return (TicketManager)distanceManager.get(serverChunkProvider);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#getSpawnList(ChunkGenerator, EntityClassification, Random, BlockPos, World)}
     */
    private static Biome.SpawnListEntry getSpawnList(ChunkGenerator<?> chunkGenerator, EntityClassification classification, Random random, BlockPos pos, World level){
        try{
            return (Biome.SpawnListEntry)getSpawnList.invoke(null, chunkGenerator, classification, random, pos, level);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#getSpawnList(ChunkGenerator, EntityClassification, Biome.SpawnListEntry, BlockPos, World)}
     */
    private static boolean getSpawnList(ChunkGenerator<?> chunkGenerator, EntityClassification classification, Biome.SpawnListEntry spawnListEntry, BlockPos pos, World level){
        try{
            return (boolean)getSpawnList2.invoke(null, chunkGenerator, classification, spawnListEntry, pos, level);
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }

    /**
     * {@link WorldEntitySpawner#getRandomPosWithin(World, Chunk)}
     */
    private static BlockPos getRandomPosWithin(World level, Chunk chunk){
        try{
            return (BlockPos)getRandomPosWithin.invoke(null, level, chunk);
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnForChunk(ServerWorld, Chunk, WorldEntitySpawner.EntityDensityManager, boolean, boolean, boolean)}
     */
    public static void spawnEntitiesInChunk(ServerWorld level, Chunk chunk, boolean spawnPassives, boolean spawnHostiles, boolean spawnAnimals){
        level.getProfiler().push("spawner");

        for(EntityClassification classification : EntityClassification.values()){
            if(classification != EntityClassification.MISC &&
                (spawnPassives || !classification.isFriendly()) &&
                (spawnHostiles || classification.isFriendly()) &&
                (spawnAnimals || !classification.isPersistent()) &&
                canSpawnForCategory(classification, level)){

                spawnCategoryForChunk(classification, level, chunk);
            }
        }

        level.getProfiler().pop();
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForChunk(EntityClassification, ServerWorld, Chunk, BlockPos)}
     */
    private static void spawnCategoryForChunk(EntityClassification classification, ServerWorld level, Chunk chunk){
        BlockPos blockpos = getRandomPosWithin(level, chunk);
        if(blockpos != null && blockpos.getY() >= 1){
            spawnCategoryForPosition(classification, level, chunk, blockpos);
        }
    }

    /**
     * {@link WorldEntitySpawner#spawnCategoryForChunk(EntityClassification, ServerWorld, Chunk, BlockPos)}
     */
    private static void spawnCategoryForPosition(EntityClassification classification, ServerWorld level, Chunk chunk, BlockPos pos){
        ChunkGenerator<?> chunkgenerator = level.getChunkSource().getGenerator();
        int y = pos.getY();
        BlockState blockstate = chunk.getBlockState(pos);
        if(!blockstate.isRedstoneConductor(chunk, pos)){
            BlockPos.Mutable spawnPos = new BlockPos.Mutable();
            int entitiesSpawned = 0;

            // try spawning a group 3 times
            for(int k = 0; k < 3; ++k){
                int spawnX = pos.getX();
                int spawnZ = pos.getZ();
                Biome.SpawnListEntry spawner = null;
                ILivingEntityData entityData = null;
                int groupSize = MathHelper.ceil(Math.random() * 4.0D);
                int entitiesInGroup = 0;

                // try spawning entities in the group
                for(int i2 = 0; i2 < groupSize; ++i2){
                    spawnX += level.random.nextInt(6) - level.random.nextInt(6);
                    spawnZ += level.random.nextInt(6) - level.random.nextInt(6);
                    spawnPos.set(spawnX, y, spawnZ);
                    double spawnXCenter = (double)spawnX + 0.5D;
                    double spawnZCenter = (double)spawnZ + 0.5D;

                    if(level.getSharedSpawnPos().closerThan(new Vec3d(spawnXCenter, y, spawnZCenter), 24.0D))
                        continue;

                    if(!ScarecrowTracker.isScarecrowInRange(level, new Vec3d(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), ScarecrowsTerritoryConfig.passiveMobRange.get()))
                        continue;

                    ChunkPos chunkpos = new ChunkPos(spawnPos);
                    if(!Objects.equals(chunkpos, chunk.getPos()) && !level.getChunkSource().isEntityTickingChunk(chunkpos))
                        continue;

                    if(spawner == null){
                        spawner = getSpawnList(chunkgenerator, classification, level.random, spawnPos, level);
                        if(spawner == null)
                            break;

                        groupSize = spawner.minCount + level.random.nextInt(1 + spawner.maxCount - spawner.minCount);
                    }

                    if(spawner.type.getCategory() == EntityClassification.MISC)
                        continue;

                    EntityType<?> entityType = spawner.type;
                    if(!entityType.canSummon() || !getSpawnList(chunkgenerator, classification, spawner, spawnPos, level))
                        continue;

                    EntitySpawnPlacementRegistry.PlacementType placementType = EntitySpawnPlacementRegistry.getPlacementType(entityType);
                    if(WorldEntitySpawner.isSpawnPositionOk(placementType, level, spawnPos, entityType) && EntitySpawnPlacementRegistry.checkSpawnRules(entityType, level, SpawnReason.NATURAL, spawnPos, level.random) && level.noCollision(entityType.getAABB(spawnXCenter, y, spawnZCenter))){

                        MobEntity entity;
                        try{
                            Entity e = entityType.create(level);
                            if(!(e instanceof MobEntity)){
                                throw new IllegalStateException("Trying to spawn a non-mob: " + Registry.ENTITY_TYPE.getKey(entityType));
                            }

                            entity = (MobEntity)e;
                        }catch(Exception exception){
                            System.err.println("Failed to create mob");
                            exception.printStackTrace();
                            return;
                        }

                        entity.getPersistentData().putBoolean("spawnedByScarecrow", true);
                        entity.moveTo(spawnXCenter, y, spawnZCenter, level.random.nextFloat() * 360.0F, 0.0F);
                        int canSpawn = net.minecraftforge.common.ForgeHooks.canEntitySpawn(entity, level, spawnXCenter, y, spawnZCenter, null, SpawnReason.NATURAL);
                        if(canSpawn != -1 && (canSpawn == 1 || (entity.checkSpawnRules(level, SpawnReason.NATURAL) && entity.checkSpawnObstruction(level)))){
                            if(!net.minecraftforge.event.ForgeEventFactory.doSpecialSpawn(entity, level, (float)spawnXCenter, (float)y, (float)spawnZCenter, null, SpawnReason.NATURAL))
                                entityData = entity.finalizeSpawn(level, level.getCurrentDifficultyAt(entity.getCommandSenderBlockPosition()), SpawnReason.NATURAL, entityData, null);
                            entitiesSpawned++;
                            entitiesInGroup++;
                            level.addFreshEntity(entity);
                            if(entitiesSpawned >= net.minecraftforge.event.ForgeEventFactory.getMaxSpawnPackSize(entity))
                                return;

                            if(entity.isMaxGroupSizeReached(entitiesInGroup))
                                break;
                        }
                    }
                }
            }
        }
    }

    private static boolean canSpawnForCategory(EntityClassification classification, ServerWorld level){
        int spawnableChunks = Math.max(1, getDistanceManager(level.getChunkSource()).getNaturalSpawnChunkCount() / (17 * 17));
        return level.getMobCategoryCounts().getInt(classification) < classification.getMaxInstancesPerChunk() * spawnableChunks;
    }
}
