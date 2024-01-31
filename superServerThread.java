/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kaps;

//import org.firebirdsql.pool.*;
//import org.apache.commons.dbcp.BasicDataSource;
import com.mchange.v2.c3p0.*;
import java.sql.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import com.kaps.utils.*;

/**
 *
 * @author Waweru
 */
public class superServerThread implements Runnable
{
    public Socket socket = null;
    public Vector handlers = new Vector();
    public Vector msgHandlers = new Vector();
    public Vector connectedclients = new Vector();
    public int msgViolationCounter = 0;
    public BufferedReader in;
    public InputStream in2;
    //private csBufferedReader into;
    public PrintWriter out;
    public OutputStream out2;
    //private boolean authorized = false;
    //private String username = "";
    //private int userID = 0;
    public Connection c;
    //public systemLogger sl;
    //public FBWrappingDataSource fbwd;
    //public BasicDataSource fbwd;
    public ComboPooledDataSource fbwd;
    //public userAuth uas;
    public String liner = "";
    public String line = "";
    public String[] extractedData;
    public long graceperiod = 15;
    public boolean gracecheck = false;
    public long freetimeperiod = 4;
    public boolean freetimecheck = false;
    private boolean goon = true;
    private java.util.Date lastMessageTime = new java.util.Date(); // The last time a msg was sent
    private int connectionGracePeriod = 5; // The max time period given for a connection to be idle
    private boolean notremoved = true; // Indicates whether this connection has been removed from handler
    public boolean authorized = false;
    public boolean agent_authorized = false;
    public String programVersion = "QSVR V1.4.0 Build 16 (09/11/2011)";
    public queue posa;
    
    public String IP = "";
    public String myNetAddress = "";
    
