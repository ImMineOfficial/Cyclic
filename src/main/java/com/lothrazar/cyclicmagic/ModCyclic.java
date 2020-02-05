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
package com.lothrazar.cyclicmagic;

import java.io.File;
import com.lothrazar.cyclicmagic.capability.IPlayerExtendedProperties;
import com.lothrazar.cyclicmagic.gui.ForgeGuiHandler;
import com.lothrazar.cyclicmagic.item.cannon.ParticleEventManager;
import com.lothrazar.cyclicmagic.item.core.BaseItemProjectile;
import com.lothrazar.cyclicmagic.module.ICyclicModule;
import com.lothrazar.cyclicmagic.module.MultiContent;
import com.lothrazar.cyclicmagic.module.dispenser.BehaviorProjectileThrowable;
import com.lothrazar.cyclicmagic.potion.PotionEffectRegistry;
import com.lothrazar.cyclicmagic.potion.PotionTypeRegistry;
import com.lothrazar.cyclicmagic.proxy.CommonProxy;
import com.lothrazar.cyclicmagic.registry.BlockRegistry;
import com.lothrazar.cyclicmagic.registry.CapabilityRegistry;
import com.lothrazar.cyclicmagic.registry.ConfigRegistry;
import com.lothrazar.cyclicmagic.registry.EnchantRegistry;
import com.lothrazar.cyclicmagic.registry.EventRegistry;
import com.lothrazar.cyclicmagic.registry.InterModCommsRegistry;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.MaterialRegistry;
import com.lothrazar.cyclicmagic.registry.ModuleRegistry;
import com.lothrazar.cyclicmagic.registry.PacketRegistry;
import com.lothrazar.cyclicmagic.registry.PermissionRegistry;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.registry.ReflectionRegistry;
import com.lothrazar.cyclicmagic.registry.SkillModule;
import com.lothrazar.cyclicmagic.registry.SoundRegistry;
import com.lothrazar.cyclicmagic.registry.VillagerProfRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilString;
import net.minecraft.block.BlockDispenser;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLFingerprintViolationEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@Mod(modid = Const.MODID, useMetadata = true, dependencies = "required:forge@[14.23.4.2705,);before:guideapi;after:jei;after:baubles,crafttweaker;after:fastbench@[1.5.3,)", canBeDeactivated = false, certificateFingerprint = "@FINGERPRINT@", updateJSON = "https://raw.githubusercontent.com/PrinceOfAmber/CyclicMagic/master/update.json", acceptableRemoteVersions = "[1.12,)", acceptedMinecraftVersions = "[1.12,)", guiFactory = "com.lothrazar." + Const.MODID + ".config.IngameConfigFactory")
public class ModCyclic {

  @Instance(value = Const.MODID)
  public static ModCyclic instance;
  @SidedProxy(clientSide = "com.lothrazar." + Const.MODID + ".proxy.ClientProxy", serverSide = "com.lothrazar." + Const.MODID + ".proxy.CommonProxy")
  public static CommonProxy proxy;
  public static ModLogger logger;
  public EventRegistry events;
  public static SimpleNetworkWrapper network;
  public final static ModCyclicCreativeTab TAB = new ModCyclicCreativeTab();
  @CapabilityInject(IPlayerExtendedProperties.class)
  public static final Capability<IPlayerExtendedProperties> CAPABILITYSTORAGE = null;
  static {
    FluidRegistry.enableUniversalBucket();//https://github.com/BluSunrize/ImmersiveEngineering/blob/c76e51998756a54c22dd40ac1877313bf95e8520/src/main/java/blusunrize/immersiveengineering/ImmersiveEngineering.java
  }

  @EventHandler
  public void onPreInit(FMLPreInitializationEvent event) {
    logger = new ModLogger(event.getModLog());
    ConfigRegistry.oreConfig = new Configuration(new File(event.getModConfigurationDirectory(), "cyclic_ores.cfg"));
    ConfigRegistry.init(new Configuration(event.getSuggestedConfigurationFile()));
    ConfigRegistry.register(logger);
    MinecraftForge.EVENT_BUS.register(new ParticleEventManager());
    network = NetworkRegistry.INSTANCE.newSimpleChannel(Const.MODID);
    PacketRegistry.register(network);
    SoundRegistry.register();
    CapabilityRegistry.register();
    ReflectionRegistry.register();
    MaterialRegistry.register();
    this.events = new EventRegistry();
    this.events.registerCoreEvents();
    ModuleRegistry.init();
    ModuleRegistry.registerAll();//create new instance of every module
    //content creation 
    CyclicContent.init();
    ConfigRegistry.syncAllConfig();
    for (ICyclicModule module : ModuleRegistry.modules) {
      module.onPreInit();
    }
    CyclicContent.register();
    proxy.preInit();
    //fluids still does the old way ( FluidRegistry.addBucketForFluid)
    //    MinecraftForge.EVENT_BUS.register(FluidsRegistry.class);
    MinecraftForge.EVENT_BUS.register(ItemRegistry.class);
    MinecraftForge.EVENT_BUS.register(BlockRegistry.class);
    MinecraftForge.EVENT_BUS.register(RecipeRegistry.class);
    MinecraftForge.EVENT_BUS.register(SoundRegistry.class);
    MinecraftForge.EVENT_BUS.register(PotionEffectRegistry.class);
    MinecraftForge.EVENT_BUS.register(PotionTypeRegistry.class);
    MinecraftForge.EVENT_BUS.register(EnchantRegistry.class);
    MinecraftForge.EVENT_BUS.register(VillagerProfRegistry.class);
  }

  @EventHandler
  public void onInit(FMLInitializationEvent event) {
    for (ICyclicModule module : ModuleRegistry.modules) {
      module.onInit();
    }
    proxy.init();
    NetworkRegistry.INSTANCE.registerGuiHandler(this, new ForgeGuiHandler());
    ConfigRegistry.syncAllConfig(); //fixes things , stuff was added to items and content that has config
    this.events.registerAll(); //important: register events AFTER modules onInit, since modules add events in this phase.
    PermissionRegistry.register();
    InterModCommsRegistry.register();
    ModCyclic.proxy.initColors();
  }

  @EventHandler
  public void onPostInit(FMLPostInitializationEvent event) {
    for (ICyclicModule module : ModuleRegistry.modules) {
      module.onPostInit();
    }
    SkillModule.onPostInit();
    /**
     * TODO: unit test module with a post init to do this stuff
     */
    if (logger.runUnitTests()) {
      UtilString.unitTests();
    }
    for (BaseItemProjectile item : MultiContent.projectiles) {
      BlockDispenser.DISPENSE_BEHAVIOR_REGISTRY.putObject(item, new BehaviorProjectileThrowable(item));
    }
  }

  @EventHandler
  public void onServerStarting(FMLServerStartingEvent event) {
    for (ICyclicModule module : ModuleRegistry.modules) {
      module.onServerStarting(event);
    }
  }

  @EventHandler
  public void onFingerprintViolation(FMLFingerprintViolationEvent event) {
    // https://tutorials.darkhax.net/tutorials/jar_signing/
    if (logger != null) {
      String source = (event.getSource() == null) ? "" : event.getSource().getName() + " ";
      String msg = "CYCLIC: Invalid fingerprint detected! The file " + source + "may have been tampered with. This version will NOT be supported by the author!";
      logger.log(msg);//use normal log so the logger config will hide it
    }
  }
}
