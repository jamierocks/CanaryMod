package net.minecraft.command;

import net.canarymod.api.world.CanaryWorld;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldServer;

import java.util.List;

public class CommandDifficulty extends CommandBase {

    public String c() {
        return "difficulty";
    }

    public int a() {
        return 2;
    }

    public String c(ICommandSender icommandsender) {
        return "commands.difficulty.usage";
    }

    public void a(ICommandSender icommandsender, String[] astring) throws CommandException {
        if (astring.length <= 0) {
            throw new WrongUsageException("commands.difficulty.usage", new Object[0]);
        }
        else {
            EnumDifficulty enumdifficulty = this.e(astring[0]);

            WorldServer worldserver = (WorldServer)icommandsender.e();
            if (astring.length > 1) { // Add an argument to allow picking a world
                boolean loaded = MinecraftServer.M().worldManager.worldIsLoaded(astring[1]);
                if (!loaded) {
                    a(icommandsender, this, "No world loaded of Name: '%s'", new Object[]{ astring[1] });
                    return;
                }
                worldserver = (WorldServer)((CanaryWorld)MinecraftServer.M().worldManager.getWorld(astring[1], false)).getHandle();
            }
            MinecraftServer.M().a(enumdifficulty, worldserver);
            a(icommandsender, this, "commands.difficulty.success", new Object[]{ new ChatComponentTranslation(enumdifficulty.b(), new Object[0]) });
        }
    }

    protected EnumDifficulty e(String s0) throws CommandException {
        return !s0.equalsIgnoreCase("peaceful") && !s0.equalsIgnoreCase("p") ? (!s0.equalsIgnoreCase("easy") && !s0.equalsIgnoreCase("e") ? (!s0.equalsIgnoreCase("normal") && !s0.equalsIgnoreCase("n") ? (!s0.equalsIgnoreCase("hard") && !s0.equalsIgnoreCase("h") ? EnumDifficulty.a(a(s0, 0, 3)) : EnumDifficulty.HARD) : EnumDifficulty.NORMAL) : EnumDifficulty.EASY) : EnumDifficulty.PEACEFUL;
    }

    public List a(ICommandSender icommandsender, String[] astring, BlockPos blockpos) {
        return astring.length == 1 ? a(astring, new String[]{ "peaceful", "easy", "normal", "hard" }) : null;
    }
}
