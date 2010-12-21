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

public class BattleshipsServer implements BattleshipsServerInterface, Observer {
    /* Global variables */
    private static final int SERVER_PORT = 22191;
    private static final int WORLD_WIDTH = 50;
    private static final int WORLD_HEIGHT = 50;
    private static final String PS1 = "Battleships Server> ";

    private Listener listener;
    private ServerSocket ssocket;
    private Socket socket;
    private static LinkedList<ServerThread> clients;
    private KeepaliveThread keepalive;
    private int[][] world;
    private static Lock clientLock;

    public BattleshipsServer () {
        // A list of each client that has connected
        clients = new LinkedList<ServerThread>();
        clientLock = new ReentrantLock();

        // Instantiate the parent thread which will be listening for new
        // connections
        listener = new Listener();
        listener.start();

        // Start the keepalive thread
        keepalive = new KeepaliveThread();
        keepalive.start();

        // Instatiate the world
        world = new int[WORLD_WIDTH][WORLD_HEIGHT];
    }

    public void update (Observable obj, Object arg) {
        // All classes that we are observing will notify us through this call
        // i.e. the console
        String response;
        if (obj instanceof Console) {
            if (arg instanceof String) {
                response = (String) arg;
                parseCLI(response);
            }
        }
    }

    private void parseCLI (String s) {
        if (s.equalsIgnoreCase("help")) {
            System.out.println("Help is not available");
        } else if (s.equalsIgnoreCase("who")) {
            System.out.println("Clients currently connected:");
            clientLock.lock();
            try {
                if (clients.size() == 0) {
                    System.out.println("No clients connected"); 
                } else {
                    for (int i=0;i<clients.size();i++) {
                        ServerThread t = clients.get(i);
                        System.out.println(t.socket.getInetAddress() + " connected");
                    }
                }
            } catch (Exception e) { e.printStackTrace(); 
            } finally { 
                clientLock.unlock();
            }
        } else if (s.equalsIgnoreCase("print world")) {
            for (int i=0;i<WORLD_WIDTH;i++) {
                for (int j=0;j<WORLD_HEIGHT;j++) {
                    if (world[i][j] == 0)
                        System.out.print(".");
                    else
                        System.out.print(world[i][j]);
                }
                System.out.println("");
            }
        } else if (s.equals("status")) {
            listener.print_status();
            keepalive.print_status();
            clientLock.lock();
            try {
                ListIterator iter = clients.listIterator();
                while(iter.hasNext()) {
                    ServerThread c = (ServerThread) iter.next();
                    c.print_status();
                }
            } catch (Exception e) { e.printStackTrace(); }
            finally { clientLock.unlock(); }
        } else if (s.equalsIgnoreCase("exit") || s.equalsIgnoreCase("quit")) {
            System.exit(0);
        } else if (s.equals("q") || s.equals("queue")) {
            clientLock.lock();
            try {
                ListIterator iter = clients.listIterator();
                while(iter.hasNext()) {
                    ServerThread c = (ServerThread) iter.next();
                    c.print_queue();
                }
            } catch (Exception e) { e.printStackTrace(); }
            finally { clientLock.unlock(); }
        } else if (s.equals("")) {
            // do nothing
        } else {
            System.out.println("Command not supported");
        }
        System.out.print(PS1);
    }

    /* Here is where we implement the Server methods */
    public boolean move (int direction, Client c) {
        // All moves are of one square, lets make sure
        // that the target square is fre
        if (c == null) return false;
        if (c.current == null) return false;
        Coordinate newLoc = null;
        switch (direction) {
            case Coordinate.NORTH:
                newLoc = new Coordinate(c.current.x+1, c.current.y);
                break;
            case Coordinate.EAST:
                newLoc = new Coordinate(c.current.x, c.current.y+1);
                break;
            case Coordinate.WEST:
                newLoc = new Coordinate(c.current.x, c.current.y-1);
                break;
            case Coordinate.SOUTH:
                newLoc = new Coordinate(c.current.x-1, c.current.y);
                break;
            default:
                break;
        }
        if (newLoc == null) return false;
        Coordinate placed = placePlayer(newLoc, c.current, c.uid, false);
        if (placed != null) {
            c.setLocation(placed);
            return true;
        }
        return false;
        
    }

    public boolean fire (int a, int b) {
        return false;
    }

