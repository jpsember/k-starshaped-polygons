package starshaped;

import java.awt.*;
import base.*;
import testbed.*;

public class StarDepth implements Renderable, Globals {

  private static String deg(double rad) {
    return Tools.f(rad * 180 / Math.PI);
  }

  public void render(Color c, int stroke, int markType) {

    if (processed) {
      V.pushColor(c, getK() <= kValue ? MyColor.cDARKGREEN : MyColor.cRED);
      V.drawCircle(starCenter, 1.5);
      V.popColor();
    }

    boolean showEnts = Editor.withLabels(false);

    V.pushColor(MyColor.cDARKGREEN);
    if (startEntry != null) {
      Inf inf = new Inf();
      Ent e = startEntry;

      do {
        {
          FPoint2 sp = e.position(0, 0);
          FPoint2 tp = e.position(0, 10);
          V.pushStroke(STRK_THIN);
          V.pushColor(MyColor.cLIGHTGRAY);
          EdSegment.plotDirectedLine(starCenter, MyMath.ptOnCircle(starCenter,
              e.theta(), 70));
          V.pop(2);

          V.drawLine(sp, tp);

          if (showEnts) {
            V.pushScale(.7);
            V.draw(e.toString(), tp, TX_FRAME | TX_BGND | 20);
            V.pop();
          }
        }

        e = e.next();
        inf.update();
        V.pushColor(MyColor.cRED);
        double rn = e.range();
        FPoint2 prev = null;
        for (double t = 0; t < rn; t += MyMath.radians(3)) {
          FPoint2 pt = e.position(t, 0);
          if (prev != null)
            V.drawLine(prev, pt);
          prev = pt;
        }
        FPoint2 pt = e.position(rn, 0);
        if (prev != null)
          V.drawLine(prev, pt);
        V.pop();
      } while (e != startEntry);
    }
    V.pop();
  }

  /**
   * Constructor
   * @param starCenter location of star center candidate
   */
  public StarDepth(FPoint2 starCenter, int kValue, EdPolygon p, boolean db) {
    this.db = db;
    this.starCenter = starCenter;
    this.kValue = kValue;
    process(p.getPts());
  }

  public boolean inKernel() {
    return getK() <= kValue;
  }

  private int kValue;
  private boolean db;

  private DArray filterStarCenter(DArray verts) {
    // filter out star center
    DArray v = new DArray();
    for (int i = 0; i < verts.size(); i++) {
      FPoint2 pt = verts.getFPoint2(i);
      if (!same(pt, starCenter))
        v.add(pt);
    }
    verts = v;
    if (db && T.update())
      T.msg("filtered out star center" + T.show(verts));
    return verts;
  }

  private int getK() {
    int d = (starDepth + 1) / 2;
    return d;
  }

  private void process(DArray verts) {
    if (processed)
      throw new IllegalStateException();
    if (db && T.update())
      T.msg("StarDepth.process" + T.show(verts));
    Inf inf = new Inf();

    do {
      verts = filterStarCenter(verts);

      // start with vertex that is start of ccw sequence
      currentEntry = null;

      int startVert = 0;
      {
        boolean found = false;
        FPoint2 p = verts.getFPoint2Mod(-1);
        FPoint2 p2 = verts.getFPoint2(0);
        for (; startVert < verts.size(); startVert++) {
          FPoint2 p3 = verts.getFPoint2Mod(startVert + 1);
          if (db && T.update())
            T.msg("examining point for start of ccw sequence" + T.show(p2));
          if (MyMath.sideOfLine(p, p2, starCenter) < 0
              && MyMath.sideOfLine(p2, p3, starCenter) > 0) {
            found = true;
            break;
          }
          p = p2;
          p2 = p3;
        }

        if (!found) {
          Ent single = new Ent(0);
          join(single, single);
          single.increaseDepth();
          if (db && T.update())
            T.msg("polygon is 1-starshaped");
          currentEntry = single;
          startEntry = single;
          break;
        }
        startVert = startVert % verts.size();
      }

      boolean fwd = true;
      if (db && T.update())
        T.msg("start vertex=" + startVert + T.show(verts.get(startVert)));
      int currVert = startVert;

      {
        // start with level 0 entry covering entire circle
        currentEntry = new Ent(0);
        startEntry = currentEntry;
        join(currentEntry, currentEntry);
        currentEntry = currentEntry.split(theta(verts.getFPoint2(startVert)));
      }

      do {
        inf.update();

        // Find next change of direction vertex.

        if (db && T.update())
          T.msg("fwd:" + Tools.f(fwd) + " current vert=" + currVert
              + T.show(verts.get(currVert)) + "\ncurrentEntry=" + currentEntry
              + " th=" + currentEntry.theta());

        int nextVert = currVert;
        double sweptAngle = 0;
        double prevTheta = currentEntry.theta();

        while (true) {
          inf.update();
          FPoint2 p1 = verts.getFPoint2Mod(nextVert);
          FPoint2 p2 = verts.getFPoint2Mod(nextVert + 1);
          if (false && db && T.update())
            T.msg("sweptRange=" + deg(sweptAngle) + "; testing side of line"
                + EdSegment.showDirected(p1, p2));
          if (MyMath.sideOfLine(p1, p2, starCenter) > 0 ^ fwd)
            break;
          double nextTheta = theta(p2);
          double sweptArc = Math.abs(MyMath.normalizeAngle(nextTheta
              - prevTheta));
          if (db && T.update())
            T.msg("sweptArc=" + deg(sweptArc) + " sweptAngle="
                + deg(sweptAngle) + EdSegment.showDirected(p1, p2)
                + "\n nextTheta=" + nextTheta + " prev=" + prevTheta);

          sweptAngle += sweptArc;
          prevTheta = nextTheta;
          nextVert = (nextVert + 1) % verts.size();
        }
        FPoint2 p = verts.getFPoint2(nextVert);

        double theta = theta(verts.getFPoint2Mod(nextVert - 1));
        if (db && T.update())
          T.msg("next turn point is " + nextVert + T.show(p) + ", theta "
              + Tools.fa2(theta) + ", sweptAngle=" + deg(sweptAngle));

        if (fwd) {
          while (sweptAngle > EPS) {
            double rng = currentEntry.range();
            double step = Math.min(sweptAngle, rng);

            if (false && db && T.update())
              T.msg("ccw, step=" + Tools.fa2(step) + " of range "
                  + Tools.fa2(rng));

            if (step < rng) {
              Ent e2 = currentEntry.split(currentEntry.theta() + step);
              currentEntry.increaseDepth();
              currentEntry = e2;
              if (db && T.update())
                T.msg("split current entry, now " + currentEntry);
            } else {
              currentEntry.increaseDepth();
              currentEntry = currentEntry.next();
            }
            sweptAngle -= step;
          }
        } else {
          while (sweptAngle > EPS) {

            currentEntry = currentEntry.prev();
            double rng = currentEntry.range();
            double step = Math.min(sweptAngle, rng);

            if (db && T.update())
              T.msg("cw, currEnt " + currentEntry + "\n sweptAngle="
                  + Tools.fa(sweptAngle) + "\n rng=" + Tools.fa(rng) + " step="
                  + Tools.fa(step));

            if (step < rng) {
              Ent split = currentEntry.split(currentEntry.theta() + rng - step);
              currentEntry = split;
              split.increaseDepth();
              if (db && T.update())
                T.msg("split current entry");
            } else {
              currentEntry.increaseDepth();
            }
            sweptAngle -= step;
          }
        }

        currVert = nextVert;
        fwd ^= true;

      } while (currVert != startVert);
    } while (false);
    processed = true;
  }

