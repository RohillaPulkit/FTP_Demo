import java.net.InetAddress;
import java.net.UnknownHostException;

public class InputAddress {

    public InetAddress ipAddress;
    public int portNumber;
    public boolean isValid = false;

    public InputAddress(String args[]) {

        if(args.length == 2){

            String ip = args[0];
            String port = args[1];

            try {

                ipAddress = InetAddress.getByName(ip);
                portNumber = Integer.parseInt(port);
                isValid = true;

                if (portNumber > 65535){

                    isValid = false;
                }
            }
            catch (UnknownHostException | NumberFormatException ex) {
                isValid = false;
            }
        }
    }
}
