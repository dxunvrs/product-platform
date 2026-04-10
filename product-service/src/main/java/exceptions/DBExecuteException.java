package exceptions;

public class DBExecuteException extends RuntimeException {
    public DBExecuteException(String message) {
        super(message);
    }
}
