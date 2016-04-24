package ru.spbau.mit;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.BitSet;
import java.util.Scanner;

/**
 * Created by n_buga on 10.04.16.
 */
public class ClientFileInfo {
    public static final int SIZE_OF_FILE_PIECE = (int) 1e5;

    private BitSet partsOfFile;
    private Path filePath;
    private long size;
    private int countOfPieces = 0;

    public ClientFileInfo(long size, Path filePath) {
        this.size = size;
        this.filePath = filePath;
        countOfPieces =  (int) ((size - 1) / SIZE_OF_FILE_PIECE) + 1;
        partsOfFile = new BitSet(countOfPieces);
    }

    public void addAllParts() {
        partsOfFile.set(0, countOfPieces);
    }

    public boolean addAvailablePart(int part) {
        if (part > countOfPieces) {
            return false;
        }
        partsOfFile.set(part);
        return true;
    }

    public int getCountOfPieces() {
        return countOfPieces;
    }

    public BitSet getPartsOfFile() {
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
        writer.println(partsOfFile.cardinality());

        for (int i = 0; i < partsOfFile.size(); i++) {
            if (partsOfFile.get(i)) {
                writer.printf("%d ", i);
            }
        }

        writer.println();
    }

    public static ClientFileInfo readClientInfo(Scanner scanner) {
        String stringPath = scanner.next();
        Path path = Paths.get(stringPath);
        long sizeOfFile = scanner.nextLong();
        int countOfParts = scanner.nextInt();
        BitSet parts = new BitSet(countOfParts);
        for (int i = 0; i < countOfParts; i++) {
            int curPart = scanner.nextInt();
            parts.set(curPart);
        }
        if (Files.exists(path)) {
            ClientFileInfo result = new ClientFileInfo(sizeOfFile, path);
            for (int part = 0; part < countOfParts; part++) {
                if (parts.get(part)) {
                    result.addAvailablePart(part);
                }
            }
            return result;
        } else {
            return null;
        }
    }
}
