// HangmanGame.java - GUI-basiertes Hangman-Spiel mit Swing

package hangman;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class HangmanGame extends JFrame {
    // GUI-Komponenten
    private final JLabel wordLabel = new JLabel();
    private final JLabel historyLabel = new JLabel();
    private final JLabel imageLabel = new JLabel();
    private final JTextField inputField = new JTextField(3);
    private final JButton guessButton = new JButton("Guess");
    private final JButton restartButton = new JButton("Restart");

    // Spiel-Daten
    private final ArrayList<String> words = new ArrayList<>();
    private String currentWord;
    private char[] displayWord;
    private Set<Character> guessedLetters = new HashSet<>();
    private int maxErrors = 8;
    private int errors = 0;

    // Einstellungen / Optionen
    private final JCheckBoxMenuItem showHistoryItem = new JCheckBoxMenuItem("Show History", true);

    public HangmanGame() {
        setTitle("Hangman Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(400, 300);
        setLayout(new BorderLayout());

        // Wörter laden
        loadWords("words.txt");

        // ===== TOP PANEL: Wortanzeige =====
        JPanel topPanel = new JPanel();
        wordLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        topPanel.add(wordLabel);
        add(topPanel, BorderLayout.NORTH);

        // ===== CENTER PANEL: Bildanzeige =====
        JPanel centerPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(imageLabel, gbc);
        add(centerPanel, BorderLayout.CENTER);

        // ===== INPUT PANEL: Buchstaben-Eingabe & Buttons =====
        Dimension inputSize = new Dimension(120, 50);

        inputField.setPreferredSize(inputSize);
        inputField.setFont(new Font("SansSerif", Font.BOLD, 24));
        inputField.setHorizontalAlignment(JTextField.CENTER);

        // Automatisch in Großbuchstaben umwandeln
        AbstractDocument doc = (AbstractDocument) inputField.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                if (text != null) fb.replace(offset, length, text.toUpperCase(), attrs);
            }
            @Override
            public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
                if (text != null) fb.insertString(offset, text.toUpperCase(), attr);
            }
        });

        guessButton.setPreferredSize(inputSize);
        guessButton.setFont(new Font("SansSerif", Font.BOLD, 24));

        restartButton.setPreferredSize(inputSize);
        restartButton.setFont(new Font("SansSerif", Font.BOLD, 24));

        JPanel inputPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        inputPanel.add(inputField);
        inputPanel.add(guessButton);
        inputPanel.add(restartButton);
        add(inputPanel, BorderLayout.SOUTH);

        // ===== RIGHT PANEL: History-Anzeige =====
        JPanel historyPanel = new JPanel();
        historyLabel.setFont(new Font("Monospaced", Font.BOLD, 24));
        historyLabel.setPreferredSize(new Dimension(300, 200));
        historyPanel.add(historyLabel);
        add(historyPanel, BorderLayout.EAST);

        // ===== MENÜ =====
        JMenuBar menuBar = new JMenuBar();
        JMenu settingsMenu = new JMenu("Settings");
        settingsMenu.setFont(new Font("SansSerif", Font.PLAIN, 20));

        JMenuItem attemptsItem = new JMenuItem("Set Max Attempts");
        attemptsItem.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(this, "Enter max attempts (1-9):", maxErrors);
            try {
                int newMax = Integer.parseInt(input);
                if (newMax >= 1 && newMax <= 8) maxErrors = newMax;
            } catch (NumberFormatException ignored) {}
        });

        settingsMenu.add(attemptsItem);
        settingsMenu.add(showHistoryItem);
        menuBar.add(settingsMenu);
        setJMenuBar(menuBar);

        // ===== EVENTS =====
        guessButton.addActionListener(e -> guessLetter());
        restartButton.addActionListener(e -> resetGame());
        inputField.addActionListener(e -> guessLetter());

        // Spiel starten
        resetGame();

        // Fenster maximieren & anzeigen
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }

    // Buchstabe raten
    private void guessLetter() {
        String input = inputField.getText().toUpperCase();
        inputField.setText("");
        if (input.length() != 1 || !Character.isLetter(input.charAt(0))) return;

        char guess = input.charAt(0);
        if (guessedLetters.contains(guess)) return;

        guessedLetters.add(guess);
        boolean correct = false;
        for (int i = 0; i < currentWord.length(); i++) {
            if (currentWord.charAt(i) == guess) {
                displayWord[i] = guess;
                correct = true;
            }
        }
        if (!correct) errors++;
        updateDisplay();
    }

    // Anzeige aktualisieren (Wort, Bild, Status)
    private void updateDisplay() {
        wordLabel.setText(new String(displayWord).replace("", " ").trim());

        if (showHistoryItem.isSelected()) {
            historyLabel.setText("<html>Guessed: " + guessedLetters.toString() + "</html>");
        } else {
            historyLabel.setText("");
        }

        String imagePath = "images/" + Math.min(errors, 8) + ".png";
        imageLabel.setIcon(new ImageIcon(imagePath));
        imageLabel.setPreferredSize(new Dimension(307, 512));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);

        if (errors >= maxErrors) {
            wordLabel.setText("Game Over! Word was: " + currentWord);
            guessButton.setEnabled(false);
        } else if (new String(displayWord).equals(currentWord)) {
            wordLabel.setText("You Win! Word: " + currentWord);
            guessButton.setEnabled(false);
        }
    }

    // Neues Spiel starten
    private void resetGame() {
        guessedLetters.clear();
        errors = 0;
        currentWord = words.get(new Random().nextInt(words.size())).toUpperCase();
        displayWord = new char[currentWord.length()];
        Arrays.fill(displayWord, '_');
        guessButton.setEnabled(true);
        updateDisplay();
    }

    // Wörter aus Datei laden oder Fallback verwenden
    private void loadWords(String fileName) {
        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) words.add(line);
            }
        } catch (FileNotFoundException e) {
            words.addAll(Arrays.asList("PROGRAMMIERUNG", "JAVA", "SWING", "LERNZIELE", "KLASSEN"));
        }
    }

    // Einstiegspunkt
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new HangmanGame().setVisible(true));
    }
}
