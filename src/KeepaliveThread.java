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

/* The keepalivethread has the sole purpose of making sure ot disconnect users
 * who exceed an idle timeout period */
public class KeepaliveThread extends Thread {
    // The purpose of this thread is to find clients 
    // who have dropped and disconnect them
    
    private boolean running;
    private final int SOFT_TIMEOUT = 100000;
    private final int HARD_TIMEOUT = 200000;
    private final int CHECK_TIMER = 1000;
    private LinkedList<ServerThread> clients;
    private Lock clientLock;

    public KeepaliveThread (LinkedList<ServerThread> clients,
                            Lock clientLock) {
        running = true;
        this.clients = clients;
        this.clientLock = clientLock;
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
