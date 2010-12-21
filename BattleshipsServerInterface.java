
public interface BattleshipsServerInterface {
    
    public boolean move (int direction, Client c);
    public boolean fire (int a, int b);
    public Client login (String username, String password);
}
