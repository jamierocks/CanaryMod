package net.minecraft.network;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Futures;
import io.netty.buffer.Unpooled;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import net.canarymod.Canary;
import net.canarymod.ToolBox;
import net.canarymod.api.CanaryNetServerHandler;
import net.canarymod.api.entity.living.humanoid.Player;
import net.canarymod.api.inventory.slot.ButtonPress;
import net.canarymod.api.inventory.slot.GrabMode;
import net.canarymod.api.inventory.slot.SecondarySlotType;
import net.canarymod.api.inventory.slot.SlotHelper;
import net.canarymod.api.inventory.slot.SlotType;
import net.canarymod.api.world.CanaryWorld;
import net.canarymod.api.world.blocks.BlockFace;
import net.canarymod.api.world.blocks.CanaryBlock;
import net.canarymod.api.world.position.BlockPosition;
import net.canarymod.api.world.position.Location;
import net.canarymod.config.Configuration;
import net.canarymod.hook.player.BlockRightClickHook;
import net.canarymod.hook.player.BookEditHook;
import net.canarymod.hook.player.DisconnectionHook;
import net.canarymod.hook.player.HeldItemChangeHook;
import net.canarymod.hook.player.KickHook;
import net.canarymod.hook.player.PlayerArmSwingHook;
import net.canarymod.hook.player.PlayerIdleHook;
import net.canarymod.hook.player.PlayerMoveHook;
import net.canarymod.hook.player.SignChangeHook;
import net.canarymod.hook.player.SlotClickHook;
import net.canarymod.hook.player.TeleportHook;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityMinecartCommandBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ContainerBeacon;
import net.minecraft.inventory.ContainerMerchant;
import net.minecraft.inventory.ContainerRepair;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.play.INetHandlerPlayServer;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C01PacketChatMessage;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.client.C07PacketPlayerDigging;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0APacketAnimation;
import net.minecraft.network.play.client.C0BPacketEntityAction;
import net.minecraft.network.play.client.C0CPacketInput;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C0EPacketClickWindow;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.network.play.client.C11PacketEnchantItem;
import net.minecraft.network.play.client.C12PacketUpdateSign;
import net.minecraft.network.play.client.C13PacketPlayerAbilities;
import net.minecraft.network.play.client.C14PacketTabComplete;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.network.play.client.C16PacketClientStatus;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C18PacketSpectate;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.server.S00PacketKeepAlive;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S07PacketRespawn;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.network.play.server.S09PacketHeldItemChange;
import net.minecraft.network.play.server.S18PacketEntityTeleport;
import net.minecraft.network.play.server.S23PacketBlockChange;
import net.minecraft.network.play.server.S2FPacketSetSlot;
import net.minecraft.network.play.server.S32PacketConfirmTransaction;
import net.minecraft.network.play.server.S3APacketTabComplete;
import net.minecraft.network.play.server.S40PacketDisconnect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.gui.IUpdatePlayerListBox;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.IntHashMap;
import net.minecraft.util.ReportedException;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.visualillusionsent.utils.DateUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

public class NetHandlerPlayServer implements INetHandlerPlayServer, IUpdatePlayerListBox {

    private static final Logger c = LogManager.getLogger();
    public final NetworkManager a;
    public final MinecraftServer d;// private to public
    public EntityPlayerMP b;
    private int e;
    private int f;
    private int g;
    private boolean h;
    private int i;
    private long j;
    private long k;
    private int l;
    private int m;
    private IntHashMap n = new IntHashMap();
    private double o;
    private double p;
    private double q;
    private boolean r = true;

    // CanaryMod
    protected CanaryNetServerHandler serverHandler;
    private final String lastJoin = DateUtils.longToDateTime(System.currentTimeMillis());
    //

    public NetHandlerPlayServer(MinecraftServer minecraftserver, NetworkManager networkmanager, EntityPlayerMP entityplayermp) {
        this.d = minecraftserver;
        this.a = networkmanager;
        networkmanager.a((INetHandler) this);
        this.b = entityplayermp;
        entityplayermp.a = this;
        serverHandler = new CanaryNetServerHandler(this);
    }

    public void c() {
        this.h = false;
        ++this.e;
        this.d.b.a("keepAlive");
        if ((long) this.e - this.k > 40L) {
            this.k = (long) this.e;
            this.j = this.d();
            this.i = (int) this.j;
            this.a((Packet) (new S00PacketKeepAlive(this.i)));
        }

        this.d.b.b();
        if (this.l > 0) {
            --this.l;
        }

        if (this.m > 0) {
            --this.m;
        }

        long timeIdle = MinecraftServer.ax() - this.b.D(); // CanaryMod: reduce, reuse, recycle
        if (this.b.D() > 0L && this.d.ay() > 0 && timeIdle > (long)(this.d.ay() * 1000 * 60) && !this.b.getPlayer().canIgnoreRestrictions()) { // CanaryMod: If IgnoreRestrictions/no idle kick
            // Player Idle Hook
            if (!((PlayerIdleHook) new PlayerIdleHook(this.b.getPlayer(), timeIdle).call()).isCanceled()) {
                this.c("You have been idle for too long!");
            }
            // Don't reset the time as there could be plugins extending allowed time per group/player
        }
    }

    public NetworkManager a() {
        return this.a;
    }

    public void c(String s0) {
        // CanaryMod: KickHook
        new KickHook(serverHandler.getUser(), Canary.getServer(), s0).call();
        // CanaryMod: Forward to kickNoHook
        this.kickNoHook(s0);
    }

    public void kickNoHook(String s0) {
        final ChatComponentText chatcomponenttext = new ChatComponentText(s0);
        this.a.a(new S40PacketDisconnect(chatcomponenttext),
                new GenericFutureListener() {
                    public void operationComplete(Future future) {
                        NetHandlerPlayServer.this.a.a((IChatComponent) chatcomponenttext);
                    }
                },
        new GenericFutureListener[0]);
        this.a.k();
        Futures.getUnchecked(this.d.a(
                                     new Runnable() {
                                         public void run() {
                                             NetHandlerPlayServer.this.a.l();
                                         }
                                     }
                                     )
                            );
    }

    public void a(C0CPacketInput c0cpacketinput) {
        PacketThreadUtil.a(c0cpacketinput, this, this.b.u());
        this.b.a(c0cpacketinput.a(), c0cpacketinput.b(), c0cpacketinput.c(), c0cpacketinput.d());
    }

