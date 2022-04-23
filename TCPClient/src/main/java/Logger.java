import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {

    private final File file;

    public Logger(File file, String ... header) {
        this.file = file;

        if (!file.exists()) {
            log((Object[]) header);
        }
    }

    public void log(Object ... lines) {
        boolean append = file.exists();

        try (FileWriter fileWriter = new FileWriter(file, append)) {

            if (lines.length == 0)
                return;

            for (int i = 0; i < lines.length - 1; i++) {
                fileWriter.write(lines[i].toString());
                fileWriter.write(",");
            }
            fileWriter.write(lines[lines.length - 1].toString());
            fileWriter.write("\n");

        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
