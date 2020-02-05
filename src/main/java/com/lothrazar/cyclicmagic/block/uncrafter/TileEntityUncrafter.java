/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.lothrazar.cyclicmagic.block.uncrafter;

import java.util.ArrayList;
import java.util.List;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.block.core.TileEntityBaseMachineInvo;
import com.lothrazar.cyclicmagic.capability.EnergyStore;
import com.lothrazar.cyclicmagic.data.ITileRedstoneToggle;
import com.lothrazar.cyclicmagic.util.UtilInventoryTransfer;
import com.lothrazar.cyclicmagic.util.UtilItemStack;
import com.lothrazar.cyclicmagic.util.UtilUncraft;
import com.lothrazar.cyclicmagic.util.UtilUncraft.UncraftResultType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;

public class TileEntityUncrafter extends TileEntityBaseMachineInvo implements ITileRedstoneToggle, ITickable {

  // http://www.minecraftforge.net/wiki/Containers_and_GUIs
  // http://greyminecraftcoder.blogspot.com.au/2015/01/tileentity.html
  // http://www.minecraftforge.net/forum/index.php?topic=28539.0
  // http://bedrockminer.jimdo.com/modding-tutorials/advanced-modding/tile-entities/
  // http://www.minecraftforge.net/wiki/Tile_Entity_Synchronization
  // http://www.minecraftforge.net/forum/index.php?topic=18871.0
  public static final int SLOT_UNCRAFTME = 0;
  public static final int SLOT_ROWS = 4;
  public static final int SLOT_COLS = 5;
  public static int TIMER_FULL = 200;

  public static enum Fields {
    TIMER, REDSTONE;
  }

  public TileEntityUncrafter() {
    super(SLOT_ROWS * SLOT_COLS + 1);
    timer = TIMER_FULL;
    this.initEnergy(new EnergyStore(MENERGY, MENERGY, MENERGY), BlockUncrafting.FUEL_COST);
    this.setSlotsForInsert(SLOT_UNCRAFTME);
    this.setSlotsForExtract(1, this.getSizeInventory() - 1);
  }

  @Override
  public int[] getFieldOrdinals() {
    return super.getFieldArray(Fields.values().length);
  }

  @Override
  public void readFromNBT(NBTTagCompound tagCompound) {
    super.readFromNBT(tagCompound);
  }

  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound tagCompound) {
    return super.writeToNBT(tagCompound);
  }

  @Override
  public void update() {
    if (!this.isRunning()) {
      return;
    }
    //else: its powered, OR it doesnt need power so its ok
    ItemStack stack = getStackInSlot(SLOT_UNCRAFTME);
    if (stack.isEmpty()) {
      return;
    }
    if (this.updateEnergyIsBurning() == false) {
      return;
    }
    if (this.updateTimerIsZero()) {
      timer = TIMER_FULL; //reset the timer and do the thing
      UtilUncraft.Uncrafter uncrafter = new UtilUncraft.Uncrafter();
      try {
        if (uncrafter.process(stack) == UncraftResultType.SUCCESS) {
          if (this.getWorld().isRemote == false) { // drop the items
            ArrayList<ItemStack> uncrafterOutput = uncrafter.getDrops();
            setOutputItems(uncrafterOutput);
            this.decrStackSize(0, uncrafter.getOutsize());
          }
        }
        else {//success = false, so try to dump to inventory first
          ArrayList<ItemStack> toDrop = new ArrayList<ItemStack>();
          toDrop.add(stack.copy());
          setOutputItems(toDrop);
          if (this.getWorld().isRemote == false) {
            this.decrStackSize(0, stack.getCount());
          }
        }
        this.getWorld().markBlockRangeForRenderUpdate(this.getPos(), this.getPos().up());
        this.markDirty();
      }
      catch (Exception e) {
        ModCyclic.logger.error("Unhandled exception in uncrafting ", e);
      }
    } //end of timer go
  }

  private void setOutputItems(List<ItemStack> output) {
    ArrayList<ItemStack> toDrop = UtilInventoryTransfer.dumpToIInventory(output, this, SLOT_UNCRAFTME + 1);
    if (!toDrop.isEmpty()) {
      for (ItemStack s : toDrop) {
        UtilItemStack.dropItemStackInWorld(this.getWorld(), this.getPos().up(), s);
      }
    }
  }

  @Override
  public int getField(int id) {
    switch (Fields.values()[id]) {
      case TIMER:
        return timer;
      case REDSTONE:
        if (needsRedstone != 1 && needsRedstone != 0) {
          needsRedstone = 0;
        }
        return this.needsRedstone;
    }
    return -7;
  }

  @Override
  public void setField(int id, int value) {
    switch (Fields.values()[id]) {
      case TIMER:
        this.timer = value;
      break;
      case REDSTONE:
        if (value != 1 && value != 0) {
          value = 0;
        }
        this.needsRedstone = value;
      break;
    }
  }

  @Override
  public int getFieldCount() {
    return Fields.values().length;
  }

  @Override
  public void toggleNeedsRedstone() {
    int val = (this.needsRedstone + 1) % 2;
    this.setField(Fields.REDSTONE.ordinal(), val);
  }

  @Override
  public boolean onlyRunIfPowered() {
    return this.needsRedstone == 1;
  }
}
