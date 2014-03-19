package net.wc3c.ode;

import static net.wc3c.util.Jass.call;
import static net.wc3c.util.Jass.endBranch;
import static net.wc3c.util.Jass.endfunction;
import static net.wc3c.util.Jass.endglobals;
import static net.wc3c.util.Jass.endlibrary;
import static net.wc3c.util.Jass.execute;
import static net.wc3c.util.Jass.function;
import static net.wc3c.util.Jass.globalVar;
import static net.wc3c.util.Jass.globals;
import static net.wc3c.util.Jass.ifBranch;
import static net.wc3c.util.Jass.library;
import static net.wc3c.util.Jass.ret;
import static net.wc3c.util.Jass.set;
import static net.wc3c.util.Jass.Type.BOOLEAN;
import static net.wc3c.util.Jass.Type.HASHTABLE;
import static net.wc3c.util.Jass.Type.NOTHING;
import static net.wc3c.util.Jass.Visibility.PRIVATE;
import static net.wc3c.util.Log.info;
import static net.wc3c.util.Log.trace;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.wc3c.util.CharInt;
import net.wc3c.util.Jass;
import net.wc3c.util.SequentialIntGenerator;
import net.wc3c.w3o.Property;
import net.wc3c.w3o.PropertyType;
import net.wc3c.w3o.W3OBase;
import net.wc3c.w3o.W3Object;

public abstract class Extractor<Data extends W3OBase<Type>, Type extends W3Object<Data>> {
    
    private static final String      LS             = System.getProperty("line.separator");
    
    private static final String      MINIMAL_INIT   = "MinimalInit";
    private static final String      DATA_PATH      = "data";
    private static final String      META_DATA_PATH = "meta";
    protected static final String    HASHTABLE_NAME = "info";
    
    private List<Field>              invalidFields  = new ArrayList<Field>();
    private Map<Integer, Field>      fields         = new Hashtable<Integer, Field>();
    private Map<Field, FieldRequest> fieldRequests  = new Hashtable<Field, FieldRequest>();
    
    private File                     dataPath;
    private File                     metaPath;
    private Data                     objectData;
    private SequentialIntGenerator   fieldIdGen     = new SequentialIntGenerator();
    
    protected abstract String getCommandString();
    
    protected abstract String getLibraryNameString();
    
    protected abstract String getLibraryInitializerString();
    
    protected abstract void loadFields();
    
    protected abstract void loadMetadata() throws IOException;
    
    protected abstract void loadDefaultObjects() throws IOException;
    
    protected abstract void loadProfile(File file) throws IOException;
    
    protected abstract void loadSLK(File file) throws IOException;
    
    protected abstract Field createField(Integer id, String name);
    
    protected abstract LoadRequest createLoadRequest(String loadRequestLine);
    
    protected abstract FieldRequest createFieldRequest(Field field, LoadRequest request);
    
    protected static enum FieldType {
        INTEGER("Integer", Jass.Type.INTEGER, PropertyType.INTEGER, 0),
        REAL("Real", Jass.Type.REAL, PropertyType.REAL, 0.0f),
        STRING("Str", Jass.Type.STRING, PropertyType.STRING, ""),
        BOOLEAN("Boolean", Jass.Type.BOOLEAN, PropertyType.BOOLEAN, false);
        
        private String       functionName;
        private Jass.Type    jassType;
        private PropertyType propertyType;
        private Object       defaultValue;
        
        public final String loadFunctionName() {
            return "Load" + this.functionName;
        }
        
        public final String saveFunctionName() {
            return "Save" + this.functionName;
        }
        
        public final Jass.Type getJassType() {
            return this.jassType;
        }
        
        public final PropertyType getPropertyType() {
            return this.propertyType;
        }
        
        public final Object getDefaultValue() {
            return this.defaultValue;
        }
        
        private FieldType(String functionName, Jass.Type jassType, PropertyType propertyType, Object defaultValue) {
            this.functionName = functionName;
            this.jassType = jassType;
            this.propertyType = propertyType;
            this.defaultValue = defaultValue;
        }
        
