package net.wc3c.ode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import net.wc3c.slk.SLKFile;
import net.wc3c.util.CharInt;
import net.wc3c.util.Jass;
import net.wc3c.util.Jass.Parameter;
import net.wc3c.util.Jass.Type;
import net.wc3c.w3o.Property;
import net.wc3c.w3o.PropertyType;
import net.wc3c.w3o.W3BFile;
import net.wc3c.w3o.W3BFile.Destructable;

public class DestructableDataExtractor extends Extractor<W3BFile, Destructable> {
    
    private static final int                               FIELD_ID_DEFAULT_DESTRUCTABLE = CharInt.toInt("defb");
    private static final int                               FIELD_ID_CATEGORY             = CharInt.toInt("bcat");
    private static final int                               COLUMN_DESTRUCTABLE_CATEGORY  = 1;
    
    private Hashtable<String, Metadata<DestructableField>> metadata                      = new Hashtable<>();
    
    protected DestructableDataExtractor(File enclosingFolder, W3BFile objectData) throws IOException {
        super(enclosingFolder, objectData);
    }
    
    private class DestructableField extends Field {
        
        public DestructableField(Integer id, String name) {
            super(id, name);
        }
        
        @Override
        public StringBuilder generateLoadFunction() {
            StringBuilder out = new StringBuilder();
            
            out.append(Jass.function("GetDestructableType" + getName(), getType().getJassType(), new Parameter(
                    Type.INTEGER,
                    "id")));
            out.append(Jass.ret(getType().loadFunctionName() + "(" + HASHTABLE_NAME + ",id," + getIndex() + ")"));
            out.append(Jass.endfunction());
            
            return out;
        }
        
        @Override
        public String generateSaveFunctionCall(Destructable object, Property<Destructable> property) {
            return generateSaveFunctionCall(
                    "'" + CharInt.toString(object.getId()) + "'",
                    Integer.toString(getIndex()),
                    property.getValue());
        }
        
    }
    
    private class DestructableFieldRequest extends FieldRequest {
        private boolean      restrictToCategories;
        
        private Set<String>  categories;
        private Set<Integer> destructableIdsToLoad;
        
        public DestructableFieldRequest(DestructableField field, DestructableLoadRequest request) {
            super(field, request);
            
            restrictToCategories = request.isRestrictedToCategories();
            categories = request.getCategories();
            destructableIdsToLoad = request.getItemIdsToLoad();
        }
        
        @Override
        public void absorb(LoadRequest request) {
            super.absorb(request);
            
            DestructableLoadRequest req = (DestructableLoadRequest) request;
            
            restrictToCategories = restrictToCategories && req.isRestrictedToCategories();
            categories.addAll(req.getCategories());
            destructableIdsToLoad.addAll(req.getItemIdsToLoad());
        }
        
        @Override
        protected boolean canLoad(Destructable object) {
            boolean load = false;
            
            if (isLoadDefaults()) {
                if (restrictToCategories) {
                    Property<Destructable> property = object.getProperty(FIELD_ID_CATEGORY);
                    if (property != null && categories.contains(property.getValue())) {
                        load = true;
                    } else if (destructableIdsToLoad.contains(object.getId())) {
                        load = true;
                    } else {
                        load = false;
                    }
                } else {
                    load = true;
                }
            }
            
            if (object.getPropertyEx(FIELD_ID_DEFAULT_DESTRUCTABLE) == null) {
                load = true;
            }
            
            return load;
        }
    }
    
    private class DestructableLoadRequest extends LoadRequest {
        private boolean      restrictToCategories  = false;
        
        private Set<String>  categories            = new HashSet<String>();
        private Set<Integer> destructableIdsToLoad = new HashSet<Integer>();
        
        public DestructableLoadRequest(String[] words) {
            super(words);
            
            for (String word : words) {
                if (word.toLowerCase().startsWith("categories=")) {
                    restrictToCategories = true;
                    String[] classes = word.substring(11).split(",");
                    for (String cls : classes) {
                        categories.add(cls);
                    }
                } else if (word.toLowerCase().startsWith("rawcodes=")) {
                    String[] rawcodes = word.substring(9).split(",");
                    for (String rawcode : rawcodes) {
                        destructableIdsToLoad.add(CharInt.toInt(rawcode));
                    }
                }
            }
        }
        
        public boolean isRestrictedToCategories() {
            return restrictToCategories;
        }
        
