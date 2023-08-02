package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.util.function.Supplier;

import javax.swing.JMenuItem;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.TableCellRenderer;

import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Coord3;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.ObjectList;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.Rotation;
import net.schwarzbaer.java.games.planetcrafter.savegameviewer.Data.WorldObject;
import net.schwarzbaer.java.lib.gui.Tables;

class WorldObjectsPanel extends AbstractTablePanel<WorldObject, WorldObjectsPanel.WorldObjectsTableModel.ColumnID> {
	private static final long serialVersionUID = 8733627835226098636L;

	WorldObjectsPanel(PlanetCrafterSaveGameViewer main, Data data, MapPanel mapPanel) {
		super(new WorldObjectsTableModel(data), false, (table,tableModel) -> new TableContextMenu(main, table, tableModel, mapPanel), LayoutPos.Right, new Dimension(300, 200));
	}
	
	private static class TableContextMenu extends AbstractTablePanel.TableContextMenu {
		private static final long serialVersionUID = -8757567111391531443L;
		private int clickedRowIndex;
		private WorldObject clickedRow;
		private int[] selectedRowIndexes;
		private WorldObject[] selectedRows;

		TableContextMenu(PlanetCrafterSaveGameViewer main, JTable table, WorldObjectsTableModel tableModel, MapPanel mapPanel) {
			super(table);
			clickedRowIndex = -1;
			
			addSeparator();
			
			JMenuItem miShowInMap = add(GUI.createMenuItem("Show in Map", e->{
				if (!WorldObject.isInstalled(clickedRow)) return;
				mapPanel.showWorldObject(clickedRow);
			}));
			
			JMenuItem miShowContainerInMap = add(GUI.createMenuItem("Show Container in Map", e->{
				if (clickedRow==null) return;
				if (!WorldObject.isInstalled(clickedRow.container))return;
				mapPanel.showWorldObject(clickedRow.container);
			}));
			
			addSeparator();
			
			JMenuItem miEditMapShapes = add(GUI.createMenuItem("Create/Edit MapShapes", e->{
				if (clickedRow==null) return;
				main.mapShapesEditor.showDialog(clickedRow.objectType);
			}));
			
			addSeparator();
			
			JMenuItem miMarkForRemoval = add(GUI.createMenuItem("Mark clicked object for removal", e->{
				if (clickedRow!=null && clickedRow.canMarkedByUser()) {
					clickedRow.markForRemoval( !clickedRow.isMarkedForRemoval(), true );
					tableModel.fireTableRowUpdate(clickedRowIndex);
					Data.notifyAllRemoveStateListeners();
				}
			}));
			
			JMenuItem miMarkSelectedForRemoval = add(GUI.createMenuItem("Mark selected object(s) for removal", e->{
				boolean markForRemoval = selectedRows[0]==null || !selectedRows[0].isMarkedForRemoval();
				for (WorldObject wo : selectedRows)
					if (wo!=null && wo.canMarkedByUser())
						wo.markForRemoval( markForRemoval, true );
				tableModel.fireTableUpdate();
				Data.notifyAllRemoveStateListeners();
			}));
			
			
			addContextMenuInvokeListener((comp, x, y) -> {
				int rowV = table.rowAtPoint(new Point(x,y));
				clickedRowIndex = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
				clickedRow = clickedRowIndex<0 ? null : tableModel.getRow(clickedRowIndex);
				
				int[] selectedRowIndexesV = table.getSelectedRows();
				selectedRowIndexes = new int[selectedRowIndexesV.length];
				selectedRows = new WorldObject[selectedRowIndexesV.length];
				for (int i=0; i<selectedRowIndexes.length; i++) {
					selectedRowIndexes[i] = selectedRowIndexesV[i]<0 ? -1 : table.convertRowIndexToModel(selectedRowIndexesV[i]);
					selectedRows[i] = selectedRowIndexes[i]<0 ? null : tableModel.getRow(selectedRowIndexes[i]);
				}
				
				miMarkForRemoval.setEnabled(clickedRow!=null && clickedRow.canMarkedByUser());
				miMarkForRemoval.setText(
						clickedRow == null
							? "Mark clicked object for removal"
							: clickedRow.isMarkedForRemoval()
								? String.format("Remove Removal Marker from \"%s\"", clickedRow.getName())
								: String.format("Mark \"%s\" for removal", clickedRow.getName())
				);
				
				miMarkSelectedForRemoval.setEnabled(selectedRows.length>0);
				miMarkSelectedForRemoval.setText(
						selectedRows.length == 0
							? "Mark selected object(s) for removal"
							: selectedRows[0]!=null && selectedRows[0].isMarkedForRemoval()
								? String.format("Remove Removal Marker from %d selected object%s", selectedRows.length, selectedRows.length==1 ? "" : "s")
								: String.format("Mark %d selected object%s for removal", selectedRows.length, selectedRows.length==1 ? "" : "s")
				);
				
				miShowInMap.setEnabled(
					WorldObject.isInstalled(clickedRow) );
				miShowInMap.setText(
					! WorldObject.isInstalled(clickedRow)
					? "Show in Map"
					: String.format("Show \"%s\" in Map", clickedRow.getName())
				);
				
				miShowContainerInMap.setEnabled(
					clickedRow!=null && WorldObject.isInstalled(clickedRow.container));
				miShowContainerInMap.setText(
					clickedRow==null || !WorldObject.isInstalled(clickedRow.container)
					? "Show Container in Map"
					: String.format("Show Container \"%s\" in Map", clickedRow.container.getName())
				);
				
				miEditMapShapes.setEnabled(clickedRow!=null);
				miEditMapShapes.setText(
					clickedRow == null
					? "Create/Edit MapShapes"
					: main.mapShapes.hasShapes(clickedRow.objectType)
						? String.format(  "Edit MapShapes of \"%s\"", clickedRow.getName())
						: String.format("Create MapShapes of \"%s\"", clickedRow.getName())
				);
			});
		}
	}
	
