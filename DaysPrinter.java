import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Locale;
import javax.print.*;
import javax.print.attribute.*;
import javax.print.attribute.standard.*;

public class DaysPrinter extends JFrame implements ActionListener {
    private JComboBox<PrintService> printerCombo;
    private JRadioButton letterBtn, legalBtn, a4Btn;
    private JRadioButton portraitBtn, landscapeBtn;
    private JButton printButton;
    private JButton previewButton;
    private JComboBox<String> monthCombo;
    private JSpinner yearSpinner;

    public DaysPrinter() {
        setTitle("DaysPrinter Printable Monthly Planner");
        setSize(620, 480);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 12, 12, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        panel.add(new JLabel("Select Printer"), gbcSet(0, 0, 1));
        PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
        printerCombo = new JComboBox<>(services.length > 0 ? services : new PrintService[]{PrintServiceLookup.lookupDefaultPrintService()});
        printerCombo.setRenderer(new PrintServiceListCellRenderer());
        printerCombo.setSelectedItem(PrintServiceLookup.lookupDefaultPrintService());
        gbc.gridx = 1; gbc.gridwidth = 2;
        panel.add(printerCombo, gbc);

        JPanel monthYearPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        monthYearPanel.add(new JLabel("Month"));
        String[] months = {"January", "February", "March", "April", "May", "June",
                           "July", "August", "September", "October", "November", "December"};
        monthCombo = new JComboBox<>(months);
        monthCombo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        monthYearPanel.add(monthCombo);

        monthYearPanel.add(new JLabel("Year"));
        SpinnerNumberModel yearModel = new SpinnerNumberModel(LocalDate.now().getYear(), 1900, 2100, 1);
        yearSpinner = new JSpinner(yearModel);
        yearSpinner.setPreferredSize(new Dimension(80, 28));
        monthYearPanel.add(yearSpinner);

        gbc.gridx = 0; gbc.gridwidth = 4; gbc.gridy = 1;
        panel.add(monthYearPanel, gbc);

        panel.add(new JLabel("Paper Size"), gbcSet(0, 2, 1));
        letterBtn = new JRadioButton("Letter 85 by 11", true);
        legalBtn = new JRadioButton("Legal 85 by 14");
        a4Btn = new JRadioButton("A4");
        ButtonGroup sizeGroup = new ButtonGroup();
        sizeGroup.add(letterBtn); sizeGroup.add(legalBtn); sizeGroup.add(a4Btn);
        JPanel sizePanel = new JPanel(new GridLayout(1, 3, 20, 0));
        sizePanel.add(letterBtn); sizePanel.add(legalBtn); sizePanel.add(a4Btn);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.gridy = 2;
        panel.add(sizePanel, gbc);

        panel.add(new JLabel("Orientation"), gbcSet(0, 3, 1));
        portraitBtn = new JRadioButton("Portrait", true);
        landscapeBtn = new JRadioButton("Landscape");
        ButtonGroup orientGroup = new ButtonGroup();
        orientGroup.add(portraitBtn); orientGroup.add(landscapeBtn);
        JPanel orientPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 30, 0));
        orientPanel.add(portraitBtn); orientPanel.add(landscapeBtn);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.gridy = 3;
        panel.add(orientPanel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 10));
        
        previewButton = new JButton("Print Preview");
        previewButton.setFont(new Font("SansSerif", Font.PLAIN, 18));
        previewButton.addActionListener(this);
        buttonPanel.add(previewButton);

        printButton = new JButton("Print");
        printButton.setFont(new Font("SansSerif", Font.BOLD, 18));
        printButton.addActionListener(this);
        buttonPanel.add(printButton);

        gbc.gridx = 0; gbc.gridwidth = 4; gbc.gridy = 4;
        gbc.insets = new Insets(40, 12, 20, 12);
        panel.add(buttonPanel, gbc);

        add(panel);
        setResizable(false);
    }

    private GridBagConstraints gbcSet(int x, int y, int width) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y; g.gridwidth = width;
        g.insets = new Insets(12, 12, 12, 12);
        g.fill = GridBagConstraints.HORIZONTAL;
        return g;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == previewButton) {
            showPreview();
            return;
        }

        PrintService printer = (PrintService) printerCombo.getSelectedItem();
        if (printer == null) {
            JOptionPane.showMessageDialog(this, "No printer selected or available");
            return;
        }

        MediaSizeName size = legalBtn.isSelected() ? MediaSizeName.NA_LEGAL :
                            a4Btn.isSelected() ? MediaSizeName.ISO_A4 : MediaSizeName.NA_LETTER;
        OrientationRequested orient = landscapeBtn.isSelected() ?
                OrientationRequested.LANDSCAPE : OrientationRequested.PORTRAIT;

        int monthIndex = monthCombo.getSelectedIndex();
        int year = (Integer) yearSpinner.getValue();
        YearMonth ym = YearMonth.of(year, monthIndex + 1);

        PrinterJob job = PrinterJob.getPrinterJob();
        try {
            job.setPrintService(printer);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Cannot use selected printer " + ex.getMessage());
            return;
        }

        PrintRequestAttributeSet attr = new HashPrintRequestAttributeSet();
        attr.add(size);
        attr.add(orient);
        job.setPrintable(new WritableCalendarPrintable(ym));

        try {
            job.print(attr);
            String monthName = monthCombo.getSelectedItem().toString();
            JOptionPane.showMessageDialog(this,
                "Success Printed " + monthName + " " + year,
                "Calendar Printed", JOptionPane.INFORMATION_MESSAGE);
        } catch (PrinterException ex) {
            JOptionPane.showMessageDialog(this, "Printing failed " + ex.getMessage());
        }
    }

    private void showPreview() {
        int monthIndex = monthCombo.getSelectedIndex();
        int year = (Integer) yearSpinner.getValue();
        YearMonth ym = YearMonth.of(year, monthIndex + 1);

        PageFormat pf = new PageFormat();
        Paper paper = new Paper();
        paper.setSize(8.5 * 72, 11 * 72);
        paper.setImageableArea(36, 36, paper.getWidth() - 72, paper.getHeight() - 72);
        pf.setPaper(paper);
        if (landscapeBtn.isSelected()) pf.setOrientation(PageFormat.LANDSCAPE);

        Printable printable = new WritableCalendarPrintable(ym);

        JPanel previewPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                double scale = Math.min((double)getWidth() / pf.getWidth(), (double)getHeight() / pf.getHeight()) * 0.9;
                g2d.translate((getWidth() - pf.getWidth() * scale)/2, (getHeight() - pf.getHeight() * scale)/2);
                g2d.scale(scale, scale);
                try {
                    printable.print(g2d, pf, 0);
                } catch (Exception ignored) {}
                g2d.dispose();
            }
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(800, 1000);
            }
        };

        JScrollPane scroll = new JScrollPane(previewPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        JDialog dialog = new JDialog(this, "Print Preview " + ym.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + year, true);
        dialog.setSize(900, 700);
        dialog.setLocationRelativeTo(this);
        dialog.add(scroll);
        dialog.setVisible(true);
    }

    private static class PrintServiceListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof PrintService) {
                setText(((PrintService)value).getName());
            }
            return this;
        }
    }

    private static class WritableCalendarPrintable implements Printable {
        private final YearMonth yearMonth;
        public WritableCalendarPrintable(YearMonth yearMonth) {
            this.yearMonth = yearMonth;
        }
        @Override
        public int print(Graphics g, PageFormat pf, int pageIndex) {
            if (pageIndex > 0) return NO_SUCH_PAGE;
            Graphics2D g2d = (Graphics2D) g;
            g2d.translate(pf.getImageableX(), pf.getImageableY());
            double w = pf.getImageableWidth();
            double h = pf.getImageableHeight();
            int daysInMonth = yearMonth.lengthOfMonth();
            LocalDate first = yearMonth.atDay(1);
            int startCol = (first.getDayOfWeek().getValue() == 7) ? 6 : first.getDayOfWeek().getValue() - 1;

            Font titleFont = new Font("SansSerif", Font.BOLD, 48);
            Font headerFont = new Font("SansSerif", Font.BOLD, 16);  // Smaller, no overlap
            Font dayFont = new Font("SansSerif", Font.BOLD, 20);
            Color lightGray = new Color(180, 180, 180);
            Color darkGray = new Color(50, 50, 50);

            g2d.setColor(Color.BLACK);
            String title = yearMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault())
                    + " " + yearMonth.getYear();
            g2d.setFont(titleFont);
            FontMetrics fmTitle = g2d.getFontMetrics();
            g2d.drawString(title, (int)((w - fmTitle.stringWidth(title)) / 2), 80);

            String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
            g2d.setFont(headerFont);
            FontMetrics fmHeader = g2d.getFontMetrics();
            double cellW = w / 7.0;
            double cellH = (h - 170) / 6.0;
            double gridY = 130;

            for (int i = 0; i < 7; i++) {
                String d = days[i];
                int x = (int)(i * cellW + (cellW - fmHeader.stringWidth(d)) / 2);
                g2d.setColor(darkGray);
                g2d.drawString(d, x, (int)gridY + 35);
            }

            g2d.setColor(lightGray);
            g2d.setFont(dayFont);
            int dayNum = 1;
            for (int row = 0; row < 6; row++) {
                for (int col = 0; col < 7; col++) {
                    int x = (int)(col * cellW);
                    int y = (int)(gridY + 55 + row * cellH);
                    g2d.drawRect(x, y, (int)cellW, (int)cellH);
                    int index = row * 7 + col;
                    if (index >= startCol && dayNum <= daysInMonth) {
                        String num = String.valueOf(dayNum);
                        g2d.setColor(Color.BLACK);
                        g2d.drawString(num, x + 20, y + 40);
                        dayNum++;
                    }
                }
            }
            return PAGE_EXISTS;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            new DaysPrinter().setVisible(true);
        });
    }
}
