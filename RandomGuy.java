package ReversiRandom_Java;

import javafx.util.Pair;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Random;

class Node{
    private int nodeID;
    private int utility;

    public Node(int id, int util){
        nodeID = id;
        utility = util;
    }

    public void setNodeID(int id){
        nodeID = id;
    }
    public void setNodeUtil(int util){
        utility = util;
    }
    public int getNodeID(){
        return nodeID;
    }
    public int getNodeUtil(){
        return utility;
    }
}

class RandomGuy {

    private BufferedReader sin;
    private PrintWriter sout;
    private Random generator = new Random();

    private int me;
    private int them;
    private int[][] state = new int[8][8]; // state[0][0] is the bottom left corner of the board (on the GUI)
    private int turn = -1;
    private int round;

    private int[] validMoves = new int[64];
    int numValidMoves;
    int[][] boardValues = new int[][]{
            {99, -8, 8, 6, 6, 8, -8, 99},
            {-8, -24, -4, -3, -3, -4, -24, 0},
            {8, -4, 7, 4, 4, 7, -4, 8},
            {6, -3, 4, 0, 0, 4, -3, 6},
            {6, -3, 4, 0, 0, 4, -3, 6},
            {8, -4, 7, 4, 4, 7, -4, 8},
            {-8, -24, -4, -3, -3, -4, -24, 0},
            {99, -8, 8, 6, 6, 8, -8, 99},
    };


    // main function that (1) establishes a connection with the server, and then plays whenever it is this player's turn
    public RandomGuy(int _me, String host) {
        me = _me;
        if (_me == 1) {
            them = 2;
        } else {
            them = 1;
        }
        initClient(host);

        int myMove;

        while (true) {
            readMessage();

            if (turn == me) {
                validMoves = getValidMoves(round, state, me);
                myMove = move();
                String sel = validMoves[myMove] / 8 + "\n" + validMoves[myMove] % 8;
                // System.out.println("Determined that the best move is at square: " + validMoves[myMove]);
                sout.println(sel);
            }
        }
    }

    private int move() {
        if (round < 4) {
            return this.generator.nextInt(this.numValidMoves);
        } else {
            return findBestMove(true, getValidMoves(round, state, me), getValidMoves(round, state, them), 0, state, round, Integer.MIN_VALUE, Integer.MAX_VALUE, validMoves[0]).getNodeID();
        }
    }

