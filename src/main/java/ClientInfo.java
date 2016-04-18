import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Scanner;

/**
 * Created by n_buga on 31.03.16.
 */
public class ClientInfo {
    private byte[] serverIP;
    private int serverPort;

    public ClientInfo(byte[] serverIP, int port) {
        this.serverIP = serverIP;
        serverPort = port;
    }

    public byte[] getServerIP() {
        return serverIP;
    }

    public int getServerPort() {
        return serverPort;
    }

    public boolean myEquals(ClientInfo clientInfo) {
        return (Arrays.equals(serverIP, clientInfo.getServerIP()) && serverPort == clientInfo.getServerPort());
    }

    static public ClientInfo readFromFile(Scanner scanner) {
        byte[] ip = new byte[Connection.COUNT_IP_PARTS];
        for (int i = 0; i < Connection.COUNT_IP_PARTS; i++) {
            ip[i] = scanner.nextByte();
        }
        int port = scanner.nextInt();
        return new ClientInfo(ip, port);
    }

    public boolean writeToFile(PrintWriter writer) {
        for (int i = 0; i < Connection.COUNT_IP_PARTS; i++) {
            writer.print(serverIP[i]);
            writer.print(" ");
        }
        writer.print("\n");
        writer.printf("%d\n", serverPort);
        return true;
    }
}
