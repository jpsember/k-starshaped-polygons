package starshaped;

import java.util.*;
import testbed.*;
import base.*;

public class VisPoly implements Globals {
  private static final double EPS = 1e-3;

  private static EdPolygon p(DArray list, int i) {
    return (EdPolygon) list.get(i);
  }

  // public static boolean arcTest;

  /**
   * Calculate visibility polygon for a vertex of a set of polygons
   * @param polys set of polygons
   * @param focusPoly index of polygon containing focus vertex, or -1 if arb pt
   * @param focusVert index of focus vertex, or -1 if arb pt
   * @param focus focus point, if arb pt
   * @param arcTest 
   * @return visibility polygon, or null if it is empty
   */
  public static EdPolygon calcForSet(DArray polys, int focusPoly,
      int focusVert, FPoint2 focus) {

    final boolean db = C.vb(VisOper.DB_CALCVISPOLY);

    if (focusPoly >= 0)
      focus = p(polys, focusPoly).getPoint(focusVert);

    if (db && T.update())
      T.msg("VisPoly.calcForSet" + T.show(polys) + T.show(focus));

    // segments comprising envelope
    DArray env = new DArray();

    DArray[] p = new DArray[2];
    VisEventQueue[] q = new VisEventQueue[2];
    Slab[] slabs = new Slab[2];
    Iterator[] iterator = new Iterator[2];
    SegEvent[] pending = new SegEvent[2];
    DArray[] removeList = new DArray[2];
    boolean[] procFlags = new boolean[2];

    for (int i = 0; i < 2; i++) {
      if (i == 0)
        p[i] = polys;
      else {
        p[i] = new DArray();
        for (int j = 0; j < polys.size(); j++)
          p[i].add(getRotated180(p(polys, j), focus));
      }
      q[i] = new VisEventQueue(focus);
      for (int j = 0; j < polys.size(); j++)
        q[i].addEdgesFrom(p(p[i], j), i);
      if (db && T.update())
        T.msg("added segments to queue #" + i + T.show(q[i]));
      iterator[i] = q[i].iterator();
      removeList[i] = new DArray();
    }

    DArray stopPts;
    {
      DArray s0 = calcSlabAngles(q[0], focusPoly >= 0 ? p(p[0], focusPoly)
          : null, focusVert, focus);
      DArray s180 = calcSlabAngles(q[1], focusPoly >= 0 ? p(p[1], focusPoly)
          : null, focusVert, focus);
      stopPts = combineAngles(s0, s180);
    }

    for (Iterator thetaIter = stopPts.iterator(); thetaIter.hasNext();) {
      double nextTheta = ((Double) thetaIter.next()).doubleValue();

      for (int i = 0; i < 2; i++) {
        if (slabs[i] == null)
          slabs[i] = new Slab(i, focusPoly >= 0 ? p(p[i], focusPoly) : null,
              focusVert, focus, stopPts.getDouble(0), nextTheta);
        else
          slabs[i].advanceThetaTo(nextTheta);
      }

      procFlags[0] = procFlags[1] = true;
      while (procFlags[0] || procFlags[1]) {
        for (int i = 0; i < 2; i++) {
          if (pending[i] == null) {
            if (iterator[i].hasNext())
              pending[i] = (SegEvent) iterator[i].next();
            else
              procFlags[i] = false;
          }

          if (pending[i] != null) {
            SegEvent evt = pending[i];
            double theta = evt.seg().theta(evt.endPt());
            if (evt.endPt() == 0) {
              if (theta > slabs[i].theta0() + EPS)
                procFlags[i] = false;
              else {
                slabs[i].addSegment(evt.seg());
                pending[i] = null;
              }
            } else {
              if (theta > slabs[i].theta1() + EPS)
                procFlags[i] = false;
              else {
                removeList[i].add(evt.seg());
                pending[i] = null;
              }
            }
          }

        }
      }

      extractKSeg(slabs[0], slabs[1], env, db);

//      if (db && T.update())
//        T.msg("VisPoly.calcForSet" + T.show(slabs[0]) + T.show(slabs[1])
//            + T.show(env, MyColor.cRED, STRK_NORMAL, MARK_DISC));

      for (int i = 0; i < 2; i++) {
        slabs[i].removeSegments(removeList[i]);
        removeList[i].clear();
      }
    }

    EdPolygon vis = envToPoly(focus, env);

    if (db && T.update())
      T.msg("envToPoly" + T.show(vis, MyColor.cRED, -1, MARK_DISC));
    return vis;
  }

