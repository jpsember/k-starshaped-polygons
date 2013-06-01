package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class VRgnOper implements TestBedOperation, Globals {

  /*! .enum  .public 4200   unboundedradius vertnumber printedges dbedgecompare dbfilter plotevents
    dbbuildedges plotneg earlylate withfilter
  */

    public static final int UNBOUNDEDRADIUS  = 4200;//!
    public static final int VERTNUMBER       = 4201;//!
    public static final int PRINTEDGES       = 4202;//!
    public static final int DBEDGECOMPARE    = 4203;//!
    public static final int DBFILTER         = 4204;//!
    public static final int PLOTEVENTS       = 4205;//!
    public static final int DBBUILDEDGES     = 4206;//!
    public static final int PLOTNEG          = 4207;//!
    public static final int EARLYLATE        = 4208;//!
    public static final int WITHFILTER       = 4209;//!
/*!*/

  public static EdPolygon constructVRgn(EdPolygon poly, int vertexNumber) {
    singleton.db = false;
    singleton.initAlg();
    EdPolygon ret = null;
    try {
      singleton.runAlg(poly, vertexNumber);
      ret = singleton.vPoly;
    } catch (Throwable t) {
      Tools.warn("ignoring exception: " + t);
    }
    return ret;
  }

  public static VRgnOper singleton = new VRgnOper();
  private VRgnOper() {
  }
  public void addControls() {
    C.sOpenTab("V-Rgn");
    {
      C.sStaticText("Constructs the V-region for an individual vertex");

      C.sOpen();
      {
        C.sIntSpinner(VERTNUMBER, "vertex", null, 0, 50, 0, 1);
        C.sIntSpinner(UNBOUNDEDRADIUS, "unbnd radius", null, 0, 200, 120, 1);
        C.sCheckBox(PRINTEDGES, "print edges", null, false);
        C.sCheckBox(PLOTEVENTS, "plot events", null, false);
        C.sCheckBox(DBEDGECOMPARE, "db edge sort", "trace edge sort function",
            false);
        C.sCheckBox(DBBUILDEDGES, "db build edges",
            "trace building of edges,\nand active edge list events", false);
        C.sCheckBox(WITHFILTER, "filter",
            "include final filtering of vertices", true);
        C.sCheckBox(DBFILTER, "db filter", "trace final filtering of vertices",
            false);
        C.sCheckBox(PLOTNEG, "plot complement", "plot complement of poly",
            false);
        C.sCheckBox(EARLYLATE, "early/late",
            "additional test for non-general position polygons", true);
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

  private static class MyPoint extends FPoint2 {
    public MyPoint(FPoint2 s, boolean inf) {
      super(s);
      this.inf = inf;
    }
    private boolean inf;
    public boolean inf() {
      return inf;
    }
  }

  /**
   * If there are delayed inserts, 
   * add them
   */
  private void flushDelayedInserts() {
    while (!delayedInserts.isEmpty()) {
      addActiveEdge((Edge) delayedInserts.pop(), "delayed insert");
    }
  }

  private void advanceTo(double nextAng) {
    final boolean db = true;

    // unless we haven't extracted any points yet,
    // extract boundary point for start of this wedge.

    if (!pts.isEmpty()) {
      MyPoint bndPt = extractBoundaryPoint();
      if (db && T.update()) {
        T.msg("extracted boundary point " + T.show(bndPt));
      }

      pts.add(bndPt);
    }

    flushDelayedInserts();

    MyPoint[] interiorEdge = extractInteriorEdge(nextAng);
    if (db && T.update()) {
      T.msg("extracted interior edge "
          + EdSegment.show(interiorEdge[0], interiorEdge[1], MyColor.cRED,
              STRK_THICK));
    }
    pts.add(interiorEdge[0]);
    pts.add(interiorEdge[1]);

    setWedgeLine(nextAng);
  }

  private DArray delayedInserts;

  private boolean db;

  private void initAlg() {
    sourceVertLoc = null;
    edgeEvents = null;
    polygon = null;
    vPoly = null;
    vComp = null;
    wedgeLine = null;
    wedgeAngle = -1;
    nextWedgeAngle = -1;
    activeEdges = null;
    pts = new DArray();
    dbEdgeSort = db && C.vb(DBEDGECOMPARE);
    delayedInserts = new DArray();
    highlightPoints = false;

    EdgeEvent.resetIds();
  }

  public void runAlgorithm() {

    db = true;
    initAlg();

    EdPolygon[] polys = SSMain.getPolygons();

    if (polys.length == 0)
      return;

    runAlg(polys[0], C.vi(VERTNUMBER) % polys[0].nPoints());

  }
  private void runAlg(EdPolygon pol, int vIndex) {

    polygon = pol;

    sourceVertexIndex = vIndex % polygon.nPoints();
    sourceVertLoc = polygon.getPoint(sourceVertexIndex);

    setWedgeLine(0);
    buildVertexAndEdgeLists();

    buildEdgeEvents();

    EdgeEvent prev = null;
    boolean doEarlyLate = C.vb(EARLYLATE);

    while (true) {
      if (edgeEvents.isEmpty())
        break;
      EdgeEvent evt2 = (EdgeEvent) edgeEvents.first();
      edgeEvents.remove(evt2);
      nextWedgeAngle = evt2.getTheta(sourceVertLoc);

      if (db && T.update())
        T.msg("next edgeEvent" + T.show(evt2) + ": " + evt2);

      boolean special = doEarlyLate
          && (prev != null && prev.getType() == EdgeEvent.EVT_INSERTEARLY && evt2
              .getType() == EdgeEvent.EVT_REMOVELATE);
      prev = evt2;

      if (nextWedgeAngle > wedgeAngle || special)
        advanceTo(nextWedgeAngle);

      switch (evt2.getType()) {
      case EdgeEvent.EVT_REMOVEEARLY:
      case EdgeEvent.EVT_REMOVELATE:
        removeActiveEdge(evt2.getEdge(), evt2.getTypeString());
        break;
      case EdgeEvent.EVT_INSERTEARLY:
        addActiveEdge(evt2.getEdge(), evt2.getTypeString());
        break;
      case EdgeEvent.EVT_INSERTLATE:
        delayedInserts.add(evt2.getEdge());
        break;
      }
    }
    flushDelayedInserts();

    // advance angle to zero
    advanceTo(Math.PI * 2 - .001);
    // throw out the active edge list, now that we're done with it
    activeEdges = null;

    // duplicate the last point, so we end with the
    // same point we started with.  It will be removed during
    // filtering.
    if (pts.size() > 0)
      pts.add(pts.get(0));

    wedgeLine = null;
    if (C.vb(WITHFILTER))
      filter();
    vPoly = new EdPolygon(pts);

    if (db && C.vb(PLOTNEG)) {
      vComp = new EdPolygon();
      Util.externalFaceOf(vPoly, vComp);
    }

    pts = null;
  }

  /**
   * Filter points that are the same as their predecessors,
   * or that are collinear with their immediate neighbors
   * (or sufficiently close, if all three are pts at infinity)
   */
  private void filter() {

    final boolean db = C.vb(DBFILTER);
    if (db && T.update())
      T.msg("filter" + T.show(pts));
    highlightPoints = true;

    if (pts.size() > 1) {
      if (db && T.update())
        T.msg("filter: duplicated consecutive points");
      DArray a2 = new DArray();
      MyPoint p0 = (MyPoint) pts.last();

      for (int i = 0; i < pts.size(); i++) {

        MyPoint p1 = (MyPoint) pts.get(i);
        if (p0 == null || p1.distance(p0) > 1e-4) {
          a2.add(p1);
          p0 = p1;
        } else {
          if (db && T.update())
            T.msg("consecutive points " + i + " and " + (i + 1)
                + " are equal, removing one" + T.show(p0));
        }

      }
      pts = a2;
    }

    if (pts.size() >= 3) {

      DArray filteredPts = new DArray();
      MyPoint p0 = null, p1 = null;

      for (int i = 0; i < pts.size(); i++) {

        MyPoint p2 = (MyPoint) pts.getMod(i);

        boolean include = true;

        do {
          if (p0 == null)
            break;

          final double EPS = 1e-4;
          final double EPS2 = 3;

          LineEqn ln = new LineEqn(p0, p2);
          // it is possible for the line to be undefined,
          // if the polygon went in and out again, for instance
          if (!ln.defined())
            break;

          double t0 = ln.parameterFor(p0);
          double t2 = ln.parameterFor(p2);
          double t1 = ln.parameterFor(p1);
          if (t1 < Math.min(t0, t2) || t1 > Math.max(t0, t2))
            break;

          double dist = ln.distanceFrom(p1);

          double toler = (p0.inf() && p1.inf() && p2.inf()) ? EPS2 : EPS;
          if (db && T.update())
            T.msg("collinear test " + EdSegment.showDirected(p0, p2)
                + T.show(p0, MyColor.cDARKGREEN) + T.show(p1)
                + T.show(p2, MyColor.cDARKGREEN) + " distance=" + Tools.f(dist)
                + " < tolerance " + Tools.f(dist) + "?");
          if (dist > toler)
            break;
          include = false;
        } while (false);

        if (include) {
          filteredPts.add(p2);
          p0 = p1;
          p1 = p2;
        } else {
          // pop p1
          MyPoint popped = (MyPoint) filteredPts.pop();
          if (db && T.update())
            T.msg("filtering out" + T.show(popped));
          p1 = p2;
          filteredPts.add(p2);
        }
      }
      pts = filteredPts;
    }
    highlightPoints = false;

  }

  private void removeActiveEdge(Edge e, String reason) {
    final boolean db = C.vb(DBBUILDEDGES);

    if (db && T.update())
      T.msg("removeActiveEdge (" + reason + ")\n" + e + T.show(e));

    boolean found = activeEdges.remove(e);
    if (!found)
      T.err("couldn't find edge in list:\n" + e + T.show(e) + " in list:\n"
          + T.show(activeEdges) + DArray.toString(activeEdges, true));

  }
  private void setWedgeLine(double theta) {
    wedgeAngle = theta;
    wedgeLine = new LineEqn(sourceVertLoc, theta);
  }

  private LineEqn lineAtInfinity(double theta) {
    int rad = C.vi(UNBOUNDEDRADIUS);

    FPoint2 p0 = MyMath.ptOnCircle(sourceVertLoc, theta, rad);
    LineEqn ln = new LineEqn(p0, theta + Math.PI / 2);
    return ln;
  }

  private MyPoint[] extractInteriorEdge(double nextAngle) {
    final boolean db = false;
    if (db && T.update())
      T.msg("extracting interior edge");

    double angle = (nextAngle + wedgeAngle) * .5;
    int k = SSMain.k() + 2 - 1; // -1 since paper counts from 1, not 0
    Edge foundEdge = null;
    for (Iterator it = activeEdges.iterator(); foundEdge == null
        && it.hasNext();) {
      Edge e = (Edge) it.next();
      if (k == 0)
        foundEdge = e;
      k--;
    }

    LineEqn ln = null;

    boolean newIsInf = false;
    if (foundEdge == null) {
      ln = lineAtInfinity(angle);
      newIsInf = true;
    } else {
      ln = foundEdge.getLine();
    }

    MyPoint[] ret = new MyPoint[2];

    // calculate intersections of line with radials
    {
      FPoint2 p0 = ln.pt(0);
      FPoint2 p1 = ln.pt(10);

      for (int i = 0; i < 2; i++) {

        FPoint2 ipt = radialIntersect(i == 0 ? wedgeAngle : nextAngle, p0, p1,
            false);
        if (ipt == null)
          ipt = sourceVertLoc;

        ret[i] = new MyPoint(ipt, newIsInf);
      }
    }

    return ret;
  }

  private FPoint2 radialIntersect(double theta, FPoint2 p0, FPoint2 p1,
      boolean mustExist) {
    FPoint2 ipt = MyMath.linesIntersection(p0, p1, sourceVertLoc, MyMath
        .ptOnCircle(sourceVertLoc, theta, 10), null);
    if (ipt == null && mustExist)
      T.err("couldn't find intersection point");
    return ipt;
  }

  private MyPoint extractBoundaryPoint() {
    final boolean db = false;
    if (db && T.update())
      T.msg("extracting boundary point");

    int k = SSMain.k() + 2 - 1; // -1 since paper counts from 1, not 0
    Edge foundEdge = null;
    for (Iterator it = activeEdges.iterator(); foundEdge == null
        && it.hasNext();) {
      Edge e = (Edge) it.next();
      if (k == 0) {
        foundEdge = e;
      }
      k--;
    }

    LineEqn ln = null;

    boolean newIsInf = false;

    if (foundEdge == null) {
      ln = lineAtInfinity(wedgeAngle);
      newIsInf = true;
    } else
      ln = foundEdge.getLine();

    // calculate intersection of line with radial
    FPoint2 ipt = radialIntersect(wedgeAngle, ln.pt(0), ln.pt(1), true);

    MyPoint ipt2 = new MyPoint(ipt, newIsInf);

    if (db && T.update()) {
      T.msg("extracted point " + T.show(ipt2));
    }
    return ipt2;
  }

  private Vertex getVert(int i) {
    return verts[MyMath.mod(i, verts.length)];
  }

  private void buildVertexAndEdgeLists() {
    final boolean db = false;

    int s = polygon.nPoints();
    verts = new Vertex[s];
    for (int i = 0; i < s; i++)
      verts[i] = new Vertex(sourceVertLoc, i, polygon.getPoint(i));

    double pAng = 0;
    if (s > 1) {
      // determine perturb angle for these edges.
      // This must be less than the smallest angle of 
      // one of the four wedges induced by the incident edges and their duals.
      // Just sort the vertices and their duals by polar angle, and choose e.g. half the smallest difference.
      DArray arr = new DArray();
      for (int i = 0; i < s; i++) {
        if (i == sourceVertexIndex)
          continue;
        arr.addDouble(new Double(verts[i].angle()));
        arr.addDouble(new Double(MyMath.normalizeAnglePositive(verts[i].angle()
            + Math.PI)));

      }
      arr.sort(new Comparator() {

        public int compare(Object o1, Object o2) {
          return Double.compare(((Double) o1).doubleValue(), ((Double) o2)
              .doubleValue());
        }
      });
      pAng = MyMath.normalizeAnglePositive(arr.getDouble(0)
          - arr.getDouble(arr.size() - 1));
      if (pAng == 0)
        pAng = Math.PI;

      for (int i = 1; i < arr.size(); i++) {
        double pAng2 = arr.getDouble(i) - arr.getDouble(i - 1);
        if (pAng2 > 0)
          pAng = Math.min(pAng, pAng2);
      }
    }
    pAng *= .3;

    if (db && T.update())
      T.msg("edge perturb angle=" + Tools.fa2(pAng));

    Edge.setPerturb(pAng);
    edges = new Edge[s * 2];
    for (int i = 0, j = 0; i < s; i++) {
      edges[j++] = new Edge(sourceVertLoc, getVert(i), getVert(i + 1), false);
      edges[j++] = new Edge(sourceVertLoc, getVert(i + 1), getVert(i), true);
    }
    for (int i = 0; i < edges.length; i++) {
      Edge.join(edges[i], edges[(i + 2) % edges.length]);
    }

  }

  private void buildEdgeEvents() {
    final boolean db = C.vb(DBBUILDEDGES);

    activeEdges = buildEdgeSet();

    edgeEvents = new TreeSet(new Comparator() {

      public int compare(Object o1, Object o2) {
        EdgeEvent e1 = (EdgeEvent) o1;
        EdgeEvent e2 = (EdgeEvent) o2;

        double a1 = e1.getTheta(sourceVertLoc);
        double a2 = e2.getTheta(sourceVertLoc);

        int sign = MyMath.sign(a1 - a2);
        if (sign == 0) {
          sign = e1.getType() - e2.getType();

        }
        if (sign == 0)
          sign = e1.getId() - e2.getId();
        return sign;
      }
    });

    DArray sEdges = new DArray();

    for (int i = 0; i < edges.length; i++) {
      Edge e = edges[i];

      // edges incident with source vertex are handled separately
      if (e.getVert(0).index() == sourceVertexIndex
          || e.getVert(1).index() == sourceVertexIndex) {
        sEdges.add(e);
        continue;
      }
      if (db && T.update())
        T.msg("next polygon edge" + T.show(e));
      if (!e.isValid())
        continue;

      EdgeEvent e1 = new EdgeEvent(e, EdgeEvent.EVT_INSERTEARLY, null, null);

      if (db && T.update())
        T.msg("adding event:" + e1 + T.show(e1));
      addEvt(e1);

      // if edge crosses x-axis, add to initial active edge list as well
      if (e.crosses()) {
        if (db && T.update())
          T.msg("edge crosses positive x-axis, adding as active edge: " + e
              + T.show(e));
        addActiveEdge(e, "edge crosses initial wedge boundary");
      }

      // Determine if we are to remove this edge early or late.
      // Remove it early if its forward vertex is connected to a valid edge that doesn't 
      // share the same forward vertex.

      Vertex v = e.getFwdVertex();

      Edge e2 = e.getNext();
      if (e2.getVert(0) != v && e2.getVert(1) != v)
        e2 = e.getPrev();

      boolean early = e2.isValid() && e2.getFwdVertex() != v;

      EdgeEvent evt = new EdgeEvent(e, early ? EdgeEvent.EVT_REMOVEEARLY
          : EdgeEvent.EVT_REMOVELATE, null, null);
      if (db && T.update())
        T.msg("adding removal event: " + evt + T.show(evt));
      addEvt(evt);
    }
    processIncidentEdges(sEdges);
  }

  /**
   * process edges incident with source vertex
   */
  private void processIncidentEdges(DArray sEdges) {
    final boolean db = true;

    if (sEdges.size() != 4)
      throw new IllegalStateException();
    sEdges.sort(new Comparator() {

      public int compare(Object o1, Object o2) {
        Edge e1 = (Edge) o1;
        Edge e2 = (Edge) o2;
        return MyMath.sign(e1.angleFromSource() - e2.angleFromSource());
      }
    });

    Edge[] ie = (Edge[]) sEdges.toArray(Edge.class);

    if (db && T.update())
      T.msg("processing edges incident with source vertex" + T.show(ie));

    // there are two cases: wedge starts with both edges to same side,
    // or to different sides.
    if (ie[0].isDual() == ie[1].isDual()) {
      if (db && T.update())
        T.msg("both to same side of x-axis" + T.show(ie[0]) + T.show(ie[1]));

      Edge S1 = ie[0], S2 = ie[1], S1t = ie[2], S2t = ie[3];

      addActiveEdge(S1, "incident edge S1");
      addActiveEdge(S2, "incident edge S2");

      if (db && T.update())
        T.msg("added S1,S2 to initial list" + T.show(S1) + T.show(S2));

      addEvt(new EdgeEvent(S1, EdgeEvent.EVT_REMOVEEARLY, S1.nonSrcPt(), "-S1"));
      addEvt(new EdgeEvent(S2, EdgeEvent.EVT_REMOVEEARLY, S2.nonSrcPt(), "-S2"));
      addEvt(new EdgeEvent(S1t, EdgeEvent.EVT_INSERTEARLY, S2.nonSrcPt(),
          "+S1*"));
      addEvt(new EdgeEvent(S2t, EdgeEvent.EVT_INSERTLATE, S2.nonSrcPt(), "+S2*"));
      addEvt(new EdgeEvent(S1t, EdgeEvent.EVT_REMOVEEARLY,
          dual(S1t.nonSrcPt()), "-S1*"));
      addEvt(new EdgeEvent(S2t, EdgeEvent.EVT_REMOVEEARLY,
          dual(S2t.nonSrcPt()), "-S2*"));
      addEvt(new EdgeEvent(S1, EdgeEvent.EVT_INSERTEARLY, dual(S2t.nonSrcPt()),
          "+S1"));
      addEvt(new EdgeEvent(S2, EdgeEvent.EVT_INSERTLATE, dual(S2t.nonSrcPt()),
          "+S2"));

    } else {
      if (db && T.update())
        T.msg("different sides of x-axis" + T.show(ie[0]) + T.show(ie[2]));
      Edge S1 = ie[0], S2t = ie[1], S1t = ie[2], S2 = ie[3];
      addActiveEdge(S1, "incident edge S1");
      if (db && T.update())
        T.msg("added S1 to initial list" + T.show(S1));
      addEvt(new EdgeEvent(S1, EdgeEvent.EVT_REMOVEEARLY, S1.nonSrcPt(), "-S1"));

      addEvt(new EdgeEvent(S2t, EdgeEvent.EVT_INSERTEARLY, S1.nonSrcPt(),
          "+S2*"));
      addEvt(new EdgeEvent(S1t, EdgeEvent.EVT_INSERTLATE, S1.nonSrcPt(), "+S1*"));
      addEvt(new EdgeEvent(S2t, EdgeEvent.EVT_REMOVEEARLY,
          dual(S2t.nonSrcPt()), "-S2*"));

      addEvt(new EdgeEvent(S1t, EdgeEvent.EVT_REMOVEEARLY,
          dual(S1t.nonSrcPt()), "-S1*"));
      addEvt(new EdgeEvent(S2, EdgeEvent.EVT_INSERTEARLY, dual(S1t.nonSrcPt()),
          "+S2"));
      addEvt(new EdgeEvent(S1, EdgeEvent.EVT_INSERTLATE, dual(S1t.nonSrcPt()),
          "+S1"));

      addEvt(new EdgeEvent(S2, EdgeEvent.EVT_REMOVEEARLY, S2.nonSrcPt(), "-S2"));

    }

  }

  private void addActiveEdge(Edge e, String reason) {
    final boolean db = C.vb(DBBUILDEDGES);

    if (db && T.update())
      T.msg("addActiveEdge (" + reason + ")\n" + T.show(e) + e);
    boolean added = activeEdges.add(e);
    if (!added)
      T.err("unable to add edge " + e + " to set:\n"
          + DArray.toString(activeEdges, true));
  }

  private FPoint2 dual(FPoint2 v0) {
    final boolean db = false;

    FPoint2 ret = new FPoint2(sourceVertLoc.x * 2 - v0.x, sourceVertLoc.y * 2
        - v0.y);
    if (db && T.update())
      T.msg("dual of vertex " + v0 + T.show(v0) + " is " + ret + T.show(ret));
    return ret;
  }

  private void addEvt(EdgeEvent e) {
    boolean added = edgeEvents.add(e);
    if (!added)
      T.err("unable to add event " + e + " to set:\n"
          + DArray.toString(edgeEvents, true));

  }

  public void paintView() {

    // draw a circle around our source vertex
    if (sourceVertLoc != null) {
      V.pushColor(MyColor.cRED);
      V.drawCircle(sourceVertLoc, V.getScale() * 1.2);
      V.pop();
    }

    // draw the polygon in gray if we're in the
    // middle of the algorithm.

    if (polygon == null || activeEdges == null)
      Editor.render();
    else
      polygon.render(MyColor.cDARKGRAY, STRK_THIN, -1);

    // plot wedge boundary lines
    if (wedgeLine != null) {
      for (int pass = 0; pass < 2; pass++) {
        double ang = wedgeAngle;
        if (pass != 0) {
          if (nextWedgeAngle < 0 || nextWedgeAngle == ang)
            continue;
          ang = nextWedgeAngle;
        }

        V.pushColor(MyColor.cRED);
        V.pushStroke(STRK_RUBBERBAND);

        FPoint2 p0 = MyMath.ptOnCircle(sourceVertLoc, ang + Math.PI, 50);
        FPoint2 p1 = MyMath.ptOnCircle(sourceVertLoc, ang, 50);

        V.drawLine(p0, sourceVertLoc);
        V.pop();

        EdSegment.plotDirectedLine(sourceVertLoc, p1);
        V.pop(1);
      }
    }

    // plot active edges, slightly displaced from the polygon that induced them
    if (activeEdges != null) {
      int pos = 0;
      for (Iterator it = activeEdges.iterator(); it.hasNext(); pos++) {
        Edge e = (Edge) it.next();
        e.render(MyColor.cPURPLE, STRK_NORMAL, -1, true);
        FPoint2 crossPt = FPoint2
            .midPoint(e.getVert(0).pt(), e.getVert(1).pt());

        if (crossPt != null)
          T.show("#" + pos, MyColor.cRED, //
              crossPt, TX_FRAME | TX_BGND, .8);
      }
    }

    // plot events
    if (C.vb(PLOTEVENTS) && edgeEvents != null) {
      // use a map to determine which vertex each
      // event is clustered around, so we can stack them
      // within each cluster
      Map map = new HashMap();
      V.pushScale(.6);
      for (Iterator it = edgeEvents.iterator(); it.hasNext();) {
        EdgeEvent e = (EdgeEvent) it.next();
        FPoint2 loc = new FPoint2(e.getVertex());
        loc = MyMath.snapToGrid(loc, 1.5);
        String key = loc.toString();
        Integer rep = (Integer) map.get(key);
        if (rep == null)
          rep = new Integer(0);
        V.draw(e.toString(), loc.x, loc.y + 1.8 * rep.intValue());
        map.put(key, new Integer(rep.intValue() + 1));
      }
      V.pop();
    }

    // print edge list in top of window
    if (C.vb(PRINTEDGES) && activeEdges != null) {
      StringBuilder sb = new StringBuilder();
      for (Iterator it = activeEdges.iterator(); it.hasNext();) {
        Edge e = (Edge) it.next();
        sb.append(e);
        sb.append("\n");
      }
      V.pushScale(.9);
      V.draw(sb.toString(), 0, 100, TX_BGND | TX_CLAMP | 70);
      V.pop();

    }

    if (pts != null) {
      V.pushColor(MyColor.cDARKGREEN);
      MyPoint prev = null;
      for (int i = 0; i < pts.size(); i++) {
        MyPoint pt = (MyPoint) pts.get(i);
        if (prev != null)
          V.drawLine(prev, pt);
        prev = pt;
        if (highlightPoints)
          V.mark(pt, MARK_X);
      }
      V.pop();
    }

    if (vComp != null) {
      vComp.fill(MyColor.cLIGHTGRAY);
      T.show(vComp, MyColor.cDARKGRAY, -1, -1);
    } else {
      //      
      //    if (vPoly != null && C.vb(PLOTNEG)) {
      //      EdPolygon vNeg = Util.externalFaceOf(vPoly);
      //      T.show(vNeg, MyColor.cDARKGRAY, -1, -1);
      //    } else
      T.show(vPoly, MyColor.cDARKGREEN, -1, -1);
    }

  }

  private SortedSet buildEdgeSet() {
    SortedSet s = new TreeSet(new Comparator() {

      public int compare(Object o1, Object o2) {
        final boolean db = dbEdgeSort;
        Edge e1 = (Edge) o1;
        Edge e2 = (Edge) o2;
        if (db && T.update())
          T.msg("compare edges:\n" + e1 //
              + "\n" + e2 + T.show(e1) + T.show(e2));

        int ret = 0;

        if (o1 == o2)
          return 0;

        FPoint2 e1Intersect = null;
        {
          // see where the edges intersect the wedge line
          e1Intersect = LineEqn.intersection(wedgeLine, e1.getLine2());
          if (db && T.update())
            T.msg("wedge line and edge1 intersect" + T.show(wedgeLine)
                + T.show(e1.getLine2()) + T.show(e1Intersect) + "\nwedgeLine="
                + wedgeLine + "\ne1.line2= " + e1.getLine2());

          FPoint2 e2Intersect = LineEqn.intersection(wedgeLine, e2.getLine2());
          if (db && T.update())
            T.msg("wedge line and edge2 intersect" + T.show(wedgeLine)
                + T.show(e2.getLine2()) + T.show(e2Intersect));

          if (e2Intersect == null || e1Intersect == null)
            T.err("no intersection" + T.show(wedgeLine) + T.show(e1.getLine2())
                + T.show(e2.getLine2()) + "\nwedgeLine=" + wedgeLine
                + "\ne2.line2= " + e2.getLine2());

          double t0 = wedgeLine.parameterFor(e1Intersect);
          double t1 = wedgeLine.parameterFor(e2Intersect);
          double tDiff = t0 - t1;
          final double EPSILON = 1e-4;

          if (Math.abs(tDiff) > EPSILON) {
            ret = MyMath.sign(tDiff);
            if (db && T.update())
              T.msg("tdiff " + tDiff + " > EPS, ret=" + ret);
            return ret;
          }
        }

        // if e1 is aligned with wedge, flip, 
        // so we only have to deal with possibility of e2 being aligned
        final double EPS = 1e-4;
        boolean flip = false;
        if (wedgeLine.distanceFrom(e1.farthestVertexFrom(wedgeLine)) < EPS) {
          if (db && T.update())
            T.msg("e1 is aligned with wedge, exchanging with e2" + T.show(e1));
          Edge tmp = e1;
          e1 = e2;
          e2 = tmp;
          flip = true;
        }
        FPoint2 v1 = e1.farthestVertexFrom(wedgeLine);
        FPoint2 v2 = e2.farthestVertexFrom(wedgeLine);

        boolean e2Aligned = (wedgeLine.distanceFrom(v2) < EPS);
        if (db && T.update())
          T.msg("e2 aligned? " + e2Aligned);

        int side1 = wedgeLine.sideOfLine(v1);
        int side2 = side1;

        if (e2Aligned) {
          v2 = e2.nonSrcPt();
        } else {
          side2 = wedgeLine.sideOfLine(v2);

          // if on opposite sides, this is a problem!
          if (db && T.update())
            T.msg("farthest vertices" + T.show(v1) + T.show(v2) + "\nside1="
                + side1 + " side2=" + side2);
          if (side1 != side2)
            T.err("unexpected");
        }
        FPoint2 commonVert = e1Intersect;

        double e2Side = MyMath.sideOfLine(commonVert, v1, v2);
        ret = MyMath.sign(e2Side);

        if (side1 < 0)
          flip ^= true;

        if (flip)
          ret = -ret;
        if (db && T.update())
          T.msg("e2Side of line=" + e2Side
              + EdSegment.showDirected(commonVert, v1) + T.show(v2) + " flip="
              + flip + " returning " + ret);
        return ret;
      }
    });
    return s;
  }

  private int sourceVertexIndex;
  private FPoint2 sourceVertLoc;
  private EdPolygon polygon;
  private Vertex[] verts;
  private LineEqn wedgeLine;
  private boolean dbEdgeSort;
  private EdPolygon vComp;

  private double wedgeAngle;
  private double nextWedgeAngle;
  private Set activeEdges;
  private Edge[] edges;
  private EdPolygon vPoly;
  private DArray pts;
  private SortedSet edgeEvents;
  private boolean highlightPoints;
}
