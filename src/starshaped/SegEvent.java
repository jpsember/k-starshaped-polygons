package starshaped;

import java.awt.*;
import testbed.*;
import base.*;

public class SegEvent implements Renderable {
  public Seg seg() {
    return seg;
  }
  public SegEvent(Seg seg, int endPt) {
    this.endPt = endPt;
    this.seg = seg;
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("SegEvent[");
    sb.append(seg.vertex(0));
    sb.append("...");
    sb.append(seg.vertex(1));
    sb.append("\nendPt:" + endPt);
    sb.append("]");
    return sb.toString();
  }

  public double distance() {
    return FPoint2.distance(seg.origin(), seg.vertex(endPt));
  }
  public double distanceOpp() {
    return FPoint2.distance(seg.origin(), seg.vertex(endPt ^ 1));
  }
  public void render(Color c, int stroke, int markType) {
    FPoint2 p0 = seg.vertex(endPt);
    FPoint2 p1 = FPoint2.interpolate(p0, seg.vertex(endPt ^ 1), .25);
    V.pushColor(c, MyColor.cPURPLE);
    EdSegment.plotDirectedLine(p0, p1);

    V.pushColor(MyColor.cLIGHTGRAY);
    V.pushStroke(Globals.STRK_RUBBERBAND);
    V.drawLine(seg.origin(), p0);
    V.pop(2);

    V.pop();
  }

  public int endPt() {
    return endPt;
  }
  private int endPt;
  private Seg seg;

}
