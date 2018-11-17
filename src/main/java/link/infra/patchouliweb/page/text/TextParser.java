package link.infra.patchouliweb.page.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import link.infra.patchouliweb.PatchouliWeb;

public class TextParser {
	
	public static interface CommandTracker {
		public boolean trigger(String command);
		public String styleText(String span);
		public default Map<String, String> getTemplates() {
			return new HashMap<String, String>();
		}
	}
	
	public static abstract class GenericStyleCommandTracker implements CommandTracker {
		boolean currentState = false;
		final String commandName;
		
		public GenericStyleCommandTracker(String commandName) {
			this.commandName = commandName;
		}
		
		// trigger returns true when this command tracker has changed state
		public boolean trigger(String command) {
			if (!currentState && command.equals(commandName)) {
				currentState = true;
				return true;
			}
			if (currentState && command.equals("")) {
				currentState = false;
				return true;
			}
			return false;
		}
	}
	
	public static class BiStyleCommandTracker extends GenericStyleCommandTracker {
		final String startingTag;
		final String endingTag;
		public BiStyleCommandTracker(String commandName, String startingTag, String endingTag) {
			super(commandName);
			this.startingTag = startingTag;
			this.endingTag = endingTag;
		}
		
		@Override
		public String styleText(String span) {
			return startingTag + span + endingTag;
		}
	}
	
	public static class OneStyleCommandTracker extends BiStyleCommandTracker {
		public OneStyleCommandTracker(String commandName, String tag) {
			super(commandName, tag, tag);
		}
	}
	
	// TODO: api to add your own CommandTrackers
	public static List<CommandTracker> COMMANDS = new ArrayList<CommandTracker>();
	public List<CommandTracker> loadedCommands = new ArrayList<CommandTracker>();
	
	static {
		// TODO: shortcode
		// Obfuscate
		COMMANDS.add(new BiStyleCommandTracker("k", "{{% obfuscate %}}", "{{% /obfuscate %}}"));
		// Bold
		COMMANDS.add(new OneStyleCommandTracker("l", "**"));
		// Strike
		COMMANDS.add(new OneStyleCommandTracker("m", "~~"));
		// Underline
		COMMANDS.add(new OneStyleCommandTracker("n", "__"));
		// Italics
		COMMANDS.add(new OneStyleCommandTracker("o", "*"));
	}
	
	public static Map<String, String> MACROS = new HashMap<String, String>();
	public Map<String, String> loadedMacros = new HashMap<String, String>();
	
	static {
		MACROS.put("$(obf)", "$(k)");
		MACROS.put("$(bold)", "$(l)");
		MACROS.put("$(strike)", "$(m)");
		MACROS.put("$(italic)", "$(o)");
		MACROS.put("$(italics)", "$(o)");
		MACROS.put("$(list", "$(li");
		MACROS.put("$(reset)", "$()");
		MACROS.put("$(clear)", "$()");
		MACROS.put("$(2br)", "$(br2)");
		MACROS.put("$(p)", "$(br2)");
		MACROS.put("/$", "$()");
		MACROS.put("<br>", "$(br)");
		MACROS.put("$(nocolor)", "$(0)");
		MACROS.put("$(item)", "$(#b0b)");
		MACROS.put("$(thing)", "$(#490)");
	}
	
	public String processMacros(String text) {
		for (Map.Entry<String, String> entry : loadedMacros.entrySet()) {
			text = text.replace(entry.getKey(), entry.getValue());
		}
		return text;
	}
	
	public String processText(String text) {
		text = processMacros(text);
		
		// triggerStatus tracks the status of CommandTrackers, so that when they are turned off they are evaluated
		Map<CommandTracker, Boolean> triggerStatus = new HashMap<CommandTracker, Boolean>();
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '$' && text.length() > i+2 && text.charAt(i+1) == '(') {
				i += 2;
				StringBuilder currentCommand = new StringBuilder();
				while (text.charAt(i) != ')' && i < text.length()) {
					currentCommand.append(text.charAt(i));
					i++;
				}
				String currentCommandString = currentCommand.toString();
				for (CommandTracker tracker : loadedCommands) {
					if (tracker.trigger(currentCommandString)) {
						if (triggerStatus.get(tracker) != null && triggerStatus.get(tracker)) {
							// triggered -> untriggered
							PatchouliWeb.logger.info("hi, " + tracker.getClass().getSimpleName());
						} else {
							// untriggered -> triggered
							triggerStatus.put(tracker, true);
						}
					}
				}
			}
		}
		
		return text;
	}
	
	public TextParser(Map<String, String> macros) {
		this.loadedMacros.putAll(MACROS); // Put default macros in first
		this.loadedMacros.putAll(macros);
		this.loadedCommands.addAll(COMMANDS);
	}
	
}
