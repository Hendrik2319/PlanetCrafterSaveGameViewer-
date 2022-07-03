package net.schwarzbaer.java.games.planetcrafter.savegameviewer;

import java.util.Locale;
import java.util.Vector;
import java.util.function.Function;
import java.util.function.IntFunction;

import net.schwarzbaer.java.lib.jsonparser.JSON_Data;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.JSON_Object;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.TraverseException;
import net.schwarzbaer.java.lib.jsonparser.JSON_Data.Value;

class Data {
	static class NV extends JSON_Data.NamedValueExtra.Dummy {}
	static class  V extends JSON_Data.ValueExtra.Dummy {}

	static Data parse(Vector<Vector<Value<NV, V>>> jsonStructure) {
		try {
			return new Data(jsonStructure);
			
		} catch (ParseException ex) {
			System.err.printf("ParseException while parsing JSON structure: %s%n", ex.getMessage());
			//ex.printStackTrace();
			return null;
			
		} catch (TraverseException ex) {
			System.err.printf("TraverseException while parsing JSON structure: %s%n", ex.getMessage());
			//ex.printStackTrace();
			return null;
		}
	}

	final Vector<TerraformingStates> terraformingStates;
	final Vector<PlayerStates> playerStates;
	final Vector<WorldObject> worldObjects;
	final Vector<ObjectList> objectLists;
	final Vector<GeneralData1> generalData1;
	final Vector<Message> messages;
	final Vector<StoryEvent> storyEvents;
	final Vector<GeneralData2> generalData2;
	final Vector<Layer> layers;
	
