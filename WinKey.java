import javax.swing.*;
import java.awt.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WinKey {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Detect Windows version
            String osName = System.getProperty("os.name");
            String osVersion = System.getProperty("os.version");

            if (!osName.startsWith("Windows")) {
                JOptionPane.showMessageDialog(null, "This program only runs on Windows.", "Error", JOptionPane.ERROR_MESSAGE);
                System.exit(0);
                return;
            }

            String detectedVersion = detectWindowsVersion();

            // Retrieve the validation key
            String productKey = getWindowsProductKey();

            // Build the GUI message
            String message = String.format(
                "<html><body style='text-align:center; font-family:Arial;'>" +
                "<h2>Windows Validation</h2>" +
                "<p><b>Detected OS:</b> %s</p>" +
                "<p><b>Validation Key:</b></p>" +
                "<p style='font-family:monospaced; font-size:14pt; letter-spacing:2px;'>%s</p>" +
                "<p style='color:red; font-size:10pt;'>If key not found: Run as Administrator or check Notes below.</p>" +
                "</body></html>",
                detectedVersion,
                productKey != null ? productKey : "Not found"
            );

            // Create and show the GUI
            JFrame frame = new JFrame("Windows Validation Key");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(550, 350);
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel(message, SwingConstants.CENTER);
            label.setVerticalAlignment(SwingConstants.CENTER);

            // Add notes at the bottom
            JTextArea notes = new JTextArea(
                "Notes:\n" +
                "- This retrieves the OEM/Retail product key stored in the registry.\n" +
                "- For digital licenses (common on modern Windows 10/11), no 25-char key is stored.\n" +
                "- Run this program as Administrator if registry access is denied.\n" +
                "- Key format: XXXXX-XXXXX-XXXXX-XXXXX-XXXXX"
            );
            notes.setEditable(false);
            notes.setBackground(frame.getBackground());
            notes.setFont(new Font("Arial", Font.PLAIN, 10));

            frame.getContentPane().add(label, BorderLayout.CENTER);
            frame.getContentPane().add(notes, BorderLayout.SOUTH);
            frame.setVisible(true);
        });
    }

    private static String detectWindowsVersion() {
        // Improved detection using registry for ReleaseId and UBR, plus build check for Win11
        String releaseId = getRegistryValue("HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "ReleaseId");
        Integer ubr = getRegistryInteger("HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "UBR");
        Integer build = getRegistryInteger("HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion", "CurrentBuildNumber");

        // Fallback to os.version if registry fails
        if (build == null) {
            float version = Float.parseFloat(System.getProperty("os.version"));
            if (version == 5.1f) return "Windows XP";
            if (version == 6.0f) return "Windows Vista";
            if (version == 6.1f) return "Windows 7";
            return (version >= 10.0f) ? "Windows 10 or 11 (run as Admin for precise detection)" : "Windows (Unknown)";
        }

        // Precise detection
        if (build >= 22000) {
            return "Windows 11 (Build " + build + ")";
        } else if (build >= 10240) {
            return "Windows 10 " + (releaseId != null ? releaseId : "") + " (Build " + build + ")";
        } else if (build >= 7600) {
            return "Windows 7 (Build " + build + ")";
        } else if (build >= 6000) {
            return "Windows Vista (Build " + build + ")";
        } else if (build >= 2600) {
            return "Windows XP (Build " + build + ")";
        }
        return "Windows (Build " + build + ")";
    }

    private static String getWindowsProductKey() {
        // Try multiple possible locations for DigitalProductId
        String[] paths = {
            "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
            "HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\DefaultProductKey" // Fallback for some installs
        };

        byte[] digitalProductId = null;
        for (String path : paths) {
            digitalProductId = getRegistryBinary(path, "DigitalProductId");
            if (digitalProductId != null && digitalProductId.length >= 164) {
                break;
            }
        }

        if (digitalProductId == null || digitalProductId.length < 164) {
            return null;
        }

        // Standard Microsoft decoding algorithm (works for XP through 11 OEM/retail keys)
        String chars = "BCDFGHJKMPQRTVWXY2346789";
        char[] productKeyChars = new char[25];
        
        for (int i = 0; i < 25; i++) {
            int cur = 0;
            for (int j = 14; j >= 0; j--) {
                cur = cur * 256 + (digitalProductId[52 + i] & 0xFF);
                digitalProductId[52 + i] = (byte) (cur / 24);
                cur %= 24;
            }
            productKeyChars[24 - i] = chars.charAt(cur);
        }

        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            key.append(productKeyChars[i]);
            if (i % 5 == 4 && i != 24) {
                key.append('-');
            }
        }
        return key.toString();
    }

    // Helper: Get string value from registry
    private static String getRegistryValue(String keyPath, String valueName) {
        try {
            String cmd = "reg query \"" + keyPath + "\" /v " + valueName;
            Process process = Runtime.getRuntime().exec(cmd);
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";
            Pattern p = Pattern.compile(valueName + "\\s+\\w+\\s+(.*)");
            Matcher m = p.matcher(output);
            if (m.find()) {
                return m.group(1).trim();
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    // Helper: Get integer value from registry
    private static Integer getRegistryInteger(String keyPath, String valueName) {
        String val = getRegistryValue(keyPath, valueName);
        if (val != null) {
            try {
                return Integer.parseInt(val.replaceAll("[^0-9]", ""));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        return null;
    }

    // Helper: Get binary (hex) value from registry and convert to byte[]
    private static byte[] getRegistryBinary(String keyPath, String valueName) {
        try {
            String cmd = "reg query \"" + keyPath + "\" /v " + valueName;
            Process process = Runtime.getRuntime().exec(cmd);
            java.util.Scanner scanner = new java.util.Scanner(process.getInputStream()).useDelimiter("\\A");
            String output = scanner.hasNext() ? scanner.next() : "";

            Pattern p = Pattern.compile(valueName + "\\s+\\w+\\s+(.*)");
            Matcher m = p.matcher(output);
            if (m.find()) {
                String hex = m.group(1).trim().replaceAll("\\s", "");
                return hexStringToByteArray(hex);
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                                 + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
