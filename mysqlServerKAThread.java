/*
 * To keep the mysql connection alive we query after every 2 minutes
 */

package com.kaps;

//import org.firebirdsql.pool.*;
//import org.apache.commons.dbcp.BasicDataSource;
import com.mchange.v2.c3p0.*;
//import java.sql.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.kaps.utils.*;
import java.sql.Connection;
//import java.sql.DriverManager;
//import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

/**
 *
 * @author Waweru
 */
public class mysqlServerKAThread implements Runnable 
{
//    private Socket socket = null;
//    private Vector handlers = new Vector();
      public Vector connections = new Vector();
      private Vector conholder = new Vector();
//    private BufferedReader in;
//    private PrintWriter out;
//    private boolean authorized = false;
//    private String username = "";
//    private int userID = 0;
//    private String agentID = "";
    public Connection c;
    //public FBWrappingDataSource dataSource;
    //public BasicDataSource fbwd;
    public ComboPooledDataSource fbwd;
    public Connection con;
    public systemLogger sl;
//    public userAuth ua;
//    public String line;
//    public superServerThread der;
    
    public mysqlServerKAThread(ComboPooledDataSource dataSource, systemLogger sla)
    {
//        socket = serv.socket;
//        handlers = serv.handlers;
//        //c = serv.c;
//        //dataSource = serv.dataSource;
//        con = serv.c;
//        ua = serv.uas;
//        fbwd = serv.fbwd;
          sl = sla;
//        line = liner;
//        in = serv.in;
//        out = serv.out;
//        connections = serv.connectedclients;
//        der = serv;
        fbwd = dataSource;
    }
    
    public void run() 
    {
        while(true)
        {
            try
            {
                Statement stmt = null;
                ResultSet rs = null;
                if(createDBConnection())
                {
                    stmt = c.createStatement();
                    //rs = stmt.executeQuery("select * from terminals where terminal_id='" + user + "' and password='" + pass + "'");
                    //String testquery = "select * from transactions";
                    //String testquery = "select * from transactions where trans_time = (select max(trans_time) from transactions)";
                    String testquery = "select * from users";
                    //System.out.println("Execute query: " + authquery);
                    //sl.println("Execute query: " + authquery);
                    rs = stmt.executeQuery(testquery);
                    if(rs.next())
                    {
                        System.out.println("DB keep alive success");
                        sl.println("DB keep alive success");
                    }
                    conholder.removeElement(c);
                    connections.removeElement(conholder);
                    //this was the breaking point earlier
                    try{rs.close();}catch(Exception gf){}
                    try{stmt.close();}catch(Exception gf){}
                    try{c.close();}catch(Exception gf){}
                }
            }
            catch(Exception ie)
            {
                System.err.println("DB Error: " + ie.getMessage());
                sl.println("DB Error: " + ie.getMessage());
//                StringWriter swe = new StringWriter();
//                PrintWriter pwe = new PrintWriter(swe);
//                ie.printStackTrace(pwe);
//                sl.println(swe.toString());
                ie.printStackTrace();
            }
            try
            {
                Thread.sleep(60000);
            }
            catch(Exception err){}
        }
    }
    
    public boolean createDBConnection()
    {
        //try to get a connection from the pool
        //We try until we get it
        while(true)
        {
            try
            {
                ///**
                c = fbwd.getConnection();
                System.out.println("DB connections before[external] : " + connections.size());
                synchronized(connections) 
                {
                    conholder.addElement(c);
                    connections.addElement(conholder);
                    System.out.println("DB connections after[external] : " + connections.size());
                }
                 //**/
                return(true);
            }
            catch(Exception em)
            {
                System.out.println("Error: Failed to connect to DB: But we keep trying: " + em.getMessage());
                sl.println("Error: Failed to connect to DB: But we keep trying: " + em.getMessage());
                em.printStackTrace();
            }
        }
    }

    public String[] extractTokenData(String datas, String delimiter)
    {
        String[] ret = {""};
        ////StringTokenizer st = new StringTokenizer(datas, delimiter);
        ////while (st.hasMoreTokens())
        ////{
            ////System.out.println(st.nextToken());
        ////}
        String[] result = datas.split("[" + delimiter + "]");
        ////for (int x=0; x<result.length; x++)
        ////{
            ////System.out.println(result[x]);
        ////}
        return(result);
    }
    
}
