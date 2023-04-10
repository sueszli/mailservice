package dslab.monitoring;

/**
 * Interface for the MonitoringServer
 */
public interface IMonitoringServer extends Runnable {
    // changed return type of all CLI commands to String

    @Override
    void run();

    String shutdown();

    String servers();

    String addresses();

}
