/*
 * By Dodge for KAPS Ltd.
 * Patrick Waweru 14-05-2011
 */

package com.kaps;

import java.util.*;
import java.io.*;
//import com.logica.smpp.debug.*;
import com.kaps.utils.*;
import com.mchange.v2.c3p0.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author patnox
 */
public class queue
{
    public boolean debug_mode = true;
    public systemLogger rl = null;
    //public systemLogger tl = null;
    public String logging = "true";
    public boolean log_to_file = true;
    public String logtofile = "true";
    public int monitorport = 4900;
    static String propsFilePath = "./settings.cfg";
    Properties properties = new Properties();
    public  ComboPooledDataSource dataSource = null;
    public static final String DEFAULT_DB_IP = "131.107.3.240";
    public String DBip = DEFAULT_DB_IP;
    public static final String DEFAULT_DB_PORT = "2527";
    public String DBport = DEFAULT_DB_PORT;
    public static final String DEFAULT_DB_NAME = "netparkingclient";
    public String DBname = DEFAULT_DB_NAME;
    public static final String DEFAULT_DB_USER = "pos";
    public String DBuser = DEFAULT_DB_USER;
    public static final String DEFAULT_DB_PASS = "zabimaru";
    public String DBpassword = DEFAULT_DB_PASS;
    public static final int DEFAULT_MAX_DB_CONNECTIONS = 30;
    public int maxDBconnections = DEFAULT_MAX_DB_CONNECTIONS;
    public static final int DEFAULT_MIN_DB_CONNECTIONS = 10;
    public int minDBconnections = DEFAULT_MIN_DB_CONNECTIONS;
    public static final int DEFAULT_QSVR_SERVER_PORT = 2000;
    public int pos_server = DEFAULT_QSVR_SERVER_PORT;
    public static final int DEFAULT_QSVR_SECURE_SERVER_PORT = 2006;
    public int qsvrSecureServerPort = DEFAULT_QSVR_SECURE_SERVER_PORT;
    public Connection c;
    public int pingdb = 0;

    public queue()
    {
        //init logger
        try
        {
//            rl = new systemLogger("./myreceiverlog.log");
//            tl = new systemLogger("./mytransmitterlog.log");
//            if(debug_mode == false)
//            {
//                rl.logtofile = false;
//                tl.logtofile = false;
//            }
            rl = new systemLogger();
            rl.logtofile = log_to_file;
        }
        catch(Exception eww)
        {
            System.err.println("Error trying to initialize log engine: " + eww.getMessage());
            eww.printStackTrace();
        }
        
        //Init the variables
        log_this("MainCore::::Initializing Queue Server....");
        loadProperties(propsFilePath);
        log_this("MainCore::::Loaded the settings....");
        log_this("MainCore::::logging = " + logging);
        log_this("MainCore::::monitorport = " + monitorport);
        log_this("MainCore::::DBip = " + DBip);
        log_this("MainCore::::DBport = " + DBport);
        log_this("MainCore::::DBname = " + DBname);
        log_this("MainCore::::DBuser = " + DBuser);
        log_this("MainCore::::DBpassword = " + DBpassword);
        log_this("MainCore::::mindbconnections = " + minDBconnections);
        log_this("MainCore::::maxdbconnections = " + maxDBconnections);
        log_this("MainCore::::pos_server = " + pos_server);
        log_this("MainCore::::qsvrsecureserverport = " + qsvrSecureServerPort);
        log_this("MainCore::::ping db = " + pingdb);
        log_this("MainCore::::logtofile = " + logtofile);
    }
    
