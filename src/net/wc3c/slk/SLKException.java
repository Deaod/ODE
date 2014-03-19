/**
 * 
 */
package net.wc3c.slk;

/**
 * Indicates a general error in an SLK file.
 * 
 * @author Deaod
 * 
 */
public class SLKException extends RuntimeException {
    
    SLKException(String string) {
        super(string);
    }
    
    /**
     * 
     */
    private static final long serialVersionUID = 8477723935437345736L;
    
}
