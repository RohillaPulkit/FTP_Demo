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

            while(true)
            {
                //receive the request sent from the client
                String request = dataInputStream.readUTF();

                //trim whitespaces
                request = request.trim();

                System.out.println("Received request: " + request);

                handleRequest(request);
            }

        }
        catch (IOException ex){

            ex.printStackTrace();
//            System.out.println("Client "+ this.socket.getInetAddress().getHostAddress() + " disconnected");
        }
    }

    //send a response to the output stream
    private void sendResponse(String response) {
        try{
            dataOutputStream.writeUTF(response);
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
                getDirectoryContent();
                break;
            case GET:
                initiateFileUpload(command.fileName);
                break;
            case UPLOAD:
                initiateFileDownload(command.fileName);
                break;
            default:
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

            sendResponse(contents);
        }
        catch (IOException ex){

            sendResponse("Unable to get directory content. Please try again later.");
        }

    }

    private void initiateFileDownload(String fileName) throws IOException{
        fileTransferUtility.receiveFileData(fileName, serverDirectory);
    }

    private void initiateFileUpload(String fileName) throws IOException{

        String path = serverDirectory + "/" + fileName;

        File file = new File(path);

        if(file.exists() && !file.isDirectory()) {

            sendResponse(FileTransferUtility.status200);

            fileTransferUtility.sendFileData(file);
        }
        else
        {
            sendResponse(FileTransferUtility.status404);
        }
    }
}