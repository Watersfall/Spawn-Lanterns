package ladysnake.spawnlanterns.common;

import ladysnake.spawnlanterns.common.block.CryingLanternBlock;
import ladysnake.spawnlanterns.common.block.SoothingLanternBlock;
import ladysnake.spawnlanterns.common.effect.SpawnStatusEffect;
import ladysnake.spawnlanterns.common.entity.CryingLanternBlockEntity;
import ladysnake.spawnlanterns.common.entity.SoothingLanternBlockEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.LanternBlock;
import net.minecraft.block.Material;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.collection.WeightedPicker;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.util.logging.Level.CONFIG;

public class SpawnLanterns implements ModInitializer {
    public static final String MODID = "spawnlanterns";

    public static Block SOOTHING_LANTERN;
    public static Block CRYING_LANTERN;

    public static StatusEffect SOOTHING;
    public static StatusEffect PROVOCATION;

    public static BlockEntityType<SoothingLanternBlockEntity> SOOTHING_LANTERN_BE;
    public static BlockEntityType<CryingLanternBlockEntity> CRYING_LANTERN_BE;

    @Override
    public void onInitialize() {
        // register lanterns
        SOOTHING_LANTERN = registerBlock(new SoothingLanternBlock(FabricBlockSettings.of(Material.METAL).requiresTool().strength(3.5F).sounds(BlockSoundGroup.LANTERN).lightLevel(10).nonOpaque()), "soothing_lantern", ItemGroup.DECORATIONS);
        CRYING_LANTERN = registerBlock(new CryingLanternBlock(FabricBlockSettings.of(Material.METAL).requiresTool().strength(3.5F).sounds(BlockSoundGroup.LANTERN).lightLevel(10).nonOpaque()), "crying_lantern", ItemGroup.DECORATIONS);

        // register status effects
        SOOTHING = registerStatusEffect(new SpawnStatusEffect(StatusEffectType.BENEFICIAL, 15747468), "soothing");
        PROVOCATION = registerStatusEffect(new SpawnStatusEffect(StatusEffectType.BENEFICIAL, 5851632), "provocation");

        // register block entities
        SOOTHING_LANTERN_BE = Registry.register(Registry.BLOCK_ENTITY_TYPE, MODID + ":" + "soothing_lantern", BlockEntityType.Builder.create(SoothingLanternBlockEntity::new, SOOTHING_LANTERN).build(null));
        CRYING_LANTERN_BE = Registry.register(Registry.BLOCK_ENTITY_TYPE, MODID + ":" + "crying_lantern", BlockEntityType.Builder.create(CryingLanternBlockEntity::new, CRYING_LANTERN).build(null));

        // register usables on soul lanterns and their effects
        UseBlockCallback.EVENT.register((playerEntity, world, hand, blockHitResult) -> {
            if (world.getBlockState(blockHitResult.getBlockPos()).getBlock() == Blocks.SOUL_LANTERN) {
                if (playerEntity.getStackInHand(hand).getItem() == Items.HONEY_BOTTLE) {
                    // consume the item
                    if (!playerEntity.abilities.creativeMode) {
                        playerEntity.getStackInHand(hand).decrement(1);
                        ItemStack glassBottleStack = new ItemStack(Items.GLASS_BOTTLE);
                        if (!playerEntity.inventory.insertStack(glassBottleStack)) {
                            playerEntity.dropItem(glassBottleStack, false);
                        }
                    }

                    // change the lantern
                    world.setBlockState(blockHitResult.getBlockPos(),
                            SOOTHING_LANTERN.getDefaultState()
                                    .with(LanternBlock.HANGING, world.getBlockState(blockHitResult.getBlockPos()).get(LanternBlock.HANGING))
                                    .with(LanternBlock.field_26441, world.getBlockState(blockHitResult.getBlockPos()).get(LanternBlock.field_26441)));

                    // play sounds
                    world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 2.0f);

                    // display particle effects
                    if (world.isClient) {
                        for(int i = 0; i < 20; ++i) {
                            double d = world.random.nextGaussian() * 0.02D;
                            double e = world.random.nextGaussian() * 0.02D;
                            double f = world.random.nextGaussian() * 0.02D;
                            double g = 10.0D;
                            world.addParticle(ParticleTypes.WITCH,
                                    (blockHitResult.getBlockPos().getX() + 0.5f) - d * 10.0D,
                                    (blockHitResult.getBlockPos().getY() + 0.5f) - e * 10.0D,
                                    (blockHitResult.getBlockPos().getZ() + 0.5f) - f * 10.0D, d, e, f);
                        }
                    }

                    // return
                    return ActionResult.SUCCESS;
                } else if (playerEntity.getStackInHand(hand).getItem() == Items.CRYING_OBSIDIAN) {
                    // consume the item
                    if (!playerEntity.abilities.creativeMode) {
                        playerEntity.getStackInHand(hand).decrement(1);
                        ItemStack obsidianStack = new ItemStack(Items.OBSIDIAN);
                        if (!playerEntity.inventory.insertStack(obsidianStack)) {
                            playerEntity.dropItem(obsidianStack, false);
                        }
                    }

                    // change the lantern
                    world.setBlockState(blockHitResult.getBlockPos(),
                            CRYING_LANTERN.getDefaultState()
                                    .with(LanternBlock.HANGING, world.getBlockState(blockHitResult.getBlockPos()).get(LanternBlock.HANGING))
                                    .with(LanternBlock.field_26441, world.getBlockState(blockHitResult.getBlockPos()).get(LanternBlock.field_26441)));

                    // play sounds
                    world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.ITEM_BOTTLE_EMPTY, SoundCategory.PLAYERS, 1.0f, 1.0f);
                    world.playSound(playerEntity, blockHitResult.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.BLOCKS, 1.0f, 2.0f);

                    // display particle effects
                    if (world.isClient) {
                        for(int i = 0; i < 20; ++i) {
                            double d = world.random.nextGaussian() * 0.02D;
                            double e = world.random.nextGaussian() * 0.02D;
                            double f = world.random.nextGaussian() * 0.02D;
                            double g = 10.0D;
                            world.addParticle(ParticleTypes.WITCH,
                                    (blockHitResult.getBlockPos().getX() + 0.5f) - d * 10.0D,
                                    (blockHitResult.getBlockPos().getY() + 0.5f) - e * 10.0D,
                                    (blockHitResult.getBlockPos().getZ() + 0.5f) - f * 10.0D, d, e, f);
                        }
                    }

                    // return
                    return ActionResult.SUCCESS;
                }
                return ActionResult.PASS;
            }
            return ActionResult.PASS;
        });

        // provocation spawn tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            server.getWorlds().forEach(world -> {
                if (world.random.nextInt(500) == 0) {
                    getLoadedChunks(world).forEach(chunk -> {
                        ChunkPos pos = chunk.getPos();
                        if (world.getEntitiesByClass(HostileEntity.class, new Box(pos.getStartPos(), pos.getStartPos().add(16, 256, 16)), e -> true).size() < 3) {
                            int randomX = world.random.nextInt(16);
                            int randomZ = world.random.nextInt(16);
                            ChunkPos chunkPos = chunk.getPos();

                            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING, chunkPos.getStartX() + randomX, chunkPos.getStartZ() + randomZ);
                            BlockPos spawnPos = new BlockPos(chunkPos.getStartX() + randomX, y, chunkPos.getStartZ() + randomZ);

                            PlayerEntity closestPlayer = world.getClosestPlayer(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(), 999999, false);
                            if (closestPlayer != null && closestPlayer.hasStatusEffect(SpawnLanterns.PROVOCATION)) {
                                SpawnSettings.SpawnEntry spawnEntry = pickRandomSpawnEntry(
                                        world.getChunkManager().getChunkGenerator(),
                                        SpawnGroup.MONSTER,
                                        world.getRandom(),
                                        spawnPos,
                                        world.getStructureAccessor(),
                                        world.getBiome(spawnPos)
                                );

                                if (spawnEntry != null) {
                                    Entity entity = spawnEntry.type.create(world);

                                    if (entity != null) {
                                        entity.setPos(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                                        entity.updateTrackedPosition(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                                        entity.updatePosition(spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
                                        world.spawnEntity(entity);
                                    }
                                }
                            }
                        }
                    });
                }
            });
        });
    }

    private static SpawnSettings.SpawnEntry pickRandomSpawnEntry(ChunkGenerator chunkGenerator, SpawnGroup spawnGroup, Random random, BlockPos pos, StructureAccessor accessor, Biome biome) {
        List<SpawnSettings.SpawnEntry> list = chunkGenerator.getEntitySpawnList(biome, accessor, spawnGroup, pos);
        return list.isEmpty() ? null : WeightedPicker.getRandom(random, list);
    }

    private static Block registerBlock(Block block, String name, ItemGroup itemGroup) {
        Registry.register(Registry.BLOCK, MODID + ":" + name, block);

        if (itemGroup != null) {
            BlockItem item = new BlockItem(block, new Item.Settings().group(itemGroup));
            item.appendBlocks(Item.BLOCK_ITEMS, item);
            SpawnLanterns.registerItem(item, name);
        }

        return block;
    }

    public static Item registerItem(Item item, String name) {
        Registry.register(Registry.ITEM, SpawnLanterns.MODID + ":" + name, item);

        return item;
    }

    private static StatusEffect registerStatusEffect(StatusEffect statusEffect, String name) {
        Registry.register(Registry.STATUS_EFFECT, MODID + ":" + name, statusEffect);

        return statusEffect;
    }

    public static List<WorldChunk> getLoadedChunks(ServerWorld world) {
        ArrayList<WorldChunk> loadedChunks = new ArrayList<>();
        int renderDistance = world.getServer().getPlayerManager().getViewDistance();

        world.getPlayers().forEach(player -> {
            ChunkPos playerChunkPos = new ChunkPos(player.getBlockPos());
            WorldChunk chunk = world.getChunk(playerChunkPos.x, playerChunkPos.z);

            if(!loadedChunks.contains(chunk)) {
                loadedChunks.add(chunk);
            }

            for(int x = -renderDistance; x <= renderDistance; x++) {
                for(int z = -renderDistance; z <= renderDistance; z++) {
                    ChunkPos offsetChunkPos = new ChunkPos(playerChunkPos.x + x, playerChunkPos.z + z);
                    WorldChunk offsetChunk = world.getChunk(offsetChunkPos.x, offsetChunkPos.z);

                    if(!loadedChunks.contains(offsetChunk)) {
                        loadedChunks.add(offsetChunk);
                    }
                }
            }
        });

        return loadedChunks;
    }
}
