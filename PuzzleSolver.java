import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;

public class PuzzleSolver {

    private final int BOARD_SIZE = 4;
    private final int DELAY = 300;

    private final Color[] pieceColors = {
            Color.RED,
            Color.CYAN,
            Color.BLUE,
            Color.ORANGE,
            Color.YELLOW,
            Color.GREEN,
            Color.MAGENTA
    };

    public boolean solveForThread(int[][] board, List<int[][]> pieces, int threadIndex, JLabel statusLabel, JPanel boardPanel, int timeoutSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Callable<Boolean> task = () -> {
            List<int[][]> threadPieces = new ArrayList<>(pieces);
            Collections.shuffle(threadPieces);
            return backtrack(board, threadPieces, 0, statusLabel, boardPanel);
        };

        Future<Boolean> future = executor.submit(task);
        boolean result = false;

        try {
            result = future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Thread " + threadIndex + ": Not Solved (Timeout)"));
            future.cancel(true); // Attempt to interrupt the task
        } catch (InterruptedException e) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Thread " + threadIndex + ": Interrupted"));
            Thread.currentThread().interrupt(); // Preserve interrupt status
        } catch (ExecutionException e) {
            e.printStackTrace();
        } finally {
            executor.shutdownNow(); // Forcefully shutdown to ensure interruption
        }

        if (!result) {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Thread " + threadIndex + ": Not Solved"));
        } else {
            SwingUtilities.invokeLater(() -> statusLabel.setText("Thread " + threadIndex + ": Solved"));
        }

        return result;
    }

    private boolean backtrack(int[][] board, List<int[][]> pieces, int pieceIndex, JLabel statusLabel, JPanel boardPanel) throws InterruptedException {
        if (pieceIndex == pieces.size()) {
            SwingUtilities.invokeLater(() -> displayBoard(board, boardPanel, statusLabel));
            return true;
        }

        for (int rotation = 0; rotation < 4; rotation++) {
            int[][] rotatedPiece = rotatePiece(pieces.get(pieceIndex), rotation);

            for (int row = 0; row < BOARD_SIZE; row++) {
                for (int col = 0; col < BOARD_SIZE; col++) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException(); // Exit if interrupted
                    }

                    if (canPlace(board, rotatedPiece, row, col)) {
                        placePiece(board, rotatedPiece, row, col, pieceIndex + 1);
                        SwingUtilities.invokeLater(() -> displayBoard(board, boardPanel, statusLabel));

                        try {
                            Thread.sleep(DELAY);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt(); // Preserve interrupt status
                            throw e;
                        }

                        if (backtrack(board, pieces, pieceIndex + 1, statusLabel, boardPanel)) {
                            return true;
                        }
                        removePiece(board, rotatedPiece, row, col);
                        SwingUtilities.invokeLater(() -> displayBoard(board, boardPanel, statusLabel));
                    }
                }
            }
        }
        return false;
    }

    private void displayBoard(int[][] board, JPanel boardPanel, JLabel statusLabel) {
        boardPanel.removeAll();
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                JLabel cell = new JLabel("", JLabel.CENTER);
                cell.setOpaque(true);
                if (board[r][c] == -1) {
                    cell.setBackground(new Color(255, 240, 245));
                } else {
                    cell.setBackground(pieceColors[board[r][c] - 1]);
                }
                cell.setBorder(BorderFactory.createLineBorder(Color.BLACK));
                boardPanel.add(cell);
            }
        }
        boardPanel.revalidate();
        boardPanel.repaint();
    }

    private boolean canPlace(int[][] board, int[][] piece, int startRow, int startCol) {
        for (int r = 0; r < piece.length; r++) {
            for (int c = 0; c < piece[0].length; c++) {
                if (piece[r][c] == 1) {
                    int row = startRow + r, col = startCol + c;
                    if (row >= BOARD_SIZE || col >= BOARD_SIZE || board[row][col] != -1) return false;
                }
            }
        }
        return true;
    }

    private void placePiece(int[][] board, int[][] piece, int startRow, int startCol, int id) {
        for (int r = 0; r < piece.length; r++) {
            for (int c = 0; c < piece[0].length; c++) {
                if (piece[r][c] == 1) {
                    board[startRow + r][startCol + c] = id;
                }
            }
        }
    }

    private void removePiece(int[][] board, int[][] piece, int startRow, int startCol) {
        for (int r = 0; r < piece.length; r++) {
            for (int c = 0; c < piece[0].length; c++) {
                if (piece[r][c] == 1) {
                    board[startRow + r][startCol + c] = -1;
                }
            }
        }
    }

    private int[][] rotatePiece(int[][] piece, int times) {
        int[][] rotated = piece;
        for (int i = 0; i < times; i++) {
            rotated = rotateOnce(rotated);
        }
        return rotated;
    }

    private int[][] rotateOnce(int[][] piece) {
        int rows = piece.length, cols = piece[0].length;
        int[][] rotated = new int[cols][rows];
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                rotated[c][rows - 1 - r] = piece[r][c];
            }
        }
        return rotated;
    }
}

