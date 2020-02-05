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
package com.lothrazar.cyclicmagic.item.storagesack;

import java.util.List;
import java.util.UUID;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.gui.ForgeGuiHandler;
import com.lothrazar.cyclicmagic.item.core.BaseItem;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.lothrazar.cyclicmagic.util.UtilInventoryTransfer;
import com.lothrazar.cyclicmagic.util.UtilInventoryTransfer.BagDepositReturn;
import com.lothrazar.cyclicmagic.util.UtilNBT;
import com.lothrazar.cyclicmagic.util.UtilSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemStorageBag extends BaseItem implements IHasRecipe {

  private static final String GUI_ID = "guiID";

  public static enum StoragePickupType {

    NOTHING, FILTER, EVERYTHING;

    private final static String NBT = "deposit";

    public static int get(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      return tags.getInteger(NBT);
    }

    public static String getName(ItemStack wand) {
      try {
        NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
        return "item.storage_bag.pickup." + StoragePickupType.values()[tags.getInteger(NBT)].toString().toLowerCase();
      }
      catch (Exception e) {
        return "item.storage_bag.pickup." + NOTHING.toString().toLowerCase();
      }
    }

    public static void toggle(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int type = tags.getInteger(NBT);
      type++;
      if (type > EVERYTHING.ordinal()) {
        type = NOTHING.ordinal();
      }
      tags.setInteger(NBT, type);
      wand.setTagCompound(tags);
    }
  }

  public static enum StorageActionType {

    NOTHING, MERGE, DEPOSIT;

    private final static String NBT_OPEN = "isOpen";
    private final static String NBT_COLOUR = "COLOUR";
    private final static String NBT = "build";
    private final static String NBTTIMEOUT = "timeout";

    public static int getTimeout(ItemStack wand) {
      return UtilNBT.getItemStackNBT(wand).getInteger(NBTTIMEOUT);
    }

    public static void setTimeout(ItemStack wand) {
      UtilNBT.getItemStackNBT(wand).setInteger(NBTTIMEOUT, 15);//less than one tick
    }

    public static void tickTimeout(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int t = tags.getInteger(NBTTIMEOUT);
      if (t > 0) {
        UtilNBT.getItemStackNBT(wand).setInteger(NBTTIMEOUT, t - 1);
      }
    }

    public static int get(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      return tags.getInteger(NBT);
    }

    public static String getName(ItemStack wand) {
      try {
        NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
        return "item.storage_bag." + StorageActionType.values()[tags.getInteger(NBT)].toString().toLowerCase();
      }
      catch (Exception e) {
        return "item.storage_bag." + NOTHING.toString().toLowerCase();
      }
    }

    public static void toggle(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int type = tags.getInteger(NBT);
      type++;
      if (type > DEPOSIT.ordinal()) {
        type = NOTHING.ordinal();
      }
      tags.setInteger(NBT, type);
      wand.setTagCompound(tags);
    }

    public static int getColour(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      if (tags.hasKey(NBT_COLOUR) == false) {
        return EnumDyeColor.BROWN.getColorValue();
      }
      return tags.getInteger(NBT_COLOUR);
    }

    public static boolean getIsOpen(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      if (tags.hasKey(NBT_OPEN) == false) {
        return false;
      }
      return tags.getBoolean(NBT_OPEN);
    }

    public static void setIsOpen(ItemStack wand, boolean s) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      tags.setBoolean(NBT_OPEN, s);
    }

    public static void setColour(ItemStack wand, int color) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      //      int type = tags.getInteger(NBT_COLOUR);
      //      type++;
      //      if (type > EnumDyeColor.values().length) {
      //        type = EnumDyeColor.BLACK.getDyeDamage();
      //      }
      tags.setInteger(NBT_COLOUR, color);
      wand.setTagCompound(tags);
      // TODO Auto-generated method stub
    }
  }

  public ItemStorageBag() {
    this.setMaxStackSize(1);
  }

  @Override
  public int getMaxItemUseDuration(ItemStack stack) {
    return 1; // Without this method, your inventory will NOT work!!!
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
    int size = InventoryStorage.countNonEmpty(stack);
    tooltip.add(UtilChat.lang("item.storage_bag.tooltip") + size);
    tooltip.add(UtilChat.lang("item.storage_bag.tooltip2") + UtilChat.lang(StorageActionType.getName(stack)));
    tooltip.add(UtilChat.lang(StoragePickupType.getName(stack)));
  }

  @Override
  public void onUpdate(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
    StorageActionType.tickTimeout(stack);
    super.onUpdate(stack, worldIn, entityIn, itemSlot, isSelected);
  }

  @SubscribeEvent
  public void onEntityItemPickupEvent(EntityItemPickupEvent event) {
    if (event.getItem().isDead) {//|| event.getItem().world.isRemote
      return;
    }
    ItemStack stackOnGround = event.getItem().getItem();
    if (this.canPickup(stackOnGround) == false) {
      return;
    }
    //multiple bags held by player
    NonNullList<ItemStack> foundBags = this.findAmmoList(event.getEntityPlayer(), this);
    for (ItemStack stackIsBag : foundBags) {
      if (StorageActionType.getIsOpen(stackIsBag)) {
        return;
      }
      int pickupType = ItemStorageBag.StoragePickupType.get(stackIsBag);
      if (pickupType == StoragePickupType.NOTHING.ordinal()) {
        continue;
      }
      if (pickupType == StoragePickupType.FILTER.ordinal()) {
        // treat bag contents as whtielist
        boolean doesMatch = false;
        NonNullList<ItemStack> inv = InventoryStorage.readFromNBT(stackIsBag);
        for (ItemStack tryMatch : inv) {
          if (tryMatch.isItemEqualIgnoreDurability(stackOnGround)) {
            doesMatch = true;
            break;
          }
        }
        if (!doesMatch) {
          return;//  filter type an it does not match
        }
      }
      //else type is everything so just go
      //do the real deposit
      InventoryStorage inventoryBag = new InventoryStorage(event.getEntityPlayer(), stackIsBag);
      NonNullList<ItemStack> onGround = NonNullList.create();
      onGround.add(stackOnGround);
      BagDepositReturn ret = UtilInventoryTransfer.dumpFromListToIInventory(event.getEntity().world, inventoryBag, onGround, false);
      if (ret.stacks.get(0).isEmpty()) {
        /// we got everything 
        event.getItem().setDead();
        event.setCanceled(true);
      }
      else {
        //we got part of it 
        event.getItem().setItem(ret.stacks.get(0));
      }
      break;
    }
  }

  /**
   * not empty and not another bag
   * 
   */
  private boolean canPickup(ItemStack stack) {
    return !stack.isEmpty() && stack.getItem() != this;
  }

  @SubscribeEvent
  public void onHit(PlayerInteractEvent.LeftClickBlock event) {
    EntityPlayer player = event.getEntityPlayer();
    ItemStack held = player.getHeldItem(event.getHand());
    if (held != null && held.getItem() == this) {
      World world = event.getWorld();
      TileEntity tile = world.getTileEntity(event.getPos());
      if (tile != null && tile instanceof IInventory) {
        int depositType = StorageActionType.get(held);
        if (depositType == StorageActionType.NOTHING.ordinal()) {
          if (world.isRemote) {
            UtilChat.addChatMessage(player, UtilChat.lang("item.storage_bag.disabled"));
          }
          return;
        }
        else {
          if (world.isRemote == false) {
            NonNullList<ItemStack> inv = InventoryStorage.readFromNBT(held);
            BagDepositReturn ret = null;
            if (depositType == StorageActionType.DEPOSIT.ordinal()) {
              ret = UtilInventoryTransfer.dumpFromListToIInventory(world, (IInventory) tile, inv, false);
            }
            else if (depositType == StorageActionType.MERGE.ordinal()) {
              ret = UtilInventoryTransfer.dumpFromListToIInventory(world, (IInventory) tile, inv, true);
            }
            if (ret != null && ret.moved > 0) {
              InventoryStorage.writeToNBT(held, ret.stacks);
              UtilChat.addChatMessage(player, UtilChat.lang("item.storage_bag.success") + ret.moved);
            }
          }
          UtilSound.playSound(player, SoundRegistry.sack_holding);
        }
      }
    }
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
    ItemStack wand = player.getHeldItem(hand);
    setIdIfEmpty(wand);
    //    StorageActionType.setIsOpen(wand, true);
    UtilSound.playSound(player, player.getPosition(), SoundEvents.ENTITY_PIG_SADDLE, SoundCategory.PLAYERS, 0.1F);
    if (!world.isRemote && wand.getItem() instanceof ItemStorageBag
        && hand == EnumHand.MAIN_HAND
        && wand.getCount() == 1) {
      BlockPos pos = player.getPosition();
      int x = pos.getX(), y = pos.getY(), z = pos.getZ();
      player.openGui(ModCyclic.instance, ForgeGuiHandler.GUI_INDEX_STORAGE, world, x, y, z);
    }
    return super.onItemRightClick(world, player, hand);
  }

  private void setIdIfEmpty(ItemStack wand) {
    if (UtilNBT.getItemStackNBT(wand).hasKey(GUI_ID) == false ||
        UtilNBT.getItemStackNBT(wand).getString(GUI_ID) == null) {
      UtilNBT.setItemStackNBTVal(wand, GUI_ID, UUID.randomUUID().toString());
    }
  }

  public static String getId(ItemStack wand) {
    return UtilNBT.getItemStackNBT(wand).getString(GUI_ID);
  }

  //
  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this), "lsl", "ldl", "lrl",
        'l', "leather",
        's', "string",
        'r', "dustRedstone",
        'd', "ingotGold");
  }
}
