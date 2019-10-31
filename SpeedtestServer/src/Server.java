/*
* Server class is a representation of speedtest server application, that receives and transmits a file to a client and measure upload and download speed.
* */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import static java.lang.System.exit;

public class Server implements Runnable{

    // Initialization.
    private final int port; // port that server is going to listen and bind.
    private Socket clientSock;
    private ServerSocket server;

    private static final int size = 1024*1024; // size of buffer is independent of size of file.
    private byte[] buffer = new byte[size]; // buffer for read and write ti file.

    private FileOutputStream fos; // stores the file that receives from client(for upload speed).
    private BufferedInputStream bfis = null;

    private String storePath; // path that stores the file of receiving.
    private File file; // file for transmission.

    // Public Methods.

    // Constructor.
    public Server(String storePath, int port, File file){
        this.storePath = storePath;
        this.port = port;
        this.file = file;
    }

    // run execute whenever a thread create.
    public void run(){
        while (true) {
            try {
                // start server.
                server = new ServerSocket(port);
                System.out.println("\nServer socket is listening at port: " + port + ".");

                // 3 clients only accept(ping - transmit - receive) and then close server.
                for (int i = 0; i < 3; i++) {

                    String operation = receiveOperation();

                    // Connect to a client.
                    clientSock = server.accept();

                    if (operation.equals("server_receive")) {
                        //------------------------------------- Receive file from client to measure upload speed.-------------------------------------
                        System.out.println("Begins Receiving Part.");

                        // open path for storing received file from client.
                        try {
                            fos = new FileOutputStream(storePath + File.separator + "receivedFile.txt");
                            System.out.println("File: " + storePath + File.separator + "receivedFile.txt" + " is opened for storing.");
                        } catch (FileNotFoundException e) {
                            System.err.println("Error opening file at path: " + storePath + File.separator + "receivedFile.txt" + ".\n\n");
                            e.printStackTrace();
                            break;
                        }

                        // receive and store this file.
                        InputStream is = clientSock.getInputStream(); // for receiving file.
                        BufferedOutputStream bos = new BufferedOutputStream(fos); // for writing-storing file through FileOutputStream.
                        int current; // holds temporary 1024*1024 data until whole file received.
                        while (((current = is.read(buffer)) > 0) && (!Thread.currentThread().isInterrupted())) {
                            bos.write(buffer, 0, current);
                        }
                        bos.flush();
                        System.out.println("Receiving complete from server side.");

                        fos.close();
                        bos.close();
                    } else if (operation.equals("server_transmit")) {
                        //------------------------------------- Transmit file to client to measure download speed.-------------------------------------
                        System.out.println("Begins Transmission Part.");
                        openFile(file); // open files and send files.
                        transmitFile(); // transmit file to client.
                    } else if (operation.equals("ping")) {
                        System.out.println("Begins Ping-Pong Part.");
                        OutputStream os = clientSock.getOutputStream();
                        Writer writer = new PrintWriter(os);
                        writer.write("pong");
                        writer.flush();
                        writer.close();
                        os.close();
                        System.out.println("Ping-Pong completed from server side.");
                    } else {
                        System.err.println("Invalid operation");
                    }
                    clientSock.close();
                }
                server.close();
            } catch (IOException e) {
                System.err.println("An error occured.");
                e.printStackTrace();
                try{
                    server.close();
                }catch (Exception ex){
                    ex.printStackTrace();
                }
            }
        }
    }

    // openFile open and sends all files.
    private void openFile(File file) throws IOException{
        try {
            FileInputStream fis = new FileInputStream(file);
            bfis = new BufferedInputStream(fis);
            System.out.println("File: " + file.getAbsolutePath() + " is opened and ready to send.");
        } catch (FileNotFoundException e) {
            System.err.println("File for transfer does'nt locate.\n\n");
            e.printStackTrace();
            exit(1);
        }
    }

    // sendFiles send one file per time.
    private void transmitFile() throws IOException{
        OutputStream os = null;
        // read file from computer.
        while (!Thread.currentThread().isInterrupted()) {
            int current = bfis.read(buffer, 0, size);// current holds temporary buffer data until whole file has transmitted.
            if (current == -1) {
                break;
            }
            // write and send file to client.
            os = clientSock.getOutputStream();
            os.write(buffer,0,current);
            os.flush();
        }
        System.out.println("Transfer complete from server side.");
        bfis.close();
        if(os != null) os.close();
    }

    // receiveName receive the name and suffix of the file.
    private String receiveOperation()throws IOException {
        Socket clientSock = server.accept();
        System.out.println("\nConnect with client " + clientSock.getInetAddress().getHostAddress() + ".");
        BufferedReader receiveName = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
        String operation = receiveName.readLine();
        receiveName.close();
        clientSock.close();
        System.out.println("Operation is: " + operation + ".");
        return operation;
    }
}
