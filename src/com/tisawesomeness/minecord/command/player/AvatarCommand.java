package com.tisawesomeness.minecord.command.player;

import java.awt.Color;
import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;

import com.tisawesomeness.minecord.Config;
import com.tisawesomeness.minecord.command.Command;
import com.tisawesomeness.minecord.util.DateUtils;
import com.tisawesomeness.minecord.util.MessageUtils;
import com.tisawesomeness.minecord.util.NameUtils;
import com.tisawesomeness.minecord.util.RequestUtils;

import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class AvatarCommand extends Command {
	
	public CommandInfo getInfo() {
		return new CommandInfo(
			"avatar",
			"Gets the avatar of a player.",
			"<username|uuid> [date] [overlay?]",
			null,
			2000,
			false,
			false,
			true
		);
	}
	
	public Result run(String[] argsOrig, MessageReceivedEvent e) {
		
		//No arguments message
		if (argsOrig.length == 0) {
			String m = ":warning: Incorrect arguments." +
				"\n" + Config.getPrefix() + "avatar <username|uuid> [date]" +
				"\n" + MessageUtils.dateHelp;
			return new Result(Outcome.WARNING, m, 5);
		}
		String[] args = argsOrig;
		
		//Check for overlay argument
		boolean overlay = false;
		int index = MessageUtils.parseBoolean(args, "overlay");
		if (index != -1) {
			overlay = true;
			args = ArrayUtils.remove(args, index);
		}

		String player = args[0];	
		if (!player.matches(NameUtils.uuidRegex)) {
			String uuid = null;
			
			//Parse date argument
			if (args.length > 1) {
				long timestamp = DateUtils.getTimestamp(Arrays.copyOfRange(args, 1, args.length));
				if (timestamp == -1) {
					String m = ":x: Improperly formatted date. " +
						"At least a date or time is required. " +
						"Do `" + Config.getPrefix() + "avatar` for more info.";
					return new Result(Outcome.WARNING, m);
				}
				
			//Get the UUID
				uuid = NameUtils.getUUID(player, timestamp);
			} else {
				uuid = NameUtils.getUUID(player);
			}
			
			//Check for errors
			if (uuid == null) {
				String m = ":x: The Mojang API could not be reached." +
					"\n" + "Are you sure that username exists?" +
					"\n" + "Usernames are case-sensitive.";
				return new Result(Outcome.WARNING, m, 2);
			} else if (!uuid.matches(NameUtils.uuidRegex)) {
				String m = ":x: The API responded with an error:\n" + uuid;
				return new Result(Outcome.ERROR, m, 3);
			}
			
			player = uuid;
		}

		//Fetch avatar
		String url = "https://crafatar.com/avatars/" + player + ".png";
		if (overlay) {url = url + "?overlay";}
		url = RequestUtils.checkPngExtension(url);
		if (url == null) {
			MessageUtils.log("Error embedding image." +
				"\n" + "Command: `" + e.getMessage().getContent() + "`" +
				"\n" + "UUID: `" + player + "`"
			);
			return new Result(Outcome.ERROR, ":x: There was an error embedding the image.");
		}
		
		//PROPER APOSTROPHE GRAMMAR THANK THE LORD
		player = args[0];
		if (player.endsWith("s")) {
			player = player + "' Avatar";
		} else {
			player = player + "'s Avatar";
		}
		
		MessageEmbed me = MessageUtils.embedImage(player, url, Color.GREEN);
		
		return new Result(Outcome.SUCCESS, new EmbedBuilder(me).build());
	}
	
}
