import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.ResultSet;
import weka.core.Attribute;
import weka.core.DenseInstance;
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
        {"NUMBERS"  ,"Down"  },
        {"LETTERS"  ,"Up"    },
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
        public String key;
        public String type;
        public double touch_pressure;
        public double touch_size;
        public double actiontime;
        public double datetime;
        
        public RawEntry(ResultSet rs){
            try {
                uuid = rs.getString("DEVICE_UUID");
                key = rs.getString("BUTTON_PRESSED");
                type = rs.getString("ACTION_TYPE");
                touch_pressure = rs.getDouble("TOUCH_PRESSURE");
                touch_size = rs.getDouble("TOUCH_SIZE");
                actiontime = rs.getDouble("ACTION_TIME_STAMP");
                datetime = rs.getDouble("DEVICE_CURRENT_DATE_TIME_ZERO_GMT");
            }
            catch (SQLException e){
                System.out.println(e.getMessage());
            }
        }
    }
    
    public void formatRawData() {
        String sql = "SELECT " +
            "DEVICE_UUID, " +
            "DEVICE_CURRENT_DATE_TIME_ZERO_GMT, " +
            "BUTTON_PRESSED, " +
            "TOUCH_PRESSURE, " +
            "TOUCH_SIZE, " +            
            "ACTION_TYPE, " +
            "ACTION_TIME_STAMP " +
            "from data " +
            "order by DEVICE_UUID, " +
            "DEVICE_CURRENT_DATE_TIME_ZERO_GMT, " +
            "ACTION_TIME_STAMP";

        // Data will hold all user's calculated data to be written out to ARFF
        LinkedHashMap<String, ArrayList<LinkedHashMap<String,Double>>> Data =
            new LinkedHashMap<String, ArrayList<LinkedHashMap<String,Double>>>();        
        try {
            ResultSet rs = CONN.createStatement().executeQuery(sql);
            while (!rs.isAfterLast()){
                // An array of raw entries representing the user's single session
                ArrayList<RawEntry> S = new ArrayList<RawEntry>();
                while (rs.next() &&
                       (S.isEmpty() || rs.getString("DEVICE_UUID").equals(S.get(S.size()-1).uuid)) &&
                       (S.isEmpty() || rs.getInt("DEVICE_CURRENT_DATE_TIME_ZERO_GMT") == S.get(S.size()-1).datetime)){
                    S.add(new RawEntry(rs));
                }
                // LinkedHashMap keeps order of insertion
                LinkedHashMap<String, Double> H = new LinkedHashMap<String, Double>();
                ArrayList<RawEntry> Filtered = filter(S, false);
                // Skip corrupted session data
                if (Filtered.isEmpty()) continue;
                holdtime(H,Filtered);
                downdown(H,Filtered);
                updown(H,Filtered);
                pressure(H,Filtered);
                fingerarea(H,Filtered);
                // for (Map.Entry<String, Double> e : H.entrySet()){
                //     String key   = e.getKey();
                //     Double value = e.getValue();
                //     System.out.println(key + " " + value);
                // }
                insertDatum(Data,H,Filtered.get(0).uuid);
            }
        }
        catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        writeToArff(Data,"formatted_data");
        System.out.println(Data.size());
    }


    // Add calculated session dataum to an exisiting user and if
    // user key doesn't already exist Data, create key value pair
    // with empty vector
    private void insertDatum(LinkedHashMap<String, ArrayList<LinkedHashMap<String,Double>>> Data,
                             LinkedHashMap<String, Double> H,
                             String uuid){
        if (Data.containsKey(uuid)){
            Data.get(uuid).add(H);
        }
        else {
            Data.put(uuid, new ArrayList<LinkedHashMap<String,Double>>());
            Data.get(uuid).add(H);
        }
    }

    // Cleanup session by filtering only relevant information.
    // Can pass in option to include offsets to incorrectly
    // typed keys.
    private ArrayList<RawEntry> filter(ArrayList<RawEntry> S, Boolean offset){
        ArrayList<RawEntry> Filtered = new ArrayList<RawEntry>();
        int counter = 0;
        for (int i = 0; i < S.size(); ++i){
            if (counter >= PASSWORD.length) break;
            String key  = PASSWORD[counter][0];
            String type = PASSWORD[counter][1];
            RawEntry e  = S.get(i);
            if (key.equals(e.key) && type.equals(e.type)){
                Filtered.add(e);
                counter++;
            }
        }
        // Skip user session with missing data points by
        // returning empty vector
        if (counter != PASSWORD.length){
            Filtered.clear();
        }
        return Filtered;
    }

    private void holdtime(LinkedHashMap<String, Double> H, ArrayList<RawEntry> Filtered){
        int item = 0;
        double mean = 0;
        for (int i = 0; i < Filtered.size()-1; i+=2){
            RawEntry e1 = Filtered.get(i);
            RawEntry e2 = Filtered.get(i+1);
            double value = e2.actiontime-e1.actiontime;
            mean+=value;
            H.put("holdtime"+ ++item,value);
        }
        if (item!=0)
            H.put("meanholdtime",mean/item);
    }

    private void downdown(LinkedHashMap<String, Double> H, ArrayList<RawEntry> Filtered){
        int item = 0;
        for (int i = 0; i < Filtered.size()-2; i+=2){
            RawEntry e1 = Filtered.get(i);
            RawEntry e2 = Filtered.get(i+2);
            H.put("downdown"+ ++item,e2.actiontime-e1.actiontime);
        }
    }

    private void updown(LinkedHashMap<String, Double> H, ArrayList<RawEntry> Filtered){
        int item = 0;
        for (int i = 1; i < Filtered.size()-2; i+=2){
            RawEntry e1 = Filtered.get(i);
            RawEntry e2 = Filtered.get(i+1);
            H.put("updown"+ ++item,e2.actiontime-e1.actiontime);
        }
    }

    private void pressure(LinkedHashMap<String, Double> H, ArrayList<RawEntry> Filtered){
        int item = 0;
        double mean = 0;
        for (int i = 0; i < Filtered.size()-1; i+=2){
            RawEntry e = Filtered.get(i);
            mean+=e.touch_pressure;
            H.put("pressure"+ ++item,e.touch_pressure);
        }
        if (item!=0)
            H.put("meanpressure",mean/item);
        
    }
    
    private void fingerarea(LinkedHashMap<String, Double> H, ArrayList<RawEntry> Filtered){
        int item = 0;
        double mean = 0;
        for (int i = 0; i < Filtered.size()-1; i+=2){
            RawEntry e = Filtered.get(i);
            mean+=e.touch_size;
            H.put("fingerarea"+ ++item,e.touch_size);
        }
        if (item!=0)
            H.put("meanfingerarea",mean/item);        
    }

    private void writeToArff(LinkedHashMap<String, ArrayList<LinkedHashMap<String,Double>>> D,
                             String filename){
        if (D.isEmpty()) return;
        ArrayList<Attribute> atts;
        ArrayList<String> attVals;
        Instances data;
        double[]  vals;
        
        // 1. Set up attributes
        atts = new ArrayList<Attribute>();
        Set<String> uuids       = D.keySet();
        Set<String> attributes = D.get(uuids.iterator().next()).get(0).keySet();

        // -numeric
        for (String a : attributes)
            atts.add(new Attribute(a));
        // -nominal
        attVals = new ArrayList<String>();
        for (String id : uuids)
            attVals.add(id);
        atts.add(new Attribute("user_id", attVals));
        
        // 2. create Instance objects
        data = new Instances("keystroketouch", atts, 0);
        
        // 3. fill with data
        outerloop:
        for (Map.Entry<String, ArrayList<LinkedHashMap<String,Double>>> e : D.entrySet()){
            String user   = e.getKey();
            for (LinkedHashMap<String,Double> session : e.getValue()){
                vals = new double[data.numAttributes()];
                int c = 0;
                for (Map.Entry<String,Double> datum : session.entrySet()){
                    vals[c++] = datum.getValue();
                }
                vals[c++] = data.attribute(data.numAttributes()-1).addStringValue(user);
                System.out.println(data.attribute(3).addStringValue(user));
                data.add(new DenseInstance(1.0, vals));
                break outerloop;
            }
        }
        
        System.out.println(data);

    }

    

    public static void main(String[] args){
        WekaArff db = new WekaArff();
        db.connect();
        db.createTableIfNotExist("data");
        db.formatRawData();
        db.close();
    }
}


