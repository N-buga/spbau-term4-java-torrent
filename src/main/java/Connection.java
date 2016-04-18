/**
 * Created by n_buga on 31.03.16.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;

public class Connection implements AutoCloseable {
    static public final int END_CONNECTION = -2;
    static public final int EOF = -1;
    static public final int LIST_QUERY = 1;
    static public final int UPLOAD_QUERY = 2;
    static public final int SOURCES_QUERY = 3;
    static public final int UPDATE_QUERY = 4;
    static public final int STAT_QUERY = 1;
    static public final int GET_QUERY = 2;
    static public final int COUNT_IP_PARTS = 4;

    private boolean isClosed = false;
    private boolean isUpdated = false;
    private ClientInfo clientInfo = null;

    private DataInputStream in;
    private DataOutputStream out;
    private Socket socket;

    public Connection(Socket socket) {
        try {
            this.socket = socket;
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setClientInfo(ClientInfo clientInfo) {
        this.clientInfo = clientInfo;
    }

    public ClientInfo getClientInfo() {
        return clientInfo;
    }

    public void update() {
        isUpdated = true;
    }

    public void resetIsUpdated() {
        isUpdated = false;
    }

    public boolean isUpdated() {
        return isUpdated;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public void close() {
        isClosed = true;
        try {
            in.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public int readQueryType() {
        try {
            return in.readInt();
        } catch (IOException e) {
            return END_CONNECTION;
        }
    }

    public Boolean readBoolean() throws IOException {
        return in.readBoolean();
    }

    public int readInt() {
        try {
            return in.readInt();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return END_CONNECTION;
    }

    public long readLong() {
        try {
            return in.readLong();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return END_CONNECTION;
    }

    public String readString() {
        try {
            return in.readUTF();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public short readShort() {
        try {
            return in.readShort();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return END_CONNECTION;
    }

    public byte[] readPart() {
        int countOfBytes = readInt();
        byte[] part = new byte[countOfBytes];
        try {
            int readBytes = in.read(part);
            if (readBytes != countOfBytes) {
                return null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return part;
    }

    public Set<Integer> readSet(int countFiles) {
        Set<Integer> result = new HashSet<>();
        for (int i = 0; i < countFiles; i++) {
            int curId = readInt();
            result.add(curId);
        }
        return result;
    }

    public ClientInfo readClient() {
        byte[] ip = new byte[COUNT_IP_PARTS];
        try {
            in.read(ip);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int port = readInt();
        return new ClientInfo(ip, port);
    }

    public void sendListOfAvailableFiles(int size, Set<TrackerFileInfo> files) throws IOException {
        out.writeInt(size);
        out.flush();
        for (TrackerFileInfo file: files) {
            out.writeInt(file.getID());
            out.writeUTF(file.getName());
            out.writeLong(file.getSize());
            out.flush();
        }
    }

    public void sendType(int type) throws IOException {
        sendInt(type);
    }

    public void sendLong(long l) throws IOException {
        out.writeLong(l);
        out.flush();
    }

    public void sendString(String s) throws IOException {
        out.writeUTF(s);
        out.flush();
    }

    public void sendInt(int i) throws IOException {
        out.writeInt(i);
        out.flush();
    }

    public void sendBoolean(boolean b) throws IOException {
        out.writeBoolean(b);
        out.flush();
    }

    public void sendSources(Set<ClientInfo> clientInfos) throws IOException {
        out.writeInt(clientInfos.size());
        out.flush();
        for (ClientInfo clientInfo : clientInfos) {
            out.write(clientInfo.getServerIP());
            out.writeInt(clientInfo.getServerPort());
            out.flush();
        }
    }
    public void sendIntegerSet(Set<Integer> num) throws IOException {
        for (Integer number: num) {
            out.writeInt(number);
            out.flush();
        }
    }

    public void sendPart(RandomAccessFile file, int offset, int len) throws IOException {
        byte[] data = new byte[len];
        file.read(data);
        out.writeInt(len);
        out.write(data, 0, len);
        out.flush();
    }

}
