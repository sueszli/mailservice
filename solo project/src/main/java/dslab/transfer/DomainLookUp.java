package dslab.transfer;

import dslab.exception.DomainNotFoundException;
import dslab.mail.Mail;
import dslab.util.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.MissingResourceException;

/**
 * DomainLookup: Validation of Mail-recipient-domains.
 * If a domain doesn't exist, create domainLookUpFailureMail for the sender, remove recipient from original Mail.
 */
public class DomainLookUp {

    private final Config transferServerProps;
    private final List<Mail> validMails = new ArrayList<>();
    private Mail errorMail;
    private boolean senderNotFound = false;

    public DomainLookUp(Mail mail, Config transferServerProps) {
        this.transferServerProps = transferServerProps;
        domainLookUp(mail);
    }

    public static Mail createErrorMail(String recipient, String errorMessage, Config transferServerProperties) throws DomainNotFoundException {
        // look up senders port and IP
        String recipientIP;
        int recipientPort;

        try {
            String domain = recipient.split("@")[1];
            String[] temp = new Config("domains").getString(domain).split(":");
            recipientIP = temp[0];
            recipientPort = Integer.parseInt(temp[1]);
        } catch (MissingResourceException e) {
            throw new DomainNotFoundException(); // <--------- thrown if sender not found
        }

        String transferServerIP = transferServerProperties.getString("monitoring.host");
        return new Mail(recipient, recipientIP, recipientPort, transferServerIP, errorMessage);
    }

    public List<Mail> getValidMails() {
        return validMails;
    }

    public Mail getErrorMail() {
        return errorMail;
    }

    private void domainLookUp(Mail m) {
        List<String> recipients = new ArrayList<>(m.getRecipients());

        // clear recipients
        Mail input = m.clone();
        input.getRecipients().clear();


        HashMap<String, Mail> domain2mailMap = new HashMap<>();

        // ==================== START OF RECIPIENT LOOP ====================

        for (String r : recipients) {
            String rDomain = r.split("@")[1];

            String rIP;
            int rPort;
            try {
                try {
                    String[] ip_port = new Config("domains").getString(rDomain).split(":");
                    rIP = ip_port[0];
                    rPort = Integer.parseInt(ip_port[1]);
                } catch (MissingResourceException e) {
                    throw new DomainNotFoundException(); // <-----
                }

                // >>>>> Recipient found, no Exception thrown
                // add to recipient of domain2mailMap (valid mails)
                if (!domain2mailMap.containsKey(rDomain)) {
                    Mail send = input.clone();
                    send.setMailboxIP(rIP);
                    send.setMailboxPort(rPort);
                    domain2mailMap.put(rDomain, send);
                }
                domain2mailMap.get(rDomain).getRecipients().add(r);

            } catch (DomainNotFoundException e) {
                // >>>>> Recipient not found, Exception thrown
                if (senderNotFound) continue; // dont add to errorMail

                if (errorMail == null) {
                    try {
                        errorMail = createErrorMail(input.getSender(), "error domain unknown for ", this.transferServerProps);
                    } catch (DomainNotFoundException ex) { // sender not found
                        senderNotFound = true;
                        continue;
                    }
                }

                errorMail.setData(errorMail.getData() + r + ", ");
            }
        }

        // ====================== END OF RECIPIENT LOOP ======================

        domain2mailMap.entrySet().stream().unordered().forEach(
                e -> validMails.add(e.getValue())
        );

        if (errorMail != null && errorMail.getData().length() > 2) {
            String data = errorMail.getData();
            errorMail.setData(data.substring(0, data.length() - 2)); // remove last ', '
        }
    }
}