    public void a(C03PacketPlayer c03packetplayer) {
        PacketThreadUtil.a(c03packetplayer, this, this.b.u());
        WorldServer worldserver = (WorldServer) this.b.getCanaryWorld().getHandle(); // this.d.a(this.b.aq);

        this.h = true;
        if (!this.b.i) {
            double d0 = this.b.s;
            double d1 = this.b.t;
            double d2 = this.b.u;
            double d3 = 0.0D;
            double d4 = c03packetplayer.a() - this.o;
            double d5 = c03packetplayer.b() - this.p;
            double d6 = c03packetplayer.c() - this.q;

            if (c03packetplayer.g()) {
                d3 = d4 * d4 + d5 * d5 + d6 * d6;
                if (!this.r && d3 < 0.25D) {
                    this.r = true;
                }
            }

            // CanaryMod: PlayerMoveHook
            Player player = this.b.getPlayer();
            // Need to use floorToBlock or the flooring doesnt come out right...
            if (ToolBox.floorToBlock(o) != ToolBox.floorToBlock(player.getX()) || ToolBox.floorToBlock(p) != ToolBox.floorToBlock(player.getY()) || ToolBox.floorToBlock(q) != ToolBox.floorToBlock(player.getZ())) {
                Location from = new Location(player.getWorld(), o, p, q, player.getPitch(), player.getRotation());// Remember rotation and pitch are swapped in Location constructor...
                PlayerMoveHook hook = (PlayerMoveHook) new PlayerMoveHook(player, from, player.getLocation()).call();
                if (hook.isCanceled()) {
                    // Return the player to their previous position gracefully, hopefully bypassing the TeleportHook and not going derp.
                    this.b.a.a(new S08PacketPlayerPosLook(from.getX(), from.getY() + 1.6200000047683716D, from.getZ(), from.getRotation(), from.getPitch(), Collections.emptySet()));
                    this.b.b(from.getX(), from.getY(), from.getZ()); // correct position server side to, or get BoUnCy
                    return;
                }
                //CanaryMod: Don't idle-kick when only moving
                this.b.z();
            }
            //

            if (this.r) {
                this.f = this.e;
                double d7;
                double d8;
                double d9;

                if (this.b.m != null) {
                    float f0 = this.b.y;
                    float f1 = this.b.z;

                    this.b.m.al();
                    d7 = this.b.s;
                    d8 = this.b.t;
                    d9 = this.b.u;
                    if (c03packetplayer.h()) {
                        f0 = c03packetplayer.d();
                        f1 = c03packetplayer.e();
                    }

                    this.b.C = c03packetplayer.f();
                    this.b.l();
                    this.b.a(d7, d8, d9, f0, f1);
                    if (this.b.m != null) {
                        this.b.m.al();
                    }

                    this.d.an().d(this.b);
                    if (this.b.m != null) {
                        if (d3 > 4.0D) {
                            Entity entity = this.b.m;

                            this.b.a.a((Packet) (new S18PacketEntityTeleport(entity)));
                            // CanaryMod: add dimension, world name, and teleport cause
                            this.a(this.b.s, this.b.t, this.b.u, this.b.y, this.b.z, this.b.am, this.b.getCanaryWorld().getName(), TeleportHook.TeleportCause.MOVEMENT);
                        }

                        this.b.m.ai = true;
                    }
                    if (this.r) {
                        this.o = this.b.s;
                        this.p = this.b.t;
                        this.q = this.b.u;
                    }

                    worldserver.g(this.b);
                    return;
                }

                if (this.b.bI()) {
                    this.b.l();
                    this.b.a(this.o, this.p, this.q, this.b.y, this.b.z);
                    worldserver.g(this.b);
                    return;
                }

                double d10 = this.b.t;

                this.o = this.b.s;
                this.p = this.b.t;
                this.q = this.b.u;
                d7 = this.b.s;
                d8 = this.b.t;
                d9 = this.b.u;
                float f2 = this.b.y;
                float f3 = this.b.z;

                if (c03packetplayer.g() && c03packetplayer.b() == -999.0D) {
                    c03packetplayer.a(false);
                }

                if (c03packetplayer.g()) {
                    d7 = c03packetplayer.a();
                    d8 = c03packetplayer.b();
                    d9 = c03packetplayer.c();
                    if (Math.abs(c03packetplayer.a()) > 3.0E7D || Math.abs(c03packetplayer.c()) > 3.0E7D) {
                        this.c("Illegal position");
                        return;
                    }
                }

                if (c03packetplayer.h()) {
                    f2 = c03packetplayer.d();
                    f3 = c03packetplayer.e();
                }

                this.b.l();
                this.b.a(this.o, this.p, this.q, f2, f3);
                if (!this.r) {
                    return;
                }

                double d11 = d7 - this.b.s;
                double d12 = d8 - this.b.t;
                double d13 = d9 - this.b.u;
                double d14 = Math.min(Math.abs(d11), Math.abs(this.b.v));
                double d15 = Math.min(Math.abs(d12), Math.abs(this.b.w));
                double d16 = Math.min(Math.abs(d13), Math.abs(this.b.x));
                double d17 = d14 * d14 + d15 * d15 + d16 * d16;

                if (d17 > 100.0D && (!this.d.S() || !this.d.R().equals(this.b.d_()))) {
                    c.warn(this.b.d_() + " moved too quickly! " + d11 + "," + d12 + "," + d13 + " (" + d14 + ", " + d15 + ", " + d16 + ")");
                    this.a(this.o, this.p, this.q, this.b.y, this.b.z, this.b.getCanaryWorld().getType().getId(), this.b.getCanaryWorld().getName(), TeleportHook.TeleportCause.MOVEMENT);
                    return;
                }

                float f4 = 0.0625F;
                boolean flag0 = worldserver.a((Entity) this.b, this.b.aQ().d((double) f4, (double) f4, (double) f4)).isEmpty();

                if (this.b.C && !c03packetplayer.f() && d12 > 0.0D) {
                    this.b.bE();
                }

                this.b.d(d11, d12, d13);
                this.b.C = c03packetplayer.f();
                double d18 = d12;

                d11 = d7 - this.b.s;
                d12 = d8 - this.b.t;
                if (d12 > -0.5D || d12 < 0.5D) {
                    d12 = 0.0D;
                }

                d13 = d9 - this.b.u;
                d17 = d11 * d11 + d12 * d12 + d13 * d13;
                boolean flag1 = false;

                if (d17 > 0.0625D && !this.b.bI() && !this.b.c.d()) {
                    flag1 = true;
                    c.warn(this.b.d_() + " moved wrongly!");
                }

                this.b.a(d7, d8, d9, f2, f3);
                this.b.k(this.b.s - d0, this.b.t - d1, this.b.u - d2);
                if (!this.b.T) {
                    boolean flag2 = worldserver.a((Entity) this.b, this.b.aQ().d((double) f4, (double) f4, (double) f4)).isEmpty();

                    if (flag0 && (flag1 || !flag2) && !this.b.bI()) {
                        this.a(this.o, this.p, this.q, f2, f3, b.getCanaryWorld().getType().getId(), b.getCanaryWorld().getName(), TeleportHook.TeleportCause.MOVEMENT);
                        return;
                    }
                }

                AxisAlignedBB axisalignedbb = this.b.aQ().b((double) f4, (double) f4, (double) f4).a(0.0D, -0.55D, 0.0D);

                // CanaryMod: check on flying capability instead of mode
                // moved allow-flight to per-world config
                if (!Configuration.getWorldConfig(worldserver.getCanaryWorld().getFqName()).isFlightAllowed()) { // CanaryMod: Check if flight is allowed
                    // CanaryMod:     on ground       |                isFlying                           |        ignorerestrictions         |      admin
                    if (!worldserver.c(axisalignedbb) && !this.b.getPlayer().getCapabilities().isFlying() && !(player.canIgnoreRestrictions() || player.isAdmin())) {
                        if (d18 >= -0.03125D) {
                            if (this.g > Configuration.getServerConfig().getFlightDetectTicks()) {
                                // CanaryMod: Configurable flight kicking
                                if (Configuration.getServerConfig().isFlightKickEnabled()) {
                                    c.warn(this.b.d_() + " was kicked for floating too long!");
                                    this.c("Flying is not enabled on this server");
                                    this.g = 0; // Reset the counter
                                    return;
                                }
                                else {
                                    // Log a warning if they aren't being kicked
                                    c.warn(this.b.d_() + " was detected floating too long! (Location: " + player.getLocation().asReadableString(Location.ReadMode.XYZ, Location.ReadMode.WORLD, Location.ReadMode.DIMENSION) + ")");
                                    this.g = 0; // Reset the counter
                                }
                            }
                        }
                    }
                    else {
                        this.g = 0;
                    }
                }

                this.b.C = c03packetplayer.f();
                this.d.an().d(this.b);
                this.b.a(this.b.t - d10, c03packetplayer.f());
            }
            else if (this.e - this.f > 20) {
                // CanaryMod: dimension, worldname, cause
                this.a(this.o, this.p, this.q, this.b.y, this.b.z, this.b.am, this.b.o.getCanaryWorld().getName(), TeleportHook.TeleportCause.MOVEMENT);
            }
        }
    }

