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
package com.lothrazar.cyclicmagic.block.fan;

import java.util.List;
import com.lothrazar.cyclicmagic.block.core.TileEntityBaseMachineInvo;
import com.lothrazar.cyclicmagic.data.ITilePreviewToggle;
import com.lothrazar.cyclicmagic.data.ITileRedstoneToggle;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import com.lothrazar.cyclicmagic.util.UtilParticle;
import com.lothrazar.cyclicmagic.util.UtilShape;
import com.lothrazar.cyclicmagic.util.UtilSound;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;

public class TileEntityFan extends TileEntityBaseMachineInvo implements ITickable, ITileRedstoneToggle, ITilePreviewToggle {

  private static final int MIN_RANGE = 1;
  public static final int MAX_SPEED = 10;
  public static final int MAX_RANGE = 32;
  private static final String NBT_PUSH = "pushpull";
  private static final String NBT_RANGE = "range";

  public static enum Fields {
    TIMER, REDSTONE, PARTICLES, PUSHPULL, RANGE, SPEED, SILENT;
  }

  private int timer;
  private int pushIfZero = 0;//else pull. 0 as default 
  private int range = 16;
  private int isSilent;

  public TileEntityFan() {
    super(0);
    this.needsRedstone = 1;
    this.speed = 5;
  }

  @Override
  public int[] getFieldOrdinals() {
    return super.getFieldArray(Fields.values().length);
  }

  @Override
  public void update() {
    if (this.isRunning() == false) {
      setAnimation(false);
      soundsOff();
      timer = 0;
      return;
    }
    particles();
    soundsOnAndLoop();
    setAnimation(true);
    tick();
    pushEntities();
  }

  private void soundsOff() {
    if (this.isSilent == 0 &&
        this.timer != 0 && timer > 30) {
      UtilSound.playSound(getWorld(), getPos(), SoundRegistry.fan_off, SoundCategory.BLOCKS, 1.0F);
    }
  }

  private void soundsOnAndLoop() {
    if (this.isSilent == 0) {
      int lengthOn = 31;
      int lengthLoop = 40;
      if (timer == 0) {
        UtilSound.playSound(getWorld(), getPos(), SoundRegistry.fan_on, SoundCategory.BLOCKS, 0.8F);
      }
      else if (timer == lengthOn || (timer - lengthOn) % lengthLoop == 0) {
        UtilSound.playSound(getWorld(), getPos(), SoundRegistry.fan_loop, SoundCategory.BLOCKS, 0.4F);
      }
    }
  }

  private void tick() {
    this.timer++;
    if (timer >= Integer.MAX_VALUE - 1) {
      timer = 0;
    }
  }

  private void particles() {
    if (timer % 10 == 0) {
      //rm this its ugly, keep in case i add a custom particle
      if (isPreviewVisible()) {
        doParticles();
      }
    }
  }

  private void doParticles() {
    List<BlockPos> shape = getShape();
    for (BlockPos pos : shape) {
      UtilParticle.spawnParticle(this.getWorld(), EnumParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1);
    }
  }

  private void setAnimation(boolean lit) {
    this.world.setBlockState(pos, this.world.getBlockState(pos).withProperty(BlockFan.IS_LIT, lit));
  }

  @Override
  public List<BlockPos> getShape() {
    return UtilShape.line(getPos(), getCurrentFacing(), getCurrentRange());
  }

  private int pushEntities() {
    List<BlockPos> shape = getShape();
    if (shape.size() == 0) {
      // sometimes is empty on changing dimension or tile load/unload
      return 0;
    }
    BlockPos start = shape.get(0);
    BlockPos end = shape.get(shape.size() - 1);//without this hotfix, fan works only on the flatedge of the band, not the 1x1 area
    switch (getCurrentFacing().getAxis()) {
      case X:
        end = end.add(0, 0, 1);//X means EASTorwest. adding +1z means GO 1 south
        end = end.add(0, 1, 0);//and of course go up one space. so we have a 3D range selected not a flat slice (ex: height 66 to 67)
      break;
      case Z:
        end = end.add(1, 0, 0);
        end = end.add(0, 1, 0);//and of course go up one space. so we have a 3D range selected not a flat slice (ex: height 66 to 67)
      break;
      case Y:
        start = start.add(1, 0, 0);
        end = end.add(0, 0, 1);
      default:
      break;
    }
    //ok now we have basically teh 3d box we wanted
    //problem: NORTH and WEST are skipping first blocks right at fan, but shouldnt.
    //EAST and SOUTH are skiping LAST blocks, but shouldnt
    //just fix it. root cause seems fine esp with UtilShape used
    EnumFacing face = getCurrentFacing();
    switch (face) {
      case NORTH:
        start = start.south();
      break;
      case SOUTH:
        end = end.south();
      break;
      case EAST:
        end = end.east();
      break;
      case WEST:
        start = start.east();
      break;
      case DOWN:
      break;
      case UP:
      default:
      break;
    }
    AxisAlignedBB region = new AxisAlignedBB(start, end);
    List<Entity> entitiesFound = this.getWorld().getEntitiesWithinAABB(Entity.class, region);//UtilEntity.getLivingHostile(, region);
    int moved = 0;
    boolean doPush = (pushIfZero == 0);
    int direction = 1;
    float SPEED = this.getSpeedCalc();
    for (Entity entity : entitiesFound) {
      if (entity instanceof EntityPlayer && ((EntityPlayer) entity).isSneaking()) {
        continue;//sneak avoid feature
      }
      moved++;
      switch (face) {
        case NORTH:
          direction = !doPush ? 1 : -1;
          entity.motionZ += direction * SPEED;
        break;
        case SOUTH:
          direction = doPush ? 1 : -1;
          entity.motionZ += direction * SPEED;
        break;
        case EAST:
          direction = doPush ? 1 : -1;
          entity.motionX += direction * SPEED;
        break;
        case WEST:
          direction = !doPush ? 1 : -1;
          entity.motionX += direction * SPEED;
        break;
        case DOWN:
          direction = !doPush ? 1 : -1;
          entity.motionY += direction * SPEED;
        break;
        case UP:
          direction = doPush ? 1 : -1;
          entity.motionY += direction * SPEED;
        default:
        break;
      }
    }
    // center of the block
    //    double x = this.getPos().getX() + 0.5;
    //    double y = this.getPos().getY() + 2;//was 0.7; dont move them up, move down. let them fall!
    //    double z = this.getPos().getZ() + 0.5;
    // UtilEntity.pullEntityList(x, y, z, pushIfFalse, entitiesFound, SPEED, SPEED, vertical);
    return moved;
  }

