package net.wc3c.w3o;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

import net.wc3c.util.BufferedDataChannel;
import net.wc3c.w3o.W3TFile.Item;
import net.wc3c.wts.WTSFile;

public class W3TFile extends W3OBase<Item> {
    public W3TFile(Path source, WTSFile trigStrs) throws IOException {
        super(source, trigStrs);
    }
    
    public W3TFile(String sourcePath, WTSFile trigStrs) throws IOException {
        super(sourcePath, trigStrs);
    }
    
    public W3TFile(Path source) throws IOException {
        super(source);
    }
    
    public W3TFile(String sourcePath) throws IOException {
        super(sourcePath);
    }
    
    public W3TFile(WTSFile trigStrs) {
        super(trigStrs);
    }
    
    public W3TFile() {
        super();
    }
    
    /**
     * Returns a read-only view on the items contained within this W3T file.
     * 
     * @return a read-only view on all items in this file.
     */
    public Collection<Item> getItems() {
        return getEntries();
    }
    
    public void addItem(Item item) {
        item.setContext(this);
        addEntry(item);
    }
    
    public Item getItem(int itemId) {
        return getEntry(itemId);
    }
    
    @Override
    protected Item readEntry(BufferedDataChannel dc) throws IOException {
        return new Item(dc, this);
    }
    
    public static class Item extends W3Object<W3TFile> {
        @Override
        protected Property<?> readProperty(BufferedDataChannel dc) throws IOException {
            return new Property<Item>(dc, this);
        }
        
        @SuppressWarnings("unchecked")
        public Property<Item> getProperty(int fieldId) {
            return (Property<Item>) super.getProperty(Property.generateKey(fieldId));
        }
        
        @SuppressWarnings("unchecked")
        public Property<Item> getPropertyEx(int fieldId) {
            return (Property<Item>) super.getPropertyEx(Property.generateKey(fieldId));
        }
        
        public boolean hasProperty(int fieldId) {
            return super.hasProperty(Property.generateKey(fieldId));
        }
        
        public void addProperty(Property<Item> property) {
            if (hasProperty(property.getFieldId())) {
                return;
            }
            
            property.setContext(this);
            putProperty(property.generateKey(), property);
        }
        
        Item(BufferedDataChannel dc, W3TFile context) throws IOException {
            super(dc, context);
        }
        
        public Item(int parentId, int id) {
            super(parentId, id);
        }
        
        public Item(int id) {
            super(id);
        }
    }
}