        public final FieldType fromString(String str) {
            if (str.equalsIgnoreCase("int")) {
                return FieldType.INTEGER;
            } else if (str.equalsIgnoreCase("bool")) {
                return FieldType.BOOLEAN;
            } else if (str.equalsIgnoreCase("real") || str.equalsIgnoreCase("unreal")) {
                return FieldType.REAL;
            } else {
                return FieldType.STRING;
            }
        }
    }
    
    protected abstract class Field {
        private int       id;
        private String    name;
        private FieldType type;
        private int       index;
        
        public Field(Integer id, String name) {
            this.id = id;
            this.name = name;
        }
        
        public final void setIndex(int index) {
            this.index = index;
        }
        
        public final void setType(FieldType type) {
            this.type = type;
        }
        
        public abstract StringBuilder generateLoadFunction();
        
        private String jassifyValue(Object value) {
            if (value == null) {
                if (getType() == FieldType.STRING) {
                    return "\"\"";
                } else {
                    return getType().getDefaultValue().toString();
                }
            }
            
            switch (getType()) {
                default:
                case STRING: {
                    String tmp;
                    if (value.toString().equals("_") || value.toString().equals("-")) {
                        tmp = getType().getDefaultValue().toString();
                    } else {
                        tmp = value.toString();
                    }
                    // surround with " and replace \ with \\
                    // take a good long look at these patterns. This is what insanity looks like in its purest form.
                    return "\"" + tmp.replaceAll("\\\\", "\\\\\\\\") + "\"";
                }
                
                case INTEGER:
                    try {
                        return Integer.valueOf(value.toString()).toString();
                    } catch (NumberFormatException e) {
                        return getType().getDefaultValue().toString();
                    }
                    
                case REAL:
                    try {
                        return Float.valueOf(value.toString()).toString();
                    } catch (NumberFormatException e) {
                        return getType().getDefaultValue().toString();
                    }
                    
                case BOOLEAN:
                    if (value.toString().equals("1") || value.toString().equals("true")) {
                        return "true";
                    } else {
                        return getType().getDefaultValue().toString();
                    }
            }
        }
        
        public final String generateSaveFunctionCall(String index1, String index2, Object value) {
            return call(getType().saveFunctionName(), HASHTABLE_NAME, index1, index2, jassifyValue(value));
        }
        
        public abstract String generateSaveFunctionCall(Type object, Property<Type> property);
        
        public final int getId() {
            return this.id;
        }
        
        public final String getName() {
            return this.name;
        }
        
        public final FieldType getType() {
            return this.type;
        }
        
        public final int getIndex() {
            return this.index;
        }
        
        public final Long generateKey() {
            return (long) this.id;
        }
    }
    
    protected abstract class FieldRequest {
        private boolean initialize   = true;
        private boolean loadDefaults = false;
        
        private Field   field;
        
        public void absorb(LoadRequest request) {
            this.initialize = this.initialize || request.initialize;
            this.loadDefaults = this.loadDefaults || request.loadDefaults;
        }
        
        public final boolean isLoadDefaults() {
            return this.loadDefaults;
        }
        
        protected abstract boolean canLoad(Type object);
        
        private StringBuilder generateInitializer(Collection<Type> data) {
            StringBuilder code = new StringBuilder();
            String initVarName = "didInitialize" + this.field.getId();
            
            code.append(globals());
            code.append(globalVar(PRIVATE, BOOLEAN, initVarName, "false"));
            code.append(endglobals());
            
            code.append(function(PRIVATE, "Init" + this.field.name, NOTHING));
            code.append(ifBranch(initVarName));
            code.append(ret());
            code.append(endBranch());
            code.append(set(initVarName, "true"));
            
            // just dump data for every unit in here
            // i doubt any map has more than 10000 modified objects of a single type (unit, item, destructable, ...).
            // 10000 was the original limit of consecutive loads to avoid crashing the thread for loading.
            
            for (Type object : data) {
                if (canLoad(object)) {
                    @SuppressWarnings("unchecked")
                    Property<Type> property = (Property<Type>) object.getProperty(this.field.generateKey());
                    
                    if (property != null) {
                        code.append(this.field.generateSaveFunctionCall(object, property));
                    }
                }
            }
            
            code.append(endfunction());
            
            return code;
        }
        
