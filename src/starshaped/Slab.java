package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

/**
 * Keep track of segments within current slab
 */
public class Slab implements Renderable, Globals, Iterable {

  public int size() {
    return set.size();
  }
  private boolean vertFlag() {
    return vertIndex >= 0;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Slab");
    sb.append(poly.getLabel());
    sb.append("[");
    sb.append(Tools.fa2(theta0));
    sb.append("...");
    sb.append(Tools.fa2(theta1));
    sb.append("]");
    return sb.toString();
  }

  /**
   * Get angle of lower bounding ray of slab
   * @return angle, 0..2*PI
   */
  public double theta0() {
    return theta0;
  }
  /**
   * Get angle of upper bounding ray of slab
   * @return angle, 0..2*PI
   */
  public double theta1() {
    return theta1;
  }

  public Iterator iterator() {
    return set.iterator();
  }

  /**
   * Constructor
   * @param plotIndex for plotting purposes only; index of poly being scanned
   *  (0 or 1)
   * @param poly polygon containing vertex serving as origin of scan, or null if
   *  arbitrary point
   * @param vertIndex index of vertex within polygon, or -1 if arbitrary point
   * @param th0 initial lower bounding ray of slab 
   * @param th1 initial higher bounding ray of slab
   */
  public Slab(int plotIndex, EdPolygon poly, int vertIndex, FPoint2 org,
      double th0, double th1) {
    this.pIndex = plotIndex;
    this.poly = poly;
    this.vertIndex = vertIndex;
    if (vertIndex >= 0)
      org = poly.getPoint(vertIndex);
    this.origin = org;
    if (origin == null)
      T.err("problem");

    this.theta0 = th0;
    this.theta1 = th1;

    set = new TreeSet(new Comparator() {
      public int compare(Object arg0, Object arg1) {
        Seg e0 = (Seg) arg0, e1 = (Seg) arg1;
        int diff = 0;
        if (e0 != e1) {
          double theta = (theta0 + theta1) * .5;
          FPoint2 i0 = e0.ptAt(theta);
          FPoint2 i1 = e1.ptAt(theta);
          //          if (i0 == null || i1 == null) {
          //            Tools.warn("no point found");
          //            return 0;
          //          }
          if (i0 == null)
            T.err("no point found at " + theta + "\n" + e0);
          if (i1 == null)
            T.err("no point found at " + theta + "\n" + e1);

          diff = MyMath.sign(FPoint2.distance(origin, i0)
              - FPoint2.distance(origin, i1));
          if (diff == 0)
            T.err("unexpected:\n" + e0 + "\n" + e1);
        }
        return diff;
      }
    });
  }

  /**
   * Add a seg to the slab
   * @param seg
   */
  public void addSegment(Seg seg) {
    if (!seg.valid())
      T.err("invalid segment:" + seg);
    set.add(seg);
  }

  /**
   * Remove a seg from the slab
   * @param seg
   */
  public void removeSegment(Seg seg) {
    set.remove(seg);
  }

  public void removeSegments(Collection c) {
    set.removeAll(c);
  }

  /**
   * Advance arc of slab counterclockwise
   * @param th1 next upper bounding ray of slab
   */
  public void advanceThetaTo(double th1) {
    if (th1 <= theta0)
      T.err("bad args: th1=" + th1 + " theta0=" + theta0 + " theta1=" + theta1);
    this.theta0 = theta1;
    this.theta1 = th1;
  }

  public void render(Color c, int stroke, int markType) {
    V.pushColor(MyColor.cLIGHTGRAY);
    V.pushStroke(pIndex == 1 ? STRK_RUBBERBAND : STRK_NORMAL);
    V.mark(origin, Globals.MARK_DISC, .6);

//    int cnt = 0;
    for (Iterator it = set.iterator(); it.hasNext();) {
      Seg evt = (Seg) it.next();
      evt.render(null, stroke, markType);
    }
    double offset = 0;
    if (pIndex == 1)
      offset = Math.PI;
    V.drawLine(origin, MyMath.ptOnCircle(origin, theta0 + offset, 100));
    V.drawLine(origin, MyMath.ptOnCircle(origin, theta1 + offset, 100));
    V.pop(2);
  }

  public FPoint2 origin() {return this.origin;}
  
  private static boolean between(double theta, double thMin, double thMax) {
    return MyMath.normalizeAnglePositive(theta - thMin) < MyMath
        .normalizeAnglePositive(thMax - thMin);
  }

  /**
   * Determine if a ray is within an 'ignore' arc of this slab's
   * source vertex.
   * 
   * @param theta angle of ray from source vertex
   * @return true if ray is to the same side of both edges incident with
   *   the source vertex
   */
  public boolean inArc(double theta) {

    if (!vertFlag())
      throw new UnsupportedOperationException();

    boolean ret = false;

    FPoint2 prev = poly.getPointMod(vertIndex - 1);
    FPoint2 next = poly.getPointMod(vertIndex + 1);

    FPoint2 pt = MyMath.ptOnCircle(origin, theta, 1.0);
    if (MyMath.left(prev, origin, pt) == MyMath.left(origin, next, pt)) {
      ret = true;
    }

    //    double vThetaP = MyMath.normalizeAnglePositive(MyMath.polarAngle(origin,
    //        prev));
    //    double vThetaN = MyMath.normalizeAnglePositive(MyMath.polarAngle(origin,
    //        next)
    //        - vThetaP);
    //
    //    double th2 = MyMath.normalizeAnglePositive(theta - vThetaP);
    //    if (th2 > vThetaN)
    //      ret = true;

    return ret;
  }

  public int kAdjust() {
    final boolean db = false;

    int kAdj = 0;
    if (vertFlag()) {

      boolean vReflex = false;

      FPoint2 prev = poly.getPointMod(vertIndex - 1);
      FPoint2 next = poly.getPointMod(vertIndex + 1);
      double vThetaP = MyMath.normalizeAnglePositive(MyMath.polarAngle(origin,
          prev));
      double vThetaN = MyMath.normalizeAnglePositive(MyMath.polarAngle(origin,
          next));
      vReflex = MyMath.normalizeAngle(vThetaP - vThetaN) < 0;

      if (db && T.update())
        T.msg("kAdjust vReflex=" + vReflex);

      if (!vReflex) {
        if (!between((theta0 + theta1) * .5, vThetaN, vThetaP))
          kAdj = 1;
      } else {
        kAdj = 1;
        double oppMid = (theta0 + theta1) * .5 + Math.PI;
        if (between(oppMid, vThetaP, vThetaN))
          kAdj = 0;
      }
    }
    return kAdj;
  }
  public FPoint2 getOrigin() {
    return this.origin;
  }

  private int pIndex;
  private EdPolygon poly;
  private int vertIndex;
  private FPoint2 origin;
  private double theta0, theta1;
  private SortedSet set;
}
