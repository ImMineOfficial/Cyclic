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
package com.lothrazar.cyclicmagic.item.core;

import com.lothrazar.cyclicmagic.util.UtilItemStack;
import com.lothrazar.cyclicmagic.util.UtilSound;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * TODO: extend BaseItemProjectile?
 * 
 * @author Sam
 *
 */
public abstract class BaseItemRapidScepter extends BaseTool {

  private static final float VELOCITY_MAX = 1.5F;
  private static final float INACCURACY_DEFAULT = 1.0F;
  private static final float PITCHOFFSET = 0.0F;
  private static final float MAX_CHARGE = 9.7F;

  //private static final int COOLDOWN = 5;
  public BaseItemRapidScepter(int durability) {
    super(durability);
  }

  public abstract SoundEvent getSound();

  public abstract EntityThrowable createBullet(World world, EntityPlayer player, float dmg);

  @Override
  public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand) {
    ItemStack stack = player.getHeldItem(hand);
    // float power = Math.min(MAX_CHARGE, ItemBow.getArrowVelocity(charge) * POWER_UPSCALE);
    float amountCharged = MAX_CHARGE;
    //between 0.3 and 5.1 roughly
    //  UtilChat.sendStatusMessage(player, amountCharged + "");
    float damage = MathHelper.floor(amountCharged) / 2;//so its an even 3 or 2.5
    int shots = 0;
    double rand = world.rand.nextDouble();
    if (rand < 0.2) {
      shootMain(world, player, VELOCITY_MAX, damage);
      shootTwins(world, player, VELOCITY_MAX, damage);
    }
    else if (rand < 0.75) {
      shootMain(world, player, VELOCITY_MAX, damage);
    }
    else {
      shootTwins(world, player, VELOCITY_MAX, damage);
    }
    UtilItemStack.damageItem(player, stack, shots);
    // player.getCooldownTracker().setCooldown(stack.getItem(), COOLDOWN);
    super.onUse(stack, player, world, EnumHand.MAIN_HAND);
    return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
  }

  private void shootMain(World world, EntityPlayer player, float velocityFactor, float damage) {
    EntityThrowable proj = createBullet(world, player, damage);
    this.launchProjectile(world, player, proj, velocityFactor);
  }

  private void shootTwins(World world, EntityPlayer player, float velocityFactor, float damage) {
    Vec3d vecCrossRight = player.getLookVec().normalize().crossProduct(new Vec3d(0, 2, 0));
    Vec3d vecCrossLeft = player.getLookVec().normalize().crossProduct(new Vec3d(0, -2, 0));
    EntityThrowable projRight = createBullet(world, player, damage);
    projRight.posX += vecCrossRight.x;
    projRight.posZ += vecCrossRight.z;
    this.launchProjectile(world, player, projRight, velocityFactor);
    EntityThrowable projLeft = createBullet(world, player, damage);
    projLeft.posX += vecCrossLeft.x;
    projLeft.posZ += vecCrossLeft.z;
    this.launchProjectile(world, player, projLeft, velocityFactor);
  }

  protected void launchProjectile(World world, EntityPlayer player, EntityThrowable thing, float velocity) {
    if (!world.isRemote) {
      //zero pitch offset, meaning match the players existing. 1.0 at end ins inn
      thing.shoot(player, player.rotationPitch, player.rotationYaw, PITCHOFFSET, velocity, INACCURACY_DEFAULT);
      world.spawnEntity(thing);
    }
    BlockPos pos = player.getPosition();
    UtilSound.playSound(player, pos, getSound(), SoundCategory.PLAYERS, 0.4F);
  }
}
