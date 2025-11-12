import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.io.File;
import java.util.*;

public class CalendarCreator extends JFrame implements Printable {

    private final Map<Integer, ImageIcon> monthImages = new HashMap<>();
    private final JPanel calendarPanel;
    private final int year;

    public CalendarCreator() {
        year = Calendar.getInstance().get(Calendar.YEAR);
        setTitle("Calendar Creator – " + year);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1200, 900);
        setLocationRelativeTo(null);

        /* ---------- MENU ---------- */
        JMenuBar menuBar = new JMenuBar();
        JMenu setupMenu = new JMenu("Setup");
        JMenuItem assignItem = new JMenuItem("Assign Images to Months");
        assignItem.addActionListener(e -> showAssignDialog());
        setupMenu.add(assignItem);

        JMenu fileMenu = new JMenu("File");
        JMenuItem printItem = new JMenuItem("Print Calendar");
        printItem.addActionListener(e -> printCalendar());
        fileMenu.add(printItem);

        menuBar.add(fileMenu);
        menuBar.add(setupMenu);
        setJMenuBar(menuBar);

        /* ---------- MAIN 3×4 GRID ---------- */
        calendarPanel = new JPanel(new GridLayout(3, 4, 12, 12));
        calendarPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        updateCalendar();

        add(new JScrollPane(calendarPanel));
    }

    /* --------------------------------------------------------------------- */
    private void updateCalendar() {
        calendarPanel.removeAll();
        for (int m = 0; m < 12; m++) {
            JPanel monthPanel = createMonthPanel(m, true);   // true = thumbnail
            calendarPanel.add(monthPanel);
        }
        calendarPanel.revalidate();
        calendarPanel.repaint();
    }

    /** month panel – used for both the 3×4 view and the full-screen preview */
    private JPanel createMonthPanel(int month, boolean thumbnail) {

        /* --------------------------------------------------------------
         *  NEW:  BackgroundPanel draws the image **behind** everything.
         * -------------------------------------------------------------- */
        BackgroundPanel panel = new BackgroundPanel(month, thumbnail);
        panel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 2));

        // ---- title ----------------------------------------------------
        String[] names = {"January", "February", "March", "April", "May", "June",
                          "July", "August", "September", "October", "November", "December"};
        JLabel title = new JLabel(names[month] + " " + year, SwingConstants.CENTER);
        title.setFont(new Font("SansSerif", Font.BOLD, thumbnail ? 16 : 28));
        title.setOpaque(false);                     // transparent
        title.setForeground(Color.BLACK);
        panel.add(title, BorderLayout.NORTH);

        // ---- day grid (overlay) ---------------------------------------
        JPanel dayGrid = createDayGrid(month, thumbnail);
        panel.add(dayGrid, BorderLayout.SOUTH);

        // ---- click to preview (only in thumbnail view) ----------------
        if (thumbnail) {
            panel.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) {
                    showMonthPreview(month);
                }
            });
            panel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        return panel;
    }

    /** Transparent grid that sits on top of the background image */
    private JPanel createDayGrid(int month, boolean thumbnail) {
        JPanel grid = new JPanel(new GridLayout(6, 7));
        grid.setOpaque(false);
        String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
        for (String d : days) {
            JLabel l = new JLabel(d, SwingConstants.CENTER);
            l.setFont(new Font("SansSerif", Font.PLAIN, thumbnail ? 9 : 13));
            l.setForeground(Color.BLACK);
            grid.add(l);
        }

        GregorianCalendar cal = new GregorianCalendar(year, month, 1);
        int first = cal.get(Calendar.DAY_OF_WEEK) - 1;   // 0 = Sunday
        int daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < first; i++) grid.add(new JLabel(""));
        for (int d = 1; d <= daysInMonth; d++) {
            JLabel l = new JLabel(String.valueOf(d), SwingConstants.CENTER);
            l.setFont(new Font("SansSerif", Font.PLAIN, thumbnail ? 11 : 15));
            l.setForeground(Color.BLACK);
            grid.add(l);
        }
        return grid;
    }

    /* --------------------------------------------------------------------- */
    /** Custom JPanel that paints the month image as a true background */
    private class BackgroundPanel extends JPanel {
        private final int month;
        private final boolean thumbnail;
        private Image scaledImage;

        BackgroundPanel(int month, boolean thumbnail) {
            this.month = month;
            this.thumbnail = thumbnail;
            setLayout(new BorderLayout());
            prepareImage();                 // scale once
        }

        private void prepareImage() {
            ImageIcon icon = monthImages.get(month);
            if (icon == null) return;

            int w = thumbnail ? 300 : 800;
            int h = thumbnail ? 220 : 600;
            scaledImage = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (scaledImage != null) {
                // centre the image
                int x = (getWidth()  - scaledImage.getWidth(null))  / 2;
                int y = (getHeight() - scaledImage.getHeight(null)) / 2;
                g.drawImage(scaledImage, x, y, this);
            } else if (thumbnail) {
                g.setColor(new Color(240, 240, 240));
                g.fillRect(0, 0, getWidth(), getHeight());
                g.setColor(Color.GRAY);
                g.setFont(new Font("SansSerif", Font.ITALIC, 14));
                String txt = "No Image";
                FontMetrics fm = g.getFontMetrics();
                int tx = (getWidth()  - fm.stringWidth(txt)) / 2;
                int ty = (getHeight() - fm.getHeight())    / 2 + fm.getAscent();
                g.drawString(txt, tx, ty);
            }
        }

        @Override
        public Dimension getPreferredSize() {
            return thumbnail ? new Dimension(320, 260) : new Dimension(850, 680);
        }
    }

    /* --------------------------------------------------------------------- */
    /** Dialog – one Browse button per month */
    private void showAssignDialog() {
        JDialog dlg = new JDialog(this, "Assign Images to Months", true);
        dlg.setSize(620, 520);
        dlg.setLocationRelativeTo(this);
        dlg.setLayout(new BorderLayout());

        JPanel list = new JPanel(new GridLayout(12, 1, 5, 5));
        list.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        Map<Integer, JTextField> pathFields = new HashMap<>();
        String[] monthNames = {"January", "February", "March", "April", "May", "June",
                               "July", "August", "September", "October", "November", "December"};

        for (int m = 0; m < 12; m++) {
            JPanel row = new JPanel(new BorderLayout());
            JLabel name = new JLabel(monthNames[m] + ":", SwingConstants.RIGHT);
            name.setPreferredSize(new Dimension(110, 28));
            JTextField tf = new JTextField(30);
            tf.setEditable(false);
            JButton browse = new JButton("Browse…");
            browse.addActionListener(e -> {
                JFileChooser fc = new JFileChooser();
                fc.setFileFilter(new FileNameExtensionFilter(
                        "Images", "jpg", "jpeg", "png", "gif", "bmp"));
                if (fc.showOpenDialog(dlg) == JFileChooser.APPROVE_OPTION) {
                    tf.setText(fc.getSelectedFile().getAbsolutePath());
                }
            });
            row.add(name, BorderLayout.WEST);
            row.add(tf, BorderLayout.CENTER);
            row.add(browse, BorderLayout.EAST);
            list.add(row);
            pathFields.put(m, tf);
        }
        dlg.add(new JScrollPane(list), BorderLayout.CENTER);

        // ---- OK / Cancel ------------------------------------------------
        JPanel btns = new JPanel(new FlowLayout());
        JButton ok = new JButton("OK");
        ok.addActionListener(e -> {
            monthImages.clear();
            for (int m = 0; m < 12; m++) {
                String p = pathFields.get(m).getText().trim();
                if (!p.isEmpty()) {
                    monthImages.put(m, new ImageIcon(p));
                }
            }
            updateCalendar();
            dlg.dispose();
        });
        JButton cancel = new JButton("Cancel");
        cancel.addActionListener(e -> dlg.dispose());
        btns.add(ok);
        btns.add(cancel);
        dlg.add(btns, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    /* --------------------------------------------------------------------- */
    /** Full-screen preview with a Close button */
    private void showMonthPreview(int month) {
        JDialog preview = new JDialog(this,
                "Preview – " + getMonthName(month) + " " + year, true);
        preview.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        preview.setSize(Toolkit.getDefaultToolkit().getScreenSize());
        preview.setLocationRelativeTo(null);

        JPanel monthPanel = createMonthPanel(month, false);   // false = full size
        preview.add(monthPanel, BorderLayout.CENTER);

        // close button
        JButton close = new JButton("Close");
        close.addActionListener(e -> preview.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(close);
        preview.add(south, BorderLayout.SOUTH);

        preview.setVisible(true);
    }

    private String getMonthName(int month) {
        String[] n = {"January", "February", "March", "April", "May", "June",
                      "July", "August", "September", "October", "November", "December"};
        return n[month];
    }

    /* --------------------------------------------------------------------- */
    private void printCalendar() {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setPrintable(this);
        if (job.printDialog()) {
            try { job.print(); }
            catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this,
                        "Printing failed: " + ex.getMessage(),
                        "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public int print(Graphics g, PageFormat pf, int page) throws PrinterException {
        if (page > 0) return NO_SUCH_PAGE;

        Graphics2D g2d = (Graphics2D) g;
        g2d.translate(pf.getImageableX(), pf.getImageableY());

        double scale = Math.min(pf.getImageableWidth() / getWidth(),
                                pf.getImageableHeight() / getHeight()) * 0.93;
        g2d.scale(scale, scale);

        RepaintManager.currentManager(calendarPanel).setDoubleBufferingEnabled(false);
        calendarPanel.printAll(g2d);
        RepaintManager.currentManager(calendarPanel).setDoubleBufferingEnabled(true);

        return PAGE_EXISTS;
    }

    /* --------------------------------------------------------------------- */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) { ex.printStackTrace(); }
            new CalendarCreator().setVisible(true);
        });
    }
}
