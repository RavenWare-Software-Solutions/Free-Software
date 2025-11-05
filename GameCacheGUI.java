import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;
import java.util.zip.CRC32;

public class GameCacheGUI extends JFrame {
    // Cache components
    private GameCache gameCache;
    private final String cacheBasePath = "game_cache";
    
    // GUI components
    private JTabbedPane tabbedPane;
    private JPanel mainPanel;
    private JPanel importPanel;
    private JPanel managePanel;
    private JTextArea logArea;
    private JTable fileTable;
    private DefaultTableModel tableModel;
    private JComboBox<Integer> archiveComboBox;
    private JLabel statusLabel;
    
    // File management
    private List<File> pendingFiles;
    private Map<File, Integer> fileArchiveMapping;
    
    public GameCacheGUI() {
        super("RS-Style Cache Manager");
        initializeComponents();
        setupGUI();
        setupEventHandlers();
        loadCache();
    }
    
    private void initializeComponents() {
        pendingFiles = new ArrayList<>();
        fileArchiveMapping = new HashMap<>();
        
        // Initialize table model
        String[] columns = {"Filename", "Archive ID", "File ID", "Size", "Container"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        
        // Initialize archive combo box
        archiveComboBox = new JComboBox<>();
        for (int i = 0; i < 10; i++) {
            archiveComboBox.addItem(i);
        }
    }
    
    private void setupGUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        setSize(800, 600);
        
        // Create main tabbed pane
        tabbedPane = new JTabbedPane();
        
        // Create panels
        importPanel = createImportPanel();
        managePanel = createManagePanel();
        
        tabbedPane.addTab("Import Files", importPanel);
        tabbedPane.addTab("Manage Cache", managePanel);
        
        // Log area
        logArea = new JTextArea(8, 60);
        logArea.setEditable(false);
        logArea.setBackground(new Color(240, 240, 240));
        logArea.setBorder(new TitledBorder("Activity Log"));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        
        // Status bar
        statusLabel = new JLabel("Ready");
        statusLabel.setBorder(BorderFactory.createLoweredBevelBorder());
        
        // Main layout
        add(tabbedPane, BorderLayout.CENTER);
        add(logScrollPane, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);
        
        centerFrame();
    }
    
    private JPanel createImportPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Top controls
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton addFilesBtn = new JButton("Add Files");
        JButton clearFilesBtn = new JButton("Clear Files");
        JButton setArchiveBtn = new JButton("Set Archive for Selected");
        
        topPanel.add(addFilesBtn);
        topPanel.add(clearFilesBtn);
        topPanel.add(new JLabel("Archive:"));
        topPanel.add(archiveComboBox);
        topPanel.add(setArchiveBtn);
        
        // File list
        fileTable = new JTable(tableModel);
        fileTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane tableScroll = new JScrollPane(fileTable);
        tableScroll.setBorder(new TitledBorder("Pending Files"));
        
        // Action buttons
        JPanel buttonPanel = new JPanel(new FlowLayout());
        JButton buildCacheBtn = new JButton("Build New Cache");
        JButton updateCacheBtn = new JButton("Update Existing Cache");
        JButton changeFilesBtn = new JButton("Change Files (TODO)");
        
        buttonPanel.add(buildCacheBtn);
        buttonPanel.add(updateCacheBtn);
        buttonPanel.add(changeFilesBtn);
        
        // Add action listeners
        addFilesBtn.addActionListener(e -> addFiles());
        clearFilesBtn.addActionListener(e -> clearFiles());
        setArchiveBtn.addActionListener(e -> setArchiveForSelected());
        buildCacheBtn.addActionListener(e -> buildCache());
        updateCacheBtn.addActionListener(e -> updateCache());
        changeFilesBtn.addActionListener(e -> showChangeNotImplemented());
        
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tableScroll, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createManagePanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // Cache info
        JTextArea infoArea = new JTextArea(15, 60);
        infoArea.setEditable(false);
        infoArea.setBorder(new TitledBorder("Cache Information"));
        updateCacheInfo(infoArea);
        
        JScrollPane infoScroll = new JScrollPane(infoArea);
        
        // Refresh button
        JButton refreshBtn = new JButton("Refresh Cache Info");
        refreshBtn.addActionListener(e -> updateCacheInfo(infoArea));
        
