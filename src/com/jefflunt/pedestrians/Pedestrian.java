package com.jefflunt.pedestrians;

import org.newdawn.slick.Color;
import org.newdawn.slick.GameContainer;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Renderable;
import org.newdawn.slick.geom.Circle;
import org.newdawn.slick.util.pathfinding.Path;

public class Pedestrian extends Circle implements Renderable {
  
  private static final long serialVersionUID = -5203575025425812220L;
  
  /** The maximum distance from a target location at which a Pedestrian is considered to have arrived. */
  public static final float STOP_DISTNACE = 3;
  /** The default collision radius of the collision circles around Pedestrians. */
  public static final float DEFAULT_COLLISION_RADIUS = 3;
  
  /** Speed for when you're stopped. */
  public static final float STOPPED = 0;
  /** Speed for when you're walking. */
  public static final float WALKING_SPEED = 15;
  /** Speed for when you're running. */
  public static final float RUNNING_SPEED = 45;
  
  /** The GameContainer of which this Pedestrian is a part. */
  private GameContainer container;
  /** The direction of travel, in radians. */
  private float direction;
  /** The speed of travel, in units/second. */
  private float speed;
  
  private float targetX;
  private float targetY;
  private int targetPathIndex;
  private Path targetPath;
  
  /** Creates a new Pedestrian */
  public Pedestrian(float x, float y, GameContainer container) {
    super(x, y, Pedestrian.DEFAULT_COLLISION_RADIUS);
    
    direction = 0;
    targetX = x;
    targetY = y;
    targetPathIndex = 0;
    targetPath = null;
    speed = STOPPED;
    this.container = container;
  }
  
  public Path getTargetPath() {
    return targetPath;
  }
  
  public int getTargetPathIndex() {
    return targetPathIndex;
  }
  
  public int getNumberOfPointsInPath() {
    return targetPath.getLength();
  }
  
  /** Causes the Pedestrian to move an appropriate amount, based on how much time has passed.
   * 
   * @param timeSlice The amount of time that has elapsed, in milliseconds.
   */
  public void move(long timeSlice) {
    float deltaX = (float) (speed*Math.cos(direction)) * (timeSlice/1000.0f);
    float deltaY = (float) (speed*Math.sin(direction)) * (timeSlice/1000.0f);
    
    if (hasReachedDestination()) {
      if (isOnAPathSomewhere()) {
        targetPathIndex++;
        if (targetPathIndex >= targetPath.getLength())
          stop();
        else
          headToward(targetPath.getX(targetPathIndex), targetPath.getY(targetPathIndex), getSpeed());
      }
    } else {
      setCenterX(getCenterX() + deltaX);
      setCenterY(getCenterY() + deltaY);
    }
  }
  
  /** The distance, from the Pedestrian's current location, along the Path they are following, to the end
   * of the Path. It should be noted that this is an approximation. Due to various Pedestrian behaviors
   * (such as collision avoidance), it's very possible that the actual Path the Pedestrian takes may be
   * slightly modified from it's planned Path. If the Pedestrian doesn't run into anything they need to avoid
   * along their Path, then the value returned from this method is going to be extremely accurate, to within
   * a fraction of a single pixel. In a heavily populated area where the Pedestrian must avoid collisions,
   * and other movement modifiers may apply, this method can only provide an approximation at best.
   * 
   * Finally, if a Pedestrian is not currently following a Path, then this method will return 0.0.
   * 
   * @return The distance from the Pedestrian's current location, along their Path, to the end of the Path.
   */
  public float distanceToEndOfPath() {
    float totalDistance = distanceToTarget();
    
    if (targetPath != null) {
      for (int i = targetPathIndex+1; i < targetPath.getLength(); i++) {
        totalDistance += Math.hypot(targetPath.getX(i)-targetPath.getX(i-1),
                                    targetPath.getY(i)-targetPath.getY(i-1));
      }
    }
    
    return totalDistance;
  }
  
  /** Whether or not the Pedestrian has reached the end of their Path.
   * 
   * @return true if the Pedestrian is currently on a Path somewhere (even if their current speed is zero). False otherwise.
   */
  public boolean isOnAPathSomewhere() {
    return (targetPath != null);
  }
  