  /**
   * Calculate visibility polygon
   * @param poly polygon
   * @param focusVert index of focus vertex, or -1 if not a vertex
   * @param focus focus point, if not a vertex
   * @return visibility polygon, or null if it is empty
   */
  public static EdPolygon calc(EdPolygon poly, int focusVert, FPoint2 focus) {

    final boolean db = C.vb(VisOper.DB_CALCVISPOLY);

    if (focusVert >= 0)
      focus = poly.getPoint(focusVert);

    if (db && T.update())
      T.msg("VisPoly.calc" + T.show(poly) + T.show(focus));

    // segments comprising envelope
    DArray env = new DArray();

    EdPolygon[] p = new EdPolygon[2];
    VisEventQueue[] q = new VisEventQueue[2];
    Slab[] slabs = new Slab[2];
    Iterator[] iterator = new Iterator[2];
    SegEvent[] pending = new SegEvent[2];
    DArray[] removeList = new DArray[2];
    boolean[] procFlags = new boolean[2];

    for (int i = 0; i < 2; i++) {
      p[i] = (i == 0) ? poly : getRotated180(poly, focus);
      q[i] = new VisEventQueue(focus);
      q[i].addEdgesFrom(p[i], i);
      if (db && T.update())
        T.msg("added segments to queue #" + i + T.show(q[i]));
      iterator[i] = q[i].iterator();
      removeList[i] = new DArray();
    }

    DArray stopPts;
    {
      DArray s0 = calcSlabAngles(q[0], p[0], focusVert, focus);
      DArray s180 = calcSlabAngles(q[1], p[1], focusVert, focus);
      stopPts = combineAngles(s0, s180);
    }

    for (Iterator thetaIter = stopPts.iterator(); thetaIter.hasNext();) {
      double nextTheta = ((Double) thetaIter.next()).doubleValue();

      for (int i = 0; i < 2; i++) {
        if (slabs[i] == null)
          slabs[i] = new Slab(i, p[i], focusVert, focus, stopPts.getDouble(0),
              nextTheta);
        else
          slabs[i].advanceThetaTo(nextTheta);
      }

      procFlags[0] = procFlags[1] = true;
      while (procFlags[0] || procFlags[1]) {
        for (int i = 0; i < 2; i++) {
          if (pending[i] == null) {
            if (iterator[i].hasNext())
              pending[i] = (SegEvent) iterator[i].next();
            else
              procFlags[i] = false;
          }

          if (pending[i] != null) {
            SegEvent evt = pending[i];
            double theta = evt.seg().theta(evt.endPt());
            if (evt.endPt() == 0) {
              if (theta > slabs[i].theta0() + EPS)
                procFlags[i] = false;
              else {
                slabs[i].addSegment(evt.seg());
                pending[i] = null;
              }
            } else {
              if (theta > slabs[i].theta1() + EPS)
                procFlags[i] = false;
              else {
                removeList[i].add(evt.seg());
                pending[i] = null;
              }
            }
          }

        }
      }

      extractKSeg(slabs[0], slabs[1], env, db);

//      if (db && T.update())
//        T.msg("VisPoly.calc" + T.show(slabs[0]) + T.show(slabs[1])
//            + T.show(env, MyColor.cRED, STRK_NORMAL, MARK_DISC)); // + show180());

      for (int i = 0; i < 2; i++) {
        slabs[i].removeSegments(removeList[i]);
        removeList[i].clear();
      }
    }

    EdPolygon vis = envToPoly(focus, env);

    if (db && T.update())
      T.msg("envToPoly" + T.show(vis, MyColor.cRED, -1, MARK_DISC));
    return vis;
  }

