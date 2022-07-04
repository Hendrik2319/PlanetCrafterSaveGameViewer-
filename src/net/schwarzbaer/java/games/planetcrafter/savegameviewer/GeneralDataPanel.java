package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import java.util.Vector;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;

import net.schwarzbaer.gui.ContextMenu;
import net.schwarzbaer.gui.Tables;
import net.schwarzbaer.gui.Tables.SimplifiedColumnConfig;
import net.schwarzbaer.gui.Tables.SimplifiedTableModel;

class GeneralDataPanel extends JScrollPane {
	private static final long serialVersionUID = -9191759791973305801L;
	//private Data data;

	GeneralDataPanel(Data data) {
		//this.data = data;
		GridBagConstraints c;
		
		
		
		JPanel upperPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 2;
		
		c.gridy = 0;
		c.gridx = 0; upperPanel.add(createPanel("Terraforming"    , data.terraformingStates, TerraformingStatesPanel::new), c);
		c.gridx = 1; upperPanel.add(createPanel("Player"          , data.playerStates      , PlayerStatesPanel      ::new), c);
		
		c.gridheight = 1;
		c.gridx = 2; 
		c.gridy = 0; upperPanel.add(createPanel("General Data (1)", data.generalData1      , GeneralData1Panel      ::new), c);
		c.gridy = 1; upperPanel.add(createPanel("General Data (2)", data.generalData2      , GeneralData2Panel      ::new), c);
		
		
		
		JPanel lowerPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridy = 0;
		c.gridx = -1;
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<Data.Message>("Messages", data.messages,
					new SimpleTablePanel.Column("ID"      , String .class, 170, row->((Data.Message)row).stringId),
					new SimpleTablePanel.Column("Is Read?", Boolean.class,  60, row->((Data.Message)row).isRead  )
				), c);
		
		c.gridx++;
		lowerPanel.add(
				new SimpleTablePanel<Data.StoryEvent>("StoryEvents", data.storyEvents,
					new SimpleTablePanel.Column("ID"      , String .class, 230, row->((Data.StoryEvent)row).stringId)
				), c);
		
		c.gridx++;
		lowerPanel.add(
			new SimpleTablePanel<Data.Layer>("Layers", data.layers,
				new SimpleTablePanel.Column("ID"              , String.class,  75, row->((Data.Layer)row).layerId        ),
				new SimpleTablePanel.Column("Color Base      ", String.class, 180, row->((Data.Layer)row).colorBase      ),
				new SimpleTablePanel.Column("Color Custom    ", String.class,  90, row->((Data.Layer)row).colorCustom    ),
				new SimpleTablePanel.Column("Color BaseLerp  ", Long  .class,  90, row->((Data.Layer)row).colorBaseLerp  ),
				new SimpleTablePanel.Column("Color CustomLerp", Long  .class, 100, row->((Data.Layer)row).colorCustomLerp)
			), c);
		
		
		
		JPanel mainPanel = new JPanel(new GridBagLayout());
		c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		
		c.weightx = 1;
		c.weighty = 1;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.gridx = 0;
		
		c.gridy = 0; mainPanel.add(upperPanel, c);
		c.gridy = 1; mainPanel.add(lowerPanel, c);
		
		
		setViewportView(mainPanel);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
		horizontalScrollBar.setUnitIncrement(10);
		verticalScrollBar  .setUnitIncrement(10);
		//System.out.printf("%d, %d%n", horizontalScrollBar.getUnitIncrement(), verticalScrollBar.getUnitIncrement());
	}
	
	private <ValueType> JComponent createPanel(String title, Vector<ValueType> values, Function<ValueType,JPanel> panelConstructor) {
		if (values==null) throw new IllegalArgumentException();
		
		if (values.isEmpty()) {
			JPanel panel = new JPanel();
			panel.setBorder(BorderFactory.createTitledBorder(title));
			return panel;
		}
		
		if (values.size()==1) {
			JPanel panel = panelConstructor.apply(values.get(0));
			panel.setBorder(BorderFactory.createTitledBorder(title));
			return panel;
		}
		
		JTabbedPane panel = new JTabbedPane();
		panel.setBorder(BorderFactory.createTitledBorder(title));
		for (int i=0; i<values.size(); i++)
			panel.addTab(Integer.toString(i), panelConstructor.apply(values.get(i)));
		
		return panel;
	}

	private static JTextField createOutputTextField(String text) {
		JTextField comp = new JTextField(text);
		comp.setEditable(false);
		return comp;
	}

	private static class TerraformingStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		TerraformingStatesPanel(Data.TerraformingStates data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			
			c.gridy = 0;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Biomass: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.biomassLevel)), c);
			
			c.gridy = 1;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Heat: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.heatLevel)), c);
			
			c.gridy = 2;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Oxygen: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.oxygenLevel)), c);
			
			c.gridy = 3;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Pressure: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.pressureLevel)), c);
			
			c.gridy = 4;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
	}

	private static class PlayerStatesPanel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		PlayerStatesPanel(Data.PlayerStates data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			
			c.gridy = 0;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Health: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.health)), c);
			
			c.gridy = 1;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Thirst: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.thirst)), c);
			
			c.gridy = 2;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Oxygen: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.oxygen)), c);
			
			c.gridy = 3;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Position: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.position)), c);
			
			c.gridy = 4;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Rotation: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.rotation)), c);
			
			Vector<String> unlockedGroups = new Vector<>(Arrays.asList(data.unlockedGroups));
			unlockedGroups.sort(Data.caseIgnoringComparator);
			JTextArea textArea = new JTextArea(String.join(", ", unlockedGroups));
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			JScrollPane textAreaScrollPane = new JScrollPane(textArea);
			textAreaScrollPane.setPreferredSize(new Dimension(100,100));
			textAreaScrollPane.setBorder(BorderFactory.createTitledBorder("Unlocked Groups"));
			
			c.gridy = 5;
			c.gridx = 0; 
			c.gridwidth = 2;
			c.weightx = 1;
			c.weighty = 1;
			add(textAreaScrollPane, c);
			
