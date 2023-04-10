package dslab.workerAbstract;

import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.util.SocketIO;
import dslab.util.orvell_V2.Shell_V2;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

/**
 * Each Worker is a Runnable responsible for a single client-connection via TCP and uses the Shell_V2.class
 * It adds and removes itself from the activeWorkers List.
 */
public abstract class Worker implements Runnable {

    private final Socket clientSocket;
    private final List<Worker> activeWorkers;
    private final String greet; //'ok DMTP' / 'ok DMAP'

    private boolean shutdown = false;
    private SocketIO socketIO;

    protected Worker(Socket clientSocket, List<Worker> activeWorkers, String initMsg) {
        this.clientSocket = clientSocket;
        this.activeWorkers = activeWorkers;
        this.greet = initMsg;
    }

    @Override
    public void run() {
        activeWorkers.add(this);

        try {
            this.socketIO = new SocketIO(clientSocket);
        } catch (IOException e) {
            throw new RuntimeException("could not initiate socket connection to client", e);
        }

        Shell_V2 REQUEST_PARSER = new Shell_V2(this, socketIO.getOutputStream());
        this.socketIO.printLine(greet);

        while (!shutdown) {
            try {
                // get request
                String request;
                try {
                    request = socketIO.readLine();
                } catch (IOException e) {
                    throw new InterruptedException();
                }

                // execute: call Method in .this based on request
                if (request != null) {
                    REQUEST_PARSER.callWorker(request);
                } else if (this.clientSocket.isClosed()) {
                    throw new InterruptedException("client connection is closed");
                }
            } catch (InterruptedException e) { // exit loop
                break;
            }
        }

        activeWorkers.remove(this);
        killConnection();
    }

    @Command
    public String quit() {
        socketIO.printLine("ok bye");
        shutdown = true;
        killConnection();
        return "ok bye";
    }

    public void quitOnError() {
        shutdown = true;
        killConnection();
    }

    public void killConnection() {
        if (!clientSocket.isClosed()) {
            try {
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