  public void setNewTargetPoint(float x, float y, float speed) {
    setTargetLocation(x, y);
    this.speed = speed;
  }
  
  /** Tells the Pedestrian to head from their current location, along the specified Path.
   * 
   * @param p the Path to follow.
   * @param speed the speed at which you want them to travel along this path.
   */
  public void headAlongPath(Path p, float speed) throws IllegalArgumentException{
    targetPath = p;
    targetPathIndex = 0;
    if (p.getLength() == 0)
      throw new IllegalArgumentException("The path you send a Pedestrian on, must have at least one step.");
    else
      setNewTargetPoint(targetPath.getX(targetPathIndex), targetPath.getY(targetPathIndex), speed);
  }
  
  public void addStepToPath(int x, int y) {
    boolean wasNotOnAPath = (targetPath == null);
    if (wasNotOnAPath) {
      targetPath = new Path();
      targetPathIndex = 0;
    }
    
    if ((targetPath.getLength() == 0) || ((targetPath.getX(targetPath.getLength()-1) != x) || (targetPath.getY(targetPath.getLength()-1) != y))) 
      targetPath.appendStep(x, y);
    
    if (wasNotOnAPath) {
      headToward(targetPath.getX(targetPathIndex), targetPath.getY(targetPathIndex), getSpeed());
    }
  }
  
  /** The distance, from the Pedestrian's current location, to their current target location. If this Pedestrian
   * is following a Path, then this method returns how far it is to the next point in the path only. If you want
   * to find out how far the Pedestrian has yet to go along their Path, see distanceToEndOfPath.
   * 
   * @see distanceToEndOfPath
   * 
   * @return The distance between the Pedestrian's current location, and their target point.
   */
  public float distanceToTarget() {
    return distanceToPoint(targetX, targetY);
  }
  
  /** The distance, directly from the Pedestrian's current location, to the (x, y) point specified.
   * 
   * @param x the x-coordinate of the point
   * @param y the y-coordinate of the point
   * @return the distance from the Pedestrian's current location to the specified point.
   */
  public float distanceToPoint(float x, float y) {
    return (float) Math.hypot(getCenterX()-x, getCenterY()-y);
  }
  
  /** Whether or not the Pedestrian has reached the (x, y) location they are currently heading for.
   * 
   * @return true if they've reached that destination, false otherwise. If you ask a Pedestrian to follow
   * a Path, this method is used interally to know when the Pedestrian arrives at each of the points
   * along that Path, and serves as a way to know when the Pedestrian should change direction and head
   * for the next point on that Path.
   */
  public boolean hasReachedDestination() {
    return (distanceToPoint(targetX, targetY) <= Pedestrian.STOP_DISTNACE);
  }
  
  /** Tells a Pedestrian to stop moving, but remember where they were headed.
   * @see resume
   * @see stop
   */
  public void pause() {
    speed = STOPPED;
  }
  
  /** Tells a Pedestrian to continue toward their destination, after pausing. If the Pedestrian
   * forgot where they were headed (i.e. stopped), this method won't do anything.
   * @see pause
   * @see resume 
   * 
   * @param speed the speed at which you desire the Pedestrian to resume their course.
   */
  public void resume(float speed) {
    this.speed = speed;
  }
  
  /** Causes the Pedestrian to stop, and forget where they were headed.
   * @see pause
   * @see resume
   */
  public void stop() {
    speed = STOPPED;
    targetX = getCenterX();
    targetY = getCenterY();
    targetPath = null;
    targetPathIndex = 0;
  }
  
  /** Changes the Pedestrian's speed, but does not change their destination. If the Pedestrian has already arrived
   * at their destination, then this method will have no effect.
   * 
   * @param speed the speed at which you desire them to get there (e.g. "Post haste!").
   */
  public void changeSpeedTo(float speed) {
    if (!hasReachedDestination())
      this.speed = speed;
  }
  
