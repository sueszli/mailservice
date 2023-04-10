package dslab.transfer;

/**
 * Interface for the TransferServer
 */
public interface ITransferServer extends Runnable {

    @Override
    void run();

    void shutdown();
}
