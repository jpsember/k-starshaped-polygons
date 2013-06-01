package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class VisOper implements TestBedOperation, Globals {
  private static final boolean SPECIALPLOT = false;

  /*! .enum  .public 3750   db_envtopoly db_calcvispoly isovert sample gensamp
        _ adjustk showvis showdepth _ _ inclorig reflexonly gen holes isovert2
        isoactive plotparr _ unbounded unboundedradius
  */

    public static final int DB_ENVTOPOLY     = 3750;//!
    public static final int DB_CALCVISPOLY   = 3751;//!
    public static final int ISOVERT          = 3752;//!
    public static final int SAMPLE           = 3753;//!
    public static final int GENSAMP          = 3754;//!
    public static final int ADJUSTK          = 3756;//!
    public static final int SHOWVIS          = 3757;//!
    public static final int SHOWDEPTH        = 3758;//!
    public static final int INCLORIG         = 3761;//!
    public static final int REFLEXONLY       = 3762;//!
    public static final int GEN              = 3763;//!
    public static final int HOLES            = 3764;//!
    public static final int ISOVERT2         = 3765;//!
    public static final int ISOACTIVE        = 3766;//!
    public static final int PLOTPARR         = 3767;//!
    public static final int UNBOUNDED        = 3769;//!
    public static final int UNBOUNDEDRADIUS  = 3770;//!
/*!*/

  private static final boolean SPECIAL = false;

  public static VisOper singleton = new VisOper();
  private VisOper() {
  }
  private boolean db = true;

  public void addControls() {
    C.sOpenTab("VertVis");
    {
      C.sStaticText("Constructs k-kernel for polygon, "
          + "by intersecting various polygons derived from original; uses 'old' definition of k-kernel");

      C.sOpen();
      {
        C.sCheckBox(INCLORIG, "incl orig",
            "intersect with original polygon as well "
                + "(necessary; see orig_required.dat)", true);
        C.sCheckBox(REFLEXONLY, "reflex only",
            "generate visibility polygons for reflex vertices only"
                + " (insufficient; see 7.dat)", false);
        C.sCheckBox(ADJUSTK, "adjust k",
            "adjusts k value based on type of vertex (required)", true);
        C.sCheckBox(UNBOUNDED, "unbounded",
            "don't repeat last edge", false);
        C.sIntSpinner(UNBOUNDEDRADIUS, "unbnd radius", null, 0, 100, 50, 1);
         C.sCheckBox(ISOACTIVE, "iso active",
            "if off, ignores isolated vertex settings", true);
        C.sIntSpinner(ISOVERT, "isolated vertex",
            "calculates vis poly for single vertex", 0, 20, 0, 1);
        C.sIntSpinner(ISOVERT2, "isolated vert #2", null, 0, 20, 0, 1);
        C.sNewColumn();
        C.sCheckBox(DB_CALCVISPOLY, "db gen",
            "trace visibility polygon construction algorithm", false);
        C.sCheckBox(DB_ENVTOPOLY, "db env",
            "trace envelope->polygon algorithm", false);
        C.sCheckBox(SHOWDEPTH, "show depth",
            "shows StarDepth structure for isolated vertex or user point",
            false);
        C.sCheckBox(SHOWVIS, "show polys", "show all visibility polygons",
            false);
        C
            .sCheckBox(HOLES, "holes",
                "allow multiple source polygons; treat 2..n as holes in 1st",
                false);
        C.sCheckBox(PLOTPARR, "poly arr", "plots (small) polygon arrangement",
            false);
      }
      C.sClose();
      C.sOpen("Samples");
      {
        C.sCheckBox(SAMPLE, "active", "generates kernel by sampling process",
            false);
        C.sNewColumn();
        C.sButton(GENSAMP, "generate", "generate another n random samples");
      }
      C.sClose();
      //      C.sButton(GEN, "Generate", null);
    }
    C.sCloseTab();
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case GENSAMP:
        SampleOper.run();
        break;
      //      case GEN:
      //        generate();
      //        break;
      }
    }
  }

  public void runAlgorithm() {
    sampledKernel = null;
    calcKernel = null;
    isectPolys = null;
    arbPt = null;
    arbPt2 = null;
    calculatingKernelFlag = false;
    starDepth = null;
    algPoly = null;
    specPoly = null;

    boolean holes = C.vb(HOLES);

    EdPolygon[] polys = SSMain.getPolygons();
    EdPoint[] points = SSMain.getPoints();

    outer: do {
      if (polys.length == 0)
        break;
      algPoly = new DArray();
      for (int i = 0; i < polys.length; i++) {
        EdPolygon poly = polys[i];
        poly = Util.filterCollinear(poly);
        if (!poly.complete())
          continue;
        if (i > 0)
          poly.reverseWinding();
        algPoly.add(poly);
      }
      EdPolygon poly2 = (EdPolygon) algPoly.get(0);

      if (C.vb(SAMPLE))
        sampledKernel = SampleOper.samplePoly(poly2);

      isectPolys = new DArray();
      int iso = 0, iso2 = 0;
      if (C.vb(ISOACTIVE)) {
        iso = C.vi(ISOVERT);
        iso2 = C.vi(ISOVERT2);
      }
      boolean isof = (iso > 0);

      if (points.length != 0) {
        FPoint2 s = points[0].getOrigin();
        EdPolygon vpoly = VisPoly.calc(poly2, -1, s );
        addIntersectPoly(vpoly, "user point", s);
        if (SPECIALPLOT)
          specPoly = vpoly;
        else
          T.show(vpoly, MyColor.cRED, -1, -1);
        constructStarDepth(s, poly2);
      }

      for (int pi = 0; pi < algPoly.size(); pi++) {
        if (!holes && pi > 0)
          break;
        EdPolygon cp = (EdPolygon) algPoly.get(pi);

        for (int i = 0; i < cp.nPoints(); i++) {
          if (isof && (pi > 0 || (i != iso - 1 && i != iso2 - 1)))
            continue;

          if (false && i >= 3) {
            Tools.warn("skipping polys >= 3");
            continue;
          }

          FPoint2 vert = cp.getPoint(i);
          if (isof)
            constructStarDepth(vert, cp);

          if (C.vb(REFLEXONLY)) {
            if (MyMath.left(cp.getPointMod(i - 1), vert, cp.getPointMod(i + 1)))
              continue;
          }
          EdPolygon vpoly;
          if (C.vb(HOLES)) {
            DArray pset = algPoly;
            vpoly = VisPoly.calcForSet(pset, pi, i, null);
          } else {
            vpoly = VisPoly.calc(cp, i, null );
          }
          if (db && T.update())
            T.msg("visibility polygon for "
                + (C.vb(REFLEXONLY) ? "(reflex)" : "") + "vertex #" + i
                + T.show(vpoly, MyColor.cRED, -1, -1) + T.show(vert)
                + T.show(constructStarDepth(vert, cp)));

          if (vpoly == null) {
            break outer;
          }

          if (isof) {
            if (i == iso - 1) {
              arbPt = vert;
              T.show(vpoly, MyColor.cRED);
            } else {
              arbPt2 = vert;
              T.show(vpoly, MyColor.cDARKGREEN);
            }
          }
          addIntersectPoly(vpoly, null, null);
        }
      }
      if (!isof && !SPECIAL) {
        if (C.vb(INCLORIG)) {
          Tools.warn("intersecting only with first original polygon");
          addIntersectPoly(poly2, "original polygon", null);
        }
        {
          calculatingKernelFlag = true;
          calcKernel = IntersectOper
              .calcPolygonsIntersection(isectPolys, false);
          calculatingKernelFlag = false;
        }
      }
    } while (false);
  }

  private void plotArr(EdPolygon p) {
    V.pushColor(new Color(180, 180, 40));
    V.pushStroke(STRK_THICK);
    FPoint2[] v = p.getPoints();
    if (true)
      for (int i = 0; i < v.length; i++)
        v[i] = FPoint2.add(v[i], new FPoint2(.3, .3), null);
    for (int i = 0; i < v.length; i++) {
      // for (int j = i + 1; j < v.length; j++) {
      FPoint2 p0 = v[i], p1 = v[(i + 1) % v.length];
      LineEqn le = new LineEqn(p0, p1);
      double[] t = le.clipToRect(V.viewRect);
      if (t != null)
        V.drawLine(le.pt(t[0]), le.pt(t[1]));
      //  }
    }
    V.pop(2);
  }
  public void paintView() {
    if (sampledKernel != null)
      sampledKernel.render();

    if (SPECIALPLOT)
      calcKernel = null;

    T.show(calcKernel, MyColor.cDARKGRAY, -1, -1); //STRK_THICK, -1);
    if (C.vb(PLOTPARR) && algPoly != null && algPoly.size() > 0) {
      plotArr((EdPolygon) algPoly.get(0));
    }

    if (!calculatingKernelFlag) {
      if (false && SPECIALPLOT) {
        Tools.warn("special plot");
        if (algPoly != null)
          T.show(algPoly, MyColor.cBLUE, STRK_THICK, -1);
        T.show(specPoly, MyColor.cRED, -1, -1);
      } else
        Editor.render();
      if (C.vb(SHOWVIS) && isectPolys != null) {
        DArray a = new DArray();
        for (int i = 0; i < isectPolys.size(); i++) {
          EdPolygon p = (EdPolygon) isectPolys.get(i);
          EdPolygon p2 = p;
          p2 = p.offsetVertices(.5 + .3 * i);
          if (SPECIAL) {
            Tools.warn("special");
            if (!(i == 0 || i == 2 || i == 16))
              continue;
            p2 = p;
          }
          a.add(p2);
        }
        T.show(a, MyColor.cDARKGREEN, C.vi(ISOVERT) != 0 ? STRK_THICK
            : STRK_THIN, -1);
      }
      T.show(arbPt);
      T.show(arbPt2, MyColor.cDARKGREEN);
    }
    T.show(starDepth);
  }

  /**
   * If SHOWDEPTH is selected, make sure starDepth object has been built so
   * it gets rendered.
   * @param vert
   * @param poly
   * @return empty string
   */
  private String constructStarDepth(FPoint2 vert, EdPolygon poly) {
    if (starDepth == null && C.vb(SHOWDEPTH))
      starDepth = new StarDepth(vert, SSMain.k(), poly, false);
    return "";
  }

  /**
   * Add polygon to be intersected to list
   * @param p
   * @param debugMsg if not null, generates trace message
   * @param debugLoc
   */
  private void addIntersectPoly(EdPolygon p, String debugMsg, FPoint2 debugLoc) {
    isectPolys.add(p);
    if (debugMsg != null && db && T.update()) {
      T.msg("intersection polygon: " + debugMsg
          + T.show(p, MyColor.cRED, STRK_THICK, -1) + T.show(debugLoc));
    }
  }

  // sampled kernel
  private BitMap sampledKernel;
  // kernel calculated as intersection of visibility polygons
  private DArray calcKernel;
  // true if in the middle of calculating the kernel
  // (to suppress display of some graphics if this is the case)
  private boolean calculatingKernelFlag;
  // polygons to be intersected to calculate kernel
  private DArray isectPolys;
  private FPoint2 arbPt;
  private FPoint2 arbPt2;
  private StarDepth starDepth;
  private DArray algPoly;
  private Renderable specPoly;
  public static double unboundedRadius() {
    double r = 0;
    if (C.vb(UNBOUNDED))
      r = C.vi(UNBOUNDEDRADIUS) * 2.0;
    return r ;
  }
}