        public Set<String> getCategories() {
            return categories;
        }
        
        public Set<Integer> getItemIdsToLoad() {
            return destructableIdsToLoad;
        }
    }
    
    @Override
    protected Field createField(Integer id, String name) {
        return new DestructableField(id, name);
    }
    
    @Override
    protected LoadRequest createLoadRequest(String loadRequestLine) {
        return new DestructableLoadRequest(loadRequestLine.split("\\s+"));
    }
    
    @Override
    protected FieldRequest createFieldRequest(Field field, LoadRequest request) {
        return new DestructableFieldRequest((DestructableField) field, (DestructableLoadRequest) request);
    }
    
    @Override
    protected String getCommandString() {
        return "LoadDestructableData";
    }
    
    @Override
    protected String getLibraryNameString() {
        return "ExportedDestructableData";
    }
    
    @Override
    protected String getLibraryInitializerString() {
        return "InitExportedDestructableData";
    }
    
    @Override
    protected void loadMetadata() throws IOException {
        SLKFile slk = new SLKFile(new File(getMetaPath(), "DestructableMetaData.slk"));
        
        for (int row = 1; row < slk.getHeight(); row += 1) {
            String fieldID = slk.getCell(0, row).toString();
            String name = slk.getCell(1, row).toString();
            String slkName = slk.getCell(2, row).toString();
            String index = slk.getCell(3, row).toString();
            String type = slk.getCell(7, row).toString();
            
            DestructableField f = (DestructableField) getField(CharInt.toInt(fieldID));
            
            if (f != null) {
                removeFromInvalidFields(f);
                
                Metadata<DestructableField> m = metadata.get(slkName);
                if (m == null) {
                    m = new Metadata<DestructableField>();
                    metadata.put(slkName, m);
                }
                m.insertIntoFieldMapping(name, Integer.parseInt(index), f);
                
                if (type.equals("int")) {
                    f.setType(FieldType.INTEGER);
                } else if (type.equals("bool")) {
                    f.setType(FieldType.BOOLEAN);
                } else if (type.endsWith("real")) {
                    f.setType(FieldType.REAL);
                } else {
                    f.setType(FieldType.STRING);
                }
            }
        }
    }
    
    @Override
    protected void loadDefaultObjects() throws IOException {
        SLKFile slk = new SLKFile(new File(getDataPath(), "DestructableData.slk"));
        for (int i = 1; i < slk.getHeight(); i += 1) {
            Object[] row = slk.getRow(i);
            
            int id = CharInt.toInt(row[0].toString());
            String cls = row[COLUMN_DESTRUCTABLE_CATEGORY].toString();
            
            if (getObject(id) == null) {
                Destructable destructable = new Destructable(id);
                destructable.addProperty(new Property<Destructable>(
                        FIELD_ID_DEFAULT_DESTRUCTABLE,
                        PropertyType.BOOLEAN,
                        true));
                destructable.addProperty(new Property<Destructable>(FIELD_ID_CATEGORY, PropertyType.STRING, cls));
                addObject(destructable);
            }
        }
    }
    
    @Override
    protected void loadProfile(File file) throws IOException {
        // Destructables don't make use of Profiles.
        // Don't do anything.
    }
    
    @Override
    protected void loadSLK(File file) throws IOException {
        SLKFile slk = new SLKFile(file);
        Metadata<DestructableField> data = metadata.get(file.getName().substring(0, file.getName().lastIndexOf('.')));
        
        if (data == null) {
            return;
        }
        
        // Decide which values need to be loaded from the SLKs,
        // as we are only interested in the ones the map requests us to load.
        //
        ArrayList<List<DestructableField>> columnFields = new ArrayList<List<DestructableField>>(slk.getWidth());
        for (int i = 0; i < slk.getWidth(); i += 1) {
            columnFields.add(null);
        }
        
        Object[] firstRow = slk.getRow(0);
        for (int i = 1; i < firstRow.length; i += 1) { // skip the first column (i=0)
            Object cell = firstRow[i];
            if (cell != null) {
                columnFields.set(i, data.getFields(cell.toString()));
            }
        }
        
        // Now load those that were requested
        //
        for (int i = 1; i < slk.getHeight(); i += 1) {
            Object[] row = slk.getRow(i);
            
            Destructable destructable = getObject(CharInt.toInt(row[0].toString()));
            if (destructable == null) {
                continue;
            }
            
            for (int column = 1; column < slk.getWidth(); column += 1) {
                // Column headings might refer to multiple fields (identified uniquely by their field IDs)
                // Example: Buttonpos refers to ubpx and ubpy, in that order. Values are separated by commas.
                
                List<DestructableField> fields = columnFields.get(column);
                if (fields == null) {
                    continue;
                }
                
                String columnHeading = slk.getCell(column, 0).toString();
                Object value = row[column];
                
                for (DestructableField f : fields) {
                    if (value == null) {
                        destructable.addProperty(new Property<Destructable>(
                                f.getId(),
                                f.getType().getPropertyType(),
                                f.getType().getDefaultValue()));
                    } else {
                        destructable.addProperty(new Property<Destructable>(
                                f.getId(),
                                f.getType().getPropertyType(),
                                data.extractValue(columnHeading, f, value.toString())));
                    }
                }
            }
        }
    }
    
