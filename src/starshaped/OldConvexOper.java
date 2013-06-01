package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class OldConvexOper implements TestBedOperation, Globals {

  /*! .enum  .public 4520 vertnumber    
  */

    public static final int VERTNUMBER       = 4520;//!
/*!*/

  public static OldConvexOper singleton = new OldConvexOper();
  private OldConvexOper() {
  }
  public void addControls() {
    C.sOpenTab("Convex");
    {
      C.sStaticText("Tests if v-region contains its polygon.\n"
          + "This test can be repeated for each vertex\n"
          + "to determine if the polygon is k-convex.");

      C.sOpen();
      {
        C.sIntSpinner(VERTNUMBER, "vertex", null, 0, 50, 0, 1);
      }
      C.sClose();
    }
    C.sCloseTab();
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      }
    }
  }

  public void runAlgorithm() {
    final boolean db = true;

    polygon = null;
    circList = null;
    EdPolygon[] polys = SSMain.getPolygons();

    if (polys.length == 0)
      return;

    polygon = polys[0];
    if (polygon.nPoints() < 3)
      return;

    srcVertexIndex = vi(C.vi(VERTNUMBER));
    circList = new OldCircList(polygon, srcVertexIndex, SSMain.k());
    origin = vert(srcVertexIndex);

    //    if (db && T.update())
    //      T.msg("src vert #" + srcVertexIndex + T.show(origin));
    int vert = vi(srcVertexIndex + 1);
    while (vert != vi(srcVertexIndex - 1)) {

      // determine direction of this subsequence
      int dir = dirOf(vert);
      if (db && T.update())
        T.msg("vert #" + vert + ", direction of next sequence: " + dirName(dir)
            + EdSegment.showDirected(origin, vert(vert))
            + T.show(vert(vert + 1)));
      if (dir == 0) {
        if (db && T.update())
          T.msg("skipping edge collinear with source vertex");
        vert++;
        continue;
      }

      int vert2 = vi(vert + 1);
      while (vert2 != vi(srcVertexIndex - 1)) {
        double dir2 = dirOf(vert2);
        if (dir2 != dir)
          break;
        vert2 = vi(vert2 + 1);
      }

      if (db && T.update())
        T.msg("processing subsequence from " + vert + " to " + vert2);

      double theta2 = MyMath.polarAngle(origin, vert(vert2));
      circList.rotateTo(dir == 1, theta2);

      vert = vert2;
    }
  }

  public static String dirName(int dir) {
    if (dir == 0)
      return "RADIAL";
    return dirName(dir > 0);
  }
  public static String dirName(boolean ccw) {
    return ccw ? "CCW" : "CW";
  }

  private int dirOf(int vn) {
    return MyMath.sign(MyMath.sideOfLine(origin, vert(vn), vert(vn + 1)));
  }
  private FPoint2 vert(int n) {
    return polygon.getPointMod(n);
  }
  private int vi(int n) {
    return MyMath.mod(n, polygon.nPoints());
  }

  public void paintView() {
    Editor.render();
    T.show(circList);
  }
  private EdPolygon polygon;
  private int srcVertexIndex;
  private OldCircList circList;
  private FPoint2 origin;
}

class OldCircList implements Renderable, Globals {

  public OldCircList(EdPolygon poly, int vertIndex, int kValue) {
    this.polygon = poly;
    this.srcVertexIndex = vertIndex;
    this.kValue = kValue;
    srcVert = vert(0);
    double thetaPrev = vertAngle(-1);
    double thetaNext = vertAngle(1);

    boolean flip = MyMath.normalizeAngle(thetaNext - thetaPrev) > 0;
    if (flip) {
      double temp = thetaPrev;
      thetaPrev = thetaNext;
      thetaNext = temp;
    }

    mainNode = CircNode.makeVert(thetaNext, 1);
    CircNode nodeA = CircNode.makeVert(thetaPrev, 1);

    antiNode = CircNode.makeVert(thetaNext + Math.PI, 0);
    CircNode nodeB = CircNode.makeVert(thetaPrev + Math.PI, 0);

    CircNode.join(mainNode, nodeA, true);
    // eJoin(mainNode, nodeA, 1);
    CircNode.join(nodeA, antiNode, true);
    CircNode.join(antiNode, nodeB, true);
    CircNode.join(nodeB, mainNode, true);

    if (flip) {
      CircNode tmp = mainNode;
      mainNode = antiNode;
      antiNode = tmp;
    }
  }
  //  private static void eJoin(CircNode a, CircNode b, int count) {
  //    CircNode.join(a,b,true);
  ////    CircNode n = CircNode.makeEdge(count);
  ////    CircNode.join(a, n, true);
  ////    CircNode.join(n, b, true);
  //  }
  private FPoint2 vert(int index) {
    return polygon.getPointMod(index + srcVertexIndex);
  }
  private double vertAngle(int index) {
    if (index == 0)
      T.err("illegal argument");
    return MyMath.polarAngle(srcVert, vert(index));
  }

