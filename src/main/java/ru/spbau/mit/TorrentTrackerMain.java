package ru.spbau.mit;

/**
 * Created by n_buga on 23.04.16.
 */


public abstract class TorrentTrackerMain {

    public static void main(String[] args) {
        try (TorrentTracker torrenttracker = new TorrentTracker()) {
            torrenttracker.start();
            System.out.print("Run!");
            while (true) {
                int i;
            }
        }
    }
}
