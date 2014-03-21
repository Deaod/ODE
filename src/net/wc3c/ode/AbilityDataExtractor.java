package net.wc3c.ode;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

import net.wc3c.slk.SLKFile;
import net.wc3c.util.CharInt;
import net.wc3c.util.Jass;
import net.wc3c.util.Jass.Parameter;
import net.wc3c.util.Jass.Type;
import net.wc3c.w3o.LevelProperty;
import net.wc3c.w3o.Property;
import net.wc3c.w3o.W3AFile;
import net.wc3c.w3o.W3AFile.Ability;

public class AbilityDataExtractor extends Extractor<W3AFile, Ability> {
    private static final int                                FIELD_ID_DEFAULT_ABILITY    = CharInt.toInt("defa");
    private static final int                                FIELD_ID_RACE               = CharInt.toInt("arac");
    private static final int                                COLUMN_ABILITY_RACE         = 8;
    private static final int                                MAX_DATA_FIELDS_PER_ABILITY = 16;
    
    private final Hashtable<String, Metadata<AbilityField>> metadata                    = new Hashtable<>();
    
    protected AbilityDataExtractor(final File enclosingFolder, final W3AFile objectData) throws IOException {
        super(enclosingFolder, objectData);
    }
    
    private class AbilityField extends Field {
        private boolean             usingLevels         = false;
        private boolean             restricted          = false;
        private boolean             restrictionInverted = false;
        private Map<Integer, int[]> abilities           = null;
        
        public AbilityField(final Integer id, final String name) {
            super(id, name);
        }
        
        public boolean isRestrictionInverted() {
            return restrictionInverted;
        }
        
        public void setRestrictionInverted(final boolean restrictionInverted) {
            this.restrictionInverted = restrictionInverted;
        }
        
        public boolean isRestricted() {
            return restricted;
        }
        
        public void setRestricted(final boolean restricted) {
            if (restricted && abilities == null) {
                abilities = new Hashtable<>();
            }
            this.restricted = restricted;
        }
        
        public boolean isUsingLevels() {
            return usingLevels;
        }
        
        public void setUsingLevels(final boolean usingLevels) {
            this.usingLevels = usingLevels;
        }
        
        @Override
        public StringBuilder generateLoadFunction() {
            final StringBuilder out = new StringBuilder();
            
            out.append(Jass.function("GetAbilityType" + getName(), getType().getJassType(), new Parameter(
                    Type.INTEGER,
                    "id")));
            out.append(Jass.ret(getType().loadFunctionName() + "(" + HASHTABLE_NAME + ",id," + getIndex() + ")"));
            out.append(Jass.endfunction());
            
            return out;
        }
        
        @Override
        public String generateSaveFunctionCall(final Ability object, final Property<Ability> property) {
            return generateSaveFunctionCall(
                    "'" + CharInt.toString(object.getId()) + "'",
                    Integer.toString(getIndex()),
                    property.getValue());
        }
        
    }
    
    private class AbilityFieldRequest extends FieldRequest {
        private boolean            restrictToRaces;
        
        private final Set<String>  races;
        private final Set<Integer> abilityIdsToLoad;
        
        public AbilityFieldRequest(final AbilityField field, final AbilityLoadRequest request) {
            super(field, request);
            
            restrictToRaces = request.isRestrictedToRaces();
            races = request.getRaces();
            abilityIdsToLoad = request.getAbilityIdsToLoad();
        }
        
        @Override
        public void absorb(final LoadRequest request) {
            super.absorb(request);
            
            final AbilityLoadRequest req = (AbilityLoadRequest) request;
            
            restrictToRaces = restrictToRaces && req.isRestrictedToRaces();
            races.addAll(req.getRaces());
            abilityIdsToLoad.addAll(req.getAbilityIdsToLoad());
        }
        
        @Override
        protected boolean canLoad(final Ability object) {
            boolean load = false;
            
            if (isLoadDefaults()) {
                if (restrictToRaces) {
                    final LevelProperty<Ability> property = object.getProperty(FIELD_ID_RACE, 0, 0);
                    if (property != null && races.contains(property.getValue())) {
                        load = true;
                    } else if (abilityIdsToLoad.contains(object.getId())) {
                        load = true;
                    } else {
                        load = false;
                    }
                } else {
                    load = true;
                }
            }
            
            if (object.getPropertyEx(FIELD_ID_DEFAULT_ABILITY, 0, 0) == null) {
                load = true;
            }
            
            return load;
        }
    }
    
