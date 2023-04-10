package dslab.transfer;

import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.exception.ProtocolException;
import dslab.exception.ValidationException;
import dslab.mail.Mail;
import dslab.util.Config;
import dslab.util.Validator;
import dslab.workerAbstract.Worker;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Command line interface CLI implementation for DMTP protocol in TransferServer.
 * Creates Mail and passes it to ForwardWorker to send.
 */
public class TransferWorkerDMTP extends Worker {

    private final Executor forwardPool;
    private final Mail mail = new Mail();

    // passed to ForwardWorker
    private final List<ForwardWorker> activeForwardWorkers;
    private final Config config;
    private final ServerSocket serverSocket;
    private boolean has_begun = false;

    /**
     * Requirements of DomainLookUp:
     * <ul>
     *     <li>Mail mail, Config transferServerProperties</li>
     * </ul>
     * Requirements of Workers:
     * <ul>
     *     <li>Worker (parent of TransferWorkerDMTP): Socket clientSocket, List<Worker> activeWorkers</li>
     *     <li>TransferWorkerDMTP: Executor forwardPool</li>
     *     <li>ForwardWorker: List<ForwardWorker> activeSenderTasks, Executor forwardPool, Config config, ServerSocket serverSocket</li>
     * </ul>
     */
    public TransferWorkerDMTP(Socket clientSocket, List<Worker> activeForwardWorkers, Executor forwardPool, List<ForwardWorker> activeSenderTasks, Config config, ServerSocket serverSocket) {
        super(clientSocket, activeForwardWorkers, "ok DMTP");
        this.forwardPool = forwardPool;

        // passed to ForwardWorker
        this.activeForwardWorkers = activeSenderTasks;
        this.config = config;
        this.serverSocket = serverSocket;
    }

    @Command
    public String begin() {
        this.has_begun = true;
        return "ok";
    }

    @Command
    public String subject(List<String> subject) {
        if (!has_begun) {
            throw new ProtocolException();
        }
        if (subject.isEmpty()) {
            throw new ValidationException("subject expected");
        }
        mail.setSubject(String.join(" ", subject));
        return "ok";
    }

    @Command
    public String data(List<String> data) {
        if (!has_begun) {
            throw new ProtocolException();
        }
        if (data.isEmpty()) {
            throw new ValidationException("data expected");
        }
        mail.setData(String.join(" ", data));
        return "ok";
    }

    @Command
    public String from(String from) throws ValidationException {
        if (!has_begun) {
            throw new ProtocolException();
        }
        mail.setSender(from);
        return "ok";
    }

    @Command
    public String to(String to) throws ValidationException {
        if (!has_begun) {
            throw new ProtocolException();
        }
        List<String> recipients = Arrays.stream(to.split(",")).unordered().map(String::trim).collect(Collectors.toList());
        mail.setRecipients(recipients);
        return "ok " + recipients.size();
    }

    @Command
    public String send() {
        // validate mail format
        Validator.validMail(mail);

        // domain lookup for all recipients
        DomainLookUp dlu = new DomainLookUp(mail, config);

        // send valid emails
        List<Mail> validMails = dlu.getValidMails();
        validMails.stream().unordered().forEach(
                v -> forwardPool.execute(new ForwardWorker(v, activeForwardWorkers, forwardPool, config, serverSocket)) // <---------------------------------
        );

        // send error of domain-look-up failures
        Mail errorMail = dlu.getErrorMail();
        if (errorMail != null) {
            forwardPool.execute(new ForwardWorker(errorMail, activeForwardWorkers, forwardPool, config, serverSocket)); // <---------------------------------
        }

        return "ok";
    }
}