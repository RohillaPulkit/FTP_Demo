import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class ServerThread extends Thread {

    private static final String serverDirectory = "Directory_Server";

    protected Socket socket;
    private DataOutputStream dataOutputStream;  //stream write to the socket
    private DataInputStream dataInputStream;    //stream read from the socket

    private FileTransferUtility fileTransferUtility;
    private boolean isAuthenticated;

    public ServerThread(Socket clientSocket) {
        this.socket = clientSocket;
    }

    @Override
    public void run() {
        //initialize Input and Output streams
        try{

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            dataOutputStream.flush();
            dataInputStream = new DataInputStream(socket.getInputStream());

            fileTransferUtility = new FileTransferUtility(dataOutputStream, dataInputStream);

            while(socket.isConnected())
            {
                if (!isAuthenticated){

                    sendString("Please log in using UserName and Password: ");

                    String credentials = dataInputStream.readUTF();

                    System.out.println("Received credentials: " + credentials);

                    isAuthenticated = Authentication.isAuthenticated(credentials);

                    if (isAuthenticated){

                        sendInt(Authentication.STATUS_CODE_AUTHENTICATED);
                    }
                    else
                    {
                        sendInt(Authentication.STATUS_CODE_AUTHENTICATION_FAILED);
                    }
                }
                else
                {
                    sendString("Please input a command to run: ");

                    //receive the request sent from the client
                    String request = dataInputStream.readUTF();

                    //trim whitespaces
                    request = request.trim();

                    System.out.println("Received request: " + request);

                    handleRequest(request);
                }
            }
        }
        catch (IOException ex){

            ex.printStackTrace();
//            System.out.println("Client "+ this.socket.getInetAddress().getHostAddress() + " disconnected");
        }
    }

    //send a response to the output stream
    private void sendString(String response) {
        try{
            dataOutputStream.writeUTF(response);
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

    private void handleRequest(String request) throws IOException{

        InputCommand command = new InputCommand(request);

        switch (command.type) {
            case DIR:
                sendInt(InputCommand.Type.DIR.ordinal());
                getDirectoryContent();
                break;
            case GET:
                sendInt(InputCommand.Type.GET.ordinal());
                sendString(command.fileName);
                initiateFileUpload(command.fileName);
                break;
            case UPLOAD:
                sendInt(InputCommand.Type.UPLOAD.ordinal());
                sendString(command.fileName);
                initiateFileDownload(command.fileName);
                break;
            default:
                sendInt(InputCommand.Type.INVALID.ordinal());
                break;
        }
    }

    private void getDirectoryContent(){

        Path serverPath = Paths.get(serverDirectory);

        try{

            final int chunkSize = 3;
            final AtomicInteger counter = new AtomicInteger();

            String contents = Files
                    .list(serverPath)
                    .map(path -> path.getFileName().toString())
                    .sorted((String s1, String s2) -> s1.compareToIgnoreCase(s2))
                    .map(str -> String.format("%-20s", str))    // Padding string on right
                    .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / chunkSize))
                    .values()
                    .stream()
                    .map(list -> String.join(" ", list))
                    .collect(Collectors.joining("\n"));

            sendString(contents);
        }
        catch (IOException ex){

            sendString("Unable to get directory content. Please try again later.");
        }

    }

    private void initiateFileDownload(String fileName) throws IOException{
        int statusCode = dataInputStream.readInt();

        if (statusCode == FileTransferUtility.STATUS_CODE_FILE_FOUND){

            fileTransferUtility.receiveFileData(fileName, serverDirectory);
        }
        else if(statusCode == FileTransferUtility.STATUS_CODE_FILE_NOT_FOUND)
        {
            System.out.println(MessageStrings.fileNotFound);
        }
    }

    private void initiateFileUpload(String fileName) throws IOException{

        String path = serverDirectory + "/" + fileName;

        File file = new File(path);

        if(file.exists() && !file.isDirectory()) {

            sendInt(FileTransferUtility.STATUS_CODE_FILE_FOUND);

            fileTransferUtility.sendFileData(file);
        }
        else
        {
            sendInt(FileTransferUtility.STATUS_CODE_FILE_NOT_FOUND);
        }
    }
}