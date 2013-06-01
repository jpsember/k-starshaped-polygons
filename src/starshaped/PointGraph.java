package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;
import base.Graph.*;

public class PointGraph implements Renderable, Iterable, Globals {
  private static final double EPS = .05;
  public MutableInteger edgeFlags(int node, int nbrInd) {
    return (MutableInteger) graph.edgeData(node, nbrInd);
  }
  //private static final double EPS = 1e-2;

  public void render(Color c, int stroke, int markType) {
    V.pushScale(.7);
    V.pushColor(c, MyColor.cDARKGREEN);
    for (int i = 0; i < size(); i++) {
      FPoint2 p = pt(i);
      V.mark(p, MARK_DISC);
      V.draw("" + i, p.x + 1.5, p.y + 1.5, TX_FRAME | TX_BGND);
      for (int j = 0; j < degree(i); j++) {
        int n = neighbor(i, j);
        if (n < i)
          continue;
        V.drawLine(pt(i), pt(n));
      }
    }
    V.pop(2);
  }
  public int degree(int node) {
    return graph.nCount(node);
  }
  public int neighbor(int node, int nNum) {
    return graph.neighbor(node, nNum);
  }

  public int hasNeighbor(int src, int dest) {
    return graph.hasNeighbor(src, dest);
  }

  /**
   * Add point to graph, if doesn't already exist
   * @param pt 
   * @return id of node
   */
  public int add(FPoint2 pt) {
    final boolean db = true && FPoint2.distance(pt.x, pt.y, 71, 83) < 5;
    if (db && T.update())
      T.msg("add point " + pt.toString(true) + T.show(pt)+"\n"+pts.toString(true));
    int ret = -1;

    double minDist = -1;
    // perform linear search of existing points.
    // This could be replaced by searching for the 9 possible
    // cells that a point could be snapped to, for O(lg n) performance.
    for (int i = 0; i < size(); i++) {
      FPoint2 pe = pt(i);
      double dist = FPoint2.distance(pt, pe);
      if (dist < .5)
        if (db && T.update())
          T.msg("add point " + pt.toString(true)
              + "\n distance from existing point "+pe.toString(true)
              + T.show(pt, MyColor.cRED, -1, MARK_X)
              + T.show(pe, MyColor.cPURPLE, -1, MARK_X) + " is " + dist + "\n"
              + pts.toString(true));
      if (dist < EPS && (minDist < 0 || dist < minDist)) {
        ret = i;
        minDist = dist;
      }
    }
    if (ret < 0) {
      ret = pts.size();
      pts.add(pt);
      graph.newNode(pt);
    }
    return ret;
  }

  public int size() {
    return pts.size();
  }
  public int addEdge(int src, int dest) {
    if (src == dest)
      throw new IllegalArgumentException();

    int ni = graph.hasNeighbor(src, dest);
    if (ni < 0)
      ni = graph.addEdge(src, dest, new MutableInteger());
    return ni;
  }
  public MutableInteger addEdgeRead(int src, int dest) {
    return edgeFlags(src, addEdge(src, dest));
  }

  public void addEdge(int src, int dest, int srcPoly) {
    int ni = addEdge(src, dest);
    //      
    //      if (src == dest)
    //        throw new IllegalArgumentException();
    //
    //      int ni = graph.hasNeighbor(src, dest);
    //      if (ni < 0)
    //        ni = graph.addEdge(src, dest, new MutableInteger());

    edgeFlags(src, ni).n |= (1 << srcPoly);
    addEdge(dest, src);
    //      {
    //        int ni2 = graph.hasNeighbor(dest, src);
    //        if (ni2 < 0)
    //          ni2 = graph.addEdge(dest, src, new MutableInteger());
    //      }
  }

  /**
   * Get point 
   * @param i id of node (this is also just an index 0..size()-1)
   * @return
   */
  public FPoint2 pt(int i) {
    return pts.getFPoint2(i);
  }
  public Iterator iterator() {
    return pts.iterator();
  }

  /**
   * Sort edges by polar angle around vertices
   */
  public void sort() {
    for (int node = 0; node < size(); node++)
      sort(node);
  }

  public void sort(int node) {
    graph.sortEdges(node, edgeSorter);
  }

  private static Comparator edgeSorter = new Comparator() {
    public int compare(Object arg0, Object arg1) {
      Object[] args0 = (Object[]) arg0;
      Object[] args1 = (Object[]) arg1;

      Graph g = (Graph) args0[0];
      int node = ((Integer) args0[1]).intValue();
      int e1 = ((Integer) args0[2]).intValue();
      int e2 = ((Integer) args1[2]).intValue();

      FPoint2 vert = (FPoint2) g.nodeData(node);

      FPoint2 v1 = (FPoint2) g.nodeData(g.neighbor(node, e1));
      FPoint2 v2 = (FPoint2) g.nodeData(g.neighbor(node, e2));

      double a1 = MyMath.normalizeAnglePositive(MyMath.polarAngle(vert, v1));
      double a2 = MyMath.normalizeAnglePositive(MyMath.polarAngle(vert, v2));
      return MyMath.sign(a1 - a2);
    }
  };

  /**
   * Remove a neighbor from a node
   * @param src : id of source node
   * @param index : index of neighbor to remove
   */
  public void removeEdge(int src, int index) {
    graph.removeEdge(src, index);
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(graph);
    return sb.toString();
  }
  private DArray pts = new DArray();
  private Graph graph = new Graph(0);

}
