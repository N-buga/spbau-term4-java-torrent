/**
 * Created by n_buga on 19.04.16.
 */
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class TorrentTests {
    private TorrentTests() {}

    @Test
    public static void testSimple() throws IOException {
        Path directory1 = Paths.get(".", "1");
        Path directory2 = Paths.get(".", "2");
        Files.createDirectory(directory1);
        Files.createDirectory(directory2);
        Path file1 = Paths.get(directory1.toString(), "1.txt");
        Path file2 = Paths.get(directory2.toString(), "2.txt");
        Files.createFile(file1);
        Files.createFile(file2);
        String[] in = {"upload", file1.toString()};
        Client.main(in);
    }
}
