/**
 * Created by n_buga on 19.04.16.
 */
import org.junit.After;
import org.junit.Test;
import ru.spbau.mit.Client;
import ru.spbau.mit.ClientFileInfo;
import ru.spbau.mit.TorrentClientMain;
import ru.spbau.mit.TorrentTracker;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

public final class TorrentTests {
    public static final String IP_TORRENT = "127.0.0.1";
    public static final String UPLOAD_QUERY = "newfile";
    public static final String RUN_QUERY = "run";

    public final String[] LIST_QUERY = {"list", IP_TORRENT};

    private final int TIME_OUT_AFTER_SERVER_START = 100;
    private final int TIME_OUT_FOR_UPDATE = 50;
    private final String CONTAINS = "cat";

    private Client clientCheck;
    private Client runClient;

    public void createClients() {
        clientCheck = new Client(IP_TORRENT);
    }

    @After public void closeClients() {
        if (clientCheck != null) {
            clientCheck.close();
            clientCheck.resetData();
        }
        if (runClient != null) {
            runClient.close();
            runClient.resetData();
        }
        System.out.println("______________________");
    }

    @Test
    public void testListSimple() throws IOException {
        Path directory1 = Paths.get(".", "1");
        Path directory2 = Paths.get(".", "2");

        Path file1 = Paths.get(directory1.toString(), "1.txt");
        Path file2 = Paths.get(directory2.toString(), "2.txt");
        try (TorrentTracker torrent = new TorrentTracker()) {
            torrent.start();
            Thread.sleep(TIME_OUT_AFTER_SERVER_START);
            Files.createDirectory(directory1);
            Files.createDirectory(directory2);
            Files.createFile(file1);
            Files.createFile(file2);
            String[] in1 = {UPLOAD_QUERY, IP_TORRENT, file1.toString()};
            String[] in2 = {UPLOAD_QUERY, IP_TORRENT, file2.toString()};
            TorrentClientMain.main(in1);
            TorrentClientMain.main(in2);
            Client clientCheck = new Client(IP_TORRENT);
            Set<Client.TorrentClient.FileInfo> list = clientCheck.getList();
            assertTrue(list.size() == 0);
            clientCheck.close();
            clientCheck.resetData();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
            Files.deleteIfExists(directory1);
            Files.deleteIfExists(directory2);
        }
    }

    @Test
    public void testList() throws IOException {
        Path directory1 = Paths.get(".", "1");
        Path directory2 = Paths.get(".", "2");

        Path file1 = Paths.get(directory1.toString(), "1.txt");
        Path file2 = Paths.get(directory2.toString(), "2.txt");
        try (TorrentTracker torrentTracker = new TorrentTracker()) {
            torrentTracker.start();
            Thread.sleep(TIME_OUT_AFTER_SERVER_START);

            createClients();

            Files.createDirectory(directory1);
            Files.createDirectory(directory2);
            Files.createFile(file1);
            Files.createFile(file2);
            String[] in1 = {UPLOAD_QUERY, IP_TORRENT, file1.toString()};
            String[] in2 = {UPLOAD_QUERY, IP_TORRENT, file2.toString()};
            TorrentClientMain.main(in1);
            TorrentClientMain.main(in2);

            int i = 0;

            runClient = new Client(IP_TORRENT);
            runClient.run();
            Thread.sleep(TIME_OUT_FOR_UPDATE);

            TorrentClientMain.main(LIST_QUERY);

            Set<Client.TorrentClient.FileInfo> list = clientCheck.getList();
            assertTrue(list.size() == 2);
            Set<String> rightNames = new HashSet<>();
            rightNames.add("1.txt");
            rightNames.add(file2.getFileName().toString());
            assertTrue(rightNames.equals(list.stream().map(Client.TorrentClient.FileInfo::getName).
                    collect(Collectors.toCollection(TreeSet::new))));

         } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(file1);
            Files.deleteIfExists(file2);
            Files.deleteIfExists(directory1);
            Files.deleteIfExists(directory2);
        }
    }

    @Test
    public void simpleTestLoad() throws IOException {
        testLoadFileSize(CONTAINS.length(), 0, CONTAINS);
        testLoadFileSize(CONTAINS.length() * 2, 0, CONTAINS);
    }

    @Test
    public void offsetTestLoad() throws IOException {
        final int offset = 10;
        testLoadFileSize(CONTAINS.length() + offset, offset, CONTAINS);
        testLoadFileSize(CONTAINS.length() * 2 + offset, offset, CONTAINS);
    }

    @Test
    public void loadBigFile() throws IOException {
        final long size = ClientFileInfo.SIZE_OF_FILE_PIECE * 5;
        final int offset = ClientFileInfo.SIZE_OF_FILE_PIECE * 2;
        testLoadFileSize(size, offset, CONTAINS);
        StringBuilder bigContain = new StringBuilder();
        for (int i = 0; i < ClientFileInfo.SIZE_OF_FILE_PIECE + 10; i++) {
            bigContain.append('a');
        }
        testLoadFileSize(size, offset, bigContain.toString());
        testLoadFileSize(size + 5, offset - 2, bigContain.toString());
    }

    private void testLoadFileSize(long size, int offset, String contains) throws IOException {
        Path directory = Paths.get(".", "files");
        String fileName = "1.txt";
        Path file = Paths.get(directory.toString(), fileName);
        Path pathToFiles = Paths.get(".", "downloads");

        try (TorrentTracker torrentTracker = new TorrentTracker()) {
            torrentTracker.start();

            Thread.sleep(TIME_OUT_AFTER_SERVER_START);

            Files.createDirectory(directory);
            Files.createFile(file);
            RandomAccessFile f = new RandomAccessFile(file.toString(), "rw");
            f.setLength(size);
            f.seek(offset);
            f.write(contains.getBytes());
            f.close();

            createClients();

            Thread.sleep(TIME_OUT_AFTER_SERVER_START);

            String[] in = {UPLOAD_QUERY, IP_TORRENT, file.toString()};
            TorrentClientMain.main(in);

            Client runClient = new Client(IP_TORRENT);
            runClient.start();

            Thread.sleep(TIME_OUT_FOR_UPDATE);

            TorrentClientMain.main(LIST_QUERY);

            Set<Client.TorrentClient.FileInfo> availableFiles = clientCheck.getList();
            assertTrue(availableFiles.size() > 0);

            clientCheck.markAsWantToLoad(0);
            Set<Thread> loadThreads = clientCheck.run();

            for (Thread t: loadThreads) {
                t.join();
            }

            Path newFile = Paths.get(pathToFiles.toString(), "0", fileName);

            assertTrue(Files.exists(pathToFiles));
            assertTrue(Files.exists(newFile));

            String curContains;

            RandomAccessFile inFile = new RandomAccessFile(newFile.toString(), "rw");
            byte[] result = new byte[contains.length()];
            inFile.seek(offset);
            inFile.read(result);
            curContains = new String(result);
            inFile.close();

            runClient.close();
            runClient.resetData();

            assertTrue(curContains.equals(contains));

        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
