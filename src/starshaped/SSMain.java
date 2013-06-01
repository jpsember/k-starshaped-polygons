package starshaped;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class SSMain extends TestBed {

  /*! .enum  .private   4000
     kvalue snap gen shiftvertfwd shiftvertbwd 
     */

    private static final int KVALUE           = 4000;//!
    private static final int SNAP             = 4001;//!
    private static final int GEN              = 4002;//!
    private static final int SHIFTVERTFWD     = 4003;//!
    private static final int SHIFTVERTBWD     = 4004;//!
/* !*/


  public static void main(String[] args) {

    if (false) {
      FPoint2 p0 = new FPoint2(0, 0);
      FPoint2 p1 = new FPoint2(1, 5);
      FPoint2 p2 = new FPoint2(0, 3);
      FPoint2 p3 = new FPoint2(3, 5);

      FPoint2 ipt = MyMath.linesIntersection(p0, p1, p2, p3, null);
      Streams.out.println("ipt=" + ipt.x + " " + ipt.y);

      double dist = MyMath.ptDistanceToLine(ipt, p0, p1, null);
      Streams.out.println("dist=" + dist);
      dist = MyMath.ptDistanceToLine(ipt, p2, p3, null);
      Streams.out.println("dist=" + dist);

      return;
    }

    new SSMain().doMainGUI(args);
  }

  public void addOperations() {
    addOper(VRgnOper.singleton);
    addOper(KernelOper.singleton);
    addOper(ConvexOper.singleton);
  //  addOper(VisOper.singleton);
    addOper(SampleOper.singleton);
  }

  public void initEditor() {
    Editor.addObjectType(EdPoint.FACTORY);
    Editor.addObjectType(EdPolygon.FACTORY);
    Editor.addObjectType(EdSegment.FACTORY);
  }

  public void setParameters() {
    parms.appTitle = "k-Starshaped Polygons";
    parms.menuTitle = "Main";
    parms.fileExt = "dat";
    parms.traceSteps = 800;
  }

  public void paintView() {
    polys = null;
    points = null;
    super.paintView();
  }
  public static int k() {
    return C.vi(KVALUE);
  }
  public void addControls() {
    if (false)
      C.sStaticText("A k-starshaped polygon is a simple polygon "
          + "that contains at least one point such that "
          + "the intersection of any ray from the point with the polygon "
          + "consists of not more than k connected components.");
    {
      C.sOpen();
      C.sIntSpinner(KVALUE, "k:", null, 0, 10, 2, 1);
      C.sHide();
      C.sCheckBox(SNAP, "snap", "snap polygons to grid", false);
      C.sHide();
      C.sButton(GEN, "generate", "generate polygon procedurally");
      C.sNewColumn();
      C.sButton(SHIFTVERTBWD, "r bwd", "rotate polygon vertices backward");
      C.sButton(SHIFTVERTFWD, "r fwd", "rotate polygon vertices forward");
      C.sClose();
    }
  }
  public static EdPolygon[] getPolygons() {
    if (polys == null) {
      DArray a = Editor.readObjects(EdPolygon.FACTORY, false, true);
      DArray f = new DArray();
      for (int i = 0; i < a.size(); i++) {
        EdPolygon p = (EdPolygon) a.get(i);
        EdPolygon p2 = new EdPolygon(p.getPts());
        if (C.vb(SNAP))
          for (int j = 0; j < p2.nPoints(); j++) {
            FPoint2 pt = p2.getPoint(j);
            pt = MyMath.snapToGrid(pt, 1);
            p2.setPoint(j, pt);
          }
        p2.setLabel(p.getLabel());
        p2.filterCollinear(1e-2);
        if (p2.complete())
          f.add(p2);
      }
      polys = (EdPolygon[]) f.toArray(EdPolygon.class);
    }
    return polys;
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case GEN:
        generate();
        break;
      case SHIFTVERTFWD:
      case SHIFTVERTBWD:
        rotVert(a.ctrlId == SHIFTVERTFWD);
        break;
      }
    }
  }

  private DArray pts;
  private void addPt(double x, double y) {
    pts.add(new FPoint2(x, y));
  }

  private void addPt(FPoint2 pt) {
    addPt(pt.x, pt.y);
  }

  private void addg(double nextx, double nexty, int cnt, boolean side,
      double aper) {

    final double SPAN = .3;
    final double INSET = (1.0 - SPAN) * .5;
    final double INSET2 = 1.0 - SPAN - INSET;

    FPoint2 prev = pts.getFPoint2Mod(-1);
    FPoint2 next = new FPoint2(nextx, nexty);
    for (int i = 0; i < cnt; i++) {
      double t = (((double) i) / (cnt - 1)) * SPAN + (side ? INSET : INSET2);
      FPoint2 org = FPoint2.interpolate(prev, next, t);
      double theta = MyMath.polarAngle(prev, next);

      addPt(org);
      addPt(MyMath.ptOnCircle(org, theta + Math.PI * .7, aper));
    }
    addPt(next);
  }

  private void generate() {
    pts = new DArray();

    int nGadgets = 3;
    final double ap1 = 1.2;
    final double ap2 = .5;

    double w = 50;
    double s = 2;
    double sn = 100 - s;
    double wn = 100 - w;

    addPt(0, 100);
    addPt(0, 0);
    addPt(w, 0);
    addPt(w, s);

    addg(s, s, nGadgets, true, ap1);
    addPt(s, wn);
    addg(w, wn, nGadgets, false, ap2);
    addg(w, sn, nGadgets, true, ap2);
    addPt(sn, sn);
    addg(sn, wn, nGadgets, false, ap1);
    addPt(100, wn);
    addPt(100, 100);

    EdPolygon p = new EdPolygon(pts);
    Editor.replaceAllObjects(DArray.build(p));
  }

  private static void rotVert(boolean fwd) {
    DArray a = Editor.editObjects(EdPolygon.FACTORY, false, true);
    for (int i = 0; i < a.size(); i++) {
      EdPolygon p = (EdPolygon) a.get(i);
      p.rotatePoints(fwd ? 1 : -1);
    }
  }

  public static EdPoint[] getPoints() {
    if (points == null) {
      DArray a = Editor.readObjects(EdPoint.FACTORY, false, true);
      points = (EdPoint[]) a.toArray(EdPoint.class);

    }
    return points;
  }

  private static EdPoint[] points;
  private static EdPolygon[] polys;
}
