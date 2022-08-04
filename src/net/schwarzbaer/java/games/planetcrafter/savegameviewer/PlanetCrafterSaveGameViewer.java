package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Vector;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import net.schwarzbaer.gui.FileChooser;
import net.schwarzbaer.gui.ProgressDialog;
import net.schwarzbaer.gui.StandardDialog;
import net.schwarzbaer.gui.StandardMainWindow;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.NV;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.TerraformingStates;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.V;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;
import net.schwarzbaer.java.lib.jsonparser.JSON_Helper;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser;
import net.schwarzbaer.java.lib.jsonparser.JSON_Parser.ParseException;
import net.schwarzbaer.system.DateTimeFormatter;
import net.schwarzbaer.system.Settings;

public class PlanetCrafterSaveGameViewer {

	private static final String FILE_OBJECT_TYPES = "PlanetCrafterSaveGameViewer - ObjectTypes.data";
	private static final String FILE_ACHIEVEMENTS = "PlanetCrafterSaveGameViewer - Achievements.data";
	static final Color COLOR_Removal_ByData = new Color(0xFFD5D5);
	static final Color COLOR_Removal_ByUser = new Color(0xFF7F7F);

	public static void main(String[] args) {
		//String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		//scanFile(pathname);
		//GeneralDataPanel.TerraformingStatesPanel.testDurationFormater();
		
		try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
		catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {}
		
		new PlanetCrafterSaveGameViewer().initialize();
	}
	
	static final AppSettings settings = new AppSettings();
	static final DateTimeFormatter dtFormatter = new DateTimeFormatter();

	private final StandardMainWindow mainWindow;
	private final FileChooser jsonFileChooser;
	private final JTabbedPane dataTabPane;
	private final MyMenuBar menuBar;
	private File openFile;
	private Data loadedData;
	private HashMap<String,ObjectType> objectTypes;
	private Achievements achievements;
	private GeneralDataPanel generalDataPanel;

	PlanetCrafterSaveGameViewer() {
		openFile = null;
		loadedData = null;
		objectTypes = null;
		achievements = null;
		generalDataPanel = null;
		jsonFileChooser = new FileChooser("JSON File", "json");
		
		mainWindow = new StandardMainWindow("Planet Crafter - SaveGame Viewer");
		dataTabPane = new JTabbedPane();
		mainWindow.startGUI(dataTabPane, menuBar = new MyMenuBar());
		mainWindow.setIconImagesFromResource("/icons/icon_", "24.png", "32.png", "48.png", "64.png", "96.png");
		
		settings.registerAppWindow(mainWindow);
		updateWindowTitle();
		
	}
	
	private class MyMenuBar extends JMenuBar {
		private static final long serialVersionUID = 940262053656728621L;
		
		private final JMenuItem miReloadSaveGame;
		private final JMenuItem miWriteReducedSaveGame;

		MyMenuBar() {
			JMenu filesMenu = add(new JMenu("Files"));
			
			miReloadSaveGame = filesMenu.add(createMenuItem("Reload SaveGame", openFile!=null, e->{
				readFile(openFile);
			}));
			
			filesMenu.add(createMenuItem("Open SaveGame", e->{
				if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
					readFile(jsonFileChooser.getSelectedFile());
			}));
			
			miWriteReducedSaveGame = filesMenu.add(createMenuItem("Write Reduced SaveGame", e->{
				if (openFile!=null)
					jsonFileChooser.setSelectedFile(openFile);
				if (loadedData!=null && jsonFileChooser.showSaveDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
					writeReducedFile(jsonFileChooser.getSelectedFile(), loadedData);
			}));
			
			filesMenu.addSeparator();
			filesMenu.add(createMenuItem("Quit", e->{
				System.exit(0);
			}));
			
			JMenu achievementsMenu = add(new JMenu("Achievements"));
			
			achievementsMenu.add(createMenuItem("Configure Achievements", e->{
				new Achievements.ConfigDialog(mainWindow,achievements).showDialog(StandardDialog.Position.PARENT_CENTER);
				achievements.writeToFile(new File(FILE_ACHIEVEMENTS));
				if (generalDataPanel!=null)
					generalDataPanel.updateAfterAchievementsChange();
			}));
		}

		void notifyFileWasLoaded() {
			miReloadSaveGame.setEnabled(true);
			miWriteReducedSaveGame.setEnabled(true);
		}
	}

