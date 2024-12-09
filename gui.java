import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class gui {

    private final int BOARD_SIZE = 4;
    private final JFrame frame;
    private final JPanel mainPanel;
    private final JTextField[] pieceInputs;
    private final JButton solveButton;
    private final ExecutorService executor;

    private List<JPanel> boardPanels;
    private List<JLabel> solutionStatusLabels;

    private final PuzzleSolver puzzleSolver;
    private final AtomicBoolean solutionFound;
    private final AtomicInteger solutionThreadIndex;

    public gui() {
        puzzleSolver = new PuzzleSolver();

        // Set up the frame
        frame = new JFrame("Puzzle Solver");
        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(200, 255, 200));

        // Left panel for inputs
        JPanel leftPanel = new JPanel(new GridLayout(8, 2));
        leftPanel.setBackground(new Color(200, 255, 200));

        String[] pieceLabels = {"Z", "I", "J", "L", "O", "S", "T"};
        pieceInputs = new JTextField[pieceLabels.length];
        for (int i = 0; i < pieceLabels.length; i++) {
            JLabel label = new JLabel(pieceLabels[i], JLabel.CENTER);
            label.setForeground(Color.BLACK);
            leftPanel.add(label);

            pieceInputs[i] = new JTextField("0");
            pieceInputs[i].setHorizontalAlignment(JTextField.CENTER);
            pieceInputs[i].setBackground(Color.WHITE);
            leftPanel.add(pieceInputs[i]);
        }

        // Solve button
        solveButton = new JButton("Solve");
        solveButton.setBackground(Color.BLACK);
        solveButton.setForeground(Color.WHITE);
        solveButton.addActionListener(e -> solvePuzzle());
        leftPanel.add(new JLabel());
        leftPanel.add(solveButton);
        frame.add(leftPanel, BorderLayout.WEST);

        // Main panel for solution visualization
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayout(2, 3));
        frame.add(mainPanel, BorderLayout.CENTER);

        frame.setVisible(true);
        executor = Executors.newFixedThreadPool(7);

        boardPanels = new ArrayList<>();
        solutionStatusLabels = new ArrayList<>();

        // Shared flags
        solutionFound = new AtomicBoolean(false);
        solutionThreadIndex = new AtomicInteger(-1);
    }

    private void solvePuzzle() {
        int[] pieceCounts = new int[pieceInputs.length];
        for (int i = 0; i < pieceInputs.length; i++) {
            try {
                pieceCounts[i] = Integer.parseInt(pieceInputs[i].getText().trim());
            } catch (NumberFormatException ex) {
                pieceCounts[i] = 0;
            }
        }

        // Define Tetris shapes based on user input
        List<int[][]> pieces = new ArrayList<>();
        for (int i = 0; i < pieceCounts[0]; i++) pieces.add(new int[][]{{1, 1, 0}, {0, 1, 1}}); // L-shape
        for (int i = 0; i < pieceCounts[1]; i++) pieces.add(new int[][]{{1, 1, 1, 1}}); // Z-shape
        for (int i = 0; i < pieceCounts[2]; i++) pieces.add(new int[][]{{0, 0, 1}, {1, 1, 1}}); // I-shape
        for (int i = 0; i < pieceCounts[3]; i++) pieces.add(new int[][]{{1, 0, 0}, {1, 1, 1}}); // J-shape
        for (int i = 0; i < pieceCounts[4]; i++) pieces.add(new int[][]{{1, 1}, {1, 1}}); // T-shape
        for (int i = 0; i < pieceCounts[5]; i++) pieces.add(new int[][]{{0, 1, 1}, {1, 1, 0}}); // S-shape
        for (int i = 0; i < pieceCounts[6]; i++) pieces.add(new int[][]{{1, 1, 1}, {0, 1, 0}}); // O-shape

        boardPanels.clear();
        solutionStatusLabels.clear();

        Thread[] threads = new Thread[4];  // Array to store the threads

        for (int i = 0; i < 4; i++) {
            final int threadIndex = i;
            JPanel boardPanel = new JPanel();
            boardPanel.setLayout(new GridLayout(BOARD_SIZE, BOARD_SIZE));
            boardPanel.setBackground(new Color(200, 255, 200));
            JLabel statusLabel = new JLabel("Not Found", JLabel.CENTER);
            statusLabel.setForeground(Color.WHITE);
            statusLabel.setBackground(new Color(200, 255, 200));
            statusLabel.setOpaque(true);

            JLabel threadNumberLabel = new JLabel("Thread " + (threadIndex + 1), JLabel.CENTER);
            threadNumberLabel.setForeground(Color.BLACK);

            JPanel threadPanel = new JPanel();
            threadPanel.setLayout(new BorderLayout());
            threadPanel.add(threadNumberLabel, BorderLayout.NORTH);
            threadPanel.add(boardPanel, BorderLayout.CENTER);
            threadPanel.add(statusLabel, BorderLayout.SOUTH);

            SwingUtilities.invokeLater(() -> {
                mainPanel.add(threadPanel);
                boardPanels.add(boardPanel);
                solutionStatusLabels.add(statusLabel);
                frame.revalidate();
                frame.repaint();
            });

            threads[threadIndex] = new Thread(() -> {
                int[][] board = new int[BOARD_SIZE][BOARD_SIZE];
                for (int row = 0; row < BOARD_SIZE; row++) {
                    for (int col = 0; col < BOARD_SIZE; col++) {
                        board[row][col] = -1;
                    }
                }

                // Wait until a solution is found
                while (!solutionFound.get()) {
                    boolean solvedForThisThread = puzzleSolver.solveForThread(board, pieces, threadIndex, statusLabel, boardPanel, solutionFound, threads);

                    SwingUtilities.invokeLater(() -> {
                        if (solvedForThisThread && !solutionFound.get()) {
                            solutionFound.set(true);
                            solutionThreadIndex.set(threadIndex);
                            statusLabel.setText("Solved");
                            statusLabel.setBackground(new Color(34, 139, 34)); // Green color
                            // Show message with which thread solved the puzzle
                            JOptionPane.showMessageDialog(frame, "Thread " + (threadIndex + 1) + " solved the puzzle!");
                        } else if (!solvedForThisThread && !solutionFound.get()) {
                            statusLabel.setText("Not Found");
                            statusLabel.setBackground(new Color(255, 69, 0)); // Red color
                        }
                    });

                    // If solution is found, freeze this thread
                    if (solutionFound.get()) {
                        break;
                    }
                }
            });

            executor.submit(threads[threadIndex]);
        }
    }

    public static void main(String[] args) {
        new gui();
    }
}
