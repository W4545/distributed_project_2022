import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class GUI {

    public static void main(String[] args) throws IOException {
        FileDialog dialog = new FileDialog((Frame) null);
        dialog.setTitle("Select Client Settings File file");
        dialog.setVisible(true);

        String configLocation = dialog.getFiles()[0].getAbsolutePath();

        Properties properties = new Properties();
        properties.load(Files.newInputStream(dialog.getFiles()[0].toPath()));

        String secondArg;
        if (properties.getProperty("status").equals("listening")) {
            secondArg = dialog.getFiles()[0].getParent();
        } else {
            dialog.setTitle("Select file to transfer");
            dialog.setVisible(true);

            secondArg = dialog.getFiles()[0].getAbsolutePath();
        }

        dialog.dispose();

        TCPClient.main(new String[] { configLocation, secondArg });
    }
}
