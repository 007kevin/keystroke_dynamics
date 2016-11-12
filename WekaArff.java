import java.util.Vector;
import java.util.LinkedHashMap;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

public class WekaArff {
    private Connection CONN = null;
    private static final String URL = "jdbc:sqlite:raw_data.db";
    private static final String[] KEYS = {
        "holdtime1"    , "holdtime2", "holdtime3", "holdtime4", "holdtime5",
        "holdtime6"    , "holdtime7", "holdtime8", "holdtime9", "holdtime10",
        "holdtime11"   , "holdtime12", "holdtime13", "holdtime14", "downdown1",
        "downdown2"    , "downdown3", "downdown4", "downdown5", "downdown6",
        "downdown7"    , "downdown8", "downdown9", "downdown10", "downdown11",
        "downdown12"   , "downdown13", "updown1", "updown2", "updown3",
        "updown4"      , "updown5", "updown6", "updown7", "updown8",
        "updown9"      , "updown10", "updown11", "updown12", "updown13",
        "pressure1"    , "pressure2", "pressure3", "pressure4", "pressure5",
        "pressure6"    , "pressure7", "pressure8", "pressure9", "pressure10",
        "pressure11"   , "pressure12", "pressure13", "pressure14", "fingerarea1",
        "fingerarea2"  , "fingerarea3", "fingerarea4", "fingerarea5", "fingerarea6",
        "fingerarea7"  , "fingerarea8", "fingerarea9", "fingerarea10", "fingerarea11",
        "fingerarea12" , "fingerarea13", "fingerarea14", "meanholdtime", "meanpressure",
        "meanfingerarea"
    };

    // Store sequence of actions and types for
    // typing valid password
    private static final String[][] PASSWORD = {
        {"."        ,"Down"  },
        {"."        ,"Up"    },
        {"LETTERS"  ,"Down"  },
        {"NUMBERS"  ,"Up"    },
        {"t"        ,"Down"  },
        {"t"        ,"Up"    },
        {"i"        ,"Down"  },
        {"i"        ,"Up"    },
        {"e"        ,"Down"  },
        {"e"        ,"Up"    },
        {"LETTERS"  ,"Down"  },
        {"NUMBERS"  ,"Up"    },
        {"5"        ,"Down"  },
        {"5"        ,"Up"    },
        {"LETTERS"  ,"Down"  },
        {"NUMBERS"  ,"Up"    },
        {"SHIFT"    ,"Down"  },
        {"SHIFT"    ,"Up"    },
        {"R"        ,"Down"  },
        {"r"        ,"Up"    },
        {"o"        ,"Down"  },
        {"o"        ,"Up"    },
        {"a"        ,"Down"  },
        {"a"        ,"Up"    },
        {"n"        ,"Down"  },
        {"n"        ,"Up"    },
        {"l"        ,"Down"  },
        {"l"        ,"Up"    }
    };
    
