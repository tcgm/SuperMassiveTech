package tterrag.supermassivetech.tile;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;
import tterrag.supermassivetech.item.ItemStar;
import tterrag.supermassivetech.registry.IStar;
import tterrag.supermassivetech.util.Utils;
import cofh.api.energy.EnergyStorage;
import cofh.api.energy.IEnergyHandler;
import cofh.api.tileentity.IEnergyInfo;

public class TileStarHarvester extends TileSMTInventory implements IEnergyHandler, IEnergyInfo
{
	private ItemStack[] inventory = new ItemStack[1];
	private int slot = 0;
	private EnergyStorage storage;
	private final int STORAGE_CAP = 100000;
	
	public TileStarHarvester()
	{
		storage = new EnergyStorage(STORAGE_CAP);
	}
	
	@Override
	public void updateEntity() 
	{
		super.updateEntity();
		if (inventory[slot] != null && inventory[slot].getItem() instanceof ItemStar)
		{
			IStar type = Utils.getType(inventory[slot]);
			int energy = inventory[slot].getTagCompound().getInteger("energy");
			if (energy > 0)
			{
				inventory[slot].getTagCompound().setInteger("energy", energy - storage.receiveEnergy(energy > type.getPowerPerTick() ? type.getPowerPerTick() : energy, false));
			}
			System.out.println(storage.getEnergyStored() + " " + inventory[slot].getTagCompound().getInteger("energy"));
		}
	}
	
	@Override
	public boolean isGravityWell() 
	{
		return inventory[slot] != null && inventory[slot].getItem() instanceof ItemStar;
	}

	@Override
	public boolean showParticles() 
	{
		return true;
	}

	@Override
	public String getInventoryName() 
	{
		return "tterrag.inventory.starHarvester";
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) 
	{
		return 0;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) 
	{
		System.out.println("attempted extract: " + maxExtract);
		return storage.extractEnergy(maxExtract, simulate);
	}

	@Override
	public boolean canInterface(ForgeDirection from) 
	{
		return true;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) 
	{
		return storage.getEnergyStored();
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) 
	{
		return STORAGE_CAP;
	}

	@Override
	public int getEnergyPerTick() 
	{
		return getMaxEnergyPerTick();
	}

	@Override
	public int getMaxEnergyPerTick() 
	{
		return ((IStar) inventory[slot].getItem()).getPowerPerTick();
	}

	@Override
	public int getEnergy() 
	{
		return storage.getEnergyStored();
	}

	@Override
	public int getMaxEnergy() 
	{
		return STORAGE_CAP;
	}
	
	public boolean insertStar(EntityPlayer player)
	{
		ItemStack stack = player.getCurrentEquippedItem();
		if (stack != null && stack.getItem() instanceof ItemStar && inventory[slot] == null)
		{
			ItemStack insert = stack.copy();
			insert.stackSize = 1;
			inventory[slot] = insert;
			player.getCurrentEquippedItem().stackSize--;
			return true;
		}
		return false;
	}
}