        panel.add(infoScroll, BorderLayout.CENTER);
        panel.add(refreshBtn, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (gameCache != null) {
                    log("Cache manager closed");
                }
            }
        });
    }
    
    private void centerFrame() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation(screenSize.width / 2 - getWidth() / 2, screenSize.height / 2 - getHeight() / 2);
    }
    
    // File management methods
    private void addFiles() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setDialogTitle("Select Files to Import");
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();
            for (File file : files) {
                if (!pendingFiles.contains(file)) {
                    pendingFiles.add(file);
                    fileArchiveMapping.put(file, 0); // Default to archive 0
                    tableModel.addRow(new Object[]{
                        file.getName(),
                        0,
                        pendingFiles.size() - 1,
                        formatFileSize(file.length()),
                        0
                    });
                }
            }
            log("Added " + files.length + " files to import list");
        }
    }
    
    private void clearFiles() {
        int fileCount = pendingFiles.size();
        pendingFiles.clear();
        fileArchiveMapping.clear();
        tableModel.setRowCount(0);
        log("Cleared " + fileCount + " pending files");
    }
    
    private void setArchiveForSelected() {
        int selectedRow = fileTable.getSelectedRow();
        if (selectedRow >= 0 && selectedRow < pendingFiles.size()) {
            File file = pendingFiles.get(selectedRow);
            int archiveId = (Integer) archiveComboBox.getSelectedItem();
            fileArchiveMapping.put(file, archiveId);
            tableModel.setValueAt(archiveId, selectedRow, 1);
            log("Set archive " + archiveId + " for file: " + file.getName());
        } else {
            JOptionPane.showMessageDialog(this, "Please select a file first", "No Selection", JOptionPane.WARNING_MESSAGE);
        }
    }
    
    // Cache operations
    private void loadCache() {
        try {
            gameCache = new GameCache(cacheBasePath, 317);
            log("Cache system initialized");
            statusLabel.setText("Cache Ready - Version: 317");
        } catch (IOException e) {
            log("Error initializing cache: " + e.getMessage());
            statusLabel.setText("Cache Error - Check Log");
        }
    }
    
    private void buildCache() {
        if (pendingFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files to import", "Build Cache", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        int result = JOptionPane.showConfirmDialog(this, 
            "This will create a NEW cache. Existing cache will be overwritten.\nProceed?",
            "Build New Cache", JOptionPane.YES_NO_OPTION);
            
        if (result == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try {
                    setUIEnabled(false);
                    statusLabel.setText("Building new cache...");
                    
                    // Delete existing cache
                    new File(cacheBasePath + ".dat").delete();
                    new File(cacheBasePath + ".idx").delete();
                    
                    // Reload cache to create new files
                    gameCache = new GameCache(cacheBasePath, 317);
                    
                    // Import all files
                    importPendingFiles();
                    
                    log("New cache built successfully!");
                    JOptionPane.showMessageDialog(this, "Cache built successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                    
                } catch (Exception e) {
                    log("Error building cache: " + e.getMessage());
                    JOptionPane.showMessageDialog(this, "Error building cache: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    setUIEnabled(true);
                    statusLabel.setText("Cache Ready");
                }
            }).start();
        }
    }
    
    private void updateCache() {
        if (pendingFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No files to import", "Update Cache", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        new Thread(() -> {
            try {
                setUIEnabled(false);
                statusLabel.setText("Updating cache...");
                
                importPendingFiles();
                
                log("Cache updated successfully!");
                JOptionPane.showMessageDialog(this, "Cache updated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                
            } catch (Exception e) {
                log("Error updating cache: " + e.getMessage());
                JOptionPane.showMessageDialog(this, "Error updating cache: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            } finally {
                setUIEnabled(true);
                statusLabel.setText("Cache Ready");
            }
        }).start();
    }
    
    private void importPendingFiles() {
    int successCount = 0;
    int totalFiles = pendingFiles.size();

    for (int i = 0; i < pendingFiles.size(); i++) {
        File file = pendingFiles.get(i);
        int archiveId = fileArchiveMapping.get(file);
        final int currentFileIndex = i; // Create a final copy of i

        try {
            byte[] fileData = Files.readAllBytes(file.toPath());
            gameCache.put(archiveId, i, fileData, 0);
            log("Imported: " + file.getName() + " to archive " + archiveId + " (ID: " + i + ")");
            successCount++;

            // Update progress
            final int progress = (currentFileIndex + 1) * 100 / totalFiles;
            SwingUtilities.invokeLater(() ->
                statusLabel.setText("Importing... " + progress + "% (" + (currentFileIndex + 1) + "/" + totalFiles + ")")
            );

        } catch (IOException e) {
            log("Failed to import " + file.getName() + ": " + e.getMessage());
        }
    }

    // Clear pending files after import
    SwingUtilities.invokeLater(() -> {
        pendingFiles.clear();
        fileArchiveMapping.clear();
        tableModel.setRowCount(0);
    });

    log("Import completed: " + successCount + "/" + totalFiles + " files successfully imported");
}
    
    private void updateCacheInfo(JTextArea infoArea) {
        if (gameCache == null) {
            infoArea.setText("Cache not initialized");
            return;
        }
        
        StringBuilder info = new StringBuilder();
        info.append("Cache Version: 317\n");
        info.append("Base Path: ").append(cacheBasePath).append("\n\n");
        
        try {
            Map<Integer, GameCache.CacheIndex> indices = gameCache.getIndices();
            info.append("Archives: ").append(indices.size()).append("\n\n");
            
            for (Map.Entry<Integer, GameCache.CacheIndex> entry : indices.entrySet()) {
                GameCache.CacheIndex index = entry.getValue();
                info.append("Archive ").append(entry.getKey()).append(":\n");
                info.append("  Files: ").append(index.entries.size()).append("\n");
                info.append("  Protocol: ").append(index.protocol).append("\n");
                
                // Show first few files
                int count = 0;
                for (GameCache.IndexEntry fileEntry : index.entries.values()) {
                    if (count++ >= 5) {
                        info.append("  ... and ").append(index.entries.size() - 5).append(" more\n");
                        break;
                    }
                    info.append("    File ID: ").append(fileEntry.fileId)
                        .append(", Size: ").append(fileEntry.dataLength)
                        .append(" bytes\n");
                }
                info.append("\n");
            }
            
            // File sizes
            File dataFile = new File(cacheBasePath + ".dat");
            File indexFile = new File(cacheBasePath + ".idx");
            
            info.append("File Sizes:\n");
            info.append("  Data file: ").append(formatFileSize(dataFile.length())).append("\n");
            info.append("  Index file: ").append(formatFileSize(indexFile.length())).append("\n");
            
        } catch (Exception e) {
            info.append("Error reading cache info: ").append(e.getMessage());
        }
        
        infoArea.setText(info.toString());
    }
    
    // Utility methods
    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append("[" + new Date() + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    private void setUIEnabled(boolean enabled) {
        SwingUtilities.invokeLater(() -> {
            tabbedPane.setEnabled(enabled);
            Component[] components = importPanel.getComponents();
            for (Component comp : components) {
                comp.setEnabled(enabled);
            }
        });
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.1f KB", size / 1024.0);
        return String.format("%.1f MB", size / (1024.0 * 1024.0));
    }
    
    private void showChangeNotImplemented() {
        JOptionPane.showMessageDialog(this, 
            "Change functionality will be implemented in a future version.\n" +
            "This will allow modifying existing files in the cache.", 
            "Feature Coming Soon", 
            JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            new GameCacheGUI().setVisible(true);
        });
    }
}

