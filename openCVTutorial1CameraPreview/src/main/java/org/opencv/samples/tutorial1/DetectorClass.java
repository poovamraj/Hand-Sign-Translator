package org.opencv.samples.tutorial1;

import android.util.Log;

import com.example.imgloader.R;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by Poovam on 2/12/2016.
 */
public class DetectorClass {
    String DETECTED_WORD;

    String detectionFunction(Rect rectangle,double area_contour,List<MatOfPoint> hull_contour,int defectsTotal)
    {
        List<MatOfPoint> temp= hull_contour;

        temp.toArray();
        double width= rectangle.width;
        double height=rectangle.height;
               Rect Sub_rect=new Rect();
        double area=rectangle.area()/area_contour;
        double RECT_RATIO=width/height;
        int COUNT=temp.size();
        Log.i("No","Contour points ="+COUNT+"\nRect height= "+height+"Rect width= "+width+"Rect ratio ="+RECT_RATIO+"Area Ratio ="+area+" defects total ="+defectsTotal);
        Sub_rect.x=rectangle.x;
        Sub_rect.y=rectangle.y;
      /*  if(area>2)
        {
            if(RECT_RATIO<1)
                DETECTED_WORD="Vertically open";
                      else
                DETECTED_WORD="Horizontally open";
        }
        else
        {
            if(RECT_RATIO<1)
                DETECTED_WORD="B";
            else if(RECT_RATIO>0.80&& RECT_RATIO<1.10)
                DETECTED_WORD="A";

            else
                DETECTED_WORD="Horizontally close";
        }*/






        if(area>3)
            DETECTED_WORD="L";
        else

        if(RECT_RATIO<1)
        {
            if(RECT_RATIO<0.65&&area>2) {
                if ((defectsTotal == 3 || defectsTotal == 4))
                    DETECTED_WORD = "Peace";
                else
                    DETECTED_WORD="Point";
            }

            else if(area>2&&defectsTotal!=1&&area<3)
                DETECTED_WORD="Hi";
            else if(defectsTotal==1) {
                if (RECT_RATIO < 0.8)

                    DETECTED_WORD = "I";
                else
                    DETECTED_WORD = "A";
            }
                else
                    DETECTED_WORD="B";

        }

        else
        {

             if(area>2&&defectsTotal!=1)
                DETECTED_WORD="G";
             else if(defectsTotal==1) {
                 if (RECT_RATIO < 0.8)
                     DETECTED_WORD = "I";
                 else
                     DETECTED_WORD = "A";
             }
            else
                DETECTED_WORD="Show";
        }



        return DETECTED_WORD;
    }


}

