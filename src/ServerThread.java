import java.net.*;
import java.io.*;
import java.util.ListIterator;
import java.util.LinkedList;
import java.util.Scanner;
import java.sql.Timestamp;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Observer;
import java.util.Observable;

/* The ServerThread is the server's view of a "Client", internally it maintains 
 * all the necessary information to spawn threads for listening, sending,
 * and working on the messages recieved on that socket */
public class ServerThread {
    public Socket socket;
    public long lastAction;
    private Thread t_listener;
    private Thread t_sender;
    private Thread t_worker;
    private ServerListener listener;
    private ServerSender sender;
    private Worker worker;
    private LinkedBlockingQueue<Message> recv_queue;
    private LinkedBlockingQueue<Message> send_queue;
    public boolean softTimeout;
    private Client client;
    BattleshipsServerInterface ServerAPI;

    public ServerThread (Socket s, BattleshipsServerInterface BSint) throws IOException {
        this.socket = s;
        this.ServerAPI = BSint;
        client = null;
        recv_queue = new LinkedBlockingQueue<Message>();
        send_queue = new LinkedBlockingQueue<Message>();
        listener = new ServerListener(s, recv_queue);
        sender = new ServerSender(s, send_queue);
        worker = new Worker(recv_queue, send_queue, client);
        lastAction = System.currentTimeMillis();

        t_worker = new Thread(worker);
        t_listener = new Thread(listener);
        t_sender = new Thread(sender);
    }

    public void start() {
        if (t_listener != null)
            t_listener.start();
        if (t_sender != null)
            t_sender.start();
        if (t_worker != null)
            t_worker.start();
    }
    public void print_status () {
        System.out.println("Thread Status for "+socket.getInetAddress()+":");
        System.out.println("  S: " +t_sender.getState());
        System.out.println("  L: " +t_listener.getState());
        System.out.println("  W: " +t_worker.getState());
    }
    public void close() {
        boolean retry = true;
        while (retry) {
            try {   
                t_sender.join();
                t_listener.join();
                t_worker.join();
                retry = false;
            } catch (InterruptedException e) { e.printStackTrace(); }
        }
    }
    public void print_queue () {
        System.out.println("Queue information:");
        System.out.println("  Recv: ["+recv_queue.size()+"]");
        System.out.println("  Send: ["+send_queue.size()+"]");
    }

    /* The ServerListener will be spawned on a per-client basis to listen
     * for new info on that thread and enqueue it */
    public class ServerListener implements Runnable {
        private Socket socket;
        private BufferedReader in;
        private InputStreamReader isr;
        private boolean running;
        private String msg;
        private LinkedBlockingQueue<Message> queue;
        int prev_seqno;
        String[] split;
        Message m;
        int seqno = 0;

        public ServerListener (Socket s, LinkedBlockingQueue<Message> q) {
            this.socket = s;
            this.queue = q;
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
                    while((msg = in.readLine()) != null) {
                        split = msg.split(" ");
                        try { seqno = Integer.parseInt(split[0]); }
                        catch (NumberFormatException e) { e.printStackTrace(); }
                        String message = msg.substring(seqno/10 + 2);
                        m = new Message(message, seqno, this.socket);
                        try {
                            queue.offer(m, 100, TimeUnit.MILLISECONDS);
                            System.out.println("Recieved: ["+seqno+" "+message+"]");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        msg = "";
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void close () {
        
        }

    }

    /* The ServerSender is the compliment to the ServerListener and will send
     * all traffic relevant on a per-client basis */
    public class ServerSender implements Runnable {
        private PrintWriter out;
        private Socket socket;
        private boolean running;
        private LinkedBlockingQueue<Message> queue;
        private int prev_seqno = 0;

        public ServerSender(Socket s, LinkedBlockingQueue q) {
            this.socket = s;
            this.queue = q;
            this.running = true;
            try {
                out = new PrintWriter(s.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void run () {
            while (running) {
                try { consume(queue.take()); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        private void consume (Message m) {
            out.println(m.seqno + " " +m.message);
        }
    }

    /* The Worker will work on a per-client basis by pulling messages off the
     * recieve queue, doing the necessary work, then putting a message on the
     * send queue */
    public class Worker implements Runnable {
        private LinkedBlockingQueue<Message> send;
        private LinkedBlockingQueue<Message> recv;
        private final int MAX_RETRIES = 1;
        private int prev_seqno;
        private boolean running;
        private Client c;

        public Worker (LinkedBlockingQueue<Message> recv, 
                       LinkedBlockingQueue<Message> send,
                       Client client) {
            this.send = send;
            this.recv = recv;
            this.c = client;
            prev_seqno = 0;
            running = true;
        }
        
        public void run () {
            while (running) {
                try { consume(recv.take()); }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private void consume (Message m) {
            if (m.seqno == (prev_seqno +1) || m.seqno == 0) {
                if (parseCommand(m.message)) {
                    prev_seqno = m.seqno;
                    try {
                    send.offer(new Message("ACK", m.seqno), 100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {}
                } else {
                    try {
                    send.offer(new Message("NAK", m.seqno), 100, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException e) {}
                }
            } else {
                // The messages may be misordered.
                // We will put this message to the back of the queue 
                // incase we see the ordered message somewhere in the queue
                // and mark it as having been read once, and purge it on the 
                // second read.  The client will handle resending.
                if (m.retries >= MAX_RETRIES)
                    return;
                m.retries++;
                try { recv.offer(m, 100, TimeUnit.MILLISECONDS); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        private boolean parseCommand (String s) {
            String[] split = s.split(" ");
            if (split[0].equals("MOVE")) { 
                if(c != null) {
                    try { 
                    return ServerAPI.move(Integer.parseInt(split[1]), c);
                    } catch (Exception e) {}
                }
            } else if (split[0].equals("LOGIN")) {
                if(c == null) {
                    c = ServerAPI.login(split[1], split[2]);
                    if (c != null) {
                        return true;
                    } else {
                        return false;
                    }
                 } else return false;
            } else if (split[0].equals("LOGOUT")) {
            } else if (split[0].equals("FIRE")) {
            } else {
                return false;
            }
            return true;
        }

    }

    /* The message class is an encapsulation used internally for the ServerThread
     * to pass around Objects (Messages) as they are recieved */
    private class Message {
        public String message;
        public Socket socket;
        public int seqno;
        public int retries;
        public Message (String str, int i, Socket sock) {
            this.message = str;
            this.socket = sock;
            this.seqno = i;
            retries = 0;
        }
        public Message (String str, int i) {
            this.message = str;
            this.seqno = i;
            retries = 0;
        }
    }
}
