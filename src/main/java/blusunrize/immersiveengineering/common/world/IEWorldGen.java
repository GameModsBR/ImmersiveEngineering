package blusunrize.immersiveengineering.common.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.google.common.collect.ArrayListMultimap;

import blusunrize.immersiveengineering.common.Config;
import blusunrize.immersiveengineering.common.util.IELogger;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockHelper;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.feature.WorldGenMinable;
import net.minecraftforge.event.world.ChunkDataEvent;
import net.minecraftforge.fml.common.IWorldGenerator;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;

public class IEWorldGen implements IWorldGenerator
{
	public static class OreGen
	{
		String name;
		WorldGenMinable mineableGen;
		int minY;
		int maxY;
		int chunkOccurence;
		int weight;
		public OreGen(String name, IBlockState state, int maxVeinSize, Block replaceTarget, int minY, int maxY,int chunkOccurence, int weight)
		{
			this.name = name;
			this.mineableGen = new WorldGenMinable(state, maxVeinSize, BlockHelper.forBlock(replaceTarget));
			this.minY=minY;
			this.maxY=maxY;
			this.chunkOccurence=chunkOccurence;
			this.weight=weight;
		}

		public void generate(World world, Random rand, int x, int z)
		{
			BlockPos pos;
			for(int i=0; i<chunkOccurence; i++)
				if(rand.nextInt(100)<weight)
				{
					pos = new BlockPos(x + rand.nextInt(16), minY + rand.nextInt(maxY - minY), z + rand.nextInt(16));
					mineableGen.generate(world, rand, pos);
				}
		}
	}
	public static ArrayList<OreGen> orespawnList = new ArrayList();
	public static ArrayList<Integer> oreDimBlacklist = new ArrayList();
	public static OreGen addOreGen(String name, IBlockState state, int maxVeinSize, int minY, int maxY,int chunkOccurence,int weight)
	{
		OreGen gen = new OreGen(name, state, maxVeinSize, Blocks.stone, minY, maxY, chunkOccurence, weight);
		orespawnList.add(gen);
		return gen;
	}

	@Override
	public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator, IChunkProvider chunkProvider)
	{
		this.generateOres(random, chunkX, chunkZ, world, true);
	}

	public void generateOres(Random random, int chunkX, int chunkZ, World world, boolean newGeneration)
	{
		if(!oreDimBlacklist.contains(world.provider.getDimensionId()))
			for(OreGen gen : orespawnList)
				if(newGeneration || Config.getBoolean("retrogen_"+gen.name))
					gen.generate(world, random, chunkX*16, chunkZ*16);
	}

	@SubscribeEvent
	public void chunkSave(ChunkDataEvent.Save event)
	{
		NBTTagCompound nbt = new NBTTagCompound();
		event.getData().setTag("ImmersiveEngineering", nbt);
		nbt.setBoolean(Config.getString("retrogen_key"), true);
	}

	@SubscribeEvent
	public void chunkLoad(ChunkDataEvent.Load event)
	{
		int dimension = event.world.provider.getDimensionId();
		if((!event.getData().getCompoundTag("ImmersiveEngineering").hasKey(Config.getString("retrogen_key"))) && (Config.getBoolean("retrogen_copper")||Config.getBoolean("retrogen_bauxite")||Config.getBoolean("retrogen_lead")||Config.getBoolean("retrogen_silver")||Config.getBoolean("retrogen_nickel")||Config.getBoolean("retrogen_uranium")))
		{
			if(Config.getBoolean("retrogen_log_flagChunk"))
				IELogger.info("Chunk "+event.getChunk().getChunkCoordIntPair()+" has been flagged for Ore RetroGeneration by IE.");
			retrogenChunks.put(dimension, event.getChunk().getChunkCoordIntPair());
		}
	}

	public static ArrayListMultimap<Integer, ChunkCoordIntPair> retrogenChunks = ArrayListMultimap.create();
	@SubscribeEvent
	public void serverWorldTick(TickEvent.WorldTickEvent event)
	{
		if(event.side==Side.CLIENT || event.phase==TickEvent.Phase.START)
			return;
		int dimension = event.world.provider.getDimensionId();
		int counter = 0;
		List<ChunkCoordIntPair> chunks = retrogenChunks.get(dimension);
		if(chunks!=null && chunks.size()>0)
			for(int i=0; i<2; i++)
			{
				chunks = retrogenChunks.get(dimension);
				if(chunks == null || chunks.size()<= 0)
					break;
				counter++;
				ChunkCoordIntPair loc = (ChunkCoordIntPair)chunks.get(0);
				long worldSeed = event.world.getSeed();
				Random fmlRandom = new Random(worldSeed);
				long xSeed = (fmlRandom.nextLong()>>3);
				long zSeed = (fmlRandom.nextLong()>>3);
				fmlRandom.setSeed(xSeed * loc.chunkXPos + zSeed * loc.chunkZPos ^ worldSeed);
				this.generateOres(fmlRandom, loc.chunkXPos, loc.chunkZPos, event.world, false);
				chunks.remove(0);
			}
		if(counter>0 && Config.getBoolean("retrogen_log_remaining"))
			IELogger.info("Retrogen was performed on "+counter+" Chunks, "+Math.max(0,chunks.size())+" chunks remaining");
	}
}