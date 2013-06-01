package starshaped;

import java.awt.*;
import testbed.*;
import base.*;

public class Seg implements Renderable, Globals {
  private static final double EPS = 1e-3;
  public Seg(int polyIndex, FPoint2 origin, FPoint2 p0, FPoint2 p1) {
    double th0 = Util.theta(origin, p0);
    double th1 = Util.theta(origin, p1);
    if (th0 > th1) {
      FPoint2 tmp = p0;
      p0 = p1;
      p1 = tmp;
    }
    this.polyIndex = polyIndex;
    this.origin = origin;
    this.p0 = p0;
    this.p1 = p1;
  }

  public double theta(int index) {
    return Util.theta(origin, vertex(index));
  }

  public FPoint2 vertex(int index) {
    return index == 0 ? p0 : p1;
  }

  public void render(Color c, int stroke, int markType) {
    V.pushColor(c, MyColor.cPURPLE);
    V.pushStroke(stroke, STRK_THICK);
    FPoint2 s0 = p0, s1 = p1;
    if (polyIndex == 1) {
      s0 = Util.rotate180(p0, origin);
      s1 = Util.rotate180(p1, origin);
    }
    if (polyIndex == 0)
      EdSegment.plotDirectedLine(s0, s1);
    else
      EdSegment.plotDirectedLine(s1, s0);
    V.pop(2);
  }
  
  public FPoint2 ptAt(double theta) {
    FPoint2 rayEnd = MyMath.ptOnCircle(origin, theta, 1.0);
    double[] ip = new double[2];
    FPoint2 isect = MyMath.linesIntersection(p0, p1, origin, rayEnd, ip);
    if (false) {
      if (isect == null)
        T.err("no intersection");
      if (ip[1] <= 0)
        T.err("unexpected: ip[1]=" + ip[1]);
      if (ip[0] < 0 || ip[0] > 1.0)
        T.err("unexpected: ip[0]=" + ip[0] + EdSegment.show(p0, p1));
    }
    return isect;
  }

  private double theta(FPoint2 pt) {
    return Util.theta(origin, pt);
  }

  public FPoint2 origin() {
    return origin;
  }
  public int getPolyIndex() {
    return polyIndex;
  }

  /**
   * Calculate angle of intersection of two segments
   * @param s0
   * @param s1
   * @return angle of intersection, or negative if none
   */
  public static double calcIntersection(Seg s0, Seg s1) {
    double ret = -1;
    FPoint2 isect = MyMath.lineSegmentIntersection(s0.p0, s0.p1, s1.p0, s1.p1,
        null);
    if (isect != null)
      ret = s0.theta(isect);
    return ret;
  }

  public Seg clipTo(double a0, double a1) {
    FPoint2 c0 = ptAt(a0);
    FPoint2 c1 = ptAt(a1);
    return new Seg(polyIndex, origin, c0, c1);
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Seg");
    sb.append("\np0=" + p0 + " theta=" + theta(p0));
    sb.append("\np1=" + p1 + " theta=" + theta(p1));
    sb.append("\npolyIndex=" + polyIndex);
    return sb.toString();
  }

  public boolean valid() {
    return FPoint2.distance(p0, p1) >= EPS;
  }

  private FPoint2 p0, p1;
  private FPoint2 origin;
  private int polyIndex;
}
