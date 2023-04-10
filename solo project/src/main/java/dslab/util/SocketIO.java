package dslab.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

/**
 * Wrapper to abstract reading and writing through a socket.
 */
public class SocketIO {

    private final BufferedReader inputStream;
    private final PrintStream outputStream;

    public SocketIO(Socket socket) throws IOException {
        this.inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        this.outputStream = new PrintStream(socket.getOutputStream(), true);
    }

    public PrintStream getOutputStream() {
        return outputStream;
    }


    public String readLine() throws IOException {
        return inputStream.readLine();
    }

    public void printLine(Object out) {
        outputStream.println(out);
    }
}