    // CanaryMod: Add dimension world teleportCause
    public void a(double d0, double d1, double d2, float f0, float f1, int dimension, String world, TeleportHook.TeleportCause cause) {
        this.a(d0, d1, d2, f0, f1, Collections.emptySet(), dimension, world, cause);
    }

    // CanaryMod: Add dimension world teleportCause
    public void a(double d0, double d1, double d2, float f0, float f1, Set set, int dimension, String world, TeleportHook.TeleportCause cause) {
        // CanaryMod: TeleportHook
        net.canarymod.api.world.DimensionType dType = net.canarymod.api.world.DimensionType.fromId(dimension);
        String worldFq = world + "_" + dType.getName();
        boolean doAutoLoad = cause.equals(TeleportHook.TeleportCause.WARP) && Configuration.getWorldConfig(worldFq).allowWarpAutoLoad();
        net.canarymod.api.world.World dim = Canary.getServer().getWorldManager().getWorld(world, dType, doAutoLoad);
        Location location = new Location(dim, d0, d1, d2, f1, f0); // Remember rotation and pitch are swapped in Location constructor...
        TeleportHook hook = (TeleportHook) new TeleportHook(b.getPlayer(), location, cause).call();
        if (hook.isCanceled()) {
            return;
        }
        // CanaryMod: InterWorld/InterDimensional Travel
        if (this.b.getCanaryWorld() != dim) {
            Canary.getServer().getConfigurationManager().switchDimension(this.b.getPlayer(), dim, false);
        }
        //
        this.r = false;
        this.o = d0;
        this.p = d1;
        this.q = d2;
        if (set.contains(S08PacketPlayerPosLook.EnumFlags.X)) {
            this.o += this.b.s;
        }

        if (set.contains(S08PacketPlayerPosLook.EnumFlags.Y)) {
            this.p += this.b.t;
        }

        if (set.contains(S08PacketPlayerPosLook.EnumFlags.Z)) {
            this.q += this.b.u;
        }

        float f2 = f0;
        float f3 = f1;

        if (set.contains(S08PacketPlayerPosLook.EnumFlags.Y_ROT)) {
            f2 = f0 + this.b.y;
        }

        if (set.contains(S08PacketPlayerPosLook.EnumFlags.X_ROT)) {
            f3 = f1 + this.b.z;
        }

        this.b.a(this.o, this.p, this.q, f2, f3);
        this.b.a.a((Packet) (new S08PacketPlayerPosLook(d0, d1, d2, f0, f1, set)));
    }

    public void a(C07PacketPlayerDigging c07packetplayerdigging) {
        PacketThreadUtil.a(c07packetplayerdigging, this, this.b.u());
        WorldServer worldserver = (WorldServer) this.b.getCanaryWorld().getHandle(); // this.d.a(this.b.aq);
        BlockPos blockpos = c07packetplayerdigging.a();

        this.b.z();
        switch (NetHandlerPlayServer.SwitchAction.a[c07packetplayerdigging.c().ordinal()]) {
            case 1:
                if (!this.b.v()) {
                    this.b.a(false);
                }

                return;

            case 2:
                if (!this.b.v()) {
                    this.b.a(true);
                }

                return;
            case 3:
                this.b.bT();
                return;

            case 4:
            case 5:
            case 6:
                double d0 = this.b.s - ((double) blockpos.n() + 0.5D);
                double d1 = this.b.t - ((double) blockpos.o() + 0.5D) + 1.5D;
                double d2 = this.b.u - ((double) blockpos.p() + 0.5D);
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                if (d3 > 36.0D) {
                    return;
                }
                else if (blockpos.o() >= this.d.al()) {
                    return;
                }
                else {
                    if (c07packetplayerdigging.c() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                        // CanaryMod: SpawnBuild and CanBuild checks
                        if ((!this.d.a((World) worldserver, blockpos, (EntityPlayer) this.b) || this.b.getPlayer().hasPermission("canary.world.spawnbuild")) && this.b.getPlayer().canBuild()) {
                            this.b.c.a(blockpos, c07packetplayerdigging.b());
                        }
                        else {
                            this.b.a.a((Packet) (new S23PacketBlockChange(worldserver, blockpos)));
                        }
                    }
                    else {
                        if (c07packetplayerdigging.c() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                            this.b.c.a(blockpos);
                        }
                        else if (c07packetplayerdigging.c() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                            this.b.c.e();
                        }

                        if (worldserver.p(blockpos).c().r() != Material.a) {
                            this.b.a.a((Packet) (new S23PacketBlockChange(worldserver, blockpos)));
                        }
                    }

                    return;
                }
            default:
                throw new IllegalArgumentException("Invalid player action");
        }
    }

    private CanaryBlock lastRightClicked;

