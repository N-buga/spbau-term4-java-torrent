import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by n_buga on 11.04.16.
 */
public final class ClientMain {
    private ClientMain() {

    }

    static void outInfo() {
        String info = "Hello! Here's your oppotunities:\n"
                + "list = to get the available files from tracker\n"
                + "upload <Path from current directory>:String = tell server about you can give this file out\n"
                + "sources <id>:int = get the information about servers given out file with that id\n"
                + "load <id>:int [-d] = load file with current id\n"
                + "\t -d = if file exists, then it is allowed to delete it"
                + "exit = exit";
        System.out.println(info);
    }
    public static void main(String[] args) {
        outInfo();
        Scanner input = new Scanner(System.in);
        Client client = new Client("127.0.0.1");
        int i = 0;
        while (input.hasNext()) {
            String command = input.next();
            switch (command) {
                case "list":
                    System.out.println(command);
                    Set<Client.TorrentClient.FileInfo> answer = client.getList();
                    System.out.printf("The count of files: %d\nFiles are:\n", answer.size());
                    for (Client.TorrentClient.FileInfo file: answer) {
                        System.out.printf("Name = %s, size = %d, id = %d\n", file.getName(),
                                file.getSize(), file.getID());
                    }
                    break;
                case "upload":
                    String fileName = input.next();
                    Path file = Paths.get(".", fileName);
                    client.upload(file);
                    System.out.println("Ready!");
                    break;
                case "sources":
                    int id = input.nextInt();
                    Set<ClientInfo> clientInfos = client.sources(id);
                    for (ClientInfo clientInfo: clientInfos) {
                        System.out.printf("serverPort = %d\n", clientInfo.getServerPort());
                        System.out.printf("serverIp = %s\n", client.ipAsString(clientInfo.getServerIP()));
                    }
                    break;
                case "load":
                    String word = input.next();
                    if (word.equals("-d")) {
                        id = input.nextInt();
                        client.load(id, true);
                    } else {
                        try {
                            id = Integer.parseInt(word);
                            client.load(id, false);
                        } catch (NumberFormatException e) {
                            System.out.println("Wrong format");
                            outInfo();
                        }
                    }
                    break;
                case "exit":
                    input.close();
                    client.close();
                    return;
                default:
            }
        }
    }
}
