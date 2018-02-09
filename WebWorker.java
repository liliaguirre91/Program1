/**
 * Web worker: an object of this class executes in its own new thread
 * to receive and respond to a single HTTP request. After the constructor
 * the object executes on its "run" method, and leaves when it is done.
 *
 * One WebWorker object is only responsible for one client connection.
 * This code uses Java threads to parallelize the handling of clients:
 * each WebWorker runs in its own thread. This means that you can essentially
 * just think about what is happening on one client at a time, ignoring
 * the fact that the entirety of the webserver execution might be handling
 * other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the
 * client interaction is done. The "run()" method is the beginning -- think
 * of it as the "main()" for a client interaction. It does three things in
 * a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it
 * writes out some HTML content for the response content. HTTP requests and
 * responses are just lines of text (in a very particular format).
 *
 **/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable
{
    
    private Socket socket;
    
    /**
     * Constructor: must have a valid open socket
     **/
    public WebWorker(Socket s)
    {
        socket = s;
    }
    
    /**
     * Worker thread starting point. Each worker handles just one HTTP
     * request and then returns, which destroys the thread. This method
     * assumes that whoever created the worker created it with a valid
     * open socket object.
     **/
    public void run()
    {
        String contentType = "";
        String HTTP_Path = "";
        
        System.err.println("Handling connection...");
        try {
            InputStream  is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            HTTP_Path = readHTTPRequest(is);
            
            if (HTTP_Path.toLowerCase().contains(".gif"))
                contentType = "image/gif";
            else if (HTTP_Path.toLowerCase().contains(".jpeg"))
                contentType = "image/jpeg";
            else if (HTTP_Path.toLowerCase().contains(".png"))
                contentType = "image/png";
            else if (HTTP_Path.toLowerCase().contains("ico"))
                contentType = "image/x-icon";
            else
                contentType = "text/html";
            
            writeHTTPHeader(os,contentType, HTTP_Path);
            writeContent(os,contentType, HTTP_Path);
            os.flush();
            socket.close();
        } catch (Exception e) {
            System.err.println("Output error: "+e);
        }
        System.err.println("Done handling connection.");
        return;
    }
    
    /**
     * Read the HTTP request header.
     **/
    private String readHTTPRequest(InputStream is)
    {
        String line;
        String path = "";
        int i = 0;
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        while (true) {
            try {
                while (!r.ready()) Thread.sleep(1);
                line = r.readLine();
                
                if (line.contains("GET ")) {
                    //System.out.println("FOUND!");
                    path= line.substring(4);
                    System.out.println(path);
                    
                    while (!(path.charAt(i) == ' ')) {
                        i++;
                    }
                    path= path.substring(0,i);
                    //System.out.println(path);
                }
                System.err.println("Request line: ("+line+")");
                if (line.length()==0) break;
            } catch (Exception e) {
                System.err.println("Request error: "+e);
                break;
            }
        }
        return path;
    }
    
    /**
     * Write the HTTP header lines to the client network connection.
     * @param os is the OutputStream object to write to
     * @param contentType is the string MIME content type (e.g. "text/html")
     **/
    private void writeHTTPHeader(OutputStream os, String contentType, String path) throws Exception
    {
        String HTTP_Path = path;
        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        //Tries opening a file and throws an exception if it's not found.
        //this also changes the status to 404
        try {
            FileReader reader = new FileReader (HTTP_Path);
            BufferedReader n = new BufferedReader (reader);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + HTTP_Path);
            os.write("HTTP/1.1 404 Not Found\n".getBytes());
        }
        
        //If the file is found then the status stays as 200
       
        os.write("HTTP/1.1 200 OK\n".getBytes());
        os.write("Date: ".getBytes());
        os.write((df.format(d)).getBytes());
        os.write("\n".getBytes());
        os.write("Server: Lili's final frontier".getBytes());
        //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
        //os.write("Content-Length: 438\n".getBytes());
        os.write("Connection: close\n".getBytes());
        os.write("Content-Type: ".getBytes());
        os.write(contentType.getBytes());
        os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
        return;
    }
    
    /**
     * Write the data content to the client network connection. This MUST
     * be done after the HTTP header has been written out.
     * @param os is the OutputStream object to write to
     **/
    private void writeContent(OutputStream os,String contentType, String HTTP_Path) throws Exception
    {
        String line;
        File file = new File(HTTP_Path);
        Date d = new Date();
        DateFormat df = DateFormat.getDateTimeInstance();
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        
        if (contentType.equals("text/html")) {
        
            //Try reading from the file and throw an exception if the file isn't found
            try {
                FileReader reader = new FileReader (file);
                BufferedReader n = new BufferedReader (reader);
                
                //Read the next line while there is a line to read
                while ((line = n.readLine()) != null) {
                    
                    os.write(line.getBytes());
                    os.write("\n".getBytes());
                    
                    //Prints the date when it encounters this tag
                    if (line.contains("<cs371date>"))
                        os.write((df.format(d)).getBytes());
                    
                    //Prints "Lili's 371 server" when it encounters this tag
                    if (line.contains("<cs371server>"))
                        os.write("<br>\nLili's Final Frontier</br>".getBytes());
                }//end while
                
            }catch(FileNotFoundException e) {             //If the file is not found it displays 404 message
                System.err.println("File not found: " + HTTP_Path);
                os.write ("<h1>404 Not Found</h1>\n".getBytes());
            }
        } //end if
        
        else if (contentType.contains("image")) {
            
            FileInputStream imageInputReader = new FileInputStream (file);
            //System.out.println(HTTP_Path);
            byte byteArray [] = new byte [(int)file.length()];
            imageInputReader.read(byteArray);
            
            DataOutputStream imageOutput = new DataOutputStream(os);
            imageOutput.write(byteArray);
            
        }
    }
    
} // end class


