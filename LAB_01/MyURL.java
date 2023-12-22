package LAB_01;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
public class MyURL {
    // Attributes to store the URL components
    private String protocol;
    private String host;
    private int port = -1; // Default value for port
    private String path;
    private String url;

    // Constructor takes a URL string as input
    public MyURL(String url) throws IllegalArgumentException {
        // Check if the URL string is null or empty
        this.url =url;
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL cannot be null or empty.");
        }

        // regex
        String regex ="^([^:/]+)://([^:/]+)(?::(\\d+))?(/.*)$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        // If the URL matches the regex
        if (matcher.matches()) {
            // Extract the matched components
            protocol = matcher.group(1);
            host = matcher.group(2);

            // If port is present, convert to integer and store
            if (matcher.group(3) != null) {
                port = Integer.parseInt(matcher.group(3));
            }

            // Store the path, ensuring it has at least a '/'
            path = (matcher.group(4) != null) ? matcher.group(4) : "/";
        } else {
            // If the URL doesn't match the regex, throw an exception
            throw new IllegalArgumentException("Invalid URL format.");
        }
    }

    // Getter methods to access the URL components
    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPath() { return path;
    }
    public String toString()
    {
        return url;
    }
    // Testing the MyURL class
    public static void main(String[] args) {
        try {
            MyURL myURL = new MyURL("http://example.com/");
            System.out.println("Protocol: " + myURL.getProtocol());
            System.out.println("Host: " + myURL.getHost());
            System.out.println("Port: " + myURL.getPort());
            System.out.println("Path: " + myURL.getPath());
            System.out.println("FullUrl:"+ myURL);
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}
