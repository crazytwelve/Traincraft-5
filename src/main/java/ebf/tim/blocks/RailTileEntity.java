package ebf.tim.blocks;

import javax.annotation.Nullable;

import org.lwjgl.opengl.GL11;

import ebf.XmlBuilder;
import ebf.tim.TrainsInMotion;
import ebf.tim.blocks.rails.RailShapeCore;
import ebf.tim.items.ItemRail;
import ebf.tim.registry.TiMBlocks;
import ebf.tim.registry.TiMItems;
import ebf.tim.render.models.Model1x1Rail;
import ebf.tim.utility.ClientProxy;
import fexcraft.tmt.slim.TextureManager;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.World;


public class RailTileEntity extends TileEntity {

    private AxisAlignedBB boundingBox = null;
    //management variables
    //todo public int snow=0;
    //todo public int timer=0;
    //todo public int overgrowth=0;
    public Integer railGLID=null;
    private int meta=0;
    private XmlBuilder data = new XmlBuilder();


    public int getMeta() {
        return meta;
    }
    public void setMeta(int i){
        meta=i;
        if(world!=null) {
        	world.setBlockMetadataWithNotify(pos, meta, 2);
        	//TODO >> world.setBlockState(pos, state, 2); ?
        }
        markDirty();
    }

    public void setData(XmlBuilder d){
        data=d;
        markDirty();
    }

    public XmlBuilder getData() {
        return data;
    }

    @Override
    public void addInfoToCrashReport(@Nullable CrashReportCategory report)  {
        if (report == null) {
            if (!world.isRemote) {
                return;
            }

            Minecraft.getMinecraft().entityRenderer.enableLightmap(1);
            TextureManager.adjustLightFixture(worldObj,xCoord,yCoord,zCoord);
            if(railGLID!=null && !ClientProxy.disableCache){
                if(!org.lwjgl.opengl.GL11.glIsList(railGLID)){
                    railGLID=null;
                    return;
                }
                org.lwjgl.opengl.GL11.glCallList(railGLID);
            }
            if(railGLID==null && data !=null && data.floatArrayMap.size()>0){
                RailShapeCore route =new RailShapeCore().fromXML(data);
                if (route.activePath!=null) {
                    if(!ClientProxy.disableCache) {
                        railGLID = net.minecraft.client.renderer.GLAllocation.generateDisplayLists(1);
                        org.lwjgl.opengl.GL11.glNewList(railGLID, org.lwjgl.opengl.GL11.GL_COMPILE);

                        Model1x1Rail.Model3DRail(worldObj, xCoord, yCoord, zCoord, route);

                        org.lwjgl.opengl.GL11.glEndList();
                    } else {
                        Model1x1Rail.Model3DRail(worldObj, xCoord, yCoord, zCoord, route);
                    }
                } // else {DebugUtil.println("NO DATA");}*/
            }
        } else {super.addInfoToCrashReport(report);}
    }

    @Override
    public boolean shouldRefresh(Block oldBlock, Block newBlock, int oldMeta, int newMeta, World world, int x, int y, int z) {
        return (oldBlock != newBlock) || (oldMeta != newMeta);
    }

    @Override
    public boolean canUpdate(){return false;}

    @Override
    public void updateEntity(){}

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        if (boundingBox == null) {
            boundingBox = AxisAlignedBB.getBoundingBox(xCoord-1, yCoord-1, zCoord-1, xCoord+1, yCoord, zCoord+1);
        }
        return boundingBox;
    }


    public void dropItem(){

        ItemStack drop = ItemRail.setStackData(
                new ItemStack(TiMItems.railItem, 1), data.getItemStack("rail"),
                data.getItemStack("ballast"), data.getItemStack("ties"), data.getItemStack("wires"));
        if(drop!=null) {
            world.spawnEntity(new EntityItem(world, xCoord, yCoord + 0.5f, zCoord, drop));
        }
    }


    public void markDirty() {
        super.markDirty();
        if (this.world != null) {
            world.markBlockForUpdate(xCoord, yCoord, zCoord);
            this.world.func_147453_f(this.xCoord, this.yCoord, this.zCoord, TiMBlocks.railBlock);
            if(world.isRemote && railGLID!=null) {
                org.lwjgl.opengl.GL11.glDeleteLists(railGLID, 1);
                railGLID = null;
            }
        }
        data.buildXML();

    }

    @Override
    public void onChunkUnload() {
        if(TrainsInMotion.proxy.isClient() && railGLID!=null){
            org.lwjgl.opengl.GL11.glDeleteLists(railGLID, 1);
            railGLID = null;
        }
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound nbttagcompound = new NBTTagCompound();
        this.writeToNBT(nbttagcompound);
        return new SPacketUpdateTileEntity(pos, 0, nbttagcompound);
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity pkt) {
        super.onDataPacket(net, pkt);
        if(pkt ==null){return;}
        readFromNBT(pkt.getNbtCompound());
        markDirty();
    }


    @Override
    public void writeToNBT(NBTTagCompound tag){
        super.writeToNBT(tag);
        tag.setInteger("meta", meta);
        if(data!=null && data.toXMLString()!=null && data.toXMLString().length()>0) {
            tag.setString("raildata", data.toXMLString());
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag){
        super.readFromNBT(tag);
        meta=tag.getInteger("meta");
        if(tag.hasKey("raildata")) {
            data = new XmlBuilder(tag.getString("raildata"));
        }
    }

}
