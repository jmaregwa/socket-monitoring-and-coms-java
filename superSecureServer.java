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
import com.mchange.v2.c3p0.*;
import java.sql.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.*;
import javax.net.ssl.*;
import java.security.*;
import java.util.StringTokenizer;
import com.kaps.utils.*;
//import com.kaps.utils.*;

/**
 *
 * @author Administrator
 */
public class superSecureServer extends Thread
{
    public Socket socket = null;
    public Vector handlers = new Vector();
    public ServerSocket serverSocket = null;
    public int port;
    //public LPHMainServer lmsl;
    //public Connection c;
    //public systemLogger rl;
    //public systemLogger tl;
    public queue cocu;
    //for SSL
    //String keystore = "serverkeys";
    String keystore = "server.keystore";
    String truststore = "server.truststore";
    char keystorepass[] = "test".toCharArray();
    char keypassword[] = "testadmin".toCharArray();
    
    public superSecureServer()
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
    
    public superSecureServer(int pot, queue coco)
    {
        //serverSocket = servsock;
        //socket = sock;
        //handlers = handl;
        port = pot;
        cocu = coco;
        //rl = rla;
        //tl = tla;
    }
    
    public ServerSocket getServer() throws Exception 
    {
        KeyStore ks = KeyStore.getInstance("JKS");
        KeyStore ts = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(keystore), keystorepass);
        ts.load(new FileInputStream(truststore), keystorepass);
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
        TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
        kmf.init(ks, keypassword);
        tmf.init(ts);
        //SSLContext sslcontext = SSLContext.getInstance("SSLv3");
        SSLContext sslcontext = SSLContext.getInstance("TLS");
        sslcontext.init(kmf.getKeyManagers(), tmf.getTrustManagers(), null);
        ServerSocketFactory ssf = sslcontext.getServerSocketFactory();
        SSLServerSocket serversocket = (SSLServerSocket)ssf.createServerSocket(port);
        return serversocket;
    }
    
    @Override
    public void run() 
    {
        try 
        {
            //serverSocket = new ServerSocket(port);
            serverSocket = getServer();
            System.out.println("Successfully set up the secure qsvr server on port: " + port);
            //rl.println("Successfully set up a monitor server on port: " + port);
            //tl.println("Successfully set up a monitor server on port: " + port);
            cocu.log_that("Successfully set up the secure qsvr server on port: " + port);
            //continous loop
            while(true) 
            {
                //wait here for any connection
                socket = serverSocket.accept();
                //a thread for every connection
                Thread uzi = new Thread(new superServerThread(this));
                uzi.setDaemon(false);
                uzi.start();
            }
        } 
        catch(Exception ioe) 
        {
            System.out.println("Error: Could not set up the SECURE TLM server on port: " + port + " and error is: " + ioe.getMessage());
            cocu.log_that("Error: Could not set up the SECURE TLM server on port: " + port + " and error is: " + ioe.getMessage());
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
