import javax.swing.*;
import java.awt.*;
import java.util.Random;
import javax.swing.Timer;

public class Peeka extends JWindow {
    private static final String IMAGE_PATH = "/images/image.png";
    private final ImageIcon icon;
    private final JLabel label;
    private final Random random = new Random();
    private final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    private Timer animationTimer;
    private Timer dwellTimer;
    private Timer delayTimer;
    private Side side;
    private int fixedPos;
    private int startPos;
    private int endPos;
    private int currentPos;
    private int stepSize;
    private boolean showing = true;

    private enum Side {
        TOP, BOTTOM, LEFT, RIGHT
    }

    public Peeka() {
        icon = new ImageIcon(Peeka.class.getResource(IMAGE_PATH));
        if (icon.getImageLoadStatus() != MediaTracker.COMPLETE) {
            System.err.println("Failed to load image: " + IMAGE_PATH);
            System.exit(1);
        }
        label = new JLabel(icon);
        add(label);
        setSize(icon.getIconWidth(), icon.getIconHeight());
        setBackground(new Color(0, 0, 0, 0));
        setAlwaysOnTop(true);

        animationTimer = new Timer(20, e -> animate());
        dwellTimer = new Timer(2000, e -> startHide());
        dwellTimer.setRepeats(false);
        delayTimer = new Timer(0, e -> startNextAppearance());
        delayTimer.setRepeats(false);

        startNextAppearance();
    }

    private void startNextAppearance() {
        chooseRandomSide();
        computePositions();
        if (side == Side.TOP || side == Side.BOTTOM) {
            setLocation(fixedPos, startPos);
        } else {
            setLocation(startPos, fixedPos);
        }
        currentPos = startPos;
        stepSize = (endPos - startPos) / 20; // 20 steps
        if (stepSize == 0) stepSize = endPos > startPos ? 1 : -1;
        showing = true;
        setVisible(true);
        animationTimer.start();
    }

    private void chooseRandomSide() {
        Side[] sides = Side.values();
        side = sides[random.nextInt(sides.length)];
    }

    private void computePositions() {
        int w = getWidth();
        int h = getHeight();
        int screenW = screenSize.width;
        int screenH = screenSize.height;

        switch (side) {
            case BOTTOM:
                fixedPos = random.nextInt(screenW - w);
                startPos = screenH;
                endPos = screenH - h;
                break;
            case TOP:
                fixedPos = random.nextInt(screenW - w);
                startPos = -h;
                endPos = 0;
                break;
            case LEFT:
                fixedPos = random.nextInt(screenH - h);
                startPos = -w;
                endPos = 0;
                break;
            case RIGHT:
                fixedPos = random.nextInt(screenH - h);
                startPos = screenW;
                endPos = screenW - w;
                break;
        }
    }

    private void animate() {
        currentPos += stepSize;
        boolean reached = (stepSize > 0 && currentPos >= endPos) || (stepSize < 0 && currentPos <= endPos);
        if (reached) {
            currentPos = endPos;
            animationTimer.stop();
            if (showing) {
                dwellTimer.start();
            } else {
                setVisible(false);
                int randomDelay = 5000 + random.nextInt(25000); // 5-30 seconds
                delayTimer.setInitialDelay(randomDelay);
                delayTimer.start();
            }
        }
        if (side == Side.TOP || side == Side.BOTTOM) {
            setLocation(fixedPos, currentPos);
        } else {
            setLocation(currentPos, fixedPos);
        }
    }

    private void startHide() {
        showing = false;
        stepSize = -stepSize; // Reverse direction
        endPos = startPos; // Target is now back to start
        startPos = currentPos; // But for calculation, swap
        animationTimer.start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Peeka());
    }
}
