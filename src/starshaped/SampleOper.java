package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class SampleOper implements TestBedOperation, Globals {
  /*! .enum  .private 3600      
      resetsamp ntrials plotlast run _ 
      step resolution    
  */

    private static final int RESETSAMP        = 3600;//!
    private static final int NTRIALS          = 3601;//!
    private static final int PLOTLAST         = 3602;//!
    private static final int RUN              = 3603;//!
    private static final int STEP             = 3605;//!
    private static final int RESOLUTION       = 3606;//!
/*!*/

  public void addControls() {
    C.sOpenTab("Sample");
    {
      C.sStaticText("Samples polygon, determines if points are star centers");
      C.sButton(STEP, "Step", "Process single random hull");
      {
        C.sOpen();
        C.sButton(RESETSAMP, "Reset", "Clears sampled hull");
        C.sButton(RUN, "Run", "Generate a number of random hulls");

        C.sNewColumn();
        C.sIntSpinner(NTRIALS, "# trials",
            "Number of samples to clip guaranteed hull to", 1, 50000, 3000,
            1000);
        C.sClose();
      }

      C.sIntSpinner(RESOLUTION, "Res", "Sampling resolution", 1, 10, 3, 1);
      C.sCheckBox(PLOTLAST, "Plot last sample", null, true);
    }
    C.sCloseTab();
  }

  public static SampleOper singleton = new SampleOper();

  private SampleOper() {
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case RESETSAMP:
        kernel = null;
        break;
      case STEP:
        constructPoly();
        run(1, -1);
        break;
      case RUN:
        constructPoly();
        run(C.vi(NTRIALS), -1);
        break;
      }
    }
  }

  private void run(int trials, int res) {
    if (poly != null) {
      prepareBitmap();
      FRect r = poly.getBounds();
      int k = SSMain.k();

      for (int t = 0; t < trials; t++) {

        // choose an interior point
        FPoint2 pt = new FPoint2(r.x + MyMath.rnd(r.width), r.y
            + MyMath.rnd(r.height));
        if (poly.isPointInside(pt) == 0)
          continue;

        // determine if point is in kernel
        StarDepth starDepth = new StarDepth(pt, k, poly, false);
        if (starDepth.inKernel())
          kernel.plot(pt,null);
        lastStarDepth = starDepth;
      }
    }
  }

  private void prepareBitmap() {
    if (poly != null) {
      int res = C.vi(RESOLUTION);
      double gs = Math.pow(res, 1.5) * .07 + .2;
      if (kernel == null)
        kernel = new BitMap();
      String hash = "r:" + res + "k:" + SSMain.k() + "| " + poly.getHash();
      kernelReset = kernel.prepare(hash, gs);
      kernel.setColor(MyColor.get(MyColor.LIGHTGRAY, .5));
    } else
      kernel = null;
  }

  public void runAlgorithm() {
    starDepth = null;
    EdPoint[] pt = SSMain.getPoints();
    EdPolygon[] polys = SSMain.getPolygons();
    do {
      if (pt.length == 0)
        break;
      if (polys.length == 0)
        break;
      EdPolygon poly = polys[0];
      FPoint2 p = pt[0].getPoint(0);

      if (poly.isPointInside(p) == 0)
        break;
      starDepth = new StarDepth(p, SSMain.k(), poly, true);
    } while (false);
  }

  private void constructPoly() {
    EdPolygon[] pl = SSMain.getPolygons();
    if (pl.length < 1)
      return;
    poly = pl[0];
    if (poly.nPoints() < 3)
      poly = null;
  }
  public void paintView() {
    T.render(kernel);
    Editor.render();
    T.render(starDepth);
    if (C.vb(PLOTLAST))
      T.render(lastStarDepth);
  }

  public static BitMap samplePoly(EdPolygon p) {
    singleton.poly = p;
    singleton.prepareBitmap();
    if (singleton.kernel != null) {
      if (singleton.kernelReset) {
        singleton.run(C.vi(NTRIALS), C.vi(RESOLUTION));
      }
    }
    return singleton.kernel;
  }

  private boolean kernelReset;
  private BitMap kernel;
  private StarDepth starDepth;
  private EdPolygon poly;
  private Renderable lastStarDepth;

  public static void run() {
    singleton.constructPoly();
    singleton.run(C.vi(NTRIALS), -1);
  }
}
