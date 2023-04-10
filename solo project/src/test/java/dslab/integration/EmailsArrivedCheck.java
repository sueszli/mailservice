package dslab.integration;

import dslab.*;
import dslab.mail.Mail;
import dslab.mailbox.IMailboxServer;
import dslab.monitoring.IMonitoringServer;
import dslab.transfer.ITransferServer;
import dslab.util.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;

public class EmailsArrivedCheck extends TestBase {

    private static final Log LOG = LogFactory.getLog(EmailsArrivedCheck.class);
    private static final int num_users = 100; // divisible by 2
    private static final int mails_per_connection = 5;
    private static final int mailRunner = 5;
    private static final int checkRunner = 10;
    private AssertionError threadedAssertion;
    private final String mailboxComponentId = "mailbox-earth-planet";
    private IMailboxServer mailbox;
    private int dmapServerPort;
    private Thread mailboxThread;
    private final TestInputStream mailboxIn = new TestInputStream();

    private final String transferComponentId = "transfer-1";
    private ITransferServer transfer;
    private int dmtpServerPort;
    private Thread transferThread;
    private final TestInputStream transferIn = new TestInputStream();
    private final TestOutputStream transferOut = new TestOutputStream();

    private final String monitorComponentId = "monitoring";
    private IMonitoringServer monitor;
    private int udpServerPort;
    private Thread monitorThread;
    private final TestInputStream monitorIn = new TestInputStream();

    private final AtomicInteger emailId = new AtomicInteger(0);

    public EmailsArrivedCheck() {
        timeout = new Timeout(12000, TimeUnit.SECONDS);
    }

    @Before
    public void setUp() throws Exception {
        mailbox = ComponentFactory.createMailboxServer(mailboxComponentId, mailboxIn, out);
        transfer = ComponentFactory.createTransferServer(transferComponentId, transferIn, transferOut);
        monitor = ComponentFactory.createMonitoringServer(monitorComponentId, monitorIn, out);

        dmapServerPort = new Config(mailboxComponentId).getInt("dmap.tcp.port");
        dmtpServerPort = new Config(transferComponentId).getInt("tcp.port");
        udpServerPort = new Config(monitorComponentId).getInt("udp.port");

        transferThread = new Thread(mailbox);
        mailboxThread = new Thread(transfer);
        monitorThread = new Thread(monitor);
        monitorThread.start();
        mailboxThread.start();
        transferThread.start();

        LOG.info("Waiting for server sockets to appear");
        Sockets.waitForSocket("localhost", dmapServerPort, Constants.COMPONENT_STARTUP_WAIT);
        Sockets.waitForSocket("localhost", dmtpServerPort, Constants.COMPONENT_STARTUP_WAIT);
    }

    @After
    public void tearDown() throws Exception {
        transferIn.addLine("shutdown"); // send "shutdown" command to command line
        monitorIn.addLine("shutdown"); // send "shutdown" command to command line
        mailboxIn.addLine("shutdown");
        Thread.sleep(Constants.COMPONENT_TEARDOWN_WAIT);
    }


    @Test
    public void sendEmail_shouldBeInMailbox() throws Exception {

        Mail email = new Mail();
        email.setSubject("Test #1");
        email.setData("Das ist ein test, kommt er an?");
        email.setRecipients(List.of("trillian@earth.planet", "test@test.asdf", "nothere@earth.planet"));
        email.setSender("arthur@earth.planet");

        LOG.info(String.format("Send email from %s to %s", email.getSender(), email.getRecipients()));
        sendMail(email, 1);

        Thread.sleep(2000); //wait to send

        LOG.info("Check if email arrived at trillian@earth.planet");
        checkReceivedEmail(email, "trillian", "12345", false);

        LOG.info("Check if err email arrived at arthur@earth.planet");
        Mail errMail = new Mail();
        errMail.setSubject("Mail Delivery Error");
        errMail.setSender("mailer@[127.0.0.1]");
        checkReceivedEmail(errMail, "arthur", "23456", true);
        checkEmailAmount("arthur", "23456", 2);

    }

    @Test
    public void bigEmailPenetration() throws Exception {

        Mail email = new Mail();
        email.setData("Test test");
        email.setSender("arthur@earth.planet");
        email.setRecipients(List.of());

        List<Thread> runner = new ArrayList<>();
        for (int i = 0; i < mailRunner; ++i)
            runner.add(getMailRunner(email, true));

        // test failure mail
        email.setRecipients(List.of("idontexist@unkno.wn"));
        runner.add(getMailRunner(email, false));
        email.setRecipients(List.of("idontexist@earth.planet"));
        runner.add(getMailRunner(email, false));

        runner.forEach(Thread::start);
        for (Thread thread : runner) {
            thread.join();
        }

        waitUntilForwardDone(2000);
        runner.clear();
        int userPerRunner = num_users / checkRunner;
        int mailsPerUser = mailRunner * 2 * mails_per_connection;
        for (int i = 0; i < checkRunner; ++i) {
            Thread r;
            if (i + 1 == checkRunner)
                r = getCheckRunner(i * userPerRunner, num_users, mailsPerUser);
            else
                r = getCheckRunner(i * userPerRunner, (i + 1) * userPerRunner, mailsPerUser);
            runner.add(r);
        }

        runner.forEach(Thread::start);

        checkEmailAmount("arthur", "23456", 2 * mails_per_connection * num_users);

        for (Thread thread : runner) {
            thread.join();
        }

        if (threadedAssertion != null) throw threadedAssertion;
    }


