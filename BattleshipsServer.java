import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;
import java.sql.Timestamp;

public class BattleshipsServer {

    private static final int SERVER_PORT = 22;
    private ServerListener listener;
    private ServerSocket ssocket;
    private Socket socket;
    private Scanner in;
    private ArrayList<ServerThread> clients;

    public BattleshipsServer () {
        // Instantiate a single thread to listen for 
        // all incoming connections
        clients = new ArrayList<ServerThread>();
        System.out.print('\f');
        System.out.println("Starting the Battleships server...");
        in = new Scanner(System.in);
        new ReaderThread().start();
        listener = new ServerListener();
        listener.start();
    }

    public class ReaderThread extends Thread {
        
        private boolean running = true;

        public ReaderThread () {
        }

        public void run () {
            while (running) {
                System.out.print("Battleships Server> ");
                parse(BattleshipsServer.this.in.next());
            }
        }

        private void parse (String s) {
            if (s.equalsIgnoreCase("help")) {
                System.out.println("Help is not available");
            } else if (s.equalsIgnoreCase("who")) {
                System.out.println("Clients currently connected:");
                if (BattleshipsServer.this.clients.size() == 0) {
                    System.out.println("No clients connected"); 
                    return;
                }
                for (ServerThread t : BattleshipsServer.this.clients) {
                    //Timestamp connected_time = new Timestamp(System.currentTimeMillis() - t.connected);
                    System.out.println(t.host + " " + t.connected);
                }
                running = false;
            } else {
                System.out.println("Command not supported");
            }
        }
    }

    public class ServerListener extends Thread {
        // Constantly query for new connection
        // when we see a new connection, spawn a new
        // ServerThread
        
        private boolean listening = true;
        public ServerListener() {
        }

        public void run () {
            try {
                BattleshipsServer.this.ssocket = new ServerSocket(BattleshipsServer.this.SERVER_PORT);

                while (listening) {
                    BattleshipsServer.this.socket = BattleshipsServer.this.ssocket.accept();
                    System.out.println("Client accepted");
                    try {
                        ServerThread t = new ServerThread(BattleshipsServer.this.socket);
                        BattleshipsServer.this.clients.add(t);
                    } catch (IOException e) {}
                }
            } catch (IOException e) {}
        }

        public void setListening (boolean b) {
            listening = b;
        }
    }

    public class ServerThread extends Thread {
        // Parse the incoming messages on a per-client
        // basis.
        
        private Socket socket;
        private InetAddress host;
        private long connected;
        private boolean running;
        private String line,input;
        private PrintStream out;

        public ServerThread (Socket s) throws IOException {
            this.socket = s;
            this.host = s.getInetAddress();
        }
        
        public void run () {
            try {
                // Get input from the client
                DataInputStream in = new DataInputStream (socket.getInputStream());
                out = new PrintStream(socket.getOutputStream());

                while (running) {
                    input = "";
                    while((line = in.readLine()) != null && !line.equals(".")) {
                        input=input + line;
                        out.println("C:" + line);
                    }
                    parse(input);
                }
            } catch (IOException e) { }
        }

        private void parse (String s) {
            // What will i do with the commands....?
            if (s.equals("Test Message 1")) {
                out.println("Received test message 1");
                System.out.println("S: Received test message 1");
            } else if (s.equals("Test Message 2")) { 
                out.println("Received test message 2");
                System.out.println("S: Received test message 2");
            } else { 
                out.println("Stop sending me garbage!");
                System.out.println("Stop sending me garbage!");
            }
        }
    }

    public static void main(String [] args) {
        new BattleshipsServer();
    }
}
