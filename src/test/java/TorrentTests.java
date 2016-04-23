/**
 * Created by n_buga on 19.04.16.
 */
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class TorrentTests {
    public static final String IP_TORRENT = "127.0.0.1";
    public static final String UPLOAD_QUERY = "newfile";
    public static final String RUN_QUERY = "run";

    public final String[] LIST_QUERY = {"list", IP_TORRENT};

    private final int TIME_OUT_AFTER_SERVER_START = 100;
    private final int TIME_OUT_FOR_UPDATE = 50;
    private final String CONTAINS = "cat";

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
            Client.main(in1);
            Client.main(in2);
            Client clientCheck = new Client(IP_TORRENT);
            Set<Client.TorrentClient.FileInfo> list = clientCheck.getList();
            assertTrue(list.size() == 0);
            torrent.close();
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
            try (Client clientCheck = new Client(IP_TORRENT);) {

                Thread.sleep(TIME_OUT_AFTER_SERVER_START);
                Files.createDirectory(directory1);
                Files.createDirectory(directory2);
                Files.createFile(file1);
                Files.createFile(file2);
                String[] in1 = {UPLOAD_QUERY, IP_TORRENT, file1.toString()};
                String[] in2 = {UPLOAD_QUERY, IP_TORRENT, file2.toString()};
                Client.main(in1);
                Client.main(in2);

                int i = 0;

                Client runClient = new Client(IP_TORRENT);
                runClient.run();
                Thread.sleep(TIME_OUT_FOR_UPDATE);
                Client.main(LIST_QUERY);

                Set<Client.TorrentClient.FileInfo> list = clientCheck.getList();
                assertTrue(list.size() == 2);
                Set<String> rightNames = new HashSet<>();
                rightNames.add("1.txt");
                rightNames.add(file2.getFileName().toString());
                assertTrue(rightNames.equals(list.stream().map(Client.TorrentClient.FileInfo::getName).
                        collect(Collectors.toCollection(TreeSet::new))));

                runClient.close();
                runClient.resetData();

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
    }

    @Test
    public void simpleLoad() throws IOException {
        Path directory = Paths.get(".", "files");
        String fileName = "1.txt";
        Path file = Paths.get(directory.toString(), fileName);
        Path pathToFiles = Paths.get(".", "downloads");

        try (TorrentTracker torrentTracker = new TorrentTracker()) {
            torrentTracker.start();

            Files.createDirectory(directory);
            Files.createFile(file);
            DataOutputStream outFile = new DataOutputStream(new FileOutputStream(file.toString()));
            outFile.writeUTF(CONTAINS);
            outFile.close();

            try (Client clientCheck = new Client(IP_TORRENT);) {

                Thread.sleep(TIME_OUT_AFTER_SERVER_START);

                String[] in = {UPLOAD_QUERY, IP_TORRENT, file.toString()};
                Client.main(in);

                Client runClient = new Client(IP_TORRENT);
                runClient.start();

                Thread.sleep(TIME_OUT_FOR_UPDATE);
                Client.main(LIST_QUERY);

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

                String contains;

                DataInputStream inFile = new DataInputStream(new FileInputStream(newFile.toString()));
                contains = inFile.readUTF();
                inFile.close();

                String initString;

                inFile = new DataInputStream(new FileInputStream(file.toString()));
                initString = inFile.readUTF();
                inFile.close();

                assertTrue(contains.equals(initString));

                runClient.close();
                clientCheck.resetData();
                runClient.resetData();
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
    }
}
