package net.wc3c.util;

public class SequentialIntGenerator {
    private int index;
    private int count = 0;
    
    public SequentialIntGenerator(int startingIndex) {
        this.index = startingIndex;
    }
    
    public SequentialIntGenerator() {
        this(0);
    }
    
    public int next() {
        int result = this.index;
        this.index += 1;
        this.count += 1;
        return result;
    }
    
    public int getGeneratedCount() {
        return this.count;
    }
}
