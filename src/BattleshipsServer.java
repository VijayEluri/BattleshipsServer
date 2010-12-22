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
        keepalive = new KeepaliveThread(clients, clientLock);
        keepalive.start();

        // Instatiate the world
        world = new int[WORLD_WIDTH][WORLD_HEIGHT];
    }

    /* Functions specific to the CLI */
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

    /* Here is where we implement the Server API methods */
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

    /* These are private functions for fulfilling the API methods */
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

    /* The GameEngine will handle all game-related processing */
    public class GameEngine extends Thread {
        // The purpose of this class is to check for when time-base
        // events in the world happen and to process them accordingly


    }
    
    /* The Listener will listen for new connections and handle them */
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
                        ServerThread t = new ServerThread(newClient, 
                                BattleshipsServer.this);
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

    public static void main(String [] args) {
        System.out.print(PS1);

        Console console = new Console();
        BattleshipsServer bs = new BattleshipsServer();

        console.addObserver(bs);

        Thread console_thread = new Thread(console);
        console_thread.start();
    }
}
