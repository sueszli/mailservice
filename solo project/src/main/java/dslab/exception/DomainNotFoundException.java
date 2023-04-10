package dslab.exception;

/**
 * Thrown when the domain of a mail-address was not found during its transfer.
 * <p>
 * Would technically be a runtime-exception, but here it's used to communicate an event.
 */
public class DomainNotFoundException extends Exception {
}
