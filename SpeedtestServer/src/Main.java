import java.io.File;
import java.io.RandomAccessFile;

public class Main {
    public static void main(String args[]){
        try
        {
            // create a file for transmission.
            String path = System.getProperty("user.home") + File.separator + "Desktop" + File.separator +"transmitFile.txt";
            new File(path).delete();
            RandomAccessFile file=new RandomAccessFile(path,"rw");
            file.setLength(1024*1024*5); // 5 MB

            // run server.
            Runnable server = new Server(System.getProperty("user.home") + File.separator + "Desktop",5050, new File(path));
            Thread serverThread = new Thread(server);
            serverThread.start();

        }catch (Exception e){
            System.err.println("Exception at main");
            e.printStackTrace();
        }

    }
}
