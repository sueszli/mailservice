package dslab.transfer;


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
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * TransferServer
 */
public class TransferServer implements ITransferServer, Runnable {

    // pool and list for TransferWorkerDMTPs (for each connected client)
    private final List<Worker> activeWorkers = Collections.synchronizedList(new ArrayList<>());
    // idle Threads steal work from others (when waiting for user input)
    private final ExecutorService connectionPool = Executors.newWorkStealingPool();

    // pool and list for ForwardWorker called by TransferWorkerDMTP
    private final List<ForwardWorker> activeForwardWorkers = Collections.synchronizedList(new ArrayList<>());
    // limited queue-size to replay mails to MailBoxes
    private final Executor forwardPool = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private final Config config;
    private final Shell shell;
    private ServerSocket serverSocket;
    private boolean listenerShutdown = false;

    public TransferServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    public static void main(String[] args) throws Exception {
        ITransferServer server = ComponentFactory.createTransferServer(args[0], System.in, System.out);
        server.run();
    }

    @Override
    public void run() {
        // Listen to Port
        int port = config.getInt("tcp.port");
        try {
            this.serverSocket = new ServerSocket(port);
        } catch (IOException e) {
            throw new RuntimeException("unable to open port " + port, e);
        }

        // Run pool to create Worker for each client that connects
        Thread listener = new Thread(() -> {
            while (!listenerShutdown) {
                Socket newConnection;
                try {
                    newConnection = this.serverSocket.accept();
                } catch (IOException e) {
                    if (listenerShutdown) {
                        continue;
                    }
                    throw new RuntimeException("unable to accept client connection", e);
                }

                connectionPool.execute(new TransferWorkerDMTP(
                        newConnection, activeWorkers, forwardPool, activeForwardWorkers, config, serverSocket // <---------------------------------------------
                ));
            }
        });
        listener.start();

        shell.run();
        if (!listenerShutdown) {
            shutdown();
            try {
                shutdown();
            } catch (StopShellException ignored) {
            }
        }

        // deadlock avoidance
        listener.interrupt();

        System.out.println("Server stopped.");
    }

    @Override
    @Command
    public void shutdown() {
        killConnection();
        listenerShutdown = true;
        connectionPool.shutdownNow();
        activeWorkers.forEach(Worker::quit);
        activeForwardWorkers.forEach(ForwardWorker::killConnection);
        throw new StopShellException();
    }

    @Command
    public String connStatus() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) connectionPool;
        return
                "------\n" +
                        "Threadpool for request execution \n" +
                        "ActiveThreads: " + executor.getActiveCount() + " \n" +
                        "Queue: " + executor.getQueue().size() + " \n" +
                        "PoolSize " + executor.getPoolSize() + " \n" +
                        "-----"
                ;
    }

    @Command
    public String forwardStatus() {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) forwardPool;
        return
                "------\n" +
                        "Threadpool for email forwarding\n" +
                        "MaxThreads: " + executor.getMaximumPoolSize() + " \n" +
                        "ActiveThreads: " + executor.getActiveCount() + " \n" +
                        "Queue: " + executor.getQueue().size() + " \n" +
                        "PoolSize " + executor.getPoolSize() + " \n" +
                        "-----"
                ;
    }

    @Command
    public void setMaxForwardThreads(int count) {
        ThreadPoolExecutor executor = (ThreadPoolExecutor) forwardPool;
        executor.setMaximumPoolSize(count);
    }

    public void killConnection() {
        try {
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to close server", e);
        }
    }
}