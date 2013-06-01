package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class ConvexOper implements TestBedOperation, Globals {

  /*! .enum  .public 4320 vertnumber entirepoly showangles
  */

  public static final int VERTNUMBER = 4320;//!
  public static final int ENTIREPOLY = 4321;//!
  public static final int SHOWANGLES = 4322;//!
  /*!*/

  public static ConvexOper singleton = new ConvexOper();
  private ConvexOper() {
  }
  public void addControls() {
    C.sOpenTab("Convex");
    {
      C.sStaticText("Tests if v-region contains its polygon.\n"
          + "This test can be repeated for each vertex\n"
          + "to determine if the polygon is k-convex.");

      C.sOpen();
      {
        C.sIntSpinner(VERTNUMBER, "vertex", "individual vertex number", 0, 50,
            0, 1);
        C.sCheckBox(SHOWANGLES, "show angles", null, false);
        C.sCheckBox(ENTIREPOLY, "check entire poly",
            "check if entire polygon is k-convex", false);
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

  public boolean testVertex(EdPolygon poly, int vertex, boolean trace) {
    final boolean db = trace;

    polygon = poly;
    circList = null;

    srcVertexIndex = vi(vertex);
    origin = vert(srcVertexIndex);
    circList = new CircList(polygon, srcVertexIndex, SSMain.k(), trace);
    if (db && T.update())
      T.msg("initial circular list");
    int vert = vi(srcVertexIndex + 1);
    while (vert != vi(srcVertexIndex - 1)) {

      // determine direction of this subsequence
      int dir = dirOf(vert);
      if (db && T.update())
        T.msg("vert #" + vert + ", direction of next sequence: " + dirName(dir)
        //+ EdSegment.showDirected(origin, vert(vert))
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

      double theta2 = MyMath.polarAngle(origin, vert(vert2));
      if (circList.rotateTo(dir == 1, theta2)) {
        break;
      }
      vert = vert2;
    }

    if (!circList.nonConvex())
      circList.stop();
    return circList.nonConvex();
  }

  public void runAlgorithm() {
    final boolean db = true;
    polygon = null;
    circList = null;
    EdPolygon[] polys = SSMain.getPolygons();

    if (polys.length == 0)
      return;

    EdPolygon polygon = polys[0];
    if (polygon.nPoints() < 3)
      return;

    if (C.vb(ENTIREPOLY)) {
      boolean convex = true;
      for (int i = 0; convex && i < polygon.nPoints(); i++) {
        if (testVertex(polygon, i, false)) {
          convex = false;
        }
        if (db && T.update())
          T.msg("tested vertex " + i);
      }
      if (convex)
        circList = null;
      V.draw((convex ? "" : "Not ") + SSMain.k() + "-convex", 0, 100, TX_CLAMP);
    } else
      testVertex(polygon, C.vi(VERTNUMBER), true);
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
  private CircList circList;
  private FPoint2 origin;
}

class CircList implements Renderable, Globals {
  private boolean trace;

  public CircList(EdPolygon poly, int vertIndex, int kValue, boolean trace) {
    //  final boolean db = true;
    this.trace = trace;
    this.polygon = poly;
    this.srcVertexIndex = vertIndex;
    this.kValue = kValue;
    srcVert = vert(0);
    double thetaPrev = vertAngle(-1);
    double thetaNext = vertAngle(1);

    boolean flip = MyMath.normalizeAngle(thetaNext - thetaPrev) < 0;
    if (flip) {
      double temp = thetaPrev;
      thetaPrev = thetaNext;
      thetaNext = temp;
    }

    primalNode = CircNode.makeVert(thetaNext, 0);
    dualNode = CircNode.makeVert(thetaNext + Math.PI, 0);

    CircNode edge = CircNode.makeEdge(0);
    CircNode edge2 = CircNode.makeEdge(0);
    CircNode.join(primalNode, edge, true);
    CircNode.join(edge, dualNode, true);
    CircNode.join(dualNode, edge2, true);
    CircNode.join(edge2, primalNode, true);

    CircNode prime2 = CircNode.makeVert(thetaPrev, 0);
    CircNode dual2 = CircNode.makeVert(thetaPrev + Math.PI, 0);

    edge2.split(prime2, true);
    edge.split(dual2, true);

    for (CircNode n = dualNode.next(true); n != dual2; n = n.next(true)) {
      n.incCount();
    }
    if (flip) {
      primalNode = prime2;
      dualNode = dual2;
      //      
      //      CircNode tmp = primalNode;
      //      primalNode = dualNode;
      //      dualNode = tmp;
    }
  }

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

  private boolean incCount() {
    primalNode.incCount();
    maxCountFound = Math.max(maxCountFound, primalNode.count()
        + dualNode.count());
    return nonConvex();
  }

  public void stop() {
    stopped = true;
  }

  public boolean rotateTo(boolean ccw, double thetaDest) {
    stopped = false;
    seekAngle = new Double(thetaDest);

    final boolean db = trace;
    final double EPS = 1e-4;
    double distance = angDistance(primalNode.theta(), thetaDest, ccw);

    if (!incCount())
      while (distance > 0) {

        if (db && T.update())
          T.msg("rotate " + ConvexOper.dirName(ccw) + " to "
              + Tools.fa2(thetaDest));

        // take minimum of distance to next primal vertex, dual vertex, or remaining distance

        double dist1 = angDistance(primalNode.theta(), primalNode.next(ccw)
            .next(ccw).theta(), ccw);
        double dist2 = angDistance(dualNode.theta(), dualNode.next(ccw).next(
            ccw).theta(), ccw);
        double nextVertDist = Math.min(dist1, dist2);

        if (Math.abs(nextVertDist - distance) < EPS)
          nextVertDist = distance;

        //      if (db && T.update())
        //        T.msg("distance to nearest next vertex is " + Tools.fa2(nextVertDist));
        if (nextVertDist > distance) {
          // create new vertex

          double newAngle = addAngle(primalNode.theta(), distance, ccw);
          if (db && T.update())
            T.msg("splitting edge at " + Tools.fa2(newAngle));

          CircNode nMain = CircNode.makeVert(newAngle, 0);
          primalNode.next(ccw).split(nMain, ccw);

          CircNode nDual = CircNode.makeVert(newAngle + Math.PI, 0);
          dualNode.next(ccw).split(nDual, ccw);
          nextVertDist = distance;
        }
        primalNode = primalNode.next(ccw);
        dualNode = dualNode.next(ccw);
        if (incCount())
          break;
        primalNode = primalNode.next(ccw);
        dualNode = dualNode.next(ccw);
        if (incCount())
          break;
        distance -= nextVertDist;
      }
    seekAngle = null;
    return nonConvex();
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

  private FPoint2 loc(CircNode n, double adj) {
    return radialPt(n.theta(), adj);
  }
  private FPoint2 radialPt(double theta, double adj) {
    return MyMath.ptOnCircle(srcVert, theta, RAD + adj);
  }

  public void render(Color c, int stroke, int markType) {
    final double SCL = .6;
    double W = SCL * 17 * V.getScale();
    double H = SCL * 3 * V.getScale();

    V.pushColor(MyColor.cDARKGREEN);
    V.pushStroke(STRK_THICK);
    V.drawCircle(srcVert, RAD);
    V.pop(2);

    V.pushColor(nonConvex() ? MyColor.cRED : MyColor.cDARKGREEN);
    V.mark(srcVert, MARK_CIRCLE);
    V.pop();

    CircNode n = primalNode;
    while (true) {

      //V.pushColor(MyColor.cLIGHTGRAY);
      V.pushStroke(STRK_RUBBERBAND);
      if (n.isVertex())
        V.drawLine(srcVert, loc(n, 0));
      FPoint2 pt = loc(n, 4);
      V.pop();
      if (n.isVertex()) {
        if (C.vb(ConvexOper.SHOWANGLES)) {
          V.pushColor(Color.white);
          V.fillRect(pt.x - W / 2, pt.y - H / 2, W, H);
          V.pop();
          V.pushColor(!stopped && n == primalNode ? MyColor.cRED
              : MyColor.cBLUE);
          V.drawRect(pt.x - W / 2, pt.y - H / 2, W, H);
          V.pushScale(SCL);
          V.draw(n.toString(), pt);
          V.pop(2);
        }
      } else
        V.draw(n.toString(), pt);

      CircNode n2 = n.next(true);

      n = n2;
      if (n == primalNode)
        break;
    }
    if (!stopped) {
      V.pushColor(MyColor.cRED);
      EdSegment.plotDirectedLine(srcVert, radialPt(primalNode.theta(), -3));
      if (seekAngle != null) {
        V.pushStroke(STRK_RUBBERBAND);
        EdSegment.plotDirectedLine(srcVert, radialPt(seekAngle.doubleValue(),
            -3));
        V.pop();
      }
      V.pop();
    }
  }

  private boolean stopped;
  private EdPolygon polygon;
  private int srcVertexIndex;
  private FPoint2 srcVert;
  private CircNode primalNode, dualNode;
  private int kValue;
  private Double seekAngle;
}

class CircNode {
  public static final int VERTEX = 0;
  public static final int EDGE = 1;

  /**
   * Split this edge node to accomodate a new vertex node.
   * Sets new vertex, and new edge, count to this count.
   * @param vert new vertex node
   * @param ccw direction
   * 
   */
  public void split(CircNode vert, boolean ccw) {
    if (isVertex() || !vert.isVertex())
      throw new IllegalArgumentException();
    CircNode edge2 = makeEdge(this.count());
    CircNode currNbr = this.next(ccw);
    join(this, vert, ccw);
    join(vert, edge2, ccw);
    join(edge2, currNbr, ccw);
    vert.count = this.count();

  }

  public static CircNode makeVert(double angle, int count) {
    CircNode n = new CircNode(VERTEX, count, angle);
    return n;
  }
  public int incCount() {
    count++;
    return count;
  }
  public boolean isVertex() {
    return type == VERTEX;
  }
  public static CircNode makeEdge(int count) {
    CircNode n = new CircNode(EDGE, count, 0);
    return n;
  }

  private CircNode(int type, int count, double angle) {
    this.type = type;
    this.count = count;
    this.angle = MyMath.normalizeAngle(angle);
  }
  public static void join(CircNode prev, CircNode next, boolean ccw) {
    if (prev.type == EDGE && next.type == EDGE)
      throw new IllegalArgumentException();
    if (ccw) {
      prev.next = next;
      next.prev = prev;
    } else {
      next.next = prev;
      prev.prev = next;
    }
  }
  public int count() {
    return count;
  }
  public double theta() {
    double a = 0;
    if (type == VERTEX)
      a = angle;
    else {
      if (next == null || prev == null)
        throw new IllegalStateException();
      a = MyMath.interpAngle(prev.theta(), next.theta(), .5);
    }
    return a;
  }
  public CircNode next(boolean ccw) {
    return ccw ? next : prev;
  }
  public CircNode prev(boolean ccw) {
    return ccw ? prev : next;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    if (isVertex()) {
      sb.append(Tools.fa2(theta()));
      sb.append(" [");
      sb.append(count());
      sb.append("]");
    } else
      sb.append(count());

    return sb.toString();
  }
  private int count;
  private int type;
  private double angle;
  private CircNode prev, next;

}
