
public interface BattleshipsServerInterface {
    
    public static final int SERVER_PORT = 22191;
    public static final int WORLD_WIDTH = 50;
    public static final int WORLD_HEIGHT = 50;

    public boolean move (int direction, Client c);
    public boolean fire (int a, int b);
    public Client login (String username, String password);
}
