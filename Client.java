import java.net.*;
import java.io.*;

public class Client {

    private static final String clientDirectory = "Directory_Client";

    private Socket requestSocket;           //socket connect to the server
    private DataOutputStream dataOutputStream;         //stream write to the socket
    private DataInputStream dataInputStream;         //stream read from the socket

    private FileTransferUtility fileTransferUtility;

    public static void main(String args[]){

        InputAddress inputAddress = new InputAddress(args);
        if (inputAddress.isValid){

            Client ftpClient = new Client();
            ftpClient.initWithAddress(inputAddress);
        }
        else
        {
           exit();
        }
    }

    public static void exit(){

        System.out.println(MessageStrings.invalidClientArgs);
        System.exit(0);
    }

    private void initWithAddress(InputAddress inputAddress){
        try{
            //create a socket to connect to the server
            requestSocket = new Socket(inputAddress.ipAddress, inputAddress.portNumber);

            System.out.println("Connected to "+inputAddress.ipAddress.getHostAddress()+" in port "+inputAddress.portNumber);

            //initialize dataInputStream and outputStream
            dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());
            dataOutputStream.flush();
            dataInputStream = new DataInputStream(requestSocket.getInputStream());

            fileTransferUtility = new FileTransferUtility(dataOutputStream, dataInputStream);

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(true)
            {
                System.out.print("Please input a command to run: ");

                //read a command from the standard input
                String request = bufferedReader.readLine();

                //Send the command to the server
                handleRequest(request);
            }
        }
        catch (ConnectException ex) {
            System.out.println(MessageStrings.serverNotInitiated);
        }
        catch(UnknownHostException unknownHost){
            System.err.println(MessageStrings.unknownHost);
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
        finally{
            closeConnection();
        }
    }

    private void closeConnection(){
        try{
            if (dataInputStream != null)
                dataInputStream.close();

            if (dataOutputStream != null)
                dataOutputStream.close();

            if (requestSocket != null)
                requestSocket.close();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void sendRequest(String command) {
        try{
            //stream write the message
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void handleRequest(String request) throws IOException {

        InputCommand command = new InputCommand(request);

        switch (command.type) {
            case DIR:
                getDirectoryContent(request);
                break;
            case GET:
                initiateFileDownload(command.fileName, request);
                break;
            case UPLOAD:
                initiateFileUpload(command.fileName, request);
                break;
            default:
                System.out.println(MessageStrings.invalidCommand);
                break;
        }
    }

    private void getDirectoryContent(String request) throws IOException{
        sendRequest(request);
        String response = dataInputStream.readUTF();
        System.out.println(response);
    }

    private void initiateFileDownload(String fileName,String request) throws IOException{

        sendRequest(request);

        String status = dataInputStream.readUTF();

        if (status.equals(FileTransferUtility.status200)){
            fileTransferUtility.receiveFileData(fileName, clientDirectory);
        }
        else if(status.equals(FileTransferUtility.status404))
        {
            System.out.println(MessageStrings.fileNotFound);
        }
    }

    private void initiateFileUpload(String fileName, String request) throws IOException{

        File file = new File(fileName);

        if(file.exists() && !file.isDirectory()) {

            sendRequest(request);

            fileTransferUtility.sendFileData(file);
        }
        else
        {
            System.out.println(MessageStrings.fileNotFound);
        }
    }
}
