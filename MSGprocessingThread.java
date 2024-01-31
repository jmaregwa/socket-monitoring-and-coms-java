/*
 * By DODGE for KAPS 28-08-2012
 * The QSVR message processor for NMK and EAPC
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
import java.text.*;
//import java.sql.DriverManager;
//import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.ResultSet;
import com.kaps.utils.systemLogger;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Waweru
 */
public class MSGprocessingThread implements Runnable 
{
    private Socket socket = null;
    private Vector handlers = new Vector();
    public Vector connections = new Vector();   
    private Vector conholder = new Vector();
    private BufferedReader in;
    private PrintWriter out;
    private boolean authorized = false;
    private String username = "";
    private int userID = 0;
    private String agentID = "";
    public Connection c; 
    public ComboPooledDataSource fbwd;
    Connection con; 
    public String line;
    public superServerThread der;
    public queue mosa;    
    public systemLogger sl;
    public String[] extractedData;
    public OutputStream outp;
    public boolean authed = false;
    public boolean isether=false;
    protected String stcommand = "";
    boolean hitmatch = false;
    
    public MSGprocessingThread(superServerThread serv) throws IOException
    {
        socket = serv.socket;
        handlers = serv.handlers;
        mosa = serv.posa;       
        con = serv.c;       
        line = serv.line;
        in = serv.in;
        out = serv.out;
        connections = serv.connectedclients;
        der = serv;
    }
    
    public MSGprocessingThread(superServerThread serv, String liner) throws IOException
    {
        socket = serv.socket;
        handlers = serv.handlers;
        mosa = serv.posa;       
        con = serv.c;      
        fbwd = serv.fbwd;       
        line = liner;
        in = serv.in;
        out = serv.out;
        connections = serv.connectedclients;
        der = serv;
    }
    
