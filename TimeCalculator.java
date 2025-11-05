import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URI;

public class TimeCalculator extends JFrame {
    private JLabel currentTimeLabel;
    private JPanel intervalsPanel;
    private java.util.List<JTextField> intervalFields = new ArrayList<>();
    private java.util.List<JLabel> intervalLabels = new ArrayList<>();
    private JButton addButton;
    private JButton removeButton;
    private JButton stopButton;
    private JLabel resultLabel;
    private JLabel warningLabel;
    private JLabel etaLabel;
    private JLabel lastChangeLabel;

    private static final LocalTime CUTOFF_3PM = LocalTime.of(15, 0);
    private static final LocalTime CUTOFF_5PM = LocalTime.of(17, 0);
    private static final LocalTime CUTOFF_7PM = LocalTime.of(19, 0);

    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
    private final DateTimeFormatter zoneFormatter = DateTimeFormatter.ofPattern("z");

    private int intervalCount = 1;
    private javax.swing.Timer countdownTimer;
    private long initialIntervalMinutes = 0;
    private long displayedIntervalMinutes = 0;
    private LocalTime orderChangeTime = null;
    private boolean countdownActive = false;
    private String lastChangeTimeText = "";

    // URLs for the four buttons
    private static final String URL_CLOCK_IN = "https://wfmprod.ipaper.com/etm/";
    private static final String URL_UPDATE_PASSWORD = "https://myaccess.ipaper.com/identityiq/login.jsf?prompt=true";
    private static final String URL_WEATHER = "https://www.accuweather.com/en/us/louisville/40202/hourly-weather-forecast/348428";
    private static final String URL_IP_HOME = "https://login.microsoftonline.com/ef4cd7d9-cd50-4892-b62e-077d05f7ba11/oauth2/authorize?client%5Fid=00000003%2D0000%2D0ff1%2Dce00%2D000000000000&response%5Fmode=form%5Fpost&response%5Ftype=code%20id%5Ftoken&resource=00000003%2D0000%2D0ff1%2Dce00%2D000000000000&scope=openid&nonce=A569C0C3EEDCC4284668C921BE8623A4D3F8E3C9A232C3F5%2DA64DC7FD2F10CC220FC4C089FA70D4CC50926D05B7E516A8ADA3C46CE4E20255&redirect%5Furi=https%3A%2F%2Fipapercloud%2Esharepoint%2Ecom%2F%5Fforms%2Fdefault%2Easpx&state=OD0w&claims=%7B%22id%5Ftoken%22%3A%7B%22xms%5Fcc%22%3A%7B%22values%22%3A%5B%22CP1%22%5D%7D%7D%7D&wsucxt=1&cobrandid=11bd8083%2D87e0%2D41b5%2Dbb78%2D0bc43c8a8e8a&client%2Drequest%2Did=f06bd6a1%2De0a9%2D0000%2D113d%2D65030363877a";

    // Secret URL
    private static final String URL_SECRET_GROK = "https://grok.com/c";

