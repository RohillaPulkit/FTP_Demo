import java.net.*;
import java.io.*;

public class Client {

    private static final String clientDirectory = "Directory_Client";

    private Socket requestSocket;           //socket connect to the server
    private DataOutputStream dataOutputStream;         //stream write to the socket
    private DataInputStream dataInputStream;         //stream read from the socket

    private FileTransferUtility fileTransferUtility;
    private boolean isAuthenticated;

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
            requestSocket = new Socket();
            requestSocket.connect(new InetSocketAddress(inputAddress.ipAddress, inputAddress.portNumber), 1000);

            System.out.println("Connected to "+inputAddress.ipAddress.getHostAddress()+" in port "+inputAddress.portNumber);

            //initialize dataInputStream and outputStream
            dataOutputStream = new DataOutputStream(requestSocket.getOutputStream());
            dataOutputStream.flush();
            dataInputStream = new DataInputStream(requestSocket.getInputStream());

            fileTransferUtility = new FileTransferUtility(dataOutputStream, dataInputStream);

            //get Input from standard input
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            while(requestSocket.isConnected())
            {
                String serverCommand = dataInputStream.readUTF();
                System.out.print(serverCommand);
                String input = bufferedReader.readLine();

                if (!isAuthenticated){

                    sendString(input);

                    int status = dataInputStream.readInt();

                    if (status == Authentication.STATUS_CODE_AUTHENTICATED){
                        isAuthenticated = true;
                        System.out.println("Authentication Successful!");
                    }
                    else if(status == Authentication.STATUS_CODE_AUTHENTICATION_FAILED){
                        System.out.println("Authentication Failed!");
                    }
                }
                else
                {
                    //Send the command to the server
                    sendString(input);

                    prepareForResponse();
                }

            }
        }
        catch (ConnectException ex) {
            System.out.println(MessageStrings.serverNotInitiated);
        }
        catch(UnknownHostException unknownHost){
            System.err.println(MessageStrings.unknownHost);
        }
        catch(IOException ioException){
            System.out.println(MessageStrings.unreachableHost);
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

    private void sendString(String command) {
        try{
            //stream write the message
            dataOutputStream.writeUTF(command);
            dataOutputStream.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void sendInt(int statusCode) {
        try{
            dataOutputStream.writeInt(statusCode);
            dataOutputStream.flush();
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private void prepareForResponse() throws IOException {

        int statusCode = dataInputStream.readInt();
        InputCommand.Type commandType = InputCommand.Type.values()[statusCode];

        switch (commandType) {
            case DIR:
                getDirectoryContent();
                break;
            case GET:
                String downloadFileName = dataInputStream.readUTF();
                initiateFileDownload(downloadFileName);
                break;
            case UPLOAD:
                String uploadFileName = dataInputStream.readUTF();
                initiateFileUpload(uploadFileName);
                break;
            default:
                System.out.println(MessageStrings.invalidCommand);
                break;
        }
    }

    private void getDirectoryContent() throws IOException{
        String response = dataInputStream.readUTF();
        System.out.println(response);
    }

    private void initiateFileDownload(String fileName) throws IOException{

        int statusCode = dataInputStream.readInt();

        if (statusCode == FileTransferUtility.STATUS_CODE_FILE_FOUND){

            fileTransferUtility.receiveFileData(fileName, clientDirectory);
        }
        else if(statusCode == FileTransferUtility.STATUS_CODE_FILE_NOT_FOUND)
        {
            System.out.println(MessageStrings.fileNotFound);
        }
    }

    private void initiateFileUpload(String fileName) throws IOException{

        File file = new File(fileName);

        if(file.exists() && !file.isDirectory()) {

            sendInt(FileTransferUtility.STATUS_CODE_FILE_FOUND);

            fileTransferUtility.sendFileData(file);
        }
        else
        {
            sendInt(FileTransferUtility.STATUS_CODE_FILE_NOT_FOUND);

            System.out.println(MessageStrings.fileNotFound);
        }
    }
}
