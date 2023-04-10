package dslab.mailbox;

/**
 * Interface for the Mailbox Server
 */
public interface IMailboxServer extends Runnable {

    @Override
    void run();

    void shutdown();

}
