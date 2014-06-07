package resonantinduction.mechanical.energy.turbine;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.material.Material;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import resonant.api.grid.INode;
import resonant.api.grid.INodeProvider;
import resonant.core.ResonantEngine;
import resonant.lib.References;
import resonant.lib.content.module.TileBase;
import resonant.lib.multiblock.IMultiBlockStructure;
import resonant.lib.network.Synced;
import resonant.lib.network.Synced.SyncedInput;
import resonant.lib.network.Synced.SyncedOutput;
import resonantinduction.mechanical.energy.grid.MechanicalNode;
import universalelectricity.api.vector.Vector3;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Reduced version of the main turbine class */
public class TileTurbine extends TileBase implements IMultiBlockStructure<TileTurbine>, INodeProvider
{
    /** Radius of large turbine? */
    public int multiBlockRadius = 1;

    /** Max power in watts. */
    protected long maxPower;

    /** The power of the turbine this tick. In joules/tick */
    public long power = 0;

    /** Current rotation of the turbine in radians. */
    public float rotation = 0;

    protected final long defaultTorque = 5000;
    protected long torque = defaultTorque;
    protected float prevAngularVelocity = 0;

    @Synced(1)
    protected float angularVelocity = 0;
    @Synced
    public int tier = 0;

    protected MechanicalNode mechanicalNode;

    public TileTurbine()
    {
        super(Material.wood);
        mechanicalNode = new TurbineNode(this);
    }

    public ForgeDirection getDirection()
    {
        return ForgeDirection.getOrientation(getBlockMetadata());
    }

    @Override
    public void initiate()
    {
        mechanicalNode.reconstruct();
        super.initiate();        
    }

    @Override
    public void updateEntity()
    {
        super.updateEntity();
        mechanicalNode.update();
        getMultiBlock().update();

        if (getMultiBlock().isPrimary())
        {
            if (!worldObj.isRemote)
            {
                /** Set angular velocity based on power and torque. */
                angularVelocity = (float) ((double) power / torque);

                if (!worldObj.isRemote && ticks % 3 == 0 && prevAngularVelocity != angularVelocity)
                {
                    sendPowerUpdate();
                    prevAngularVelocity = angularVelocity;
                }

                if (power > 0)
                    onProduce();
            }

            if (angularVelocity != 0)
            {
                playSound();

                /** Update rotation. */
                rotation = (float) ((rotation + angularVelocity / 20) % (Math.PI * 2));
            }
        }

        if (!worldObj.isRemote)
            power = 0;
    }

    protected long getMaxPower()
    {
        if (this.getMultiBlock().isConstructed())
        {
            return (long) (maxPower * getArea());
        }

        return maxPower;
    }

    public int getArea()
    {
        return (int) (((multiBlockRadius + 0.5) * 2) * ((multiBlockRadius + 0.5) * 2));
    }

    
    public void onProduce()
    {
        if (!worldObj.isRemote)
        {
            if (mechanicalNode.torque < 0)
                torque = -Math.abs(torque);

            if (mechanicalNode.angularVelocity < 0)
                angularVelocity = -Math.abs(angularVelocity);

            mechanicalNode.apply(this, (torque - mechanicalNode.getTorque()) / 10, (angularVelocity - mechanicalNode.getAngularSpeed()) / 10);
        }
    }

    public void playSound()
    {

    }

    @Override
    public Packet getDescriptionPacket()
    {
        return References.PACKET_ANNOTATION.getPacket(this);
    }

    public void sendPowerUpdate()
    {
        References.PACKET_ANNOTATION.sync(this, 1);
    }

    /** Reads a tile entity from NBT. */
    @Override
    @SyncedInput
    public void readFromNBT(NBTTagCompound nbt)
    {
        super.readFromNBT(nbt);
        multiBlockRadius = nbt.getInteger("multiBlockRadius");
        tier = nbt.getInteger("tier");
        mechanicalNode.load(nbt);
        getMultiBlock().load(nbt);
    }

    /** Writes a tile entity to NBT. */
    @Override
    @SyncedOutput
    public void writeToNBT(NBTTagCompound nbt)
    {
        super.writeToNBT(nbt);
        nbt.setInteger("multiBlockRadius", multiBlockRadius);
        nbt.setInteger("tier", tier);
        mechanicalNode.save(nbt);
        getMultiBlock().save(nbt);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return AxisAlignedBB.getAABBPool().getAABB(this.xCoord - multiBlockRadius, this.yCoord - multiBlockRadius, this.zCoord - multiBlockRadius, this.xCoord + 1 + multiBlockRadius, this.yCoord + 1 + multiBlockRadius, this.zCoord + 1 + multiBlockRadius);
    }

    /** MutliBlock methods. */
    private TurbineMBlockHandler multiBlock;

    @Override
    public Vector3[] getMultiBlockVectors()
    {
        Set<Vector3> vectors = new HashSet<Vector3>();

        ForgeDirection dir = getDirection();
        int xMulti = dir.offsetX != 0 ? 0 : 1;
        int yMulti = dir.offsetY != 0 ? 0 : 1;
        int zMulti = dir.offsetZ != 0 ? 0 : 1;

        for (int x = -multiBlockRadius; x <= multiBlockRadius; x++)
        {
            for (int y = -multiBlockRadius; y <= multiBlockRadius; y++)
            {
                for (int z = -multiBlockRadius; z <= multiBlockRadius; z++)
                {
                    vectors.add(new Vector3(x * xMulti, y * yMulti, z * zMulti));
                }
            }
        }

        return vectors.toArray(new Vector3[0]);
    }

    @Override
    public Vector3 getPosition()
    {
        return new Vector3(this);
    }

    @Override
    public TurbineMBlockHandler getMultiBlock()
    {
        if (multiBlock == null)
            multiBlock = new TurbineMBlockHandler(this);

        return multiBlock;
    }

    @Override
    public void onMultiBlockChanged()
    {
        worldObj.notifyBlocksOfNeighborChange(xCoord, yCoord, zCoord, getBlockType() != null ? getBlockType().blockID : 0);
        worldObj.markBlockForUpdate(xCoord, yCoord, zCoord);
    }

    @Override
    public World getWorld()
    {
        return worldObj;
    }

    @Override
    public INode getNode(Class<? extends INode> nodeType, ForgeDirection from)
    {
        if (nodeType.isAssignableFrom(mechanicalNode.getClass()))
            return ((TileTurbine) getMultiBlock().get()).mechanicalNode;
        return null;
    }

    @Override
    public void invalidate()
    {
        mechanicalNode.deconstruct();
        super.invalidate();
    }

}
