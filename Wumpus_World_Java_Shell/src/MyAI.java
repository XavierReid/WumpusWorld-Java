
// ======================================================================
// FILE:        MyAI.java
//
// AUTHOR:      Abdullah Younis
//
// DESCRIPTION: This file contains your agent class, which you will
//              implement. You are responsible for implementing the
//              'getAction' function and any helper methods you feel you
//              need.
//
// NOTES:       - If you are having trouble understanding how the shell
//                works, look at the other parts of the code, as well as
//                the documentation.
//
//              - You are only allowed to make changes to this portion of
//                the code. Any changes to other portions of the code will
//                be lost when the tournament runs your code.
// ======================================================================
import java.util.*;
import java.awt.Point;

public class MyAI extends Agent {
    private Point currentPosition, focus;
    double xLimit, yLimit;
    private boolean goHome, hasArrow, wumpusRemoved; // go home if no safe options, found gold, or all safe are visited
    private int score, direction, stenches;
    private Map<Point, boolean[]> worldMap;
    private Queue<Action> toExecute; // Actions we will take
    private Set<Point> safePoints;

    public MyAI() {
        // ======================================================================
        // YOUR CODE BEGINS
        // ======================================================================
        this.xLimit = -1;
        this.yLimit = -1;
        this.currentPosition = new Point();
        this.focus = null;
        this.score = 0;
        this.stenches = 0;
        this.direction = 2; // 1 = N, 2 = E, 3 = S, 4 = W
        this.goHome = false;
        this.hasArrow = true;
        this.wumpusRemoved = false;
        this.worldMap = new HashMap<>();
        this.toExecute = new LinkedList<>();
        this.safePoints = new HashSet<>();
        /*
         * To make sure a spot that could have a wumpus or pit really has it, check its
         * adjacent spots if one of them has been visited but doesn't have the breeze or
         * stench then it's safe
         */
        // ======================================================================
        // YOUR CODE ENDS
        // ======================================================================
    }

    public Action getAction(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        // ======================================================================
        // YOUR CODE BEGINS
        // ======================================================================
        updateWorldMap(stench, breeze, glitter, bump, scream);
        // printInfo();
        if (scream && !this.wumpusRemoved) {
            deadWumpus();
            this.wumpusRemoved = true;
        }
        Set<Point> options = mapOutArea();
        if (glitter) {
            this.toExecute.add(Action.GRAB);
            this.goHome = true;
            this.focus = new Point();
        }

        if (stench && !breeze && this.hasArrow && !this.goHome) {
            shootYourShot();
            this.hasArrow = false;
        }

        if (this.toExecute.peek() == Action.SHOOT) {
            Point deadWumpus = faceTheWumpus();
            this.worldMap.get(deadWumpus)[7] = false;
            this.worldMap.get(deadWumpus)[1] = false;
        }

        if (toExecute.isEmpty()) {
            getDecision(options);
        }
        // System.out.println(this.toExecute);
        return doAction();
        // ======================================================================
        // YOUR CODE ENDS
        // ======================================================================
    }
    // ======================================================================
    // YOUR CODE BEGINS
    // ======================================================================

    /* Getter Functions */
    private void getDecision(Set<Point> options) {
        if (this.focus == null) {
            if (!this.goHome && !options.isEmpty() && !allVisited(this.safePoints)) {
                // not going home
                if (allVisited(options)) {
                    // all adj options explored
                    this.focus = findUnvisitedSafe();
                    Point nextMove = moveTowardFocus(this.currentPosition, this.focus);
                    moveToBestOption(nextMove);
                } else {
                    // unexplored options
                    Point best = getBestOption(options);
                    moveToBestOption(best);
                }
            } else {
                // going home
                this.goHome = true;
                this.focus = new Point();
                Point next = moveTowardFocus(this.currentPosition, this.focus);
                moveToBestOption(next);
            }
        } else {
            // going toward focus;
            if (!this.currentPosition.equals(this.focus)) {
                if (!this.goHome) {
                    this.focus = findUnvisitedSafe();
                }
                Point next = moveTowardFocus(this.currentPosition, this.focus);
                moveToBestOption(next);
            } else {
                this.focus = null;
                if (this.goHome) {
                    // if the focus was home
                    this.toExecute.add(Action.CLIMB);
                } else {
                    // if the focus wasn't home
                    getDecision(options);
                }
            }
        }
    }

