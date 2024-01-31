/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.kaps;

//import org.firebirdsql.pool.*;
//import org.apache.commons.dbcp.BasicDataSource;
//import java.sql.*;
import java.io.*;
import java.net.*;
import java.util.*;
import com.kaps.utils.*;

/**
 *
 * @author Administrator
 */
public class monitorServer extends Thread
{
    public Socket socket = null;
    public Vector handlers = new Vector();
    public ServerSocket serverSocket = null;
    public int port;
    //public LPHMainServer lmsl;
    //public Connection c;
    public systemLogger rl;
    public systemLogger tl;
    
    public monitorServer()
    {
        //empty
    }
    
//    public monitorServer(ServerSocket servsock, Socket sock, Vector handl, int pot, LPHMainServer lms)
//    {
//        serverSocket = servsock;
//        socket = sock;
//        handlers = handl;
//        port = pot;
//        lmsl = lms;
//        //c = ca;
//        sl = lms.sl;
//    }
//
//    public monitorServer(ServerSocket servsock, Socket sock, Vector handl, int pot)
//    {
//        serverSocket = servsock;
//        socket = sock;
//        handlers = handl;
//        port = pot;
//    }
    
    public monitorServer(int pot, systemLogger rla, systemLogger tla)
    {
        //serverSocket = servsock;
        //socket = sock;
        //handlers = handl;
        port = pot;
        rl = rla;
        tl = tla;
    }
    
    @Override
    public void run() 
    {
        try 
        {
            serverSocket = new ServerSocket(port);
            System.out.println("Successfully set up a monitor server on port: " + port);
            rl.println("Successfully set up a monitor server on port: " + port);
            tl.println("Successfully set up a monitor server on port: " + port);
            //continous loop
            while(true) 
            {
                //wait here for any connection
                socket = serverSocket.accept();
                //a thread for every connection
                Thread uzi = new Thread(new monitorServerThread(this));
                uzi.setDaemon(false);
                uzi.start();
            }
        } 
        catch(IOException ioe) 
        {
            ioe.printStackTrace();
        } 
        finally 
        {
            try 
            {
                serverSocket.close();
            } catch(IOException ioe) 
            {
                ioe.printStackTrace();
            }
        }
    }
}
