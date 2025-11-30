// ===== PHASE 1: IMPORTS AND CONFIGURATION =====
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Robotic AI Council - Single File Macro System
 * Controls browsers via mouse/keyboard to execute specialized prompt council
 * User must pre-login and position browser windows before running
 */
public class RoboticAICouncil {
    
    // Configuration - Adjust these coordinates for your screen setup
    private static final Map<String, Point> BROWSER_COORDINATES = Map.of(
        "chatgpt", new Point(100, 100),    // ChatGPT browser click position
        "grok", new Point(100, 500),       // Grok browser click position  
        "deepseek", new Point(100, 900)    // DeepSeek browser click position
    );
    
    private static final int RESPONSE_WAIT_MS = 20000; // 20 seconds for AI response
    private static final int TYPE_DELAY_MS = 50;       // Delay between keystrokes
    
    private Robot robot;
    private Scanner scanner;

    public static void main(String[] args) throws Exception {
        new RoboticAICouncil().run();
    }
    
    public RoboticAICouncil() throws AWTException {
        this.robot = new Robot();
        this.scanner = new Scanner(System.in);
    }

    // ===== PHASE 2: CORE COUNCIL TEMPLATES =====
    /**
     * Pre-defined council templates that transform a user prompt into specialized roles
     * Each role targets a different AI with a specific perspective
     */
    private enum CouncilTemplate {
        CODE_REVIEW(
            "Code Review Council",
            Arrays.asList(
                new CouncilRole("architect", "chatgpt", 
                    "As senior architect, provide optimal implementation for: {PROMPT}. Focus on best practices, scalability, and modern patterns."),
                new CouncilRole("critic", "grok", 
                    "As critical reviewer, identify potential issues, edge cases, anti-patterns, and performance concerns in: {PROMPT}"),
                new CouncilRole("practitioner", "deepseek", 
                    "As hands-on developer, provide minimal working code examples for: {PROMPT}. Focus on practicality over theory.")
            )
        ),
        
        LEARNING_ASSISTANT(
            "Learning Assistant Council", 
            Arrays.asList(
                new CouncilRole("teacher", "chatgpt",
                    "Explain this concept clearly for beginners: {PROMPT}. Use simple analogies and practical examples."),
                new CouncilRole("skeptic", "grok",
                    "Challenge this concept and identify common misunderstandings: {PROMPT}. What are the limitations?"),
                new CouncilRole("researcher", "deepseek",
                    "Provide detailed technical depth and related concepts for: {PROMPT}. Include references to further learning.")
            )
        );
        
        final String name;
        final List<CouncilRole> roles;
        
        CouncilTemplate(String name, List<CouncilRole> roles) {
            this.name = name;
            this.roles = roles;
        }
    }
    
    // ===== PHASE 3: COUNCIL ROLE DEFINITION =====
    /**
     * Represents a specialized role that will query a specific AI
     */
    private static class CouncilRole {
        String roleName;
        String browserTarget;
        String instructionTemplate;
        
        CouncilRole(String roleName, String browserTarget, String instructionTemplate) {
            this.roleName = roleName;
            this.browserTarget = browserTarget;
            this.instructionTemplate = instructionTemplate;
        }
        
        String buildPrompt(String userPrompt) {
            return instructionTemplate.replace("{PROMPT}", userPrompt);
        }
    }

    // ===== PHASE 4: USER INTERFACE AND SETUP =====
    /**
     * Main program flow - gets user input and executes the council
     */
    private void run() {
        System.out.println("=== ROBOTIC AI COUNCIL ===");
        System.out.println("PREREQUISITES:");
        System.out.println("1. Have ChatGPT, Grok, and DeepSeek open in browser tabs");
        System.out.println("2. Already logged into each service");
        System.out.println("3. Browser windows positioned at configured coordinates");
        System.out.println();
        
        // Template selection
        CouncilTemplate template = selectTemplate();
        System.out.println();
        
        // Get user prompt
        System.out.println("Enter your prompt:");
        String userPrompt = scanner.nextLine();
        System.out.println();
        
        // Confirmation
        System.out.println("Ready to execute " + template.name + " with prompt:");
        System.out.println("\"" + userPrompt + "\"");
        System.out.println();
        System.out.println("Position cursor safely and press ENTER to begin (5 second delay)...");
        scanner.nextLine();
        
        // Execute the council
        executeCouncil(template, userPrompt);
        
        System.out.println("=== COUNCIL COMPLETE ===");
    }
    