  public boolean nonConvex() {
    return maxCountFound > kValue + 2;
  }

  public void rotateTo(boolean ccw, double thetaDest) {
    final boolean db = true;
    final double EPS = 1e-4;
    double distance = angDistance(mainNode.theta(), thetaDest, ccw);
    // thetaDest = MyMath.normalizeAngle(thetaDest);

    while (true) {
      mainNode.incCount();
      maxCountFound = Math.max(maxCountFound, mainNode.count()
          + antiNode.count());
      if (nonConvex())
        break;

      if (db && T.update())
        T.msg("rotateTo " + Tools.fa2(thetaDest) + " dir="
            + ConvexOper.dirName(ccw) + " mainNode=" + mainNode + " distance="
            + Tools.fa2(distance));
      if (distance == 0)
        break;
      double dist1 = angDistance(mainNode.theta(), mainNode.next(ccw).next(ccw)
          .theta(), ccw);
      double dist2 = angDistance(antiNode.theta(), antiNode.next(ccw).next(ccw)
          .theta(), ccw);
      double nextVertDist = Math.min(dist1, dist2);

      if (Math.abs(nextVertDist - distance) < EPS)
        nextVertDist = distance;

      if (nextVertDist > distance) {
        // create new vertex

        double newAngle = addAngle(mainNode.theta(), distance, ccw);
        if (db && T.update())
          T.msg("next vert distance=" + Tools.f(nextVertDist)
              + " > required distance " + Tools.f(distance) + ";\n"
              + "making new vertex at " + Tools.fa2(newAngle));

        CircNode nMain = CircNode.makeVert(newAngle, mainNode.count());
        CircNode nAnti = CircNode
            .makeVert(newAngle + Math.PI, antiNode.count());
        // CircNode n = CircNode.makeEdge(mainNode.next(ccw).count());
        CircNode oldNext = mainNode.next(ccw);
        CircNode.join(mainNode, nMain, ccw);
        //CircNode.join(nMain, nMain, ccw);
        CircNode.join(nMain, oldNext, ccw);

        //n = CircNode.makeEdge(antiNode.next(ccw).count());
        oldNext = antiNode.next(ccw);
        CircNode.join(antiNode, nAnti, ccw);
        // CircNode.join(n, nAnti, ccw);
        CircNode.join(nAnti, oldNext, ccw);
      } else {
        //  Tools.warn("don't need edge nodes?");
        mainNode = mainNode.next(ccw);
        antiNode = antiNode.next(ccw);
        //        mainNode.incCount();
        //        antiNode.incCount();
        //        mainNode = mainNode.next(ccw);
        //        antiNode = antiNode.next(ccw);
        distance -= nextVertDist;
      }
    }

  }
  private double addAngle(double th0, double dist, boolean ccw) {
    return MyMath.normalizeAngle(th0 + dist * (ccw ? 1 : -1));
  }

  private double angDistance(double th0, double th1, boolean ccw) {
    double delta = th1 - th0;
    if (!ccw)
      delta = -delta;
    return MyMath.normalizeAnglePositive(delta);
  }

  private int maxCountFound;

  private static final double RAD = 34;