  private float getSpeedCalc() {
    return this.speed / 35F;
  }

  private int getCurrentRange() {
    EnumFacing facing = getCurrentFacing();
    BlockPos tester;
    for (int i = MIN_RANGE; i <= this.getRange(); i++) {//if we start at fan, we hit MYSELF (the fan)
      tester = this.getPos().offset(facing, i);
      if (canBlowThrough(tester) == false) {
        return i; //cant pass thru
      }
    }
    return getRange();
  }

  public int getRange() {
    return this.range;
  }

  private void setRange(int value) {
    this.range = Math.min(value, MAX_RANGE);
    if (range < MIN_RANGE) {
      range = MIN_RANGE;
    }
  }

  private boolean canBlowThrough(BlockPos tester) {
    //passes through air, and anything NOT a full block
    return this.getWorld().isAirBlock(tester) || !this.getWorld().getBlockState(tester).isFullBlock();
  }

  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound tags) {
    tags.setInteger(NBT_TIMER, timer);
    tags.setInteger(NBT_REDST, this.needsRedstone);
    tags.setInteger(NBT_PUSH, this.pushIfZero);
    tags.setInteger(NBT_RANGE, this.range);
    tags.setInteger("silent", this.isSilent);
    return super.writeToNBT(tags);
  }

  @Override
  public void readFromNBT(NBTTagCompound tags) {
    super.readFromNBT(tags);
    timer = tags.getInteger(NBT_TIMER);
    needsRedstone = tags.getInteger(NBT_REDST);
    this.pushIfZero = tags.getInteger(NBT_PUSH);
    this.range = tags.getInteger(NBT_RANGE);
    this.isSilent = tags.getInteger("silent");
  }

  @Override
  public void toggleNeedsRedstone() {
    int val = this.needsRedstone + 1;
    this.setField(Fields.REDSTONE.ordinal(), val % 2);
  }

  private void setPushPull(int value) {
    this.pushIfZero = value % 2;
    this.markDirty();
    this.world.markBlockRangeForRenderUpdate(pos, pos);
  }

  @Override
  public boolean onlyRunIfPowered() {
    return this.needsRedstone == 1;
  }

  @Override
  public int getFieldCount() {
    return Fields.values().length;
  }

  @Override
  public int getField(int id) {
    if (id >= 0 && id < this.getFieldCount()) {
      switch (Fields.values()[id]) {
        case TIMER:
          return timer;
        case REDSTONE:
          return this.needsRedstone;
        case PARTICLES:
          return this.renderParticles;
        case PUSHPULL:
          return this.pushIfZero;
        case RANGE:
          return this.range;
        case SPEED:
          return this.speed;
        case SILENT:
          return this.isSilent;
      }
    }
    return -1;
  }

  @Override
  public void setField(int id, int value) {
    if (id >= 0 && id < this.getFieldCount()) {
      switch (Fields.values()[id]) {
        case TIMER:
          this.timer = value;
        break;
        case REDSTONE:
          this.needsRedstone = value;
        break;
        case PARTICLES:
          this.renderParticles = value % 2;
        break;
        case PUSHPULL:
          this.setPushPull(value);
        break;
        case RANGE:
          this.setRange(value);
        break;
        case SPEED:
          this.setSpeed(value);
        break;
        case SILENT:
          this.isSilent = value % 2;
        break;
      }
    }
  }

  @Override
  public void setSpeed(int value) {
    if (value < 1) {
      value = 1;
    }
    speed = Math.min(value, MAX_SPEED);
  }

  @Override
  public boolean isPreviewVisible() {
    return this.renderParticles == 1;
  }
}
