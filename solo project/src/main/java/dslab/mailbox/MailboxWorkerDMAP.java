package dslab.mailbox;

import at.ac.tuwien.dsg.orvell.annotation.Command;
import dslab.exception.ValidationException;
import dslab.mail.Mail;
import dslab.util.Config;
import dslab.workerAbstract.Worker;

import java.net.Socket;
import java.util.List;
import java.util.MissingResourceException;
import java.util.stream.Collectors;

/**
 * Command line interface CLI implementation for DMAP protocol in MailboxServer.
 * Implements: login, list, show, delete, logout
 * Inherits: quit, quitOnError
 */
public class MailboxWorkerDMAP extends Worker {

    private final InboxRepository userInboxes;
    private final Config propertiesReader;
    private String loggedInUsername;

    public MailboxWorkerDMAP(Socket clientSocket, List<Worker> activeWorkers, InboxRepository userInboxes, Config config) {
        super(clientSocket, activeWorkers, "ok DMAP");
        this.userInboxes = userInboxes;
        this.propertiesReader = new Config(config.getString("users.config"));
    }

    // checks if password and username match
    @Command
    public String login(String username, String password) throws ValidationException {
        if (loggedInUsername != null) {
            throw new ValidationException("already logged in");
        }

        // read password
        String pwd;
        try {
            pwd = propertiesReader.getString(username);
        } catch (MissingResourceException e) {
            throw new ValidationException("there is no such username");
        }

        // compare input with password
        if (!pwd.equals(password)) {
            throw new ValidationException("wrong password");
        }
        this.loggedInUsername = username;

        return "ok";
    }

    // lists all emails by user that is logged in
    @Command
    public String list() throws ValidationException {
        if (loggedInUsername == null) {
            throw new ValidationException("log in first");
        }
        return userInboxes.readAllMails(loggedInUsername).stream().unordered()
                .map(m -> m.getId() + " " + m.getSender() + " " + m.getSubject())
                .collect(Collectors.joining("\n"));
    }

    @Command
    public String logout() throws ValidationException {
        loggedInUsername = null;
        return "ok";
    }

    // shows specific mail
    @Command
    public String show(Integer id) throws ValidationException {
        if (loggedInUsername == null) {
            throw new ValidationException("log in first");
        }

        Mail m = userInboxes.readMail(id, loggedInUsername);
        if (m == null) {
            throw new ValidationException("message id not known");
        }

        return "from " + m.getSender() + " \n" +
                "to " + String.join(",", m.getRecipients()) + " \n" +
                "subject " + m.getSubject() + " \n" +
                "data " + m.getData();
    }

    @Command
    public String delete(Integer id) throws ValidationException {
        if (loggedInUsername == null) {
            throw new ValidationException("log in first");
        }
        userInboxes.deleteMail(id, loggedInUsername);
        return "ok";
    }
}