  private boolean processed;

  private double theta(FPoint2 pt) {
    return MyMath.normalizeAnglePositive(MyMath.polarAngle(starCenter, pt));
  }

  private static final double EPS = 1e-5;
  private static boolean same(FPoint2 p1, FPoint2 p2) {
    return p1.distance(p2) < EPS;
  }
  private static void join(Ent a, Ent b) {
    a.next = b;
    b.prev = a;
  }

  private FPoint2 starCenter;
  private Ent currentEntry;
  private Ent startEntry;
  private int starDepth;
  private int entId = 10;
  private static final boolean VERBOSE = false;

  private class Ent {

    public String toString() {
      StringBuilder sb = new StringBuilder();
      if (currentEntry == this)
        sb.append('*');
      sb.append(id);
      sb.append('>');
      sb.append(Tools.fa2(theta));
      sb.append(" d:" + depth);
      if (VERBOSE)
        sb.append(" (" + prev.id + "|" + next.id + ")");
      return sb.toString();
    }
    public boolean contains(double th) {
      boolean ret = false;

      th = MyMath.normalizeAnglePositive(th);
      do {
        if (th < theta)
          break;
        double t2 = next.theta;
        if (t2 == 0)
          t2 = Math.PI * 2;
        if (t2 <= theta)
          break;
        ret = true;

      } while (false);
      return ret;
    }

    public int depth() {
      return depth;
    }

    public Ent next() {
      return next;
    }
    public Ent prev() {
      return prev;
    }

    public double theta() {
      return theta;
    }

    public double range() {
      double th2 = next.theta;
      if (th2 == 0.0)
        th2 = Math.PI * 2;
      return th2 - theta;
    }

    public FPoint2 position(double offsetAngle, double radOffset) {

      double rad = radOffset + 30 + depth() * 1.5;
      FPoint2 pt = MyMath.ptOnCircle(starCenter, theta + offsetAngle, rad);
      return pt;
    }

    public void increaseDepth() {
      depth++;
      starDepth = Math.max(starDepth, depth);
    }
    public void setTheta(double th) {
      this.theta = MyMath.normalizeAnglePositive(th);
    }
    private int id;
    public Ent(double th) {
      this.id = entId++;
      setTheta(th);
    }

    /**
     * Split entry at a particular theta, if it is not the entry's
     * current theta
     * @param th theta to split at
     * @return new entry
     */
    public Ent split(double th) {
      if (db && T.update())
        T.msg("split " + this + " at theta=" + th + "\n" + seq());
      if (!contains(th))
        T.err("doesn't contain " + th + ":\n" + this + "\n next=" + next.theta);

      Ent e2 = this;
      if (th != theta) {
        e2 = new Ent(th);
        e2.depth = this.depth;
        Ent oldPrev = prev;
        Ent oldNext = next;

        if (oldPrev == oldNext) {
          join(this, e2);
          join(e2, oldNext);
        } else {
          join(this, e2);
          join(e2, oldNext);
        }
      }
      if (db && T.update())
        T.msg("split, sequence now\n" + seq() + "\n returning " + e2);
      return e2;
    }
    private String seq() {
      int inf = 20;
      StringBuilder sb = new StringBuilder("<");
      Ent e = this;
      for (int i = 0;; i++) {
        sb.append(e.id + "(" + e.prev.id + "|" + e.next.id + ") ");
        e = e.next;
        if (e == this)
          break;
        if (i == inf) {
          sb.append("****");
          break;
        }
      }
      return sb.toString();
    }

    private double theta;
    private Ent next;
    private Ent prev;
    private int depth;
  }

}
