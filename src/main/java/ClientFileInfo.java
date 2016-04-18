import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

/**
 * Created by n_buga on 10.04.16.
 */
public class ClientFileInfo {
    public static final int SIZE_OF_FILE_PIECE = (int) 1e5;

    private Set<Integer> partsOfFile;
    private Path filePath;
    private long size;
    private int countOfPieces = 0;

    public ClientFileInfo(long size, Path filePath) {
        this.size = size;
        this.filePath = filePath;
        partsOfFile = new HashSet<>();
        countOfPieces =  (int) ((size - 1) / SIZE_OF_FILE_PIECE) + 1;
    }

    public void addAllParts() {
        for (int i = 0; i < ((size - 1) / SIZE_OF_FILE_PIECE + 1); i++) {
            partsOfFile.add(i);
        }
    }

    public boolean addAvailablePart(int part) {
        if (part > countOfPieces) {
            return false;
        }
        partsOfFile.add(part);
        return true;
    }

    public int getCountOfPieces() {
        return countOfPieces;
    }

    public Set<Integer> getPartsOfFile() {
        return partsOfFile;
    }

    public RandomAccessFile getFile() {
        try {
            return new RandomAccessFile(filePath.toString(), "rw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    public long getSize() {
        return size;
    }

    public void printClientInfo(PrintWriter writer) throws IOException {
        writer.printf(filePath.toAbsolutePath().toString() + " " + "%d\n", size);
        writer.println(partsOfFile.size());
        for (Integer part: partsOfFile) {
            writer.printf("%d ", part);
        }
        writer.println();
    }

    static public ClientFileInfo readClientInfo(Scanner scanner) {
        String stringPath = scanner.next();
        Path path = Paths.get(stringPath);
        long sizeOfFile = scanner.nextLong();
        int countOfParts = scanner.nextInt();
        Set<Integer> parts = new HashSet<>();
        for (int i = 0; i < countOfParts; i++) {
            int curPart = scanner.nextInt();
            parts.add(curPart);
        }
        if (Files.exists(path)) {
            ClientFileInfo result = new ClientFileInfo(sizeOfFile, path);
            for (Integer part: parts) {
                result.addAvailablePart(part);
            }
            return result;
        } else {
            return null;
        }
    }
}
