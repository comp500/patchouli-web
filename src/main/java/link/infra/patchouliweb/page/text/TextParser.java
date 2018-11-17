package link.infra.patchouliweb.page.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextParser {
	
	public static interface CommandTracker {
		// trigger returns true when this command tracker has changed state
		public boolean trigger(String command, StringBuilder sb);
		public default Map<String, String> getTemplates() {
			return new HashMap<String, String>();
		}
		public default void resetState() {}
	}
	
	public static class SimpleCommandTracker implements CommandTracker {
		final String commandName;
		final String tag;
		
		public SimpleCommandTracker(String commandName, String tag) {
			this.commandName = commandName;
			this.tag = tag;
		}
		
		public boolean trigger(String command, StringBuilder sb) {
			if (command.equals(commandName)) {
				sb.append(tag);
				return true;
			}
			return false;
		}
	}
	
	public static class BiStyleCommandTracker implements CommandTracker {
		boolean currentState = false;
		final String commandName;
		final String startingTag;
		final String endingTag;
		
		public BiStyleCommandTracker(String commandName, String startingTag, String endingTag) {
			this.commandName = commandName;
			this.startingTag = startingTag;
			this.endingTag = endingTag;
		}
		
		public boolean trigger(String command, StringBuilder sb) {
			if (!currentState && command.equals(commandName)) {
				currentState = true;
				sb.append(startingTag);
				return true;
			}
			if (currentState && command.equals("")) {
				currentState = false;
				sb.append(endingTag);
				return true;
			}
			return false;
		}
	}
	
	public static class SymmetricStyleCommandTracker extends BiStyleCommandTracker {
		public SymmetricStyleCommandTracker(String commandName, String tag) {
			super(commandName, tag, tag);
		}
	}
	
	public static class ListCommandTracker implements CommandTracker {
		boolean inList = false;
		
		@Override
		public boolean trigger(String command, StringBuilder sb) {
			if (command.equals("li")) {
				if (inList) {
					sb.append("\n- ");
					return true;
				} else {
					inList = true;
					sb.append("\n\n- ");
					return true;
				}
			} else {
				if (inList && command.equals("br2")) {
					inList = false;
					return true;
				}
			}
			return false;
		}
	}
	
	
	
	// TODO: api to add your own CommandTrackers
	public static List<CommandTracker> COMMANDS = new ArrayList<CommandTracker>();
	public List<CommandTracker> loadedCommands = new ArrayList<CommandTracker>();
	
	static {
		// Newline
		COMMANDS.add(new SimpleCommandTracker("br", "  \n"));
		// New paragraph
		COMMANDS.add(new SimpleCommandTracker("br2", "\n\n"));
		// List
		COMMANDS.add(new ListCommandTracker());
		// TODO: color
		// TODO: color code
		// TODO: shortcode for obfuscate
		// Obfuscate
		COMMANDS.add(new BiStyleCommandTracker("k", "{{% obfuscate %}}", "{{% /obfuscate %}}"));
		// Bold
		COMMANDS.add(new SymmetricStyleCommandTracker("l", "**"));
		// Strike
		COMMANDS.add(new SymmetricStyleCommandTracker("m", "~~"));
		// Underline
		COMMANDS.add(new SymmetricStyleCommandTracker("n", "__"));
		// Italics
		COMMANDS.add(new SymmetricStyleCommandTracker("o", "*"));
		// TODO: links
		// Player name (TODO: I18n, better alternative to "Player"?)
		COMMANDS.add(new SimpleCommandTracker("playername", "Player"));
		// TODO: keybinds
		// TODO: tooltips
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
		
		// Reset CommandTracker states
		for (CommandTracker tracker : loadedCommands) {
			tracker.resetState();
		}
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '$' && text.length() > i+2 && text.charAt(i+1) == '(') {
				i += 2;
				StringBuilder currentCommand = new StringBuilder();
				while (text.charAt(i) != ')' && i < text.length()) {
					currentCommand.append(text.charAt(i));
					i++;
				}
				String currentCommandString = currentCommand.toString();
				boolean hasTriggered = false;
				if (currentCommandString.length() == 0) {
					hasTriggered = true; // So that clearing twice won't add the $()
				}
				for (CommandTracker tracker : loadedCommands) {
					if (tracker.trigger(currentCommandString, sb)) {
						hasTriggered = true;
					}
				}
				
				if (!hasTriggered) {
					sb.append("$(");
					sb.append(currentCommandString);
					sb.append(')');
				}
			} else {
				sb.append(text.charAt(i));
			}
		}
		
		return sb.toString();
	}
	
	public TextParser(Map<String, String> macros) {
		this.loadedMacros.putAll(MACROS); // Put default macros in first
		this.loadedMacros.putAll(macros);
		this.loadedCommands.addAll(COMMANDS);
	}
	
}
