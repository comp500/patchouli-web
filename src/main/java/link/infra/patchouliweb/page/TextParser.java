package link.infra.patchouliweb.page;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import link.infra.patchouliweb.PatchouliWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;

public class TextParser {
	
	public static interface CommandTracker {
		// addStartTag returns true when this command tracker has an end tag
		// If this function uses the command variable, you MUST implement readdStartTag to store the previous tag
		// isRepeated is true if the previous tag was a call to this addEndTag
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated);
		// Never called if addStartTag returns false.
		public void addEndTag(StringBuilder sb, ParserState ps);
		// readdStartTag replaces the same start tag that the last addStartTag placed
		// Override this if your tag needs to pass the original command.
		// Never called if addStartTag returns false.
		public default void readdStartTag(StringBuilder sb, ParserState ps) {
			addStartTag(null, sb, ps, false);
		}
		// matches returns true when this command tracker can process the given command
		public boolean matches(String command);
		// Does this command tracker need to be closed when the given command is reached?
		// Never called if addStartTag returns false.
		public default boolean breaksOn(String command) {
			return command.equals("");
		}
	}
	
	public static class SimpleCommandTracker implements CommandTracker {
		final String commandName;
		final String tag;
		
		public SimpleCommandTracker(String commandName, String tag) {
			this.commandName = commandName;
			this.tag = tag;
		}
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			sb.append(tag);
			return false; // Return false as there is no end tag
		}
		
		@Override // Do nothing
		public void addEndTag(StringBuilder sb, ParserState ps) {}

		@Override
		public boolean matches(String command) {
			return command.equals(commandName);
		}
	}
	
	public static class BiStyleCommandTracker implements CommandTracker {
		final String commandName;
		final String startingTag;
		final String endingTag;
		
		public BiStyleCommandTracker(String commandName, String startingTag, String endingTag) {
			this.commandName = commandName;
			this.startingTag = startingTag;
			this.endingTag = endingTag;
		}
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			sb.append(startingTag);
			return true;
		}
		
		@Override
		public void addEndTag(StringBuilder sb, ParserState ps) {
			sb.append(endingTag);
		}

		@Override
		public boolean matches(String command) {
			return command.equals(commandName);
		}
	}
	
	public static class SymmetricStyleCommandTracker extends BiStyleCommandTracker {
		public SymmetricStyleCommandTracker(String commandName, String tag) {
			super(commandName, tag, tag);
		}
	}
	
	public static class ListCommandTracker implements CommandTracker {
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			if (isRepeated) {
				sb.append("\n- "); // Only one \n if already in a list
			} else {
				sb.append("\n\n- ");
			}
			return true;
		}
		
		@Override
		public void addEndTag(StringBuilder sb, ParserState ps) {
			// Has no end tag, is closed by $(br2) which already adds necessary newlines
		}
		
		@Override
		public void readdStartTag(StringBuilder sb, ParserState ps) {
			// Must set isRepeated to true, as we are still in a list
			addStartTag(null, sb, ps, true);
		}

		@Override
		public boolean matches(String command) {
			return command.equals("li");
		}
		
		@Override
		public boolean breaksOn(String command) {
			return command.equals("br2"); // TODO: does it break on $() as well?
		}
	}
	
	public static class ColorStyleCommandTracker implements CommandTracker {
		String previousCommand = "";
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			if (command.toLowerCase().equals("#b0b")) {
				sb.append("{{% color/item %}}");
			} else if (command.equals("#490")) {
				sb.append("{{% color/thing %}}");
			} else if (command.equals("#000") || command.equals("#000000")) {
				// Do nothing, only break on $(0)
			} else {
				sb.append("{{% color \"" + command + "\" %}}");
			}
			previousCommand = command;
			return true;
		}
		
		@Override
		public void addEndTag(StringBuilder sb, ParserState ps) {
			if (previousCommand.toLowerCase().equals("#b0b")) {
				sb.append("{{% /color/item %}}");
			} else if (previousCommand.equals("#490")) {
				sb.append("{{% /color/thing %}}");
			} else if (previousCommand.equals("#000") || previousCommand.equals("#000000")) {
				// Do nothing, only break on $(0)
			} else {
				sb.append("{{% /color %}}");
			}
		}
		
		@Override
		public void readdStartTag(StringBuilder sb, ParserState ps) {
			addStartTag(previousCommand, sb, ps, false);
		}
		
		@Override
		public boolean matches(String command) {
			return command.matches("#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})");
		}
		
		@Override
		public boolean breaksOn(String command) {
			return command.equals("") || command.equals("#000") || command.equals("#000000");
		}
	}
	
	public static class KeybindCommandTracker implements CommandTracker {
		private KeyBinding getKeybind(String keybind) {
			KeyBinding[] keys = Minecraft.getMinecraft().gameSettings.keyBindings;
			for (KeyBinding k : keys) {
				String name = k.getKeyDescription();
				if (name.equals(keybind) || name.equals("key." + keybind)) {
					return k;
				}
			}
			return null;
		}
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState parser, boolean isRepeated) {
			KeyBinding k = getKeybind(command.substring(2));
			if (k != null) {
				// TODO: does this vary between translations?
				String translatedDescription = I18n.format(k.getKeyDescription());
				sb.append("{{< keybind \"" + k.getDisplayName() + "\" \"" + translatedDescription + "\" >}}");
			} else {
				sb.append("{{< keybind \"N/A\" \"Failed to obtain keybind\" >}}");
			}
			return false; // Return false as there is no end tag
		}
		
		@Override // Do nothing
		public void addEndTag(StringBuilder sb, ParserState ps) {}
		
		@Override
		public boolean matches(String command) {
			return command.startsWith("k:");
		}
	}
	
	public static class TooltipCommandTracker implements CommandTracker {
		String previousCommand = "";
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			sb.append("{{% tooltip \"" + command.substring(2) + "\" %}}");
			previousCommand = command;
			return true;
		}
		
		@Override
		public void addEndTag(StringBuilder sb, ParserState ps) {
			sb.append("{{% /tooltip %}}");
		}
		
		@Override
		public void readdStartTag(StringBuilder sb, ParserState ps) {
			addStartTag(previousCommand, sb, ps, false);
		}
		
		@Override
		public boolean matches(String command) {
			return command.startsWith("t:");
		}
		
		@Override
		public boolean breaksOn(String command) {
			return command.equals("") || command.equals("/t");
		}
	}
	
	public static class LinkCommandTracker implements CommandTracker {
		boolean currentState = false;
		String previousCommand = "";
		
		public String resolveURL(String url, String bookID) {
			if (url.startsWith("http")) { // External url
				return url;
			} else {
				ResourceLocation loc = new ResourceLocation(url);
				return "{{< relref \"/" + bookID + "/" + loc.getResourcePath() + ".md\" >}}";
			}
		}
		
		@Override
		public boolean addStartTag(String command, StringBuilder sb, ParserState ps, boolean isRepeated) {
			sb.append("[");
			previousCommand = command;
			return true;
		}
		
		@Override
		public void addEndTag(StringBuilder sb, ParserState ps) {
			sb.append("](");
			sb.append(resolveURL(previousCommand.substring(2), ps.bookID));
			sb.append(")");
		}
		
		@Override
		public void readdStartTag(StringBuilder sb, ParserState ps) {
			addStartTag(previousCommand, sb, ps, false);
		}
		
		@Override
		public boolean matches(String command) {
			return command.startsWith("l:");
		}
		
		@Override
		public boolean breaksOn(String command) {
			return command.equals("") || command.equals("/l");
		}
	}
	
	// TODO: api to add your own CommandTrackers
	public static final List<CommandTracker> COMMANDS = new ArrayList<CommandTracker>();
	
	static {
		// Newline
		COMMANDS.add(new SimpleCommandTracker("br", "  \n"));
		// New paragraph
		COMMANDS.add(new SimpleCommandTracker("br2", "\n\n"));
		COMMANDS.add(new ListCommandTracker());
		COMMANDS.add(new ColorStyleCommandTracker());
		// TODO: shortcode for obfuscate
		// Obfuscate
		COMMANDS.add(new BiStyleCommandTracker("k", "{{% obfuscate %}}", "{{% /obfuscate %}}"));
		// Bold
		COMMANDS.add(new SymmetricStyleCommandTracker("l", "__"));
		// Strike
		COMMANDS.add(new SymmetricStyleCommandTracker("m", "~~"));
		// Underline
		COMMANDS.add(new BiStyleCommandTracker("n", "<span style=\"text-decoration: underline\">", "</span>"));
		// Italics
		COMMANDS.add(new SymmetricStyleCommandTracker("o", "*"));
		COMMANDS.add(new LinkCommandTracker());
		// Player name (TODO: I18n, better alternative to "Player"?)
		COMMANDS.add(new SimpleCommandTracker("playername", "Player"));
		COMMANDS.add(new KeybindCommandTracker());
		COMMANDS.add(new TooltipCommandTracker());
	}
	
	public static final Map<String, String> MACROS = new HashMap<String, String>();
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
		// Not technically macros, but makes implementation of color codes easier
		MACROS.put("$(0)", "$(#000)");
		MACROS.put("$(1)", "$(#00a)");
		MACROS.put("$(2)", "$(#0a0)");
		MACROS.put("$(3)", "$(#0aa)");
		MACROS.put("$(4)", "$(#a00)");
		MACROS.put("$(5)", "$(#a0a)");
		MACROS.put("$(6)", "$(#fa0)");
		MACROS.put("$(7)", "$(#aaa)");
		MACROS.put("$(8)", "$(#555)");
		MACROS.put("$(9)", "$(#55f)");
		MACROS.put("$(a)", "$(#5f5)");
		MACROS.put("$(b)", "$(#5ff)");
		MACROS.put("$(c)", "$(#f55)");
		MACROS.put("$(d)", "$(#f5f)");
		MACROS.put("$(e)", "$(#ff5)");
		MACROS.put("$(f)", "$(#fff)");
	}
	
	public String processMacros(String text) {
		for (Map.Entry<String, String> entry : loadedMacros.entrySet()) {
			text = text.replace(entry.getKey(), entry.getValue());
		}
		return text;
	}
	
	public static class ParserState {
		public final String bookID;

		public ParserState(String bookID) {
			this.bookID = bookID;
		}
	}
	
	public String processText(String text) {
		text = processMacros(text);
		
		StringBuilder sb = new StringBuilder();
		ParserState ps = new ParserState(bookID);
		Deque<CommandTracker> trackedCommands = new ArrayDeque<CommandTracker>();
		
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) == '$' && text.length() > i+2 && text.charAt(i+1) == '(') {
				i += 2;
				StringBuilder currentCommand = new StringBuilder();
				while (text.charAt(i) != ')' && i < text.length()) {
					currentCommand.append(text.charAt(i));
					i++;
				}
				String currentCommandString = currentCommand.toString();
				
				/*
				 * This is the heart of the TextParser.
				 * It uses rudimentary computer science to move commands between stacks to open and close tags.
				 */
				Deque<CommandTracker> poppedCommands = new ArrayDeque<CommandTracker>();
				boolean wasRecognised = false; // True if the tag was parsed by the TextParser in some way
				/*
				 * FOR BREAKING TAGS (e.g. $(), $(/t) etc.)
				 * Note: a command can have both breaksOn and actual tag stuff
				 */
				// First check breaksOn for the command in the stack
				int stopIndex = -1;
				int j = 0;
				for (CommandTracker tracker : trackedCommands) {
					if (tracker.breaksOn(currentCommandString)) {
						stopIndex = j;
						wasRecognised = true;
					}
					j++;
				}
				j = 0;
				// Close tags up to the last broken tag
				while (!trackedCommands.isEmpty()) {
					if (j > stopIndex) {
						break;
					}
					CommandTracker tracker = trackedCommands.pop();
					tracker.addEndTag(sb, ps);
					if (!tracker.breaksOn(currentCommandString)) { // Add back only if it isn't broken by this command
						poppedCommands.push(tracker);
					}
					j++;
				}
				// Re-push tags back after closing all broken tags
				while (!poppedCommands.isEmpty()) {
					CommandTracker tracker = poppedCommands.pop();
					tracker.readdStartTag(sb, ps);
					trackedCommands.push(tracker);
				}
				/*
				 * FOR REPEATED TAGS (e.g. $(f)hi$(bold) there$(0) person$() where $(0) and $(f) are the same tag type)
				 */
				stopIndex = -1;
				j = 0;
				// Check the stack of previously issued commands to find the command
				for (CommandTracker tracker : trackedCommands) {
					if (tracker.matches(currentCommandString)) {
						wasRecognised = true;
						stopIndex = j;
						break;
					}
					j++;
				}
				j = 0;
				// Close tags up to the previous tag
				if (stopIndex > -1) {
					while (!trackedCommands.isEmpty()) {
						CommandTracker tracker = trackedCommands.pop();
						tracker.addEndTag(sb, ps);
						if (j < stopIndex) {
							poppedCommands.push(tracker);
						} else {
							// Add new start tag, don't push onto stack (which contains pending readds)
							tracker.addStartTag(currentCommandString, sb, ps, true);
							// Push onto tracked commands stack
							trackedCommands.push(tracker);
							break;
						}
						j++;
					}
				}
				// Re-push tags back after closing the repeated tag
				while (!poppedCommands.isEmpty()) {
					CommandTracker tracker = poppedCommands.pop();
					tracker.readdStartTag(sb, ps);
					trackedCommands.push(tracker);
				}
				/*
				 * FOR ALL NORMAL TAGS (e.g. $(bold))
				 */
				// Check the main list of commands to find a command
				if (stopIndex == -1) { // stopIndex == -1 means the command was not found in the stack
					for (CommandTracker tracker : COMMANDS) {
						if (tracker.matches(currentCommandString)) {
							if (tracker.addStartTag(currentCommandString, sb, ps, false)) {
								// If it returns true, tracker has an end tag and is added to trackedCommands
								trackedCommands.push(tracker);
							}
							wasRecognised = true;
							break;
						}
					}
				}
				
				// If it was not recognised, print a warning
				// Also fires when breaksOn was not called, e.g. for $(bold)no u$()$()
				if (!wasRecognised) {
					if (currentCommandString.length() != 0) { // Ignore $()
						PatchouliWeb.logger.warn("Command $(" + currentCommandString + ") not recognised!");
					}
				}
			} else {
				sb.append(text.charAt(i));
			}
		}
		
		return sb.toString();
	}
	
	final String bookID;
	
	public TextParser(Map<String, String> macros, String bookID) {
		this.loadedMacros.putAll(MACROS); // Put default macros in first
		this.loadedMacros.putAll(macros);
		this.bookID = bookID;
	}
	
}