        protected void initialize(LoadRequest request) {
            this.initialize = request.initialize;
            this.loadDefaults = request.loadDefaults;
        }
        
        public FieldRequest(Field field, LoadRequest request) {
            this.field = field;
            
            initialize(request);
        }
    }
    
    protected abstract class LoadRequest {
        private boolean     initialize      = true;
        private boolean     loadDefaults    = false;
        
        private List<Field> requestedFields = new ArrayList<Field>();
        
        public LoadRequest(String[] words) {
            for (String word : words) {
                if (word.toLowerCase().equals("-noinit")) {
                    this.initialize = false;
                } else if (word.toLowerCase().equals("-defaults")) {
                    this.loadDefaults = true;
                } else if (word.toLowerCase().startsWith("fields=")) {
                    String fieldList[] = word.substring(7).split(",");
                    for (String field : fieldList) {
                        Field f = Extractor.this.fields.get(CharInt.toInt(field));
                        if (f != null) {
                            this.requestedFields.add(f);
                        }
                    }
                }
            }
            
        }
        
        public List<Field> getRequestedFields() {
            return Collections.unmodifiableList(this.requestedFields);
        }
    }
    
    private final void addToInvalidFields(Field f) {
        this.invalidFields.add(f);
    }
    
    protected final void removeFromInvalidFields(Field f) {
        this.invalidFields.remove(f);
    }
    
    private final void removeInvalidFields() {
        for (Field f : this.invalidFields) {
            this.fields.remove(f);
        }
    }
    
    protected final void addRawcodeMapEntry(String id, String name) {
        Field f = createField(CharInt.toInt(id), name);
        f.setIndex(this.fieldIdGen.next());
        this.fields.put(CharInt.toInt(id), f);
        addToInvalidFields(f);
    }
    
    private StringBuilder processLibrary(String line) {
        StringBuilder out = new StringBuilder();
        String[] words = line.split("\\s+");
        boolean hasReq = false;
        boolean isReq = false;
        
        for (String word : words) {
            if (hasReq) {
                if (word.startsWith(getLibraryNameString() + ",")
                        || word.equals(getLibraryNameString())
                        || word.endsWith("," + getLibraryNameString())
                        || word.contains("," + getLibraryNameString() + ",")) {
                    isReq = true;
                }
            } else if (word.equals("requires") || word.equals("uses") || word.equals("needs")) {
                hasReq = true;
            }
        }
        
        out.append(line);
        if (!hasReq) {
            out.append(" requires " + getLibraryNameString());
        } else if (!isReq) {
            out.append("," + getLibraryNameString());
        }
        out.append(LS);
        
        return out;
    }
    
    private void mergeLoadRequestIntoExistingRequests(LoadRequest request) {
        for (Field field : request.getRequestedFields()) {
            FieldRequest fieldRequest = this.fieldRequests.get(field);
            if (fieldRequest == null) {
                fieldRequest = createFieldRequest(field, request);
                this.fieldRequests.put(field, fieldRequest);
            } else {
                fieldRequest.absorb(request);
            }
        }
    }
    
