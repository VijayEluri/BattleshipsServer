import java.sql.*;

public class User extends ActiveRecord {
    
    private String table = "users";
    private int id;
    private ResultSet results;

    public User(int identifier) {
        this.id = identifier;
        results = load_internal(table, id);
    }

    public User(String username) {
        results = load_by_unique_string(table, username, "username");
        if (results != null) {
            try {
                this.id = results.getInt("id");
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    public void load() {
       results = load_internal(table, id); 
    }

    public void save () {
        try {
            results.updateRow();
        } catch (SQLException e) { e.printStackTrace(); }
    }

    public void setUsername(String s) {
        try {
            results.updateString("username", s);
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    public void setPassword(String s) {
        try {
            results.updateString("passwd", s);
        } catch (SQLException e) { e.printStackTrace(); }
    }

}
