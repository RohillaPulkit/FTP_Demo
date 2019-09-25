import java.util.Map;

public class Authentication {

    public static int STATUS_CODE_AUTHENTICATED = 200;
    public static int STATUS_CODE_AUTHENTICATION_FAILED = 401;

    private static Map<String, String> credentials = Map.of("abc", "123", "def", "456");

    public static boolean isAuthenticated(String credentials){

        if (credentials == null || credentials.length() == 0)
            return false;

        String parts[] = credentials.split("\\s+");

        if (parts.length != 2)
            return false;

        String userName = parts[0];
        String password = parts[1];

        String storedPassword = Authentication.credentials.get(userName);

        if (storedPassword != null && storedPassword.equals(password))
            return true;

        return false;
    }
}
