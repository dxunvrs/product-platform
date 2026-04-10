package exceptions;

public class AuthExpiredException extends RuntimeException {
    public AuthExpiredException(String message) {
        super(message);
    }
}
