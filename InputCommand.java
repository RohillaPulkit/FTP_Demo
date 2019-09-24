public class InputCommand {

    private static final String dirCommand = "dir";
    private static final String getCommand = "get";
    private static final String uploadCommand = "upload";

    public enum Type
    {
        DIR,
        GET,
        UPLOAD,
        INVALID
    }

    public Type type = Type.INVALID;
    public String fileName = null;

    public InputCommand(String input){

        String parts[] = input.split("\\s+");

        if (parts.length == 1 && parts[0].toLowerCase().equals(dirCommand)){

            this.type = Type.DIR;
        }
        else if (parts.length == 2){

            String command = parts[0];
            String fileName = parts[1];

            if (command.toLowerCase().equals(getCommand)){

                this.type = Type.GET;
                this.fileName = fileName;
            }
            else if(command.toLowerCase().equals(uploadCommand)){

                this.type = Type.UPLOAD;
                this.fileName = fileName;
            }
        }
    }
}
