package net.wc3c.util;

import java.nio.charset.StandardCharsets;

public final class CharInt {
    private CharInt() {
    }
    
    /**
     * Convert a four character string into an integer.<br>
     * Do not use characters outside the ASCII character set.
     * 
     * @param id the string to convert
     * @return integer representation of the string.
     */
    public final static int toInt(String id) {
        byte[] bytes = id.getBytes(StandardCharsets.US_ASCII);
        int result = 0;
        if (bytes.length >= 4) {
            result |= (bytes[3] << 0) & 0x000000FF;
            result |= (bytes[2] << 8) & 0x0000FF00;
            result |= (bytes[1] << 16) & 0x00FF0000;
            result |= (bytes[0] << 24) & 0xFF000000;
        } else {
            for (int i = 0; i < bytes.length; i++) {
                result |= (bytes[i] << (24 - (i << 3)));
            }
        }
        
        return result;
    }
    
    /**
     * Convert an integer into a four character string.
     * 
     * @param id the integer to convert
     * @return four character string representing the integer.
     */
    public final static String toString(int id) {
        StringBuffer result = new StringBuffer(4);
        
        result.append((char) ((id >> 24) & 0xFF));
        result.append((char) ((id >> 16) & 0xFF));
        result.append((char) ((id >> 8) & 0xFF));
        result.append((char) ((id >> 0) & 0xFF));
        
        return result.toString();
    }
}
