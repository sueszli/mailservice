package dslab.exception;

/**
 * Thrown for an invalid input of a client.
 * Will always get caught by the parser and printed onto the shell.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
