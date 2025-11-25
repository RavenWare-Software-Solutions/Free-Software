import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.time.LocalDate;
import java.time.YearMonth;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class DaysPrinter extends JFrame implements ActionListener {

    private JComboBox<PrintService> printerCombo;
    private JRadioButton letterBtn, legalBtn, a4Btn;
    private JRadioButton portraitBtn, landscapeBtn;
    private JButton printButton;

    public DaysPrinter() {
        setTitle("DaysPrinter - Printable Monthly Planner");
        setSize(580, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Printer
        panel.add(new JLabel("Select Printer:"), gbcSet(0, 0, 1));
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        printerCombo = new JComboBox<>(services);
        printerCombo.setRenderer(new PrintServiceListCellRenderer());
        printerCombo.setSelectedItem(PrintServiceLookup.lookupDefaultPrintService());
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(printerCombo, gbc);

        // Paper Size
        panel.add(new JLabel("Paper Size:"), gbcSet(0, 1, 1));
        letterBtn = new JRadioButton("Letter (8.5\" × 11\")", true);
        legalBtn = new JRadioButton("Legal (8.5\" × 14\")");
        a4Btn = new JRadioButton("A4");
        ButtonGroup sizeGroup = new ButtonGroup();
        sizeGroup.add(letterBtn); sizeGroup.add(legalBtn); sizeGroup.add(a4Btn);
        JPanel sizePanel = new JPanel(new GridLayout(3,1,0,10));
        sizePanel.add(letterBtn); sizePanel.add(legalBtn); sizePanel.add(a4Btn);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.gridy = 1;
        panel.add(sizePanel, gbc);

        // Orientation
        panel.add(new JLabel("Orientation:"), gbcSet(0, 2, 1));
        portraitBtn = new JRadioButton("Portrait", true);
        landscapeBtn = new JRadioButton("Landscape");
        ButtonGroup orientGroup = new ButtonGroup();
        orientGroup.add(portraitBtn); orientGroup.add(landscapeBtn);
        JPanel orientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 25, 0));
        orientPanel.add(portraitBtn); orientPanel.add(landscapeBtn);
        gbc.gridx = 1; gbc.gridwidth = 2; gbc.gridy = 2;
        panel.add(orientPanel, gbc);

        // Print Button
        printButton = new JButton("PRINT CURRENT MONTH CALENDAR");
        printButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        printButton.setBackground(new Color(0, 120, 215));
        printButton.setForeground(Color.WHITE);
        printButton.addActionListener(this);
        gbc.gridx = 0; gbc.gridwidth = 3; gbc.gridy = 3;
        gbc.insets = new Insets(30, 12, 12, 12);
        panel.add(printButton, gbc);

        add(panel);
        setResizable(false);
    }

    private GridBagConstraints gbcSet(int x, int y, int width) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y; g.gridwidth = width;
        g.insets = new Insets(12,12,12,12);
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        PrintService printer = (PrintService) printerCombo.getSelectedItem();
        if (printer == null) {
            JOptionPane.showMessageDialog(this, "No printer selected!");
            return;
        }

        MediaSizeName size = legalBtn.isSelected() ? MediaSizeName.NA_LEGAL :
                            a4Btn.isSelected() ? MediaSizeName.ISO_A4 : MediaSizeName.NA_LETTER;

        OrientationRequested orient = landscapeBtn.isSelected() ?
                OrientationRequested.LANDSCAPE : OrientationRequested.PORTRAIT;

        PrinterJob job = PrinterJob.getPrinterJob();
        try { job.setPrintService(printer); }
        catch (PrinterException ex) { JOptionPane.showMessageDialog(this, "Printer error."); return; }

        PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
        attr.add(size);
        attr.add(orient);

        job.setPrintable(new WritableCalendarPrintable());

        try {
            job.print(attr);
            JOptionPane.showMessageDialog(this,
                "Success!\n" + printer.getName() + "\n" +
                getSelectedPaperName() + " • " + (landscapeBtn.isSelected()?"Landscape":"Portrait"),
                "Calendar Printed", JOptionPane.INFORMATION_MESSAGE);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Printing failed: " + ex.getMessage());
        }
    }

    private String getSelectedPaperName() {
        if (legalBtn.isSelected()) return "Legal (8.5\" × 14\")";
        if (a4Btn.isSelected()) return "A4";
        return "Letter (8.5\" × 11\")";
    }

    private static class PrintServiceListCellRenderer extends DefaultListCellRenderer {
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PrintService) setText(((PrintService)value).getName());
            return this;
        }
    }

    // ==================================================================
    // FINAL "WRITE-ALL-OVER-IT" CALENDAR DESIGN
    // ==================================================================
    private static class WritableCalendarPrintable implements Printable {
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            if (pageIndex > 0) return NO_SUCH_PAGE;

            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());

            double w = pf.getImageableWidth();
            double h = pf.getImageableHeight();

            YearMonth ym = YearMonth.now();
            int daysInMonth = ym.lengthOfMonth();
            LocalDate first = ym.atDay(1);
            int startCol = (first.getDayOfWeek().getValue() == 7) ? 6 : first.getDayOfWeek().getValue() - 1;

            // === Fonts & Colors ===
            Font titleFont   = new Font("SansSerif", Font.BOLD, 42);
            Font headerFont  = new Font("SansSerif", Font.BOLD, 20);
            Font dayFont     = new Font("SansSerif", Font.BOLD, 16);
            Color lightGray  = new Color(220, 220, 220);   // Very light lines
            Color darkGray   = new Color(100, 100, 100);

            g2d.setColor(Color.BLACK);

            // === Month + Year Title ===
            String title = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                    + " " + ym.getYear();
            g2d.setFont(titleFont);
            FontMetrics fmTitle = g2d.getFontMetrics();
            g2d.drawString(title, (int)(w - fmTitle.stringWidth(title))/2, 70);

            // === Weekday Headers ===
            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            g2d.setFont(headerFont);
            FontMetrics fmHeader = g2d.getFontMetrics();

            double cellW = w / 7.0;
            double cellH = (h - 160) / 6.0;      // Max possible height for huge writable cells
            double gridY = 120;

            for (int i = 0; i < 7; i++) {
                String d = days[i];
                int x = (int)(i * cellW + (cellW - fmHeader.stringWidth(d)) / 2);
                g2d.setColor(darkGray);
                g2d.drawString(d, x, (int)gridY + 35);
            }

            // === Draw 6×7 grid + day numbers in top-left ===
            g2d.setColor(lightGray);
            g2d.setFont(dayFont);
            FontMetrics fmDay = g2d.getFontMetrics();

            int dayNum = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    int x = (int)(col * cellW);
                    int y = (int)(gridY + 55 + row * cellH);

                    // Draw light border
                    g2d.drawRect(x, y, (int)cellW, (int)cellH);

                    int index = row * 7 + col;
                    if (index >= startCol && dayNum <= daysInMonth) {
                        String num = String.valueOf(dayNum);
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(num, x + 15, y + 30);   // Top-left corner
                        g2d.setColor(lightGray);
                        dayNum++;
                    }
                }
            }

            return PAGE_EXISTS;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
            catch (Exception ignored) {}
            new DaysPrinter().setVisible(true);
        });
    }
}
