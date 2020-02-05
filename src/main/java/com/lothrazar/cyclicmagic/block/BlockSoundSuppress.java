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
package com.lothrazar.cyclicmagic.block;

import java.util.List;
import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.block.core.BlockBase;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.guide.GuideCategory;
import com.lothrazar.cyclicmagic.registry.BlockRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.lothrazar.cyclicmagic.util.UtilWorld;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.Sound;
import net.minecraft.client.audio.SoundEventAccessor;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockSoundSuppress extends BlockBase implements IHasRecipe, IContent {

  private static final int VOL_REDUCE_PER_BLOCK = 4;
  private static final int RADIUS = 6;

  //TODO: future upgrade: Filter by category. some types only filter some types of block categories?
  //filter by range? b power? different block types?
  public BlockSoundSuppress() {
    super(Material.CLAY);
    this.myTooltip = UtilChat.lang("tile." + getContentName() + ".tooltip") + RADIUS;
  }

  @Override
  public String getContentName() {
    return "block_soundproofing";
  }

  @Override
  public void register() {
    BlockRegistry.registerBlock(this, getContentName(), GuideCategory.BLOCK);
    ModCyclic.instance.events.register(this);
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("Soundproofing", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
  }

  @SideOnly(Side.CLIENT)
  @SubscribeEvent
  public void onPlaySound(PlaySoundEvent event) {
    if (event.getResultSound() == null || event.getResultSound() instanceof ITickableSound || ModCyclic.proxy.getClientWorld() == null) {
      return;
    } //long term/repeating/music
    ISound sound = event.getResultSound();
    List<BlockPos> blocks = UtilWorld.findBlocks(ModCyclic.proxy.getClientWorld(), new BlockPos(sound.getXPosF(), sound.getYPosF(), sound.getZPosF()), this, RADIUS);
    if (blocks == null || blocks.size() == 0) {
      return;
    }
    try {//WARNING": DO NOT USE getVolume anywhere here it just crashes
      //we do use it inside the sound class, but the engine callss tat later on, and our factor is tacked in
      SoundVolumeControlled newSound = new SoundVolumeControlled(sound);
      //the number of nearby blocks informs how much we muffle the sound by
      float pct = (VOL_REDUCE_PER_BLOCK) / 100F;
      newSound.setVolume(pct / blocks.size());
      event.setResultSound(newSound);
    }
    catch (Exception e) {
      ModCyclic.logger.error("Error trying to detect volume of sound from 3rd party ", e);
      ModCyclic.logger.error(e.getMessage());
    }
  }

  //copy a sound and control its volume
  //because there is no setVolume() fn in ISound... we must clone it
  private static class SoundVolumeControlled implements ISound {

    public float volume;
    public ISound sound;

    public SoundVolumeControlled(ISound s) {
      sound = s;
    }

    public void setVolume(float v) {
      this.volume = v;
    }

    @Override
    public float getVolume() {
      return volume * sound.getVolume();//not from the input, our own control
    }

    @Override
    public ResourceLocation getSoundLocation() {
      return sound.getSoundLocation();
    }

    @Override
    public SoundEventAccessor createAccessor(SoundHandler handler) {
      return sound.createAccessor(handler);
    }

    @Override
    public Sound getSound() {
      return sound.getSound();
    }

    @Override
    public SoundCategory getCategory() {
      return sound.getCategory();
    }

    @Override
    public boolean canRepeat() {
      return sound.canRepeat();
    }

    @Override
    public int getRepeatDelay() {
      return sound.getRepeatDelay();
    }

    @Override
    public float getPitch() {
      return sound.getPitch();
    }

    @Override
    public float getXPosF() {
      return sound.getXPosF();
    }

    @Override
    public float getYPosF() {
      return sound.getYPosF();
    }

    @Override
    public float getZPosF() {
      return sound.getZPosF();
    }

    @Override
    public AttenuationType getAttenuationType() {
      return sound.getAttenuationType();
    }
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this, 8),
        " s ",
        "sos",
        " s ",
        's', "dyeOrange",
        'o', Blocks.BONE_BLOCK);
  }
}
