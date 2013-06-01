package starshaped;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class KernelOper implements TestBedOperation, Globals {

  /*! .enum  .public 4280     
  */

/*!*/

  public static KernelOper singleton = new KernelOper();
  private KernelOper() {
  }
  public void addControls() {
    C.sOpenTab("Kernel");
    {
      C.sStaticText("Displays kernel as union of complements of V-regions");

      C.sOpen();
      {
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

  public void runAlgorithm() {

  }

  public void paintView() {

    //DArray vPolys = new DArray();
    EdPolygon[] polys = SSMain.getPolygons();
    EdPolygon polygon = null;

    boolean problem = false;

    if (polys.length > 0) {
      V.pushColor(MyColor.cDARKGRAY);
      V.fillRect(V.viewRect);
      V.pop();

      polygon = polys[0];
      for (int i = 0; i < polygon.nPoints(); i++) {

        EdPolygon vp = VRgnOper.constructVRgn(polygon, i);
        if (vp == null) {
          problem = true;
          continue;
        }

        EdPolygon neg = new EdPolygon();
        Util.externalFaceOf(vp, neg);
        neg.fill(Color.white);
      }
    }
    if (problem)
      V.draw("A problem occurred.", 0, 100, TX_CLAMP | 50);
    //    if (vPolys != null) {
    //      V.pushColor(MyColor.cDARKGRAY);
    //      V.fillRect(V.viewRect);
    //      V.pop();
    //      V.pushColor(Color.white);
    //      for (int i = 0; i < vPolys.size(); i++) {
    //        EdPolygon p = (EdPolygon) vPolys.get(i);
    //        EdPolygon neg = new EdPolygon();
    //        Util.externalFaceOf(p, neg);
    //        neg.fill(Color.white);
    //      }
    //      V.pop();
    //    }
    Editor.render();
  }
}