    private Node findBestMove(boolean isMax, int[] validMoves, int[] otherPlayersValidMoves, int curRecursionLevel, int[][] currentState, int curRound, int alpha, int beta, int potentialMove) {
        // Check if we are at the end of the tree or at our max recursion level.
        curRecursionLevel += 1;
//		System.out.println("recursion level: " + curRecursionLevel + "current nodes: " + validMoves[0] + " : " + validMoves[1] + " : " + validMoves[2]);
//		String tabs = "";
//		for(int j = 0; j < curRecursionLevel; j++){
//			tabs = tabs + "\t|";
//		}
//		System.out.println(tabs + "Starting new recursion:");
//		System.out.println(tabs + "curRecursionLevel: " + curRecursionLevel);

        if ((getNumValidMoves(validMoves) == 0) || curRecursionLevel == 10) {
            //			System.out.println(tabs + "Arrived at a leaf node. returning utility of: " + util);
            return new Node(-1, determineStateUtility(currentState, potentialMove, validMoves, otherPlayersValidMoves));
        }
        if (isMax) {
            // The first item in the pair is the index and the second item is the utility.
            Node value = new Node(-1, Integer.MIN_VALUE);
//			System.out.println(tabs + "It's randomGuy's turn. Looping through possible valid moves:");
//			int numMoves = getNumValidMoves(validMoves);
//			for(int k = 0; k < numMoves; k++) {
//				System.out.println(tabs + validMoves[k]);
//			}
            int numMoves = getNumValidMoves(validMoves);
            for (int i = 0; i < numMoves; i++) {
//				System.out.println(tabs + "Exploring randmonGuy's move at: " + validMoves[i]);
                currentState[validMoves[i] / 8][validMoves[i] % 8] = me;
//				System.out.println(tabs + "Making another recursive call.");
                Node tempValue = findBestMove(false, getValidMoves(curRound + 1, currentState, them), getValidMoves(curRound + 1, currentState, me), curRecursionLevel, currentState, curRound + 1, alpha, beta, validMoves[i]);
                tempValue.setNodeID(i);
                currentState[validMoves[i] / 8][validMoves[i] % 8] = 0;
//				System.out.println(tabs + "Recursion returned. Move " + validMoves[i] + " has a utility of " + tempValue.getValue());
//				System.out.println(tabs + "^^^PROBLEM! We return the *index* not the utility. So move " + validMoves[i] + " gets assigned the index of the best move and not the returned utility. So we can't compare it with the 'value' variable");
                if(tempValue.getNodeUtil() > value.getNodeUtil()){
                    value = tempValue;
                }
                alpha = Math.max(alpha, value.getNodeUtil());
                if (alpha >= beta) {
                    // System.out.println(tabs + "Future branches deemed obsolete through pruning. Break called to stop exploring them");
                    break;
                }
            }
            // System.out.println(tabs + "After the alpha-beta pruning, returning utility of: " + value.getNodeUtil() + " associated with square: " + validMoves[value.getNodeID()]);

            return value;
        } else {
            // The first item in the pair is the index and the second item is the utility.
//			System.out.println(tabs + "It's human's turn. Looping through possible valid moves:");
// 			int numMoves = getNumValidMoves(validMoves);
// 			for (int k = 0; k < numMoves; k++) {
// //				System.out.println(tabs + validMoves[k]);
// 			}
            // getValidMoves(round, currentState, them);
            Node value = new Node(-1, Integer.MAX_VALUE);
            int numMoves =  getNumValidMoves(validMoves);
            for (int i = 0; i < numMoves; i++) {
//				System.out.println(tabs + "Exploring human's move at: " + validMoves[i]);
                currentState[validMoves[i] / 8][validMoves[i] % 8] = them;
//				System.out.println(tabs + "Making another recursive call.");
                Node tempValue = findBestMove(true, getValidMoves(curRound + 1, currentState, me), getValidMoves(curRound + 1, currentState, them), curRecursionLevel, currentState, curRound + 1, alpha, beta, validMoves[i]);
                tempValue.setNodeID(i);
                currentState[validMoves[i] / 8][validMoves[i] % 8] = 0;
//				System.out.println(tabs + "Recursion returned. Move " + validMoves[i] + " has a utility of " + tempValue.getValue());
//				System.out.println(tabs + "^^^PROBLEM! We return the *index* not the utility. So move " + validMoves[i] + " gets assigned the index of the best move and not the returned utility. So we can't compare it with the 'value' variable");
                if(tempValue.getNodeUtil() < value.getNodeUtil()){
                    value = tempValue;
                }
                beta = Math.min(beta, value.getNodeUtil());
                if (alpha >= beta) {
                    // System.out.println(tabs + "Future branches deemed obsolete through pruning. Break called to stop exploring them");
                    break;
                }
            }
            // System.out.println(tabs + "After the alpha-beta pruning, returning utility of: " + value.getNodeUtil() + " associated with square: " + validMoves[value.getNodeID()]);
            return value;
        }
    }

    private int determineStateUtility(int[][] currState, int potentialMove, int[] validMoves, int[] otherPlayerValidMoves) {
        int numNonZerosPlayer = getNumValidMoves(validMoves);
        int numZerosOtherPlayer = otherPlayerValidMoves.length - getNumValidMoves(otherPlayerValidMoves);
        return (numNonZerosPlayer) + (numZerosOtherPlayer) ;//+ boardValues[potentialMove / 8][potentialMove % 8];
    }

    private int getNumValidMoves(int[] validMoves) {
        int countValidMoves;
        // first check if the number of valid moves is 0:
        if (validMoves[0] == 0 && validMoves[1] == 0) {
            return 0;
        } else {
            //if it gets here, we know there's at least one valid move in index 0
            countValidMoves = 1;
            //loop through the list to count the number of non-zero items
            int len = validMoves.length;    //this is a (small) optimization so it doesn't have to get recalculated all the time
            //start at index 1
            for (int i = 1; i < len; i++) {
                if (validMoves[i] != 0) {
                    countValidMoves++;
                }
                else{
                    // having this here will speed things up slightly so it doesn't keep looping through values we know are zero
                    break;
                }
            }
        }
        return countValidMoves;

    }