    public Client login (String username, String password) {
        // Need some logic here to connect to SQL database and
        // verify login information... later...
        
        // Place him in the world
        Client tester1 = new Client (1, WORLD_WIDTH/2-1, WORLD_HEIGHT/2);
        Client tester2 = new Client (2, WORLD_WIDTH/2+1, WORLD_HEIGHT/2);
        Coordinate placed;

        if (username.equals("tester1")) {
            placed = placePlayer(tester1, true); 
            if (placed == null) return null;
            tester1.setLocation(placed);
            return tester1;
        } else if (username.equals("tester2")) {
            placed = placePlayer(tester2, true); 
            if (placed == null) return null;
            tester2.setLocation(placed);
            return tester2;
        }

        return null;
    }

    private Coordinate placePlayer(Client c, boolean placeNearby) {
        return placePlayer(c.current, c.current, c.uid, placeNearby);
    }

    private Coordinate placePlayer(Coordinate current,
                                   Coordinate previous,
                                   int uid, boolean placeNearby) {
        final int MAX_DISTANCE = 5;

        if(world[current.x][current.y] == 0) {
            world[previous.x][previous.y] = 0;
            world[current.x][current.y] = uid;
            System.out.println("<Server>: Placing player at ("+current.x+","+current.y+")");
            return current;
        } else if (placeNearby) {
            int iter = 0;
            for (int i=0;i<iter;i++) {
                for (int j=0;j<iter;j++) {
                    if (world[current.x+i][current.y+j] == 0) {
                        world[previous.x][previous.y] = 0;
                        world[current.x+i][current.y+j] = uid;
                        System.out.println("<Server>: Placing player at ("
                            +current.x+","+current.y+")");
                        return new Coordinate(current.x+i, current.y+j);
                    }
                }
            }
            return null;
        } else {
        }
        return null;
    }

    public class GameEngine extends Thread {
        // The purpose of this class is to check for when time-base
        // events in the world happen and to process them accordingly


    }

    public class Listener extends Thread {

        private boolean listening = true;
        public Listener() {
        }

        public void run () {
            try {
                ssocket = new ServerSocket(SERVER_PORT);

                while (listening) {
                    Socket newClient = ssocket.accept();
                    System.out.println("\nClient connected");
                    try {
                        ServerThread t = new ServerThread(newClient);
                        clientLock.lock();
                        try {
                            clients.add(t);
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            clientLock.unlock();
                        }
                        t.start();
                    } catch (IOException e) { e.printStackTrace(); }
                }
            } catch (IOException e) {}
        }

        public void setListening (boolean b) {
            listening = b;
        }

        public void print_status() {
            System.out.println("Listener: " +Thread.currentThread().getState());
        }
    }

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

    public class Worker implements Runnable {
        private LinkedBlockingQueue<Message> send;
        private LinkedBlockingQueue<Message> recv;
        private final int MAX_RETRIES = 1;
        private int prev_seqno;
        private boolean running;

        public Worker (LinkedBlockingQueue<Message> recv, 
                       LinkedBlockingQueue<Message> send) {
            this.send = send;
            this.recv = recv;
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
            if (m.seqno == (prev_seqno +1)) {
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
            return true;
        }

    }

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
        private boolean softTimeout;

        public ServerThread (Socket s) throws IOException {
            this.socket = s;
            recv_queue = new LinkedBlockingQueue<Message>();
            send_queue = new LinkedBlockingQueue<Message>();
            listener = new ServerListener(s, recv_queue);
            sender = new ServerSender(s, send_queue);
            worker = new Worker(recv_queue, send_queue);
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
    }

    private class KeepaliveThread extends Thread {
        // The purpose of this thread is to find clients 
        // who have dropped and disconnect them
        
        private boolean running;
        private final int SOFT_TIMEOUT = 100000;
        private final int HARD_TIMEOUT = 200000;
        private final int CHECK_TIMER = 1000;

        public KeepaliveThread () {
            running = true;
        }

        public void run () {
            while (running) {
                long now = System.currentTimeMillis();
                clientLock.lock();
                try {
                    for (ServerThread t : clients) {
                        if(now - t.lastAction > SOFT_TIMEOUT && !t.softTimeout) {
                            System.out.println("Client " +t.socket.getInetAddress()+
                                " hit soft-timeout limit");
                            t.softTimeout = true;
                        }
                        if(now - t.lastAction > HARD_TIMEOUT) {
                            System.out.println("Client " +t.socket.getInetAddress()+
                               " hit hard-timeout limit, disconecting...");
                            t.close();
                            clients.remove(t);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); 
                } finally { clientLock.unlock(); }
                try { this.sleep(CHECK_TIMER); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
        }

        public void print_status() {
            System.out.println("Keepalive: " +Thread.currentThread().getState());
        }
    }

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

    public static void main(String [] args) {
        System.out.print(PS1);

        Console console = new Console();
        BattleshipsServer bs = new BattleshipsServer();

        console.addObserver(bs);

        Thread console_thread = new Thread(console);
        console_thread.start();
    }
}
