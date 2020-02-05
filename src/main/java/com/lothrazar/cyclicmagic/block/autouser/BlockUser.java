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
package com.lothrazar.cyclicmagic.block.autouser;

import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.block.core.BlockBaseFacing;
import com.lothrazar.cyclicmagic.block.core.BlockBaseFacingInventory;
import com.lothrazar.cyclicmagic.block.core.IBlockHasTESR;
import com.lothrazar.cyclicmagic.block.core.MachineTESR;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.gui.ForgeGuiHandler;
import com.lothrazar.cyclicmagic.guide.GuideCategory;
import com.lothrazar.cyclicmagic.registry.BlockRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockUser extends BlockBaseFacingInventory implements IHasRecipe, IBlockHasTESR, IContent {

  public static final PropertyDirection PROPERTYFACING = BlockBaseFacing.PROPERTYFACING;
  public static int FUEL_COST = 0;
  public static int maxAttackPer;

  public BlockUser() {
    super(Material.IRON, ForgeGuiHandler.GUI_INDEX_USER);
    this.setHardness(3.0F).setResistance(5.0F);
    this.setSoundType(SoundType.METAL);
  }

  @Override
  public String getContentName() {
    return "block_user";
  }

  @Override
  public TileEntity createTileEntity(World worldIn, IBlockState state) {
    return new TileEntityUser();
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void initModel() {
    ModelLoader.setCustomModelResourceLocation(Item.getItemFromBlock(this), 0, new ModelResourceLocation(getRegistryName(), "inventory"));
    // Bind our TESR to our tile entity
    ClientRegistry.bindTileEntitySpecialRenderer(TileEntityUser.class, new MachineTESR(this));
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this), "rsr", " b ", "ooo",
        'o', "obsidian",
        's', Blocks.DISPENSER,
        'r', "ingotGold",
        'b', Blocks.MAGMA);
  }

  @Override
  public void register() {
    BlockRegistry.registerBlock(this, getContentName(), GuideCategory.BLOCKMACHINE);
    BlockRegistry.registerTileEntity(TileEntityUser.class, Const.MODID + getContentName() + "_te");
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("AutomatedUser", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
    FUEL_COST = config.getInt(getContentName(), Const.ConfigCategory.fuelCost, 10, 0, 500000, Const.ConfigText.fuelCost);
    maxAttackPer = config.getInt("AutoUserMaxAttackPerAction", Const.ConfigCategory.modpackMisc, 0, 0, 100, "How many entities can be attacked with one swipe from the block_user when in attack mode.  Zero means no limit.  ");
    //
    TileEntityUser.MAX_SPEED = config.getInt("AutoUserLargestTick", Const.ConfigCategory.modpackMisc, 200, 2, 999, "Largest tick delay allowed in auto user control  ");
    TileEntityUser.MIN_SPEED = config.getInt("AutoUserSmallestTick", Const.ConfigCategory.modpackMisc, 1, 1, 200, "Smallest tick delay allowed in auto user control.  if 1 use per tick is too much for your server than raise this larger");
  }
}