    public void run() 
    {
	try 
        {	   
                //does the client want to loggin?
                line = line.trim();
                System.out.println("Got MSG line: " + line );
                mosa.log_that("Got MSG line: " + line );      
                der.authorized = true;
                if(line.length()> 9)
                {
                    this.getPosCommand();
                    
                    
                   System.out.println("auth state : " + der.authorized );
                mosa.log_that("auth stat: " + der.authorized );      
        
                    
                
              /* if(der.authorized == false)
                {
                    
                   /*if(stcommand.equalsIgnoreCase("PA")){
                    this.getAuthorization();
                    }else{
                        
                        System.err.println("OOPs: Not Authorized Protocol.");
                        mosa.log_that("OOPs: unknown Protocol.");
                        out.println("|STX|ERROR|004|ETX|\n\r");
                        out.flush();
                }
                }
                else*/ if(der.authorized == true)
                {/*
                    //all other code goes here i.e u must be authorized
      System.out.println("is authed. check len n cmd : " + line.length() + " cmd " + stcommand);
        
                    if(line.length() > 9)
                    {
                        this.getPosCommand();*/
                        if(stcommand.equalsIgnoreCase("PA"))
                {
                    extractedData = extractTokenData(line, "|");
                    int lenof = extractedData.length;
                    System.out.println("The length of data array is: " + lenof);
                    String STX = "", POSID = "", EVENTID = "", USERNAME = "", PASSWORD = "", ETX = "";
                    if(lenof > 1)
                    {
                        STX = extractedData[1];
                        System.out.println("STX is: " + STX);
                    }
                    if(lenof > 2)
                    {
                        POSID = extractedData[2];
                        System.out.println("POSID is: " + POSID);
                    }
                    if(lenof > 3)
                    {
                        EVENTID = extractedData[3];
                        System.out.println("EVENTID is: " + EVENTID);
                    }
                    if(lenof > 4)
                    {
                        USERNAME = extractedData[4];
                        System.out.println("USERNAME is: " + USERNAME);
                    }
                    if(lenof > 5)
                    {
                        PASSWORD = extractedData[5];
                        System.out.println("PASSWORD is: " + PASSWORD);
                    }
                    if(lenof > 6)
                    {
                        ETX = extractedData[6];
                        System.out.println("ETX is: " + ETX);
                    }
                    STX = STX.trim();
                    POSID = POSID.trim();
                    EVENTID = EVENTID.trim();
                    USERNAME = USERNAME.trim();
                    PASSWORD = PASSWORD.trim();
                    ETX = ETX.trim();
                    hitmatch=true;

                    try{
                        if(createDBConnection())
                        {
                            Statement stmt = c.createStatement();
                            ResultSet rs = stmt.executeQuery("CALL P_POS_LOGIN('" + POSID + "','" + EVENTID + "','" + USERNAME +  "','" + PASSWORD +  "')");
                            System.out.println("Query to Execute : CALL P_POS_LOGIN('" + POSID + "','" + EVENTID + "','" + USERNAME +  "','" + PASSWORD +  "')");
                            mosa.log_that("Query to Execute : CALL P_POS_LOGIN('" + POSID + "','" + EVENTID + "','" + USERNAME +  "','" + PASSWORD +  "')" ); 
                            if(rs.next())
                            {
                                System.out.println("We entered the loop");
                                String rez = rs.getString("loginstatus");
                                if (rez.contains("TRUE")){
                                    der.authorized = true;
                                }
                                System.out.println("We get the info string " + rez);                                         
                                out.println(rez);                                         
                                try{out.flush();}catch(Exception er){}
                                 stmt.close();
                            }
                        }
                        else
                        {
                            System.out.println("Could not process this message. Failed to connect to DB: " + line);
                            sl.println("Could not process this message. Failed to connect to DB: " + line);
                            System.out.println("Found an error in message");
                            String outdata = "|STX|" +  POSID + "|PA|ERROR|DB_ERROR|0|ETX|\r\n\r\n\r\n";
                            outp.write(outdata.getBytes());
                            try{outp.flush();}catch(Exception er){}
                            sl.println("Sent to PLC: " + outdata);
                            //closeDBConnection();
                        }
                    }
                    catch(Exception etr)
                    {
                        System.out.println("Could not process this message Error  " + etr.getMessage());
                        sl.println("Could not process this message Error : " + etr.getMessage());
                        System.out.println("Found an error PROCESSING message");
                        String outdata = POSID + "|ERROR|DB_ERROR|\r\n\r\n\r\n";
                    }
                }
                        
                        
                      else if(stcommand.equalsIgnoreCase("PS"))
                        {
                            
                            
                             /*IF CHARGED POS SENDS TO SERVER |STX|PS|NUMPLATE|CARDNO|CARDDATETIME|TERMINALID|AMOUNT|PAYMODE|USERID|ETX| SERVER TO DEVICE
                            |STX|PS|STATE|DETAILS|ETX| |STX|PS|OK|SAVED|ETX| |STX|PS|ERROR|NO RECORD|ETX|*/
                            if (der.authorized){
                                extractedData = extractTokenData(line, "|");
                                int lenof = extractedData.length;
                                System.out.println("The length of data array is: " + lenof);
                                String EVENTID = "",ACARDNO = "", DEV_ID = "", NUMPLATE = "",CARDNO = "",CARDDATETIME = "",AMOUNT = "",CARTYPE = "", PAYMODE = "",userid="" ;

                                if(lenof > 2)
                                {
                                    DEV_ID = extractedData[2].trim();
                                    System.out.println("DEV_ID is: " + DEV_ID);
                                }
                                if(lenof > 2)
                                {
                                    EVENTID = extractedData[3].trim();
                                    System.out.println("EVENTID is: " + EVENTID);
                                }
                                if(lenof > 3)
                                {
                                    NUMPLATE = extractedData[4].trim();
                                    System.out.println("NUMPLATE is: " + NUMPLATE);
                                }
                                if(lenof > 4)
                                {
                                    CARDNO = extractedData[5].trim();
                                    System.out.println("CARDNO/TICKET NO is: " + CARDNO);
                                }
                                if(lenof > 5)
                                {
                                    CARDDATETIME = extractedData[6].trim();
                                    System.out.println("CARDDATETIME/RECEIPT NO is: " + CARDDATETIME);
                                }
                                if(lenof > 5)
                                {
                                    CARTYPE = extractedData[7].trim();
                                    System.out.println("CARTYPE is: " + CARTYPE);
                                }
                               
                                 if(lenof > 6)
                                {
                                    AMOUNT = extractedData[8].trim();
                                    System.out.println("AMOUNT is: " + AMOUNT);
                                }
                                  if(lenof > 7)
                                {
                                    PAYMODE = extractedData[9].trim();
                                    System.out.println("PAYMODE is: " + PAYMODE);
                                }
                                   if(lenof > 8)
                                {
                                    ACARDNO = extractedData[10].trim();
                                    System.out.println("ACARD is: " + ACARDNO);
                                }
                                   if(lenof > 9)
                                {
                                    userid = extractedData[11].trim();
                                    System.out.println("userid is: " + userid);
                                }
                                
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                                System.out.println("POS Payment Check event detected");
                                String dt = "",  mn = "", yy = "", hh = "", mm = "",ss = "", allDate = "";
                                hitmatch = true;
                                if(CARDDATETIME.length()>7){
                                    //    siteno = CARDDATETIME.substring(12,14);
                                    //    stationno = CARDDATETIME.substring(14);
                                    dt = CARDDATETIME.substring(4,6);
                                    mn = CARDDATETIME.substring(2,4);
                                    yy = CARDDATETIME.substring(0,2);
                                    hh = CARDDATETIME.substring(6,8);
                                    mm = CARDDATETIME.substring(8,10);
                                    ss = CARDDATETIME.substring(10,12);
                                    allDate ="20" + yy + "-" + mn + "-" + dt + " " + hh + ":" + mm + ":" + ss;
                                }
                                try{
                                     
                                    if(createDBConnection())
                                    {
                                        Statement stmt = c.createStatement();
                                        ResultSet rs = stmt.executeQuery("call p_pos_payment('" + EVENTID + "','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "','" + userid +  "')");
                                        System.out.println("Query to Execute : call p_pos_payment('PS','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "','" + userid +  "')");
                                        //('" + EVENTID + "','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "')");
                                        //System.out.println("Query to Execute :   CALL P_POS_PROCESS('PC','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "')");
                                        if(rs.next())
                                        {
                                            System.out.println("We entered the loop");
                                            String rez = rs.getString("payresult");
                                            System.out.println("We get the info string " + rez);                                         
                                            out.println(rez);                                         
                                            try{out.flush();}catch(Exception er){}
                                            
                                        }
                                        stmt.close();
                                        System.out.println("An error cause by a loop");
                                    }
                                    else
                                    {
                                        System.out.println("Could not process this message. Failed to connect to DB: " + line);
                                        sl.println("Could not process this message. Failed to connect to DB: " + line);
                                        System.out.println("Found an error in message");
                                        String outdata = "|STX|" +  DEV_ID + "|PS|ERROR|ERROR PROCESSING|0|ETX|\r\n\r\n\r\n";
                                        outp.write(outdata.getBytes());
                                        try{outp.flush();}catch(Exception er){}
                                        sl.println("Sent to PLC: " + outdata);
                                         
                                       // closeDBConnection();
                                    }
                                }
                                catch(Exception etr)
                                {
                                    System.out.println("Could not process this message Error  " + etr.getMessage());
                                    sl.println("Could not process this message Error : " + etr.getMessage());
                                    System.out.println("Found an error PROCESSING message");
                                    String outdata = DEV_ID + "|ERROR|MISSING|\r\n\r\n\r\n";
                                }
                            }
                            else{
                                System.out.println("Could not process this message Error Is NOT AUTHORIZED");
                                sl.println("Could not process this message Error Is NOT AUTHORIZED");
                                String outdata = "|STX|POS1|PS|ERROR|NOT AUTHORIZED|ETX|\r\n\r\n\r\n";
                                outp.write(outdata.getBytes());
                                try{outp.flush();}catch(Exception er){}
                                sl.println("Sent to PLC: " + outdata);
                            }

                        }
                      
                      else if(stcommand.equalsIgnoreCase("PSA"))
                        {
                            
                            
                             /*IF CHARGED POS SENDS TO SERVER |STX|PS|NUMPLATE|CARDNO|CARDDATETIME|TERMINALID|AMOUNT|PAYMODE|USERID|ETX| SERVER TO DEVICE
                            |STX|PS|STATE|DETAILS|ETX| |STX|PS|OK|SAVED|ETX| |STX|PS|ERROR|NO RECORD|ETX|*/
                            if (der.authorized){
                                extractedData = extractTokenData(line, "|");
                                int lenof = extractedData.length;
                                System.out.println("The length of data array is: " + lenof);
                                String EVENTID = "",ACARDNO = "", DEV_ID = "", NUMPLATE = "", CARDNO = "",CARDDATETIME = "",AMOUNT = "", AMOUNTA = "",AMOUNTB = "",CARTYPE = "", PAYMODE = "",userid="" ;

                                if(lenof > 2)
                                {
                                    DEV_ID = extractedData[2].trim();
                                    System.out.println("DEV_ID is: " + DEV_ID);
                                }
                                if(lenof > 2)
                                {
                                    EVENTID = extractedData[3].trim();
                                    System.out.println("EVENTID is: " + EVENTID);
                                }
                                if(lenof > 3)
                                {
                                    NUMPLATE = extractedData[4].trim();
                                    System.out.println("NUMPLATE is: " + NUMPLATE);
                                }
                                if(lenof > 4)
                                {
                                    CARDNO = extractedData[5].trim();
                                    System.out.println("CARDNO/TICKET NO is: " + CARDNO);
                                }
                                if(lenof > 5)
                                {
                                    CARDDATETIME = extractedData[6].trim();
                                    System.out.println("CARDDATETIME/RECEIPT NO is: " + CARDDATETIME);
                                }
                                if(lenof > 6)
                                {
                                    CARTYPE = extractedData[7].trim();
                                    System.out.println("CARTYPE is: " + CARTYPE);
                                }
                               
                                 if(lenof > 7)
                                {
                                    AMOUNT = extractedData[8].trim();
                                    System.out.println("AMOUNT is: " + AMOUNT);
                                }
                                 if(lenof > 8)
                                {
                                    AMOUNTB = extractedData[9].trim();
                                    System.out.println("AMOUNT before is: " + AMOUNTB);
                                }if(lenof > 9)
                                {
                                    AMOUNTA = extractedData[10].trim();
                                    System.out.println("AMOUNT after is: " + AMOUNTA);
                                }
                                 
                                  if(lenof > 10)
                                {
                                    PAYMODE = extractedData[11].trim();
                                    System.out.println("PAYMODE is: " + PAYMODE);
                                }
                                   if(lenof > 11)
                                {
                                    ACARDNO = extractedData[12].trim();
                                    System.out.println("ACARD is: " + ACARDNO);
                                }
                                   if(lenof > 12)
                                {
                                    userid = extractedData[13].trim();
                                    System.out.println("userid is: " + userid);
                                }
                                
                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                                System.out.println("POS Payment Check event detected");
                                String dt = "",  mn = "", yy = "", hh = "", mm = "",ss = "", allDate = "";
                                hitmatch = true;
                                if(CARDDATETIME.length()>7){
                                    //    siteno = CARDDATETIME.substring(12,14);
                                    //    stationno = CARDDATETIME.substring(14);
                                    dt = CARDDATETIME.substring(4,6);
                                    mn = CARDDATETIME.substring(2,4);
                                    yy = CARDDATETIME.substring(0,2);
                                    hh = CARDDATETIME.substring(6,8);
                                    mm = CARDDATETIME.substring(8,10);
                                    ss = CARDDATETIME.substring(10,12);
                                    allDate ="20" + yy + "-" + mn + "-" + dt + " " + hh + ":" + mm + ":" + ss;
                                }
                                try{
                                     
                                    if(createDBConnection())
                                    {
                                        Statement stmt = c.createStatement();
                                        ResultSet rs = stmt.executeQuery("call p_pos_paymentacard('" + EVENTID + "','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + AMOUNTB +  "','" + AMOUNTA +  "','" + PAYMODE +  "','" + ACARDNO +  "','" + userid +  "')");
                                        System.out.println("Query to Execute : call p_pos_paymentacard('PSA','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + AMOUNTB +  "','" + AMOUNTA +  "','" + PAYMODE +  "','" + ACARDNO +  "','" + userid +  "')");
                                        //('" + EVENTID + "','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "')");
                                        //System.out.println("Query to Execute :   CALL P_POS_PROCESS('PC','" + DEV_ID + "','" + NUMPLATE +  "','" + CARDNO +  "','" + CARDDATETIME + "','" + CARTYPE +  "','" + AMOUNT +  "','" + PAYMODE +  "')");
                                        if(rs.next())
                                        {
                                            System.out.println("We entered the loop");
                                            String rez = rs.getString("payresult");
                                            System.out.println("We get the info string " + rez);                                         
                                            out.println(rez);                                         
                                            try{out.flush();}catch(Exception er){}
                                            
                                        }
                                        stmt.close();
                                        System.out.println("An error cause by a loop");
                                    }
                                    else
                                    {
                                        System.out.println("Could not process this message. Failed to connect to DB: " + line);
                                        sl.println("Could not process this message. Failed to connect to DB: " + line);
                                        System.out.println("Found an error in message");
                                        String outdata = "|STX|" +  DEV_ID + "|PS|ERROR|ERROR PROCESSING|0|ETX|\r\n\r\n\r\n";
                                        outp.write(outdata.getBytes());
                                        try{outp.flush();}catch(Exception er){}
                                        sl.println("Sent to PLC: " + outdata);
                                         
                                       // closeDBConnection();
                                    }
                                }
                                catch(Exception etr)
                                {
                                    System.out.println("Could not process this message Error  " + etr.getMessage());
                                    sl.println("Could not process this message Error : " + etr.getMessage());
                                    System.out.println("Found an error PROCESSING message");
                                    String outdata = DEV_ID + "|ERROR|MISSING|\r\n\r\n\r\n";
                                }
                            }
                            else{
                                System.out.println("Could not process this message Error Is NOT AUTHORIZED");
                                sl.println("Could not process this message Error Is NOT AUTHORIZED");
                                String outdata = "|STX|POS1|PS|ERROR|NOT AUTHORIZED|ETX|\r\n\r\n\r\n";
                                outp.write(outdata.getBytes());
                                try{outp.flush();}catch(Exception er){}
                                sl.println("Sent to PLC: " + outdata);
                            }

                        }
                      else if(stcommand.equalsIgnoreCase("PC"))
                        {
                            //der.authorized = Boolean.TRUE;
                            //POS PAYMENT
                            if (der.authorized)
                                {
                                extractedData = extractTokenData(line, "|");
                                int lenof = extractedData.length;
                                System.out.println("The length of data array is: " + lenof);
                                String STX = "", EVENTID = "", DEV_ID = "", NUMPLATE = "", CARDNO = "", CARDDATETIME = "", PAYMODE = "", CARTYPE = "", REFID = "", ETX = "";

                                if(lenof > 1)
                                {
                                    STX = extractedData[1].trim();
                                    System.out.println("STX is: " + STX);
                                }
                                if(lenof > 2)
                                {
                                    DEV_ID = extractedData[2].trim();
                                    System.out.println("DEV_ID is: " + DEV_ID);
                                }
                                if(lenof > 2)
                                {
                                    EVENTID = extractedData[3].trim();
                                    System.out.println("EVENTID is: " + EVENTID);
                                }
                                if(lenof > 3)
                                {
                                    NUMPLATE = extractedData[4].trim();
                                    System.out.println("NUMPLATE is: " + NUMPLATE);
                                }
                                if(lenof > 4)
                                {
                                    CARDNO = extractedData[5].trim();
                                    System.out.println("CARDNO/TICKET NUMBER is: " + CARDNO);
                                }
                                if(lenof > 5)
                                {
                                    CARDDATETIME = extractedData[6].trim();
                                    System.out.println("CARDDATETIME/RECEIPT NO is: " + CARDDATETIME);
                                }
                                if(lenof > 6)
                                {
                                    CARTYPE = extractedData[7].trim();
                                    System.out.println("CARTYPE is: " + CARTYPE);
                                }
//                                if(lenof > 7)
//                                {
//                                    PAYMODE = extractedData[8].trim();
//                                    System.out.println("PAYMODE is: " + PAYMODE);
//                                }
//                                if(lenof > 8)
//                                {
//                                    REFID = extractedData[9];
//                                    System.out.println("REFID is: " + REFID);
//                                }
                                if(lenof > 7)
                                {
                                    ETX = extractedData[8];
                                    System.out.println("Here I am");
                                    System.out.println("ETX is: " + ETX);
                                }

                                Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                                System.out.println("POS Payment Check event detected");
                                String siteno = "";
                                String stationno = "";
                                String dt = "", mn = "", yy = "", hh = "", mm = "", ss = "", allDate = "";
                                hitmatch = true;
                                if(CARDDATETIME.length()>7){
                                    siteno = CARDDATETIME.substring(12,14);
                                    stationno = CARDDATETIME.substring(14);
                                    dt = CARDDATETIME.substring(4,6);
                                    mn = CARDDATETIME.substring(2,4);
                                    yy = CARDDATETIME.substring(0,2);
                                    hh = CARDDATETIME.substring(6,8);
                                    mm = CARDDATETIME.substring(8,10);
                                    ss = CARDDATETIME.substring(10,12);
                                    allDate ="20" + yy + "-" + mn + "-" + dt + " " + hh + ":" + mm + ":" + ss;
                                }
                                try{
                                    if(createDBConnection())
                                    {
                                        Statement stmt = c.createStatement();
                                        ResultSet rs = stmt.executeQuery(" CALL P_POS_PROCESS('" + DEV_ID +  "','" + EVENTID +  "','" + CARDNO +  "','" + CARDDATETIME +  "','" + NUMPLATE +  "','" + CARTYPE + "')");
                                        System.out.println("Query to Execute :   CALL P_POS_PROCESS('" + DEV_ID +  "','" + EVENTID +  "','" + CARDNO +  "','" + CARDDATETIME +  "','" + NUMPLATE +  "','" + CARTYPE + "')");
                                        //('" + DEV_ID +  "','" + EVENTID +  "','" + CARDNO +  "','" + CARDDATETIME +  "','" + NUMPLATE +  "','" + CARTYPE + "')");
                                        //System.out.println("Query to Execute : call p_pos_payment('" + DEV_ID +  "','" + EVENTID +  "','" + CARDNO +  "','" + CARDDATETIME +  "','" + NUMPLATE +  "','" + CARTYPE + "')");
                                        
                                        if(rs.next())
                                        {
                                            String rez = rs.getString("EXITRESULT");                                          
                                            out.println(rez);
                                            try{out.flush();}catch(Exception er){}
                                            stmt.close(); 
                                       }
                                    }
                                
                          
                                    else
                                    {
                                        System.out.println("Could not process this message. Failed to connect to DB: " + line);
                                        sl.println("Could not process this message. Failed to connect to DB: " + line);
                                        System.out.println("Found an error in message");
                                        String outdata = "|STX|" +  DEV_ID + "|PC|ERROR|ERROR PROCESSING|0|ETX|\r\n\r\n\r\n";
                                        outp.write(outdata.getBytes());
                                        try{outp.flush();}catch(Exception er){}
                                        sl.println("Sent to PLC: " + outdata);
                                        //closeDBConnection();
                                    }
                                }
                                catch(Exception etr) // oiur procedure to do parking not exit. so we do the normal one
                                {
                                    etr.printStackTrace();
                                    System.out.println("Could not process this message Error  " + etr.getMessage());
                                    sl.println("Could not process this message Error : " + etr.getMessage());
                                    System.out.println("Found an error PROCESSING message");
                                    String outdata = DEV_ID + "|ERROR|MISSING||ETX|\r\n\r\n\r\n";
                                }
                            }
                        
                            else
                            {
                                System.out.println("Could not process this message Error Is NOT AUTHORIZED");
                                sl.println("Could not process this message Error Is NOT AUTHORIZED");
                                String outdata = "|STX|POS1|PC|ERROR|NOT AUTHORIZED||ETX|\r\n\r\n\r\n";
                                outp.write(outdata.getBytes());
                                try{outp.flush();}catch(Exception er){}
                                sl.println("Sent to PLC: " + outdata);
                            }
                        
                    }
                        else
                    {
                        System.err.println("OOPs: unknown Protocol.");
                        mosa.log_that("OOPs: unknown Protocol.");
                        out.println("|STX|||ERROR|004||ETX|\n\r");
                        out.flush();
                    }
                        
                }
                   else
                            {
                                System.out.println("Could not process this message Error Is NOT AUTHORIZED");
                                sl.println("Could not process this message Error Is NOT AUTHORIZED");
                                String outdata = "|STX|POS1|AUTH|ERROR|NOT AUTHORIZED||ETX|\r\n\r\n\r\n";
                                outp.write(outdata.getBytes());
                                try{outp.flush();}catch(Exception er){}
                                sl.println("Sent to PLC: " + outdata);
                            }
                        //*************************************END OF ADDITION PS BY BOSS LOGIC FROM LGM******************//
                        //************************SMART QUEING***************************************************// 
                           
                      
                    }
                   else
                    {
                        System.err.println("OOPs: Too short a message.");
                        mosa.log_that("OOPs: Too short a message.");
                         mosa.log_that("|STX|pos|g|ERROR|002||ETX|\n\r");
                        out.println("|STX|||ERROR|002||ETX|\n\r");
                        out.flush();
                    }
                //}
	    //}
	}
        catch(Exception ie) 
        {
            System.err.println("General Error: " + ie.getMessage());
            mosa.log_that("General Error: " + ie.getMessage());
            mosa.log_that("|STX|pos|g|error|003||ETX|\n\r");
	    ie.printStackTrace();
            out.println("|STX|||error|003||ETX|\n\r");
            out.flush();
        }
        finally 
        {
	    try 
            {
                //c.close();
		//in.close();
		//out.close();
		//socket.close();
	    } 
            //catch(IOException ioe)
            //{
                //keep quiet
            //}
            catch(Exception ie) 
            {
                System.out.println("Error ending thread: " + ie.getMessage());
            }
            finally
            {
                try{connections.removeElement(conholder);}catch(Exception err){}
            }
	}
        try{c.close();}catch(Exception err){}
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
                /*System.out.println("DB connections before[external] : " + connections.size());
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
                mosa.log_that("Error: Failed to connect to DB: But we keep trying: " + em.getMessage());
                em.printStackTrace();
            }
        }
    }

    public String[] extractTokenData(String datas, String delimiter)
    {
        String[] ret = {""};      
        String[] result = datas.split("[" + delimiter + "]"); 
        for(String rsa : result){
             System.out.println(rsa);System.out.println("Result Array Above");
             
        }
       
        System.out.println("Result Array Above");
        return(result);
    }
    
     private  void getAuthorization() throws SQLException{
           if(stcommand.equalsIgnoreCase("PA"))
                {
                    extractedData = extractTokenData(line, "|");
                    int lenof = extractedData.length;
                    System.out.println("The length of data array is: " + lenof);
                    String STX = "", POSID = "", EVENTID = "", USERNAME = "", PASSWORD = "", ETX = "";
                    if(lenof > 1)
                    {
                        STX = extractedData[1];
                        System.out.println("STX is: " + STX);
                    }
                    if(lenof > 2)
                    {
                        POSID = extractedData[2];
                        System.out.println("POSID is: " + POSID);
                    }
                    if(lenof > 3)
                    {
                        EVENTID = extractedData[3];
                        System.out.println("EVENTID is: " + EVENTID);
                    }
                    if(lenof > 4)
                    {
                        USERNAME = extractedData[4];
                        System.out.println("USERNAME is: " + USERNAME);
                    }
                    if(lenof > 5)
                    {
                        PASSWORD = extractedData[5];
                        System.out.println("PASSWORD is: " + PASSWORD);
                    }
                    if(lenof > 6)
                    {
                        ETX = extractedData[6];
                        System.out.println("ETX is: " + ETX);
                    }
                    STX = STX.trim();
                    POSID = POSID.trim();
                    EVENTID = EVENTID.trim();
                    USERNAME = USERNAME.trim();
                    PASSWORD = PASSWORD.trim();
                    ETX = ETX.trim();
                    hitmatch=true;

                    try{
                        if(createDBConnection())
                        {
                            Statement stmt = c.createStatement();
                            ResultSet rs = stmt.executeQuery("CALL P_POS_LOGIN('" + POSID + "','" + EVENTID + "','" + USERNAME +  "','" + PASSWORD +  "')");
                            System.out.println("Query to Execute : CALL P_POS_LOGIN('" + POSID + "','" + EVENTID + "','" + USERNAME +  "','" + PASSWORD +  "')");
                            if(rs.next())
                            {
                                System.out.println("We entered the loop");
                                String rez = rs.getString("loginstatus");
                                if (rez.contains("TRUE")){
                                    der.authorized = true;
                                }
                                System.out.println("We get the info string " + rez);                                         
                                out.println(rez);                                         
                                try{out.flush();}catch(Exception er){}
                                 stmt.close();
                            }
                        }
                        else
                        {
                            System.out.println("Could not process this message. Failed to connect to DB: " + line);
                            sl.println("Could not process this message. Failed to connect to DB: " + line);
                            System.out.println("Found an error in message");
                            String outdata = "|STX|" +  POSID + "|PA|ERROR|DB_ERROR|0|ETX|\r\n\r\n\r\n";
                            outp.write(outdata.getBytes());
                            try{outp.flush();}catch(Exception er){}
                            sl.println("Sent to PLC: " + outdata);
                            //closeDBConnection();
                        }
                    }
                    catch(Exception etr)
                    {
                        System.out.println("Could not process this message Error  " + etr.getMessage());
                        sl.println("Could not process this message Error : " + etr.getMessage());
                        System.out.println("Found an error PROCESSING message");
                        String outdata = POSID + "|ERROR|DB_ERROR|\r\n\r\n\r\n";
                    }
                }
    }
     
     private void getPosCommand(){
        System.out.println("Got line: " + line + " and command is: " + line.substring(0, 7));
        System.out.println("Got proc line: " + line);
        extractedData = extractTokenData(line, "|");
        int lenofa = extractedData.length;        
        if(lenofa > 3)
        {
            stcommand = extractedData[3];
        }
        System.out.println("Got terminal MSG line: " + line + " and command is: " + stcommand);
        mosa.log_that("Got terminal MSG line: " + line + " and command is: " + stcommand);
        System.err.println("Got command as: " + stcommand);
     }
    
}