    public  void MakePool()
        {
               try 
               {
                 dataSource=new ComboPooledDataSource();
                 dataSource.setDriverClass("com.mysql.jdbc.Driver");
                 dataSource.setJdbcUrl("jdbc:mysql://131.107.3.240:2527/netparkingclient");
                 dataSource.setUser("pos");
                 dataSource.setPassword("zabimaru");
                 dataSource.setMaxPoolSize(DEFAULT_MAX_DB_CONNECTIONS);
                 dataSource.setMinPoolSize(DEFAULT_MIN_DB_CONNECTIONS);
                 //dataSource.setAcquireIncrement(Accomodation);
             } 
             catch (Exception ew) 
             {
                 System.err.println("Error trying to initialize datasource: " + ew.getMessage());
                log_this("MainCore::::Error trying to initialize datasource: " + ew.getMessage());
                ew.printStackTrace();
              }

    }
    public  Connection getConnection()
    {
        try {
            return dataSource.getConnection();
        } catch (SQLException ex) {
            System.err.println("Error trying to initialize datasource: " + ex.getMessage());
            Logger.getLogger(queue.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public void start()
    {
        this.MakePool();
  
        if (connectToDB())
        {
            //Start monitor
            Thread uzi1 = new Thread(new monitorServer(monitorport, rl, rl));
            uzi1.setDaemon(false);
            uzi1.start();
//
//            //sleep
            try{Thread.sleep(2000);}catch(Exception we){}


            //Start command server
            log_this("MainCore::::Starting the qsvr server....");
            Thread uzi3 = new Thread(new superServer(pos_server, this));
            uzi3.setDaemon(false);
            uzi3.start();

//            
//            //The DB keep alive thread
            if(pingdb == 1)
            {
                log_this("Starting Ping DB");
                Thread uzi9 = new Thread(new mysqlServerKAThread(dataSource, rl));
                uzi9.setDaemon(false);
                uzi9.start();
            }
            else
            {
                log_this("Ping DB not started due to setting");
            }
        }
    }

    public boolean connectToDB()
    {
        // Connect to the Firebird DataSource
        // Try to Connect to the Firebird DataSource
        //We try until we get it
        log_this("Trying to connect");
        while(true)
        {
            try
            {
                c = this.getConnection();
                log_this("got  to connect");
                Statement stmt = c.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT * FROM users");
                while(rs.next()){
                     System.out.println("MainCore: Registered user: " + rs.getString("username"));
                }                 
               
                stmt.close();

                // At this point, there is no implicit driver instance
                // registered with the driver manager!
                System.out.println ("MainCore: Got DB Connection");
                //c.close ();
                return(true);
            }
            catch (java.sql.SQLException e)
            {
                System.out.println ("MainCore: Error. Initial Failure to connect to DB: But we keep trying: " + e.getMessage ());
                log_this("MainCore: Error: Initial Failure to connect to DB: But we keep trying: " + e.getMessage());
            }
            try{Thread.sleep(10000);}catch(Exception dgff){System.out.println("err here "+dgff.getMessage());}
        }
            
    }

    private void loadProperties(String fileName)
    {
        System.out.println("Reading configuration file "+fileName+"...");
        log_this("MainCore::::Reading configuration file "+fileName+"...");
        try
        {
            FileInputStream propsFile = new FileInputStream(fileName);
            properties.load(propsFile);
            propsFile.close();
            System.out.println("MainCore: Setting default parameters...");
            log_this("MainCore::::Setting default parameters....");
            
            logging = properties.getProperty("logging");
            logtofile = properties.getProperty("logtofile");
            try{monitorport = Integer.parseInt(properties.getProperty("monitorport"));}catch(Exception e){}
            
            DBip = properties.getProperty("dbip");
            DBport = properties.getProperty("dbport");
            DBpassword = properties.getProperty("dbpassword");
            DBname = properties.getProperty("dbname");
            DBuser = properties.getProperty("dbuser");
            System.out.println("MainCore: here default parameters...");
            log_this("MainCore::::Setting dhereefault parameters....");
            
            String pingmydb = properties.getProperty("pingdb");
            try{minDBconnections = Integer.parseInt(properties.getProperty("mindbconnections"));}catch(Exception e){}
            try{maxDBconnections = Integer.parseInt(properties.getProperty("maxdbconnections"));}catch(Exception e){}
            try{pos_server = Integer.parseInt(properties.getProperty("qsvr_server_port"));}catch(Exception e){}
            try{qsvrSecureServerPort = Integer.parseInt(properties.getProperty("qsvr_server_secure_port"));}catch(Exception e){}
            
            if(pingmydb.trim().equalsIgnoreCase("true"))
            {
                pingdb = 1;
            }
            if(pingmydb.trim().equalsIgnoreCase("false"))
            {
                pingdb = 0;
            }
            
            if(logging.trim().equalsIgnoreCase("true"))
            {
                debug_mode = true;
            }
            if(logging.trim().equalsIgnoreCase("false"))
            {
                debug_mode = false;
            }
            
            if(logtofile.trim().equalsIgnoreCase("true"))
            {
                log_to_file = true;
            }
            if(logtofile.trim().equalsIgnoreCase("false"))
            {
                log_to_file = false;
            }
            
        }
        catch(Exception we)
        {
            System.out.println("Main Core: Settings error: " + we.getMessage());
            log_this("MainCore::::Settings error: " + we.getMessage());
            we.printStackTrace();
        }
    }

    public final void log_this(String sqa)
    {
        if(debug_mode)
        {
            try
            {
//                debug.write(sqa);
//                event.write(sqa);
                rl.println(sqa);
            }
            catch(Exception de){}
        }
    }

    public final void log_that(String sqa)
    {
//        if(debug_mode)
//        {
//            try
//            {
////                debug2.write(sqa);
////                event2.write(sqa);
//                tl.println(sqa);
//            }
//            catch(Exception de){}
//        }
        log_this(sqa);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
    {
        System.out.println("Starting Transceiver");

        //Start
        queue fred = new queue();
        fred.start();
    }

}
