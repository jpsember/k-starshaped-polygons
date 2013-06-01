package starshaped;

import java.util.*;
import testbed.*;
import base.*;

public class Util {
  public static double EPS = 1e-5;

  public static boolean same(FPoint2 p1, FPoint2 p2) {
    return same(p1, p2, EPS);
  }
  public static boolean same(FPoint2 p1, FPoint2 p2, double epsilon) {
    return FPoint2.distance(p1, p2) < epsilon;
  }
  public static double theta(FPoint2 origin, FPoint2 pt) {
    if (same(origin, pt))
      T.err("pt = origin");
    return MyMath.normalizeAnglePositive(MyMath.polarAngle(origin, pt));
  }
  public static boolean same(double v1, double v2) {
    return same(v1, v2, EPS);
  }
  public static boolean same(double v1, double v2, double epsilon) {
    return Math.abs(v1 - v2) < epsilon;
  }
  public static FPoint2 rotate180(FPoint2 pt, FPoint2 origin) {
    pt = new FPoint2(origin.x - (pt.x - origin.x), origin.y - (pt.y - origin.y));
    return pt;
  }

  public static EdPolygon scalePoly(EdPolygon p, FPoint2 origin, double sf) {
    Matrix m = Matrix.getTranslate(origin, true);
    Matrix mScale = Matrix.getScale(sf, sf);
    Matrix m3 = Matrix.getTranslate(origin, false);
    Matrix tfm = Matrix.mult(m3, mScale, null);
    Matrix.mult(tfm, m, tfm);

    EdPolygon p2 = new EdPolygon();
    for (int j = 0; j < p.nPoints(); j++) {
      FPoint2 pt = p.getPoint(j);
      p2.addPoint(tfm.apply(pt));
    }
    return p2;
  }
  /**
   * Filter collinear vertices from a polygon; doesn't change original
   * @param p source polygon
   * @return new, filtered polygon
   */
  public static EdPolygon filterCollinear(EdPolygon p) {
    EdPolygon p2 = new EdPolygon(p);
    p2.filterCollinear(1e-2);
    return p2;
  }
  public static EdPolygon perturb(EdPolygon p, Random random) {
    if (random == null)
      random = new Random();
    EdPolygon r = new EdPolygon(p);
    for (int i = 0; i < r.nPoints(); i++) {
      FPoint2 pt = r.getPoint(i);
      pt.x += random.nextDouble() - .5;
      pt.y += random.nextDouble() - .5;
    }
    return r;
  }

  /**
   * Utility function: show an object in red, with thick stroke
   * @param p
   * @return
   */
  public static String show(Object p) {
    return T.show(p, MyColor.cRED, Globals.STRK_THICK, -1);
  }

  /**
   * Utility function: show thick directed line
   * @param p1
   * @param p2
   * @return
   */
  public static String show(FPoint2 p1, FPoint2 p2) {
    return EdSegment.showDirected(p1, p2, null, Globals.STRK_THICK);
  }

  /**
   * Construct external face of a polygon
   * @param p
   * @return
   */
  public static void externalFaceOf(EdPolygon p, EdPolygon p2) {
    final boolean db = true;

    FRect r = p.getBounds();

    r.inset(-10);
    FPoint2 center = r.midPoint();

    double dMax = -1;
    int iMax = 0;
    for (int i = 0; i < p.nPoints(); i++) {
      double dist = p.getPoint(i).distance(center);
      if (dMax < dist) {
        dMax = dist;
        iMax = i;
      }
    }
    double theta = MyMath.polarAngle(center, p.getPoint(iMax));

    if (db && T.update())
      T.msg("externalFaceOf: " + T.show(p) + " bounding disc:"
          + T.show(new EdDisc(center, dMax))
          + T.show(MyMath.ptOnCircle(center, theta, dMax)));

    // EdPolygon p2 = new EdPolygon();
    for (int i = 0; i < p.nPoints(); i++)
      p2.addPoint(p.getPointMod(iMax - i - 1));
    if (db && T.update())
      T.msg("cut point" + T.show(p.getPoint(iMax)));
    int corn = (int) ((theta + Math.PI) / (Math.PI / 2));
    corn = MyMath.clamp(corn, 0, 3);

    for (int i = 0; i <= 4; i++) {
      FPoint2 corner = MyMath.ptOnCircle(center, (i + corn) * Math.PI / 2
          - (Math.PI * .75), dMax * 1.42);
      if (db && T.update())
        T.msg("corn=" + corn + ", corner #" + i + " is " + T.show(corner));
      p2.addPoint(corner);
    }
    p2.addPoint(p.getPointMod(iMax));
  }
}
