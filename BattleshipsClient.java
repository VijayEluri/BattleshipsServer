import java.net.*;
import java.util.*;
import java.io.*;

public class BattleshipsClient {
    private Socket s;
    private ClientThread thread;
    private static final int SERVER_PORT = 22191;
    private static final int NORTH = 1;
    private static final int EAST = 2;
    private static final int WEST = 3;
    private static final int SOUTH = 4;

    public BattleshipsClient () { 
        try { s = new Socket("scottio.us", 22191); }
        catch (IOException e) {e.printStackTrace();}
        thread = new ClientThread(s);
        thread.start();
        thread.sendMessage("LOGIN tester1 blah");
        thread.sendMessage("MOVE " + NORTH);
        thread.sendMessage("MOVE " + SOUTH);
        thread.sendMessage("garbage");
        /*
        thread.close();
        try { thread.join(); }
        catch (InterruptedException e) {e.printStackTrace();}
        System.exit(0);
        */
    }

    public class ClientThread extends Thread {
        private boolean running;
        private BufferedReader in;
        private PrintWriter out;
        private Socket socket;
        private String msg;
        public ClientThread(Socket s) {
            running = true;
            this.socket = s;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {}
        }
        
        public void run () {
            while (running) {
                try {
                    while ((msg = in.readLine()) != null) {
                        System.out.println("S: " + msg);
                        msg = "";
                    }
                } catch (IOException e) {}
            }
        }

        public void sendMessage(String s) {
            System.out.println("C: " +s);
            out.println(s);
        }

        public void close() {
            try {
                this.socket.close();
                System.out.println("Closing connection...");
            } catch (IOException e) {}
        }

    }

    public static void main (String[] args) {
        new BattleshipsClient();
    }

}