	static class GeneralTCR implements TableCellRenderer {
		
		private final Tables.LabelRendererComponent standardComp;
		private final Tables.CheckBoxRendererComponent boolComp;
		private final Tables.ColorRendererComponent colorComp;
		private final WorldObjectsTableModel tableModel;

		GeneralTCR(WorldObjectsTableModel tableModel) {
			this.tableModel = tableModel;
			colorComp = new Tables.ColorRendererComponent();
			standardComp = new Tables.LabelRendererComponent();
			boolComp = new Tables.CheckBoxRendererComponent();
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int rowV, int columnV) {
			int rowM = rowV<0 ? -1 : table.convertRowIndexToModel(rowV);
			int columnM = columnV<0 ? -1 : table.convertColumnIndexToModel(columnV);
			WorldObjectsTableModel.ColumnID columnID = tableModel.getColumnID(columnM);
			WorldObject row = tableModel.getRow(rowM);
			
			final Component selectedRendererComponent;
			
			Supplier<Color> getCustomBackground = ()->{
				if (row==null) return null;
				if (!row.isMarkedForRemoval()) return null;
				if (!row.canMarkedByUser())
					return GUI.COLOR_Removal_ByData;
				else
					return GUI.COLOR_Removal_ByUser;
			};
			
			if (value instanceof Data.Color) {
				Data.Color color = (Data.Color) value;
				value = color.getColor();
			}
			
			if (value instanceof Color) {
				selectedRendererComponent = colorComp;
				Supplier<String> getSurrogateText = ()->{
					if (columnID==WorldObjectsTableModel.ColumnID.color && row!=null)
						return row.colorStr;
					return null;
				};
				colorComp.configureAsTableCellRendererComponent(table, value, isSelected, hasFocus, getSurrogateText, getCustomBackground, null);
			} else
				
			if (value instanceof Boolean) {
				boolean isChecked = (Boolean) value;
				selectedRendererComponent = boolComp;
				boolComp.configureAsTableCellRendererComponent(table, isChecked, null, isSelected, hasFocus, null, getCustomBackground);
				boolComp.setHorizontalAlignment(SwingConstants.CENTER);
				
			} else {
				selectedRendererComponent = standardComp;
				String valueStr = value==null ? null : value.toString();
				standardComp.configureAsTableCellRendererComponent(table, null, valueStr, isSelected, hasFocus, getCustomBackground, null);
				if (value instanceof Number)
					standardComp.setHorizontalAlignment(SwingConstants.RIGHT);
				else
					standardComp.setHorizontalAlignment(SwingConstants.LEFT);
			}
			
			
			return selectedRendererComponent;
		}
	}
	
