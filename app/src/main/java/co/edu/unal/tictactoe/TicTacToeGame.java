package co.edu.unal.tictactoe;

/* TicTacToeConsole.java
 * By Frank McCown (Harding University)
 *
 * This is a tic-tac-toe game that runs in the console window.  The human
 * is X and the computer is O.
 */

import java.util.InputMismatchException;
import java.util.Random;
import java.util.Scanner;

public class TicTacToeGame {

    // The computer's difficulty levels
    public enum DifficultyLevel {
        Easy,
        Harder,
        Expert
    }

    // Current difficulty level
    private DifficultyLevel mDifficultyLevel = DifficultyLevel.Expert;

    public static final char HUMAN_PLAYER = 'X';
    public static final char COMPUTER_PLAYER = 'O';
    public static final char OPEN_SPOT = ' ';
    private char mBoard[] = {
            OPEN_SPOT, OPEN_SPOT, OPEN_SPOT,
            OPEN_SPOT, OPEN_SPOT, OPEN_SPOT,
            OPEN_SPOT, OPEN_SPOT, OPEN_SPOT
    };

    private final int BOARD_SIZE = 9;

    private Random mRand;

    public TicTacToeGame() {

        // Seed the random number generator
        mRand = new Random();

    }

    public DifficultyLevel getDifficultyLevel() {
        return mDifficultyLevel;
    }

    public void setDifficultyLevel(DifficultyLevel difficultyLevel) {
        mDifficultyLevel = difficultyLevel;
    }

    //para usarlo en el activity seria
    //mGame.setDifficultyLevel(TicTacToeGame.DifficultyLevel.Easy);


    // Check for a winner.  Return
    //  0 if no winner or tie yet
    //  1 if it's a tie
    //  2 if X won
    //  3 if O won
    public int checkForWinner() {

        // Check horizontal wins
        for (int i = 0; i <= 6; i += 3)	{
            if (mBoard[i] == HUMAN_PLAYER &&
                    mBoard[i+1] == HUMAN_PLAYER &&
                    mBoard[i+2]== HUMAN_PLAYER)
                return 2;
            if (mBoard[i] == COMPUTER_PLAYER &&
                    mBoard[i+1]== COMPUTER_PLAYER &&
                    mBoard[i+2] == COMPUTER_PLAYER)
                return 3;
        }

        // Check vertical wins
        for (int i = 0; i <= 2; i++) {
            if (mBoard[i] == HUMAN_PLAYER &&
                    mBoard[i+3] == HUMAN_PLAYER &&
                    mBoard[i+6]== HUMAN_PLAYER)
                return 2;
            if (mBoard[i] == COMPUTER_PLAYER &&
                    mBoard[i+3] == COMPUTER_PLAYER &&
                    mBoard[i+6]== COMPUTER_PLAYER)
                return 3;
        }

        // Check for diagonal wins
        if ((mBoard[0] == HUMAN_PLAYER &&
                mBoard[4] == HUMAN_PLAYER &&
                mBoard[8] == HUMAN_PLAYER) ||
                (mBoard[2] == HUMAN_PLAYER &&
                        mBoard[4] == HUMAN_PLAYER &&
                        mBoard[6] == HUMAN_PLAYER))
            return 2;
        if ((mBoard[0] == COMPUTER_PLAYER &&
                mBoard[4] == COMPUTER_PLAYER &&
                mBoard[8] == COMPUTER_PLAYER) ||
                (mBoard[2] == COMPUTER_PLAYER &&
                        mBoard[4] == COMPUTER_PLAYER &&
                        mBoard[6] == COMPUTER_PLAYER))
            return 3;

        // Check for tie
        for (int i = 0; i < BOARD_SIZE; i++) {
            // If we find a number, then no one has won yet
            if (mBoard[i] != HUMAN_PLAYER && mBoard[i] != COMPUTER_PLAYER)
                return 0;
        }

        // If we make it through the previous loop, all places are taken, so it's a tie
        return 1;
    }

    public void clearBoard() {
        for (int i = 0; i < BOARD_SIZE; i++) {
            mBoard[i] = OPEN_SPOT;
        }
    }
    public void setMove(char player, int location) {
        if (mBoard[location] == OPEN_SPOT)
            mBoard[location] = player;
    }

    public int getComputerMove() {
        int move = -1;

        if (mDifficultyLevel == DifficultyLevel.Easy) {
            move = getRandomMove();
        }
        else if (mDifficultyLevel == DifficultyLevel.Harder) {
            move = getWinningMove();

            if (move == -1)
                move = getRandomMove();
        }
        else if (mDifficultyLevel == DifficultyLevel.Expert) {
            // Try to win, but if that's not possible, block.
            // If that's not possible, move anywhere.
            move = getWinningMove();

            if (move == -1)
                move = getBlockingMove();

            if (move == -1)
                move = getRandomMove();
        }

        return move;
    }

    private int getRandomMove() {
        int move;

        do {
            move = mRand.nextInt(BOARD_SIZE);
        } while (mBoard[move] != OPEN_SPOT);

        return move;
    }

    private int getWinningMove() {

        for (int i = 0; i < BOARD_SIZE; i++) {

            if (mBoard[i] == OPEN_SPOT) {

                char curr = mBoard[i];

                // Temporarily place O
                mBoard[i] = COMPUTER_PLAYER;

                if (checkForWinner() == 3) {
                    // Restore the board before returning
                    mBoard[i] = curr;
                    return i;
                }

                // Restore the board
                mBoard[i] = curr;
            }
        }

        return -1;
    }

    private int getBlockingMove() {

        for (int i = 0; i < BOARD_SIZE; i++) {

            if (mBoard[i] == OPEN_SPOT) {

                char curr = mBoard[i];

                // Temporarily place X
                mBoard[i] = HUMAN_PLAYER;

                if (checkForWinner() == 2) {
                    // Restore the board before returning
                    mBoard[i] = curr;
                    return i;
                }

                // Restore the board
                mBoard[i] = curr;
            }
        }

        return -1;
    }

}