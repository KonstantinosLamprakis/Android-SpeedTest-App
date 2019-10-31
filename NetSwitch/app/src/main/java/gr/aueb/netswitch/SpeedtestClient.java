/*
* SpeedtestClient class represents a client that communicate with a server, send and receive a file and make a ping.
* A SpeedtestClient object's constructor takes as arguments ip and port of server, a file to transmit and a path for storing receiving file.
**/

package gr.aueb.netswitch;

import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.Callable;


class SpeedtestClient implements Callable<Boolean>{

    // Initialization.
    private static final int size = 1024*1024; // size of buffer for reading and writing files.
    private byte[] buffer = new byte[size]; // buffer stores temporary parts of a file for reading and writing.

    private String ip; // ip of server.
    private int port; // port of server.
    private File file; // file which is going to transmit.
    private String storePath; // path that stores the file of receiving.

    private boolean pingCompleted = false;
    private boolean receiveCompleted = false;
    private boolean transmitCompleted = false;

    private long latency = 0;
    private long uplpoadMs = 0;
    private long downloadMs = 0;

    // Constructor.
    SpeedtestClient(String ip, int port, File file, String storePath){
        this.file = file;
        this.ip = ip;
        this.port = port;
        this.storePath = storePath;
    }

    // run execute whenever a thread created.
    public Boolean call(){

        // get latency (ping).
        try{
            sendOperation("ping");

            // 1st way(ICMP):
            latency = getLatencyICMP(InetAddress.getByName(ip));
            if(latency == -1){
                Log.e("error","Server is not reachable");

                // 2nd way(TCP Sockets) in case of ICMP failure.
                latency = getLatencyTCP();
            }else{
                getLatencyTCP();
                Log.i("info", "latency: " + latency);
            }
            pingCompleted = true;
            Log.i("info", "ping-pong completed.\n");
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }

        // transmit file to client to measure download speed.
        try{
            sendOperation("server_receive");
            uplpoadMs = transmission();
            transmitCompleted = true;
            Log.i("info", "client_transmit completed completed.\n");
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }

        // receive file from client to measure upload speed.
        try{
            sendOperation("server_transmit");
            downloadMs = receipt();
            receiveCompleted = true;
            Log.i("info", "client_receive completed.\n");
        }catch(IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // sendOperation sends a string message to server, to ask for a specific service. Valid String values are "ping", "server_receive", "server_transmit".
    private void sendOperation(String operation) throws IOException{

        Log.i("info", "\nsend operation: " + operation +".");
        Socket serverSocket = new Socket(ip, port);
        OutputStream os = serverSocket.getOutputStream();
        Writer writer = new PrintWriter(os);
        writer.write(operation);
        writer.flush();

        writer.close();
        os.close();
        serverSocket.close();
    }

    // getLatencyICMP returns latency at ms(as long) and use ICMP frames.
    private long getLatencyICMP(InetAddress serverAddress) throws IOException{
        boolean isReachable;
        long start = System.currentTimeMillis();
        isReachable = serverAddress.isReachable(100);// tries ICMP(and if failure tries TCP at port 7)
        long stop = System.currentTimeMillis();
        if(!isReachable) return -1;
        return stop-start; // ms
    }

    // getLatencyTCP returns latency at ms(as long) and use TCP sockets.
    private long getLatencyTCP() throws IOException{

        long start = System.currentTimeMillis();
        Socket serverSocket = new Socket(ip, port);
        BufferedReader receiveAnswer = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
        String pong = receiveAnswer.readLine();
        long stop = System.currentTimeMillis();

        receiveAnswer.close();
        serverSocket.close();
        if(!pong.equals("pong")) throw new IOException();
        return stop - start;
    }

    // transmission handles whole process of file transmission(for upload speed).
    private long transmission() throws IOException {

        Log.i("info", "Begins Transmission Part.");
        Socket serverSocket = new Socket(ip, port);

        BufferedInputStream bfis = null; // read from file(at local pc) which is going to transmit.
        OutputStream os = null; // transmit file to server.

        // open file.
        try {
            FileInputStream fis = new FileInputStream(file);
            bfis = new BufferedInputStream(fis);
            Log.i("info", "File: " + file.getAbsolutePath() + " is opened and ready to send.");
        } catch (FileNotFoundException e) {
            Log.e("error", "File for transfer does'nt locate.");
            e.printStackTrace();
            throw new IOException();// to return false the call method.
        }

        // transmit file.
        long start = System.currentTimeMillis();
        while (!Thread.currentThread().isInterrupted()) {

            int current = bfis.read(buffer, 0, size);// current holds temporary buffer data until whole file has transmitted.
            if (current == -1) {
                break;
            }
            // write and send file to client.
            os = serverSocket.getOutputStream();
            os.write(buffer,0,current);
            os.flush();
        }

        long stop = System.currentTimeMillis();
        Log.i("info", "Transfer complete from client side.");

        bfis.close();
        if(os != null) os.close();
        serverSocket.close();

        return stop - start;
    }

    // receipt handles whole process of file receipt(for download speed).
    private long receipt() throws IOException{

        Log.i("info", "Begins Receiving Part.");
        Socket serverSocket = new Socket(ip, port);
        FileOutputStream fos = null; // stores the file that receives from server.

        // open path for storing received file from client.
        try{
            fos = new FileOutputStream(storePath + File.separator + "receivedFile.txt");
            Log.i("info", "File: "+ storePath + File.separator + "receivedFile.txt" + " is opened for storing.");
        }catch(FileNotFoundException e){
            Log.e("error", "Error opening file at path: " + storePath + File.separator + "receivedFile.txt");
            e.printStackTrace();
            throw new IOException();
        }

        // receive and store this file.
        InputStream is = serverSocket.getInputStream(); // for receiving file.
        if(fos == null) throw new IOException();
        BufferedOutputStream bos = new BufferedOutputStream(fos); // for writing-storing file through FileOutputStream.
        int current; // holds temporary 1024*1024 data until whole file received.
        long start = System.currentTimeMillis();
        while(((current = is.read(buffer)) > 0) && (!Thread.currentThread().isInterrupted())){
            bos.write(buffer,0,current);
        }
        long stop = System.currentTimeMillis();
        bos.flush();
        Log.i("info", "Receiving complete from client side.");

        fos.close();
        bos.close();
        serverSocket.close();
        return stop - start;
    }

    long getLatency() {
        if(pingCompleted) return latency;
        return 0;
    }

    long getUplpoadMs() {
        if(transmitCompleted) return uplpoadMs;
        return 0;
    }

    long getDownloadMs() {
        if(receiveCompleted) return downloadMs;
        return 0;
    }
}


