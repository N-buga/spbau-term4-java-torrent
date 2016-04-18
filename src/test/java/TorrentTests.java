/**
 * Created by n_buga on 17.04.16.
 */

import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;

import static org.junit.Assert.assertTrue;

public class TorrentTests {
    private final String TORRENT_IP = "127.0.0.1";
    private final int TIME_OUT_AFTER_SERVER_START = 100;
    private final int TIME_OUT_FOR_UPDATE = 50;
    private final int TIME_OUT_AFTER_UPLOAD_FILE = 100;
    private final String TEST_NAME_1 = "TorrentTest1";
    private final String TEST_NAME_2 = "TorrentTest2";
    private final String TEST_NAME = "TorrentTest";
    private final String TEST_CONTAIN = "Hello!";

    @Test
    public void verySimpleListTest() throws IOException, InterruptedException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Set<Client.TorrentClient.FileInfo> listAvailableFiles = client.getList();
        assertTrue(listAvailableFiles.size() == 0);
        client.close();
        torrent.close();
        client.resetData();
        torrent.resetData();
    }

    @Test
    public void simpleListTest() throws IOException, InterruptedException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        client.upload(testPath);
        Set<Client.TorrentClient.FileInfo> listAvailableFiles = client.getList();
        assertTrue(listAvailableFiles.size() == 1);
        for (Client.TorrentClient.FileInfo fileInfo: listAvailableFiles) {
            assertTrue(fileInfo.getName().equals(TEST_NAME));
            assertTrue(fileInfo.getSize() == 0);
            assertTrue(fileInfo.getID() == 0);
        }
        Files.deleteIfExists(testPath);
        client.close();
        torrent.close();
        client.resetData();
        torrent.resetData();
    }

    @Test
    public void simpleSourcesTest() throws IOException, InterruptedException {
        final int curID = 0;
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        client.upload(testPath);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Set<ClientInfo> info = client.sources(curID);
        assertTrue(info.size() == 1);
        for (ClientInfo clientInfo: info) {
            assertTrue(Arrays.equals(clientInfo.getServerIP(), torrent.getBytes(client.getServerIP())));
            assertTrue(clientInfo.getServerPort() == client.getServerPort());
        }
        Files.deleteIfExists(testPath);
        torrent.close();
        client.close();
        client.resetData();
        torrent.resetData();
    }

    @Test
    public void listTest() throws IOException, InterruptedException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client1 = new Client(TORRENT_IP);
        Client client2 = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath1 = Paths.get(".", TEST_NAME_1);
        Path testPath2 = Paths.get(".", TEST_NAME_2);
        Files.deleteIfExists(testPath1);
        Files.deleteIfExists(testPath2);
        Files.createFile(testPath1);
        Files.createFile(testPath2);
        int id1 = client1.upload(testPath1);
        int id2 = client2.upload(testPath2);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Set<Client.TorrentClient.FileInfo> listAvailableFiles = client1.getList();
        assertTrue(listAvailableFiles.size() == 2);
        for (Client.TorrentClient.FileInfo fileInfo: listAvailableFiles) {
            assertTrue(fileInfo.getID() < 2);
            assertTrue(fileInfo.getSize() == 0);
            if (fileInfo.getID() == id1) {
                assertTrue(fileInfo.getName().equals(TEST_NAME_1));
            } else {
                assertTrue(fileInfo.getName().equals(TEST_NAME_2));
            }
        }
        Files.deleteIfExists(testPath1);
        Files.deleteIfExists(testPath2);
        client1.close();
        client2.close();
        torrent.close();
        client1.resetData();
        client2.resetData();
        torrent.resetData();
    }

    @Test
    public void sourcesTest() throws IOException, InterruptedException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client1 = new Client(TORRENT_IP);
        Client client2 = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath1 = Paths.get(".", TEST_NAME_1);
        Path testPath2 = Paths.get(".", TEST_NAME_2);
        Files.deleteIfExists(testPath1);
        Files.deleteIfExists(testPath2);
        Files.createFile(testPath1);
        Files.createFile(testPath2);
        int id1 = client1.upload(testPath1);
        int id2 = client2.upload(testPath2);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Set<ClientInfo> clients2 = client1.sources(id2);
        Set<ClientInfo> clients1 = client2.sources(id1);
        assertTrue(clients2.size() == 1);
        assertTrue(clients1.size() == 1);
        for (ClientInfo clientInfo: clients1) {
            assertTrue(clientInfo.getServerPort() == client1.getServerPort());
            assertTrue(Arrays.equals(clientInfo.getServerIP(), torrent.getBytes(client1.getServerIP())));
        }
        for (ClientInfo clientInfo: clients2) {
            assertTrue(clientInfo.getServerPort() == client2.getServerPort());
            assertTrue(Arrays.equals(clientInfo.getServerIP(), torrent.getBytes(client2.getServerIP())));
        }
        Files.deleteIfExists(testPath1);
        Files.deleteIfExists(testPath2);
        client1.close();
        client2.close();
        torrent.close();
        client1.resetData();
        client2.resetData();
        torrent.resetData();
    }

    @Test
    public void loadsTestSimple() throws InterruptedException, IOException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client1 = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        client1.upload(testPath);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Client client2 = new Client(TORRENT_IP);
        Thread loadThread = client2.load(0, true);
        loadThread.join();
        Path path = Paths.get(".", "Download", "0", TEST_NAME);
        assertTrue(Files.exists(path));
        assertTrue(Files.size(path) == 0);
        Files.deleteIfExists(testPath);
        Files.deleteIfExists(path);
        client1.close();
        client2.close();
        torrent.close();
        client1.resetData();
        client2.resetData();
        torrent.resetData();
    }

    @Test
    public void loadTest() throws InterruptedException, IOException {
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client1 = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        DataOutputStream outFile = new DataOutputStream(new FileOutputStream(testPath.toString()));
        outFile.writeUTF(TEST_CONTAIN);
        outFile.flush();
        outFile.close();
        client1.upload(testPath);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Client client2 = new Client(TORRENT_IP);
        Thread loadThread = client2.load(0, true);
        loadThread.join();
        Path path = Paths.get(".", "Download", "0", TEST_NAME);
        assertTrue(Files.exists(path));
        assertTrue(Files.size(path) == Files.size(testPath));
        DataInputStream inFile = new DataInputStream(new FileInputStream(path.toString()));
        assertTrue(TEST_CONTAIN.equals(inFile.readUTF()));
        Files.deleteIfExists(testPath);
        Files.deleteIfExists(path);
        client1.close();
        client2.close();
        torrent.close();
        client1.resetData();
        client2.resetData();
        torrent.resetData();
    }

    @Test
    public void testOnSaveClientData() throws InterruptedException, IOException {
        final int curID = 0;
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        client.upload(testPath);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Set<Client.TorrentClient.FileInfo> fileInfos1 = client.getList();
        client.close();
        client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Set<Client.TorrentClient.FileInfo> fileInfos2 = client.getList();
        assertTrue(fileInfos1.size() == fileInfos2.size());
        assertTrue(fileInfos1.size() == 1);
        for (Client.TorrentClient.FileInfo fileInfo1: fileInfos1) {
            for (Client.TorrentClient.FileInfo fileInfo2: fileInfos2) {
                assertTrue(fileInfo1.getName().equals(fileInfo2.getName()));
                assertTrue(fileInfo1.getSize() == fileInfo2.getSize());
                assertTrue(fileInfo1.getID() == fileInfo2.getID());
            }
        }
        Files.deleteIfExists(testPath);
        torrent.close();
        client.close();
        client.resetData();
        torrent.resetData();
    }

    @Test
    public void testOnSaveServerData() throws InterruptedException, IOException {
        final int curID = 0;
        TorrentTracker torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        Client client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Path testPath = Paths.get(".", TEST_NAME);
        Files.deleteIfExists(testPath);
        Files.createFile(testPath);
        client.upload(testPath);
        Thread.sleep(TIME_OUT_AFTER_UPLOAD_FILE);
        Set<Client.TorrentClient.FileInfo> fileInfos1 = client.getList();
        client.close();
        torrent.close();
        torrent = new TorrentTracker();
        torrent.start();
        Thread.sleep(TIME_OUT_AFTER_SERVER_START);
        client = new Client(TORRENT_IP);
        Thread.sleep(TIME_OUT_FOR_UPDATE);
        Set<Client.TorrentClient.FileInfo> fileInfos2 = client.getList();
        assertTrue(fileInfos1.size() == fileInfos2.size());
        assertTrue(fileInfos1.size() == 1);
        for (Client.TorrentClient.FileInfo fileInfo1: fileInfos1) {
            for (Client.TorrentClient.FileInfo fileInfo2: fileInfos2) {
                assertTrue(fileInfo1.getName().equals(fileInfo2.getName()));
                assertTrue(fileInfo1.getSize() == fileInfo2.getSize());
                assertTrue(fileInfo1.getID() == fileInfo2.getID());
            }
        }
        Files.deleteIfExists(testPath);
        torrent.close();
        client.close();
        client.resetData();
        torrent.resetData();
    }
}
