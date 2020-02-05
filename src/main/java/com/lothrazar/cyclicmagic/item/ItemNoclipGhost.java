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
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.capability.IPlayerExtendedProperties;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.item.core.ItemFoodCreative;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry.ChestType;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.lothrazar.cyclicmagic.util.UtilEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemNoclipGhost extends ItemFoodCreative implements IHasRecipe, IContent {

  //revived from https://github.com/PrinceOfAmber/Cyclic/blob/d2f91d1f97b9cfba47786a30b427fbfdcd714212/src/main/java/com/lothrazar/cyclicmagic/spell/SpellGhost.java
  public static int GHOST_SECONDS;
  public static int POTION_SECONDS;
  private static final int numFood = 2;

  public ItemNoclipGhost() {
    super(numFood, false);
    this.setAlwaysEdible();
  }

  @Override
  protected void onFoodEaten(ItemStack par1ItemStack, World world, EntityPlayer player) {
    setPlayerGhostMode(player, world);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltips, net.minecraft.client.util.ITooltipFlag advanced) {
    tooltips.add(UtilChat.lang(this.getTranslationKey() + ".tooltip"));
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this, 3),
        "lal", "lal", "lal",
        'l', Items.FERMENTED_SPIDER_EYE,
        'a', Items.CHORUS_FRUIT);
  }

  @Override
  public void register() {
    ItemRegistry.register(this, getContentName());
    ModCyclic.instance.events.register(this);
    LootTableRegistry.registerLoot(this);
    LootTableRegistry.registerLoot(this, ChestType.ENDCITY);
  }

  @Override
  public String getContentName() {
    return "corrupted_chorus";
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("CorruptedChorus(Food)", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
    String category = Const.ConfigCategory.modpackMisc;
    GHOST_SECONDS = config.getInt("CorruptedChorusSeconds", category, 10, 1, 600, "How long you can noclip after eating corrupted chorus");
    POTION_SECONDS = config.getInt("CorruptedChorusPotions", category, 10, 0, 600, "How long the negative potion effects last after a corrupted chorus teleports you");
  }

  private void setPlayerGhostMode(EntityPlayer player, World par2World) {
    if (par2World.isRemote == false) {
      player.setGameType(GameType.SPECTATOR);
    }
    IPlayerExtendedProperties props = CapabilityRegistry.getPlayerProperties(player);
    props.setChorusTimer(GHOST_SECONDS * Const.TICKS_PER_SEC);
    props.setChorusOn(true);
    props.setChorusStart(player.getPosition());
    props.setChorusDim(player.dimension);
    //and remove flying 
    ItemFlight.setNonFlying(player);
  }

  @SubscribeEvent
  public void onPlayerUpdate(LivingUpdateEvent event) {
    if (event.getEntityLiving() instanceof EntityPlayer == false) {
      return;
    }
    EntityPlayer player = (EntityPlayer) event.getEntityLiving();
    World world = player.getEntityWorld();
    IPlayerExtendedProperties props = CapabilityRegistry.getPlayerProperties(player);
    if (props.getChorusOn()) {
      int playerGhost = props.getChorusTimer();
      if (playerGhost > 0) {
        ModCyclic.proxy.closeSpectatorGui();
        props.setChorusTimer(playerGhost - 1);
        // no fall damage set
        player.fallDistance = 0.0F;
      }
      else {
        //times up!
        props.setChorusOn(false);
        if (props.getChorusDim() != player.dimension) {
          // if the player changed dimension while a ghost, thats not
          // allowed. dont tp them back
          player.setGameType(GameType.SURVIVAL);
          player.attackEntityFrom(DamageSource.MAGIC, 500);
        }
        else {
          BlockPos currentPos = player.getPosition();
          BlockPos sourcePos = props.getChorusStart();
          if (isPosValidTeleport(world, currentPos)) {
            //then we can stay, but add potions
            //if config allows
            if (POTION_SECONDS > 0) {
              player.addPotionEffect(new PotionEffect(MobEffects.WEAKNESS, Const.TICKS_PER_SEC * POTION_SECONDS));
              player.addPotionEffect(new PotionEffect(MobEffects.BLINDNESS, Const.TICKS_PER_SEC * POTION_SECONDS));
            }
          }
          else {
            //teleport back home	
            UtilEntity.teleportWallSafe(player, world, sourcePos);
          }
          player.fallDistance = 0.0F;
          player.setGameType(GameType.SURVIVAL);
        }
      }
    }
  }

  private boolean isPosValidTeleport(World world, BlockPos pos) {
    return world.isAirBlock(pos)
        && world.isAirBlock(pos.up())
        && pos.getY() > 0;
  }
}