// Game Cache Implementation (same file)
class GameCache {
    private final File dataFile;
    private final File indexFile;
    private final Map<Integer, CacheIndex> indices;
    private final int version;
    
    public GameCache(String basePath, int version) throws IOException {
        this.dataFile = new File(basePath + ".dat");
        this.indexFile = new File(basePath + ".idx");
        this.indices = new HashMap<>();
        this.version = version;
        
        // Ensure files exist
        if (!dataFile.exists()) dataFile.createNewFile();
        if (!indexFile.exists()) indexFile.createNewFile();
        
        loadIndices();
    }
    
    static class CacheEntry {
        int file;
        int container;
        byte[] data;
        int crc;
        int version;
        
        CacheEntry(int file, int container, byte[] data, int crc, int version) {
            this.file = file;
            this.container = container;
            this.data = data;
            this.crc = crc;
            this.version = version;
        }
    }
    
    static class CacheIndex {
        int indexId;
        Map<Integer, IndexEntry> entries = new HashMap<>();
        int protocol;
        
        CacheIndex(int indexId, int protocol) {
            this.indexId = indexId;
            this.protocol = protocol;
        }
    }
    
    static class IndexEntry {
        int fileId;
        long dataPosition;
        int dataLength;
        int sector;
        int container;
    }
    
