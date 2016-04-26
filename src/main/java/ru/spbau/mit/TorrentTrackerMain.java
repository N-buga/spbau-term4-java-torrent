package ru.spbau.mit;

/**
 * Created by n_buga on 23.04.16.
 */


public abstract class TorrentTrackerMain {
    private static final int TIME_OUT_AFTER_SERVER_START = 100;

    public static void main(String[] args) {
        try (TorrentTracker torrenttracker = new TorrentTracker()) {
            torrenttracker.start();
            System.out.print("Run!");
            try {
                Thread.sleep(TIME_OUT_AFTER_SERVER_START);
            } catch (InterruptedException e) {
/* !!!! */      e.printStackTrace();
            }
            while (true) {
                int i;
            }
        }
    }
}
