package dslab.exception;

/**
 * Only thrown when replaying a message to a Mailbox from the TransferServer failed (mailbox response was not 'ok').
 * Then a failure mail must be sent to the sender of the mail.
 */
public class InvalidRequestException extends Exception {
}
