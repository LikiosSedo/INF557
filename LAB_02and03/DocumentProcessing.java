package LAB_02and03;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DocumentProcessing {

    public interface URLhandler {
        void takeUrl(String url);
    }

    public static URLhandler handler = new URLhandler() {
        @Override
        public void takeUrl(String url) {
            System.out.println(url);        // DON'T change anything here
        }
    };

    /**
     * Parse the given buffer to fetch embedded links and call the handler to
     * process these links.
     *
     * @param data
     *          the buffer containing the html document
     *
     */
    public static void parseBuffer(CharSequence data) {
        // Regular expression to match URLs inside <a> tags in HTML
        String regex = "(?i)<a\\b[^>]*?\\s+href\\s*=\\s*(['\"])(http[^'\\\"]+)\\1[^>]*?>";
        // Compile the regular expression and get a Matcher
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(data);

        while (matcher.find()) {
            String url = matcher.group(2);  // Extract the matched URL from the input data

            try {
                MyURL myUrl = new MyURL(url);   // Attempt to create a MyURL object
                if ("http".equalsIgnoreCase(myUrl.getProtocol())) {
                    handler.takeUrl(myUrl.toString());  // If no exception is thrown and the protocol is 'http', the URL is valid
                }
            }
            catch (IllegalArgumentException e) {
            }
        }

    }
    }