    public TimeCalculator() {
        setTitle("Time Calculator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setAutoRequestFocus(true);

        // Main panel with existing content
        JPanel mainPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        ZonedDateTime now = ZonedDateTime.now();
        currentTimeLabel = new JLabel("Current Time: " + now.format(timeFormatter) + " " + now.format(zoneFormatter));
        currentTimeLabel.setToolTipText("Time is synced with your computer's system clock.");
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 3; gbc.anchor = GridBagConstraints.CENTER;
        mainPanel.add(currentTimeLabel, gbc);

        // Update current time every minute
        long secsToNext = 60 - now.getSecond();
        int delay = (int) (secsToNext * 1000 - now.getNano() / 1_000_000);
        javax.swing.Timer timer = new javax.swing.Timer(60000, e -> updateCurrentTime());
        timer.setInitialDelay(delay);
        timer.start();

        // Countdown timer
        countdownTimer = new javax.swing.Timer(1000, e -> updateCountdown());

        intervalsPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 3;
        mainPanel.add(intervalsPanel, gbc);

        // Initialize buttons
        addPlusButton();
        addRemoveButton();
        addStopButton();

        // Add first interval
        addFirstInterval();

        // Last change label (blue)
        lastChangeLabel = new JLabel(" ");
        lastChangeLabel.setForeground(Color.BLUE.darker());
        lastChangeLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        gbc.gridy = 2; gbc.gridwidth = 3;
        mainPanel.add(lastChangeLabel, gbc);

        // ETA label
        etaLabel = new JLabel("Next order change in: --:--");
        etaLabel.setForeground(Color.BLUE.darker());
        etaLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        gbc.gridy = 3;
        mainPanel.add(etaLabel, gbc);

        JButton calcBtn = new JButton("Calculate");
        calcBtn.setFocusable(false);
        gbc.gridy = 4;
        mainPanel.add(calcBtn, gbc);
        calcBtn.addActionListener(e -> {
            calculateResult();
            startCountdown();
        });

        resultLabel = new JLabel("Resulting Time: ");
        gbc.gridy = 5;
        mainPanel.add(resultLabel, gbc);

        warningLabel = new JLabel(" ");
        warningLabel.setForeground(Color.RED);
        gbc.gridy = 6;
        mainPanel.add(warningLabel, gbc);

        // === NEW: Bottom Layout with Visible + Secret Button ===
        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Top: 2x2 visible buttons
        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        JButton clockInBtn = createLinkButton("Clock In", URL_CLOCK_IN);
        JButton updatePassBtn = createLinkButton("Update Password", URL_UPDATE_PASSWORD);
        JButton weatherBtn = createLinkButton("Weather", URL_WEATHER);
        JButton ipHomeBtn = createLinkButton("IP Home", URL_IP_HOME);

        buttonPanel.add(clockInBtn);
        buttonPanel.add(updatePassBtn);
        buttonPanel.add(weatherBtn);
        buttonPanel.add(ipHomeBtn);

        southPanel.add(buttonPanel, BorderLayout.NORTH);

        // Bottom: Invisible secret button (centered)
        JPanel secretPanel = new JPanel(new GridBagLayout());
        JButton secretButton = new JButton();
        secretButton.setToolTipText("Secret: Open Grok Chat");
        secretButton.setContentAreaFilled(false);
        secretButton.setBorderPainted(false);
        secretButton.setFocusPainted(false);
        secretButton.setOpaque(false);
        secretButton.setPreferredSize(new Dimension(0, 0));
        secretButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        secretButton.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(URL_SECRET_GROK));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not open Grok: " + ex.getMessage(),
                    "Secret Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        // Make it clickable in a 100x40 area (invisible but detectable)
        secretButton.setPreferredSize(new Dimension(100, 40));
        secretPanel.add(secretButton);

        southPanel.add(secretPanel, BorderLayout.SOUTH);

        // === Layout: Main content in CENTER, buttons in SOUTH ===
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
        add(southPanel, BorderLayout.SOUTH);

        setPreferredSize(new Dimension(380, 720));
        pack();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(380, 720));

