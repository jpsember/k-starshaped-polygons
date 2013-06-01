package starshaped;

import java.awt.*;
import testbed.*;
import base.*;

public class Vertex implements Renderable, Globals {

  /**
   * Construct vertex
   * @param sourceVertex location of source vertex for V-region
   * @param index index of vertex
   * @param pt location of vertex
   */
  public Vertex(FPoint2 sourceVertex, int index, FPoint2 pt) {
    this.v = index;
    this.pt = pt;
    isSource = (pt.distanceSq(sourceVertex) == 0);
    if (!isSource)
      angle = MyMath
          .normalizeAnglePositive(MyMath.polarAngle(sourceVertex, pt));
  }

  /**
   * Get angle that vertex makes with source vertex
   * @return angle
   */
  public double angle() {
    return angle;
  }
  
  /**
   * Get location of vertex
   * @return
   */
  public FPoint2 pt() {
    return pt;
  }
  
  /**
   * Get index of vertex
   * @return index of vertex within its polygon
   */
  public int index() {
    return v;
  }
  
  /**
   * Renderable interface
   */
  public void render(Color c, int stroke, int markType) {
    V.pushColor(c, MyColor.cDARKGREEN);
    V.mark(pt(), MARK_DISC);
    V.pop();
  }
  
  /**
   * Determine if this is the source vertex
   * @return true if so
   */
  public boolean isSource () {
    return isSource;
  }

  private int v;
  private FPoint2 pt;
  private double angle;
  private boolean isSource;
}