    private String getPosition(Point position) {
        if (position == null) {
            return "none";
        }
        return "(" + (int) position.getX() + ", " + (int) position.getY() + ")";
    }

    private String getDirection() {
        switch (this.direction) {
        case 1:
            return "Up";
        case 2:
            return "Right";
        case 3:
            return "Down";
        case 4:
            return "Left";
        default:
            return "ERROR";
        }
    }

    private Point getBestOption(Set<Point> options) {
        Point best = null;
        int min = Integer.MAX_VALUE;
        for (Point p : options) {
            int score = 0;
            if (beenThere(p)) {
                score += 3;
            }
            score += actionsRequired(this.currentPosition, p);
            if (score < min) {
                min = score;
                best = p;
            }
        }
        return best;
    }

    /* Book-keeping Functions */

    private void clearStenches(Point deadWumpus) {
        Point[] stenches = surroundingPoints(deadWumpus);
        for (Point stench : stenches) {
            if (!outOfBounds(stench)) {
                if (this.worldMap.containsKey(stench)) {
                    this.worldMap.get(stench)[1] = false;
                }
            }
        }
    }

    private Point faceTheWumpus() {
        int x = (int) this.currentPosition.getX();
        int y = (int) this.currentPosition.getY();
        switch (this.direction) {
        case 1:
            return new Point(x, y + 1);
        case 2:
            return new Point(x + 1, y);
        case 3:
            return new Point(x, y - 1);
        case 4:
            return new Point(x - 1, y);
        }
        return null;
    }

    private void deadWumpus() {
        Point deadWumpus = faceTheWumpus();
        clearStenches(deadWumpus);
    }

    private Set<Point> mapOutArea() {
        Set<Point> whereToMoveNext = new HashSet<Point>();
        Point[] surroundings = surroundingPoints(this.currentPosition);
        boolean[] currSensors = this.worldMap.get(this.currentPosition);
        // System.out.println(printSensors(currSensors));
        for (Point sur : surroundings) {
            if (!outOfBounds(sur)) {
                if (!this.worldMap.containsKey(sur)) {
                    boolean wumpus = true;
                    if (this.stenches > 1) {
                        wumpus = false;
                    }
                    this.worldMap.put(sur, new boolean[] { false, false, false, false, false, false, true, wumpus });
                }
                if (!this.worldMap.get(sur)[0]) {
                    // if current tile is clear - turn surrounding wumpuses and pits off
                    if (!currSensors[1] && !currSensors[2]) {
                        this.worldMap.get(sur)[6] = false;
                        this.worldMap.get(sur)[7] = false;
                    }
                    // just a breeze - turn surrounding wumpus off
                    if (currSensors[2] && !currSensors[1]) {
                        this.worldMap.get(sur)[7] = false;
                    }
                    // just a stench - turn surrounding pits off
                    if (currSensors[1] && !currSensors[2]) {
                        this.worldMap.get(sur)[6] = false;
                    }
                }
                boolean[] sensors = this.worldMap.get(sur);
                if (!sensors[6] && !sensors[7]) {
                    safePoints.add(sur);
                    whereToMoveNext.add(sur);
                }
            }
        }
        return whereToMoveNext;
    }

    /*
     * change so it only takes current position index 0: visited index 1: stench
     * index 2: breeze index 3: glitter index 4: bump index 5: scream index 6: pit
     * index 7: wumpus
     */

    private void updateWorldMap(boolean stench, boolean breeze, boolean glitter, boolean bump, boolean scream) {
        int x = (int) this.currentPosition.getX();
        int y = (int) this.currentPosition.getY();
        if (this.wumpusRemoved) {
            this.worldMap.put(new Point(x, y),
                    new boolean[] { true, false, breeze, glitter, bump, scream, false, false });
        } else {
            if (stench) {
                this.stenches++;
            }
            this.worldMap.put(new Point(x, y),
                    new boolean[] { true, stench, breeze, glitter, bump, scream, false, false });
        }
        fixPosition();

    }

    private Point updatePosition() {
        double x = this.currentPosition.getX();
        double y = this.currentPosition.getY();
        switch (this.direction) {
        case 1: // North
            y++;
            break;
        case 2: // East
            x++;
            break;
        case 3: // South
            y--;
            break;
        case 4: // West
            x--;
            break;
        }
        this.currentPosition.setLocation(x, y);
        return this.currentPosition;
    }

