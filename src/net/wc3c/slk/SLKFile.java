/**
 * 
 */
package net.wc3c.slk;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public final class SLKFile {
    private final BufferedReader slkReader;
    private int                  width  = 0;
    private int                  height = 0;
    private Object[][]           cells  = null;
    
    private static final String readFileToString(final String path, final Charset encoding) throws IOException {
        final byte[] encoded = Files.readAllBytes(Paths.get(path));
        return encoding.decode(ByteBuffer.wrap(encoded)).toString();
    }
    
    private final int convertNumber(final String num, final int lineNumber) {
        try {
            return Integer.parseInt(num) - 1;
        } catch (final NumberFormatException e) {
            throw new SLKParseException(null);
        }
    }
    
    private final Object parseValue(final String line, final int lineNumber) {
        if (line.startsWith("\"")) {
            // its a string
            return line.substring(1, line.length() - 1);
        } else {
            try {
                // try reading it as an Integer
                return Integer.valueOf(line);
            } catch (final NumberFormatException e) {
                // not an integer
                try {
                    // try reading as a float
                    return Float.valueOf(line);
                } catch (final NumberFormatException e2) {
                    // not a float
                    if (line.equalsIgnoreCase("true") || line.equalsIgnoreCase("false")) {
                        // see if its a boolean
                        return Boolean.valueOf(line);
                    } else {
                        return line;
                    }
                }
            }
        }
    }
    
    private final void recordID(final String[] fields, final int lineNumber) {
    }
    
    private final void recordB(final String[] fields, final int lineNumber) {
        int x = -1;
        int y = -1;
        
        for (int i = 1; i < fields.length; i += 1) {
            final String field = fields[i];
            switch (field.charAt(0)) {
                case 'X':
                    x = convertNumber(field.substring(1), lineNumber);
                    break;
                case 'Y':
                    y = convertNumber(field.substring(1), lineNumber);
                    break;
            }
        }
        
        if (x == -1 || y == -1) {
            throw new SLKParseException(null);
        }
        width = x + 1;
        height = y + 1;
        cells = new Object[height][width];
    }
    
    private int currentX = 0;
    private int currentY = 0;
    
    private final void recordC(final String[] fields, final int lineNumber) {
        int x = currentX;
        int y = currentY;
        Object value = null;
        
        for (int i = 1; i < fields.length; i += 1) {
            final String field = fields[i];
            switch (field.charAt(0)) {
                case 'X':
                    currentX = x = convertNumber(field.substring(1), lineNumber);
                    break;
                case 'Y':
                    currentY = y = convertNumber(field.substring(1), lineNumber);
                    break;
                case 'K':
                    value = parseValue(field.substring(1), lineNumber);
                    break;
            }
        }
        
        cells[y][x] = value;
    }
    
    private final void recordE(final String[] fields, final int lineNumber) {
    }
    
    private final void recordUnhandled(final String[] fields, final int lineNumber) {
    }
    
    private final void parse() throws IOException {
        String line = slkReader.readLine();
        int lineNumber = 1;
        while (line != null) {
            final String[] fields = line.split(";");
            switch (fields[0]) {
                case "ID":
                    recordID(fields, lineNumber);
                    break;
                
                case "B":
                    recordB(fields, lineNumber);
                    break;
                
                case "C":
                    recordC(fields, lineNumber);
                    break;
                
                case "P":
                case "F":
                case "O":
                case "NU":
                case "NE":
                case "NN":
                case "W":
                    recordUnhandled(fields, lineNumber);
                    break;
                
                case "E":
                    recordE(fields, lineNumber);
                    break;
            }
            
            lineNumber += 1;
            line = slkReader.readLine();
        }
    }
    
    /**
     * Returns the width of the SLK.
     * 
     * @return The number of columns of the SLK
     */
    public final int getWidth() {
        return width;
    }
    
    /**
     * Returns the height of the SLK.
     * 
     * @return The number of rows of the SLK.
     */
    public final int getHeight() {
        return height;
    }
    
    /**
     * Returns a read-only list of the specified row.
     * 
     * @param n Which row to get, 0 based.
     * @return Returns the <i>n</i><sup>th</sup> row of the SLK.
     * @throws IndexOutOfBoundsException.
     */
    public final Object[] getRow(final int n) throws IndexOutOfBoundsException {
        return cells[n];
    }
    
    /**
     * Returns the content of the specified cell.
     * 
     * @param x 0 based horizontal index.
     * @param y 0 based vertical index.
     * @return Returns the content of the cell of the SLK.
     * @throws IndexOutOfBoundsException.
     */
    public final Object getCell(final int x, final int y) throws IndexOutOfBoundsException {
        return cells[y][x];
    }
    
    /**
     * Opens and parses an SLK file, making its contents accessible and changeable
     * 
     * @param path The path to the SLK file you want to open.
     * @throws NullPointerException If the <i>path</i> parameter is null
     * @throws IOException If the <i>path</i> parameter points to a file you can't access, or can't create should the
     *             file not yet exist. This exception can also be raised should a general I/O error occur.
     * @throws SecurityException If a SecurityManager exists and denies read access to the file
     * 
     */
    public SLKFile(final File path) throws IOException {
        slkReader = new BufferedReader(new StringReader(readFileToString(path.getPath(), StandardCharsets.UTF_8)));
        parse();
    }
    
}