    public superServerThread(superServer serv) throws IOException
    {
        socket = serv.socket;
        handlers = serv.handlers;
        //connectedclients = serv.connectedclients;
        //sl = serv.sl;
        posa = serv.cocu;
        fbwd = posa.dataSource;
        //uas = serv.uas;
        //graceperiod = serv.graceperiod;
        //gracecheck = serv.gracecheck;
        //freetimeperiod = serv.freetimeperiod;
        //freetimecheck = serv.freetimecheck;
        in2 = socket.getInputStream();
        in = new BufferedReader(new InputStreamReader(in2));
        //into = new csBufferedReader(new InputStreamReader(socket.getInputStream()));
        out2 = socket.getOutputStream();
	out = new PrintWriter(new OutputStreamWriter(out2));
        
        //This thread will ensure that the connection is terminated if idle for a given time
        Thread uzi8 = new Thread(new Runnable() 
        {
            public void run() 
            {
                while(goon)
                {
                    checkIdleness();
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception err){}
                }
            }
        });
        uzi8.setDaemon(false);
        uzi8.start();
    }
    
    public superServerThread(superSecureServer serv) throws IOException
    {
        socket = serv.socket;
        handlers = serv.handlers;
        //connectedclients = serv.connectedclients;
        //sl = serv.sl;
        posa = serv.cocu;
        fbwd = posa.dataSource;
        //uas = serv.uas;
        //graceperiod = serv.graceperiod;
        //gracecheck = serv.gracecheck;
        //freetimeperiod = serv.freetimeperiod;
        //freetimecheck = serv.freetimecheck;
        in2 = socket.getInputStream();
        in = new BufferedReader(new InputStreamReader(in2));
        //into = new csBufferedReader(new InputStreamReader(socket.getInputStream()));
        out2 = socket.getOutputStream();
	out = new PrintWriter(new OutputStreamWriter(out2));
        
        //This thread will ensure that the connection is terminated if idle for a given time
        Thread uzi8 = new Thread(new Runnable() 
        {
            public void run() 
            {
                while(goon)
                {
                    checkIdleness();
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch(Exception err){}
                }
            }
        });
        uzi8.setDaemon(false);
        uzi8.start();
    }
    
    public boolean createDBConnection()
    {
        //try to get a connection from the pool
        try
        {
            c = fbwd.getConnection();
            return(true);
        }
        catch(Exception em)
        {
            System.out.println("Error: Failed to connect to DB: " + em.getMessage());
            posa.log_that("Error: Failed to connect to DB: " + em.getMessage());
            em.printStackTrace();
        }
        return(false);
    }
    
    //preprocess the message
    //just in case the message has picked some noize on the way
    public String preprocessMsg(String msg)
    {
        msg = msg.trim();
        if(msg.startsWith("|STX|") && msg.endsWith("|ETX|"))
        {
            return(msg);
        }
        else if(msg.indexOf("|STX|") != -1 && msg.indexOf("|ETX|") != -1)
        {
            int ft = msg.indexOf("|STX|");
            msg = msg.substring(ft);
            int et = msg.indexOf("|ETX|");
            msg = msg.substring(0, et+5);
        }
        return(msg);
    }
    
    //Make sure the message is in the correct format
    public boolean checkMSG(String msg)
    {
        int len = msg.length();
        if(len > 0)
        {
            if(msg.substring(1, 4).equalsIgnoreCase("STX"))
            {
                if(msg.substring(len-4, len-1).equalsIgnoreCase("ETX"))
                {
                    if(msg.startsWith("|STX|") && msg.endsWith("|ETX|"))
                    {
                        return(true);
                    }
                }
            }
        }
        System.err.println("This string failed to satisfy our requirements: " + msg);
        return(false);
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
    
    //remove a string from a string
    public String removeFromMSG(String main, String submain)
    {
        //main = main.replaceFirst("/"+submain+"/", "");
        main = main.trim();
        submain = submain.trim();
        int rt = main.indexOf(submain);
        //String part1 = main.substring(0, rt);
        String part1 = "";
        String part2 = main.substring(rt+submain.length());
        String mabo = part1 + part2;
        return(mabo);
    }
    
    public void run() 
    {  
        //Lets see who this is
        System.out.println("Testing if socket is bound to address. For connection limit test");
        if(socket.isBound())
        {
            System.out.println("Client connected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
            posa.log_that("Client connected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
        }
        IP = socket.getInetAddress().getHostAddress().toLowerCase().trim();
        ////if(checkIfAlreadyConnected(socket.getInetAddress().getHostAddress()))
        if(checkIfAlreadyConnected(IP))    
        {
            //OOps, you are already connected
//            System.out.println("Oops too many connections from you, rejecting this one : " + socket.getInetAddress().getHostAddress());
//            posa.log_that("Oops too many connections from you, rejecting this one : " + socket.getInetAddress().getHostAddress());
//            out.println("Oops too many connections from you. I will reject this one. Try again later.\n\r");
//            out.flush();
//            try
//            {
//                in.close();
//                out.close();
//                in2.close();
//                out2.close();
//                socket.close();
//            }
//            catch(Exception err){}
            boolean foundThread = false;
            
            System.err.println("OOPS: Found previous connection from this IP: " + IP);
            posa.log_that("OOPS: Found previous connection from this IP: " + IP);
            
            //search through the connections vector for the connecting IP
            System.out.println("Checking for thread if already connected: " + IP);
            posa.log_that("Checking for thread if already connected: " + IP);
            Enumeration eff =  handlers.elements();
            while(eff.hasMoreElements())
            {                    
                try
                {
                    superServerThread fsma = (superServerThread)eff.nextElement();
                    String fsm = fsma.myNetAddress;
                    fsm = fsm.toLowerCase().trim();
                    if(IP.equalsIgnoreCase(fsm))
                    {
                        System.out.println("Found Thread of address already connected: " + IP);
                        posa.log_that("Found Thread of address already connected: " + IP);
                        foundThread = true;
                        fsma.terminateConnection();
                        System.err.println("Finished removing previous connection");
                    }
                }
                catch(Exception elf)
                {
                    System.err.println("An error occured while searching for connected clients Thread: " + elf.getMessage());
                    posa.log_that("An error occured while searching for connected clients Thread: " + elf.getMessage());
                }
            }
            if(!foundThread)
            {
                //This should never happen
                System.err.println("OOPS: Did not Find Thread of address already connected: " + IP);
                posa.log_that("OOPS: Did not Find Thread of address already connected: " + IP);
            }
        }
        else
        {
            System.out.println("Found no previous connection from this IP: " + IP);
            posa.log_that("Found no previous connection from this IP: " + IP);
        }
        //else
        {
            //Great, you are not connected already
            //Lets limit the number of concurent connections to 20
            if(handlers.size() >= 100)
            {
                System.out.println("Oops too many connections, rejecting this one : IP: " + socket.getInetAddress().getHostAddress() + " Total Connections: " + handlers.size());
                out.println("Oops too many connections. I will reject this one. Try again later.\n\r");
                out.flush();
                try{in.close();}catch(Exception err){}
                try{out.close();}catch(Exception err){}
                try{in2.close();}catch(Exception err){}
                try{out2.close();}catch(Exception err){}
                try{socket.close();}catch(Exception err){}
            }
            else
            {
                System.out.println("QUEUE Server Threads active before : " + handlers.size());
                //synchronized(handlers) 
                {
                    handlers.addElement(this);
                    //connectedclients.addElement(socket.getInetAddress().getHostAddress());
                    myNetAddress = socket.getInetAddress().getHostAddress();
                    System.err.println("Socket String 2: " + socket.toString());
                    System.out.println("Client Threads active after (New Connection) : " + handlers.size());
                }
                readData3();
            }
        }
    }
    
    public void readData3()
    {
        try 
        {
            out.println("#@200# Welcome to KAPS POS_QUEUE Host Server\n\r");
            out.println("#@201# Connection will close if left idle\n\r\n\r");
            out.flush();
            while(((liner = in.readLine()) != null) && goon)
            {
                liner = liner.trim();
                lastMessageTime = new java.util.Date();
                if(msgHandlers.size() == 1)
                {
                    msgViolationCounter++;
                }
                if(msgViolationCounter > 4)
                {
                    //if a client continuously violates the one message at a time rule, close the connection
                    goon = false;
                    System.out.println("Connection misbehaving too much. Now terminating.\n\r\n\r");
                    posa.log_that("Connection misbehaving too much. Now terminating.\n\r\n\r");
                    out.println("Connection misbehaving too much. Now terminating.\n\r\n\r");
                    out.flush();
                    try 
                    {
                        socket.close();
                        in.close();
                        socket.close();
                        out.close();
                        socket.close();
                        in2.close();
                        socket.close();
                        out2.close();
                        socket.close();
                    } 
                    catch(Exception ie) 
                    {
                        //keep quiet
                    }
                }
                else
                {
                    Thread uzi = new Thread(new MSGprocessingThread(this, liner));
                    uzi.setDaemon(false);
                    uzi.start();
                }
            }
        }
        catch(Exception em)
        {
            System.out.println("Error2: Cannot read from socket: " + em.getMessage());
            em.printStackTrace();
        }
        finally
        {
            System.out.println("APP Station Threads active before : " + handlers.size());
            synchronized(handlers) 
            {
                handlers.removeElement(this);
                connectedclients.removeElement(socket.getInetAddress().getHostAddress());
                System.out.println("APP Station Threads active after : " + handlers.size());
            }
            System.out.println("Client disconnected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
            posa.log_that("Client disconnected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
        }
    }
    
    public void checkIdleness()
    {
        java.util.Date timenow = new java.util.Date();
        long last = lastMessageTime.getTime();
        long now = timenow.getTime();
        long timediffmillisec = now - last;
        //System.out.println("Idleness Check: Time diff millis: " + timediffmillisec);
        long timediffsec = (timediffmillisec/1000);
        timediffsec = Math.abs(timediffsec);
        //System.out.println("Idleness Check: Time diff seconds: " + timediffsec);
        long timediffmin = (timediffsec/60);
        //System.out.println("Idleness Check: Time diff minutes: " + timediffmin);
        if(timediffmin >= connectionGracePeriod + 1)
        {
            //This connection has exceeded the allowed idleness time and will be terminated
            System.out.println("Terminating a connection for being idle");
            posa.log_that("Terminating a connection for being idle");
            goon = false;
            out.println("#@202# Connection too idle. Now terminating.\n\r\n\r");
            out.flush();
            try 
            {
                socket.close();
                in.close();
                socket.close();
		out.close();
                socket.close();
                in2.close();
                socket.close();
		out2.close();
		socket.close();
            } 
            catch(Exception ie) 
            {
                //keep quiet
            }
            finally 
            {
                //This should only run once
                if(notremoved)
                {
                    synchronized(handlers) 
                    {
                        System.out.println("Clients connected before this: " + handlers.size());
                        handlers.removeElement(this);
                        connectedclients.removeElement(socket.getInetAddress().getHostAddress());
                        System.out.println("Client Just disconnected: Clients connected after this: " + handlers.size());
                        posa.log_that("Client Just disconnected: Clients connected after this: " + handlers.size());
                    }
                    notremoved = false;
                }
	    }
        }
    }
    
    public boolean checkIfAlreadyConnected(String IP)
    {
        boolean pes = false;
        IP = IP.toLowerCase().trim();
        //search through the connections vector for the connecting IP
        System.out.println("Checking if already connected: " + IP);
        posa.log_that("Checking if already connected: " + IP);
        Enumeration eff = handlers.elements();
        while(eff.hasMoreElements())
        {                    
            try
            {
                superServerThread fsma = (superServerThread)eff.nextElement();
                String fsm = fsma.myNetAddress;
                fsm = fsm.toLowerCase().trim();
                if(IP.equalsIgnoreCase(fsm))
                {
                    pes = true;
                    System.err.println("Found address already connected: " + IP);
                    posa.log_that("Found address already connected: " + IP);
                    return(pes);
                }
            }
            catch(Exception elf)
            {
                System.out.println("An error occured while searching for connected clients IPs: " + elf.getMessage());
                posa.log_that("An error occured while searching for connected clients IPs: " + elf.getMessage());
            }
        }
        return(pes);
    }
    
    public void terminateConnection()
    {
        System.out.println("Terminating connection");
        posa.log_that("Terminating connection");
        System.err.println("Terminal Socket String: " + socket.toString());
        try
        {
//            in.close();
//            out.close();
//            in2.close();
//            out2.close();
            socket.close();
        }
        catch(Exception err)
        {
            System.out.println("OOPS: Error Terminating connection");
            posa.log_that("OOPS: Error Terminating connection");
        }
        System.err.println("Client Threads active before (Previous Con Termination) : " + handlers.size());
        //System.err.println("Connected Clients before (Previous Con Termination) : " + connectedclients.size());
        //synchronized(handlers) 
        {
            handlers.removeElement(this);
            //connectedclients.removeElement(socket.getInetAddress().getHostAddress());
            System.err.println("Client Threads active after (Previous Con Termination) : " + handlers.size());
            //System.err.println("Connected Clients after (Previous Con Termination) : " + connectedclients.size());
        }
        System.out.println("Client disconnected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
        posa.log_that("Client disconnected from address : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
    }
    
}
