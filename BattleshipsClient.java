import java.net.*;
import java.util.*;
import java.io.*;

public class BattleshipsClient {
    private Socket s;
    private ClientThread thread;

    public BattleshipsClient () { 
        try { s = new Socket("scottio.us", 22); }
        catch (IOException e) {e.printStackTrace();}
        thread = new ClientThread(s);
        thread.start();
        thread.sendMessage("Test Message 1");
        thread.sendMessage("Test Message 2");
        thread.sendMessage("alkjdfal;kjsadf;lkjadsf;lkj");
        thread.close();
        try { thread.join(); }
        catch (InterruptedException e) {}
    }

    public class ClientThread extends Thread {
        private boolean running;
        private BufferedReader br;
        private PrintWriter pw;
        private Socket socket;
        private String msg;
        public ClientThread(Socket s) {
            running = true;
            this.socket = s;
            try {
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                pw = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {}
        }
        
        public void run () {
            while (running) {
                try {
                    while ((msg = br.readLine()) != null && running) {
                        System.out.println("S: " + msg);
                    }
                } catch (IOException e) {}
            }
        }

        public void sendMessage(String s) {
            System.out.println("C: " +s);
            pw.println(s);
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
