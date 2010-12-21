public class Client {
    // Just a container class to keep a bunch of info in one place
    public String username;
    public Coordinate current;
    public int uid;

    public Client (int uid, int locX, int locY) {
        this.uid = uid;
        this.current = new Coordinate(locX, locY);
    }
    
    public void setLocation(Coordinate newLoc) {
        current = newLoc;
    }
}
