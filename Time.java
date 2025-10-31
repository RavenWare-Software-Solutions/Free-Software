import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Time extends JFrame {
    private JLabel currentTimeLabel;
    private JPanel intervalsPanel;
    private JLabel resultLabel;
    private static final LocalTime CUTOFF_3PM = LocalTime.of(15, 0); // 3:00 PM
    private static final LocalTime CUTOFF_5PM = LocalTime.of(17, 0); // 5:00 PM
    private static final LocalTime CUTOFF_7PM = LocalTime.of(19, 0); // 7:00 PM
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("z");
    private List<IntervalRow> intervalRows;

    public Time() {
        // Set up the JFrame
        setTitle("Time Calculator");
        setAutoRequestFocus(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize interval rows list
        intervalRows = new ArrayList<>();

        // Create a main panel with centered layout
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Initialize current time label with time zone
        ZonedDateTime now = ZonedDateTime.now();
        currentTimeLabel = new JLabel("Current Time: " + now.format(timeFormatter) + " " + now.format(zoneFormatter));
        currentTimeLabel.setToolTipText("Time is synced with your computer's system clock. Ensure it is set correctly.");
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(currentTimeLabel, gbc);

        // Calculate initial delay to align with the next minute
        long secondsUntilNextMinute = 60 - now.getSecond();
        int initialDelay = (int) (secondsUntilNextMinute * 1000 - now.getNano() / 1_000_000);

        // Start a timer to update the current time every minute
        Timer timeUpdateTimer = new Timer(60000, e -> updateCurrentTime());
        timeUpdateTimer.setInitialDelay(initialDelay); // Align to next minute
        timeUpdateTimer.start();

        // Create panel for intervals
        intervalsPanel = new JPanel();
        intervalsPanel.setLayout(new BoxLayout(intervalsPanel, BoxLayout.Y_AXIS));
        
        // Add initial interval row
        addIntervalRow(1);

        // Scroll pane for intervals
        JScrollPane scrollPane = new JScrollPane(intervalsPanel);
        scrollPane.setPreferredSize(new Dimension(500, 200));
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        mainPanel.add(scrollPane, gbc);

        // Calculate button
        JButton calculateButton = new JButton("Calculate");
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        mainPanel.add(calculateButton, gbc);

        // Result label
        resultLabel = new JLabel("Resulting Time: ");
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        mainPanel.add(resultLabel, gbc);

        // Action listener for calculate button
        calculateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                calculateResult();
            }
        });

        // Add main panel to a centered container
        JPanel container = new JPanel(new GridBagLayout());
        container.add(mainPanel);
        add(container);

        pack();
        setLocationRelativeTo(null); // Center the window
        setSize(600, 400); // Set a reasonable initial size
    }

    private class IntervalRow {
        private JPanel panel;
        private JLabel numberLabel;
        private JTextField textField;
        private JLabel etaLabel;
        private JButton removeButton;
        private int index;

        public IntervalRow(int index) {
            this.index = index;
            createComponents();
        }

        private void createComponents() {
            panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

            numberLabel = new JLabel("Interval " + index + ":");
            textField = new JTextField(10);
            etaLabel = new JLabel("");
            removeButton = new JButton("X");
            removeButton.setMargin(new Insets(1, 4, 1, 4));
            removeButton.setPreferredSize(new Dimension(30, 25));

            // Add key listener for Tab key to add new row
            textField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_TAB) {
                        // Check if this is the last row and field has focus
                        if (isLastRow() && textField.hasFocus()) {
                            addNewRow();
                            e.consume(); // Prevent default tab behavior
                        }
                    }
                }
            });

            // Add action listener for text changes to update ETA
            textField.addActionListener(e -> updateETA());
            textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                public void changedUpdate(javax.swing.event.DocumentEvent e) { updateETA(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { updateETA(); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { updateETA(); }
            });

            // Add action listener for remove button
            removeButton.addActionListener(e -> removeRow());

            panel.add(numberLabel);
            panel.add(textField);
            panel.add(etaLabel);
            panel.add(removeButton);
        }

        private void updateETA() {
            String input = textField.getText().trim();
            if (!input.isEmpty()) {
                try {
                    int minutesToAdd = parseInput(input);
                    if (minutesToAdd >= 0) {
                        LocalTime currentTime = LocalTime.now();
                        LocalTime eta = currentTime.plusMinutes(minutesToAdd);
                        etaLabel.setText("ETA: " + eta.format(timeFormatter));
                    } else {
                        etaLabel.setText("Invalid input");
                    }
                } catch (NumberFormatException ex) {
                    etaLabel.setText("Invalid input");
                }
            } else {
                etaLabel.setText("");
            }
        }

        private int parseInput(String input) {
            try {
                int value = Integer.parseInt(input);
                if (input.length() == 3) {
                    // Interpret as HMM (hours and minutes, e.g., "130" = 1h30m)
                    int hours = value / 100;
                    int minutes = value % 100;
                    if (hours >= 10 || minutes >= 60) {
                        return -1;
                    }
                    return hours * 60 + minutes;
                } else if (input.length() <= 2) {
                    // Interpret as minutes
                    return value;
                } else {
                    return -1;
                }
            } catch (NumberFormatException ex) {
                return -1;
            }
        }

        private boolean isLastRow() {
            return index == intervalRows.size();
        }

        public JPanel getPanel() { return panel; }
        public JTextField getTextField() { return textField; }
        public int getIndex() { return index; }
        public void setIndex(int index) { 
            this.index = index; 
            numberLabel.setText("Interval " + index + ":");
        }
        public String getText() { return textField.getText(); }
    }

    private void addIntervalRow(int index) {
        IntervalRow newRow = new IntervalRow(index);
        intervalRows.add(index - 1, newRow);
        intervalsPanel.add(newRow.getPanel());
        revalidateIntervals();
        intervalsPanel.revalidate();
        intervalsPanel.repaint();
        
        // Focus the new field
        SwingUtilities.invokeLater(() -> newRow.getTextField().requestFocusInWindow());
    }

    private void addNewRow() {
        addIntervalRow(intervalRows.size() + 1);
    }

    private void removeRow() {
        if (intervalRows.size() > 1) {
            // Find which row to remove
            for (int i = 0; i < intervalRows.size(); i++) {
                IntervalRow row = intervalRows.get(i);
                if (row.removeButton.getModel().isArmed()) {
                    intervalsPanel.remove(row.getPanel());
                    intervalRows.remove(i);
                    revalidateIntervals();
                    intervalsPanel.revalidate();
                    intervalsPanel.repaint();
                    break;
                }
            }
        }
    }

    private void revalidateIntervals() {
        for (int i = 0; i < intervalRows.size(); i++) {
            intervalRows.get(i).setIndex(i + 1);
        }
    }

    // Method to update the current time label
    private void updateCurrentTime() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        currentTimeLabel.setText("Current Time: " + currentTime.format(timeFormatter) + " " + currentTime.format(zoneFormatter));
        
        // Update all ETA labels
        for (IntervalRow row : intervalRows) {
            row.updateETA();
        }
    }

    private void calculateResult() {
        LocalTime currentTime = LocalTime.now();
        List<Integer> intervals = new ArrayList<>();
        int totalMinutes = 0;
        int cutoffIndexTime = -1;
        long minutesPastCutoff = 0;
        String cutoffTimeStr = "";
        int[] cutoffIndices = new int[3]; // For 3PM, 5PM, 7PM
        long[] minutesPastCutoffs = new long[3];

        // Process each interval row
        for (int i = 0; i < intervalRows.size(); i++) {
            IntervalRow row = intervalRows.get(i);
            String input = row.getText().trim();
            if (!input.isEmpty()) {
                try {
                    int minutesToAdd = row.parseInput(input);
                    if (minutesToAdd < 0) {
                        resultLabel.setText("Error: Invalid input in Interval " + (i + 1) + ": " + input);
                        return;
                    }

                    totalMinutes += minutesToAdd;
                    intervals.add(minutesToAdd);

                    // Check cutoffs
                    LocalTime tempTime = currentTime.plusMinutes(totalMinutes);
                    if (cutoffIndices[0] == 0 && tempTime.isAfter(CUTOFF_3PM)) {
                        cutoffIndices[0] = i + 1;
                        minutesPastCutoffs[0] = java.time.Duration.between(currentTime, tempTime).toMinutes() - java.time.Duration.between(currentTime, CUTOFF_3PM).toMinutes();
                    }
                    if (cutoffIndices[1] == 0 && tempTime.isAfter(CUTOFF_5PM)) {
                        cutoffIndices[1] = i + 1;
                        minutesPastCutoffs[1] = java.time.Duration.between(currentTime, tempTime).toMinutes() - java.time.Duration.between(currentTime, CUTOFF_5PM).toMinutes();
                    }
                    if (cutoffIndices[2] == 0 && tempTime.isAfter(CUTOFF_7PM)) {
                        cutoffIndices[2] = i + 1;
                        minutesPastCutoffs[2] = java.time.Duration.between(currentTime, tempTime).toMinutes() - java.time.Duration.between(currentTime, CUTOFF_7PM).toMinutes();
                    }
                } catch (NumberFormatException ex) {
                    resultLabel.setText("Error: Invalid input in Interval " + (i + 1) + ": " + input);
                    return;
                }
            }
        }

        // Calculate resulting time
        LocalTime newTime = currentTime.plusMinutes(totalMinutes);

        // Check for cutoff violations and show warning in result label
        StringBuilder resultText = new StringBuilder();
        resultText.append("Resulting Time: ").append(newTime.format(timeFormatter)).append(" (Added ").append(totalMinutes).append(" minutes)");

        if (cutoffIndices[2] != 0) {
            cutoffIndexTime = cutoffIndices[2];
            cutoffTimeStr = "7:00 PM";
            minutesPastCutoff = minutesPastCutoffs[2];
            resultText.append(" - WARNING: Exceeds ").append(cutoffTimeStr).append(" at Interval ").append(cutoffIndexTime).append(" by ").append(minutesPastCutoff).append(" minutes");
        } else if (cutoffIndices[1] != 0) {
            cutoffIndexTime = cutoffIndices[1];
            cutoffTimeStr = "5:00 PM";
            minutesPastCutoff = minutesPastCutoffs[1];
            resultText.append(" - WARNING: Exceeds ").append(cutoffTimeStr).append(" at Interval ").append(cutoffIndexTime).append(" by ").append(minutesPastCutoff).append(" minutes");
        } else if (cutoffIndices[0] != 0) {
            cutoffIndexTime = cutoffIndices[0];
            cutoffTimeStr = "3:00 PM";
            minutesPastCutoff = minutesPastCutoffs[0];
            resultText.append(" - WARNING: Exceeds ").append(cutoffTimeStr).append(" at Interval ").append(cutoffIndexTime).append(" by ").append(minutesPastCutoff).append(" minutes");
        }

        resultLabel.setText(resultText.toString());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Time calculator = new Time();
            calculator.setVisible(true);
        });
    }
}
