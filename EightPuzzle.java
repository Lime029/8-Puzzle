import java.util.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.concurrent.CancellationException;

public class EightPuzzle {

    private State currentState; // Current state of this EightPuzzle
    private int maxStates; // Maximum number of nodes that can be expanded
    private long randomSeed = 0; // Seed used in randomizeState method (starts at 0 for each new EightPuzzle object)

    public EightPuzzle() {
        currentState = new State("b12345678",0,null,null); // Initial goal state
        maxStates = Integer.MAX_VALUE;
    }

    // Reads and executes commands from specified text file
    public void readCommands(String filepath) throws Exception {
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        // Check commands line by line and parse into corresponding method calls
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            try {
                if (line.startsWith("setState")) setState(line.substring(9));
                else if (line.equals("printState")) printState(currentState);
                else if (line.startsWith("move")) currentState = getMoveState(currentState, line.substring(5));
                else if (line.startsWith("randomizeState")) randomizeState(Integer.parseInt(line.substring(15)));
                else if (line.startsWith("solve A-star")) solveAStar(line.substring(13));
                else if (line.startsWith("solve beam")) solveLocalBeam(Integer.parseInt(line.substring(11)));
                else if (line.startsWith("maxNodes")) maxStates = Integer.parseInt(line.substring(9));
                else throw new IllegalArgumentException();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Sets state of board to specified state
    private void setState(String stateStr) {
        // Check if provided String is a valid representation of a state
        if (stateStr.length() == 11) stateStr = stateStr.substring(0,3) + stateStr.substring(4,7) + stateStr.substring(8,11);
        if (stateStr.length() == 9) {
            Queue<Character> tiles = new LinkedList<>();
            tiles.add('1');
            tiles.add('2');
            tiles.add('3');
            tiles.add('4');
            tiles.add('5');
            tiles.add('6');
            tiles.add('7');
            tiles.add('8');
            int newBlank = stateStr.indexOf('b'); // Blank index of input
            if (newBlank == -1) throw new IllegalArgumentException();
            while (!tiles.isEmpty()) {
                if (stateStr.indexOf(tiles.remove()) == -1) throw new IllegalArgumentException();
            }
            currentState = new State(stateStr,0,null,null);
        } else throw new IllegalArgumentException();
    }

    // Prints specified state in matrix form
    private static void printState(State s) {
        System.out.printf("%s\n", s.stateStr.substring(0, 3));
        System.out.printf("%s\n", s.stateStr.substring(3, 6));
        System.out.printf("%s\n", s.stateStr.substring(6, 9));
        System.out.println();
    }

    // Returns State that would result from moving the blank tile in the specified direction
    private State getMoveState(State s, String direction) {
        // Generalize direction indices
        int dirOffset;
        switch (direction) {
            case "up" -> {
                if (s.blankIndex < 3) throw new UnsupportedOperationException();
                dirOffset = -3;
            }
            case "down" -> {
                if (s.blankIndex >= 6) throw new UnsupportedOperationException();
                dirOffset = 3;
            }
            case "left" -> {
                if (s.blankIndex % 3 == 0) throw new UnsupportedOperationException();
                dirOffset = -1;
            }
            case "right" -> {
                if ((s.blankIndex + 1) % 3 == 0) throw new UnsupportedOperationException();
                dirOffset = 1;
            }
            default -> throw new IllegalArgumentException();
        }
        // Swap tiles
        int from = s.blankIndex;
        int to = s.blankIndex + dirOffset;
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < 9; i++) {
            if (i == from) temp.append(s.stateStr.charAt(to));
            else if (i == to) temp.append('b');
            else temp.append(s.stateStr.charAt(i));
        }
        return new State(temp.toString(),s.functionValue,s,direction); // Accounts for new previous state/move
    }

    // Randomizes state of 8-puzzle
    private void randomizeState(int n) {
        if (n < 0) throw new IllegalArgumentException();
        // Reset to goal state
        currentState = new State("b12345678",0,null,null);
        // Perform n moves
        Random rand = new Random(randomSeed++); // Have same order have same random values, but not each in the order
        for (int i = 0; i < n; i++) {
            ArrayList<String> legalMoves = new ArrayList<>(4);
            legalMoves.add("up");
            legalMoves.add("down");
            legalMoves.add("left");
            legalMoves.add("right");
            boolean found = false;
            while (!found) {
                int dirIndex = rand.nextInt(legalMoves.size());
                String direction = legalMoves.get(dirIndex);
                try {
                    currentState = getMoveState(currentState,direction);
                    found = true; // Only happens if no error from trying invalid move above
                } catch (UnsupportedOperationException e) {
                    legalMoves.remove(dirIndex); // Retry with another direction if move was invalid
                }
            }
        }
    }

    // Prints solution to 8-puzzle using A-Star and returns the solution length
    private int solveAStar(String heuristic) {
        System.out.println("Solving A-star with starting state " + currentState.stateStr + " and heuristic " + heuristic + ":");
        PriorityQueue<State> frontier = new PriorityQueue<>(); // Ordered by ascending functionValue
        HashSet<String> explored = new HashSet<>(); // States that have already been explored (String representation)
        setState(currentState.stateStr); // Resets functionValue and previous references to prepare for new search
        currentState.functionValue = h2(currentState.stateStr); // h2 value is nonzero, so set it
        frontier.add(currentState); // Initial state
        int nodesGenerated = 0;
        if (++nodesGenerated > maxStates) throw new CancellationException("Exceeded max node generation limit");
        State goal = null;
        while(!frontier.isEmpty()) {
            State s = frontier.remove(); // Lowest functionValue node that hasn't been explored yet
            if (explored.contains(s.stateStr)) continue; // s was added to frontier before better path to it was found
            explored.add(s.stateStr); // Finalize s
            if (s.stateStr.equals("b12345678")) { // Found goal
                goal = s;
                break;
            }
            // Add neighboring states that haven't been explored to frontier
            List<State> neighborStates = s.getNextStates(heuristic);
            for (State neighbor : neighborStates) {
                if (++nodesGenerated > maxStates) throw new CancellationException("Exceeded max node generation limit");
                if (!explored.contains(neighbor.stateStr)) frontier.add(neighbor);
            }
        }
        if (goal == null) throw new CancellationException("Unreachable goal state, having considered " + nodesGenerated + " nodes");
        currentState = goal; // Solves the puzzle by setting it to goal state
        int numMoves = printMovePath(goal);
        System.out.printf("Puzzle successfully solved in %d moves, considering %d nodes\n\n",numMoves,nodesGenerated);
        return numMoves;
    }

    // Evaluates h1 heuristic for input state configuration
    private static int h1(String stateStr) {
        int misplaced = 0;
        char[] tiles = {'b','1','2','3','4','5','6','7','8'};
        for (int i = 1; i < 9; i++) { // Start at 1 so blank tile isn't counted
            if (stateStr.charAt(i) != tiles[i]) misplaced++; // ith tile is different from solution
        }
        return misplaced;
    }

    // Evaluates h2 heuristic for input state configuration
    private static int h2(String stateStr) {
        int distanceSum = 0;
        // Calculate Manhattan distance for each tile
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                int temp = stateStr.charAt(i*3+j) - '0'; // Iterates through characters in stateStr one by one
                if (temp != 'b' - '0') { // 'b' is not a tile
                    distanceSum += Math.abs(temp / 3 - i); // Vertical displacement
                    distanceSum += Math.abs(temp % 3 - j); // Horizontal displacement
                }
            }
        }
        return distanceSum;
    }

    // Prints solution to 8-puzzle using Local Beam Search and returns the solution length
    private int solveLocalBeam(int k) {
        System.out.println("Solving Local Beam with starting state " + currentState.stateStr + " and k = " + k + ":");
        List<State> bestStates = new ArrayList<>(k); // List to keep track of best k states (or fewer if not enough)
        HashSet<String> explored = new HashSet<>(); // States that have already been explored (String representation)
        setState(currentState.stateStr); // Resets functionValue and previous references to prepare for new search
        currentState.functionValue = h2(currentState.stateStr); // Need to set it for comparison to see if local minimum
        bestStates.add(currentState); // Initial state
        int nodesGenerated = 0;
        if (++nodesGenerated > maxStates) throw new CancellationException("Exceeded max node generation limit");
        State goal = null;
        while(!bestStates.isEmpty()) {
            PriorityQueue<State> nextStates = new PriorityQueue<>(); // Keeps track of next possible by ascending h2
            for (State s : bestStates) {
                explored.add(s.stateStr); // Finalize s
                if (s.stateStr.equals("b12345678")) { // Found goal
                    goal = s;
                    break;
                }
                // Add all possible next states to nextStates
                List<State> temp = s.getNextStates("lb");
                for (State next : temp) {
                    if (++nodesGenerated > maxStates) throw new CancellationException("Exceeded max node generation limit");
                    if (!explored.contains(next.stateStr) && next.functionValue <= s.functionValue) nextStates.add(next);
                }
            }
            // Replace bestStates with k most optimal states
            bestStates.clear();
            for (int i = 0; i < k && !nextStates.isEmpty(); i++) {
                bestStates.add(nextStates.remove());
            }
        }
        if (goal == null) throw new CancellationException("Goal state not found, considering " + nodesGenerated + " nodes");
        currentState = goal; // Solves the puzzle by setting it to goal state
        int numMoves = printMovePath(goal);
        System.out.printf("Puzzle successfully solved in %d moves, considering %d nodes\n\n",numMoves,nodesGenerated);
        return numMoves;
    }

    // Prints sequence of moves taken to reach input goal state and returns number of such moves
    private static int printMovePath(State goal) {
        // Get path by backtracking
        Stack<String> moves = new Stack<>();
        State pointer = goal;
        while (pointer.prevState != null) {
            moves.push(pointer.prevMove);
            pointer = pointer.prevState;
        }
        int numMoves = 0;
        // Print out path
        while (!moves.isEmpty()) {
            System.out.println(moves.pop());
            numMoves++;
        }
        return numMoves;
    }

    // Package declaration to allow for testing
    class State implements Comparable<State>{

        private int functionValue; // f(n) for state
        private String stateStr; // String configuration of state
        private State prevState; // Previous state, used to get solution path
        private String prevMove; // Direction used to get from prevState to this state
        private int blankIndex; // Index of blank tile

        private State(String stateStr, int functionValue, State parentState, String prevDirection) {
            this.functionValue = functionValue;
            this.stateStr = stateStr;
            prevState = parentState;
            prevMove = prevDirection;
            for (int i = 0; i < 9; i++) {
                if (stateStr.charAt(i) == 'b') blankIndex = i;
            }
        }

        // Used for Priority Queue ordering
        public int compareTo(State s) {
            return this.functionValue - s.functionValue;
        }

        // Returns possible new States that can result from a single move from this State
        private List<State> getNextStates(String heuristic) {
            List<State> states = new LinkedList<>();
            for (String direction : new String[]{"up","down","left","right"}) {
                try {
                    State temp = getMoveState(this, direction);
                    int newFunctionValue;
                    switch (heuristic) {
                        case "h1" -> newFunctionValue = functionValue - h1(stateStr) + 1 + h1(temp.stateStr);
                        case "h2" -> newFunctionValue = functionValue - h2(stateStr) + 1 + h2(temp.stateStr);
                        case "lb" -> newFunctionValue = h2(temp.stateStr);
                        default -> throw new IllegalArgumentException();
                    }
                    temp.functionValue = newFunctionValue;
                    states.add(temp);
                } catch (UnsupportedOperationException ignored) {} // Cannot move in this direction
            }
            return states;
        }
    }
}