	static JButton createButton(String title, boolean isEnabled, ActionListener al) {
		JButton comp = new JButton(title);
		if (al!=null) comp.addActionListener(al);
		comp.setEnabled(isEnabled);
		return comp;
	}

	static JMenuItem createMenuItem(String title, ActionListener al) {
		return createMenuItem(title, true, al);
	}

	static JMenuItem createMenuItem(String title, boolean isEnabled, ActionListener al) {
		JMenuItem comp = new JMenuItem(title);
		if (al!=null) comp.addActionListener(al);
		comp.setEnabled(isEnabled);
		return comp;
	}

	static JTextField createOutputTextField(String text) {
		JTextField comp = new JTextField(text);
		comp.setEditable(false);
		return comp;
	}

	static JTextField createOutputTextField(String text, int horizontalAlignment) {
		JTextField comp = createOutputTextField(text);
		comp.setHorizontalAlignment(horizontalAlignment);
		return comp;
	}

	private void updateWindowTitle() {
		mainWindow.setTitle(
			openFile == null
			?               "Planet Crafter - SaveGame Viewer"
			: String.format("Planet Crafter - SaveGame Viewer - \"%s\" [%s]", openFile.getName(), dtFormatter.getTimeStr(openFile.lastModified(), false, true, false, true, false))
		);
	}

	private void initialize() {
		jsonFileChooser.setCurrentDirectory(guessDirectory());
		
		objectTypes = ObjectType.readFromFile(new File(FILE_OBJECT_TYPES));
		achievements = Achievements.readFromFile(new File(FILE_ACHIEVEMENTS));
		
		// String pathname = "c:\\Users\\Hendrik 2\\AppData\\LocalLow\\MijuGames\\Planet Crafter\\Survival-1.json";
		File file = settings.getFile(AppSettings.ValueKey.OpenFile, null);
		if (file==null || !file.isFile()) {
			file = null;
			if (jsonFileChooser.showOpenDialog(mainWindow)==JFileChooser.APPROVE_OPTION)
				file = jsonFileChooser.getSelectedFile();
		}
		
		readFile(file);
	}

	private File guessDirectory() {
		File currentDir = null;
		
		// c:\Users\Hendrik 2\AppData\LocalLow\MijuGames\Planet Crafter\
		String user_home = System.getProperty("user.home"); // "C:\Users\Hendrik 2"
		if (user_home!=null) {
			currentDir = new File(user_home, "AppData\\LocalLow\\MijuGames\\Planet Crafter");
			if (!currentDir.isDirectory())
				currentDir = null;
		}
		if (currentDir==null || !currentDir.isDirectory())
			currentDir = new File("./");
		
		return currentDir;
	}

	private void readFile(File file) {
		if (file==null) return;
		if (!file.isFile()) return;
		
		String title = String.format("Read File \"%s\" [%s]", file.getName(), file.getParent());
		ProgressDialog.runWithProgressDialog(mainWindow, title, 400, pd->{
			
			Vector<Vector<JSON_Data.Value<NV,V>>> jsonStructure = readContent(pd, file);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Reading Aborted"); return; }
			if (jsonStructure==null) return;
			
			showIndeterminateTask(pd, "Parse JSON Structure");
			HashSet<String> newObjectTypes = new HashSet<>();
			Function<String,ObjectType> getOrCreateObjectType =
					objectTypeID -> ObjectType.getOrCreate(objectTypes, objectTypeID, newObjectTypes);
			Data data = Data.parse(jsonStructure, getOrCreateObjectType);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Reading Aborted"); return; }
			if (data == null) return;
			
			showIndeterminateTask(pd, "Write new ObjectTypes to File");
			writeObjectTypesToFile();
			
			if (!newObjectTypes.isEmpty()) {
				Vector<String> vec = new Vector<>(newObjectTypes);
				vec.sort(Data.caseIgnoringComparator);
				vec.insertElementAt("Some new Object Types found:", 0);
				String[] message = vec.toArray(String[]::new);
				JOptionPane.showMessageDialog(mainWindow, message, "New ObjectTypes", JOptionPane.INFORMATION_MESSAGE);
			}
			
			SwingUtilities.invokeLater(()->{
				pd.setTaskTitle("Update GUI");
				pd.setIndeterminate(true);
				
				settings.putFile(AppSettings.ValueKey.OpenFile, file);
				loadedData = data;
				openFile = file;
				menuBar.notifyFileWasLoaded();
				setGUI(data);
				updateWindowTitle();
			});
		});
		
	}