  private static EdPolygon getRotated180(EdPolygon p, FPoint2 origin) {
    EdPolygon r = new EdPolygon();
    r.setLabel(p.getLabel() + "*");
    for (int i = 0; i < p.nPoints(); i++) {
      FPoint2 pt = p.getPoint(i);
      pt = Util.rotate180(pt, origin);
      r.addPoint(pt);
    }
    return r;
  }

  private static DArray combineAngles(DArray a0, DArray a1) {
    DArray r = new DArray();
    double prev = -1;
    int i0 = 0, i1 = 0;
    while (true) {
      double next = -1;
      double n0 = -1, n1 = -1;

      if (i0 < a0.size())
        n0 = a0.getDouble(i0);
      if (i1 < a1.size())
        n1 = a1.getDouble(i1);
      if (n0 < 0 && n1 < 0)
        break;
      if (n1 < 0 || (n0 >= 0 && n0 < n1)) {
        next = n0;
        i0++;
      } else {
        next = n1;
        i1++;
      }
      if (next > prev) {
        r.addDouble(next);
      }
      prev = next;
    }
    return r;
  }

  /**
   * Extract k-level envelope from pair of slabs.
   * Each slab contains at most k segments, so this is O(1) if k is constant.
   * @param s0 slab for original polygon
   * @param s180 slab of polygon rotated 180 degrees 
   * @param env where to store segments
   */
  private static void extractKSeg(Slab s0, Slab s180, DArray env, boolean db) {

    int k = SSMain.k();
    int kAdjust = 0;
    if (C.vb(VisOper.ADJUSTK)) 
    {
      kAdjust = s0.kAdjust();
      k -= kAdjust;
    }

    int s18s = s180.size();

    int kVal = k - s18s;
    Seg sClip = null;

    if (sClip == null) {
      Seg sFound = null;
      int i = 0;
      for (Iterator it = s0.iterator(); i < kVal && it.hasNext(); i++) {
        sFound = (Seg) it.next();
      }
      double radius = VisOper.unboundedRadius();

      if (radius == 0 || i == kVal) {
        if (sFound != null) {
          sClip = sFound.clipTo(s0.theta0(), s0.theta1());
        }
      } else {
        FPoint2 orig = s0.getOrigin();
        Seg sinf = new Seg(0, orig, MyMath
            .ptOnCircle(orig, s0.theta0(), radius), MyMath.ptOnCircle(orig, s0
            .theta1(), radius));
        sClip = sinf;
      }

    }
    if (sClip != null)
      env.add(sClip);

    if (db && T.update())
      T.msg("extracted seg E(A)_k" + T.show(s0) + T.show(s180)
          + T.show(sClip, MyColor.cRED, STRK_THICK, -1) 
          //+ " kAdjust=" + kAdjust
          //+ " s180 size=" + s180.size()
          );

  }
  private static EdPolygon envToPoly(FPoint2 origin, DArray env) {
    final boolean db = C.vb(VisOper.DB_ENVTOPOLY);

    if (db && T.update())
      T.msg("envToPoly" + T.show(env, MyColor.cRED) + T.show(origin));

    EdPolygon p = null;
    if (!env.isEmpty()) {
      p = new EdPolygon();
      p.setFlags(EdPolygon.FLG_OPEN);
      Seg sPrev = (Seg) env.last();

      for (int i = 0; i < env.size(); i++) {
        Seg s = (Seg) env.get(i);

        if (db && T.update())
          T.msg("prevSeg, currSeg" + T.show(sPrev, MyColor.cDARKGREEN)
              + T.show(s, MyColor.cBLUE)
              + T.show(p, MyColor.cRED, -1, MARK_DISC));

        // see if previous segment is connected to this one
        FPoint2 epPrev = sPrev.vertex(1);
        FPoint2 ep = s.vertex(0);
        if (!Util.same(epPrev, ep, Util.EPS * 1000)) {
          if (db && T.update())
            T.msg("not same vertex:\n" + epPrev + "\n" + ep + "\n dist="
                + epPrev.distance(ep));
          double thPrev = sPrev.theta(1);
          double thCurr = s.theta(0);
          if (db && T.update())
            T.msg("seeing if thPrev=thCurr:\n" + thPrev + "\n" + thCurr);
          if (Math.abs(MyMath.normalizeAngle(thPrev - thCurr)) > Util.EPS * 100) {
            p.addPoint(epPrev);
            p.addPoint(origin);
            if (db && T.update())
              T.msg("filling gap by trip through origin"
                  + EdSegment.showDirected(epPrev, origin)
                  + T.show(p, MyColor.cRED, -1, MARK_DISC));
          } else {
            p.addPoint(epPrev);
            if (db && T.update())
              T.msg("filling gap by connecting prev to curr" + T.show(epPrev)
                  + T.show(p, MyColor.cRED, -1, MARK_DISC));
          }
        }
        p.addPoint(ep);
        sPrev = s;
      }
      p.clearFlags(EdPolygon.FLG_OPEN);
    }
    if (db && T.update())
      T.msg("envToPoly returning" + T.show(p, MyColor.cRED, -1, MARK_DISC));
    return p;
  }

