package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class IntersectOper implements TestBedOperation, Globals {
  /*! .enum  .private  4100  
      _ db_split db_addedges db_filter db_buildpoly _
      db_containment _
  */

    private static final int DB_SPLIT         = 4101;//!
    private static final int DB_ADDEDGES      = 4102;//!
    private static final int DB_FILTER        = 4103;//!
    private static final int DB_BUILDPOLY     = 4104;//!
    private static final int DB_CONTAINMENT   = 4106;//!
/*!*/

  private static final Color C1 = MyColor.cDARKGREEN;
  private static final Color C2 = MyColor.get(MyColor.BROWN);

  public void addControls() {
    C.sOpenTab("Intersect3");
    {
      C.sStaticText("Calculates intersection of simple polygons");
      // C.sCheckBox(SNAP, "snap", null, false);
      C.sCheckBox(DB_SPLIT, "db:split", null, false);
      C.sCheckBox(DB_ADDEDGES, "db:addEdges", null, false);
      C.sCheckBox(DB_FILTER, "db:filter", null, false);
      C.sCheckBox(DB_BUILDPOLY, "db:buildpoly", null, false);
      //  C.sCheckBox(DB_ADDTOGRAPH, "db:addtograph", null, false);
      C.sCheckBox(DB_CONTAINMENT, "db:containment", null, false);
      //     C.sCheckBox(REMOVECOLLINEAR, "rem collinear", null, true);
    }
    C.sCloseTab();
  }

  private boolean db = true;

  public static IntersectOper singleton = new IntersectOper();
  private static final double EPS = 1e-2;

  private IntersectOper() {
  }

  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      }
    }
  }

  /**
   * Prepare for tracing algorithm by clearing references to renderables
   */
  private void initAlg() {
    graphPoly = null;
    graph = null;
  }

  //  private static DArray snap(DArray pl, StringBuilder sb) {
  //    DArray ar = new DArray();
  //    for (int jj = 0; jj < pl.size(); jj++) {
  //      EdPolygon p = (EdPolygon) pl.get(jj);
  //      EdPolygon p2 = new EdPolygon();
  //      for (int j = 0; j < p.nPoints(); j++) {
  //        FPoint2 pt = p.getPoint(j);
  //        pt = MyMath.snapToGrid(pt, .1);
  //        p2.addPoint(pt);
  //      }
  //      ar.add(p2);
  //      {
  //        Editor.write(p2, sb);
  //      }
  //    }
  //    return ar;
  //  }

  /**
   * Edge within point graph
   */
  private class Edge implements Renderable {
    public Edge(int polySet, int n0, int n1) {
      this.polySet = polySet;
      this.n0 = n0;
      this.n1 = n1;
      splitAt(n0);
      splitAt(n1);
    }
    public void render(Color c, int stroke, int markType) {
      V.pushColor(c, MyColor.cRED);
      V.pushStroke(stroke, STRK_THICK);
      EdSegment.plotDirectedLine(graph.pt(n0), graph.pt(n1));
      V.pop(2);
    }
    public FPoint2 pt(int index) {
      return graph.pt(index == 0 ? n0 : n1);
    }
    public void splitAt(int node) {
      splitPoints.addInt(node);
    }
    private DArray splitPoints = new DArray();
    private int n0, n1;
    private int polySet;

    public void addToGraph() {
      final boolean db = false; // && C.vb(DB_ADDTOGRAPH);
      splitPoints.sort(new Comparator() {
        public int compare(Object arg0, Object arg1) {
          int nd0 = ((Integer) arg0).intValue();
          int nd1 = ((Integer) arg1).intValue();
          int ret = 0;
          do {
            if (nd0 == nd1)
              break;
            FPoint2 origin = graph.pt(n0);
            FPoint2 p0 = graph.pt(nd0);
            FPoint2 p1 = graph.pt(nd1);
            double d0 = FPoint2.distanceSquared(p0, origin);
            double d1 = FPoint2.distanceSquared(p1, origin);
            ret = MyMath.sign(d0 - d1);
          } while (false);
          return ret;
        }
      });

      for (int i = 0; i < splitPoints.size() - 1; i++) {
        int node0 = splitPoints.getInt(i);
        int node1 = splitPoints.getInt(i + 1);
        if (node0 == node1)
          continue;
        if (db && T.update())
          T.msg("adding edge from "
              + node0
              + " to "
              + node1
              + EdSegment.showDirected(graph.pt(node0), graph.pt(node1), null,
                  STRK_THICK));
        graph.addEdge(node0, node1, polySet);
      }
    }
    public double length() {
      return pt(0).distance(pt(1));
    }
    public String show(Color c2) {
      return EdSegment.showDirected(graph.pt(n0), graph.pt(n1), c2, STRK_THICK);
    }
  }

  /**
   * Determine if two edges intersect, and if so, add a point there
   * @param e0
   * @param e1
   * @return
   */
  private int addPossibleIntersectionPoint(Edge e0, Edge e1) {

    final boolean db = false;

    int ret = -1;
    do {
      double[] ip = new double[2];
      FPoint2 isect = MyMath.linesIntersection(e0.pt(0), e0.pt(1), e1.pt(0), e1
          .pt(1), ip);
      if (isect == null)
        break;
      double l0 = e0.length();
      double l1 = e1.length();

      double p0 = ip[0] * l0;
      double p1 = ip[1] * l1;

      if (db && T.update())
        T.msg("intersection p0=" + p0 + " p1=" + p1 + " l0=" + l0 + " l1=" + l1
            + e0.show(C1) + e1.show(C2));
      if (p0 < -EPS || p0 - l0 > EPS || p1 < -EPS || p1 - l1 > EPS)
        break;
      ret = graph.add(isect);
    } while (false);
    return ret;
  }

  /**
   * Add intersection points of edges, split edges to include them;
   * add edges to graph
   * @param edges
   */
  private void splitEdges(DArray edges) {
    final boolean db2 = this.db && C.vb(DB_SPLIT);

    if (db2 && T.update())
      T.msg("splitEdges" + T.show(edges));
    for (int i = 0; i < edges.size(); i++) {
      Edge ei = (Edge) edges.get(i);
      for (int j = i + 1; j < edges.size(); j++) {
        Edge ej = (Edge) edges.get(j);
        int iNode = addPossibleIntersectionPoint(ei, ej);
        if (db2 && T.update())
          T.msg("intersection node=" + iNode + T.show(ei, C1) + T.show(ej, C2));
        if (iNode < 0)
          continue;
        if (db2 && T.update())
          T.msg("splitting " + ei + T.show(ei) + " and " + ej + T.show(ej)
              + " at " + iNode);
        ei.splitAt(iNode);
        ej.splitAt(iNode);
      }
    }

    for (int i = 0; i < edges.size(); i++) {
      Edge ei = (Edge) edges.get(i);
      ei.addToGraph();
    }
  }

  /**
   * Create edges for a set of input polygons; also, add their 
   * endpoints to the graph
   * @param pl set of input polygons
   * @param polySet which set they belong to 0:A 1:B
   * @param edges where to store the created edges
   */
  private void addEdgesFrom(DArray pl, int polySet, DArray edges) {
    final boolean db = this.db && C.vb(DB_ADDEDGES);
    if (db && T.update())
      T.msg("addEdgesFrom" + T.show(pl) + " polySet=" + polySet);
    for (int pi = 0; pi < pl.size(); pi++) {
      EdPolygon p = (EdPolygon) pl.get(pi);
      p = EdPolygon.normalize(p);
      for (int i = 0; i < p.nPoints(); i++) {
        FPoint2 pt0 = p.getPoint(i);
        FPoint2 pt1 = p.getPointMod(i + 1);
        int n0 = graph.add(pt0);
        int n1 = graph.add(pt1);
        if (db && T.update())
          T.msg("n0=" + n0 + " n1=" + n1 + EdSegment.show(pt0, pt1));
        if (n0 == n1)
          continue;
        edges.add(new Edge(polySet, n0, n1));
      }
    }
  }

  /**
   * Remove from graph the pair of edges connecting vertices (must exist)
   * @param src
   * @param dest
   */
  private void removeEdgesBetween(int src, int dest) {
    int n1 = graph.hasNeighbor(src, dest);
    int n2 = graph.hasNeighbor(dest, src);
    if (n1 < 0 || n2 < 0)
      T.err("no such edges");
    graph.removeEdge(src, n1);
    graph.removeEdge(dest, n2);
  }

  /**
   * Detect and remove collinear edges, while maintaining
   * inherent parity of graph
   */
  private void filterEdges() {
    final boolean db = this.db && C.vb(DB_FILTER);
    if (db && T.update())
      T.msg("filterEdges");

    for (int node = 0; node < graph.size(); node++) {
      graph.sort(node);
      int degree = graph.degree(node);
      if (degree < 2)
        continue;

      FPoint2 nodeLoc = graph.pt(node);

      int nbrIndex1 = 0;
      for (int count = 0; count < degree; count++, nbrIndex1++) {
        if (degree < 2)
          break;

        nbrIndex1 = MyMath.mod(nbrIndex1, degree);
        int nbrIndex2 = (nbrIndex1 + 1) % degree;

        int neighbor1 = graph.neighbor(node, nbrIndex1);
        int neighbor2 = graph.neighbor(node, nbrIndex2);
        FPoint2 nbr1Loc = graph.pt(neighbor1);
        FPoint2 nbr2Loc = graph.pt(neighbor2);

        if (db && T.update())
          T.msg("node " + node + " ni=" + nbrIndex1 + " ni2=" + nbrIndex2
              + " k=" + neighbor1 + " k2=" + neighbor2 + T.show(graph) + "\n"
              + graph + T.show(nodeLoc) + show(nodeLoc, nbr1Loc)
              + show(nodeLoc, nbr2Loc));

        double distk = FPoint2.distance(nodeLoc, nbr1Loc);
        double distk2 = FPoint2.distance(nodeLoc, nbr2Loc);

        MutableInteger mi = graph.edgeFlags(node, nbrIndex1);
        MutableInteger mi2 = graph.edgeFlags(node, nbrIndex2);

        double th1 = MyMath.polarAngle(nodeLoc, nbr1Loc);
        double th2 = MyMath.polarAngle(nodeLoc, nbr2Loc);

        if (Math.abs(MyMath.normalizeAngle(th1 - th2)) > Math.PI / 3)
          continue;

        if (distk2 > distk) {
          double dp = MyMath.ptDistanceToLine(nbr1Loc, nodeLoc, nbr2Loc, null);
          if (dp < .1) {

            mi.n |= mi2.n;
            if (db && T.update())
              T.msg("k<k2: adding edge from " + neighbor1 + " -> " + neighbor2
                  + " and removing " + node + "." + nbrIndex2 + "\n" + graph
                  + T.show(graph));

            addEdges(neighbor1, neighbor2, node, neighbor2);
            removeEdgesBetween(node, neighbor2);
            addEdgesWithData(neighbor1, neighbor2, mi2.n, 0);
            degree--;
            nbrIndex1--;
            count--;
          }
        } else {
          double dp = MyMath.ptDistanceToLine(nbr2Loc, nodeLoc, nbr1Loc, null);
          if (dp < .1) {
            mi2.n |= mi.n;
            if (db && T.update())
              T.msg("k>k2: adding edge from " + neighbor2 + " -> " + neighbor1
                  + " and removing " + node + "." + nbrIndex1 + "\n" + graph
                  + T.show(graph));
            addEdges(neighbor2, neighbor1, node, neighbor1);
            removeEdgesBetween(node, neighbor1);
            addEdgesWithData(neighbor2, neighbor1, mi.n, 0);
            degree--;
            nbrIndex1--;
            count--;
          }
        }
      }
    }

    // remove edges incident with degree 1 vertices 
    for (int node = 0; node < graph.size(); node++) {
      int deg = graph.degree(node);
      if (deg != 1)
        continue;
      int dest = graph.neighbor(node, 0);
      if (db && T.update())
        T.msg("removing degree 1 node: " + node + T.show(graph.pt(node))
            + " -> " + dest);
      graph.removeEdge(node, 0);
      int si = graph.hasNeighbor(dest, node);
      if (si >= 0) {
        if (db && T.update())
          T.msg("removing corresponding edge " + dest + " -> " + node);
        graph.removeEdge(dest, si);
      }
    }
    graph.sort();

  }

  /**
   * Add edges connecting src with dest, copying data from 
   * existing edges between another couple of edges (if they exist)
   * @param src
   * @param dest
   * @param src2
   * @param dest2
   */
  private void addEdges(int src, int dest, int src2, int dest2) {

    // add edge src->dest
    int data2 = 0;
    {
      int q = graph.hasNeighbor(src2, dest2);
      if (q >= 0) {
        data2 = graph.edgeFlags(src2, q).n;
      }
    }
    int nbr = graph.addEdge(src, dest);
    graph.edgeFlags(src, nbr).n |= data2;

    // add edge dest->src
    data2 = 0;
    {
      int q = graph.hasNeighbor(dest2, src2);
      if (q >= 0) {
        data2 = graph.edgeFlags(dest2, q).n;
      }
    }
    nbr = graph.addEdge(dest, src);
    graph.edgeFlags(dest, nbr).n |= data2;

  }

  /**
   * Add edges, if necessary, between two vertices, and store 
   * data with them
   * @param src
   * @param dest edges
   * @param dataSD bits to include in src - dest
   * @param dataDS bits to include in dest - src
   */
  private void addEdgesWithData(int src, int dest, int dataSD, int dataDS) {
    graph.addEdgeRead(src, dest).n |= dataSD;
    graph.addEdgeRead(dest, src).n |= dataDS;
  }

  private DArray splitIntoSimple(DArray pl) {

    final boolean db = false;

    DArray ret = new DArray();

    for (int srcPolyIndex = 0; srcPolyIndex < pl.size(); srcPolyIndex++) {
      EdPolygon srcPoly = (EdPolygon) pl.get(srcPolyIndex);
      srcPoly = EdPolygon.normalize(srcPoly);

      if (db && T.update())
        T.msg("splitIntoSimple" + T.show(srcPoly));

      int startNode = -1;

      int[] mergedVertInds = new int[srcPoly.nPoints()];
      // PointGraph g = new PointGraph();
      for (int i = 0; i < srcPoly.nPoints(); i++) {
        int di = i;
        FPoint2 pti = srcPoly.getPoint(i);
        for (int j = 0; j < i; j++) {
          FPoint2 ptj = srcPoly.getPoint(mergedVertInds[j]);
          double dist = FPoint2.distance(pti, ptj);
          if (dist < EPS) {
            di = j;
            break;
          }
        }
        mergedVertInds[i] = di;
        if (startNode < 0 && di < i)
          startNode = di;
      }

      if (startNode < 0)
        startNode = 0;

      int startVert = mergedVertInds[startNode];
      EdPolygon splitPoly = new EdPolygon();

      if (db && T.update())
        T.msg("startNode=" + startNode + " startVert=" + startVert + "\nmgInd="
            + DArray.toString(mergedVertInds));

      for (int i = 0; i < srcPoly.nPoints(); i++) {
        int origVert = (i + startNode) % srcPoly.nPoints();
        int mergVert = mergedVertInds[origVert];
        FPoint2 mPt = srcPoly.getPoint(mergVert);

        if (db && T.update())
          T.msg("startVert=" + startVert + " merg=" + mergVert + T.show(mPt));

        if (splitPoly.nPoints() != 0) {
          if (mergVert == startVert) {
            if (db && T.update())
              T.msg("adding split poly" + T.show(splitPoly));

            ret.add(splitPoly);
            splitPoly = new EdPolygon();
          }
        }

        splitPoly.addPoint(mPt);
      }
      if (splitPoly.nPoints() > 2) {
        if (db && T.update())
          T.msg("adding final split poly" + T.show(splitPoly));
        ret.add(splitPoly);
      }
    }
    return ret;
  }

  /**
   * Calculate the intersection of two sets of polygons
   */
  private DArray calcIntersection(DArray pListA, DArray pListB) {
    initAlg();

    if (db) {
      T.show(pListA, C1, STRK_THIN, -1);
      T.show(pListB, C2, STRK_THIN, -1);
    }

    //    if (C.vb(SNAP)) {
    //      StringBuilder sb = new StringBuilder("\n\nsnapping polygons:\n\n");
    //      pListA = snap(pListA, sb);
    //      pListB = snap(pListB, sb);
    //      if (true)
    //        Tools.warn("disable print");
    //      else
    //        Streams.out.println(sb);
    //    }
    graph = new PointGraph();

    DArray edges = new DArray();

    if (true) {
      Tools.warn("splitting lists into simple polygons");
      pListA = splitIntoSimple(pListA);
      pListB = splitIntoSimple(pListB);
    }

    addEdgesFrom(pListA, 0, edges);
    addEdgesFrom(pListB, 1, edges);
    splitEdges(edges);
    filterEdges();

    //    if (db && T.update())
    //      T.msg("" + graph + T.show(graph));

    DArray ret = new DArray();
    ret = buildPolysFromGraph();
    testContainment(pListA, pListB, ret);
    initAlg();
    if (db && T.update())
      T.msg("returning intersection polygons"
          + T.show(ret, MyColor.cRED, STRK_THICK, -1));

    return ret;
  }

  /**
   * Utility function: show an object in red, with thick stroke
   * @param p
   * @return
   */
  private static String show(Object p) {
    return T.show(p, MyColor.cRED, STRK_THICK, -1);
  }

  /**
   * Utility function: show thick directed line
   * @param p1
   * @param p2
   * @return
   */
  private static String show(FPoint2 p1, FPoint2 p2) {
    return EdSegment.showDirected(p1, p2, null, STRK_THICK);
  }

  /**
   * Detect polygons that are contained by others for intersection
   * @param pListA
   * @param pListB
   * @param pIntersection
   */
  private void testContainment(DArray pListA, DArray pListB,
      DArray pIntersection) {
    final boolean db = this.db; //&& C.vb(DB_CONTAINMENT);

    DArray iPointsA = new DArray(), iPointsB = new DArray();

    // find an interior point for each polygon
    for (int pass = 0; pass < 2; pass++) {
      DArray pl = pass == 0 ? pListA : pListB;
      DArray ip = pass == 0 ? iPointsA : iPointsB;

      for (int pi = 0; pi < pl.size(); pi++) {
        EdPolygon p = (EdPolygon) pl.get(pi);
        FPoint2 intPt = p.findInteriorPoint();
        if (intPt == null)
          T.err("no interior point found"
              + T.show(p, MyColor.cRED, STRK_THICK, -1));
        ip.add(intPt);
        if (db && T.update())
          T.msg("interior point" + T.show(intPt) + T.show(p));
      }
    }

    for (int pass = 0; pass < 2; pass++) {
      DArray pl = pass == 0 ? pListA : pListB;
      DArray pl2 = pass == 1 ? pListA : pListB;
      DArray ip = pass == 0 ? iPointsA : iPointsB;

      // process each A polygon
      for (int i = 0; i < ip.size(); i++) {
        EdPolygon aPoly = (EdPolygon) pl.get(i);

        FPoint2 ipt = ip.getFPoint2(i);
        if (db && T.update())
          T.msg("testContainment, interior point of " + aPoly + show(aPoly)
              + T.show(ipt));

        // see if an intersection polygon already contains this point
        boolean found = false;
        for (int j = 0; j < pIntersection.size(); j++) {
          EdPolygon ipoly = (EdPolygon) pIntersection.get(j);
          if (ipoly.contains(ipt)) {
            if (db && T.update())
              T.msg("found containing intersection poly" + show(ipoly));
            found = true;
            break;
          }
        }
        if (found)
          continue;

        // see if B polygon contains it
        EdPolygon bPoly = null;
        // int bIndex = -1;
        for (int j = 0; j < pl2.size(); j++) {
          EdPolygon ipoly = (EdPolygon) pl2.get(j);
          if (ipoly.contains(ipt)) {
            if (db && T.update())
              T.msg("found containing opposite poly" + show(ipoly));
            bPoly = ipoly;
            // bIndex = j;
            break;
          }
        }
        if (bPoly == null)
          continue;

        // if A contains B's interior point, add B; else, add A

        EdPolygon addPoly = null;

        boolean aContainsB = true;
        boolean bContainsA = true;
        for (int q = 0; q < bPoly.nPoints(); q++)
          if (!aPoly.contains(bPoly.getPoint(q))) {
            aContainsB = false;
            break;
          }
        if (aContainsB) {
          for (int q = 0; q < aPoly.nPoints(); q++)
            if (!bPoly.contains(aPoly.getPoint(q))) {
              bContainsA = false;
              break;
            }
        }

        if (bContainsA)
          addPoly = aPoly;
        else
          addPoly = bPoly;

        if (db && T.update())
          T.msg("adding contained polygon" + show(addPoly));
        pIntersection.add(addPoly);
      }
    }
  }

  /**
   * Walk the graph, extracting polygons representing 
   * intersection of the two input sets
   * @return array of intersection polygons
   */
  private DArray buildPolysFromGraph() {
    boolean db = this.db; // && C.vb(DB_BUILDPOLY);

    if (db && T.update())
      T.msg("buildPolysFromGraph" + T.show(graph) + "\n" + graph);

    DArray pl = new DArray();

    for (int startNode = 0; startNode < graph.size(); startNode++) {
      for (int sNbrInd = 0; sNbrInd < graph.degree(startNode); sNbrInd++) {
        int nPrev = startNode;
        int nPrevInd = sNbrInd;
        int n = graph.neighbor(startNode, nPrevInd);

        if (db && T.update())
          T.msg("seeing if already processed edge "
              + nPrev
              + " -> "
              + n
              + EdSegment.showDirected(graph.pt(startNode), graph.pt(n), null,
                  STRK_THICK) + T.show(graph) + "\n" + graph);

        if (graph.edgeFlags(nPrev, sNbrInd).testBit(2))
          continue;

        graphPoly = new EdPolygon();
        graphPoly.setFlags(EdPolygon.FLG_OPEN);
        FPoint2 prevPt = graph.pt(nPrev);
        graphPoly.addPoint(prevPt);

        int edgeFlagSum = 0;

        boolean isXPoly = false;
        int prevV = 0;

        while (true) {

          if (db && T.update())
            T.msg("constructing polygon"
                + ", nPrev="
                + nPrev
                + ", n="
                + n
                + EdSegment.showDirected(graph.pt(nPrev), graph.pt(n), null,
                    STRK_THICK));

          if (graph.degree(n) < 1)
            T.err("degree of node < 1!" + T.show(graph.pt(n)));

          MutableInteger mi = graph.edgeFlags(nPrev, nPrevInd);
          mi.setBit(2);

          if (((mi.n | prevV) & (1 | 2)) == (1 | 2))
            isXPoly = true;

          if (db && T.update())
            T
                .msg(EdSegment.showDirected(graph.pt(nPrev), graph.pt(n), null,
                    STRK_THICK)
                    + "prevV = "
                    + prevV
                    + " currV="
                    + mi.n
                    + " isXPoly="
                    + isXPoly);
          prevV = mi.n;
          edgeFlagSum |= mi.n;

          if (n == startNode && graphPoly.nPoints() > 0)
            break;
          FPoint2 currVert = graph.pt(n);
          graphPoly.addPoint(currVert);

          // determine which neighbor of new node will be next vertex
          int next = -1;
          int nextNbrInd = -1;

          for (int j = 0; j < graph.degree(n); j++) {
            int nbrNode = graph.neighbor(n, j);
            if (nbrNode == nPrev) {
              nextNbrInd = MyMath.mod(j - 1, graph.degree(n));
              break;
            }
          }

          if (nextNbrInd < 0)
            T.err("no next edge found");
          next = graph.neighbor(n, nextNbrInd);

          nPrev = n;
          prevPt = currVert;
          n = next;
          nPrevInd = nextNbrInd;
        }

        isXPoly = (edgeFlagSum & (1 | 2)) == (1 | 2);
        graphPoly.clearFlags(EdPolygon.FLG_OPEN);
        do {
          if (!isXPoly)
            break;
          if (graphPoly.winding() <= 0)
            break;

          //if (C.vb(REMOVECOLLINEAR)) 
          {
            graphPoly = Util.filterCollinear(graphPoly);
            if (!graphPoly.complete())
              break;
          }

          if (db && T.update())
            T.msg("adding poly #" + pl.size()
                + T.show(graphPoly, MyColor.cRED, -1, MARK_DISC));
          pl.add(graphPoly);
        } while (false);
        graphPoly = null;
      }
    }
    return pl;
  }

  /**
   * Calculate intersection of a set of polygons
   * @param polygons
   * @param db if true, tracing enabled
   * @return array of polygons representing intersection
   */
  public static DArray calcPolygonsIntersection(DArray polygons, boolean db) {
    // normalize(polygons);
    DArray sets = new DArray();
    for (int i = 0; i < polygons.size(); i++) {
      EdPolygon p = (EdPolygon) polygons.get(i);
      p = Util.filterCollinear(p);
      sets.add(DArray.build(p));
    }

    if (false) {
      Tools.warn("set db true");
      db = true;
    }

    singleton.db = db;
    return singleton.calcPolygonsIntersectionAux(sets, 0, sets.size());
  }

  /**
   * Recursive helper function to calculate intersection of sets of polygons
   * @param polySets polygons
   * @param iStart first in set
   * @param iLength size of set
   * @return array of intersection polygons
   */
  private DArray calcPolygonsIntersectionAux(DArray polySets, int iStart,
      int iLength) {

    DArray ret = null;

    if (iLength <= 0)
      throw new IllegalArgumentException();
    if (iLength == 1) {
      ret = polySets.getDArray(iStart);
    } else {
      int len = iLength / 2;
      if (db && T.update())
        T.msg("calcPolygonsIntersection, start="
            + iStart
            + " length="
            + iLength
            + T.show(polySets.subset(iStart, len), C1, STRK_THIN, -1)
            + T.show(polySets.subset(iStart + len, iLength - len), C2,
                STRK_THIN, -1));
      DArray set1 = calcPolygonsIntersectionAux(polySets, iStart, len);
      DArray set2 = calcPolygonsIntersectionAux(polySets, iStart + len, iLength
          - len);
      if (db && T.update())
        T.msg("calculating intersection of two sets of polygons"
            + T.show(set1, C1, STRK_THICK, -1)
            + T.show(set2, C2, STRK_THICK, -1));
      ret = calcIntersection(set1, set2);
      if (db && T.update())
        T.msg("sets of polys" + T.show(ret, MyColor.cRED, -1, MARK_DISC));
    }
    return ret;
  }

  //  private static void normalize(DArray a) {
  //    for (int i = 0; i < a.size(); i++) {
  //      EdPolygon p = (EdPolygon) a.get(i);
  //      p = EdPolygon.normalize(p);
  //      a.set(i, p);
  //    }
  //  }

  public void runAlgorithm() {
    db = true;
    initAlg();

    EdPolygon[] polys = SSMain.getPolygons();
    if (polys.length < 2)
      return;

    DArray polySets = new DArray();
    for (int i = 0; i < polys.length; i++) {
      DArray set = DArray.build(Util.filterCollinear(polys[i]));
      //  normalize(set);
      polySets.add(set);
    }

    DArray isectPolys = calcPolygonsIntersectionAux(polySets, 0, polySets
        .size());

    T.show(isectPolys, MyColor.cRED, -1, -1);
  }

  public void paintView() {
    Editor.render();
    T.show(graphPoly, MyColor.cDARKGREEN, STRK_THICK, MARK_DISC);
    if (true && graph != null) {
      T.show(graph);
      V.pushScale(.7);
      V.draw(graph.toString(), 100, 100, TX_CLAMP | TX_BGND | 60);
      V.pop();
    }
  }

  private EdPolygon graphPoly;
  private PointGraph graph;
}
