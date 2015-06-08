package net.minecraft.entity;

import net.canarymod.api.entity.vehicle.CanaryCommandBlockMinecart;
import net.minecraft.block.state.IBlockState;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;


public class EntityMinecartCommandBlock extends EntityMinecart {

    private final CommandBlockLogic a = new CommandBlockLogic() {

        public void h() {
            EntityMinecartCommandBlock.this.H().b(23, this.l());
            EntityMinecartCommandBlock.this.H().b(24, IChatComponent.Serializer.a(this.k()));
        }

        public BlockPos c() {
            return new BlockPos(EntityMinecartCommandBlock.this.s, EntityMinecartCommandBlock.this.t + 0.5D, EntityMinecartCommandBlock.this.u);
        }

        public Vec3 d() {
            return new Vec3(EntityMinecartCommandBlock.this.s, EntityMinecartCommandBlock.this.t, EntityMinecartCommandBlock.this.u);
        }

        public World e() {
            return EntityMinecartCommandBlock.this.o;
        }

        public Entity f() {
            return EntityMinecartCommandBlock.this;
        }

        @Override
        public net.canarymod.api.CommandBlockLogic getReference() {
            return EntityMinecartCommandBlock.this.getCanaryCommandBlockMinecart();
        }
    };
    private int b = 0;
   
    public EntityMinecartCommandBlock(World world) {
        super(world);
        this.entity = new CanaryCommandBlockMinecart(this); // CanaryMod: Wrap Entity
    }

    public EntityMinecartCommandBlock(World world, double d0, double d1, double d2) {
        super(world, d0, d1, d2);
        this.entity = new CanaryCommandBlockMinecart(this); // CanaryMod: Wrap Entity
    }

    protected void h() {
        super.h();
        this.H().a(23, "");
        this.H().a(24, "");
    }

    protected void a(NBTTagCompound nbttagcompound) {
        super.a(nbttagcompound);
        this.a.b(nbttagcompound);
        this.H().b(23, this.j().l());
        this.H().b(24, IChatComponent.Serializer.a(this.j().k()));
    }

    protected void b(NBTTagCompound nbttagcompound) {
        super.b(nbttagcompound);
        this.a.a(nbttagcompound);
    }

    public EntityMinecart.EnumMinecartType s() {
        return EntityMinecart.EnumMinecartType.COMMAND_BLOCK;
    }

    public IBlockState u() {
        return Blocks.bX.P();
    }

    public CommandBlockLogic j() {
        return this.a;
    }

    public void a(int i0, int i1, int i2, boolean flag0) {
        if (flag0 && this.W - this.b >= 4) {
            this.j().a(this.o);
            this.b = this.W;
        }

    }

    public boolean e(EntityPlayer entityplayer) {
        this.a.a(entityplayer);
        return false;
    }

    public void i(int i0) {
        super.i(i0);
        if (i0 == 24) {
            try {
                this.a.b(IChatComponent.Serializer.a(this.H().e(24)));
            } catch (Throwable throwable) {
                ;
            }
        } else if (i0 == 23) {
            this.a.a(this.H().e(23));
        }

    }

    // CanaryMod
    public CommandBlockLogic getLogic() {
        return this.a;
    }

    public CanaryCommandBlockMinecart getCanaryCommandBlockMinecart() {
        return (CanaryCommandBlockMinecart) this.entity;
    }
    //
}
