/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kaps;

import java.util.*;
import java.io.*;
import com.kaps.utils.*;
//import com.mchange.v2.c3p0.*;
import org.firebirdsql.pool.*;
import java.sql.*;

/**
 *
 * @author patnox
 */
public class firebirdDBprocessor
{
    public String dbip = "";
    public String dbport = "";
    public String dbpath = "";
    public String dbuser = "";
    public String dbpass = "";
    public queue tisa = null;
    //public ComboPooledDataSource dataSource = null;
    public FBWrappingDataSource dataSource = null;
    public Connection c;

    public firebirdDBprocessor(queue rewa, String dbipa, String dbporta, String dbpatha, String dbusera, String dbpassa)
    {
        //constructor
        dbip = dbipa;
        dbport = dbporta;
        dbpath = dbpatha;
        dbuser = dbusera;
        dbpass = dbpassa;
        tisa = rewa;
        //dataSource = new ComboPooledDataSource();
        dataSource = new FBWrappingDataSource();
        init();
    }

    public final void init()
    {
        try
        {
            dataSource.setDatabase (dbip + "/" + dbport + ":" + dbpath);
            dataSource.setType("PURE_JAVA");
            dataSource.setEncoding("ISO8859_1");
            dataSource.setPooling(true);
            //dataSource.setMaxPoolSize(tisa.maxDBconnections);
            //dataSource.setMinPoolSize(tisa.minDBconnections);
            dataSource.setLoginTimeout(1200);
            dataSource.setBlockingTimeout(1200);
            dataSource.setUserName(dbuser);
            dataSource.setPassword(dbpass);
        }
        catch(Exception ew)
        {
            System.err.println("Error trying to initialize datasource: " + ew.getMessage());
            tisa.log_that("Error trying to initialize datasource: " + ew.getMessage());
            ew.printStackTrace();
        }
    }

    public boolean connectToDB()
    {
        // Connect to the Firebird DataSource
        // Try to Connect to the Firebird DataSource
        //We try for upto 4 minutes
        int counter = 0;
        while(true)
        {
            try
            {
                dataSource.setLoginTimeout (1200);
                //c = dataSource.getConnection ("sysdba", "patrick");
                //c = dataSource.getConnection ("sysdba", "a");
                c = dataSource.getConnection (dbuser, dbpass);
                //c = dataSource.getConnection();

//                Statement stmt = c.createStatement();
//                ResultSet rs = stmt.executeQuery("SELECT * FROM users");
//                while(rs.next())
//                //System.out.println("a1 = " + rs.getString(1) + ", a2 = " + rs.getString(2));
//                System.out.println("MainCore: Registered user: " + rs.getString("name"));
//                stmt.close();

                // At this point, there is no implicit driver instance
                // registered with the driver manager!
                System.out.println ("MainCore: Got DB Connection");
                //c.close ();
                return(true);
            }
            catch (java.sql.SQLException e)
            {
                System.out.println ("MainCore: Error. Initial Failure to connect to DB: But we keep trying: " + e.getMessage ());
                tisa.log_this("MainCore: Error: Initial Failure to connect to DB: But we keep trying: " + e.getMessage());
            }
            counter++;
            //sleep for a minute
            try{Thread.sleep(50000);}catch(Exception ex){}
            //Break when we reach 4 minutes
            if(counter >= 4)
            {
                break;
            }
        }
        return(false);
    }

    public String processAmountQuery(String ticketno)
    {
        String reta = "|STX|ZM|ERROR|001|ETX|";
        if (connectToDB())
        {
            //Process the query
            Statement stmt = null;
            ResultSet rs = null;
            try
            {
                stmt = c.createStatement();
                //String query = "select getparkingfee('" + ticketno + "') as amount;";
                String query = "select amountia as amount from getparkingfee('" + ticketno + "')";
                System.err.println("Checking parking balance with: " + query);
                tisa.log_this("Checking parking balance with: " + query);
                rs = stmt.executeQuery(query);
                int amount = 0;
                if(rs.next())
                {
                    amount = rs.getInt("amount");
                    if(amount > 0)
                    {
                        reta = "|STX|ZM|OK|" + amount + "|ETX|";
                    }
                    else
                    {
                        reta = "|STX|ZM|ERROR|008|ETX|";
                    }
                }
                else
                {
                    reta = "|STX|ZM|ERROR|008|ETX|";
                }
                stmt.close();
                
            }
            catch(Exception de)
            {
                //Error
                System.err.println("An error occured while processing amount query: " + de.getMessage());
                tisa.log_this("An error occured while processing amount query: " + de.getMessage());
                de.printStackTrace();
                reta = "|STX|ZM|ERROR|012|ETX|";
            }
        }
        return(reta);
    }

    public String processPaymentRequest(String ticketno, int amo)
    {
        String reta = "|STX|ZT|ERROR|001|ETX|";
        if (connectToDB())
        {
            //Process the query
            Statement stmt = null;
            ResultSet rs = null;
            try
            {
                stmt = c.createStatement();
                String query = "select confirm from payparkingfee('" + ticketno + "','" + amo + "')";
                System.err.println("Checking parking balance with: " + query);
                tisa.log_this("Checking parking balance with: " + query);
                rs = stmt.executeQuery(query);
                if(rs.next())
                {
                    int rept = rs.getInt("confirm");
                    if(rept == 0)
                    {
                        reta = "|STX|ZT|OK|ETX|";
                    }
                    else if(rept == 6)
                    {
                        reta = "|STX|ZT|ERROR|021|ETX|";
                    }
                    else if(rept == 3)
                    {
                        reta = "|STX|ZT|ERROR|022|ETX|";
                    }
                    else if(rept == 4)
                    {
                        reta = "|STX|ZT|ERROR|023|ETX|";
                    }
                    else if(rept == 5)
                    {
                        reta = "|STX|ZT|ERROR|024|ETX|";
                    }
                    else if(rept == 1)
                    {
                        reta = "|STX|ZT|ERROR|025|ETX|";
                    }
                    else
                    {
                        reta = "|STX|ZT|ERROR|009|ETX|";
                    }
                }
                else
                {
                    reta = "|STX|ZT|ERROR|011|ETX|";
                }
                stmt.close();
            }
            catch(Exception de)
            {
                //Error
                System.err.println("An error occured while processing payment request: " + de.getMessage());
                tisa.log_this("An error occured while processing payment request: " + de.getMessage());
                de.printStackTrace();
                reta = "|STX|ZT|ERROR|013|ETX|";
            }
        }
        return(reta);
    }
}
