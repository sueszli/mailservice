package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.exception.ProtocolException;
import dslab.exception.ValidationException;
import dslab.mail.Mail;
import dslab.util.Config;
import dslab.util.Validator;
import dslab.workerAbstract.Worker;

import java.net.Socket;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Command line interface CLI implementation for DMTP protocol in MailboxServer.
 * Implements: begin, subject, data, from, to, send
 * Inherits: quit, quitOnError
 */
public class MailboxWorkerDMTP extends Worker {

    private final Mail mail = new Mail();
    private final InboxRepository userInboxes;
    private final Config propertiesReader;
    private boolean has_begun = false;

    public MailboxWorkerDMTP(Socket clientSocket, List<Worker> activeWorkers, InboxRepository userInboxes, Config propertiesReader) {
        super(clientSocket, activeWorkers, "ok DMTP");
        this.userInboxes = userInboxes;
        this.propertiesReader = propertiesReader;
    }

    public Mail getMail() {
        return mail;
    }

    @Command
    public String begin() {
        this.has_begun = true;
        return "ok";
    }

    @Command
    public String subject(List<String> subject) throws ValidationException {
        if (!has_begun) {
            throw new ProtocolException();
        }
        if (subject.isEmpty()) {
            throw new ValidationException("subject required");
        }
        this.mail.setSubject(String.join(" ", subject));
        return "ok";
    }

    @Command
    public String data(List<String> data) throws ValidationException {
        if (!has_begun) {
            throw new ProtocolException();
        }
        if (data.isEmpty()) {
            throw new ValidationException("data required");
        }
        this.mail.setData(String.join(" ", data));
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
    public String to(String to) throws ValidationException { // only accepts mails directed towards this domain
        String domain = propertiesReader.getString("domain");

        // actual users
        List<String> existentUsers = userInboxes.readAllUsernames();

        List<String> nonExistentUsers = Stream.of(to.split(",")).unordered()
                // filter mails where recipient is of this domain
                .filter(Validator::validMailAddressBoolean) //valid
                .filter(r -> r.split("@")[1].equals(domain)) //to this domain
                .collect(Collectors.toList())

                // get usernames from filtered mails
                .stream().unordered()
                .map(r -> r.trim().split("@")[0])
                .collect(Collectors.toList())

                // get non existent users
                .stream().unordered()
                .filter(
                    u -> existentUsers.stream().noneMatch(eu -> eu.equals(u))
                ).collect(Collectors.toList());

        if (!nonExistentUsers.isEmpty()) {
            throw new ValidationException("unknown recipient " + String.join(",", nonExistentUsers));
        }

        // all recipients are known
        List<String> rs =  Stream.of(to.split(",")).unordered()
                .filter(Validator::validMailAddressBoolean) //valid
                .filter(r -> r.split("@")[1].equals(domain)) //to this domain
                .collect(Collectors.toList());

        this.mail.setRecipients(rs);
        return "ok " + rs.size();
    }

    @Command
    public String send() throws ValidationException {
        Validator.validMail(this.mail);

        // add email to all users known to the db
        userInboxes.saveMail(this.mail);
        return "ok";
    }
}