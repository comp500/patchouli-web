package link.infra.patchouliweb.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.ResourceLocation;

public class TextParser {
	
	public static interface CommandTracker {
		// trigger returns true when this command tracker has changed state
		public boolean trigger(String command, StringBuilder sb, TextParser parser);
		// TODO: move to seperate files, rather than a set of functions?
		public default Map<String, String> getTemplates() {
			return new HashMap<String, String>();
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
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
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
		
		@Override
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
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
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
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
	
	public static class ColorStyleCommandTracker implements CommandTracker {
		boolean currentState = false;
		String previousTag = "";
		
		private boolean matches(String command) {
			return command.matches("#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})");
		}
		
		private void addOpenTag(String command, StringBuilder sb) {
			if (command.toLowerCase().equals("#b0b")) {
				sb.append("{{% item %}}");
			} else if (command.equals("#490")) {
				sb.append("{{% thing %}}");
			} else {
				sb.append("{{% color \"" + command + "\" %}}");
			}
			previousTag = command;
		}
		
		private void addCloseTag(StringBuilder sb) {
			if (previousTag.toLowerCase().equals("#b0b")) {
				sb.append("{{% /item %}}");
			} else if (previousTag.equals("#490")) {
				sb.append("{{% /thing %}}");
			} else {
				sb.append("{{% /color %}}");
			}
		}
		
		@Override
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
			if (!currentState && matches(command)) {
				currentState = true;
				addOpenTag(command, sb);
				return true;
			}
			if (currentState && (command.equals("") || command.equals("#000"))) {
				currentState = false;
				addCloseTag(sb);
				return true;
			}
			if (currentState && matches(command) && !command.equals(previousTag)) {
				addCloseTag(sb);
				addOpenTag(command, sb);
				return true;
			}
			return false;
		}
		
		@Override
		public Map<String, String> getTemplates() {
			Map<String, String> templateMap = new HashMap<String, String>();
			templateMap.put("item", "<span class=\"text-item\">{{ .Inner }}</span>");
			templateMap.put("thing", "<span class=\"text-thing\">{{ .Inner }}</span>");
			templateMap.put("color", "<span style=\"color: {{ .Get 0 }}\">{{ .Inner }}</span>");
			return templateMap;
		}
	}
	
	public static class ColorCodeStyleCommandTracker extends ColorStyleCommandTracker {
		@Override
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
			Map<String, String> codeMap = new HashMap<String, String>();
			
			return super.trigger(codeMap.getOrDefault(command, command), sb, parser);
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
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
			if (command.startsWith("k:")) {
				KeyBinding k = getKeybind(command.substring(2));
				if (k != null) {
					// TODO: does this vary between translations?
					String translatedDescription = I18n.format(k.getKeyDescription());
					sb.append("{{< keybind \"" + k.getDisplayName() + "\" \"" + translatedDescription + "\" >}}");
				} else {
					sb.append("{{< keybind \"N/A\" \"Failed to obtain keybind\" >}}");
				}
				return true;
			}
			return false;
		}
		
		@Override
		public Map<String, String> getTemplates() {
			Map<String, String> templateMap = new HashMap<String, String>();
			templateMap.put("keybind", "<code title=\"{{ .Get 1 }}\">{{ .Get 0 }}</code>");
			return templateMap;
		}
	}
	
	public static class TooltipCommandTracker implements CommandTracker {
		boolean currentState = false;
		
		@Override
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
			if (!currentState && command.startsWith("t:")) {
				currentState = true;
				sb.append("{{% tooltip \"" + command.substring(2) + "\" %}}");
				return true;
			}
			if (currentState && (command.equals("") || command.equals("\t"))) {
				currentState = false;
				sb.append("{{% /tooltip %}}");
				return true;
			}
			return false;
		}
		
		@Override
		public Map<String, String> getTemplates() {
			Map<String, String> templateMap = new HashMap<String, String>();
			templateMap.put("tooltip", "<span title=\"{{ .Get 0 }}\">{{ .Inner }}</span>");
			return templateMap;
		}
	}
	
	public static class LinkCommandTracker implements CommandTracker {
		boolean currentState = false;
		String previousURL = "";
		
		public String resolveURL(String url, String bookID) {
			if (url.startsWith("http")) { // External url
				return url;
			} else {
				ResourceLocation loc = new ResourceLocation(url);
				return "{{< relref \"/page/" + bookID + "/" + loc.getResourcePath() + ".md\" >}}";
			}
		}
		
		@Override
		public boolean trigger(String command, StringBuilder sb, TextParser parser) {
			if (!currentState && command.startsWith("l:")) {
				currentState = true;
				sb.append("[");
				previousURL = resolveURL(command.substring(2), parser.bookID);
				return true;
			}
			if (currentState && (command.equals("") || command.equals("/l"))) {
				currentState = false;
				sb.append("](");
				sb.append(previousURL);
				sb.append(")");
				return true;
			}
			return false;
		}
		
		@Override
		public Map<String, String> getTemplates() {
			Map<String, String> templateMap = new HashMap<String, String>();
			templateMap.put("tooltip", "<span title=\"{{ .Get 0 }}\">{{ .Inner }}</span>");
			return templateMap;
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
		COMMANDS.add(new ListCommandTracker());
		COMMANDS.add(new ColorStyleCommandTracker());
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
		COMMANDS.add(new LinkCommandTracker());
		// Player name (TODO: I18n, better alternative to "Player"?)
		COMMANDS.add(new SimpleCommandTracker("playername", "Player"));
		COMMANDS.add(new KeybindCommandTracker());
		COMMANDS.add(new TooltipCommandTracker());
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
	
	public String processText(String text) {
		text = processMacros(text);
		
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
					if (tracker.trigger(currentCommandString, sb, this)) {
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
	
	public Map<String, String> getAllTemplates() {
		Map<String, String> allOfThem = new HashMap<String, String>();
		for (CommandTracker tracker : loadedCommands) {
			tracker.getTemplates().putAll(allOfThem);
		}
		return allOfThem;
	}
	
	final String bookID;
	
	public TextParser(Map<String, String> macros, String bookID) {
		this.loadedMacros.putAll(MACROS); // Put default macros in first
		this.loadedMacros.putAll(macros);
		this.loadedCommands.addAll(COMMANDS);
		this.bookID = bookID;
	}
	
}
