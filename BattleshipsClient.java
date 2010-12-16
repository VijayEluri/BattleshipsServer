import java.net.*;
import java.util.*;
import java.io.*;

public class BattleshipsClient implements Observer {
    private Socket s;
    private ClientThread client_thread;
    private static final int SERVER_PORT = 22191;
    private static final String PS1 = "Battleships Client> ";
    private final int WINDOW_SIZE = 10;
    private Scanner in;
    private int goodSeq;
    private int seqNo;

    public BattleshipsClient () { 
        // Instantiate the console...
        System.out.print('\f');
        seqNo = 0;
    }

    public void test () {
        client_thread.sendMessage("LOGIN tester1 blah");
        client_thread.sendMessage("MOVE " + Coordinate.NORTH);
        client_thread.sendMessage("MOVE " + Coordinate.SOUTH);
        client_thread.sendMessage("garbage");
    }

    public void update(Observable obj, Object arg) {
        if (obj instanceof Console) {
            // We got a message from the console
            if (arg instanceof String) {
                parseCLI((String) arg);
            }
        }
    }

    public void parseCLI (String s) {
        String [] split = s.split(" ");
        String msg = "";
        if (s.equals("start")) {
            start();
        } else if (s.equals("quit")) {
            close();
        } else if (s.equals("test")) {
            test();
        } else if (split[0].equals("send")) {
            for(int i = 1; i < split.length; i++) {
                msg += split[i];
                msg += " ";
            }
            client_thread.sendMessage(msg);
        } else {
            System.out.println("Command not supported");
        }
        System.out.print(PS1);
    }

    public void start() {
        // Start the socket
        try { s = new Socket("scottio.us", 22191); }
        catch (IOException e) {e.printStackTrace();}
        client_thread = new ClientThread(s);
    }

    public void close () {
        if (client_thread != null) {
            client_thread.close();
        }
        System.exit(0);
    }

    public class ClientSender implements Runnable, Observer {
        private LinkedList<String> queue;
        private PrintWriter out;
        private Socket socket;
        private boolean running;
        private int seqno;
        private int ack_seqno;
        private final int WINDOW_SIZE = 10;

        public ClientSender (Socket s) {
            this.socket = s;
            this.running = true;
            this.seqno = 0;

            try {
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void update(Observable obj, Object arg) {
            String response;
            String [] split;
            int recv_seqno = 0;

            if (arg instanceof String) {
                response = (String)arg;
                split = response.split(" "); 
                if (split[1].equalsIgnoreCase("ACK")) {
                    try { recv_seqno = Integer.parseInt(split[0]); }
                    catch (NumberFormatException e) {}
                    System.out.println("ack_seqno = " + ack_seqno+
                                       " recv_seqno = " + recv_seqno+
                                       " queue size = " + queue.size());
                    ack_seqno = recv_seqno;
                }
            }
        }

        public void run () {
            while (running) {
                if (queue.size() > 0 && seqno - ack_seqno < WINDOW_SIZE) {
                    out.println(seqno+" "+queue.removeFirst());
                }
            }

        }

        public void sendMessage(String s) {
            queue.addLast(s);
        }

        public void close() {
            try {
                this.socket.close();
                System.out.println("Closing connection...");
            } catch (IOException e) {}
        }

    }

    public class ClientListener extends Observable implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private boolean running;
        private String msg;

        public ClientListener (Socket s) {
            this.socket = s;
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run () {
            while (running) {
                try {
                    while ((msg = in.readLine()) != null) {
                        setChanged();
                        notifyObservers( msg );
                        msg = "";
                    }
                } catch (IOException e) {}
            }
        }

        public void close() {
            try {
                this.socket.close();
                System.out.println("Closing connection...");
            } catch (IOException e) {}
        }

    }

    public class ClientThread  {
        private ClientListener listener;
        private ClientSender sender;
        private Thread t_listener;
        private Thread t_sender;

        public ClientThread(Socket s) {
            listener = new ClientListener(s);
            sender = new ClientSender(s);
            listener.addObserver(sender);
            t_listener = new Thread(listener);
            t_sender = new Thread(sender);
        }
        
        public void start() {
            t_listener.start();
            t_sender.start();
        }
        public void sendMessage(String s) {
           sender.sendMessage(s);
        }
        public void close() {
            sender.close();
            listener.close();
            boolean retry = true;
            while (retry) {
                try {
                    t_listener.join();
                    t_sender.join();
                    retry = false;
                } catch (InterruptedException e) { e.printStackTrace(); }
            }
       }
    }

    public static void main (String[] args) {
        System.out.print(PS1);

        Console console = new Console();
        BattleshipsClient bc = new BattleshipsClient();

        console.addObserver(bc);

        Thread console_thread = new Thread(console);
        console_thread.start();
    }
}
