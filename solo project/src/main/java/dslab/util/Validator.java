package dslab.util;

import dslab.exception.ValidationException;
import dslab.mail.Mail;

import java.util.List;

/**
 * Validator for Mail-Entities.
 * Called every time a Mail property is initiated.
 * Offers static methods to validate a Mails property.
 */
public class Validator {

    public static boolean validMailAddressBoolean(String address) {
        // https://howtodoinjava.com/java/regex/java-regex-validate-email-address/
        // return address.matches("^[a-zA-Z0-9_!#$%&'*+/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$");

        // https://stackoverflow.com/questions/201323/how-can-i-validate-an-email-address-using-a-regular-expression
        String regex = "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";
        return address.matches(regex);
    }

    public static void validMail(Mail m) {
        //required fields
        String sender = m.getSender();
        List<String> recipients = m.getRecipients();
        String subject = m.getSubject();
        String data = m.getData();

        //sender
        validMailAddress(sender);

        //recipients
        validMailList(recipients);

        //subject
        if (subject == null || subject.isBlank()) {
            throw new ValidationException("mail has no subject");
        }

        //data
        if (data == null || data.isBlank()) {
            throw new ValidationException("mail has no body");
        }
    }

    public static void validMailList(List<String> recipients) {
        if (recipients == null || recipients.isEmpty()) {
            throw new ValidationException("mail has no recipients");
        }

        // get invalid recipients
        StringBuilder invalids = new StringBuilder();
        recipients.stream().unordered().forEach(r -> {
            if (!validMailAddressBoolean(r)) {
                invalids.append(r).append(", ");
            }
        });

        // found invalid recipients
        if (!invalids.toString().isBlank()) {
            String out = invalids.substring(0, invalids.length() - 2); // remove last ', '
            throw new ValidationException("mail contains invalid recipients: " + out);
        }
    }

    public static void validMailAddress(String address) {
        if (!validMailAddressBoolean(address)) {
            throw new ValidationException("mail has invalid mail address");
        }
    }
}