	static class WorldObjectsTableModel extends AbstractTablePanel.AbstractTableModel<WorldObject, WorldObjectsTableModel.ColumnID> {
		
		// Column Widths: [75, 30, 35, 130, 130, 350, 70, 120, 60, 130, 36, 200, 33, 205, 50, 90, 50] in ModelOrder
		enum ColumnID implements Tables.SimplifiedColumnIDInterface {
			id          ("ID"          , Long      .class,  75),
			NonUniqueID ("UnI"         , Boolean   .class,  30),
			twinID      ("Twin"        , Boolean   .class,  35),
			objectTypeID("ObjectTypeID", String    .class, 130),
			Name        ("Name"        , String    .class, 130),
			container   ("Container"   , String    .class, 350),
			listId      ("List-ID"     , Long      .class,  70),
			text        ("Text"        , String    .class, 120),
			growth      ("Growth"      , Long      .class,  60),
			product     ("Product"     , String    .class, 130),
			has_position("Pos."        , Boolean   .class,  35),
			position    ("Position"    , Coord3    .class, 200),
			has_rotation("Rot."        , Boolean   .class,  35),
			rotation    ("Rotation"    , Rotation  .class, 205),
			color       ("Color"       , Data.Color.class,  50),
			mods        ("Mods"        , String    .class,  90),
			_wear       ("[wear]"      , Long      .class,  50),
			;
			private final Tables.SimplifiedColumnConfig cfg;
			ColumnID(String name, Class<?> colClass, int width) {
				cfg = new Tables.SimplifiedColumnConfig(name, colClass, 20, -1, width, width);
			}
			@Override public Tables.SimplifiedColumnConfig getColumnConfig() {
				return cfg;
			}
		
		}

		private final Data data;

		WorldObjectsTableModel(Data data) {
			super(ColumnID.values(), data.worldObjects);
			this.data = data;
		}

		@Override public void setDefaultRenderers() {
			GeneralTCR renderer = new GeneralTCR(this);
			table.setDefaultRenderer(Long      .class, renderer);
			table.setDefaultRenderer(Boolean   .class, renderer);
			table.setDefaultRenderer(String    .class, renderer);
			table.setDefaultRenderer(Coord3    .class, renderer);
			table.setDefaultRenderer(Rotation  .class, renderer);
			table.setDefaultRenderer(Data.Color.class, renderer);
		}

		@Override protected String getRowText(WorldObject row, int rowIndex) {
			String str = row==null ? "No Data" : row.generateOutput();
			if (data.mapObjectLists.containsKey(row.id)) {
				ObjectList twin = data.mapObjectLists.get(row.id);
				str += String.format("%n#################################%n");
				str += String.format(  "  Twin ObjectList with same ID%n%n");
				str += twin.generateOutput();
			}
			return str;
		}

		@Override protected Object getValueAt(int rowIndex, int columnIndex, WorldObjectsTableModel.ColumnID columnID, WorldObject row) {
			switch (columnID) {
			case _wear        : return row._wear;
			case color        : return row.color;
			case growth       : return row.growth;
			case has_position : return row.position!=null && !row.position.isZero();
			case has_rotation : return row.rotation!=null && !row.rotation.isZero();
			case id           : return row.id;
			case listId       : return row.listId;
			case mods         : return row.mods;
			case objectTypeID : return row.objectTypeID;
			case position     : return row.position;
			case product      : return row.product!=null ? row.product.getName() : row.productID.isEmpty() ? "" : String.format("{%s}", row.productID);
			case rotation     : return row.rotation;
			case text         : return row.text;
			case Name         : return row.getName();
			case twinID       : return data.mapObjectLists.containsKey(row.id);
			case NonUniqueID  : return row.nonUniqueID;
			case container:
				if (row.containerList==null)
					return null;
				if (row.container==null) {
					if (row.containerList.id==1)
						return "Player Inventory";
					if (row.containerList.id==2)
						return "Player Equipment";
					return String.format("<UnknownContainer> [List:%d]", row.containerList.id);
				}
				//return String.format("%s (\"%s\", Pos:%s)", row.container.objType, row.container.text, row.container.position);
				return row.container.getShortDesc();
			}
			return null;
		}

		@Override public void fireTableRowUpdate(int rowIndex) {
			super.fireTableRowUpdate(rowIndex);
		}
		
		
	}
}