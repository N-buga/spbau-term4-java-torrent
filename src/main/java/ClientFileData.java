import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Created by n_buga on 10.04.16.
 */
public class ClientFileData {
    private Set<Integer> idAvailableFiles;
    private Map<Integer, ClientFileInfo> idFileMap;

    private final String CLIENT_DIRECTORY = "ClientData";
    private final String FILES_DATA = "FilesData";


    ClientFileData() {
        idAvailableFiles = new HashSet<>();
        idFileMap = new HashMap<>();
    }

    public void resetData() {
        Path toDataFile = Paths.get(".", CLIENT_DIRECTORY, FILES_DATA);
        try {
            Files.deleteIfExists(toDataFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addFile(int id, long size, Path filePath) {
        if (!idFileMap.containsKey(id)) {
            idAvailableFiles.add(id);
            idFileMap.put(id, new ClientFileInfo(size, filePath));
        }
    }

    public void addPart(int id, int part) {
        idFileMap.get(id).addAvailablePart(part);
    }

    public void addAllParts(int id) {
        idFileMap.get(id).addAllParts();
    }

    public Set<Integer> getIdAvailableFiles() {
        return idAvailableFiles;
    }

    public Map<Integer, ClientFileInfo> getIdFileMap() {
        return idFileMap;
    }

    public boolean saveDataToFile() {
        Path pathToFile = Paths.get(".", CLIENT_DIRECTORY, FILES_DATA);
        try {
            Files.createDirectories(pathToFile.getParent());
            Files.deleteIfExists(pathToFile);
            Files.createFile(pathToFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (PrintWriter writer = new PrintWriter(pathToFile.toString(), "UTF-8")) {
            writer.printf("%d\n", idFileMap.size());
            for (int key: idFileMap.keySet()) {
                writer.printf("%d\n", key);
                idFileMap.get(key).printClientInfo(writer);
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public void updateDataFromFile() {
        Path pathToFile = Paths.get(".", CLIENT_DIRECTORY, FILES_DATA);
        if (!Files.exists(pathToFile)) {
            return;
        }
        try (Scanner scanner = new Scanner(new File(pathToFile.toString()))) {
            int countOfFiles = scanner.nextInt();
            for (int i = 0; i < countOfFiles; i++) {
                int id = scanner.nextInt();
                ClientFileInfo curFileInfo = ClientFileInfo.readClientInfo(scanner);
                idFileMap.put(id, curFileInfo);
            }
            for (Integer id: idFileMap.keySet()) {
                idAvailableFiles.add(id);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
