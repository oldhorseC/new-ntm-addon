package com.ntmit.command;

import com.ntmit.lib.NTMITWorldRules;
import com.ntmit.packet.PacketNTMITRbmkDebug;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

public class CommandNTMIT extends CommandBase {

	@Override
	public String getName() {
		return "ntmit";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/ntmit <rbmkdbg|enablemobilemode> <true|false>";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos targetPos) {
		if (args.length == 1) {
			return getListOfStringsMatchingLastWord(args, "rbmkdbg", "enablemobilemode");
		}
		if (args.length == 2 && ("rbmkdbg".equalsIgnoreCase(args[0]) || "enablemobilemode".equalsIgnoreCase(args[0]))) {
			return getListOfStringsMatchingLastWord(args, "true", "false");
		}
		return Collections.emptyList();
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 2) {
			boolean value;
			if ("true".equalsIgnoreCase(args[1])) {
				value = true;
			} else if ("false".equalsIgnoreCase(args[1])) {
				value = false;
			} else {
				throw new WrongUsageException(getUsage(sender));
			}
			World world = sender.getEntityWorld();
			if (world == null && server != null) world = server.getWorld(0);
			if (world == null) throw new CommandException("No world available");
			if ("rbmkdbg".equalsIgnoreCase(args[0])) {
				NTMITWorldRules.setRbmkDebug(world, value);
				PacketNTMITRbmkDebug.sendToAll(value);
				sender.sendMessage(new TextComponentString("[ntm-it] rbmkdbg = " + value));
				return;
			}
			if ("enablemobilemode".equalsIgnoreCase(args[0])) {
				NTMITWorldRules.setMobileMode(world, value);
				sender.sendMessage(new TextComponentString("[ntm-it] enablemobilemode = " + value));
				return;
			}
		}
		throw new WrongUsageException(getUsage(sender));
	}
}