    private void loadIndices() throws IOException {
        if (indexFile.length() == 0) return;
        
        try (RandomAccessFile raf = new RandomAccessFile(indexFile, "r");
             FileChannel channel = raf.getChannel()) {
            
            ByteBuffer buffer = ByteBuffer.allocate((int) indexFile.length());
            channel.read(buffer);
            buffer.flip();
            
            while (buffer.remaining() >= 12) {
                int indexId = buffer.getInt();
                int protocol = buffer.getInt();
                int entryCount = buffer.getInt();
                
                CacheIndex index = new CacheIndex(indexId, protocol);
                
                for (int i = 0; i < entryCount; i++) {
                    if (buffer.remaining() < 24) break;
                    
                    IndexEntry entry = new IndexEntry();
                    entry.fileId = buffer.getInt();
                    entry.dataPosition = buffer.getLong();
                    entry.dataLength = buffer.getInt();
                    entry.sector = buffer.getInt();
                    entry.container = buffer.getInt();
                    
                    index.entries.put(entry.fileId, entry);
                }
                
                indices.put(indexId, index);
            }
        }
    }
    
    
    private void saveIndices() throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(indexFile, "rw");
             FileChannel channel = raf.getChannel()) {
            
            // Calculate total size needed
            int totalSize = indices.values().stream()
                .mapToInt(index -> 12 + (index.entries.size() * 24))
                .sum();
            
            ByteBuffer buffer = ByteBuffer.allocate(totalSize);
            
            for (CacheIndex index : indices.values()) {
                buffer.putInt(index.indexId);
                buffer.putInt(index.protocol);
                buffer.putInt(index.entries.size());
                
                for (IndexEntry entry : index.entries.values()) {
                    buffer.putInt(entry.fileId);
                    buffer.putLong(entry.dataPosition);
                    buffer.putInt(entry.dataLength);
                    buffer.putInt(entry.sector);
                    buffer.putInt(entry.container);
                }
            }
            
            buffer.flip();
            channel.write(buffer);
            channel.truncate(buffer.limit());
        }
    }
    
    public void put(int indexId, int fileId, byte[] data, int container) throws IOException {
        CacheIndex index = indices.computeIfAbsent(indexId, 
            id -> new CacheIndex(id, this.version));
        
        try (RandomAccessFile dataRaf = new RandomAccessFile(dataFile, "rw");
             FileChannel dataChannel = dataRaf.getChannel()) {
            
            // Calculate CRC for data integrity
            CRC32 crc = new CRC32();
            crc.update(data);
            int dataCrc = (int) crc.getValue();
            
            // Seek to end for new data
            long dataPosition = dataFile.length();
            dataChannel.position(dataPosition);
            
            // Write data with header
            ByteBuffer dataBuffer = ByteBuffer.allocate(data.length + 12);
            dataBuffer.putInt(data.length);
            dataBuffer.putInt(dataCrc);
            dataBuffer.putInt(version);
            dataBuffer.put(data);
            dataBuffer.flip();
            
            dataChannel.write(dataBuffer);
            
            // Update index
            IndexEntry entry = new IndexEntry();
            entry.fileId = fileId;
            entry.dataPosition = dataPosition;
            entry.dataLength = data.length + 12;
            entry.sector = (int) (dataPosition / 512);
            entry.container = container;
            
            index.entries.put(fileId, entry);
        }
        
        saveIndices();
    }
    
    public byte[] get(int indexId, int fileId) throws IOException {
        CacheIndex index = indices.get(indexId);
        if (index == null) return null;
        
        IndexEntry entry = index.entries.get(fileId);
        if (entry == null) return null;
        
        try (RandomAccessFile dataRaf = new RandomAccessFile(dataFile, "r");
             FileChannel dataChannel = dataRaf.getChannel()) {
            
            ByteBuffer buffer = ByteBuffer.allocate(entry.dataLength);
            dataChannel.position(entry.dataPosition);
            dataChannel.read(buffer);
            buffer.flip();
            
            // Read and verify header
            int dataLength = buffer.getInt();
            int storedCrc = buffer.getInt();
            int storedVersion = buffer.getInt();
            
            if (dataLength != entry.dataLength - 12) {
                throw new IOException("Data length mismatch for file " + fileId);
            }
            
            byte[] data = new byte[dataLength];
            buffer.get(data);
            
            // Verify CRC
            CRC32 crc = new CRC32();
            crc.update(data);
            int calculatedCrc = (int) crc.getValue();
            
            if (storedCrc != calculatedCrc) {
                throw new IOException("CRC mismatch for file " + fileId);
            }
            
            return data;
        }
    }
    
    public boolean remove(int indexId, int fileId) throws IOException {
        CacheIndex index = indices.get(indexId);
        if (index == null) return false;
        
        IndexEntry removed = index.entries.remove(fileId);
        if (removed != null) {
            saveIndices();
            return true;
        }
        return false;
    }
    
    public int getFileCount(int indexId) {
        CacheIndex index = indices.get(indexId);
        return index != null ? index.entries.size() : 0;
    }
    
    public boolean contains(int indexId, int fileId) {
        CacheIndex index = indices.get(indexId);
        return index != null && index.entries.containsKey(fileId);
    }
    
    public Map<Integer, CacheIndex> getIndices() {
        return Collections.unmodifiableMap(indices);
    }
}
