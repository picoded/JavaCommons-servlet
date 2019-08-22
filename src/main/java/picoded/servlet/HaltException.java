package picoded.servlet;

public class HaltException extends RuntimeException {
    
    public HaltException() {
        super();
    }
    
    public HaltException(String message) {
        super(message);
    }
}