  public void render(Color c, int stroke, int markType) {
    final double SCL = .6;
    double W =SCL* 13 * V.getScale();
    double H =SCL* 2.6 * V.getScale();

    final boolean db = false;

    V.pushColor(MyColor.cLIGHTGRAY);
    V.pushStroke(STRK_THICK);
    V.drawCircle(srcVert, RAD);
    V.pop(2);

    V.pushColor(nonConvex() ? MyColor.cRED : MyColor.cDARKGREEN);
    V.mark(srcVert, MARK_CIRCLE);
    V.pop();

    CircNode n = mainNode;
    while (true) {

      if (db)
        Streams.out.println("plotting circNode " + n);

      FPoint2 pt = MyMath.ptOnCircle(srcVert, n.theta(), RAD);
      V.pushColor(MyColor.cLIGHTGRAY);
      V.pushStroke(STRK_THIN);
      V.drawLine(srcVert, pt);
      V.pop(2);
      //      if (n.isVertex()) {
      V.pushColor(Color.white);
      V.fillRect(pt.x - W / 2, pt.y - H / 2, W, H);
      V.pop();
      V.pushColor(n == mainNode ? MyColor.cRED : MyColor.cBLUE);
      V.drawRect(pt.x - W / 2, pt.y - H / 2, W, H);
      V.pushScale(SCL);
      V.draw(n.toString(), pt);
      V.pop(2);
      //      } else
      //        V.draw(n.toString(), pt);

      CircNode n2 = n.next(true);

      // double sep = MyMath.normalizeAnglePositive(n2.theta() - n.theta());

      /*
      final double STEP = MyMath.radians(2);
      final double ISTEP = MyMath.radians(12) * V.getScale();

      double distToNext = MyMath.normalizeAnglePositive(n2.theta() - n.theta());

      double a = ISTEP;

      if (db)
        Streams.out.println("plotting sep=" + sep + " a=" + a);

      V.pushColor(MyColor.cLIGHTGRAY);
      V.pushStroke(STRK_THICK);
      FPoint2 prev = null;

      while (a + ISTEP < distToNext) {
        FPoint2 pt2 = MyMath.ptOnCircle(srcVert, a + n.theta(), RAD);
        if (prev != null) {
          V.drawLine(prev, pt2);
        }
        prev = pt2;
        a += STEP;
      }
      V.pop(2);
      */
      n = n2;
      if (n == mainNode)
        break;
    }

  }
  private EdPolygon polygon;
  private int srcVertexIndex;
  private FPoint2 srcVert;
  private CircNode mainNode, antiNode;
  private int kValue;
}

class OldCircNode {
  //public static final int VERTEX = 0;
  // public static final int EDGE = 1;

  public static OldCircNode makeVert(double angle, int count) {
    OldCircNode n = new OldCircNode(count, angle);
    return n;
  }
  public int incCount() {
    count++;
    return count;
  }
  //  public boolean isVertex() {
  //    return type == VERTEX;
  //  }
  //  public static CircNode makeEdge(int count) {
  //    CircNode n = new CircNode(EDGE, count, 0);
  //    return n;
  //  }

  private OldCircNode(int count, double angle) {
    //   this.type = type;
    this.count = count;
    this.angle = MyMath.normalizeAngle(angle);
  }
  public static void join(OldCircNode lower, OldCircNode higher, boolean ccw) {
    //    if (lower.type == EDGE && higher.type == EDGE)
    //      throw new IllegalArgumentException();
    if (ccw) {
      lower.next = higher;
      higher.prev = lower;
    } else {
      higher.next = lower;
      lower.prev = higher;
    }
  }
  public int count() {
    return count;
  }
  public double theta() {
    double a = 0;
    //    if (type == VERTEX)
    a = angle;
    //    else {
    //      if (next == null || prev == null)
    //        throw new IllegalStateException();
    //      a = MyMath.interpAngle(prev.theta(), next.theta(), .5);
    //    }
    return a;
  }
  //  public CircNode next() {
  //    return next;
  //  }
  //  public CircNode prev() {
  //    return prev;
  //  }
  public OldCircNode next(boolean ccw) {
    return ccw ? next : prev;
  }
  public OldCircNode prev(boolean ccw) {
    return ccw ? prev : next;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    //    if (isVertex()) {
    sb.append(Tools.fa2(theta()));
    sb.append(" [");
    sb.append(count());
    sb.append("]");
    //    } else
    //      sb.append(count());

    return sb.toString();
  }
  private int count;
  //  private int type;
  private double angle;
  private OldCircNode prev, next;

}