	private Data(Vector<Vector<Value<NV, V>>> dataVec) throws ParseException, TraverseException {
		if (dataVec==null) throw new IllegalArgumentException();
		
		System.out.printf("Parse JSON Structure ...%n");
		int blockIndex = 0;
		/* 0 */ terraformingStates = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), TerraformingStates::new, "TerraformingStates"); blockIndex++;
		/* 1 */ playerStates       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), PlayerStates      ::new, "PlayerStates"      ); blockIndex++;
		/* 2 */ worldObjects       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), WorldObject       ::new, "WorldObjects"      ); blockIndex++;
		/* 3 */ objectLists        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), ObjectList        ::new, "ObjectLists"       ); blockIndex++;
		/* 4 */ generalData1       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData1      ::new, "GeneralData1"      ); blockIndex++;
		/* 5 */ messages           = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Message           ::new, "Messages"          ); blockIndex++;
		/* 6 */ storyEvents        = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), StoryEvent        ::new, "StoryEvents"       ); blockIndex++;
		/* 7 */ generalData2       = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), GeneralData2      ::new, "GeneralData2"      ); blockIndex++;
		/* 8 */ layers             = dataVec.size()<=blockIndex ? null : parseArray( dataVec.get(blockIndex), Layer             ::new, "Layers"            ); blockIndex++;
		
		System.out.printf("Process Data ...%n");
		for (WorldObject wo : worldObjects) {
			if (wo.listId <= 0) continue;
			for (ObjectList ol : objectLists)
				if (ol.id==wo.listId) {
					ol.container = wo;
					wo.list = ol;
					break;
				}
		}
		
		for (ObjectList ol : objectLists) {
			int[] worldObjIds = ol.worldObjIds;
			ol.worldObjs = new WorldObject[worldObjIds.length];
			for (int i=0; i<worldObjIds.length; i++) {
				int woId = worldObjIds[i];
				ol.worldObjs[i] = null;
				for (WorldObject wo : worldObjects)
					if (wo.id==woId) {
						ol.worldObjs[i] = wo;
						wo.container = ol.container;
						wo.containerList = ol;
						break;
					}
			}
		}
		
		
		System.out.printf("Done%n");
	}
	
	private static <ValueType> Vector<ValueType> parseArray(Vector<Value<NV, V>> vector, ParseConstructor<ValueType> parseConstructor, String debugLabel) throws ParseException, TraverseException {
		Vector<ValueType> parsedVec = new Vector<>();
		for (int i=0; i< vector.size(); i++) {
			Value<NV, V> val = vector.get(i);
			String newDebugLabel = String.format("%s[%d]", debugLabel, i);
			ValueType parsedValue = parseConstructor.parse(val, newDebugLabel);
			parsedVec.add(parsedValue);
		}
		return parsedVec;
	}
	
	private static <ValueType> ValueType[] parseCommaSeparatedArray(
			String str, String debugLabel, String typeLabel,
			IntFunction<ValueType[]> createArray,
			Function<String,ValueType> parseValue)
					throws ParseException {
		
		if (str==null) throw new IllegalArgumentException();
		if (debugLabel==null) throw new IllegalArgumentException();
		
		if (str.isEmpty())
			return createArray.apply(0);
		
		String[] parts = str.split(",",-1);
		ValueType[] results = createArray.apply(parts.length);
		for (int i=0; i<parts.length; i++) {
			String part = parts[i];
			results[i] = parseValue.apply(part);
			if (results[i] == null)
				throw new ParseException("%s: Can't convert value %d of list into %s: \"%s\"", debugLabel, i, typeLabel, part);
		}
		
		return results;
	}

	static double[] parseDoubleArray(String str, String debugLabel) throws ParseException {
		Double[] arr1 = parseCommaSeparatedArray(str, debugLabel, "Double", Double[]::new, valStr->{
			try { return Double.parseDouble(valStr); }
			catch (NumberFormatException e) { return null; }
		});
		
		double[] results = new double[arr1.length];
		for (int i=0; i<arr1.length; i++)
			results[i] = arr1[i].doubleValue();
		
		return results;
	}

	static int[] parseIntegerArray(String str, String debugLabel) throws ParseException {
		Integer[] arr1 = parseCommaSeparatedArray(str, debugLabel, "Integer", Integer[]::new, valStr->{
			try { return Integer.parseInt(valStr); }
			catch (NumberFormatException e) { return null; }
		});
		
		int[] results = new int[arr1.length];
		for (int i=0; i<arr1.length; i++)
			results[i] = arr1[i].intValue();
		
		return results;
	}

	static String[] parseStringArray(String str, String debugLabel) throws ParseException {
		return parseCommaSeparatedArray(str, debugLabel, "String", String[]::new, str1->str1);
	}

	interface ParseConstructor<ValueType> {
		ValueType parse(Value<NV, V> value, String debugLabel) throws ParseException, TraverseException;
	}
	
	static class ParseException extends Exception {
		private static final long serialVersionUID = 7894187588880980010L;

		ParseException(String format, Object...args) {
			super(String.format(Locale.ENGLISH, format, args));
		}
	}

	static class Coord3 {
		final double x,y,z;
		Coord3(String str, String debugLabel) throws ParseException {
			double[] arr =  parseDoubleArray(str, debugLabel);
			if (arr.length!=3) throw new ParseException("%s: Unexpected length of array: %d (!=3)", debugLabel, arr.length);
			x = arr[0];
			y = arr[1];
			z = arr[2];
		}
		@Override
		public String toString() {
			if (isZero()) return "- 0 -";
			return String.format(Locale.ENGLISH, "%1.5f, %1.5f, %1.5f", x, y, z);
		}
		public boolean isZero() {
			return x==0 && y==0 && z==0;
		}
	}
	
	static class Rotation {
		final double x,y,z,w;
		Rotation(String str, String debugLabel) throws ParseException {
			double[] arr =  parseDoubleArray(str, debugLabel);
			if (arr.length!=4) throw new ParseException("%s: Unexpected length of array: %d (!=4)", debugLabel, arr.length);
			x = arr[0];
			y = arr[1];
			z = arr[2];
			w = arr[3];
		}
		@Override
		public String toString() {
			if (isZero()) return "- 0 -";
			return String.format(Locale.ENGLISH, "%1.5f, %1.5f, %1.5f, %1.5f", x, y, z, w);
		}
		public boolean isZero() {
			return x==0 && y==0 && z==0 && w==0;
		}
	}
	static class TerraformingStates {
		final double biomassLevel;
		final double heatLevel;
		final double oxygenLevel;
		final double pressureLevel;
		/*
			Block[0]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [4]
			        unitBiomassLevel:Float
			        unitHeatLevel:Float
			        unitOxygenLevel:Float
			        unitPressureLevel:Float
		 */
		TerraformingStates(Value<NV, V> value, String debugLabel) throws TraverseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			biomassLevel  = JSON_Data.getFloatValue(object, "unitBiomassLevel" , debugLabel);
			heatLevel     = JSON_Data.getFloatValue(object, "unitHeatLevel"    , debugLabel);
			oxygenLevel   = JSON_Data.getFloatValue(object, "unitOxygenLevel"  , debugLabel);
			pressureLevel = JSON_Data.getFloatValue(object, "unitPressureLevel", debugLabel);
		}
	}

	static class PlayerStates {
		final double health;
		final double oxygen;
		final double thirst;
		final Coord3 position;
		final Rotation rotation;
		final String[] unlockedGroups;
		/*
			Block[1]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [6]
			        playerGaugeHealth:Float
			        playerGaugeOxygen:Float
			        playerGaugeThirst:Float
			        playerPosition:String
			        playerRotation:String
			        unlockedGroups:String
		 */
		PlayerStates(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			String positionStr, rotationStr, unlockedGroupsStr; 
			health            = JSON_Data.getFloatValue (object, "playerGaugeHealth", debugLabel);
			oxygen            = JSON_Data.getFloatValue (object, "playerGaugeOxygen", debugLabel);
			thirst            = JSON_Data.getFloatValue (object, "playerGaugeThirst", debugLabel);
			positionStr       = JSON_Data.getStringValue(object, "playerPosition"   , debugLabel);
			rotationStr       = JSON_Data.getStringValue(object, "playerRotation"   , debugLabel);
			unlockedGroupsStr = JSON_Data.getStringValue(object, "unlockedGroups"   , debugLabel);
			
			position = new Coord3  (positionStr, debugLabel+".playerPosition");
			rotation = new Rotation(rotationStr, debugLabel+".playerRotation");
			unlockedGroups = parseStringArray(unlockedGroupsStr, debugLabel+".unlockedGroups");
		}
	}
	
	static class WorldObject {
		final long     id;
		final String   objType;
		final long     listId;
		final String   _liGrps;
		final Coord3   position;
		final Rotation rotation;
		final long     _wear;
		final String   _pnls;
		final String   _color;
		//final Coord3   color;
		final String   text;
		final long     _growth;
		
		ObjectList     list;
		WorldObject    container;
		ObjectList     containerList;
		/*
			Block[2]: 3033 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [11]
			        id    :Integer
			        gId   :String
			        liId  :Integer
			        liGrps:String
			        pos   :String
			        rot   :String
			        wear  :Integer
			        pnls  :String
			        color :String
			        text  :String
			        grwth :Integer
		 */
		WorldObject(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			@SuppressWarnings("unused")
			String positionStr, rotationStr, colorStr;
			id          = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			objType     = JSON_Data.getStringValue (object, "gId"   , debugLabel);
			listId      = JSON_Data.getIntegerValue(object, "liId"  , debugLabel);
			_liGrps     = JSON_Data.getStringValue (object, "liGrps", debugLabel);
			positionStr = JSON_Data.getStringValue (object, "pos"   , debugLabel);
			rotationStr = JSON_Data.getStringValue (object, "rot"   , debugLabel);
			_wear       = JSON_Data.getIntegerValue(object, "wear"  , debugLabel);
			_pnls       = JSON_Data.getStringValue (object, "pnls"  , debugLabel);
			_color      = JSON_Data.getStringValue (object, "color" , debugLabel);
			//colorStr    = JSON_Data.getStringValue (object, "color" , debugLabel); // TODO: wait for color value
			text        = JSON_Data.getStringValue (object, "text"  , debugLabel);
			_growth     = JSON_Data.getIntegerValue(object, "grwth" , debugLabel);
			
			position    = new Coord3  (positionStr, debugLabel+".pos");
			rotation    = new Rotation(rotationStr, debugLabel+".rot");
			//color       = new Coord3  (colorStr   , debugLabel+".color");
			
			list      = null;
			container = null;
			containerList = null;
		}
	}

	static class ObjectList {
		final long id;
		final long size;
		final int[] worldObjIds;
		WorldObject[] worldObjs;
		WorldObject container;

		/*
			Block[3]: 221 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [3]
			        id:Integer
			        size:Integer
			        woIds:String
		 */
		ObjectList(Value<NV, V> value, String debugLabel) throws TraverseException, ParseException {
			JSON_Object<NV, V> object = JSON_Data.getObjectValue(value, debugLabel);
			String woIdsStr;
			id          = JSON_Data.getIntegerValue(object, "id"    , debugLabel);
			size        = JSON_Data.getIntegerValue(object, "size"  , debugLabel);
			woIdsStr    = JSON_Data.getStringValue (object, "woIds" , debugLabel);
			
			worldObjIds = parseIntegerArray(woIdsStr, debugLabel+".woIds");
			worldObjs = null;
			container = null;
		}
	}

	static class GeneralData1 {
		/*
			Block[4]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [3]
			        craftedObjects:Integer
			        totalSaveFileLoad:Integer
			        totalSaveFileTime:Integer
		 */
		GeneralData1(Value<NV, V> value, String debugLabel) {
			// TODO
		}
	}

	static class Message {
		/*
			Block[5]: 6 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [2]
			        isRead:Bool
			        stringId:String
		 */
		Message(Value<NV, V> value, String debugLabel) {
			// TODO
		}
	}

	static class StoryEvent {
		/*
			Block[6]: 6 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [1]
			        stringId:String
		 */
		StoryEvent(Value<NV, V> value, String debugLabel) {
			// TODO
		}
	}

	static class GeneralData2 {
		/*
			Block[7]: 1 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [2]
			        hasPlayedIntro:Bool
			        mode:String
		 */
		GeneralData2(Value<NV, V> value, String debugLabel) {
			// TODO
		}
	}

	static class Layer {
		/*
			Block[8]: 10 entries
			-> Format: [2 blocks]
			    Block "ParseResult" [0]
			        <Base>:Object
			    Block "ParseResult.<Base>" [5]
			        colorBase:String
			        colorBaseLerp:Integer
			        colorCustom:String
			        colorCustomLerp:Integer
			        layerId:String
		 */
		Layer(Value<NV, V> value, String debugLabel) {
			// TODO
		}
	}

