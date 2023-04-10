package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;
import dslab.workerAbstract.Worker;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * MailboxServer
 */
public class MailboxServer implements IMailboxServer, Runnable {

    // worker pools
    private final List<Worker> activeWorkers = Collections.synchronizedList(new ArrayList<>());
    // idle Threads steal work from others (when waiting for user input)
    private final ExecutorService dmapConnectionPool = Executors.newWorkStealingPool();
    // limited queue-size to receive mails and store in this DB
    private final ExecutorService dmtpConnectionPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Config propertiesReader;
    private final InboxRepository userInboxes;
    private final Shell shell;

    private boolean listenerShutdown = false;
    private ServerSocket dmapServerSocket;
    private ServerSocket dmtpServerSocket;

    public MailboxServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.propertiesReader = config;
        this.userInboxes = new InboxRepository(new Config(config.getString("users.config")));

        shell = new Shell(in, out);
        shell.register(this); //adds Methods annotated with @Command to Shells commands-List
        shell.setPrompt(componentId + "> ");
    }

    public static void main(String[] args) throws Exception {
        IMailboxServer server = ComponentFactory.createMailboxServer(args[0], System.in, System.out);
        server.run(); //doesn't start() Thread, just calls Method
    }

    @Override
    public void run() {
        // read ports
        int dmapPort = propertiesReader.getInt("dmap.tcp.port");
        int dmtpPort = propertiesReader.getInt("dmtp.tcp.port");

        // open sockets
        try {
            dmapServerSocket = new ServerSocket(dmapPort);
            dmtpServerSocket = new ServerSocket(dmtpPort);
        } catch (IOException e) {
            killConnection();
            throw new RuntimeException("unable to open ports " + dmapPort + ", " + dmtpPort, e);
        }

        // Threads with loops that create a worker for each connection accepted by client
        Thread dmapListener = new Thread(() -> {
            while (!listenerShutdown) {
                Socket newConnection;
                try {
                    newConnection = this.dmapServerSocket.accept(); // accept clients connection (blocking)
                } catch (IOException e) {
                    if (listenerShutdown) {
                        continue;
                    }
                    throw new RuntimeException("unable to accept client connection", e);
                }
                MailboxWorkerDMAP worker = new MailboxWorkerDMAP(newConnection, activeWorkers, userInboxes, propertiesReader); // <---------------------------------
                dmapConnectionPool.execute(worker);
            }
        });

        Thread dmtpListener = new Thread(() -> {
            while (!listenerShutdown) {
                Socket newConnection;
                try {
                    newConnection = this.dmtpServerSocket.accept(); // accept clients connection (blocking)
                } catch (IOException e) {
                    if (listenerShutdown) {
                        continue;
                    }
                    throw new RuntimeException("unable to accept client connection", e);
                }
                MailboxWorkerDMTP worker = new MailboxWorkerDMTP(newConnection, activeWorkers, userInboxes, propertiesReader); // <---------------------------------
                dmtpConnectionPool.execute(worker);
            }
        });

        // start loop Threads
        dmapListener.start();
        dmtpListener.start();
        shell.run();

        // shutdown() if not already shut down
        if (!listenerShutdown) {
            this.shutdown();
            try {
                this.shutdown();
            } catch (StopShellException ignored) {
            }
        }

        // deadlock avoidance
        dmapListener.interrupt();
        dmtpListener.interrupt();
    }

    @Override
    @Command
    public void shutdown() {
        killConnection();
        listenerShutdown = true;
        dmapConnectionPool.shutdown();
        dmtpConnectionPool.shutdown();
        activeWorkers.stream().unordered().forEach(Worker::quit);
        throw new StopShellException(); //kill shell
    }

    public void killConnection() {
        try {
            if (this.dmapServerSocket != null) {
                this.dmapServerSocket.close();
            }
            if (this.dmtpServerSocket != null) {
                this.dmtpServerSocket.close();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to close server", e);
        }
    }
}