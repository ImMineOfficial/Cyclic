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
package com.lothrazar.cyclicmagic.net;

import com.lothrazar.cyclicmagic.gui.ITileFuel;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * generic packet handler for tile entities. assumes they handle cyclic fields that start over
 * 
 * TODO: see if we can reuse this mroe and remove uneccessary classes
 * 
 * @author Sam
 *
 */
public class PacketTileFuelDisplay implements IMessage, IMessageHandler<PacketTileFuelDisplay, IMessage> {

  private BlockPos pos;

  public PacketTileFuelDisplay() {}

  public PacketTileFuelDisplay(BlockPos p) {
    this.pos = p;
  }

  @Override
  public void fromBytes(ByteBuf buf) {
    NBTTagCompound tags = ByteBufUtils.readTag(buf);
    int x = tags.getInteger("x");
    int y = tags.getInteger("y");
    int z = tags.getInteger("z");
    pos = new BlockPos(x, y, z);
  }

  @Override
  public void toBytes(ByteBuf buf) {
    NBTTagCompound tags = new NBTTagCompound();
    tags.setInteger("x", pos.getX());
    tags.setInteger("y", pos.getY());
    tags.setInteger("z", pos.getZ());
    ByteBufUtils.writeTag(buf, tags);
  }

  @Override
  public IMessage onMessage(PacketTileFuelDisplay message, MessageContext ctx) {
    EntityPlayerMP player = ctx.getServerHandler().player;
    try {
      TileEntity tile = player.getEntityWorld().getTileEntity(message.pos);
      /**
       * TODO: eventually this packet could hit any fuel function, with type flag
       */
      if (tile != null && tile instanceof ITileFuel) {
        ITileFuel tileInvo = ((ITileFuel) tile);
        tileInvo.toggleFuelDisplay();
      }
    }
    catch (Exception e) {//since we dont know which class exactly this might get run on
      e.printStackTrace();
    }
    return null;
  }
}