/*
Block[0]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [4]
        unitBiomassLevel:Float
        unitHeatLevel:Float
        unitOxygenLevel:Float
        unitPressureLevel:Float
Block[1]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [6]
        playerGaugeHealth:Float
        playerGaugeOxygen:Float
        playerGaugeThirst:Float
        playerPosition:String
        playerRotation:String
        unlockedGroups:String
Block[2]: 3033 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [11]
        color:String
        gId:String
        grwth:Integer
        id:Integer
        liGrps:String
        liId:Integer
        pnls:String
        pos:String
        rot:String
        text:String
        wear:Integer
Block[3]: 221 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [3]
        id:Integer
        size:Integer
        woIds:String
Block[4]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [3]
        craftedObjects:Integer
        totalSaveFileLoad:Integer
        totalSaveFileTime:Integer
Block[5]: 6 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [2]
        isRead:Bool
        stringId:String
Block[6]: 6 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [1]
        stringId:String
Block[7]: 1 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [2]
        hasPlayedIntro:Bool
        mode:String
Block[8]: 10 entries
-> Format: [2 blocks]
    Block "ParseResult" [0]
        <Base>:Object
    Block "ParseResult.<Base>" [5]
        colorBase:String
        colorBaseLerp:Integer
        colorCustom:String
        colorCustomLerp:Integer
        layerId:String
 */
}
