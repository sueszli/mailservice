package dslab;

import dslab.mailbox.IMailboxServer;
import dslab.mailbox.MailboxServer;
import dslab.monitoring.IMonitoringServer;
import dslab.monitoring.MonitoringServer;
import dslab.transfer.ITransferServer;
import dslab.transfer.TransferServer;
import dslab.util.Config;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * The component factory provides methods to create the core components of the application. You can edit the method body
 * if the component instantiation requires additional logic.
 * <p>
 * Do not change the existing method signatures!
 */
public final class ComponentFactory {

    private ComponentFactory() {
    }

    public static IMonitoringServer createMonitoringServer(String componentId, InputStream in, PrintStream out) throws Exception {
        Config config = new Config(componentId);
        return new MonitoringServer(componentId, config, in, out);
    }

    public static IMailboxServer createMailboxServer(String componentId, InputStream in, PrintStream out) throws Exception {
        Config config = new Config(componentId);
        return new MailboxServer(componentId, config, in, out);
    }

    public static ITransferServer createTransferServer(String componentId, InputStream in, PrintStream out) throws Exception {
        Config config = new Config(componentId);
        return new TransferServer(componentId, config, in, out);
    }

}
