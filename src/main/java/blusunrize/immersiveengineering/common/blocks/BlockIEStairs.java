package blusunrize.immersiveengineering.common.blocks;

import blusunrize.immersiveengineering.ImmersiveEngineering;
import blusunrize.immersiveengineering.common.IEContent;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.state.IBlockState;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlockIEStairs extends BlockStairs
{
	public boolean hasFlavour = false;
	public boolean isFlammable = false;
	public String name;
	
	public BlockIEStairs(String name, IBlockState state)
	{
		super(state);
		this.name = name;
		this.setUnlocalizedName(ImmersiveEngineering.MODID+"."+name);
		GameRegistry.registerBlock(this, ItemBlockIEStairs.class, name);
		this.setCreativeTab(ImmersiveEngineering.creativeTab);
		this.useNeighborBrightness = true;
		IEContent.registeredIEBlocks.add(this);
	}
	
	public BlockIEStairs setFlammable(boolean b)
	{
		this.isFlammable = b;
		return this;
	}
	
	public BlockIEStairs setHasFlavour(boolean hasFlavour)
	{
		this.hasFlavour = hasFlavour;
		return this;
	}
}