  /**
   * Determine angles separating slabs.
   * Inserts angles corresponding to edges incident to origin,
   * so k adjustment is consistent throughout each slab.
   * @param evtQueue
   * @param poly polygon being scanned
   * @param vert origin vertex being scanned
   * @return array of sorted angles
   */
  private static DArray calcSlabAngles(VisEventQueue evtQueue, EdPolygon poly,
      int vert, FPoint2 origin) {

    final boolean db = false;

    if (origin == null)
      origin = poly.getPoint(vert);
    double aux0 = Math.PI * 3;
    double aux1 = aux0;
    if (vert >= 0) {
      aux0 = Util.theta(poly.getPointMod(vert - 1), origin);
      aux1 = Util.theta(poly.getPointMod(vert + 1), origin);
      if (aux0 > aux1) {
        double tmp = aux0;
        aux0 = aux1;
        aux1 = tmp;
      }
    }

    DArray stopPts = new DArray();
    double prevTheta = 0;
    stopPts.addDouble(prevTheta);

    Iterator it = evtQueue.iterator();
    while (true) {
      double theta = Math.PI * 2;
      if (it.hasNext()) {
        SegEvent evt = (SegEvent) it.next();
        Seg seg = evt.seg();
        theta = seg.theta(evt.endPt());
      }

      if (theta > aux0 + EPS) {
        if (!Util.same(aux0, prevTheta, EPS))
          stopPts.addDouble(aux0);
        prevTheta = aux0;
        aux0 = Math.PI * 3;
      }
      if (theta > aux1 + EPS) {
        if (!Util.same(aux1, prevTheta, EPS))
          stopPts.addDouble(aux1);
        prevTheta = aux1;
        aux1 = Math.PI * 3;
      }

      if (db && T.update())
        T.msg("curr theta=" + theta + "\nprev theta=" + prevTheta);
      if (Util.same(theta, prevTheta, EPS)) {
        if (!it.hasNext())
          break;
        continue;
      }
      if (theta < prevTheta)
        T.err("unexp");
      if (db && T.update())
        T.msg("adding new stop point:" + theta);
      stopPts.addDouble(theta);
      prevTheta = theta;
    }

    if (stopPts.size() < 2)
      T.err("unexpected");
    return stopPts;
  }
}
