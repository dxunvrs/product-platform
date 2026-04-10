package exceptions;

public class InvalidAuthorizeException extends RuntimeException {
    public InvalidAuthorizeException(String message) {
        super(message);
    }
}
