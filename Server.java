import java.net.*;
import java.io.*;

public class Server {

    static final int maxConnections = 100;

    ServerSocket serverSocket = null;   //serversocket used to listen on port number 8000
    Socket clientSocket = null;   //socket for the connection with the client

    public static void main(String args[]) {

        InputAddress inputAddress = new InputAddress(args);

        if (inputAddress.isValid){

            Server ftpServer = new Server();
            ftpServer.initWithAddress(inputAddress);
        }
        else
        {
            exit();
        }
    }

    public static void exit(){

        System.out.println(MessageStrings.invalidServerArgs);
        System.exit(0);
    }

    private void initWithAddress(InputAddress inputAddress) {
        try{

            //create a serversocket
            serverSocket = new ServerSocket(inputAddress.portNumber, maxConnections, inputAddress.ipAddress);
            System.out.println("Server started at "+inputAddress.ipAddress.getHostAddress()+":"+inputAddress.portNumber+", Waiting for connection...");

            //accept a connection from the client
            while(true){

                try{
                    clientSocket = serverSocket.accept();
                    System.out.println("Connection received from " + clientSocket.getInetAddress().getHostAddress());

                }catch (IOException ex){
                    ex.printStackTrace();
                }

                new ServerThread(clientSocket).start();
            }

        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            //Close connections
            try{
                serverSocket.close();
                clientSocket.close();
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }
}