    public void a(C08PacketPlayerBlockPlacement c08packetplayerblockplacement) {
        PacketThreadUtil.a(c08packetplayerblockplacement, this, this.b.u());
        WorldServer worldserver = (WorldServer) this.b.getCanaryWorld().getHandle(); // this.d.a(this.c.ar);
        ItemStack itemstack = this.b.bg.h();
        boolean flag0 = false;
        BlockPos blockpos = c08packetplayerblockplacement.a();
        EnumFacing enumfacing = EnumFacing.a(c08packetplayerblockplacement.b());

        this.b.z();

        // CanaryMod: BlockRightClick/ItemUse
        CanaryBlock blockClicked = (CanaryBlock) worldserver.getCanaryWorld().getBlockAt(new BlockPosition(blockpos));
        blockClicked.setFaceClicked(BlockFace.fromByte((byte) c08packetplayerblockplacement.b()));

        if (c08packetplayerblockplacement.b() == 255) {
            // item use: use last right clicked block
            blockClicked = lastRightClicked;
            lastRightClicked = null;
        }
        else {
            lastRightClicked = blockClicked;
        }

        if (c08packetplayerblockplacement.b() == 255) {
            if (itemstack == null) {
                return;
            }

            blockClicked = blockClicked != null ? blockClicked : CanaryBlock.getPooledBlock(Block.d(0), new BlockPos(ToolBox.floorToBlock(this.o), ToolBox.floorToBlock(this.p), ToolBox.floorToBlock(this.q)), this.b.o);
            //
            this.b.c.itemUsed(this.b.getPlayer(), worldserver, itemstack, blockClicked); // CanaryMod: Redirect through ItemInWorldManager.itemUsed
        }
        else if (blockpos.o() >= this.d.al() - 1 && (enumfacing == EnumFacing.UP || blockpos.o() >= this.d.al())) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("build.tooHigh", new Object[]{Integer.valueOf(this.d.al())});

            chatcomponenttranslation.b().a(EnumChatFormatting.RED);
            this.b.a.a((Packet) (new S02PacketChat(chatcomponenttranslation)));
            flag0 = true;
        }
        else {
            if (this.r && this.b.e((double) blockpos.n() + 0.5D, (double) blockpos.o() + 0.5D, (double) blockpos.p() + 0.5D) < 64.0D && (!this.d.a((World) worldserver, blockpos, (EntityPlayer) this.b) || b.getPlayer().hasPermission("canary.world.spawnbuild")) && b.getPlayer().canBuild()) {
                // CanaryMod: BlockRightClicked
                BlockRightClickHook hook = (BlockRightClickHook) new BlockRightClickHook(b.getPlayer(), blockClicked).call();
                if (!hook.isCanceled()) {
                    this.b.c.a(this.b, worldserver, itemstack, blockpos, enumfacing, c08packetplayerblockplacement.d(), c08packetplayerblockplacement.e(), c08packetplayerblockplacement.f());
                }
                // NOTE: calling the BlockChange packet here and returning was causing ghosting (fake visible blocks)
            }
            //

            flag0 = true;
        }

        if (flag0) {
            this.b.a.a((Packet) (new S23PacketBlockChange(worldserver, blockpos)));
            this.b.a.a((Packet) (new S23PacketBlockChange(worldserver, blockpos.a(enumfacing))));
        }

        itemstack = this.b.bg.h();
        if (itemstack != null && itemstack.b == 0) {
            this.b.bg.a[this.b.bg.c] = null;
            itemstack = null;
        }

