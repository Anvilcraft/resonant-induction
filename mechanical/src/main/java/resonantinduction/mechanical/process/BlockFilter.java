package resonantinduction.mechanical.process;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidHandler;
import resonantinduction.api.recipe.MachineRecipes;
import resonantinduction.api.recipe.MachineRecipes.RecipeType;
import resonantinduction.api.recipe.RecipeResource;
import resonantinduction.core.resource.ResourceGenerator;
import resonantinduction.core.resource.fluid.BlockFluidMixture;
import universalelectricity.api.vector.Vector3;
import calclavia.lib.prefab.block.BlockTile;
import calclavia.lib.utility.LanguageUtility;
import calclavia.lib.utility.inventory.InventoryUtility;

/**
 * Used for filtering liquid mixtures
 * 
 * @author Calclavia
 * 
 */
public class BlockFilter extends BlockTile
{
	public BlockFilter(int id)
	{
		super(id, Material.iron);
	}

	@Override
	public void onBlockAdded(World world, int x, int y, int z)
	{
		world.scheduleBlockUpdate(x, y, z, blockID, 20);
	}

	@Override
	public void onNeighborBlockChange(World world, int x, int y, int z, int neighborID)
	{
		world.scheduleBlockUpdate(x, y, z, blockID, 20);
	}

	@Override
	public void updateTick(World world, int x, int y, int z, Random random)
	{
		Vector3 position = new Vector3(x, y, z);
		Vector3 checkAbove = position.clone().translate(ForgeDirection.UP);
		Vector3 checkBelow = position.clone().translate(ForgeDirection.DOWN);

		Block bAbove = Block.blocksList[checkAbove.getBlockID(world)];
		Block bBelow = Block.blocksList[checkAbove.getBlockID(world)];

		if (bAbove instanceof BlockFluidMixture && (world.isAirBlock(checkBelow.intX(), checkBelow.intY(), checkBelow.intZ()) || checkBelow.getTileEntity(world) instanceof IFluidHandler))
		{
			world.spawnParticle("dripWater", x + 0.5, y, z + 0.5, 0, 0, 0);

			if (checkBelow.getTileEntity(world) instanceof IFluidHandler)
			{
				IFluidHandler handler = ((IFluidHandler) checkBelow.getTileEntity(world));

				if (handler.fill(ForgeDirection.UP, new FluidStack(FluidRegistry.WATER, FluidContainerRegistry.BUCKET_VOLUME), false) <= 0)
					return;
			}

			/**
			 * Leak the fluid down.
			 */
			BlockFluidMixture fluidBlock = (BlockFluidMixture) bAbove;
			int amount = fluidBlock.getQuantaValue(world, checkAbove.intX(), checkAbove.intY(), checkAbove.intZ());
			int leakAmount = 2;

			/**
			 * Drop item from fluid.
			 */
			for (RecipeResource resoure : MachineRecipes.INSTANCE.getOutput(RecipeType.MIXER, "dust" + ResourceGenerator.mixtureToMaterial(fluidBlock.getFluid().getName())))
			{
				InventoryUtility.dropItemStack(world, checkAbove.clone().add(0.5), resoure.getItemStack().copy());
			}

			// TODO: Check if this is correct?
			int remaining = amount - leakAmount;

			/**
			 * Remove liquid from top.
			 */
			fluidBlock.setQuanta(world, checkAbove.intX(), checkAbove.intY(), checkAbove.intZ(), remaining);
			world.scheduleBlockUpdate(x, y, z, blockID, 20);

			/**
			 * Add liquid to bottom.
			 */
			if (checkBelow.getTileEntity(world) instanceof IFluidHandler)
			{
				IFluidHandler handler = ((IFluidHandler) checkBelow.getTileEntity(world));
				handler.fill(ForgeDirection.UP, new FluidStack(FluidRegistry.WATER, FluidContainerRegistry.BUCKET_VOLUME), true);
			}
			else
			{
				checkBelow.setBlock(world, Block.waterMoving.blockID);
			}
		}
	}

	@Override
	public boolean renderAsNormalBlock()
	{
		return false;
	}

	@Override
	public boolean isOpaqueCube()
	{
		return false;
	}

}