    public void connect() {
        try {
            CONN = DriverManager.getConnection(URL);
            System.out.println("Connection to SQLite has been established.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    
    public void close() {
        try {
            if (CONN != null) {
                CONN.close();
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
    }
    
    public void createTableIfNotExist(String TABLE_NAME) {
        String sql = "CREATE TABLE IF NOT EXISTS " +TABLE_NAME+ " (" + 
            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +         
            "DEVICE_UUID varchar(255), " +
            "DEVICE_LANGUAGE varchar(255), " +                       
            "DEVICE_HARDWARE_MODEL varchar(255), " +                 
            "SDK_VERSION varchar(255), " +                     
            "DEVICE_MANUFACTURE varchar(255), " +                    
            "DEVICE_IN_INCH DOUBLE, " +                              
            "DEVICE_TIME_ZONE varchar(255), " +                      
            "DEVICE_CURRENT_DATE_TIME_ZERO_GMT BIGINT, " +           
            "DEVICE_LOCAL_COUNTRY_CODE varchar(255), " +             
            "DEVICE_NUMBER_OF_PROCESSORS INTEGER, " +                
            "DEVICE_LOCATION varchar(255), " +                       
            "DEVICE_LOCATION_LAT DOUBLE, " +                         
            "DEVICE_LOCATION_LONG DOUBLE, " +                        
            "BUTTON_PRESSED varchar(255), " +                        
            "TOUCH_PRESSURE DOUBLE, " +                              
            "TOUCH_SIZE DOUBLE, " +                                  
            "RAW_X DOUBLE, " +                                       
            "RAW_Y DOUBLE, " +                                       
            "X_PRECISION DOUBLE, " +                                 
            "Y_PRECISION DOUBLE, " +                                 
            "ACTION_TYPE varchar(255), " +                           
            "ACTION_TIME_STAMP BIGINT, " +                           
            "HR_TIME_STAMP varchar(255) " +                          
            ")";
        
        try {
            CONN.createStatement().execute(sql);
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    private class RawEntry {
        public String uuid;
        public String button;
        public String type;
        public int actiontime;
        public int datetime;
        
        public RawEntry(ResultSet rs){
            try {
            uuid = rs.getString("DEVICE_UUID");
            button = rs.getString("BUTTON_PRESSED");
            type = rs.getString("ACTION_TYPE");
            actiontime = rs.getInt("ACTION_TIME_STAMP");
            datetime = rs.getInt("DEVICE_CURRENT_DATE_TIME_ZERO_GMT");
            }
            catch (SQLException e) {
                System.out.println(e.getMessage());
            }
        }
    }
    
    public void formatRawData() {
        String sql = "SELECT " +
            "DEVICE_UUID, " +
            "DEVICE_CURRENT_DATE_TIME_ZERO_GMT, " +
            "BUTTON_PRESSED, " +
            "ACTION_TYPE, " +
            "ACTION_TIME_STAMP " +
            "from data " +
            "where DEVICE_UUID = 'GWIHW1476465997505' " +
            "order by DEVICE_UUID, " +
            "DEVICE_CURRENT_DATE_TIME_ZERO_GMT, " +
            "ACTION_TIME_STAMP";
        try {
            ResultSet rs = CONN.createStatement().executeQuery(sql);
            while (!rs.isAfterLast()){
                // An array of raw entries representing the user's single session
                Vector<RawEntry> S = new Vector<RawEntry>();
                while (rs.next() &&
                       (S.isEmpty() || rs.getString("DEVICE_UUID").equals(S.lastElement().uuid)) &&
                       (S.isEmpty() || rs.getInt("DEVICE_CURRENT_DATE_TIME_ZERO_GMT") == S.lastElement().datetime)){
                    S.addElement(new RawEntry(rs));
                }
                System.out.println(S.size());
                LinkedHashMap<String, Double> H = extractFeatures(S);
                break;
            }
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    // LinkedListHashMap keeps order of insertion
    private LinkedHashMap<String, Double> extractFeatures(Vector<RawEntry> S){
        LinkedHashMap<String, Double> H = new LinkedHashMap<String, Double>();
        holdtime(H,S);
        return H;
    }

    private void holdtime(LinkedHashMap<String, Double> H, Vector<RawEntry> S){
        // Copy of original session for removing unneeded information for holdtime
        Vector<RawEntry> ht;
        int counter = 0;
        for (int i = 0; i < S.size(); ++i){
            
            for (String key : PASSWORD[counter]){
                if (S[i].button == key && S[i].type == type){
                    
            }
        }
    }

    public void printPass(){
        for (String[] array : PASSWORD)
            for (String action : array)
                System.out.println(action);
    }

    public static void main(String[] args){
        WekaArff db = new WekaArff();
        db.connect();
        db.createTableIfNotExist("data");
        db.formatRawData();
        db.printPass();
        db.close();
    }
}