    private StringBuilder generateJASSCode() {
        StringBuilder out = new StringBuilder();
        
        out.append(library(getLibraryNameString(), MINIMAL_INIT));
        out.append(globals());
        out.append(globalVar(PRIVATE, HASHTABLE, HASHTABLE_NAME, "InitHashtable()"));
        out.append(globalVar(PRIVATE, BOOLEAN, "alreadyRan", "false"));
        out.append(endglobals());
        
        for (FieldRequest request : this.fieldRequests.values()) {
            out.append(request.field.generateLoadFunction());
            out.append(request.generateInitializer(this.objectData.getEntries()));
        }
        
        out.append(function(getLibraryInitializerString(), NOTHING));
        out.append(ifBranch("alreadyRan"));
        out.append(ret());
        out.append(endBranch());
        out.append(set("alreadyRan", "true"));
        for (FieldRequest request : this.fieldRequests.values()) {
            out.append(execute("SCOPE_PRIVATE+\"Init" + request.field.name + "\""));
        }
        out.append(endfunction());
        
        out.append(function(PRIVATE, MINIMAL_INIT, NOTHING));
        for (FieldRequest request : this.fieldRequests.values()) {
            if (request.initialize) {
                out.append(execute("SCOPE_PRIVATE+\"Init" + request.field.name + "\""));
            }
        }
        out.append(endfunction());
        out.append(endlibrary());
        
        return out;
    }
    
    private void loadProfiles() throws IOException {
        File profile = new File(this.dataPath, "profile");
        
        for (File fi : profile.listFiles()) {
            loadProfile(fi);
        }
        
    }
    
    private void loadSlks() throws IOException {
        for (File f : this.dataPath.listFiles()) {
            // make sure we only load files ending with .slk
            if (f.isFile() && f.getName().endsWith(".slk")) {
                loadSLK(f);
            }
        }
    }
    
    protected final File getDataPath() {
        return this.dataPath;
    }
    
    protected final File getMetaPath() {
        return this.metaPath;
    }
    
    protected final Field getField(int id) {
        return this.fields.get(id);
    }
    
    protected final void addObject(Type object) {
        this.objectData.addEntry(object);
    }
    
    protected final Type getObject(int id) {
        return this.objectData.getEntry(id);
    }
    
    public final String processScript(String scriptContent) throws IOException {
        trace("Entering Extractor.processScript(String)");
        BufferedReader br = new BufferedReader(new StringReader(scriptContent));
        StringBuilder out = new StringBuilder();
        List<String> loadRequestLines = new LinkedList<String>();
        
        info("Processing script content");
        
        String currentLine = br.readLine();
        while (currentLine != null) {
            String rls = currentLine.trim();
            
            if (rls.startsWith("//! ")) {
                String[] words = rls.split("\\s+");
                
                if (words.length >= 2) {
                    String normalizedRequest = words[1].toLowerCase(Locale.ENGLISH);
                    String normalizedCommand = getCommandString().toLowerCase(Locale.ENGLISH);
                    
                    if (normalizedRequest.equals(normalizedCommand)) {
                        loadRequestLines.add(rls);
                    } else {
                        out.append(currentLine).append(LS);
                    }
                } else {
                    out.append(currentLine).append(LS);
                }
            } else if (rls.startsWith("library")) {
                out.append(processLibrary(currentLine));
            } else {
                out.append(currentLine).append(LS);
            }
            currentLine = br.readLine();
        }
        br.close();
        
        info("Loading fields");
        loadFields();
        info("Loading meta data");
        loadMetadata();
        removeInvalidFields();
        
        info("Processing load request lines");
        for (String loadRequestLine : loadRequestLines) {
            mergeLoadRequestIntoExistingRequests(createLoadRequest(loadRequestLine));
        }
        
        if (this.fieldRequests.size() > 0) {
            info("Loading default object");
            loadDefaultObjects();
            info("Loading profiles");
            loadProfiles();
            info("Loading slks");
            loadSlks();
            
            info("Generating JASS code");
            out.append(generateJASSCode());
            
            trace("Leaving Extractor.processScript(String) [changed]");
            return out.toString();
        }
        
        trace("Leaving Extractor.processScript(String) [unchanged]");
        return scriptContent;
    }
    
    protected Extractor(File enclosingFolder, Data objectData) throws IOException {
        this.dataPath = new File(enclosingFolder, DATA_PATH);
        this.metaPath = new File(this.dataPath, META_DATA_PATH);
        this.objectData = objectData;
    }
}