    private class AbilityLoadRequest extends LoadRequest {
        private boolean            restrictToRaces  = false;
        
        private final Set<String>  races            = new HashSet<String>();
        private final Set<Integer> abilityIdsToLoad = new HashSet<Integer>();
        
        public AbilityLoadRequest(final String[] words) {
            super(words);
            
            for (final String word : words) {
                if (word.toLowerCase().startsWith("races=")) {
                    restrictToRaces = true;
                    final String[] races = word.substring(6).split(",");
                    for (final String race : races) {
                        this.races.add(race);
                    }
                } else if (word.toLowerCase().startsWith("rawcodes=")) {
                    final String[] rawcodes = word.substring(9).split(",");
                    for (final String rawcode : rawcodes) {
                        abilityIdsToLoad.add(CharInt.toInt(rawcode));
                    }
                }
            }
        }
        
        public boolean isRestrictedToRaces() {
            return restrictToRaces;
        }
        
        public Set<String> getRaces() {
            return races;
        }
        
        public Set<Integer> getAbilityIdsToLoad() {
            return abilityIdsToLoad;
        }
    }
    
    @Override
    protected Field createField(final Integer id, final String name) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected LoadRequest createLoadRequest(final String loadRequestLine) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected FieldRequest createFieldRequest(final Field field, final LoadRequest request) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected String getCommandString() {
        return "LoadAbilityData";
    }
    
    @Override
    protected String getLibraryNameString() {
        return "ExportedAbilityData";
    }
    
    @Override
    protected String getLibraryInitializerString() {
        return "InitExportedAbilityData";
    }
    
    @Override
    protected void loadMetadata() throws IOException {
        final SLKFile slk = new SLKFile(new File(getMetaPath(), "AbilityMetaData.slk"));
        
        for (int row = 1; row < slk.getHeight(); row += 1) {
            final String fieldIdString = slk.getCell(0, row).toString();
            final int fieldId = CharInt.toInt(fieldIdString);
            final String name = slk.getCell(1, row).toString();
            final String slkName = slk.getCell(2, row).toString();
            final String index = slk.getCell(3, row).toString();
            final String repeat = slk.getCell(4, row).toString();
            final String data = slk.getCell(5, row).toString();
            final String type = slk.getCell(9, row).toString();
            final String specific = slk.getCell(22, row).toString();
            final String notSpecific = slk.getCell(23, row).toString();
            
            final AbilityField f;
            if (name.equals("Data")) {
                f = (AbilityField) getField(CharInt.toInt("adat"));
            } else if (name.equals("UnitID")) {
                f = (AbilityField) getField(CharInt.toInt("auid"));
            } else {
                f = (AbilityField) getField(fieldId);
            }
            
            f.setUsingLevels(Integer.parseInt(repeat) > 0);
            f.setRestricted((specific.length() > 0) || (notSpecific.length() > 0));
            f.setRestrictionInverted(notSpecific.length() > 0);
            
            final int dataIndex = Integer.parseInt(data);
            final String[] abilityIds = specific.split(",");
            
            for (final String abilityId : abilityIds) {
                final int id = CharInt.toInt(abilityId);
                int[] dataFields = f.abilities.get(id);
                if (dataFields == null) {
                    dataFields = new int[MAX_DATA_FIELDS_PER_ABILITY];
                    f.abilities.put(id, dataFields);
                }
                
                dataFields[dataIndex] = fieldId;
            }
            
            if (f != null) {
                removeFromInvalidFields(f);
                
                Metadata<AbilityField> m = metadata.get(slkName);
                if (m == null) {
                    m = new Metadata<AbilityField>();
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
        
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadDefaultObjects() throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadProfile(final File file) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadSLK(final File file) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadFields() {
        addRawcodeMapEntry("anam", "Name");
        // TODO
    }
    
}
