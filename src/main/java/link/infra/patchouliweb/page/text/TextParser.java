package link.infra.patchouliweb.page.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TextParser {
	
	public static interface CommandTracker {
		// trigger returns true when this command tracker has changed state
		public boolean trigger(String command);
		// Does the command have state, does it have an end/start tag?
		public default boolean biTriggerable() {
			return true;
		}
		public String prependText();
		public String appendText();
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
		
		public boolean trigger(String command) {
			if (command.equals(commandName)) {
				return true;
			}
			return false;
		}
		
		@Override
		public boolean biTriggerable() {
			return false;
		}
		
		public String prependText() {
			return "";
		}
		public String appendText() {
			return tag;
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
		
		public String prependText() {
			return startingTag;
		}
		public String appendText() {
			return endingTag;
		}
	}
	
	public static class SymmetricStyleCommandTracker extends BiStyleCommandTracker {
		public SymmetricStyleCommandTracker(String commandName, String tag) {
			super(commandName, tag, tag);
		}
	}
	
	public static class ListCommandTracker implements CommandTracker {
		boolean isFirst = true;
		boolean currentState = false;
		
		public void resetState() {
			isFirst = true;
		}
		
		@Override
		public boolean trigger(String command) {
			if (!currentState && command.equals("li")) {
				currentState = true;
				return true;
			}
			if (currentState) { // TODO: allow single line formatting in lists
				currentState = false;
				return true;
			}
			return false;
		}
		
		public String prependText() {
			if (isFirst) {
				return "\n\n- ";
			} else {
				return "- ";
			}
		}
		public String appendText() {
			return "\n";
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
		
		// triggerStatus tracks the status of CommandTrackers, so that when they are turned off they are evaluated
		Map<CommandTracker, Boolean> triggerStatus = new HashMap<CommandTracker, Boolean>();
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
					if (tracker.trigger(currentCommandString)) {
						hasTriggered = true;
						if (tracker.biTriggerable()) {
							if (triggerStatus.get(tracker) != null && triggerStatus.get(tracker)) {
								// triggered -> untriggered
								sb.append(tracker.appendText());
							} else {
								// untriggered -> triggered
								triggerStatus.put(tracker, true);
								sb.append(tracker.prependText());
							}
						} else {
							sb.append(tracker.appendText());
						}
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
		
		return text + "\n" + sb.toString();
	}
	
	public TextParser(Map<String, String> macros) {
		this.loadedMacros.putAll(MACROS); // Put default macros in first
		this.loadedMacros.putAll(macros);
		this.loadedCommands.addAll(COMMANDS);
	}
	
}