    private void sendMail(Mail email, int iterations) throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmtpServerPort, err)) {
            email.setSubject("Test-" + emailId.getAndIncrement());
            LOG.info(Thread.currentThread().getName() + ": Send mail " + email);
            client.verify("ok DMTP");
            for (int i = 0; i < iterations; ++i) {
                client.sendAndVerify("begin", "ok");
                client.sendAndVerify("from " + email.getSender(), "ok");
                client.sendAndVerify("to " + String.join(",", email.getRecipients()), "ok " + email.getRecipients().size());
                client.sendAndVerify("subject " + email.getSubject(), "ok");
                client.sendAndVerify("data " + email.getSubject(), "ok");
                client.sendAndVerify("send", "ok");
            }
            client.sendAndVerify("quit", "ok bye");
        }
    }

    private void checkReceivedEmail(Mail email, String user, String password, boolean isError) throws Exception {
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify(String.format("login %s %s", user, password), "ok");

            client.send("list");
            String listResult = client.listen();
            InetAddress add = client.getSocket().getInetAddress();
            InetAddress cl = client.getSocket().getLocalAddress();
            err.checkThat(listResult, containsString(String.
                    format("%s %s", isError ? "mailer@[127.0.0.1]" : email.getSender(), email.getSubject())));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    private void checkEmailAmount(String user, String password, int amount) throws IOException, InterruptedException {

        LOG.info(String.format("User %s should have %d mails", user, amount));
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            client.sendAndVerify(String.format("login %s %s", user, password), "ok");

            client.send("list");
            String listResult = client.listen();

            assertEquals(amount, listResult.split("\n").length);
            LOG.info(String.format("OK for %s", user));

            client.sendAndVerify("logout", "ok");
            client.sendAndVerify("quit", "ok bye");
        }
    }

    /**
     * @param fromUser inclusive
     * @param toUser   exclusive
     */
    private void checkAllUsers(int fromUser, int toUser, int emailCount) throws IOException {
        LOG.info(String.format(Thread.currentThread().getName() + ": All users %d-%d should have %d mails", fromUser, toUser, emailCount));
        try (JunitSocketClient client = new JunitSocketClient(dmapServerPort, err)) {
            client.verify("ok DMAP");
            for (int i = fromUser; i < toUser; ++i) {
                LOG.info(String.format(Thread.currentThread().getName() + ": Check user %s ...", i));
                client.sendAndVerify(String.format("login %s %s", i, 'p'), "ok");
                client.send("list");
                String[] listResult = client.listen().split("\n");
                assertEquals(emailCount, listResult.length);
                LOG.info(String.format(Thread.currentThread().getName() + ": OK for %s", i));

                client.sendAndVerify("logout", "ok");
            }
            client.sendAndVerify("quit", "ok bye");
        }
    }

    private Thread getMailRunner(Mail copy, boolean addRecv) {
        return new Thread(() -> {
            Mail email = copy.clone(); //create copy of mail
            for (int i = 0; i < num_users; ++i) {
                if (addRecv)
                    email.setRecipients(List.of(
                            i % num_users + "@earth.planet",
                            (i + 1) % num_users + "@earth.planet"
                    ));
                try {
                    sendMail(email, mails_per_connection);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void waitUntilForwardDone(int checkInterval) throws InterruptedException {
        while (true) {
            LOG.info("Check mails of server");
            transferIn.addLine("forwardStatus");
            Thread.sleep(checkInterval);
            List<String> servers = transferOut.getLines();
            transferOut.reset();
            String activeThreads = servers.stream()
                    .filter(s -> s.contains("ActiveThreads:"))
                    .findFirst().orElse("notfound 1");
            String queue = servers.stream()
                    .filter(s -> s.contains("Queue:"))
                    .findFirst().orElse("notfound 0");
            int activeThreadsCount = Integer.parseInt(activeThreads.split(" ")[1]);
            int queueCount = Integer.parseInt(queue.split(" ")[1]);
            LOG.info(String.format("%d threads are transfering mails and %d are in queue", activeThreadsCount, queueCount));
            if (activeThreadsCount == 0) {
                break;
            }
        }
    }

    private Thread getCheckRunner(int from, int to, int count) {
        return new Thread(() -> {
            try {
                checkAllUsers(from, to, count);
            } catch (AssertionError err) {
                threadedAssertion = err;
                throw err;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

}