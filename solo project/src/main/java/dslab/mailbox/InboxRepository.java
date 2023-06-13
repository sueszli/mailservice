package dslab.mailbox;

import dslab.exception.ValidationException;
import dslab.mail.Mail;
import dslab.util.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

// Repository inside each MailBox-Server

/**
 * Map of {@code <username, <id, Mail>>} for the Mailbox server.
 */
public class InboxRepository {

    // username -> Map (users Inbox)
    // only inner HashMap is synchronized: that's the individual users Inbox accessed by Workers
    private final HashMap<String, ConcurrentHashMap<Integer, Mail>> userInboxes;
    private final AtomicInteger maxId = new AtomicInteger();

    public InboxRepository(Config propertiesReader) {
        this.userInboxes = new HashMap<>();
        propertiesReader.listKeys().stream().unordered().forEach(
                username -> userInboxes.put(username, new ConcurrentHashMap<>())
        );
    }

    public void saveMail(Mail mail) {
        mail.setId(maxId.getAndIncrement()); // set ID

        mail.getRecipients().stream().unordered()
                .map(r -> r.split("@")[0]) // usernames
                .collect(Collectors.toList())
                .stream().unordered().forEach(
                        r -> userInboxes.get(r).put(mail.getId(), mail) // Inbox based on username -> <id, mail>
                );
    }

    public Mail readMail(Integer id, String username) {
        return userInboxes.get(username).get(id);
    }

    public void deleteMail(Integer id, String username) {
        if (userInboxes.get(username).remove(id) == null) {
            throw new ValidationException("message " + id + " does not exist");
        }
    }

    public List<String> readAllUsernames() {
        return new ArrayList<>(userInboxes.keySet());
    }

    public List<Mail> readAllMails(String username) {
        return new ArrayList<>(userInboxes.get(username).values());
    }
}
