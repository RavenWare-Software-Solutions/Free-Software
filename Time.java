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
    private List<IntervalSection> intervalSections;
    private JPanel intervalsPanel;
    private JLabel resultLabel;
    private static final LocalTime CUTOFF_3PM = LocalTime.of(15, 0); // 3:00 PM
    private static final LocalTime CUTOFF_5PM = LocalTime.of(17, 0); // 5:00 PM
    private static final LocalTime CUTOFF_7PM = LocalTime.of(19, 0); // 7:00 PM
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("z");

    public Time() {
        // Set up the JFrame
        setTitle("Time Calculator");
        setAutoRequestFocus(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Initialize interval sections list
        intervalSections = new ArrayList<>();

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
        intervalsPanel.setLayout(new GridBagLayout());

        // Add initial interval section
        addIntervalSection(1);

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
        setSize(500, 400); // Set a reasonable initial size
    }

    private class IntervalSection {
        private JLabel label;
        private JTextField textField;
        private JLabel etaLabel;
        private JButton removeButton;
        private int index;

        public IntervalSection(int index) {
            this.index = index;
        }

        public JLabel getLabel() { return label; }
        public JTextField getTextField() { return textField; }
        public JLabel getEtaLabel() { return etaLabel; }
        public JButton getRemoveButton() { return removeButton; }
        public int getIndex() { return index; }
        public void setIndex(int index) { 
            this.index = index; 
            label.setText("Interval " + index + " (minutes or HMM):");
        }
        public String getText() { return textField.getText(); }
    }

    private void addIntervalSection(int index) {
        IntervalSection section = new IntervalSection(index);
        
        // Create components
        section.label = new JLabel("Interval " + index + " (minutes or HMM):");
        section.textField = new JTextField(10);
        section.etaLabel = new JLabel("");
        section.removeButton = new JButton("X");
        section.removeButton.setMargin(new Insets(1, 4, 1, 4));
        section.removeButton.setPreferredSize(new Dimension(30, 25));

        // Add key listener for Tab key to add new row
        section.textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    // Check if this is the last section and field has focus
                    if (isLastSection(section) && section.textField.hasFocus()) {
                        addNewSection();
                        e.consume(); // Prevent default tab behavior
                    }
                }
            }
        });

        // Add action listener for text changes to update ETA
        section.textField.addActionListener(e -> updateETA(section));
        section.textField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateETA(section); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateETA(section); }
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateETA(section); }
        });

        // Add action listener for remove button
        section.removeButton.addActionListener(e -> removeSection(section));

        // Add to intervals panel using GridBagLayout
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Label
        gbc.gridx = 0;
        gbc.gridy = index - 1;
        gbc.weightx = 0.3;
        intervalsPanel.add(section.label, gbc);

        // Text field
        gbc.gridx = 1;
        gbc.weightx = 0.3;
        intervalsPanel.add(section.textField, gbc);

        // ETA label
        gbc.gridx = 2;
        gbc.weightx = 0.3;
        intervalsPanel.add(section.etaLabel, gbc);

        // Remove button (only show if not the only section)
        gbc.gridx = 3;
        gbc.weightx = 0.1;
        if (intervalSections.size() > 0) { // Only show remove button if there are existing sections
            intervalsPanel.add(section.removeButton, gbc);
        }

        intervalSections.add(index - 1, section);
        revalidateIntervals();
        intervalsPanel.revalidate();
        intervalsPanel.repaint();
        
        // Focus the new field
        SwingUtilities.invokeLater(() -> section.textField.requestFocusInWindow());
    }

    private void updateETA(IntervalSection section) {
        String input = section.getText().trim();
        if (!input.isEmpty()) {
            try {
                int minutesToAdd = parseInput(input);
                if (minutesToAdd >= 0) {
                    LocalTime currentTime = LocalTime.now();
                    LocalTime eta = currentTime.plusMinutes(minutesToAdd);
                    section.etaLabel.setText("ETA: " + eta.format(timeFormatter));
                } else {
                    section.etaLabel.setText("Invalid input");
                }
            } catch (NumberFormatException ex) {
                section.etaLabel.setText("Invalid input");
            }
        } else {
            section.etaLabel.setText("");
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

    private boolean isLastSection(IntervalSection section) {
        return section.getIndex() == intervalSections.size();
    }

    private void addNewSection() {
        addIntervalSection(intervalSections.size() + 1);
    }

    private void removeSection(IntervalSection sectionToRemove) {
        if (intervalSections.size() > 1) {
            // Remove all components from intervals panel
            intervalsPanel.removeAll();
            
            // Remove the section from our list
            intervalSections.remove(sectionToRemove);
            
            // Re-add all remaining sections with updated indices
            for (int i = 0; i < intervalSections.size(); i++) {
                IntervalSection section = intervalSections.get(i);
                section.setIndex(i + 1);
                
                GridBagConstraints gbc = new GridBagConstraints();
                gbc.insets = new Insets(2, 5, 2, 5);
                gbc.fill = GridBagConstraints.HORIZONTAL;

                // Label
                gbc.gridx = 0;
                gbc.gridy = i;
                gbc.weightx = 0.3;
                intervalsPanel.add(section.label, gbc);

                // Text field
                gbc.gridx = 1;
                gbc.weightx = 0.3;
                intervalsPanel.add(section.textField, gbc);

                // ETA label
                gbc.gridx = 2;
                gbc.weightx = 0.3;
                intervalsPanel.add(section.etaLabel, gbc);

                // Remove button (only show if not the only section)
                gbc.gridx = 3;
                gbc.weightx = 0.1;
                if (intervalSections.size() > 1) {
                    intervalsPanel.add(section.removeButton, gbc);
                }
            }
            
            intervalsPanel.revalidate();
            intervalsPanel.repaint();
        }
    }

    private void revalidateIntervals() {
        for (int i = 0; i < intervalSections.size(); i++) {
            intervalSections.get(i).setIndex(i + 1);
        }
    }

    // Method to update the current time label
    private void updateCurrentTime() {
        ZonedDateTime currentTime = ZonedDateTime.now();
        currentTimeLabel.setText("Current Time: " + currentTime.format(timeFormatter) + " " + currentTime.format(zoneFormatter));
        
        // Update all ETA labels
        for (IntervalSection section : intervalSections) {
            updateETA(section);
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

        // Process each interval section
        for (int i = 0; i < intervalSections.size(); i++) {
            IntervalSection section = intervalSections.get(i);
            String input = section.getText().trim();
            if (!input.isEmpty()) {
                try {
                    int minutesToAdd = parseInput(input);
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
