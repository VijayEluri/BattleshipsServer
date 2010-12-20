import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;

public class BattleshipsClient implements Observer {
    private Socket s;
    private ClientThread client_thread;
    private static final int SERVER_PORT = 22191;
    private static final String PS1 = "Battleships Client> ";
    private final int WINDOW_SIZE = 10;
    private Scanner in;
    private int goodSeq;
    private int seqNo;
    private static Thread console_thread;

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
        } else if (split[0].equals("q") || split[0].equals("queue")) {
            if (client_thread != null) {
                client_thread.printQueue();
            }
        } else if (split[0].equals("thread")) {
            System.out.println("Thread state:");
            System.out.println("---------------");
            System.out.println("Console: \t"+console_thread.getState());
            client_thread.thread_state();
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
        client_thread.start();
    }

    public void close () {
        if (client_thread != null) {
            client_thread.close();
        }
        System.exit(0);
    }

    public class ClientSender implements Runnable, Observer {
        private final LinkedBlockingQueue<Message> queue;
        private PrintWriter out;
        private Socket socket;
        private boolean running = true;
        private int seqno;
        private int ack_seqno;
        private final int WINDOW_SIZE = 10;

        public ClientSender (Socket s) {
            this.socket = s;
            this.running = true;
            this.seqno = 0;

            try {
                out = new PrintWriter(s.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }

            queue = new LinkedBlockingQueue<Message>();
            seqno = 0;
            ack_seqno = 0;
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
            while ( true ) {
                try {
                    consume(queue.take());
                } catch (InterruptedException e) { 
                    e.printStackTrace(); 
                }
            }
        }
    
        private void consume (Message m) {
            out.println(seqno+" "+m.message);
            System.out.println("Sending ["+m.seqno+" "+m.message+"]");
        }

        private void printQueue() {
            System.out.print("["+queue.size()+"]: ");
            Iterator itr = queue.iterator();
            while(itr.hasNext()) {
                Message m = (Message)itr.next();
                System.out.print(m.message);
                if (itr.hasNext()) System.out.print("-->");
            }
            System.out.println("");
        }
    
        public void sendMessage(String s) {
            try {
                Message m = new Message(s, seqno);
                queue.offer(m, 100, TimeUnit.MILLISECONDS);
                //queue.put(m);
                seqno++;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
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
        private InputStreamReader isr;
        private boolean running;
        private String msg;

        public ClientListener (Socket s) {
            this.socket = s;
            try {
                isr = new InputStreamReader(socket.getInputStream());
                in = new BufferedReader(isr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.running = true;
        }

        public void run () {
            while (running) {
                try {
                    while ((msg = in.readLine()) != null) {
                        setChanged();
                        notifyObservers( msg );
                        msg = "";
                    }
                } catch (IOException e) { e.printStackTrace(); }
            }
        }

        public void close() {
            running = false;
            try {
                this.socket.close();
                System.out.println("Closing connection...");
                this.in.close();
                this.isr.close();
                System.out.println("Closing input reader...");
            } catch (IOException e) { e.printStackTrace(); }
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
        public void printQueue() {
            sender.printQueue();
        }
        public void thread_state() {
            if (t_listener != null)
                System.out.println("Listener:\t"+t_listener.getState());
            if (t_sender != null)
                System.out.println("Sender:  \t"+t_sender.getState());
        }
    }

    public class Message {
        public String message;
        public int seqno;
        public Message (String s, int i) {
            message = s;
            seqno = i;
        }
    }

    public static void main (String[] args) {
        System.out.print(PS1);

        Console console = new Console();
        BattleshipsClient bc = new BattleshipsClient();

        console.addObserver(bc);

        console_thread = new Thread(console);
        console_thread.start();
    }
}
