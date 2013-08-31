package dark.farmtech.machines.farmer;

import dark.farmtech.machines.TileEntityFT;
import dark.farmtech.machines.farmer.EntityFarmDrone.DroneData;

public class TileEntityFarmBox extends TileEntityFT
{
    /** Current amount of drone slots this box has */
    private int droneSlots = 1;
    /** Stores drone data while the drone is stored in the block */
    private DroneData[] droneData = new DroneData[4];
    /** Stores drone instances as drones are active outside the block */
    private EntityFarmDrone[] drones = new EntityFarmDrone[4];

    public TileEntityFarmBox()
    {
        this.MAX_WATTS = 100;
        this.WATTS_PER_TICK = 5;
    }

    public void updateEntity()
    {
        super.updateEntity();
    }
}