        if (itemstack == null || itemstack.l() == 0) {
            this.b.g = true;
            this.b.bg.a[this.b.bg.c] = ItemStack.b(this.b.bg.a[this.b.bg.c]);
            Slot slot = this.b.bi.a((IInventory) this.b.bg, this.b.bg.c);

            this.b.bi.b();
            this.b.g = false;
            if (!ItemStack.b(this.b.bg.h(), c08packetplayerblockplacement.c())) {
                this.a((Packet) (new S2FPacketSetSlot(this.b.bi.d, slot.e, this.b.bg.h())));
            }
        }
    }

    public void a(C18PacketSpectate c18packetspectate) {
        PacketThreadUtil.a(c18packetspectate, this, this.b.u());
        if (this.b.v()) {
            Entity entity = null;
            /* CanaryMod: Multiworld incompatible
            WorldServer[] aworldserver = this.d.c;
            int i0 = aworldserver.length;

            for (int i1 = 0; i1 < i0; ++i1) {
                WorldServer worldserver = aworldserver[i1];

                if (worldserver != null) {
                    entity = c18packetspectate.a(worldserver);
                    if (entity != null) {
                        break;
                    }
                }
            }
            */
            // CanaryMod: Multiworld compatible
            for (net.canarymod.api.world.World world : Canary.getServer().getWorldManager().getAllWorlds()) {
                WorldServer worldserver = (WorldServer) ((CanaryWorld) world).getHandle();
                entity = c18packetspectate.a(worldserver);
                if (entity != null) {
                    break;
                }
            }
            //

            if (entity != null) {
                this.b.e(this.b);
                this.b.a((Entity) null);
                if (entity.o != this.b.o) {
                    WorldServer worldserver1 = this.b.u();
                    WorldServer worldserver2 = (WorldServer) entity.o;

                    this.b.am = entity.am;
                    this.a((Packet) (new S07PacketRespawn(this.b.am, worldserver1.aa(), worldserver1.P().u(), this.b.c.b())));
                    worldserver1.f(this.b);
                    this.b.I = false;
                    this.b.b(entity.s, entity.t, entity.u, entity.y, entity.z);
                    if (this.b.ai()) {
                        worldserver1.a((Entity) this.b, false);
                        worldserver2.d(this.b);
                        worldserver2.a((Entity) this.b, false);
                    }

                    this.b.a((World) worldserver2);
                    this.d.an().a(this.b, worldserver1);
                    this.b.a(entity.s, entity.t, entity.u);
                    this.b.c.a(worldserver2);
                    this.d.an().b(this.b, worldserver2);
                    this.d.an().f(this.b);
                }
                else {
                    this.b.a(entity.s, entity.t, entity.u);
                }
            }
        }
    }

    public void a(C19PacketResourcePackStatus c19packetresourcepackstatus) {
    }

    public void a(IChatComponent ichatcomponent) {
        c.info(this.b.d_() + " lost connection: " + ichatcomponent.e());
        this.b.getPlayer().getMetaData().put("LastJoin", lastJoin); // Respawning could push this around, so save at a true disconnect
        this.d.aF();
        ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("multiplayer.player.left", new Object[]{this.b.e_()});
        chatcomponenttranslation.b().a(EnumChatFormatting.YELLOW);

        // CanaryMod: DisconnectionHook
        DisconnectionHook hook = (DisconnectionHook) new DisconnectionHook(this.b.getPlayer(), ichatcomponent.e(), chatcomponenttranslation.e()).call();
        if (!hook.isHidden()) {
            this.d.an().a((IChatComponent) chatcomponenttranslation);
        }
        //
        this.b.q();
        this.d.an().e(this.b);
        // CanaryMod unregester Custom Payload registrations
        Canary.channels().unregisterClientAll(serverHandler);
        // End
        if (this.d.S() && this.b.d_().equals(this.d.R())) {
            c.info("Stopping singleplayer server as player logged out");
            this.d.u();
        }
    }

    public void a(final Packet packet) {
        if (packet instanceof S02PacketChat) {
            S02PacketChat s02packetchat = (S02PacketChat) packet;
            EntityPlayer.EnumChatVisibility entityplayer_enumchatvisibility = this.b.y();

            if (entityplayer_enumchatvisibility == EntityPlayer.EnumChatVisibility.HIDDEN) {
                return;
            }

            if (entityplayer_enumchatvisibility == EntityPlayer.EnumChatVisibility.SYSTEM && !s02packetchat.b()) {
                return;
            }
        }

        try {
            this.a.a(packet);
        }
        catch (Throwable throwable) {
            CrashReport crashreport = CrashReport.a(throwable, "Sending packet");
            CrashReportCategory crashreportcategory = crashreport.a("Packet being sent");

            crashreportcategory.a("Packet class", new Callable() {

                        public String a() {
                            return packet.getClass().getCanonicalName();
                        }

                        public Object call() {
                            return this.a();
                        }
                    }
            );
            throw new ReportedException(crashreport);
        }
    }

    public void a(C09PacketHeldItemChange c09packethelditemchange) {
        PacketThreadUtil.a(c09packethelditemchange, this, this.b.u());
        if (c09packethelditemchange.a() >= 0 && c09packethelditemchange.a() < InventoryPlayer.i()) {
            // CanaryMod: HeldItemChange Hook
            HeldItemChangeHook heldItemChangeHook = (HeldItemChangeHook) new HeldItemChangeHook(this.b.getPlayer(), this.b.bg.c, c09packethelditemchange.a()).call();
            this.b.bg.c = Math.min(Math.max(heldItemChangeHook.getNewValue(), 0), InventoryPlayer.i());
            //update helditem
            if (this.b.bg.c != c09packethelditemchange.a()){
                a.a(new S09PacketHeldItemChange(this.b.bg.c));
            }
            this.b.z();
        }
        else {
            c.warn(this.b.d_() + " tried to set an invalid carried item");
        }
    }

    @Override
    public void a(C01PacketChatMessage c01packetchatmessage) {
        PacketThreadUtil.a(c01packetchatmessage, this, this.b.u());
        /* Diff visibility funkyness
        if (this.b.u() == EntityPlayer.EnumChatVisibility.HIDDEN) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("chat.cannotSend", new Object[0]);

            chatcomponenttranslation.b().a(EnumChatFormatting.RED);
            this.a((Packet) (new S02PacketChat(chatcomponenttranslation)));
        } 
        else {
            this.b.v();
            String s0 = c01packetchatmessage.c();

            s0 = StringUtils.normalizeSpace(s0);

            for (int i0 = 0; i0 < s0.length(); ++i0) {
                if (!ChatAllowedCharacters.a(s0.charAt(i0))) {
                    this.c("Illegal characters in chat");
                    return;
                }
            }

            if (s0.startsWith("/")) {
                this.d(s0);
            } 
            else {
                ChatComponentTranslation chatcomponenttranslation1 = new ChatComponentTranslation("chat.type.text", new Object[] { this.b.c_(), s0});

                this.d.ah().a(chatcomponenttranslation1, false);
            }

            this.l += 20;
            if (this.l > 200 && !this.d.ah().g(this.b.bJ())) {
                this.c("disconnect.spam");
            }

        }
        }*/
        // CanaryMod: Re-route to Player chat
        if (this.b.y() == EntityPlayer.EnumChatVisibility.HIDDEN) {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("chat.cannotSend", new Object[0]);

            chatcomponenttranslation.b().a(EnumChatFormatting.RED);
            this.a((Packet) (new S02PacketChat(chatcomponenttranslation)));
            return;
        }
        // Reuse Anti-Spam but implement our config into the system
        this.l += 20;
        // OP or Ignores restrictions
        boolean op = Canary.ops().isOpped(this.b.aJ().toString()), ignore = this.b.getPlayer().canIgnoreRestrictions();
        String spamProLvl = Configuration.getServerConfig().getSpamProtectionLevel();
        if (spamProLvl.toLowerCase().equals("all") || (spamProLvl.toLowerCase().equals("default") && !(op || ignore))) {
            if (this.l > 200) {
                this.c("disconnect.spam");
                return;
            }
        }
        this.b.z(); //Not Idle
        this.b.getPlayer().chat(c01packetchatmessage.a());
        //
    }

    private void d(String s0) {
        this.d.O().a(this.b, s0);
    }

    public void a(C0APacketAnimation c0apacketanimation) {
        PacketThreadUtil.a(c0apacketanimation, this, this.b.u());
        this.b.z();
        // CanaryMod: ArmSwingHook
        if (this.b.showAnimation()) { // Best guess at a replacement for animation state
            new PlayerArmSwingHook(this.b.getPlayer()).call();
        }
        //
        this.b.bv();
    }

    public void a(C0BPacketEntityAction c0bpacketentityaction) {
        PacketThreadUtil.a(c0bpacketentityaction, this, this.b.u());
        this.b.z();
        switch (NetHandlerPlayServer.SwitchAction.b[c0bpacketentityaction.b().ordinal()]) {
            case 1:
                this.b.c(true);
                break;

            case 2:
                this.b.c(false);
                break;

            case 3:
                this.b.d(true);
                break;

            case 4:
                this.b.d(false);
                break;

            case 5:
                this.b.a(false, true, true);
                this.r = false;
                break;

            case 6:
                if (this.b.m instanceof EntityHorse) {
                    ((EntityHorse) this.b.m).v(c0bpacketentityaction.c());
                }
                break;

            case 7:
                if (this.b.m instanceof EntityHorse) {
                    ((EntityHorse) this.b.m).g(this.b);
                }
                break;

            default:
                throw new IllegalArgumentException("Invalid client command!");
        }
    }

    public void a(C02PacketUseEntity c02packetuseentity) {
        PacketThreadUtil.a(c02packetuseentity, this, this.b.u());
        WorldServer worldserver = (WorldServer) this.b.getCanaryWorld().getHandle(); // this.d.a(this.b.aq);
        Entity entity = c02packetuseentity.a((World) worldserver);

        this.b.z();
        if (entity != null) {
            boolean flag0 = this.b.t(entity);
            double d0 = 36.0D;

            if (!flag0) {
                d0 = 9.0D;
            }

            if (this.b.h(entity) < d0) {
                if (c02packetuseentity.a() == C02PacketUseEntity.Action.INTERACT) {
                    this.b.u(entity);
                }
                else if (c02packetuseentity.a() == C02PacketUseEntity.Action.INTERACT_AT) {
                    entity.a((EntityPlayer) this.b, c02packetuseentity.b());
                }
                else if (c02packetuseentity.a() == C02PacketUseEntity.Action.ATTACK) {
                    if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof EntityArrow || entity == this.b) {
                        this.c("Attempting to attack an invalid entity");
                        this.d.f("Player " + this.b.d_() + " tried to attack an invalid entity");
                        return;
                    }

                    this.b.f(entity);
                }
            }
        }
    }

    public void a(C16PacketClientStatus c16packetclientstatus) {
        PacketThreadUtil.a(c16packetclientstatus, this, this.b.u());
        this.b.z();
        C16PacketClientStatus.EnumState c16packetclientstatus_enumstate = c16packetclientstatus.a();

        switch (NetHandlerPlayServer.SwitchAction.c[c16packetclientstatus_enumstate.ordinal()]) {
            case 1:
                if (this.b.i) {
                    this.b = this.d.an().a(this.b, 0, true);
                }
                else if (this.b.u().P().t()) {
                    if (this.d.S() && this.b.d_().equals(this.d.R())) {
                        this.b.a.c("You have died. Game over, man, it\'s game over!");
                        this.d.Z();
                    }
                    else {
                        // CanaryMod use our Ban System instead
                        Canary.bans().issueBan(this.b.getPlayer(), "Death in Hardcore");
                        this.b.a.c("You have died. Game over, man, it\'s game over!");
                    }
                }
                else {
                    if (this.b.bm() > 0.0F) {
                        return;
                    }

                    this.b = this.d.an().a(this.b, 0, false);
                }
                break;

            case 2:
                this.b.A().a(this.b);
                break;

            case 3:
                this.b.b((StatBase) AchievementList.f);
        }
    }

    public void a(C0DPacketCloseWindow c0dpacketclosewindow) {
        PacketThreadUtil.a(c0dpacketclosewindow, this, this.b.u());
        this.b.p();
    }

    public void a(C0EPacketClickWindow c0epacketclickwindow) {
        PacketThreadUtil.a(c0epacketclickwindow, this, this.b.u());
        this.b.z();
        if (this.b.bi.d == c0epacketclickwindow.a() && this.b.bi.c(this.b)) {
            if (this.b.v()) { // Is Spectator
                ArrayList arraylist = Lists.newArrayList();

                for (int i0 = 0; i0 < this.b.bi.c.size(); ++i0) {
                    arraylist.add(((Slot) this.b.bi.c.get(i0)).d());
                }

                this.b.a(this.b.bi, (List) arraylist);
            }
            else {
                // CanaryMod: SlotClick
                // This already updates the inventory, don't use it when we wanna cancel this transaction just now (look further down)
//                ItemStack itemstack = this.b.bi.a(c0epacketclickwindow.b(), c0epacketclickwindow.c(), c0epacketclickwindow.f(), this.b);

                // Temporary item
                ItemStack itemstack = c0epacketclickwindow.b() > -1 ? this.b.bi.getSlot(c0epacketclickwindow.b()).d() : null;
                SlotType slotType = SlotHelper.getSlotType(this.b.bi, c0epacketclickwindow.b());
                SecondarySlotType secondarySlotType = SlotHelper.getSpecificSlotType(this.b.bi, c0epacketclickwindow.b());
                GrabMode grabMode = GrabMode.fromInt(c0epacketclickwindow.f());

                ButtonPress mouseClick = ButtonPress.matchButton(grabMode, c0epacketclickwindow.c(), c0epacketclickwindow.b());
                SlotClickHook sch = (SlotClickHook)new SlotClickHook(this.b.getPlayer(), this.b.bi.getInventory(), itemstack != null ? itemstack.getCanaryItem() : null, slotType, secondarySlotType, grabMode, mouseClick, (short)c0epacketclickwindow.b(), c0epacketclickwindow.d()).call();
                if (sch.isCanceled()) {
                    if (sch.doUpdate()) {
                        if (c0epacketclickwindow.f() == 0) {
                            this.b.bi.updateSlot(c0epacketclickwindow.b());
                        }
                        else {
                            ArrayList arraylist = Lists.newArrayList();

                            for (int i0 = 0; i0 < this.b.bi.c.size(); ++i0) {
                                arraylist.add(((Slot)this.b.bi.c.get(i0)).d());
                            }

                            this.b.a(this.b.bi, (List)arraylist);
                        }
                        this.b.o(); // Update item in hand
                    }
                    return;
                }
                // Not cancelled, properly handle the item now
                itemstack = this.b.bi.a(c0epacketclickwindow.b(), c0epacketclickwindow.c(), c0epacketclickwindow.f(), this.b);
                //

                if (ItemStack.b(c0epacketclickwindow.e(), itemstack)) {
                    this.b.a.a((Packet) (new S32PacketConfirmTransaction(c0epacketclickwindow.a(), c0epacketclickwindow.d(), true)));
                    this.b.g = true;
                    this.b.bi.b();
                    this.b.o();
                    this.b.g = false;
                }
                else {
                    this.n.a(this.b.bi.d, Short.valueOf(c0epacketclickwindow.d()));
                    this.b.a.a((Packet) (new S32PacketConfirmTransaction(c0epacketclickwindow.a(), c0epacketclickwindow.d(), false)));
                    this.b.bi.a(this.b, false);
                    ArrayList arraylist1 = Lists.newArrayList();

                    for (int i1 = 0; i1 < this.b.bi.c.size(); ++i1) {
                        arraylist1.add(((Slot) this.b.bi.c.get(i1)).d());
                    }

                    this.b.a(this.b.bi, (List) arraylist1);
                }
            }
        }
    }

    public void a(C11PacketEnchantItem c11packetenchantitem) {
        PacketThreadUtil.a(c11packetenchantitem, this, this.b.u());
        this.b.z();
        if (this.b.bi.d == c11packetenchantitem.a() && this.b.bi.c(this.b) && !this.b.v()) {
            this.b.bi.a((EntityPlayer) this.b, c11packetenchantitem.b());
            this.b.bi.b();
        }
    }

    public void a(C10PacketCreativeInventoryAction c10packetcreativeinventoryaction) {
        PacketThreadUtil.a(c10packetcreativeinventoryaction, this, this.b.u());
        if (this.b.c.d()) {
            boolean flag0 = c10packetcreativeinventoryaction.a() < 0;
            ItemStack itemstack = c10packetcreativeinventoryaction.b();

            if (itemstack != null && itemstack.n() && itemstack.o().b("BlockEntityTag", 10)) {
                NBTTagCompound nbttagcompound = itemstack.o().m("BlockEntityTag");

                if (nbttagcompound.c("x") && nbttagcompound.c("y") && nbttagcompound.c("z")) {
                    BlockPos blockpos = new BlockPos(nbttagcompound.f("x"), nbttagcompound.f("y"), nbttagcompound.f("z"));
                    TileEntity tileentity = this.b.o.s(blockpos);

                    if (tileentity != null) {
                        NBTTagCompound nbttagcompound1 = new NBTTagCompound();

                        tileentity.b(nbttagcompound1);
                        nbttagcompound1.o("x");
                        nbttagcompound1.o("y");
                        nbttagcompound1.o("z");
                        itemstack.a("BlockEntityTag", (NBTBase) nbttagcompound1);
                    }
                }
            }

            boolean flag1 = c10packetcreativeinventoryaction.a() >= 1 && c10packetcreativeinventoryaction.a() < 36 + InventoryPlayer.i();
            boolean flag2 = itemstack == null || itemstack.b() != null;
            boolean flag3 = itemstack == null || itemstack.i() >= 0 && itemstack.b <= 64 && itemstack.b > 0;

            if (flag1 && flag2 && flag3) {
                if (itemstack == null) {
                    this.b.bh.a(c10packetcreativeinventoryaction.a(), (ItemStack) null);
                }
                else {
                    this.b.bh.a(c10packetcreativeinventoryaction.a(), itemstack);
                }

                this.b.bh.a(this.b, true);
            }
            else if (flag0 && flag2 && flag3 && this.m < 200) {
                this.m += 20;
                EntityItem entityitem = this.b.a(itemstack, true);

                if (entityitem != null) {
                    entityitem.j();
                }
            }
        }
    }

    public void a(C0FPacketConfirmTransaction c0fpacketconfirmtransaction) {
        PacketThreadUtil.a(c0fpacketconfirmtransaction, this, this.b.u());
        Short oshort = (Short) this.n.a(this.b.bi.d);

        if (oshort != null && c0fpacketconfirmtransaction.b() == oshort.shortValue() && this.b.bi.d == c0fpacketconfirmtransaction.a() && !this.b.bi.c(this.b) && !this.b.v()) {
            this.b.bi.a(this.b, true);
        }
    }

    public void a(C12PacketUpdateSign c12packetupdatesign) {
        PacketThreadUtil.a(c12packetupdatesign, this, this.b.u());
        this.b.z();
        WorldServer worldserver = (WorldServer) this.b.getCanaryWorld().getHandle(); // this.d.a(this.b.aq);
        BlockPos blockpos = c12packetupdatesign.a();

        if (worldserver.e(blockpos)) {
            TileEntity tileentity = worldserver.s(blockpos);

            if (!(tileentity instanceof TileEntitySign)) {
                return;
            }

            TileEntitySign tileentitysign = (TileEntitySign) tileentity;

            if (!tileentitysign.b() || tileentitysign.c() != this.b) {
                this.d.f("Player " + this.b.d_() + " just tried to change non-editable sign");
                return;
            }

            // CanaryMod: Copy the old line text
            IChatComponent[] old = Arrays.copyOf(tileentitysign.a, tileentitysign.a.length);
            //
            System.arraycopy(c12packetupdatesign.b(), 0, tileentitysign.a, 0, 4);
            // CanaryMod: SignChange Hook
            SignChangeHook hook = (SignChangeHook) new SignChangeHook(b.getPlayer(), tileentitysign.getCanarySign()).call();
            if (hook.isCanceled()) {
                System.arraycopy(old, 0, tileentitysign.a, 0, 4); // Restore old text
            }
            //

            tileentitysign.o_();
            worldserver.h(blockpos);
        }
    }

    public void a(C00PacketKeepAlive c00packetkeepalive) {
        if (c00packetkeepalive.a() == this.i) {
            int i0 = (int) (this.d() - this.j);

            this.b.h = (this.b.h * 3 + i0) / 4;
        }
    }

    private long d() {
        return System.nanoTime() / 1000000L;
    }

    public void a(C13PacketPlayerAbilities c13packetplayerabilities) {
        PacketThreadUtil.a(c13packetplayerabilities, this, this.b.u());
        this.b.by.b = c13packetplayerabilities.b() && this.b.by.c;
    }

    public void a(C14PacketTabComplete c14packettabcomplete) {
        PacketThreadUtil.a(c14packettabcomplete, this, this.b.u());
        ArrayList arraylist = Lists.newArrayList();
        Iterator iterator = this.d.a((ICommandSender) this.b, c14packettabcomplete.a(), c14packettabcomplete.b()).iterator();

        while (iterator.hasNext()) {
            String s0 = (String) iterator.next();

            arraylist.add(s0);
        }

        this.b.a.a((Packet) (new S3APacketTabComplete((String[]) arraylist.toArray(new String[arraylist.size()]))));
    }

    public void a(C15PacketClientSettings c15packetclientsettings) {
        PacketThreadUtil.a(c15packetclientsettings, this, this.b.u());
        this.b.a(c15packetclientsettings);
    }

    public void a(C17PacketCustomPayload c17packetcustompayload) {
        PacketThreadUtil.a(c17packetcustompayload, this, this.b.u());
        PacketBuffer packetbuffer;
        ItemStack itemstack;
        ItemStack itemstack1;

        if ("MC|BEdit".equals(c17packetcustompayload.a())) {
            packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(c17packetcustompayload.b()));

            try {
                itemstack = packetbuffer.i();
                if (itemstack == null) {
                    return;
                }
                if (!ItemWritableBook.b(itemstack.o())) {
                    throw new IOException("Invalid book tag!");
                }

                itemstack1 = this.b.bg.h();
                if (itemstack1 != null) {
                    if (itemstack.b() == Items.bM && itemstack.b() == itemstack1.b()) {
                        BookEditHook beh = (BookEditHook) new BookEditHook(itemstack.getCanaryItem(), this.b.getPlayer()).call();
                        if (!beh.isCanceled()) {
                            itemstack1.a("pages", (NBTBase) itemstack.o().c("pages", 8));
                        }
                        return;
                    }
                }
            }
            catch (Exception exception) {
                c.error("Couldn\'t handle book info", exception);
                return;
            }
            finally {
                packetbuffer.release();
            }

            return;
        }
        else if ("MC|BSign".equals(c17packetcustompayload.a())) {
            packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(c17packetcustompayload.b()));
            try {
                itemstack = packetbuffer.i();
                if (itemstack == null) {
                    return;
                }

                if (!ItemEditableBook.b(itemstack.o())) {
                    throw new IOException("Invalid book tag!");
                }

                itemstack1 = this.b.bg.h();
                if (itemstack1 != null) {
                    if (itemstack.b() == Items.bN && itemstack1.b() == Items.bM) {
                        itemstack1.a("author", (NBTBase) (new NBTTagString(this.b.d_())));
                        itemstack1.a("title", (NBTBase) (new NBTTagString(itemstack.o().j("title"))));
                        itemstack1.a("pages", (NBTBase) itemstack.o().c("pages", 8));
                        itemstack1.a(Items.bN);
                    }

                    return;
                }
            }
            catch (Exception exception1) {
                c.error("Couldn\'t sign book", exception1);
                return;
            }
            finally {
                packetbuffer.release();
            }

            return;
        }
        else if ("MC|TrSel".equals(c17packetcustompayload.a())) {
            try {
                int i0 = c17packetcustompayload.b().readInt();
                Container container = this.b.bi;

                if (container instanceof ContainerMerchant) {
                    ((ContainerMerchant) container).d(i0);
                }
            }
            catch (Exception exception2) {
                c.error("Couldn\'t select trade", exception2);
            }
        }
        else if ("MC|AdvCdm".equals(c17packetcustompayload.a())) {
            if (!this.d.aj()) {
                this.b.a((IChatComponent) (new ChatComponentTranslation("advMode.notEnabled", new Object[0])));
            }
            else if (this.b.a(2, "") && this.b.by.d) {
                packetbuffer = c17packetcustompayload.b();
                try {
                    byte b0 = packetbuffer.readByte();
                    CommandBlockLogic commandblocklogic = null;

                    if (b0 == 0) {
                        TileEntity tileentity = this.b.o.s(new BlockPos(packetbuffer.readInt(), packetbuffer.readInt(), packetbuffer.readInt()));

                        if (tileentity instanceof TileEntityCommandBlock) {
                            commandblocklogic = ((TileEntityCommandBlock) tileentity).b();
                        }
                    }
                    else if (b0 == 1) {
                        Entity entity = this.b.o.a(packetbuffer.readInt());

                        if (entity instanceof EntityMinecartCommandBlock) {
                            commandblocklogic = ((EntityMinecartCommandBlock) entity).j();
                        }
                    }

                    String s0 = packetbuffer.c(packetbuffer.readableBytes());
                    boolean flag0 = packetbuffer.readBoolean();

                    if (commandblocklogic != null) {
                        commandblocklogic.a(s0);
                        commandblocklogic.a(flag0);
                        if (!flag0) {
                            commandblocklogic.b((IChatComponent) null);
                        }

                        commandblocklogic.h();
                        this.b.a((IChatComponent) (new ChatComponentTranslation("advMode.setCommand.success", new Object[]{s0})));
                    }
                }
                catch (Exception exception3) {
                    c.error("Couldn\'t set command block", exception3);
                }
                finally {
                    packetbuffer.release();
                }
            }
            else {
                this.b.a((IChatComponent) (new ChatComponentTranslation("advMode.notAllowed", new Object[0])));
            }
        }
        else if ("MC|Beacon".equals(c17packetcustompayload.a())) {
            if (this.b.bi instanceof ContainerBeacon) {
                try {
                    packetbuffer = c17packetcustompayload.b();
                    int i1 = packetbuffer.readInt();
                    int i2 = packetbuffer.readInt();
                    ContainerBeacon containerbeacon = (ContainerBeacon) this.b.bi;
                    Slot slot = containerbeacon.a(0);

                    if (slot.e()) {
                        slot.a(1);
                        IInventory iinventory = containerbeacon.e();

                        iinventory.b(1, i1);
                        iinventory.b(2, i2);
                        iinventory.o_();
                    }
                }
                catch (Exception exception4) {
                    c.error("Couldn\'t set beacon", exception4);
                }
            }
        }
        else if ("MC|ItemName".equals(c17packetcustompayload.a()) && this.b.bi instanceof ContainerRepair) {
            ContainerRepair containerrepair = (ContainerRepair) this.b.bi;

            if (c17packetcustompayload.b() != null && c17packetcustompayload.b().readableBytes() >= 1) {
                String s1 = ChatAllowedCharacters.a(c17packetcustompayload.b().c(32767));

                if (s1.length() <= 30) {
                    containerrepair.a(s1);
                }
            }
            else {
                containerrepair.a("");
            }
        }
        // CanaryMod: Custom Payload implementation!
        else if ("REGISTER".equals(c17packetcustompayload.a())) {
            try {
                if (c17packetcustompayload.b() != null && c17packetcustompayload.b().readableBytes() >= 1) {
                    packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(c17packetcustompayload.b()));
                    String channel = packetbuffer.c(packetbuffer.readableBytes());
                    for (String chan : channel.split("\0")) {
                        Canary.channels().registerClient(chan, this.serverHandler);
                    }
                    Canary.log.info(String.format("Player '%s' registered Custom Payload on channel(s) '%s'", this.b.getPlayer().getName(), Arrays.toString(channel.split("\0"))));
                }
            }
            catch (Exception ex) {
                c.error("Error receiving 'C17PacketCustomPayload': " + ex.getMessage(), ex);
            }
        }
        else if ("UNREGISTER".equals(c17packetcustompayload.a())) {
            try {
                if (c17packetcustompayload.b() != null && c17packetcustompayload.b().readableBytes() >= 1) {
                    packetbuffer = new PacketBuffer(Unpooled.wrappedBuffer(c17packetcustompayload.b()));
                    String channel = packetbuffer.c(packetbuffer.readableBytes());
                    Canary.channels().unregisterClient(channel, this.serverHandler);
                    Canary.log.info(String.format("Player '%s' unregistered Custom Payload on channel '%s'", this.b.getPlayer().getName(), channel));
                }
            }
            catch (Exception ex) {
                c.error("Error receiving 'C17PacketCustomPayload': " + ex.getMessage(), ex);
            }
        }
        else {
            try {
                if (c17packetcustompayload.b() != null && c17packetcustompayload.b().readableBytes() >= 1) {
                    Canary.channels().sendCustomPayloadToListeners(c17packetcustompayload.a(), c17packetcustompayload.b().a(), this.b.getPlayer());
                }
            }
            catch (Exception ex) {
                c.error("Error receiving 'C17PacketCustomPayload': " + ex.getMessage(), ex);
            }
        }
        // CanaryMod: End
    }

    static final class SwitchAction {

        static final int[] a;

        static final int[] b;

        static final int[] c = new int[C16PacketClientStatus.EnumState.values().length];

        static {
            try {
                c[C16PacketClientStatus.EnumState.PERFORM_RESPAWN.ordinal()] = 1;
            }
            catch (NoSuchFieldError nosuchfielderror156) {
                ;
            }

            try {
                c[C16PacketClientStatus.EnumState.REQUEST_STATS.ordinal()] = 2;
            }
            catch (NoSuchFieldError nosuchfielderror155) {
                ;
            }

            try {
                c[C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT.ordinal()] = 3;
            }
            catch (NoSuchFieldError nosuchfielderror154) {
                ;
            }

            b = new int[C0BPacketEntityAction.Action.values().length];

            try {
                b[C0BPacketEntityAction.Action.START_SNEAKING.ordinal()] = 1;
            }
            catch (NoSuchFieldError nosuchfielderror153) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.STOP_SNEAKING.ordinal()] = 2;
            }
            catch (NoSuchFieldError nosuchfielderror152) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.START_SPRINTING.ordinal()] = 3;
            }
            catch (NoSuchFieldError nosuchfielderror151) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.STOP_SPRINTING.ordinal()] = 4;
            }
            catch (NoSuchFieldError nosuchfielderror150) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.STOP_SLEEPING.ordinal()] = 5;
            }
            catch (NoSuchFieldError nosuchfielderror7) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.RIDING_JUMP.ordinal()] = 6;
            }
            catch (NoSuchFieldError nosuchfielderror8) {
                ;
            }

            try {
                b[C0BPacketEntityAction.Action.OPEN_INVENTORY.ordinal()] = 7;
            }
            catch (NoSuchFieldError nosuchfielderror9) {
                ;
            }

            a = new int[C07PacketPlayerDigging.Action.values().length];

            try {
                a[C07PacketPlayerDigging.Action.DROP_ITEM.ordinal()] = 1;
            }
            catch (NoSuchFieldError nosuchfielderror10) {
                ;
            }

            try {
                a[C07PacketPlayerDigging.Action.DROP_ALL_ITEMS.ordinal()] = 2;
            }
            catch (NoSuchFieldError nosuchfielderror11) {
                ;
            }

            try {
                a[C07PacketPlayerDigging.Action.RELEASE_USE_ITEM.ordinal()] = 3;
            }
            catch (NoSuchFieldError nosuchfielderror12) {
                ;
            }

            try {
                a[C07PacketPlayerDigging.Action.START_DESTROY_BLOCK.ordinal()] = 4;
            }
            catch (NoSuchFieldError nosuchfielderror13) {
                ;
            }

            try {
                a[C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK.ordinal()] = 5;
            }
            catch (NoSuchFieldError nosuchfielderror14) {
                ;
            }

            try {
                a[C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK.ordinal()] = 6;
            }
            catch (NoSuchFieldError nosuchfielderror15) {
                ;
            }
        }
    }

    /**
     * gets the CanaryNetServerHandler wrapper
     *
     * @return
     */
    public CanaryNetServerHandler getCanaryServerHandler() {
        return serverHandler;
    }
}