    private void fixPosition() {
        double x = this.currentPosition.getX();
        double y = this.currentPosition.getY();
        if (this.worldMap.get(this.currentPosition)[4]) {
            if (x < 0) {
                x = 0;
            } else if (y < 0) {
                y = 0;
            } else if (this.direction == 2) {
                xLimit = x - 1;
                x--;
            } else if (this.direction == 1) {
                yLimit = y - 1;
                y--;
            }
            this.currentPosition.setLocation(x, y);
        }
    }

    /* Action Functions */

    private void shootYourShot() {
        Point[] surroundings = surroundingPoints(this.currentPosition);
        Point d = null;
        int actions = Integer.MAX_VALUE;
        int score = 0;
        for (Point sur : surroundings) {
            if (!beenThere(sur) && !outOfBounds(sur)) {
                boolean[] sensors = this.worldMap.get(sur);
                if (sensors[7]) {
                    score = actionsRequired(this.currentPosition, sur);
                    if (score <= actions) {
                        actions = score;
                        d = sur;
                    }
                }

            }
        }
        moveToBestOption(d);
        this.toExecute.remove(Action.FORWARD);
        this.toExecute.add(Action.SHOOT);
    }

    private Point moveTowardFocus(Point source, Point destination) {
        Point[] surroundings = surroundingPoints(source);
        Point best = null;
        double min = Double.MAX_VALUE;
        for (Point p : surroundings) {
            if (this.safePoints.contains(p) && !outOfBounds(p)) {
                if (p.equals(destination)) {
                    return p;
                }
                double score = distance(p, destination);
                if (score == min) {
                    int bestActions = actionsRequired(source, best);
                    int actions = actionsRequired(source, p);
                    // actions require
                    if (actions < bestActions) {
                        best = p;
                    }
                } else if (score < min) {
                    min = score;
                    best = p;
                }
            }
        }

        return (best != null) ? best : source;
    }

    private void moveToBestOption(Point best) {
        if (best.equals(this.currentPosition)) {
            this.toExecute.add(Action.CLIMB);
            return;
        }
        double bestX = best.getX();
        double bestY = best.getY();
        double currX = this.currentPosition.getX();
        double currY = this.currentPosition.getY();

        if (this.direction == 1) {
            if (bestX > currX) {
                makeRightTurn();
            } else if (bestX < currX) {
                makeLeftTurn();
            } else if (bestY < currY) {
                toExecute.add(Action.TURN_LEFT);
                makeLeftTurn();
            } else {
                toExecute.add(Action.FORWARD);
            }
        } else if (this.direction == 2) {
            if (bestY > currY) {
                makeLeftTurn();
            } else if (bestY < currY) {
                makeRightTurn();
            } else if (bestX < currX) {
                toExecute.add(Action.TURN_LEFT);
                makeLeftTurn();
            } else {
                toExecute.add(Action.FORWARD);
            }
        } else if (this.direction == 3) {
            if (bestX > currX) {
                makeLeftTurn();
            } else if (bestX < currX) {
                makeRightTurn();
            } else if (bestY > currY) {
                toExecute.add(Action.TURN_RIGHT);
                makeRightTurn();
            } else {
                toExecute.add(Action.FORWARD);
            }
        } else if (this.direction == 4) {
            if (bestY > currY) {
                makeRightTurn();
            } else if (bestY < currY) {
                makeLeftTurn();
            } else if (bestX > currX) {
                toExecute.add(Action.TURN_RIGHT);
                makeRightTurn();
            } else {
                toExecute.add(Action.FORWARD);
            }
        }
    }

    private Action turnLeft() {
        if (this.direction > 1) {
            this.direction--;
        } else {
            this.direction = 4;
        }
        this.score--;
        return Action.TURN_LEFT;
    }

    public Action turnRight() {
        if (this.direction < 4) {
            this.direction++;
        } else {
            this.direction = 1;
        }
        this.score--;
        return Action.TURN_RIGHT;
    }

    private Action moveForward() {
        updatePosition();
        this.score--;
        return Action.FORWARD;
    }

    private Action grab() {
        this.score--;
        return Action.GRAB;
    }

    private Action climb() {
        this.score--;
        return Action.CLIMB;
    }

    private Action shoot() {
        this.score -= 10;
        return Action.SHOOT;
    }

