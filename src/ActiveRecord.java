import java.sql.*;

public abstract class ActiveRecord {

    public abstract void load();
    public abstract void save();
    
    public ResultSet load_internal(String table, int id) {
        Connection conn = Database.getSQLConnection();
        ResultSet results = null;
        PreparedStatement query = null;
        try {
            query = conn.prepareStatement("SELECT * FROM ? WHERE id=?;");
            query.setString(1, table);
            query.setInt(2, id);
            results = query.executeQuery();
            return results;
        } catch (SQLException e) {
            e.printStackTrace(); 
        } finally {
            try {
            if (query != null) query.close();
            if (conn != null) conn.close();
            } catch (SQLException e) { e.printStackTrace(); } 
        }
        return null;
    }

    public ResultSet load_by_unique_string (String table, String unique_string, String column_name) {
        Connection conn = Database.getSQLConnection();
        ResultSet results = null;
        PreparedStatement query = null;
        try {
            query = conn.prepareStatement("SELECT * FROM ? WHERE ?=?;");
            query.setString(1, table);
            query.setString(2, column_name);
            query.setString(3, unique_string);
            results = query.executeQuery();
            return results;
        } catch (SQLException e) {
            e.printStackTrace(); 
        } finally {
            try {
            if (query != null) query.close();
            if (conn != null) conn.close();
            } catch (SQLException e) { e.printStackTrace(); } 
        }
        return null;
        
    }

    public void set(String table, String field, String value) {
        Connection conn = Database.getSQLConnection();
        PreparedStatement update = null;
        String query = "UPDATE ? SET ? VALUES ?;";   
        try {
            update = conn.prepareStatement(query);
            update.setString(1, table);
            update.setString(2, field);
            update.setString(3, value);
            update.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if(update != null) update.close();
                if(conn != null) conn.close();
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }
}
