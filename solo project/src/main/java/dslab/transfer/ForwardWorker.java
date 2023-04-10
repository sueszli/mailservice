package dslab.transfer;

import dslab.exception.DomainNotFoundException;
import dslab.exception.InvalidRequestException;
import dslab.mail.Mail;
import dslab.util.Config;
import dslab.util.SocketIO;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Responsible for forwarding lookUpError mails or replaying received mails to mailboxes
 */
public class ForwardWorker implements Runnable {

    private final Mail email;
    // received from caller
    private final List<ForwardWorker> activeForwardWorkers;
    private final Executor forwardPool;
    private final Config config;
    private final ServerSocket serverSocket;
    private Socket mailboxSocket;
    private SocketIO socketIO;

    public ForwardWorker(Mail email, List<ForwardWorker> activeForwardWorkers, Executor forwardPool, Config config, ServerSocket serverSocket) {
        this.email = email;

        this.activeForwardWorkers = activeForwardWorkers;
        this.forwardPool = forwardPool;
        this.config = config;
        this.serverSocket = serverSocket;
    }

    // connect to Mailbox as client and replay Mail
    @Override
    public void run() {
        activeForwardWorkers.add(this);

        try {
            try {
                this.mailboxSocket = new Socket(email.getMailboxIP(), email.getMailboxPort());
            } catch (IOException e) {
                sendFailureMail("could not connect to recipient server");
                throw new RuntimeException("Could not connect to " + email.getMailboxIP() + ":" + email.getMailboxPort() + ": " + e.getMessage(), e);
            }

            socketIO = new SocketIO(mailboxSocket);
            socketIO.readLine();
            socketIO.printLine("begin");
            socketIO.readLine();

            this.replayMail();

            socketIO.printLine("quit");
            this.killConnection();

        } catch (SocketException ignored) {
            System.out.println("Stopped sending because the Socket is closed");
        } catch (IOException e) {
            e.printStackTrace();
        }

        activeForwardWorkers.remove(this);
    }

    public void killConnection() {
        if (!mailboxSocket.isClosed()) {
            try {
                this.mailboxSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void replayMail() throws IOException {
        // make sure recipients exist
        int c = 0;
        do {
            socketIO.printLine("to " + String.join(",", email.getRecipients()));
        } while (c++ < 5 && email.getRecipients().size() > 0 && // try 5 more times if
                !email.getLookUpErrorBoolean() && // not failure mail
                containsNonExistentRecipient() // contains nonexistent recipient (removes them and sends failure mails in another thread in each call)
        );

        socketIO.printLine("data " + email.getData());
        socketIO.readLine();
        socketIO.printLine("subject " + email.getSubject());
        socketIO.readLine();
        socketIO.printLine("from " + email.getSender());
        socketIO.readLine();
        socketIO.printLine("send");

        try {
            // check if response is ok
            if (!socketIO.readLine().trim().startsWith("ok")) {
                throw new InvalidRequestException();
            }

            sendUDPData(); // on successful sending

        } catch (InvalidRequestException e) {
            // response was not ok
            if (!email.getLookUpErrorBoolean()) {
                String rs = String.join(",", email.getSender());
                this.sendFailureMail("error sending mail to " + rs);
            }
            e.printStackTrace();
        }
    }

    private boolean containsNonExistentRecipient() throws IOException {
        String res = socketIO.readLine().trim();

        if (res.startsWith("ok")) {
            return false;
        }

        String[] tmp = res.split(" ");
        // error message from of mailbox says: unknown recipient
        if (List.of(tmp).contains("unknown") && List.of(tmp).contains("recipient")) {

            // list of names after last space
            List<String> invalidNames = Arrays.stream(tmp[tmp.length - 1].split(",")).unordered()
                    .map(String::trim)
                    .collect(Collectors.toList());

            List<String> invalidMails = email.getRecipients().stream().unordered()
                    .filter(r -> invalidNames.contains(r.split("@")[0]))
                    .collect(Collectors.toList());

            email.getRecipients().removeAll(invalidMails);

            // send failure mail to sender
            this.sendFailureMail("error not-existent emails " + String.join(",", invalidMails));
        }

        return true;
    }

    private void sendFailureMail(String error) {
        try {
            Mail failureMail = DomainLookUp.createErrorMail(email.getSender(), error, config);
            forwardPool.execute(new ForwardWorker(failureMail, activeForwardWorkers, forwardPool, config, serverSocket)); // <-----------------------------------
        } catch (DomainNotFoundException ignored) {
        }
    }

    private void sendUDPData() {
        try {
            String data = serverSocket.getInetAddress().getHostAddress() + ":" + serverSocket.getLocalPort() + " " + this.email.getSender();
            InetSocketAddress address = new InetSocketAddress(config.getString("monitoring.host"), config.getInt("monitoring.port"));

            new DatagramSocket().send(new DatagramPacket(data.getBytes(), data.getBytes().length, address));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}