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
public class monitorServerListenMornitor implements Runnable 
{
    private Socket socket = null;
    private Vector handlers = new Vector();
    private BufferedReader in;
    private PrintWriter out;
    public ByteArrayOutputStream baos;
    public monitorServerThread cli;
    
    public monitorServerListenMornitor(monitorServerThread serv) throws IOException
    {
        cli = serv;
    }

    public void run() 
    {
        try
        {
            String tryme = "";
            System.err.println("We now start listening for client disconnect. This typically causes an error trying to read.");
            while(!(tryme=cli.in.readLine()).trim().equalsIgnoreCase("quit"))
            {
                //do nothing
                //we want it to throw an error
            }
        }
        catch(Exception ett)
        {
            cli.keepalive = false;
        }
    }
    
}
