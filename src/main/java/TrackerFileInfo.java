/**
 * Created by n_buga on 31.03.16.
 */

import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

public class TrackerFileInfo {
    private int id;
    private String name;
    private long size;
    private Set<ClientInfo> clientInfos = new HashSet<>();

    TrackerFileInfo(String name, long size, int id) {
        this.name = name;
        this.size = size;
        this.id = id;
    }

    public void addClient(ClientInfo clientInfo) {
        clientInfos.add(clientInfo);
    }

    public int getID() {
        return id;
    }

    public Set<ClientInfo> getClientInfos() {
        return clientInfos;
    }

    public String getName() {
        return name;
    }

    public long getSize() {
        return size;
    }

    static public TrackerFileInfo readFromFile(Scanner scanner) {
        int curId = scanner.nextInt();
        long size = scanner.nextLong();
        String name = scanner.next();
        TrackerFileInfo result = new TrackerFileInfo(name, size, curId);
        int countOfClients = scanner.nextInt();
        for (int i = 0; i < countOfClients; i++) {
            ClientInfo curClientInfo = ClientInfo.readFromFile(scanner);
            result.addClient(curClientInfo);
        }
        return result;
    }

    public boolean writeToFile(PrintWriter writer) {
        writer.printf("%d ", id);
        writer.print(size);
        writer.printf(" %s\n", name);
        writer.printf("%d\n", clientInfos.size());
        for (ClientInfo clientInfo: clientInfos) {
            clientInfo.writeToFile(writer);
        }
        return true;
    }
}
