import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by n_buga on 11.04.16.
 */
public class Client implements AutoCloseable{
    private ClientFileData clientFileData;
    private TorrentClient torrentClient;
    private TorrentServer torrentServer;

    public Client(String ipTorrent) {
        clientFileData = new ClientFileData();
        clientFileData.updateDataFromFile();
        torrentServer = new TorrentServer();
        torrentServer.start();
        torrentClient = new TorrentClient(ipTorrent);
    }

    public void close() {
        clientFileData.saveDataToFile();
        torrentClient.close();
        torrentServer.close();
    }

    public void resetData() {
        clientFileData.resetData();
    }

    public int getServerPort() {
        return torrentServer.getPort();
    }

    public String getServerIP() {
        return torrentClient.getIP();
    }

    private enum State {NOT_STARTED, END, MISSED_CONNECTION, RUNNING};

    public class TorrentClient implements AutoCloseable {
        private final int TIME_OUT_OF_WAITING_CONNECTION = 200;
        private Connection trackerConnection;
        private ScheduledExecutorService scheduledExecutorService;
        private final int serverPort = 8081;
        private Socket trackerSocket;
        private boolean end = false;
        private String ipTorrent = "127.0.0.1";
        private State state = State.NOT_STARTED;
        private Lock lock = new ReentrantLock(true);

        public class FileInfo {
            private int id;
            private String name;
            private long size;
            private int countParts;

            public FileInfo(int id, String name, long size) {
                this.id = id;
                this.name = name;
                this.size = size;
                countParts = (int) (size - 1) / ClientFileInfo.SIZE_OF_FILE_PIECE + 1;
            }

            public int getID() {
                return id;
            }

            public String getName() {
                return name;
            }

            public long getSize() {
                return size;
            }

            public int getCountParts() {
                return countParts;
            }
        }

        TorrentClient(String ipTorrent) {
            this.ipTorrent = ipTorrent;
            Thread connectThread = new Thread(this::tryConnect);
            connectThread.start();
            try {
                connectThread.join(TIME_OUT_OF_WAITING_CONNECTION);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }
            scheduledExecutorService = Executors.newScheduledThreadPool(1);
            scheduledExecutorService.scheduleAtFixedRate(this::update, 0,
                    TorrentTracker.TIME_OUT_SCHEDULE / 2, TimeUnit.SECONDS);
        }

        private void connect() {
            trackerSocket = null;
            try {
                trackerSocket = new Socket(ipTorrent, serverPort);
                state = State.RUNNING;
            } catch (IOException e) {
                return;
            }
            trackerConnection = new Connection(trackerSocket);
        }

        private void tryConnect() {
            System.out.println("Try to connect");
            while (state != State.RUNNING) {
                connect();
            }
            System.out.println("Connection successfully repaired");
        }

