package net.wc3c.ode;

import java.io.File;
import java.io.IOException;

import net.wc3c.w3o.W3AFile;
import net.wc3c.w3o.W3AFile.Ability;

public class AbilityDataExtractor extends Extractor<W3AFile, Ability> {
    
    protected AbilityDataExtractor(File enclosingFolder, W3AFile objectData) throws IOException {
        super(enclosingFolder, objectData);
    }
    
    @Override
    protected Field createField(Integer id, String name) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected LoadRequest createLoadRequest(String loadRequestLine) {
        // TODO Auto-generated method stub
        return null;
    }
    
    @Override
    protected FieldRequest createFieldRequest(Field field, LoadRequest request) {
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
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadDefaultObjects() throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadProfile(File file) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadSLK(File file) throws IOException {
        // TODO Auto-generated method stub
        
    }
    
    @Override
    protected void loadFields() {
        addRawcodeMapEntry("anam", "Name");
        // TODO
    }
    
}
