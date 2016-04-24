package ru.spbau.mit;

import java.nio.file.Paths;
import java.util.Set;

/**
 * Created by n_buga on 23.04.16.
 */
public class TorrentClientMain {
    public static void main(String[] args) {
        final int minAllowedCountArgs = 2;
        if (args.length < minAllowedCountArgs) {
            System.out.println("Wrong format");
            outFormat();
            return;
        }
        String command = args[0];
        String address = args[1];
        Client client = new Client(address);

        switch (command) {
            case "list":
                Set<Client.TorrentClient.FileInfo> answer = client.getList();
                System.out.printf("The count of files: %d\nFiles are:\n", answer.size());
                for (Client.TorrentClient.FileInfo file: answer) {
                    System.out.printf("Name = %s, size = %d, id = %d\n", file.getName(),
                            file.getSize(), file.getID());
                }
                break;
            case "get":
                assertExtraArgs(args);
                String fileStringID = args[2];
                int fileID = -1;
                try {
                    fileID = Integer.parseInt(fileStringID);
                } catch (NumberFormatException e) {
                    System.out.println("ID isn't a number");
                    outFormat();
                    return;
                }
                client.markAsWantToLoad(fileID);
                break;
            case "newfile":
                assertExtraArgs(args);
                String path = args[2];
                client.uploadInfo(Paths.get(path));
                break;
            case "run":
                client.run();
                while (true) {
                    int b = 1;
                }
            case "help":
                outFormat();
            default:
                System.out.println("Wrong format");
                outFormat();
        }
        client.close();
    }

    private static void outFormat() {
        System.out.printf("You can use the next formats: \n"
                + "list <tracker-address> = get the list of available files from server\n"
                + "get <tracker-address> <file-id> = mark the file to load in the future\n"
                + "newfile <tracker-address> <path> = add available file to server\n"
                + "run <tracker-address> = load all files, that we want to load\n");
    }

    private static void assertExtraArgs(String[] args) {
        if (args.length < 3) {
            System.out.println("Wrong format");
            outFormat();
            System.exit(0);
        }
    }
}
