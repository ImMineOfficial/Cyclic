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
package com.lothrazar.cyclicmagic.item;

import java.util.List;
import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.capability.IPlayerExtendedProperties;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.item.core.ItemFoodCreative;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.lothrazar.cyclicmagic.util.UtilParticle;
import com.lothrazar.cyclicmagic.util.UtilSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemFishFood;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCraftingUnlock extends ItemFoodCreative implements IHasRecipe, IContent {

  private static final int numFood = 10;

  public ItemCraftingUnlock() {
    super(numFood, false);
  }

  @Override
  public String getContentName() {
    return "crafting_food";
  }

  @Override
  public void register() {
    ItemRegistry.register(this, getContentName());
    LootTableRegistry.registerLoot(this);
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("InventoryCrafting(Food)", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
  }

  @Override
  protected void onFoodEaten(ItemStack stack, World world, EntityPlayer player) {
    final IPlayerExtendedProperties data = CapabilityRegistry.getPlayerProperties(player);
    if (data.hasInventoryCrafting()) {
      data.setInventoryCrafting(false);
      UtilSound.playSound(player, SoundEvents.BLOCK_FIRE_EXTINGUISH);
      return;
    }
    data.setInventoryCrafting(true);
    UtilParticle.spawnParticle(world, EnumParticleTypes.CRIT_MAGIC, player.getPosition());
    UtilParticle.spawnParticle(world, EnumParticleTypes.CRIT_MAGIC, player.getPosition().up());
    UtilSound.playSound(player, SoundRegistry.inventory_upgrade);
    if (player.getEntityWorld().isRemote) {
      UtilChat.addChatMessage(player, "unlocks.crafting");
    }
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapelessRecipe(new ItemStack(this),
        "workbench", Items.PUMPKIN_PIE, Items.CAKE, Items.COOKIE, new ItemStack(Items.FISH, 1, ItemFishFood.FishType.SALMON.getMetadata()), Items.POISONOUS_POTATO, "gemDiamond", "gemEmerald", "gemQuartz");
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltips, net.minecraft.client.util.ITooltipFlag advanced) {
    tooltips.add(UtilChat.lang(this.getTranslationKey() + ".tooltip"));
  }
}
