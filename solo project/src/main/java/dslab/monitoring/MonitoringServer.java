package dslab.monitoring;

import at.ac.tuwien.dsg.orvell.Shell;
import at.ac.tuwien.dsg.orvell.StopShellException;
import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.ComponentFactory;
import dslab.util.Config;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitoring Server - stores data received by TransferServer via UDP.
 */
public class MonitoringServer implements IMonitoringServer {

    private final Config config;
    private final Shell shell;

    // repository
    private final ConcurrentMap<String, AtomicLong> transferServerCount = new ConcurrentHashMap<>(); // number of messages transfered
    private final ConcurrentMap<String, AtomicLong> mailboxCount = new ConcurrentHashMap<>(); // number of messages received

    // listener
    private boolean listenerShutdown = false;
    private DatagramSocket udpSocket;

    public MonitoringServer(String componentId, Config config, InputStream in, PrintStream out) {
        this.config = config;
        shell = new Shell(in, out);
        shell.register(this);
        shell.setPrompt(componentId + "> ");
    }

    public static void main(String[] args) throws Exception {
        IMonitoringServer server = ComponentFactory.createMonitoringServer(args[0], System.in, System.out);
        server.run();
    }

    @Override
    public void run() {
        // open socket
        try {
            this.udpSocket = new DatagramSocket(config.getInt("udp.port"));
        } catch (IOException e) {
            throw new RuntimeException("opening UDP socket failed", e);
        }

        // define listener runnable
        Thread packageListener = new Thread(() -> {
            byte[] buffer;
            DatagramPacket packet;

            while (!listenerShutdown) {
                buffer = new byte[1024];
                packet = new DatagramPacket(buffer, buffer.length);

                try {
                    this.udpSocket.receive(packet);
                } catch (SocketException e) {
                    if (listenerShutdown) {
                        break;
                    }
                    // e.printStackTrace();
                } catch (IOException e) {
                    // e.printStackTrace();
                }

                // validate data
                String dataString = new String(Arrays.copyOfRange(packet.getData(), 0, packet.getLength())).trim();
                String regex = "^([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}):([0-9]{1,5})\\s([a-zA-Z0-9_!#$%&'*+=?`{|}~^.-]+@[a-zA-Z0-9.-]+)$";
                if (!dataString.matches(regex)) {
                    break;
                }

                // store in DB
                String[] dataArray = dataString.split("\\s"); // split at whitespace
                if (dataArray.length == 2) {
                    transferServerCount.putIfAbsent(dataArray[0], new AtomicLong(0));
                    transferServerCount.get(dataArray[0]).incrementAndGet();

                    mailboxCount.putIfAbsent(dataArray[1], new AtomicLong(0));
                    mailboxCount.get(dataArray[1]).incrementAndGet();
                }
            }
        });

        // run listener
        packageListener.start();

        // run shell
        shell.run();

        // close socket
        if (!udpSocket.isClosed()) {
            shutdown();
        }
    }

    @Override
    @Command
    public String addresses() {
        return mapToString(mailboxCount);
    }

    @Override
    @Command
    public String servers() {
        return mapToString(transferServerCount);
    }

    @Override
    @Command
    public String shutdown() {
        listenerShutdown = true;
        udpSocket.close();
        throw new StopShellException();
    }

    private String mapToString(ConcurrentMap<String, AtomicLong> map) {
        StringBuilder result = new StringBuilder();
        map.entrySet().stream().unordered().forEach(
                r -> result.append(r.getKey()).append(" ").append(r.getValue().get()).append("\n")
        );
        return result.toString();
    }
}
