package starshaped;

import java.awt.*;
import testbed.*;
import base.*;

public class EdgeEvent implements Renderable, Globals {
  public static final int EVT_REMOVEEARLY = 0;
  public static final int EVT_INSERTEARLY = 1;
  public static final int EVT_REMOVELATE = 2;
  public static final int EVT_INSERTLATE = 3;

  private static String[] names = { "RemEarly", "InsEarly", "RemLate",
      "InsLate" };
  public String getTypeString() {
    return names[type];
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(id);
    sb.append(":");
    sb.append(getTypeString());
    if (debugMsg != null) {
      sb.append(" ");
      sb.append(debugMsg);
    }
    return sb.toString();
  }

  public double getTheta(FPoint2 srcVert) {
    double theta = MyMath.polarAngle(srcVert, getVertex());
    if (auxVertex == null && edge.isDual())
      theta += Math.PI;
    return MyMath.normalizeAnglePositive(theta);
  }

  public EdgeEvent(Edge edge, int type, FPoint2 auxVertex, String debugMsg) {
    this.id = eventIds++;
    this.edge = edge;
    this.type = type;
    this.auxVertex = auxVertex;
    this.debugMsg = debugMsg;
    if ((auxVertex == null) ^ edge.isValid())
      throw new IllegalArgumentException();

  }

  public int getId() {
    return id;
  }

  public FPoint2 getVertex() {
    FPoint2 v = null;
    if (auxVertex != null)
      v = auxVertex;
    else {
      boolean fwd = (type == EVT_REMOVEEARLY || type == EVT_REMOVELATE);
      v = (fwd ? edge.getFwdVertex() : edge.getBwdVertex()).pt();
    }
    return v;

  }

  //  public Vertex getVertex0() {
  //    if (edge.isValid()) {
  //      boolean fwd = (type == EVT_REMOVEEARLY || type == EVT_REMOVELATE);
  //      return fwd ? edge.getFwdVertex() : edge.getBwdVertex();
  //    } else {
  //      return auxVertex;
  //    }
  //
  //  }
  public Edge getEdge() {
    return edge;
  }
  public int getType() {
    return type;
  }

  public void render(Color c, int stroke, int markType) {
    edge.render(c, stroke, markType);
    FPoint2 pt = getVertex(); //.pt();
    FPoint2 pt2 = MyMath.ptOnCircle(pt, 0, 14);

    V.pushScale(.8);
    V.pushColor(c, MyColor.cDARKGREEN);
    V.pushStroke(STRK_RUBBERBAND);
    V.drawLine(pt, pt2);
    V.pop();
    V.mark(pt, MARK_DISC);
    V.draw(this.toString(), pt2, TX_BGND | TX_FRAME);
    V.pop(2);
  }
  public static void resetIds() {
    eventIds = 0;
  }
  private static int eventIds;
  private Edge edge;
  private int type;
  private int id;
  private FPoint2 auxVertex;
  private String debugMsg;

}
