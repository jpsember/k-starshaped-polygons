package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class VisEventQueue implements Renderable, Comparator {

  public int compare(Object arg0, Object arg1) {
    SegEvent e0 = (SegEvent) arg0, e1 = (SegEvent) arg1;
    Seg s0 = e0.seg();
    Seg s1 = e1.seg();

    int diff = 0;
    if (e0 != e1) {
      diff = MyMath.sign(s0.theta(e0.endPt()) - s1.theta(e1.endPt()));
      if (diff == 0)
        diff = MyMath.sign(e0.distance() - e1.distance());
      if (diff == 0)
        diff = MyMath.sign(s0.theta(e0.endPt() ^ 1) - s1.theta(e1.endPt() ^ 1));
      if (diff == 0)
        diff = MyMath.sign(e0.distanceOpp() - e1.distanceOpp());
      if (diff == 0)
        T.err("unexpected:\n" + e0 + "\n" + e1);
    }
    return diff;
  }

  public Iterator iterator() {
    return set.iterator();
  }

  public VisEventQueue(FPoint2 origin) {
    this.origin = origin;
    set = new TreeSet(this);
  }

  public void addEdgesFrom(EdPolygon poly, int polyIndex) {
    FPoint2 prev = null;
    for (int i = 0; i <= poly.nPoints(); i++) {
      FPoint2 pt = poly.getPointMod(i);
      if (prev != null) {
        boolean add;
        add = MyMath.left(prev, pt, origin) == (polyIndex == 0);
        if (add)
          addSegment(polyIndex, prev, pt, true);
      }
      prev = pt;
    }
  }

  private void addSegment(int polyIndex, FPoint2 p0, FPoint2 p1,
      boolean crossTest) {
    do {
      if (Util.same(p0, origin) || Util.same(p1, origin))
        break;
      {
        // split if it crosses the theta=0 ray
        double[] iParam = new double[2];
        FPoint2 leftOfOrigin = new FPoint2(origin.x + 1, origin.y);
        FPoint2 isect = MyMath.linesIntersection(p0, p1, origin, leftOfOrigin,
            iParam);
        double p = iParam[0];
        if (isect != null //
            && iParam[1] >= 0 //
            && p >= 0 //
            && p <= 1.0 //
        ) {
          if (!crossTest)
            T.err("unexpected");

          if (p > Util.EPS) {
            FPoint2 nearRay = FPoint2.interpolate(p0, p1, p - Util.EPS);
            addSegment(polyIndex, p0, nearRay, false);
          }
          if (p < 1.0 - Util.EPS) {
            FPoint2 nearRay = FPoint2.interpolate(p0, p1, p + Util.EPS);
            addSegment(polyIndex, nearRay, p1, false);
          }
          break;
        }
      }
      Seg seg = new Seg(polyIndex, origin, p0, p1);
      if (!seg.valid()) break;
      if (seg.theta(0) == seg.theta(1))
        break;
      SegEvent e0 = new SegEvent(seg, 0);
      SegEvent e1 = new SegEvent(seg, 1);
      set.add(e0);
      set.add(e1);
    } while (false);
  }

  public void render(Color c, int stroke, int markType) {
    V.pushColor(MyColor.cPURPLE);
    V.mark(origin, Globals.MARK_DISC, .6);

    for (Iterator it = set.iterator(); it.hasNext();) {
      SegEvent evt = (SegEvent) it.next();
      T.render(evt);
    }
    V.popColor();
  }

  private FPoint2 origin;
  private SortedSet set;
}
