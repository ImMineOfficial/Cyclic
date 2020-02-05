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
package com.lothrazar.cyclicmagic.util;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import com.lothrazar.cyclicmagic.ModCyclic;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

public class UtilFluid {

  /**
   * Picks up fluid fills a container with it.
   */
  public static FluidActionResult fillContainer(World world, BlockPos pos, ItemStack stackIn, EnumFacing facing) {
    return FluidUtil.tryPickUpFluid(stackIn, null, world, pos, facing);
  }

  /**
   * Drains a filled container and places the fluid.
   * 
   * RETURN new item stack that has been drained after placing in world if it works null otherwise
   */
  public static ItemStack dumpContainer(World world, BlockPos pos, ItemStack stackIn) {
    ItemStack dispensedStack = stackIn.copy();
    IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(dispensedStack);
    if (fluidHandler == null) {
      return ItemStack.EMPTY;
    }
    FluidStack fluidStack = fluidHandler.drain(Fluid.BUCKET_VOLUME, false);
    if (fluidStack != null// && fluidStack.amount >= Fluid.BUCKET_VOLUME
    ) {
      FluidActionResult placementResult = FluidUtil.tryPlaceFluid(null, world, pos, dispensedStack,
          fluidStack.copy());
      if (placementResult.isSuccess()) {
        //http://www.minecraftforge.net/forum/topic/56265-1112-fluidhandler-capability-on-buckets/
        return placementResult.result;
      }
    }
    return stackIn;
  }

  public static ItemStack drainOneBucket(ItemStack d) {
    IFluidHandlerItem fluidHandler = FluidUtil.getFluidHandler(d);
    if (fluidHandler == null) {
      return d;
    } //its empty, ok no problem
    fluidHandler.drain(Fluid.BUCKET_VOLUME, true);
    return fluidHandler.getContainer();
  }

  public static boolean isEmptyOfFluid(ItemStack returnMe) {
    FluidStack fs = FluidUtil.getFluidContained(returnMe);
    return fs == null || fs.amount == 0;
  }

  public static FluidStack getFluidContained(ItemStack returnMe) {
    return FluidUtil.getFluidContained(returnMe);
  }

  public static Fluid getFluidType(ItemStack returnMe) {
    FluidStack f = FluidUtil.getFluidContained(returnMe);
    return (f == null) ? null : f.getFluid();
  }

  public static boolean stackHasFluidHandler(ItemStack stackIn) {
    return FluidUtil.getFluidHandler(stackIn) != null;
  }

  public static boolean hasFluidHandler(TileEntity tile, EnumFacing side) {
    return tile != null && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
  }

  public static IFluidHandler getFluidHandler(TileEntity tile, EnumFacing side) {
    return (tile != null && tile.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) ? tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side) : null;
  }

  public static boolean interactWithFluidHandler(@Nonnull EntityPlayer player, @Nonnull World world, @Nonnull BlockPos pos, @Nullable EnumFacing side) {
    return FluidUtil.interactWithFluidHandler(player, EnumHand.MAIN_HAND, world, pos, side);
  }

  public static boolean tryFillTankFromPosition(World world, BlockPos posSide, EnumFacing sideOpp, FluidTank tankTo, final int amount) {
    return tryFillTankFromPosition(world, posSide, sideOpp, tankTo, amount, false, null);
  }

  public static boolean isStackInvalid(FluidStack stackToTest,
      boolean isWhitelist, List<FluidStack> filterList) {
    if (filterList == null) {
      return true;
    }
    boolean hasMatch = false;
    for (FluidStack filt : filterList) {
      if (stackToTest.getFluid() == filt.getFluid()) {
        hasMatch = true;
        break;
      }
    }
    if (hasMatch) {
      // fluid matches something in my list . so whitelist means ok
      return isWhitelist;
    }
    //here is the opposite: i did NOT match the list
    return !isWhitelist;
  }

  /**
   * Look for a fluid handler with gien position and direction try to extract from that pos and fill the tank
   * 
   */
  public static boolean tryFillTankFromPosition(World world, BlockPos posSide, EnumFacing sideOpp, FluidTank tankTo, final int amount,
      boolean isWhitelist, @Nullable List<FluidStack> allowedToMove) {
    try {
      IFluidHandler fluidFrom = FluidUtil.getFluidHandler(world, posSide, sideOpp);
      if (fluidFrom != null) {
        //its not my facing dir
        // SO: pull fluid from that into myself
        FluidStack wasDrained = fluidFrom.drain(amount, false);
        if (wasDrained == null) {
          return false;
        }
        if (!isStackInvalid(wasDrained, isWhitelist, allowedToMove)) {
          return false;
        }
        int filled = tankTo.fill(wasDrained, false);
        if (wasDrained != null && wasDrained.amount > 0
            && filled > 0) {
          int realAmt = Math.min(filled, wasDrained.amount);
          wasDrained = fluidFrom.drain(realAmt, true);
          if (wasDrained == null) {
            return false;
          }
          return tankTo.fill(wasDrained, true) > 0;
        }
      }
      return false;
    }
    catch (Exception e) {
      ModCyclic.logger.error("External fluid block had an issue when we tried to drain", e);
      //charset crashes here i guess
      //https://github.com/PrinceOfAmber/Cyclic/issues/605
      // https://github.com/PrinceOfAmber/Cyclic/issues/605https://pastebin.com/YVtMYsF6
      return false;
    }
  }

  public static boolean tryFillPositionFromTank(World world, BlockPos posSide, EnumFacing sideOpp, FluidTank tankFrom, int amount) {
    try {
      IFluidHandler fluidTo = FluidUtil.getFluidHandler(world, posSide, sideOpp);
      if (fluidTo != null) {
        //its not my facing dir
        // SO: pull fluid from that into myself
        FluidStack wasDrained = tankFrom.drain(amount, false);
        if (wasDrained == null) {
          return false;
        }
        int filled = fluidTo.fill(wasDrained, false);
        if (wasDrained != null && wasDrained.amount > 0
            && filled > 0) {
          int realAmt = Math.min(filled, wasDrained.amount);
          wasDrained = tankFrom.drain(realAmt, true);
          if (wasDrained == null) {
            return false;
          }
          return fluidTo.fill(wasDrained, true) > 0;
        }
      }
      return false;
    }
    catch (Exception e) {
      ModCyclic.logger.error("A fluid tank had an issue when we tried to fill", e);
      //charset crashes here i guess
      //https://github.com/PrinceOfAmber/Cyclic/issues/605
      // https://github.com/PrinceOfAmber/Cyclic/issues/605https://pastebin.com/YVtMYsF6
      return false;
    }
  }
  // IF is MIT license so i used this one function 
  // color from fluid?
  // https://github.com/Buuz135/Industrial-Foregoing/blob/427307ceb4188bd43c940cfdce1e941a05e24ee4/src/main/java/com/buuz135/industrial/utils/FluidUtils.java
}
