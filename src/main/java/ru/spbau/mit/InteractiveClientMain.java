package ru.spbau.mit;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by n_buga on 16.05.16.
 */
public final class InteractiveClientMain {
    private static final JFrame FRAME = new JFrame("Client");
    private static final Container CONTENT_PANE = FRAME.getContentPane();
    private static Client client;

    private InteractiveClientMain() {
    }

    public static void main(String[] args) {
        ButtonGroup buttonGroup = new ButtonGroup();
        JTextField jTextField = new JTextField("127.0.0.1");
        JToggleButton jToggleButtonLoad = new JToggleButton("load mode");
        buttonGroup.add(jToggleButtonLoad);

        jToggleButtonLoad.addActionListener(e -> {
            client = tryToConnect(jTextField);
            if (client != null)  {
                Set<Client.TorrentClient.FileInfo> fileList = client.getList();
            handlerLoadMode(fileList);
            }
        });

        JToggleButton jToggleButtonUpload = new JToggleButton("upload mode");
        buttonGroup.add(jToggleButtonUpload);

        jToggleButtonUpload.addActionListener(e -> {
            client = tryToConnect(jTextField);
            if (client != null) {
                handlerUploadMode();
            }
        });

        JToggleButton jToggleButtonRun = new JToggleButton("run mode");
        buttonGroup.add(jToggleButtonRun);

        jToggleButtonRun.addActionListener(e -> {
            client = tryToConnect(jTextField);
            handlerRunMode();
        });

        Label label = new Label("server ip:");

        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
        buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        buttonPane.add(label);
        buttonPane.add(jTextField);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(jToggleButtonLoad);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(jToggleButtonUpload);
        buttonPane.add(Box.createHorizontalGlue());
        buttonPane.add(jToggleButtonRun);
        CONTENT_PANE.add(buttonPane, BorderLayout.NORTH);

        FRAME.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                if (client != null) {
                    client.close();
                }
                System.exit(0);
            }
        });

        FRAME.setSize(1200, 600);
        FRAME.setResizable(false);
        FRAME.setVisible(true);
    }

    private static Client tryToConnect(JTextField jTextField) {
        if (client != null && client.getStateClient() == Client.State.RUNNING) {
            client.close();
        }
        if (jTextField.getText().isEmpty()) {
            JOptionPane.showMessageDialog(FRAME, "You don't write ip of server!");
            return null;
        }
        client = new Client(jTextField.getText());
        if (!checkConnection()) {
            client.close();
            JOptionPane.showMessageDialog(FRAME, "Bad ip or connection. Cannot connect to server.");
            return null;
        }
        return client;
    }

    private static boolean checkConnection() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
            return false;
        }
        return client.getStateClient() == Client.State.RUNNING;
    }

    private static void handlerRunMode() {
        deleteOldElements();

        SwingUtilities.invokeLater(() -> {
            JLabel label = new JLabel("Work!", SwingConstants.CENTER);
            CONTENT_PANE.add(label, BorderLayout.CENTER);
            drawFrame();
            client.run();
        });
    }

    private static void handlerUploadMode() {

        deleteOldElements();

        SwingUtilities.invokeLater(() -> {
            Container insertPain = new Container();
            FlowLayout flowLayout = new FlowLayout(FlowLayout.CENTER);
            insertPain.setLayout(flowLayout);
            JFileChooser fileopen = new JFileChooser();
            int ret = fileopen.showDialog(null, "Открыть файл");
            if (ret == JFileChooser.APPROVE_OPTION) {
                File file = fileopen.getSelectedFile();
                JLabel fileName = new JLabel(file.toString());
                insertPain.add(fileName);
                JButton acceptButton = new JButton("upload");
                acceptButton.addActionListener(e -> client.uploadInfo(file.toPath()));
                insertPain.add(acceptButton);
                CONTENT_PANE.add(insertPain, BorderLayout.CENTER);
            }
            drawFrame();
        });
    }

    private static void handlerLoadMode(Set<Client.TorrentClient.FileInfo> fileInfos) {

        deleteOldElements();

        SwingUtilities.invokeLater(() -> {
            Container insertPane = new Container();

            ButtonGroup buttonGroup = new ButtonGroup();
            Map<JToggleButton, Client.TorrentClient.FileInfo> availableFiles = new HashMap<>();
            for (Client.TorrentClient.FileInfo fileInfo : fileInfos) {
                JToggleButton curButton = new JToggleButton("name " + fileInfo.getName()
                        + "; size = " + fileInfo.getSize() + "; id = " + fileInfo.getID());
                buttonGroup.add(curButton);
                availableFiles.put(curButton, fileInfo);
            }

            BoxLayout boxLayout = new BoxLayout(insertPane, BoxLayout.Y_AXIS);
            insertPane.setLayout(boxLayout);
            for (JToggleButton toggleButton : availableFiles.keySet()) {
                insertPane.add(toggleButton);
            }

            JButton buttonAccept = new JButton("download");
            buttonAccept.setMaximumSize(new Dimension(buttonAccept.getWidth(), buttonAccept.getHeight()));
            buttonAccept.addActionListener(e -> {
                for (JToggleButton button : availableFiles.keySet()) {
                    if (button.isSelected()) {
                        BorderLayout layout = (BorderLayout) CONTENT_PANE.getLayout();
                        if (layout.getLayoutComponent(BorderLayout.SOUTH) != null) {
                            CONTENT_PANE.remove(layout.getLayoutComponent(BorderLayout.SOUTH));
                        }
                        client.markAsWantToLoad(availableFiles.get(button).getID());
                        JProgressBar progressBar = new JProgressBar();
                        progressBar.setStringPainted(true);
                        progressBar.setMinimum(0);
                        progressBar.setValue(0);
                        FlowLayout flowLayout = new FlowLayout();
                        Container labelAndProgress = new Container();
                        labelAndProgress.setLayout(flowLayout);
                        JLabel fileLabel = new JLabel(availableFiles.get(button).getName() + "; id = "
                                + availableFiles.get(button).getID());
                        labelAndProgress.add(fileLabel);
                        labelAndProgress.add(progressBar);
                        CONTENT_PANE.add(labelAndProgress, BorderLayout.SOUTH);
                        drawFrame();
                        Set<Thread> threads = client.run();
                        for (Thread t: threads) {
                            progressBar.setMaximum(availableFiles.get(button).getCountParts());
                            ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
                            executorService.scheduleAtFixedRate(() ->
                                    progressBar.setValue(client.getLoadedParts(t)), 0, 2, TimeUnit.SECONDS);
                            try {
                                t.join();
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                                return;
                            } finally {
                                executorService.shutdown();
                                progressBar.setValue(progressBar.getMaximum());
                            }
                        }
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(insertPane);
            CONTENT_PANE.add(scrollPane, BorderLayout.CENTER);
            CONTENT_PANE.add(buttonAccept, BorderLayout.EAST);
            drawFrame();
        });
    }

    private static void deleteOldElements() {
        SwingUtilities.invokeLater(() -> {
            BorderLayout layout = (BorderLayout) CONTENT_PANE.getLayout();
            if (layout.getLayoutComponent(BorderLayout.CENTER) != null) {
                CONTENT_PANE.remove(layout.getLayoutComponent(BorderLayout.CENTER));
            }
            if (layout.getLayoutComponent(BorderLayout.EAST) != null) {
                CONTENT_PANE.remove(layout.getLayoutComponent(BorderLayout.EAST));
            }
            if (layout.getLayoutComponent(BorderLayout.SOUTH) != null) {
                CONTENT_PANE.remove(layout.getLayoutComponent(BorderLayout.SOUTH));
            }
            drawFrame();
        });
    }

    private static void drawFrame() {
        FRAME.repaint();
        FRAME.setVisible(true);
    }
}
