package blusunrize.immersiveengineering.common.blocks.metal;

import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.IEProperties.PropertyBoolInverted;
import blusunrize.immersiveengineering.api.TargetingInfo;
import blusunrize.immersiveengineering.api.energy.wires.IImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler;
import blusunrize.immersiveengineering.api.energy.wires.ImmersiveNetHandler.Connection;
import blusunrize.immersiveengineering.api.energy.wires.TileEntityImmersiveConnectable;
import blusunrize.immersiveengineering.api.energy.wires.WireType;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IActiveState;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IDirectionalTile;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHammerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IPlayerInteraction;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IUsesBooleanProperty;
import blusunrize.immersiveengineering.common.util.ChatUtils;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;
import net.minecraft.util.EnumFacing.Axis;

public class TileEntityBreakerSwitch extends TileEntityImmersiveConnectable implements IBlockBounds, IDirectionalTile, IActiveState, IHammerInteraction, IPlayerInteraction
{
	public int sideAttached=0;
	public EnumFacing facing=EnumFacing.NORTH;
	public int wires = 0;
	public boolean active=false;
	public boolean inverted=false;

	@Override
	protected boolean canTakeLV()
	{
		return true;
	}
	@Override
	protected boolean canTakeMV()
	{
		return true;
	}
	@Override
	protected boolean canTakeHV()
	{
		return false;
	}

	@Override
	public boolean allowEnergyToPass(Connection con)
	{
		return active;
	}

	@Override
	public boolean canConnectCable(WireType cableType, TargetingInfo target)
	{
		if(cableType!=null && !cableType.isEnergyWire())
			return false;
		if(cableType==WireType.STEEL&&!canTakeHV())
			return false;
		if(wires>=2)
			return false;
		return limitType==null || cableType==limitType;
	}
	@Override
	public void connectCable(WireType cableType, TargetingInfo target)
	{
		if(this.limitType==null)
			this.limitType = cableType;
		wires++;
	}
	@Override
	public WireType getCableLimiter(TargetingInfo target)
	{
		return limitType;
	}
	@Override
	public void removeCable(Connection connection)
	{
		WireType type = connection!=null?connection.cableType:null;
		if(type==null)
			wires = 0;
		else
			wires--;
		if(wires<=0)
			limitType=null;
		worldObj.markBlockForUpdate(getPos());
	}

	@Override
	public void writeCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.writeCustomNBT(nbt, descPacket);
		nbt.setInteger("facing", facing.ordinal());
		nbt.setInteger("sideAttached", sideAttached);
		nbt.setInteger("wires", wires);
		nbt.setBoolean("active", active);
		nbt.setBoolean("inverted", inverted);
	}
	@Override
	public void readCustomNBT(NBTTagCompound nbt, boolean descPacket)
	{
		super.readCustomNBT(nbt, descPacket);
		facing = EnumFacing.getFront(nbt.getInteger("facing"));
		sideAttached = nbt.getInteger("sideAttached");
		wires = nbt.getInteger("wires");
		active = nbt.getBoolean("active");
		inverted = nbt.getBoolean("inverted");
	}

	@Override
	public Vec3 getRaytraceOffset(IImmersiveConnectable link)
	{
		return new Vec3(.5,.5,.5);
	}
	@Override
	public Vec3 getConnectionOffset(Connection con)
	{
		BlockPos here = getPos();
		if(facing.getAxis()==Axis.Y)
		{
			int xDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(here)&&con.end!=null)? con.end.getX()-here.getX(): (con.end.equals(here)&& con.start!=null)?con.start.getX()-here.getX(): 0;
			return new Vec3(xDif<0?.25:.75, facing==EnumFacing.UP?.875:.125, .5);
		}
		else
		{
			int xDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(here)&&con.end!=null)? con.end.getX()-here.getX(): (con.end.equals(here)&& con.start!=null)?con.start.getX()-here.getX(): 0;
			int zDif = (con==null||con.start==null||con.end==null)?0: (con.start.equals(here)&&con.end!=null)? con.end.getZ()-here.getZ(): (con.end.equals(here)&& con.start!=null)?con.start.getZ()-here.getZ(): 0;

			return new Vec3(facing==EnumFacing.WEST?.125:facing==EnumFacing.EAST?.875:xDif<0?.25:.75, .5, facing==EnumFacing.NORTH?.125:facing==EnumFacing.SOUTH?.875:zDif<0?.25:.75);
		}
	}

	@Override
	public boolean hammerUseSide(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		inverted = !inverted;
		ChatUtils.sendServerNoSpamMessages(player, new ChatComponentTranslation(Lib.CHAT_INFO+"rsSignal."+(inverted?"invertedOn":"invertedOff")));
		notifyNeighbours();
		return true;
	}
	@Override
	public boolean interact(EnumFacing side, EntityPlayer player, float hitX, float hitY, float hitZ)
	{
		active = !active;
		ImmersiveNetHandler.INSTANCE.resetCachedIndirectConnections();
		worldObj.addBlockEvent(getPos(), getBlockType(), active?1:0, 0);
		notifyNeighbours();
		return true;
	}
	public void notifyNeighbours()
	{
		markDirty();
		worldObj.notifyNeighborsOfStateChange(getPos(), getBlockType());
		for(EnumFacing f : EnumFacing.VALUES)
			worldObj.notifyNeighborsOfStateChange(getPos().offset(f), getBlockType());
	}

	@Override
	public boolean receiveClientEvent(int id, int arg)
	{
		if(super.receiveClientEvent(id, arg))
			return true;
		this.active = id==1;
		this.worldObj.markBlockForUpdate(getPos());
		return true;
	}
	@Override
	public PropertyBoolInverted getBoolProperty(Class<? extends IUsesBooleanProperty> inf)
	{
		return IEProperties.BOOLEANS[0];
	}
	@Override
	public boolean getIsActive()
	{
		return inverted?!active:active;
	}
	@Override
	public EnumFacing getFacing()
	{
		return facing;
	}
	@Override
	public void setFacing(EnumFacing facing)
	{
		this.facing = facing;
	}
	@Override
	public int getFacingLimitation()
	{
		return 0;
	}
	@Override
	public boolean mirrorFacingOnPlacement(EntityLivingBase placer)
	{
		return true;
	}
	@Override
	public boolean canHammerRotate(EnumFacing side, float hitX, float hitY, float hitZ, EntityLivingBase entity)
	{
		return false;
	}

	@Override
	public float[] getBlockBounds()
	{
		if(facing==EnumFacing.DOWN)
			return new float[]{.25f,0,.1875f, .75f,.5f,.8125f};
		if(facing==EnumFacing.UP)
			return new float[]{.25f,.5f,.1875f, .75f,1,.8125f};

		return new float[]{facing==EnumFacing.WEST?0:facing==EnumFacing.EAST?.5f:.25f,.1875f,facing==EnumFacing.NORTH?0:facing==EnumFacing.SOUTH?.5f:.25f, facing==EnumFacing.WEST?.5f:facing==EnumFacing.EAST?1:.75f,.8125f,facing==EnumFacing.NORTH?.5f:facing==EnumFacing.SOUTH?1:.75f};
	}
	@Override
	public float[] getSpecialCollisionBounds()
	{
		return null;
	}
	@Override
	public float[] getSpecialSelectionBounds()
	{
		return null;
	}
}