package starshaped;

import java.awt.*;
import testbed.*;
import base.*;

public class Edge implements Renderable, Globals {
  public LineEqn getLine() {
    return line;
  }

  public LineEqn getLine2() {
    return line2;

  }

  public boolean crosses() {
    return crosses;
  }

  public Edge(FPoint2 sourceVertex, Vertex v0, Vertex v1, boolean dual) {

    final boolean db = false;

    this.dual = dual;

    if (v0.isSource())
      nonSourceVertex = v1;
    else if (v1.isSource())
      nonSourceVertex = v0;
    if (nonSourceVertex != null) {
      angleFromSource = MyMath.normalizeAnglePositive(MyMath.polarAngle(
          sourceVertex, nonSourceVertex.pt())
          + (dual ? Math.PI : 0));
    }

    valid = nonSourceVertex == null
        && (Math.abs(v0.angle() - v1.angle()) > 1e-3);
    if (valid) {

      // if edge crosses positive x-axis, treat as special
      FPoint2 axisVert = new FPoint2(sourceVertex.x + (dual ? -1000 : 1000),
          sourceVertex.y);

      FPoint2 iPt = MyMath.lineSegmentIntersection(sourceVertex, axisVert, v0
          .pt(), v1.pt(), null);
      if (iPt != null) {
        crosses = true;
      }

      // arrange vertices so v0 -> v1 is ccw

      if (MyMath.sideOfLine(v0.pt(), v1.pt(), sourceVertex) < 0) {
        Vertex tmp = v0;
        v0 = v1;
        v1 = tmp;
      }
    }
    this.v0 = v0;
    this.v1 = v1;
    line = new LineEqn(v0.pt(), v1.pt());
    line2 = line;
    if (nonSourceVertex != null) {
      if (nonSourceVertex == v1) {
        line2 = new LineEqn(v0.pt(), line.polarAngle() + perturbAngle);
        if (db && T.update())
          T.msg("perturbed line2:" + T.show(this) + T.show(line2) + "\n"
              + line2);
      } else {
        FPoint2 npt0 = MyMath.ptOnCircle(v1.pt(), line.polarAngle() + Math.PI
            + perturbAngle, v0.pt().distance(v1.pt()));
        line2 = new LineEqn(npt0, v1.pt());
        if (db && T.update())
          T.msg("perturbed line2:" + T.show(this) + T.show(line2) + "\n"
              + line2);
      }

      if (db && T.update())
        T
            .msg("constructed edge from v0=" + v0.pt() + " to v1=" + v1.pt()
                + ", perturb=" + Tools.fa2(perturbAngle) + " line2"
                + T.show(line2));
    }

  }
  /**
   * Determine if edge is not on ray from source vertex
   * @return true if so
   */
  public boolean isValid() {
    return valid;
  }

  public Vertex getBwdVertex() {
    if (!isValid())
      throw new IllegalStateException();
    return getVert(0);
  }
  public Vertex getVert(int index) {
    return (index == 0) ? v0 : v1;
  }

  public Vertex getFwdVertex() {
    if (!isValid())
      throw new IllegalStateException();
    return getVert(1);
  }
  public FPoint2 farthestVertexFrom(LineEqn ln) {

    if (nonSourceVertex != null)
      return nonSourceVertex.pt();

    Vertex ret = null;
    double bestDist = 0;
    for (int i = 0; i < 2; i++) {
      Vertex v = getVert(i);
      double dist = ln.distanceFrom(v.pt());
      if (ret == null || dist > bestDist) {
        bestDist = dist;
        ret = v;
      }
    }
    return ret.pt();
  }

  public void render(Color c, int stroke, int markType, boolean displace) {
    V.pushColor(c, MyColor.cRED);
    V.pushStroke(stroke, STRK_THICK);
    FPoint2 p1, p2;
    if (isDual()) {
      p1 = v1.pt();
      p2 = v0.pt();
    } else {
      p1 = v0.pt();
      p2 = v1.pt();
    }

    if (displace) {
      FPoint2 cent = FPoint2.midPoint(p1, p2);
      double theta = MyMath.polarAngle(p1, p2);
      double rad = cent.distance(p1);
      cent = MyMath.ptOnCircle(cent, theta - Math.PI / 2, 1.0);
      //)- Math.PI / 2;
      p1 = MyMath.ptOnCircle(cent, theta, rad);
      p2 = MyMath.ptOnCircle(cent, theta - Math.PI, rad);
    }

    EdSegment.plotDirectedLine(p1, p2);

    V.pop(2);
  }

  public void render(Color c, int stroke, int markType) {
    render(c, stroke, markType, false);
  }
  public static void join(Edge edge, Edge edge2) {
    edge.next = edge2;
    edge2.prev = edge;
  }
  public Edge getNext() {
    return next;
  }
  public Edge getPrev() {
    return prev;
  }
  public boolean isDual() {
    return dual;
  }

  public Vertex nsVert() {
    return v0.isSource() ? v1 : v0;
  }

  public FPoint2 nonSrcPt() {
    return nsVert().pt();
  }

  public double angleFromSource() {
    if (nonSourceVertex == null)
      throw new IllegalStateException();
    return angleFromSource;

  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Edge[");
    if (nonSourceVertex != null) {
      sb.append("src to ");
      sb.append(nsVert().pt());
    } else {
      sb.append(getVert(0).pt());
      sb.append(" : ");
      sb.append(getVert(1).pt());
    }
    if (isDual())
      sb.append(" (dual)");
    sb.append("]");
    return sb.toString();
  }

  public static void setPerturb(double ang) {
    perturbAngle = ang;
  }

  private static double perturbAngle;

  private Edge prev, next;
  private Vertex nonSourceVertex;
  private double angleFromSource;
  private boolean dual;
  private boolean crosses;
  boolean valid;
  private LineEqn line;
  private LineEqn line2;
  private Vertex v0, v1;
  //private FPoint2 perturbVertex;
}
