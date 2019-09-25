import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FileTransferUtility {

    public static int STATUS_CODE_FILE_FOUND = 200;
    public static int STATUS_CODE_FILE_NOT_FOUND = 404;

    private DataOutputStream dataOutputStream;
    private DataInputStream dataInputStream;

    public FileTransferUtility(DataOutputStream dataOutputStream, DataInputStream dataInputStream){

        this.dataOutputStream = dataOutputStream;
        this.dataInputStream = dataInputStream;
    }

    public void sendFileData(File file) throws IOException {

        System.out.print("Sending");

        long fileSize = file.length();

        byte[] buffer = new byte[1024];
        FileInputStream fileInputStream = new FileInputStream(file);
        BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream);

        dataOutputStream.writeLong(fileSize);
        dataOutputStream.flush();

        long bytesLeft = fileSize;

        long divisions = fileSize/1024;
        long increment = 0;
        int percent = 10;

        int bytesRead;

        while (bytesLeft > 0 && (bytesRead = bufferedInputStream.read(buffer, 0, (int) Math.min(bytesLeft, buffer.length))) != -1){

            if (increment == divisions*percent/100){
                System.out.print(".");
                percent += 10;
            }

            dataOutputStream.write(buffer, 0, bytesRead);
            dataOutputStream.flush();

            bytesLeft -= bytesRead;
            increment++;
        }

        while(percent <= 100){
            System.out.print(".");
            percent += 10;
        }

        System.out.println(" Done");

        fileInputStream.close();
        bufferedInputStream.close();
    }

    public void receiveFileData(String path, String destination) throws IOException{

        System.out.print("Receiving");

        Path filePath = Paths.get(path);
        String fileName = filePath.getFileName().toString();
        String outputLocation = destination + "/" + fileName;

        byte[] buffer = new byte[1024];

        FileOutputStream fileOutputStream = new FileOutputStream(outputLocation);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

        long fileSize = dataInputStream.readLong();
        long bytesLeft = fileSize;

        long divisions = fileSize/1024;
        long increment = 0;
        int percent = 10;

        while (bytesLeft > 0){

            if (increment == divisions*percent/100){
                System.out.print(".");
                percent += 10;
            }

            int bytesRead = dataInputStream.read(buffer, 0, (int)Math.min(bytesLeft, buffer.length));

            if (bytesRead < 0) {
                throw new EOFException("Expected " + bytesLeft + " more bytes to read");
            }

            bufferedOutputStream.write(buffer, 0, bytesRead);
            bufferedOutputStream.flush();

            bytesLeft -= bytesRead;
            increment++;
        }

        while(percent <= 100){
            System.out.print(".");
            percent += 10;
        }

        System.out.println(" Done");

        bufferedOutputStream.close();
        fileOutputStream.close();
    }
}