//			c.gridy = 6;
//			c.weighty = 1;
//			c.weightx = 1;
//			c.gridwidth = 2;
//			c.gridx = 0;
//			add(new JLabel(), c);
		}
	}

	private static class GeneralData1Panel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		GeneralData1Panel(Data.GeneralData1 data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			
			c.gridy = 0;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Crafted Objects: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.craftedObjects)), c);
			
			c.gridy = 1;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Total SaveFile Load: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.totalSaveFileLoad)), c);
			
			c.gridy = 2;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Total SaveFile Time: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.totalSaveFileTime)), c);
			
			c.gridy = 3;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
	}

	private static class GeneralData2Panel extends JPanel {
		private static final long serialVersionUID = 6272012218012618784L;

		GeneralData2Panel(Data.GeneralData2 data) {
			super(new GridBagLayout());
			
			GridBagConstraints c = new GridBagConstraints();
			c.fill = GridBagConstraints.BOTH;
			
			c.weighty = 0;
			c.gridwidth = 1;
			c.gridheight = 1;
			
			c.gridy = 0;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Has Played Intro: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.hasPlayedIntro)), c);
			
			c.gridy = 1;
			c.weightx = 0; c.gridx = 0; add(new JLabel("Mode: "), c);
			c.weightx = 1; c.gridx = 1; add(createOutputTextField(String.format("%s", data.mode)), c);
			
			c.gridy = 2;
			c.weighty = 1;
			c.weightx = 1;
			c.gridwidth = 2;
			c.gridx = 0;
			add(new JLabel(), c);
		}
	}
	
	private static class SimpleTablePanel<ValueType> extends JScrollPane {
		private static final long serialVersionUID = -8500969138264337829L;

		SimpleTablePanel(String title, Vector<ValueType> data, Column... columns) {
			SimpleTableModel<ValueType> tableModel = new SimpleTableModel<ValueType>(data,columns);
			JTable table = new JTable(tableModel);
			table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			table.setRowSorter(new Tables.SimplifiedRowSorter(tableModel));
			table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			tableModel.setTable(table);
			tableModel.setColumnWidths(table);
			
			new TableContextMenu(table,tableModel);
			
			setViewportView(table);
			setBorder(BorderFactory.createTitledBorder(title));
			
			//Dimension size = table.getPreferredScrollableViewportSize();
			Dimension size = table.getPreferredSize();
			size.width  += 30;
			size.height += 50;
			setPreferredSize(size);
			
			//setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			//setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
		}
		
		private class TableContextMenu extends ContextMenu {
			private static final long serialVersionUID = 1755523803906870773L;

			TableContextMenu(JTable table, SimpleTableModel<ValueType> tableModel) {
				add(PlanetCrafterSaveGameViewer.createMenuItem("Show Column Widths", e->{
					System.out.printf("Column Widths: %s%n", SimplifiedTableModel.getColumnWidthsAsString(table));
				}));
				
				addTo(table);
			}
		}
		
		private static class SimpleTableModel<ValueType> extends Tables.SimplifiedTableModel<Column> {

			private final Vector<ValueType> data;

			protected SimpleTableModel(Vector<ValueType> data, Column[] columns) {
				super(columns);
				this.data = data;
			}

			@Override public int getRowCount() { return data.size(); }

			@Override public Object getValueAt(int rowIndex, int columnIndex, Column columnID) {
				if (rowIndex<0) return null;
				if (rowIndex>=data.size()) return null;
				ValueType row = data.get(rowIndex);
				return columnID.getValue.apply(row);
			}
			
		}
		
		static class Column implements Tables.SimplifiedColumnIDInterface {
			
			private final SimplifiedColumnConfig config;
			private final Function<Object, Object> getValue;

			Column(String name, Class<?> columnClass, int width, Function<Object,Object> getValue) {
				this.getValue = getValue;
				config = new SimplifiedColumnConfig(name, columnClass, 20, -1, width, width);
			}

			@Override public SimplifiedColumnConfig getColumnConfig() {
				return config;
			}
		}
	}
}