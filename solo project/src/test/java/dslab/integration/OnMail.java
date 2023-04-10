package dslab.integration;
import dslab.JunitSocketClient;
import dslab.TestBase;
import dslab.mail.Mail;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import java.util.List;

public class OnMail extends TestBase {

    private static final Log LOG = LogFactory.getLog(EmailsArrivedCheck.class);

    // private static int dmtpServerPort = 13451; // Transfer-1
    private static int dmtpServerPort = 13450; // Transfer-2

    public void oneMail() throws Exception {
        Mail email = new Mail();

        email.setSubject("Test fail");
        email.setData("Test fail mal schauen");
        email.setRecipients(List.of("notexisting@earth.planet"));
        email.setSender("arthur@earth.planet");
        sendMail(email, 1);

    }

    private void sendMail(Mail email, int iterations) throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            LOG.info(Thread.currentThread().getName() + ": Send mail " + email);
            client.verify("ok DMTP");
            for (int i = 0; i < iterations; ++i) {
                client.sendAndVerify("begin", "ok");
                client.sendAndVerify("from " + email.getSender(), "ok");
                client.sendAndVerify("to " + String.join(",",email.getRecipients()), "ok " + email.getRecipients().size());
                client.sendAndVerify("subject " + email.getSubject(), "ok");
                client.sendAndVerify("data " + email.getSubject(), "ok");
                client.sendAndVerify("send", "ok");
            }
            client.sendAndVerify("quit", "ok bye");
        }
    }
}