        SwingUtilities.invokeLater(() -> intervalFields.get(0).requestFocusInWindow());
        calculateResult();
    }

    // Helper to create a button that opens a URL
    private JButton createLinkButton(String text, String url) {
        JButton btn = new JButton(text);
        btn.setToolTipText("Open: " + url);
        btn.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                    "Could not open browser: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            }
        });
        return btn;
    }

    // === [Rest of your methods unchanged below] ===
    // ... (startCountdown, stopCountdown, addIntervalRow, etc.)
    // [All other methods remain exactly the same as in your original code]

    private void startCountdown() {
        updateCountdownTarget();
        if (countdownActive && countdownTimer.isRunning()) {
            countdownTimer.restart();
        } else {
            countdownTimer.start();
            countdownActive = true;
        }
        updateStopButtonState();
        revalidateControlButtonsPosition();
    }

    private void stopCountdown() {
        if (countdownTimer.isRunning()) {
            countdownTimer.stop();
        }
        countdownActive = false;

        if (!intervalFields.isEmpty()) {
            intervalFields.get(0).setText(String.valueOf(initialIntervalMinutes));
        }

        etaLabel.setText("Next order change in: --:--");
        updateStopButtonState();
        revalidateControlButtonsPosition();
    }

    private void updateStopButtonState() {
        stopButton.setEnabled(countdownActive);
        if (countdownActive) {
            stopButton.setForeground(Color.RED.darker());
            stopButton.setToolTipText("Stop countdown and edit Interval 1");
        } else {
            stopButton.setForeground(Color.GRAY);
            stopButton.setToolTipText("Countdown is not running");
        }
    }

    private void addFirstInterval() {
        addIntervalRow(1);
    }

    private void addIntervalRow(int idx) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        int row = idx - 1;

        JLabel lbl = new JLabel("Interval " + idx + " (MM / HMM):");
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
        intervalsPanel.add(lbl, gbc);
        intervalLabels.add(lbl);

        JTextField field = new JTextField();
        field.setColumns(8);
        field.setMaximumSize(new Dimension(120, field.getPreferredSize().height));
        field.setMinimumSize(new Dimension(120, field.getPreferredSize().height));
        field.setPreferredSize(new Dimension(120, field.getPreferredSize().height));
        field.setHorizontalAlignment(JTextField.RIGHT);
        gbc.gridx = 1; gbc.weightx = 1;
        intervalsPanel.add(field, gbc);
        intervalFields.add(field);
        addLiveUpdate(field);
        setupFieldTabToPlus(field);

        revalidateControlButtonsPosition();
    }

    private void addPlusButton() {
        addButton = new JButton("+");
        addButton.setMargin(new Insets(2, 8, 2, 8));
        addButton.setToolTipText("Add new interval (press Tab to add)");

        Action addAction = new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                intervalCount++;
                addIntervalRow(intervalCount);
                calculateResult();
                JTextField newField = intervalFields.get(intervalFields.size() - 1);
                SwingUtilities.invokeLater(newField::requestFocusInWindow);
            }
        };

        addButton.addActionListener(addAction);
        addButton.setFocusable(true);

        addButton.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE || e.getKeyCode() == KeyEvent.VK_ENTER) {
                    addAction.actionPerformed(null);
                }
            }
        });

        addButton.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isShiftDown()) {
                    e.consume();
                    addAction.actionPerformed(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB && e.isShiftDown()) {
                    e.consume();
                    JTextField last = intervalFields.get(intervalFields.size() - 1);
                    last.requestFocusInWindow();
                }
            }
        });
    }

    private void addRemoveButton() {
        removeButton = new JButton("-");
        removeButton.setMargin(new Insets(2, 6, 2, 6));
        removeButton.setToolTipText("Remove last interval");
        removeButton.setFont(new Font("SansSerif", Font.BOLD, 12));
        removeButton.setForeground(Color.GRAY.darker());
        removeButton.setFocusable(true);

        removeButton.addActionListener(e -> {
            if (intervalCount > 1) {
                JTextField lastField = intervalFields.remove(intervalFields.size() - 1);
                JLabel lastLabel = intervalLabels.remove(intervalLabels.size() - 1);
                intervalsPanel.remove(lastField);
                intervalsPanel.remove(lastLabel);

                intervalCount--;
                revalidateControlButtonsPosition();
                calculateResult();
            }
        });
    }

    private void addStopButton() {
        stopButton = new JButton("Stop");
        stopButton.setMargin(new Insets(2, 4, 2, 4));
        stopButton.setFont(new Font("SansSerif", Font.BOLD, 10));
        stopButton.setForeground(Color.GRAY);
        stopButton.setFocusable(true);
        stopButton.addActionListener(e -> stopCountdown());
        stopButton.setEnabled(false);
        stopButton.setToolTipText("Countdown is not running");
    }

    private void revalidateControlButtonsPosition() {
        if (addButton.getParent() != null) intervalsPanel.remove(addButton);
        if (removeButton.getParent() != null) intervalsPanel.remove(removeButton);
        if (stopButton.getParent() != null) intervalsPanel.remove(stopButton);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(2, 5, 2, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 1;
        gbc.weightx = 1;

        gbc.gridy = intervalCount;
        intervalsPanel.add(addButton, gbc);

        if (intervalCount > 1) {
            gbc.gridy = intervalCount + 1;
            intervalsPanel.add(removeButton, gbc);
        }

        int stopY = intervalCount + (intervalCount > 1 ? 2 : 1);
        gbc.gridy = stopY;
        intervalsPanel.add(stopButton, gbc);

        intervalsPanel.revalidate();
        intervalsPanel.repaint();
    }

    private void setupFieldTabToPlus(JTextField field) {
        field.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_TAB && !e.isShiftDown()) {
                    e.consume();
                    addButton.requestFocusInWindow();
                }
            }
        });
    }

    private void addLiveUpdate(JTextField f) {
        f.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { onFieldChanged(); }
            public void removeUpdate(DocumentEvent e)  { onFieldChanged(); }
            public void changedUpdate(DocumentEvent e) { onFieldChanged(); }
        });
    }

    private void onFieldChanged() {
        calculateResult();
    }

    private void updateCurrentTime() {
        ZonedDateTime now = ZonedDateTime.now();
        currentTimeLabel.setText("Current Time: " + now.format(timeFormatter) + " " + now.format(zoneFormatter));
        calculateResult();
    }

    private void updateCountdownTarget() {
        if (intervalFields.isEmpty()) {
            initialIntervalMinutes = 0;
            displayedIntervalMinutes = 0;
            orderChangeTime = null;
            etaLabel.setText("Next order change in: --:--");
            return;
        }

        String txt = intervalFields.get(0).getText().trim();
        if (txt.isEmpty()) {
            initialIntervalMinutes = 0;
            displayedIntervalMinutes = 0;
            orderChangeTime = null;
            etaLabel.setText("Next order change in: --:--");
            return;
        }

        int mins = 0;
        try {
            if (txt.matches("\\d{3}")) {
                int val = Integer.parseInt(txt);
                int h = val / 100, m = val % 100;
                if (h >= 10 || m >= 60) return;
                mins = h * 60 + m;
            } else {
                mins = Integer.parseInt(txt);
            }
        } catch (NumberFormatException ex) {
            initialIntervalMinutes = 0;
            displayedIntervalMinutes = 0;
            orderChangeTime = null;
            etaLabel.setText("Next order change in: --:--");
            return;
        }

        initialIntervalMinutes = mins;
        displayedIntervalMinutes = mins;
        orderChangeTime = LocalTime.now().plusMinutes(mins);
        intervalFields.get(0).setText(String.valueOf(mins));
    }

    private void updateCountdown() {
        if (initialIntervalMinutes <= 0 || orderChangeTime == null) {
            etaLabel.setText("Next order change in: --:--");
            return;
        }

        LocalTime now = LocalTime.now();
        long remainingSec = Duration.between(now, orderChangeTime).getSeconds();

        if (remainingSec > 0) {
            displayedIntervalMinutes = remainingSec / 60;
            intervalFields.get(0).setText(String.valueOf(displayedIntervalMinutes));

            long mins = remainingSec / 60;
            long secs = remainingSec % 60;
            etaLabel.setText(String.format("Next order change in: %d:%02d", mins, secs));
        } else {
            String changeTime = orderChangeTime.format(timeFormatter);
            lastChangeTimeText = "Order changed at: " + changeTime;
            lastChangeLabel.setText("<html><b><font color='blue'>" + lastChangeTimeText + "</font></b></html>");
            etaLabel.setText("<html><b>Order changed at " + changeTime + "</b></html>");

            shiftIntervalsUp();
            updateCountdownTarget();
            if (countdownActive) {
                startCountdown();
            }
        }
    }

    private void shiftIntervalsUp() {
        if (intervalFields.size() <= 1) {
            intervalFields.get(0).setText("");
            initialIntervalMinutes = 0;
            displayedIntervalMinutes = 0;
            orderChangeTime = null;
            calculateResult();
            return;
        }

        java.util.List<String> values = new ArrayList<>();
        for (JTextField f : intervalFields) {
            values.add(f.getText().trim());
        }

        intervalsPanel.removeAll();
        intervalFields.clear();
        intervalLabels.clear();

        intervalCount = 0;
        for (int i = 1; i < values.size(); i++) {
            intervalCount++;
            addIntervalRow(intervalCount);
            intervalFields.get(intervalFields.size() - 1).setText(values.get(i));
        }

        if (intervalCount == 0) {
            intervalCount = 1;
            addIntervalRow(1);
            intervalFields.get(0).setText("");
        }

        revalidateControlButtonsPosition();
        calculateResult();
    }

    private String formatDuration(long minutes) {
        if (minutes < 60) {
            return minutes + " min";
        } else {
            long hrs = minutes / 60;
            long mins = minutes % 60;
            return hrs + " hr" + (mins > 0 ? " " + mins + " min" : "");
        }
    }

    private void calculateResult() {
        LocalTime now = LocalTime.now();
        int totalMins = 0;
        int cutoffIdx = -1, cutoffMins = 0;
        String cutoffName = "";
        int[] idxArr = new int[3];
        long[] minsArr = new long[3];
        boolean err = false;

        for (int i = 0; i < intervalFields.size(); i++) {
            String txt = intervalFields.get(i).getText().trim();
            if (txt.isEmpty()) continue;

            int minsAdd = 0;
            try {
                if (txt.matches("\\d{3}")) {
                    int val = Integer.parseInt(txt);
                    int h = val / 100, m = val % 100;
                    if (h >= 10 || m >= 60) throw new NumberFormatException();
                    minsAdd = h * 60 + m;
                } else {
                    minsAdd = Integer.parseInt(txt);
                }
            } catch (NumberFormatException ex) {
                err = true; break;
            }

            totalMins += minsAdd;

            LocalTime temp = now.plusMinutes(totalMins);

            if (idxArr[0] == 0 && temp.isAfter(CUTOFF_3PM)) {
                idxArr[0] = i + 1;
                minsArr[0] = Duration.between(CUTOFF_3PM, temp).toMinutes();
            }
            if (idxArr[1] == 0 && temp.isAfter(CUTOFF_5PM)) {
                idxArr[1] = i + 1;
                minsArr[1] = Duration.between(CUTOFF_5PM, temp).toMinutes();
            }
            if (idxArr[2] == 0 && temp.isAfter(CUTOFF_7PM)) {
                idxArr[2] = i + 1;
                minsArr[2] = Duration.between(CUTOFF_7PM, temp).toMinutes();
            }
        }

        if (err) {
            resultLabel.setText("<html><font color='red'>Invalid input – check fields</font></html>");
            warningLabel.setText("");
            return;
        }

        LocalTime finalTime = now.plusMinutes(totalMins);
        resultLabel.setText("Resulting Time: " + finalTime.format(timeFormatter) +
                            " (Added " + formatDuration(totalMins) + ")");

        if (idxArr[2] != 0) { cutoffIdx = idxArr[2]; cutoffName = "7:00 PM"; cutoffMins = (int)minsArr[2]; }
        else if (idxArr[1] != 0) { cutoffIdx = idxArr[1]; cutoffName = "5:00 PM"; cutoffMins = (int)minsArr[1]; }
        else if (idxArr[0] != 0) { cutoffIdx = idxArr[0]; cutoffName = "3:00 PM"; cutoffMins = (int)minsArr[0]; }

        if (cutoffIdx != -1) {
            warningLabel.setText("<html><b>Warning:</b> exceeds " + cutoffName +
                    " at Interval " + cutoffIdx + " by " + formatDuration(cutoffMins) + "</html>");
        } else {
            warningLabel.setText("");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Ignore
            }
            new TimeCalculator().setVisible(true);
        });
    }
}