    @Override
    protected void loadFields() {
        addRawcodeMapEntry("bnam", "Name");
        addRawcodeMapEntry("bsuf", "EditorSuffix");
        addRawcodeMapEntry("bcat", "Category");
        addRawcodeMapEntry("btil", "Tilesets");
        addRawcodeMapEntry("btsp", "IsTilesetSpecific");
        addRawcodeMapEntry("bfil", "File");
        addRawcodeMapEntry("blit", "IsLightweight");
        addRawcodeMapEntry("bflo", "IsFatLOS");
        addRawcodeMapEntry("btxi", "TextureID");
        addRawcodeMapEntry("btxf", "TextureFile");
        addRawcodeMapEntry("buch", "UseClickHelper");
        addRawcodeMapEntry("bonc", "CanPlaceOnCliffs");
        addRawcodeMapEntry("bonw", "CanPlaceOnWater");
        addRawcodeMapEntry("bcpd", "CanPlaceDead");
        addRawcodeMapEntry("bwal", "IsWalkable");
        addRawcodeMapEntry("bclh", "CliffHeight");
        addRawcodeMapEntry("btar", "TargetType");
        addRawcodeMapEntry("barm", "Armor");
        addRawcodeMapEntry("bvar", "NumVar");
        addRawcodeMapEntry("bhps", "Health");
        addRawcodeMapEntry("boch", "OcclusionHeight");
        addRawcodeMapEntry("bflh", "FlyHeight");
        addRawcodeMapEntry("bfxr", "FixedRotation");
        addRawcodeMapEntry("bsel", "SelectionSize");
        addRawcodeMapEntry("bmis", "MinScale");
        addRawcodeMapEntry("bmas", "MaxScale");
        addRawcodeMapEntry("bcpr", "CanPlaceRandScale");
        addRawcodeMapEntry("bmap", "MaxPitch");
        addRawcodeMapEntry("bmar", "MaxRoll");
        addRawcodeMapEntry("brad", "Radius");
        addRawcodeMapEntry("bfra", "FogRadius");
        addRawcodeMapEntry("bfvi", "FogVisibility");
        addRawcodeMapEntry("bptx", "PathTexture");
        addRawcodeMapEntry("bptd", "PathTextureDeath");
        addRawcodeMapEntry("bdsn", "DeathSound");
        addRawcodeMapEntry("bshd", "Shadow");
        addRawcodeMapEntry("bsmm", "ShowInMM");
        addRawcodeMapEntry("bmmr", "MMRed");
        addRawcodeMapEntry("bmmg", "MMGreen");
        addRawcodeMapEntry("bmmb", "MMBlue");
        addRawcodeMapEntry("bumm", "UseMMColor");
        addRawcodeMapEntry("bbut", "BuildTime");
        addRawcodeMapEntry("bret", "RepairTime");
        addRawcodeMapEntry("breg", "GoldRepairCost");
        addRawcodeMapEntry("brel", "LumberRepairCost");
        addRawcodeMapEntry("busr", "UserList");
        addRawcodeMapEntry("bvcr", "ColorRed");
        addRawcodeMapEntry("bvcg", "ColorGreen");
        addRawcodeMapEntry("bvcb", "ColorBlue");
        addRawcodeMapEntry("bgse", "IsSelectable");
        addRawcodeMapEntry("bgsc", "SelectionCircleSize");
        addRawcodeMapEntry("bgpm", "PortraitModel");
    }
    
}
