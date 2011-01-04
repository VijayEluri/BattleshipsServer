import java.sql.*;
import java.util.LinkedList;
import java.util.HashMap;

public abstract class ActiveRecord {

    private ResultSet results;

    public abstract void load();
    public abstract void save();
    
    public void load_internal(String table, int id) {
        Connection conn = Database.getSQLConnection();
        PreparedStatement query = null;
        try {
            query = conn.prepareStatement("SELECT * FROM ? WHERE id=?;");
            query.setString(1, table);
            query.setInt(2, id);
            results = query.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace(); 
        } finally {
            try {
            if (query != null) query.close();
            if (conn != null) conn.close();
            } catch (SQLException e) { e.printStackTrace(); } 
        }
    }
    public void save_internal(String table, LinkedList<Field> fields) {
        Connection conn = Database.getSQLConnection();
        PreparedStatement update = null;
        String query = "UPDATE ? SET ? VALUES ?;";   
        try {
            for (Field f : fields) {
                if (!f.changed) continue;
                update = conn.prepareStatement(query);
                update.setString(1, table);
                update.setString(2, f.field);
                update.setString(3, f.value);
                update.executeQuery();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if(update != null) update.close();
                if(conn != null) conn.close();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
    
    public void remove () {
    
    } 

    public void getColumn(String field) {
        try {
            results.getString(field);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    protected class Field {
        public String field;
        public String value;
        public boolean changed;

        public Field (String f, String v) {
            this.field = f;
            this.value = v;
        }
    }
}