  /** Sends this Pedestrian from their current location, directly toward the specified point in space.
   * 
   * @param x the x-coordinate of the destination
   * @param y the y-coordinate of the destination
   * @param speed the speed at which to travel there.
   */
  public void headToward(float x, float y, float speed) {
    setCurrSpeed(speed);
    setTargetLocation(x, y);
  }
  
  /** Sets the Pedestrian in motion either up, down, left, or right. The Pedestrian's target
   * location is actually set to Float.MAX_VALUE or -Float.MAX_VALUE, depending on which way
   * you tell them to go. They will continue in that direction unless blocked by something,
   * until they literally reach the edge of the world, where they will stop.
   * 
   * It should also be noted that an assumption about direction has been made. It is assumed
   * that "UP" is going to move you up along a computer screen, since it's assumed that you
   * will be displaying Pedestrians on a screen of some sort.
   * 
   * Coordinate systems on a computer screen usually have their y-axis increasing DOWNWARD,
   * whereas the coordinate systems you learned about in school have their y-axis increasing UPWARD. 
   * <pre>
   * Your typical coordinate system:          Your computer screen's coordinate system:
   * 
   *                  +y  (up)                               ((-Y))  (up)
   *                 / \                                      / \
   *                  |                                        |
   *                  |                                        |
   * (left) -x  <-----+-----> +x (right)      (left) -x  <-----+-----> +x (right)
   *                  |                                        |
   *                  |                                        |
   *                  |                                        |
   *                 \ /                                      \ /
   *                  -y  (down)                             ((+Y)) (down)
   * 
   * </pre>
   * 
   * That's it. No big deal, but it might save you some trouble one day.
   * 
   * @see ConfigValues.UP, DOWN, LEFT, and RIGHT.
   * 
   * @param upDownLeftOrRight the direction you want this Pedestrian to go.
   * @param speed the speed at which you desire the Pedestrian to travel.
   */
  public void headDirection(int upDownLeftOrRight, float speed) {
    switch (upDownLeftOrRight) {
      case ConfigValues.UP:
        headToward(getCenterX(), -Float.MAX_VALUE, speed);
        break;
      case ConfigValues.DOWN:
        headToward(getCenterX(), Float.MAX_VALUE, speed);
        break;
      case ConfigValues.LEFT:
        headToward(-Float.MAX_VALUE, getCenterY(), speed);
        break;
      case ConfigValues.RIGHT:
        headToward(Float.MAX_VALUE, getCenterY(), speed);
        break;
    }
  }
  
  public float getDirection() {
    return direction;
  }
  
  public float getSpeed() {
    return speed;
  }
  
  public float getTargetX() {
    return targetX;
  }
  
  public float getTargetY() {
    return targetY;
  }
  
  private void setTargetLocation(float x, float y) {
    targetX = x;
    targetY = y;
    
    float deltaX = targetX-getCenterX();
    float deltaY = targetY-getCenterY();
    
    direction = (float) Math.atan(deltaY/deltaX);
    if (deltaX < 0)
      direction += Math.PI;      // Second quadrant
    else if (deltaX > 0 && deltaY < 0)
      direction += Math.PI*2.0;  // Fourth quadrand
  }
  
  private void setCurrSpeed(float speed) {
    this.speed = speed;
  }

  @Override
  public void draw(float x, float y) {
    Graphics g = container.getGraphics();
    
    if (isOnAPathSomewhere()) {
      g.setColor(Color.blue);
      g.drawLine(getCenterX(), getCenterY(), getTargetX(), getTargetY());
      
      for (int i = targetPathIndex+1; i < targetPath.getLength(); i++) {
        if (i % 2 == 0)
          g.setColor(Color.cyan);
        else
          g.setColor(Color.orange);
        g.drawLine(targetPath.getX(i), targetPath.getY(i), targetPath.getX(i-1), targetPath.getY(i-1));
      }
      
      g.setColor(Color.red);
      g.fillOval(getTargetX(), getTargetY(), 2, 2);
      
    }
    
    g.setColor(Color.white);
    g.fillOval(x-radius, y-radius, 2*radius, 2*radius);
  }

}