    private CouncilTemplate selectTemplate() {
        System.out.println("Select council template:");
        CouncilTemplate[] templates = CouncilTemplate.values();
        for (int i = 0; i < templates.length; i++) {
            System.out.println((i + 1) + ". " + templates[i].name);
        }
        
        int choice = scanner.nextInt();
        scanner.nextLine(); // consume newline
        
        return templates[choice - 1];
    }

    // ===== PHASE 5: CORE AUTOMATION ENGINE =====
    /**
     * Executes the complete council by controlling browsers robotically
     */
    private void executeCouncil(CouncilTemplate template, String userPrompt) {
        System.out.println("Starting council execution...");
        
        for (CouncilRole role : template.roles) {
            System.out.println("Executing " + role.roleName + " role on " + role.browserTarget + "...");
            
            try {
                // Step 1: Activate target browser
                activateBrowser(role.browserTarget);
                
                // Step 2: Clear existing prompt text
                clearPromptArea();
                
                // Step 3: Type specialized prompt
                String specializedPrompt = role.buildPrompt(userPrompt);
                typeString(specializedPrompt);
                
                // Step 4: Submit prompt
                submitPrompt();
                
                // Step 5: Wait for AI response
                waitForResponse();
                
                System.out.println("✓ " + role.roleName + " complete");
                
            } catch (Exception e) {
                System.out.println("✗ Failed on " + role.roleName + ": " + e.getMessage());
            }
        }
    }
    
    // ===== PHASE 6: ROBOTIC CONTROL METHODS =====
    /**
     * Activates a browser window by clicking at its predefined coordinates
     */
    private void activateBrowser(String browser) {
        Point coords = BROWSER_COORDINATES.get(browser);
        if (coords == null) {
            throw new IllegalArgumentException("Unknown browser: " + browser);
        }
        
        robot.mouseMove(coords.x, coords.y);
        robot.delay(500);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(1000); // Wait for browser to activate
    }
    
    /**
     * Clears the prompt area using Ctrl+A → Delete
     */
    private void clearPromptArea() {
        // Select all (Ctrl+A)
        robot.keyPress(KeyEvent.VK_CONTROL);
        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_CONTROL);
        robot.delay(500);
        
        // Delete selection
        robot.keyPress(KeyEvent.VK_DELETE);
        robot.keyRelease(KeyEvent.VK_DELETE);
        robot.delay(500);
    }
    
    /**
     * Types a string character by character with realistic delays
     */
    private void typeString(String text) {
        for (char c : text.toCharArray()) {
            typeCharacter(c);
            robot.delay(TYPE_DELAY_MS);
        }
    }
    
    /**
     * Types a single character, handling special cases
     */
    private void typeCharacter(char c) {
        if (Character.isUpperCase(c)) {
            robot.keyPress(KeyEvent.VK_SHIFT);
        }
        
        try {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (KeyEvent.CHAR_UNDEFINED != keyCode) {
                robot.keyPress(keyCode);
                robot.keyRelease(keyCode);
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not type character '" + c + "'");
        }
        
        if (Character.isUpperCase(c)) {
            robot.keyRelease(KeyEvent.VK_SHIFT);
        }
    }
    
    /**
     * Submits the prompt by pressing Enter
     */
    private void submitPrompt() {
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.delay(1000);
    }
    
    /**
     * Waits for the AI to generate a response
     */
    private void waitForResponse() {
        System.out.println("    Waiting " + (RESPONSE_WAIT_MS / 1000) + " seconds for response...");
        robot.delay(RESPONSE_WAIT_MS);
    }
}
