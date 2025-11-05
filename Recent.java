import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Recent extends JFrame {
    private JTable table;
    private DefaultTableModel tableModel;
    private JButton refreshButton;
    private JLabel statusLabel;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Recent() {
        // Set up the JFrame
        setTitle("Recent Files and Folders");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize table model
        String[] columns = {"Name", "Type", "Created", "Modified", "Path"};
        tableModel = new DefaultTableModel(columns, 0);
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);

        // Center text in Type, Created, and Modified columns
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        table.getColumnModel().getColumn(1).setCellRenderer(centerRenderer); // Type
        table.getColumnModel().getColumn(2).setCellRenderer(centerRenderer); // Created
        table.getColumnModel().getColumn(3).setCellRenderer(centerRenderer); // Modified

        // Add components to the frame
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(new JScrollPane(table), BorderLayout.CENTER);

        // Refresh button and status label
        refreshButton = new JButton("Refresh");
        statusLabel = new JLabel("Ready");
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomPanel.add(refreshButton);
        bottomPanel.add(statusLabel);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel);

        // Refresh button action
        refreshButton.addActionListener(e -> refreshFileList());

        // Initial population of the table
        refreshFileList();
    }

    private void refreshFileList() {
        tableModel.setRowCount(0); // Clear existing rows
        statusLabel.setText("Scanning...");
        refreshButton.setEnabled(false);

        // Run file scanning in a separate thread to keep GUI responsive
        executor.submit(() -> {
            try {
                List<FileInfo> fileList = new ArrayList<>();
                String startPath = System.getProperty("user.home"); // Default to user home directory
                scanDirectory(Paths.get(startPath), fileList);

                // Sort by modification time, newest first
                fileList.sort(Comparator.comparing(FileInfo::getModifiedTime).reversed());

                // Update table on EDT
                SwingUtilities.invokeLater(() -> {
                    // Track maximum content length for each column
                    int[] maxWidths = new int[tableModel.getColumnCount()];
                    FontMetrics fontMetrics = table.getFontMetrics(table.getFont());

                    // Add rows and calculate max content width
                    for (FileInfo file : fileList) {
                        String[] rowData = new String[]{
                            file.getName(),
                            file.isDirectory() ? "Folder" : "File",
                            formatTime(file.getCreatedTime()),
                            formatTime(file.getModifiedTime()),
                            file.getPath()
                        };
                        tableModel.addRow(rowData);

                        // Update max widths based on content
                        for (int i = 0; i < rowData.length; i++) {
                            int width = fontMetrics.stringWidth(rowData[i] != null ? rowData[i] : "");
                            maxWidths[i] = Math.max(maxWidths[i], width);
                        }
                    }

                    // Adjust column widths based on content
                    for (int i = 0; i < tableModel.getColumnCount(); i++) {
                        TableColumn column = table.getColumnModel().getColumn(i);
                        // Add padding (20 pixels) to max content width
                        int width = maxWidths[i] + 20;
                        // Ensure minimum width for readability
                        width = Math.max(width, fontMetrics.stringWidth(tableModel.getColumnName(i)) + 20);
                        column.setPreferredWidth(width);
                    }

                    statusLabel.setText("Scan complete. " + fileList.size() + " items found.");
                    refreshButton.setEnabled(true);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + ex.getMessage());
                    refreshButton.setEnabled(true);
                });
            }
        });
    }

    private void scanDirectory(Path dir, List<FileInfo> fileList) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
            for (Path path : stream) {
                try {
                    BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
                    fileList.add(new FileInfo(
                        path.getFileName().toString(),
                        path.toString(),
                        attrs.isDirectory(),
                        attrs.creationTime(),
                        attrs.lastModifiedTime()
                    ));

                    // Recursively scan subdirectories
                    if (attrs.isDirectory()) {
                        scanDirectory(path, fileList);
                    }
                } catch (IOException | SecurityException ex) {
                    // Skip files/folders we can't access
                    System.err.println("Error accessing " + path + ": " + ex.getMessage());
                }
            }
        } catch (IOException | SecurityException ex) {
            System.err.println("Error scanning directory " + dir + ": " + ex.getMessage());
        }
    }

    private String formatTime(FileTime fileTime) {
        return fileTime != null ? dateFormat.format(fileTime.toMillis()) : "Unknown";
    }

    // Helper class to store file information
    private static class FileInfo {
        private final String name;
        private final String path;
        private final boolean isDirectory;
        private final FileTime createdTime;
        private final FileTime modifiedTime;

        public FileInfo(String name, String path, boolean isDirectory, FileTime createdTime, FileTime modifiedTime) {
            this.name = name;
            this.path = path;
            this.isDirectory = isDirectory;
            this.createdTime = createdTime;
            this.modifiedTime = modifiedTime;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public boolean isDirectory() {
            return isDirectory;
        }

        public FileTime getCreatedTime() {
            return createdTime;
        }

        public FileTime getModifiedTime() {
            return modifiedTime;
        }
    }

    public static void main(String[] args) {
        // Run GUI on EDT
        SwingUtilities.invokeLater(() -> {
            Recent recent = new Recent();
            recent.setVisible(true);
        });
    }
}
