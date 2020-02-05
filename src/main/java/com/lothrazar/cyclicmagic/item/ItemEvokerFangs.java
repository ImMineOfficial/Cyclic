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

import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.guide.GuideCategory;
import com.lothrazar.cyclicmagic.item.core.BaseTool;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilEntity;
import com.lothrazar.cyclicmagic.util.UtilItemStack;
import com.lothrazar.cyclicmagic.util.UtilSound;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityEvokerFangs;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

public class ItemEvokerFangs extends BaseTool implements IHasRecipe, IContent {

  private static final int COOLDOWN = 10;//ticks not seconds
  private static final int DURABILITY = 666;
  private static final int MAX_RANGE = 16;

  public ItemEvokerFangs() {
    super(DURABILITY);
  }

  @Override
  public String getContentName() {
    return "evoker_fang";
  }

  @Override
  public void register() {
    ItemRegistry.register(this, getContentName(), GuideCategory.ITEM);
    LootTableRegistry.registerLoot(this);
    ModCyclic.instance.events.register(this);
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("EvokerFang", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
  }

  @Override
  public boolean itemInteractionForEntity(ItemStack stack, EntityPlayer player, EntityLivingBase entity, EnumHand hand) {
    if (player.getCooldownTracker().hasCooldown(this)) {
      return false;
    }
    summonFangRay(player.posX, player.posZ, player, entity.posX, entity.posY, entity.posZ);
    UtilItemStack.damageItem(player, player.getHeldItem(hand));
    return true;
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
    if (player.getCooldownTracker().hasCooldown(this)) {
      return super.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);
    }
    summonFangRay(pos.getX(), pos.getZ(), player, pos.getX() + hitX, pos.getY() + hitY, pos.getZ() + hitZ);
    UtilItemStack.damageItem(player, player.getHeldItem(hand));
    return EnumActionResult.SUCCESS;
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand handIn) {
    BlockPos target = playerIn.getPosition().offset(playerIn.getAdjustedHorizontalFacing());
    this.summonFangRay(playerIn.posX, playerIn.posZ, playerIn, target.getX(), target.getY(), target.getZ());
    return super.onItemRightClick(worldIn, playerIn, handIn);
  }

  /**
   * Summon a ray of fangs in the direction of these coordinates away from the caster
   * 
   * @param caster
   * @param posX
   * @param posY
   * @param posZ
   */
  private void summonFangRay(double startX, double startZ, EntityPlayer caster, double posX, double posY, double posZ) {
    double minY = posY;//Math.min(posY, caster.posY);
    //double d1 = Math.max(posY,caster.posY) ;
    float arctan = (float) MathHelper.atan2(posZ - caster.posZ, posX - caster.posX);
    for (int i = 0; i < MAX_RANGE; ++i) {
      double fract = 1.25D * (i + 1);
      this.summonFangSingle(caster,
          startX + MathHelper.cos(arctan) * fract,
          minY,
          startZ + MathHelper.sin(arctan) * fract,
          arctan, i);
    }
    onCastSuccess(caster);
  }

  /**
   * cast a single fang from the caster towards the direction of the given coordinates with delay
   * 
   * @param caster
   * @param x
   * @param y
   * @param z
   * @param yaw
   * @param delay
   */
  private void summonFangSingle(EntityPlayer caster, double x, double y, double z, float yaw, int delay) {
    EntityEvokerFangs entityevokerfangs = new EntityEvokerFangs(caster.world, x, y, z, yaw, delay, caster);
    if (caster.world.isRemote == false) {
      caster.world.spawnEntity(entityevokerfangs);
    }
    // so. WE are using this hack because the entity has a MAGIC NUMBER of 6.0F hardcoded in a few places deep inside methods and if statements
    //this number is the damage that it deals.  ( It should be a property )
    //    UtilNBT.setEntityBoolean(entityevokerfangs, NBT_FANG_FROMPLAYER);
  }

  private void onCastSuccess(EntityPlayer caster) {
    UtilSound.playSound(caster, SoundEvents.EVOCATION_ILLAGER_PREPARE_ATTACK);
    UtilEntity.setCooldownItem(caster, this, COOLDOWN);
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this),
        "wpc",
        " dp",
        "r w",
        'w', Blocks.WEB,
        'r', Items.BLAZE_ROD,
        'c', Items.END_CRYSTAL,
        'p', Blocks.ICE, //ore dict ice doesnt exist
        'd', "gemEmerald");
  }
}
