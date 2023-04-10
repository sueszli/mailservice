package dslab.mail;

import dslab.exception.ValidationException;
import dslab.util.Validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mail {

    // set by DB
    private Integer id;

    // set by transferServer
    private boolean lookUpError;
    private String mailboxIP;
    private Integer mailboxPort;

    // required fields
    private String sender;
    private List<String> recipients;
    private String subject;
    private String data;

    public Mail() {
    }

    /**
     * Validates mail-address-format of sender and recipients
     */
    public Mail(String sender, List<String> recipients, String subject, String data) {
        // format of mail addresses must be valid
        Validator.validMailAddress(sender);
        this.sender = sender;
        Validator.validMailList(recipients);
        this.recipients = recipients;
        this.subject = subject;
        this.data = data;
    }

    /**
     * Initializes this as an domainLookUp error mail
     * Only validates sender.
     */
    public Mail(String recipient, String recipientIP, int recipientPort, String senderIP, String errorMessage) {
        String senderAdr = "mailer@[" + senderIP + "]"; // sender must be transferServer
        Validator.validMailAddress(senderAdr);
        this.sender = senderAdr;

        this.recipients = new ArrayList<>(Collections.singleton(recipient));
        this.subject = "Mail Delivery Error";
        this.data = errorMessage;

        this.mailboxIP = recipientIP;
        this.mailboxPort = recipientPort;

        this.lookUpError = true;
    }

    // for Database
    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }


    // ===============================
    // for TransferServer
    public boolean getLookUpErrorBoolean() {
        return lookUpError;
    }

    public void setLookUpErrorBoolean(boolean val) {
        this.lookUpError = val;
    }

    public String getMailboxIP() {
        return mailboxIP;
    }

    public void setMailboxIP(String mailboxIP) {
        this.mailboxIP = mailboxIP;
    }

    public Integer getMailboxPort() {
        return mailboxPort;
    }

    public void setMailboxPort(Integer mailboxPort) {
        this.mailboxPort = mailboxPort;
    }

    // ===============================

    public String getSender() {
        return sender;
    }

    /**
     * Validates format of addresses
     */
    public void setSender(String sender) throws ValidationException {
        // format of mail addresses must be valid
        Validator.validMailAddress(sender);
        this.sender = sender;
    }

    public List<String> getRecipients() {
        return recipients;
    }

    /**
     * Validates format of addresses
     */
    public void setRecipients(List<String> recipients) {
        // format of mail addresses must be valid
        if (!recipients.isEmpty()) {
            Validator.validMailList(recipients);
        }
        this.recipients = new ArrayList<>(recipients); // clone
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    /**
     * Does not validate format of addresses
     */
    @Override
    public Mail clone() {
        // Strings can be used with their reference, because they are immutable
        Mail c = new Mail();
        c.id = this.id;

        c.lookUpError = this.lookUpError;
        c.mailboxIP = this.mailboxIP;
        c.mailboxPort = this.mailboxPort;

        c.sender = this.sender;
        c.recipients = new ArrayList<>(this.recipients);
        c.subject = this.subject;
        c.data = this.data;
        return c;
    }
}