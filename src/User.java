import java.util.LinkedList;

public class User extends ActiveRecord {
    
    private String table = "users";
    private int id;
    private LinkedList<Field> fields;

    public User(int identifier) {
        this.id = identifier;
        this.fields = new LinkedList<Field>();
    }
    
    public void load() {
       load_internal(table, id); 
    }

    public void save () {
        save_internal(table, fields);
    }

    public void setUsername(String s) {
        setColumn("username", s);
    }
    
    public void setPassword(String s) {
        setColumn("passwd", s);
    }

    private void setColumn(String field, String val) {
        
        for (Field s : fields) {
            if (s.field.equals(field)) {
                s.value = val;
                s.changed = true;
                return;
            }
        }
        fields.addLast(new Field(field, val));
        return;
    }
}
