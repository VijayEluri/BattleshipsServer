import java.sql.*;

public class Database {
    private static volatile Database instance = null;
    private String db, username, password, driver;

    public Database () throws SQLException {
        driver = "com.mysql.jdbc.Driver";
        username = "doug";
        password = "doug";
        db = "jdbc:mysql://localhost:3306/battle";
    }

    public static Database getInstance() {
        if (instance == null) { 
            try {
            instance = new Database();
            } catch (Exception e) {e.printStackTrace();}
        }
        return instance;
    }
    private Connection _getSQLConnection() {
        try {
            return DriverManager.getConnection(db + "?autoReconnect=true&user=" + username + "&password=" + password);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    public static Connection getSQLConnection() {
       return getInstance()._getSQLConnection(); 
    }
}
