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
 * @author Waweru
 */
public class monitorServerThread implements Runnable 
{
    private Socket socket = null;
    //private Vector handlers = new Vector();
    public BufferedReader in;
    public PrintWriter out;
    private boolean authorized = false;
    private String username = "";
    private int userID = 0;
    //public Connection c;
    public ByteArrayOutputStream baos;
    public monitorServer cli;
    public boolean keepalive = true;
    //public consoleSimulator cs;
    public systemLogger rl;
    public systemLogger tl;
    
    public monitorServerThread(monitorServer serv) throws IOException
    {
        socket = serv.socket;
        //handlers = serv.handlers;
        cli = serv;
        rl = serv.rl;
        tl = serv.tl;
        //c = serv.c;
        //socket.setKeepAlive(false);
        //socket.setSoLinger(false, 0);
        //socket.setSoTimeout(1);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()));
    }
    
    //This retrieves the last line of the system out messages
    //Its here that we also control the size of the output buffer by maintaining it at 100 line always
    //deprecated
    private String getLastLine(BufferedReader in) 
    { 
        String line=null, tmp;
        int mon = 0;
        try 
        {
            while ((tmp = in.readLine()) != null)
            {
                line = tmp ;
                mon++;
            }
            if(mon > 100)
            {
                //Reset the output buffer: let hope we are not writting into it
                baos.reset();
            }
        }
        catch (IOException e) 
        {
            //do nothing
        } 
        return line ;
    }

    
    public void run() 
    {
        //throw new UnsupportedOperationException("Not supported yet.");
        String line = "";
        String tmpline = "";
        String tmpline2 = "";
        System.out.println("MONITOR: Connected to IP : " + socket.getInetAddress().getHostAddress() + " at port: " + socket.getPort());
        //System.out.println("Clients connected before : " + handlers.size());
        //store the original output stream
        PrintStream origOut = System.out;
//        try
//        {
//            cs = new consoleSimulator(cli.lmsl.getMSGField());
//        }
//        catch(Exception eww){}
        baos = new ByteArrayOutputStream();
        PrintStream outer = new PrintStream(baos, true);
        //PrintStream outer = new PrintStream(cs, true);
        //System.setOut(outer);
//	synchronized(handlers)
//        {
//	    handlers.addElement(this);
//            System.err.println("Clients connected after : " + handlers.size());
//	}
	try 
        {
            //a read thread for every connection to keep us alive
            Thread uzi = new Thread(new monitorServerListenMornitor(this));
            uzi.setDaemon(true);
            uzi.start();
	    while(keepalive) 
            {
                //if((line = getLastLine(new BufferedReader(new InputStreamReader(new ByteArrayInputStream(baos.toByteArray()))))) != null && !tmpline.equalsIgnoreCase(line))
                if(((line = rl.dogetlastline()) != null))
                {
                    if(!tmpline.equalsIgnoreCase(line.trim()))
                    {
                        line = line.trim();
                        line = line + "\n\r\n\r\n\r";
                        out.println(line);
                        out.flush();
                        tmpline = line.trim();
                        System.err.println("Mornitor sending : " + line);
                    }
                }

                if(((line = tl.dogetlastline()) != null))
                {
                    if(!tmpline2.equalsIgnoreCase(line.trim()))
                    {
                        line = line.trim();
                        line = line + "\n\r\n\r\n\r";
                        out.println(line);
                        out.flush();
                        tmpline2 = line.trim();
                        System.err.println("Mornitor sending : " + line);
                    }
                }
	    }
	} 
        //catch(IOException ioe) 
        //{
            //System.err.println("Error: " + ioe.getMessage());
	    //ioe.printStackTrace();
	//}
        catch(Exception ie) 
        {
            System.err.println("Error: Monitor application error: " + ie.getMessage());
	    ie.printStackTrace();
        }
        finally 
        {
	    try 
            {
                baos.reset();
		in.close();
		out.close();
		socket.close();
	    } 
            catch(IOException ioe) 
            {
                //keep quiet
            }
            catch(Exception ie) 
            {
                //keep quiet
            }
//            finally
//            {
//		synchronized(handlers)
//                {
//                    System.err.println("Clients disconnected before: " + handlers.size());
//		    handlers.removeElement(this);
//                    System.err.println("Clients disconnected after: " + handlers.size());
//		}
//	    }
	}
        //PrintStream ou = new PrintStream(cs, true);
        //System.setOut(ou);
    }
    
}