	private void writeReducedFile(File file, Data data) {
		if (file==null) return;
		
		Data.TerraformingStates modifiedTerraformingStates;
		String msg = "Do you want to change Terraforming States?";
		String dlgTitle = "Modified Terraforming States";
		int result = JOptionPane.showConfirmDialog(mainWindow, msg, dlgTitle, JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
		if (JOptionPane.YES_OPTION == result) {
			modifiedTerraformingStates = TerraformingStatesDialog.show(mainWindow, "Modify Terraforming States", data.terraformingStates);
			if (modifiedTerraformingStates==null)
				return;
		} else if (JOptionPane.NO_OPTION == result)
			modifiedTerraformingStates = null;
		else
			return;
		
		String title = String.format("Write Reduced File \"%s\" [%s]", file.getName(), file.getParent());
		ProgressDialog.runWithProgressDialog(mainWindow, title, 400, pd->{
			
			showIndeterminateTask(pd, "Create JSON Code");
			Vector<Vector<String>> jsonStrs = data.toJsonStrs(modifiedTerraformingStates);
			if (Thread.currentThread().isInterrupted()) { System.out.println("File Writing Aborted"); return; }
			
			writeContent(pd, file, jsonStrs);
		
		});
	}

	private void writeObjectTypesToFile() {
		ObjectType.writeToFile(new File(FILE_OBJECT_TYPES), objectTypes);
	}

	private static void showIndeterminateTask(ProgressDialog pd, String taskTitle) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setIndeterminate(true);
		});
	}

	private static void showTask(ProgressDialog pd, String taskTitle, int max) {
		SwingUtilities.invokeLater(()->{
			pd.setTaskTitle(taskTitle);
			pd.setValue(0, max);
		});
	}

	private static void setTaskValue(ProgressDialog pd, int value) {
		SwingUtilities.invokeLater(()->{
			pd.setValue(value);
		});
	}

	private void setGUI(Data data) {
		dataTabPane.removeAll();
		generalDataPanel = new GeneralDataPanel(data, achievements);
		TerraformingPanel terraformingPanel = new TerraformingPanel(data, generalDataPanel);
		MapPanel mapPanel = new MapPanel(data);

		ObjectTypesPanel objectTypesPanel = new ObjectTypesPanel(objectTypes);
		objectTypesPanel.addObjectTypesChangeListener(e -> writeObjectTypesToFile());
		objectTypesPanel.addObjectTypesChangeListener(mapPanel);
		objectTypesPanel.addObjectTypesChangeListener(terraformingPanel);
		objectTypesPanel.addObjectTypesChangeListener(generalDataPanel);
		
		dataTabPane.addTab("General", generalDataPanel);
		dataTabPane.addTab("Map", mapPanel);
		dataTabPane.addTab("Terraforming", terraformingPanel);
		dataTabPane.addTab("World Objects", new WorldObjectsPanel(data,mapPanel));
		dataTabPane.addTab("Object Lists", new ObjectListsPanel(data,mapPanel));
		dataTabPane.addTab("Object Types", objectTypesPanel);
		
		SwingUtilities.invokeLater(() -> {
			mapPanel.initialize();
		});
	}

	private static Vector<Vector<Value<NV, V>>> readContent(ProgressDialog pd, File file) {
		showIndeterminateTask(pd, "Read Content");
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return null;
		}
		if (Thread.currentThread().isInterrupted()) return null;
		String content = new String(bytes);
		
		//showIndeterminateTask(pd, "Scan JSON Structure");
		//scanFileContent(content);
		
		showIndeterminateTask(pd, "Create JSON Structure");
		Vector<Vector<Value<NV, V>>> fileData = new Vector<>();
		Vector<Value<NV, V>> blockData = new Vector<>();
		
		new IterativeJsonParser().parse(content, (val,ch) -> {
			blockData.add(val);
			if (ch.equals('@')) {
				System.out.printf("Block[%d]: %d entries%n", fileData.size(), blockData.size());
				fileData.add(new Vector<>(blockData));
				blockData.clear();
			}
		}, '@','|');
		
		if (!blockData.isEmpty()) {
			System.out.printf("Block[%d]: %d entries%n", fileData.size(), blockData.size());
			fileData.add(new Vector<>(blockData));
			blockData.clear();
		}
		if (Thread.currentThread().isInterrupted()) return null;
		
		return fileData;
	}

	private static void writeContent(ProgressDialog pd, File file, Vector<Vector<String>> jsonStrs) {
		if (Thread.currentThread().isInterrupted()) return;
		
		int lineAmount = 0;
		for (Vector<String> block : jsonStrs)
			lineAmount += block.size();
		
		showTask(pd, "Write JSON code to file", lineAmount);
		
		try (PrintWriter out = new PrintWriter(file, StandardCharsets.UTF_8)) {
			
			int lineCounter = 0;
			for (Vector<String> block : jsonStrs) {
				out.print("\r");
				boolean isFirst = true;
				for (String line : block) {
					if (Thread.currentThread().isInterrupted()) return;
					if (!isFirst) out.print("|\n");
					isFirst = false;
					out.print(line);
					setTaskValue(pd, ++lineCounter);
				}
				out.print("\r@");
			}
			
		}
		catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	@SuppressWarnings("unused")
	private static void scanFile(String pathname) {
		File file = new File(pathname);
		if (!file.isFile()) return;
		
		byte[] bytes;
		try { bytes = Files.readAllBytes(file.toPath()); }
		catch (IOException ex) {
			System.err.printf("IOException while reading file \"%s\".", ex.getMessage());
			//ex.printStackTrace();
			return;
		}
		
		String content = new String(bytes);
		scanFileContent(content);
	}

	private static void scanFileContent(String content) {
		JSON_Helper.OptionalValues<NV, V> optionalValues = new JSON_Helper.OptionalValues<NV,V>();
		ValueContainer<Integer> blockIndex = new ValueContainer<>(0);
		ValueContainer<Integer> entriesCount = new ValueContainer<>(0);
		
		new IterativeJsonParser().parse(content, (val,ch) -> {
			entriesCount.value++;
			optionalValues.scan(val,"ParseResult");
			if (ch==null || ch.equals('@')) {
				System.out.printf("Block[%d]: %d entries%n", blockIndex.value, entriesCount.value);
				optionalValues.show("-> Format", System.out);
				optionalValues.clear();
				blockIndex.value++;
				entriesCount.value = 0;
			}
		}, '@','|');
	}

	private static class TerraformingStatesDialog extends StandardDialog {
		private static final long serialVersionUID = -580668583006732866L;
		
		private final JButton btnOk;
		private final DoubleTextField oxygenLevel  ;
		private final DoubleTextField heatLevel    ;
		private final DoubleTextField pressureLevel;
		private final DoubleTextField plantsLevel  ;
		private final DoubleTextField insectsLevel ;
		private final DoubleTextField animalsLevel ;
		private Data.TerraformingStates results;
		
		private TerraformingStatesDialog(Window parent, String title, Vector<Data.TerraformingStates> terraformingStates) {
			super(parent, title);
			results = null;
			
			oxygenLevel   = new DoubleTextField(0.0);
			heatLevel     = new DoubleTextField(0.0);
			pressureLevel = new DoubleTextField(0.0);
			plantsLevel   = new DoubleTextField(0.0);
			insectsLevel  = new DoubleTextField(0.0);
			animalsLevel  = new DoubleTextField(0.0);
			
			if (!terraformingStates.isEmpty()) {
				Data.TerraformingStates values = terraformingStates.firstElement();
				oxygenLevel  .setValue(values.oxygenLevel  );
				heatLevel    .setValue(values.heatLevel    );
				pressureLevel.setValue(values.pressureLevel);
				plantsLevel  .setValue(values.plantsLevel  );
				insectsLevel .setValue(values.insectsLevel );
				animalsLevel .setValue(values.animalsLevel );
			}
			
			JPanel contentPane = new JPanel(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			c.gridwidth = 1;
			
			int gridy = 0;
			addRow(contentPane, c, gridy++, "Oxygen Level"  , oxygenLevel  );
			addRow(contentPane, c, gridy++, "Heat Level"    , heatLevel    );
			addRow(contentPane, c, gridy++, "Pressure Level", pressureLevel);
			addRow(contentPane, c, gridy++, "Plants Level"  , plantsLevel  );
			addRow(contentPane, c, gridy++, "Insects Level" , insectsLevel );
			addRow(contentPane, c, gridy++, "Animals Level" , animalsLevel );
			
			c.weightx = 1;
			c.weighty = 1;
			c.gridx = 0;
			c.gridy = gridy;
			c.gridwidth = 2;
			contentPane.add(new JLabel(), c);
			
			createGUI(contentPane,
					btnOk = createButton("Ok", true, e->{
						createResult();
						if (results!=null) closeDialog();
					}),
					createButton("Cancel", true, e->{
						closeDialog();
					}));
			
			updateGUI();
		}

		private static void addRow(JPanel panel, GridBagConstraints c, int gridy, String label, DoubleTextField txtField) {
			c.gridy = gridy;
			c.weightx = 0; c.gridx = 0; panel.add(new JLabel(label+": "), c);
			c.weightx = 1; c.gridx = 1; panel.add(txtField, c);
		}

		private boolean areAllValuesOk() {
			return  oxygenLevel  .isOK() &&
					heatLevel    .isOK() &&
					pressureLevel.isOK() &&
					plantsLevel  .isOK() &&
					insectsLevel .isOK() &&
					animalsLevel .isOK();
		}

		private void createResult() {
			results = !areAllValuesOk() ? null : new Data.TerraformingStates(
					oxygenLevel  .value,
					heatLevel    .value,
					pressureLevel.value,
					plantsLevel  .value,
					insectsLevel .value,
					animalsLevel .value
					);
		}
		
		private void updateGUI() {
			btnOk.setEnabled(areAllValuesOk());
		}

		private class DoubleTextField extends JTextField {
			private static final long serialVersionUID = 7631623492689405688L;
			
			private double value;
			private boolean isOK;

			DoubleTextField(double value) {
				super(20);
				setValue(value);
				Color defaultBG = getBackground();
				isOK = true;
				
				Runnable setValue = ()->{
					isOK = false;
					try {
						double newValue = Double.parseDouble(getText());
						if (!Double.isNaN(newValue)) { this.value = newValue; isOK = true; }
					} catch (NumberFormatException e1) { }
					setBackground(isOK ? defaultBG : Color.RED);
					updateGUI();
				};
				
				addActionListener(e->{setValue.run();});
				addFocusListener(new FocusListener() {
					@Override public void focusGained(FocusEvent e) {}
					@Override public void focusLost(FocusEvent e) { setValue.run(); }
				});
			}

			boolean isOK() {
				return isOK;
			}

			void setValue(double value) {
				this.value = value;
				setText(String.format(Locale.ENGLISH, "%s", this.value));
			}
			
		}

		static TerraformingStates show(Window parent, String title, Vector<TerraformingStates> terraformingStates) {
			TerraformingStatesDialog dlg = new TerraformingStatesDialog(parent, title, terraformingStates);
			dlg.showDialog();
			return dlg.results;
		}
	}

	private static class ValueContainer<Val> {
		Val value;
		ValueContainer(Val value) { this.value = value; }
	}
	
	private static class IterativeJsonParser {
		
		private String content = null;
		private Character glueChar = null;

		void parse(String json_text, BiConsumer<JSON_Data.Value<NV,V>, Character> consumeValue, Character...glueChars) {
			content = json_text.trim();
			Vector<Character> knownGlueChars = new Vector<>(Arrays.asList(glueChars));
			try {
				
				while( !content.isEmpty() ) {
					if (Thread.currentThread().isInterrupted()) break;
					
					JSON_Parser<NV, V> parser = new JSON_Parser<NV,V>(content, null);
					glueChar = null;
					JSON_Data.Value<NV,V> result = parser.parse_withParseException(str -> {
						//if (str.length()>40) System.out.printf("Remaining Text: \"%s...\"%n", str.substring(0, 40));
						//else                 System.out.printf("Remaining Text: \"%s\"%n", str);
						content = str.trim();
						if (!content.isEmpty()) {
							char ch = content.charAt(0);
							//System.out.printf("GlueChar: \"%s\"%n", ch);
							if (knownGlueChars.contains((Character)ch)) {
								content = content.substring(1);
								glueChar = ch;
							}
						}
					});
					consumeValue.accept(result,glueChar);
				}
				
			} catch (ParseException ex) {
				System.err.printf("ParseException while parsing content of file \"%s\".", ex.getMessage());
				//ex.printStackTrace();
				return;
			}
		}
	}


	static class AppSettings extends Settings.DefaultAppSettings<AppSettings.ValueGroup, AppSettings.ValueKey> {
		public enum ValueKey {
			OpenFile,
			AchievementsConfigDialogWidth,
			AchievementsConfigDialogHeight,
			AchievementsConfigDialogShowTabbedView
		}
	
		enum ValueGroup implements Settings.GroupKeys<ValueKey> {
			;
			ValueKey[] keys;
			ValueGroup(ValueKey...keys) { this.keys = keys;}
			@Override public ValueKey[] getKeys() { return keys; }
		}
		
		public AppSettings() { super(PlanetCrafterSaveGameViewer.class, ValueKey.values()); }
	}
}
