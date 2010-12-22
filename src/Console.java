import java.util.Observable;      
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Console extends Observable implements Runnable {
    public void run() {
        try {
            final InputStreamReader isr = new InputStreamReader( System.in );
            final BufferedReader br = new BufferedReader( isr );
            while( true ) {
                String response = br.readLine();
                setChanged();
                notifyObservers( response );
                try {
                    Thread.currentThread().sleep(100);
                } catch (InterruptedException e) { }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