    private void makeLeftTurn() {
        toExecute.add(Action.TURN_LEFT);
        toExecute.add(Action.FORWARD);
    }

    private void makeRightTurn() {
        toExecute.add(Action.TURN_RIGHT);
        toExecute.add(Action.FORWARD);
    }

    private Action doAction() {
        Action a = this.toExecute.remove();
        if (a == Action.FORWARD) {
            return moveForward();
        } else if (a == Action.TURN_LEFT) {
            return turnLeft();
        } else if (a == Action.TURN_RIGHT) {
            return turnRight();
        } else if (a == Action.GRAB) {
            return grab();
        } else if (a == Action.CLIMB) {
            return climb();
        } else {
            return shoot();
        }
    }
    /* Other Helper Functions */

    private boolean beenThere(Point p) {
        return this.worldMap.containsKey(p) && this.worldMap.get(p)[0];
    }

    private boolean outOfBounds(Point p) {
        double x = p.getX();
        double y = p.getY();
        if (x < 0 || y < 0) {
            return true;
        }
        if (xLimit != -1 && x > xLimit) {
            return true;
        }
        if (yLimit != -1 && y > yLimit) {
            return true;
        }
        return false;
    }

    private void mapLog() {
        for (Point p : this.worldMap.keySet()) {
            System.out.println("   " + getPosition(p) + " : " + printSensors(this.worldMap.get(p)) + "\n");
        }
    }

    private String printSensors(boolean[] s) {
        return "{visited : " + s[0] + ",\nstench : " + s[1] + ",\nbreeze : " + s[2] + ",\nglitter : " + s[3]
                + ",\nbump : " + s[4] + ",\nscream : " + s[5] + ",\npit : " + s[6] + ",\nwumpus : " + s[7] + "}";
    }

    private void printInfo() {
        System.out.println("=========TURN INFO==========");
        System.out.println("direction : " + getDirection());
        System.out.println("position : " + getPosition(this.currentPosition));
        System.out.println("score : " + this.score);
        System.out.println("\n* * * * MAP LOG * * * *");
        mapLog();
        System.out.println("============================");
    }

    private boolean allVisited(Set<Point> options) {
        for (Point p : options) {
            if (!beenThere(p)) {
                return false;
            }
        }
        return true;
    }

    private double distance(Point a, Point b) {
        double x, y, x1, y1, x2, y2;
        x1 = a.getX();
        x2 = b.getX();
        y1 = a.getY();
        y2 = b.getY();
        x = Math.pow(x2 - x1, 2);
        y = Math.pow(y2 - y1, 2);
        // return Math.sqrt(x + y);
        return Math.abs(x2 - x1) + Math.abs(y2 - y1);
    }

    private int actionsRequired(Point source, Point mightMoveTo) {
        int actions = 0;
        if (this.direction == 1) {
            if (source.getX() != mightMoveTo.getX()) {
                actions++;
            } else if (source.getY() > mightMoveTo.getY()) {
                actions += 2;
            }
        } else if (this.direction == 2) {
            if (source.getY() != mightMoveTo.getY()) {
                actions++;
            } else if (source.getX() > mightMoveTo.getX()) {
                actions += 2;
            }
        } else if (this.direction == 3) {
            if (source.getX() != mightMoveTo.getX()) {
                actions++;
            } else if (source.getY() < mightMoveTo.getY()) {
                actions += 2;
            }
        } else if (this.direction == 4) {
            if (source.getY() != mightMoveTo.getY()) {
                actions++;
            } else if (source.getX() < mightMoveTo.getX()) {
                actions += 2;
            }
        }
        return actions;
    }

    private Point[] surroundingPoints(Point source) {
        int x = (int) source.getX();
        int y = (int) source.getY();
        Point u, d, l, r;
        u = new Point(x, y + 1);
        d = new Point(x, y - 1);
        l = new Point(x - 1, y);
        r = new Point(x + 1, y);

        return new Point[] { u, d, l, r };
    }

    private Point findUnvisitedSafe() {
        double distance = Double.MAX_VALUE;
        Point toReturn = null;
        for (Point safe : this.safePoints) {
            if (!beenThere(safe)) {
                double d = distance(safe, this.currentPosition);
                if (d < distance) {
                    distance = d;
                    toReturn = safe;
                }
            }
        }
        return toReturn;
    }
}
// ======================================================================
// YOUR CODE ENDS
//