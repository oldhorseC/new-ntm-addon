package com.ntmit.command;

import com.ntmit.render.RbmkDebugRender;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.WrongUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class CommandNTMIT extends CommandBase {

	@Override
	public String getName() {
		return "ntmit";
	}

	@Override
	public String getUsage(ICommandSender sender) {
		return "/ntmit rbmkdbg <true|false>";
	}

	@Override
	public boolean checkPermission(MinecraftServer server, ICommandSender sender) {
		return true;
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
		if (args.length == 2 && "rbmkdbg".equalsIgnoreCase(args[0])) {
			boolean value;
			if ("true".equalsIgnoreCase(args[1])) {
				value = true;
			} else if ("false".equalsIgnoreCase(args[1])) {
				value = false;
			} else {
				throw new WrongUsageException(getUsage(sender));
			}
			RbmkDebugRender.enabled = value;
			sender.sendMessage(new TextComponentString("[ntm-it] rbmkdbg = " + value));
			return;
		}
		throw new WrongUsageException(getUsage(sender));
	}
}
