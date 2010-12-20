import java.net.*;
import java.io.*;
import java.util.LinkedList;
import java.util.Scanner;
import java.sql.Timestamp;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.Observer;
import java.util.Observable;

public class BattleshipsServer implements BattleshipsServerInterface, Observer {

    private static final int SERVER_PORT = 22191;
    private static final int WORLD_WIDTH = 50;
    private static final int WORLD_HEIGHT = 50;
    private static final String PS1 = "Battleships Server> ";

    private ServerListener listener;
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
        listener = new ServerListener();
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
                        System.out.println(t.host + " " + t.connected);
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
        } else if (s.equalsIgnoreCase("exit") || s.equalsIgnoreCase("quit")) {
            System.exit(0);
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

    public class ServerListener extends Thread {
        // Constantly query for new connection
        // when we see a new connection, spawn a new
        // ServerThread
        
        private boolean listening = true;
        public ServerListener() {
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
        private DataInputStream in;
    
        public Client client;
        public long lastAction;
        public boolean softTimeout;

        public ServerThread (Socket s) throws IOException {
            this.socket = s;
            this.host = s.getInetAddress();
            in = new DataInputStream (socket.getInputStream());
            out = new PrintStream(socket.getOutputStream());
            running = true;
            lastAction = System.currentTimeMillis();
        }
        
        public void run () {
            try {
                // Get input from the client
                while (running) {
                    while((input = in.readLine()) != null) {
                        System.out.println("C: " +input);
                        parse(input);
                        lastAction = System.currentTimeMillis();
                        input = "";
                    }
                    try { sleep(500);} 
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
            } catch (IOException e) { e.printStackTrace(); }
        }

        private void parse (String s) {
            // What will i do with the commands....?
            String[] split = s.split(" ");
            boolean status = false;
            int seqno = 0;
            try { seqno = Integer.parseInt(split[0]); }
            catch (NumberFormatException e) { e.printStackTrace(); }
            String goodOutMsg = seqno + " ACK ";
            String badOutMsg = seqno + " NAK ";

            if (split.length <= 0)
                return;

            if (split[1].equalsIgnoreCase("MOVE")) {
                status = move(Integer.parseInt(split[1]),
                                            client);
                if (status) {
                    goodOutMsg += input;
                    sendMessage(goodOutMsg);
                }
            } else if (split[1].equalsIgnoreCase("FIRE")) {
                status = fire(Integer.parseInt(split[1]),
                                            Integer.parseInt(split[2]));
            } else if (split[1].equalsIgnoreCase("LOGIN")) {
                Client newClient = login(split[1], split[2]);
                if (newClient == null) {
                    sendMessage(badOutMsg + s);
                } else {
                    this.client = newClient;
                    goodOutMsg += input;
                    sendMessage(goodOutMsg);
                }
            } else {
                sendMessage(badOutMsg +s);
            }
        }

        private void sendMessage(String s) {
            System.out.println("S: ["+s+"]");
            out.println(s);
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
                            System.out.println("Client " +t.host+" hit soft-timeout limit");
                            t.softTimeout = true;
                        }
                        if(now - t.lastAction > HARD_TIMEOUT) {
                            System.out.println("Client " +t.host+
                               " hit hard-timeout limit, disconecting...");
                            boolean retry = true;
                            while (retry) {
                                try {   
                                    t.join();
                                    retry = false;
                                } catch (InterruptedException e) { e.printStackTrace(); }
                            }
                            clients.remove(t);
                        }
                    }
                } catch (Exception e) { e.printStackTrace(); 
                } finally { clientLock.unlock(); }
                try { this.sleep(CHECK_TIMER); }
                catch (InterruptedException e) { e.printStackTrace(); }
            }
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
