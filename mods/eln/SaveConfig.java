package mods.eln;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

public class SaveConfig extends WorldSavedData{

	public static SaveConfig instance;
	
	public boolean heatFurnaceFuel = true;
	public boolean electricalLampAging = true;
	public boolean batteryAging = true;
	public boolean infinitPortableBattery = false;
	public SaveConfig(String par1Str) {
		super(par1Str);
		instance = this;	
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		heatFurnaceFuel = nbt.getBoolean("heatFurnaceFuel");
		electricalLampAging = nbt.getBoolean("electricalLampAging");
		batteryAging = nbt.getBoolean("batteryAging");
		infinitPortableBattery = nbt.getBoolean("infinitPortableBattery");
		
		Eln.wind.readFromNBT(nbt, "wind");
	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		// TODO Auto-generated method stub
		nbt.setBoolean("heatFurnaceFuel", heatFurnaceFuel);
		nbt.setBoolean("electricalLampAging", electricalLampAging);
		nbt.setBoolean("batteryAging", batteryAging);
		nbt.setBoolean("infinitPortableBattery", infinitPortableBattery);
		
		Eln.wind.writeToNBT(nbt, "wind");
	}
	@Override
	public boolean isDirty() {
		return true;
	}
}