        public void close() {
            System.out.println("Closed");
            state = State.END;
            end = true;
            scheduledExecutorService.shutdown();
            try {
                trackerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String getIP() {
            return  trackerSocket.getLocalSocketAddress().toString();
        }

        public Set<FileInfo> getList() {
            lock.lock();
            try {
                trackerConnection.sendType(Connection.LIST_QUERY);
            } catch (IOException e) {
                e.printStackTrace();
                trackerConnection.close();
                lock.unlock();
                return null;
            }
            int countFiles = trackerConnection.readInt();
            Set<FileInfo> fileList = new HashSet<>();
            for (int i = 0; i < countFiles; i++) {
                int id = trackerConnection.readInt();
                String name = trackerConnection.readString();
                long size = trackerConnection.readLong();
                fileList.add(new FileInfo(id, name, size));
            }
            lock.unlock();
            return fileList;
        }

        public int upload(Path filePath) {
            lock.lock();
            long size = 0;
            try {
                size = Files.size(filePath);
            } catch (IOException e) {
                System.out.println("Sorry, file doesn't exist");
                lock.unlock();
                return -1;
            }
            String name = filePath.getFileName().toString();
            try {
                trackerConnection.sendType(Connection.UPLOAD_QUERY);
                trackerConnection.sendString(name);
                trackerConnection.sendLong(size);
            } catch (IOException e) {
                e.printStackTrace();
                trackerConnection.close();
                lock.unlock();
                return -1;
            }
            int id = trackerConnection.readInt();
            clientFileData.addFile(id, size, filePath);
            clientFileData.addAllParts(id);
            lock.unlock();
            return id;
        }

        public void update() {
            if (state == State.MISSED_CONNECTION) {
                return;
            }
            lock.lock();
            try {
                trackerConnection.sendType(Connection.UPDATE_QUERY);
                trackerConnection.sendInt(torrentServer.getPort());
                trackerConnection.sendInt(clientFileData.getIdAvailableFiles().size());
                trackerConnection.sendIntegerSet(clientFileData.getIdAvailableFiles());
            } catch (SocketException e) {
                System.out.println("Connection with server was missed.");
                state = State.MISSED_CONNECTION;
                lock.unlock();
                tryConnect();
                return;
            } catch (IOException e) {
                e.printStackTrace();
                trackerConnection.close();
                lock.unlock();
                return;
            }

            try {
                trackerConnection.readBoolean();
            } catch (IOException e) {
                state = State.MISSED_CONNECTION;
                System.out.println("Connection with server was missed.");
                lock.unlock();
                tryConnect();
                return;
            }
            lock.unlock();
        }

        public Thread load(int id, boolean allowedDelete) {
            Thread loadThread = new Thread((Runnable) () -> {
                long size = -1;
                int countOfParts = -1;
                String name = "";
                Set<FileInfo> fileList = getList();
                for (FileInfo fi : fileList) {
                    if (fi.getID() == id) {
                        size = fi.getSize();
                        name = fi.getName();
                        countOfParts = fi.getCountParts();
                    }
                }
                if (size == -1) {
                    System.out.print("Not find file with ID = ");
                    System.out.print(id);
                    System.out.println();
                    return;
                }

                RandomAccessFile curFile;
                Path curPath;
                try {
                    curPath = Paths.get(".", "Download", Integer.toString(id), name);
                    Files.createDirectories(curPath.getParent());
                    if (Files.exists(curPath) && !allowedDelete) {
                        System.out.println("Exist such file. Please move it or allow to delete it");
                        return;
                    }
                    Files.deleteIfExists(curPath);
                    Files.createFile(curPath);
                    curFile = new RandomAccessFile(curPath.toString(), "rw");
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
                clientFileData.addFile(id, size, curPath);
                Set<ClientInfo> seeds = sources(id);
                if (seeds == null) {
                    return;
                }
                Boolean[] loadParts = new Boolean[countOfParts + 1];
                for (int i = 0; i < countOfParts + 1; i++) {
                    loadParts[i] = false;
                }
                for (ClientInfo seed: seeds) {
                    try (Socket socket = new Socket(InetAddress.getByAddress(seed.getServerIP()),
                            seed.getServerPort())) {
                        Connection curConnection = new Connection(socket);
                        curConnection.sendType(Connection.STAT_QUERY);
                        curConnection.sendInt(id);
                        int count = curConnection.readInt();
                        for (int j = 0; j < count; j++) {
                            int part = curConnection.readInt();
                            if (part >= countOfParts) {
                                continue;
                            }
                            if (loadParts[part].equals(false)) {
                                savePart(curConnection, part, id, curFile);
                                clientFileData.addPart(id, part);
                                loadParts[part] = true;
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            loadThread.start();
            return loadThread;
        }

        private Set<ClientInfo> sources(int id) {
            lock.lock();
            try {
                trackerConnection.sendType(Connection.SOURCES_QUERY);
                trackerConnection.sendInt(id);
            } catch (IOException e) {
                e.printStackTrace();
                trackerConnection.close();
                lock.unlock();
                return null;
            }
            int count = trackerConnection.readInt();
            Set<ClientInfo> result = new HashSet<>();
            for (int i = 0; i < count; i++) {
                result.add(trackerConnection.readClient());
            }
            lock.unlock();
            return result;
        }

        private void savePart(Connection curConnection, int part, int id, RandomAccessFile randomAccessFile) {
            try {
                curConnection.sendType(Connection.GET_QUERY);
                curConnection.sendInt(id);
                curConnection.sendInt(part);
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                byte[] partText = curConnection.readPart();
                randomAccessFile.write(partText,
                                part * ClientFileInfo.SIZE_OF_FILE_PIECE, partText.length);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public class TorrentServer {
        private boolean end = false;
        private ServerSocket serverSocket;
        private final int TIME_OUT = 500;
        private Thread serverThread;
        private int port;

        TorrentServer() {}

        public int getPort() {
            return port;
        }

        public Thread start() {
            try {
                serverSocket = new ServerSocket(0);
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.port = serverSocket.getLocalPort();
            serverThread = new Thread(this::connectionHandler);
            serverThread.start();
            return serverThread;
        }

        public void close() {
            end = true;
            try {
                serverSocket.close();
            } catch (NullPointerException | IOException ignored) {

            }
        }

        private void connectionHandler() {
            try {
                while (!end) {
                    serverSocket.setSoTimeout(TIME_OUT);
                    try {
                        Socket clientSocket = serverSocket.accept();
                        (new Thread(() -> queryHandler(clientSocket))).start();
                    } catch (SocketTimeoutException ignored) {
                    }
                }
            } catch (IOException e) {
                close();
            }
        }

        private void queryHandler(Socket socket) {
            Connection curConnection = new Connection(socket);
            while (!end && !curConnection.isClosed()) {
                switch (curConnection.readQueryType()) {
                    case Connection.STAT_QUERY:
                        getAvailablePartOfFile(curConnection);
                        break;
                    case Connection.GET_QUERY:
                        getPartOfFile(curConnection);
                        break;
                    case Connection.END_CONNECTION:
                        curConnection.close();
                        break;
                    case Connection.EOF:
                        continue;
                    default:
                        System.out.print("Undefined query");
                }
            }
        }

        private void getPartOfFile(Connection curConnection) {
            int id = curConnection.readInt();
            int part = curConnection.readInt();
            ClientFileInfo curInfo = clientFileData.getIdFileMap().get(id);
            if (!curInfo.getPartsOfFile().contains(part)) {
                curConnection.close();
                return;
            }
            int sizeOfPiece;
            if (part != curInfo.getCountOfPieces() - 1) {
                sizeOfPiece = ClientFileInfo.SIZE_OF_FILE_PIECE;
            } else {
                sizeOfPiece = (int) curInfo.getSize() % ClientFileInfo.SIZE_OF_FILE_PIECE;
            }
            try {
                curConnection.sendPart(clientFileData.getIdFileMap().get(id).getFile(),
                        part * ClientFileInfo.SIZE_OF_FILE_PIECE, sizeOfPiece);
            } catch (IOException e) {
                e.printStackTrace();
                curConnection.close();
            }
        }

        private void getAvailablePartOfFile(Connection curConnection) {
            int fileId = curConnection.readInt();
            Set<Integer> parts = clientFileData.getIdFileMap().get(fileId).getPartsOfFile();
            try {
                curConnection.sendInt(parts.size());
                curConnection.sendIntegerSet(parts);
            } catch (IOException e) {
                e.printStackTrace();
                curConnection.close();
            }
        }
    }

    public Set<TorrentClient.FileInfo> getList() {
        return torrentClient.getList();
    }

    public int upload(Path filePath) {
        return torrentClient.upload(filePath);
    }

    public Set<ClientInfo> sources(int id) {
        return torrentClient.sources(id);
    }

    public String ipAsString(byte[] ip) {
        String result = "";
        for (int i = 0; i < Connection.COUNT_IP_PARTS; i++) {
            if (i != 0) {
                result += '.';
            }
            result += Integer.toString(ip[i]);
        }
        return result;
    }

    public Thread load(int id, boolean allowedDelte) {
        return torrentClient.load(id, allowedDelte);
    }
}