    // generates the set of valid moves for the player; returns a list of valid moves (validMoves)
    private int[] getValidMoves(int round, int[][] state, int player) {
        int i, j;

        int[] newValidMoves = new int[64];
        numValidMoves = 0;
        if (round < 4) {
            if (state[3][3] == 0) {
                newValidMoves[numValidMoves] = 3 * 8 + 3;
                numValidMoves++;
            }
            if (state[3][4] == 0) {
                newValidMoves[numValidMoves] = 3 * 8 + 4;
                numValidMoves++;
            }
            if (state[4][3] == 0) {
                newValidMoves[numValidMoves] = 4 * 8 + 3;
                numValidMoves++;
            }
            if (state[4][4] == 0) {
                newValidMoves[numValidMoves] = 4 * 8 + 4;
                numValidMoves++;
            }
//			System.out.println("Valid Moves:");
            for (i = 0; i < numValidMoves; i++) {
//				System.out.println(newValidMoves[i] / 8 + ", " + newValidMoves[i] % 8);
            }
        } else {
//			System.out.println("Valid Moves:");
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    if (state[i][j] == 0) {
                        if (couldBe(state, i, j, player)) {
                            newValidMoves[numValidMoves] = i * 8 + j;
                            numValidMoves++;
//							System.out.println(i + ", " + j);
                        }
                    }
                }
            }
        }
        return newValidMoves;
    }

    private boolean checkDirection(int[][] state, int row, int col, int incx, int incy, int me) {
        int[] sequence = new int[7];
        int seqLen;
        int i, r, c;

        seqLen = 0;
        for (i = 1; i < 8; i++) {
            r = row + incy * i;
            c = col + incx * i;

            if ((r < 0) || (r > 7) || (c < 0) || (c > 7))
                break;

            sequence[seqLen] = state[r][c];
            seqLen++;
        }

        int count = 0;
        for (i = 0; i < seqLen; i++) {
            if (me == 1) {
                if (sequence[i] == 2)
                    count++;
                else {
                    if ((sequence[i] == 1) && (count > 0))
                        return true;
                    break;
                }
            } else {
                if (sequence[i] == 1)
                    count++;
                else {
                    if ((sequence[i] == 2) && (count > 0))
                        return true;
                    break;
                }
            }
        }

        return false;
    }

    private boolean couldBe(int[][] state, int row, int col, int player) {
        int incx, incy;

        for (incx = -1; incx < 2; incx++) {
            for (incy = -1; incy < 2; incy++) {
                if ((incx == 0) && (incy == 0))
                    continue;

                if (checkDirection(state, row, col, incx, incy, player))
                    return true;
            }
        }

        return false;
    }

    private void readMessage() {
        int i, j;
        try {
            //System.out.println("Ready to read again");
            turn = Integer.parseInt(sin.readLine());

            if (turn == -999) {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
//					System.out.println(e);
                }

                System.exit(1);
            }

            //System.out.println("Turn: " + turn);
            round = Integer.parseInt(sin.readLine());
            double t1 = Double.parseDouble(sin.readLine());
//			System.out.println(t1);
            double t2 = Double.parseDouble(sin.readLine());
//			System.out.println(t2);
            for (i = 0; i < 8; i++) {
                for (j = 0; j < 8; j++) {
                    state[i][j] = Integer.parseInt(sin.readLine());
                }
            }
            sin.readLine();
        } catch (IOException e) {
//			System.err.println("Caught IOException: " + e.getMessage());
        }

//		System.out.println("Turn: " + turn);
//		System.out.println("Round: " + round);
        for (i = 7; i >= 0; i--) {
            for (j = 0; j < 8; j++) {
//				System.out.print(state[i][j]);
            }
//			System.out.println();
        }
//		System.out.println();
    }

    private void initClient(String host) {
        int portNumber = 3333 + me;

        try {
            Socket s = new Socket(host, portNumber);
            sout = new PrintWriter(s.getOutputStream(), true);
            sin = new BufferedReader(new InputStreamReader(s.getInputStream()));

            String info = sin.readLine();
//			System.out.println(info);
        } catch (IOException e) {
//			System.err.println("Caught IOException: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new RandomGuy(Integer.parseInt(args[1]), args[0]);
    }

}
