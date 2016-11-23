package org.opencv.samples.tutorial1;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.*;

import org.opencv.imgproc.Imgproc;


import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.ImageButton;

import com.example.imgloader.R;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class Tutorial1Activity extends Activity implements CvCameraViewListener2,OnTouchListener {

    private static final String TAG = "OCVSample::Activity";
    private Mat mRgba;
    private Mat mGray;
    private boolean              mIsColorSelected = false;
    List<MatOfPoint> HULL_COUNTOURS = new ArrayList<MatOfPoint>();
    double AC;
    Rect AR;


    private CameraBridgeViewBase mOpenCvCameraView;
    //Scalar min = new Scalar(0, 118, 102, 0);
    //Scalar max = new Scalar(36, 218, 202, 255);
    Mat pointMatHsv;
    ImageButton button;
    Mat heirarchy;
    private Scalar               mBlobColorRgba;
    private Scalar               mBlobColorHsv;
    private Scalar mLowerBound = new Scalar(0);
    private Scalar mUpperBound = new Scalar(0);
    private Scalar mColorRadius = new Scalar(25,50,50,0);
    DetectorClass mDetector = new DetectorClass();
    int defTotal=0;
    int hullTotal;
    String recog;

    private static double mMinContourArea = 0.1;
    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }



    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    mOpenCvCameraView.setOnTouchListener(Tutorial1Activity.this);

                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    public Tutorial1Activity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.tutorial1_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.tutorial1_activity_java_surface_view);
        mOpenCvCameraView.setMaxFrameSize(720, 480);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        button=(ImageButton)findViewById(R.id.switchBtn);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

              //  setContentView(R.layout.speechrecognizer);
                Intent i=new Intent(getApplicationContext(),SpeechRecognizer.class);
                startActivity(i);
            }
        });
    }


    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }


    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
        pointMatHsv = new Mat(1, 1, CvType.CV_8UC3);
        heirarchy = new Mat();

    }

    public void onCameraViewStopped() {
        mRgba.release();
    }
    public boolean onTouch(View v, MotionEvent event)
    {
        int cols = mRgba.cols();
        int rows = mRgba.rows();

        int xOffset = (mOpenCvCameraView.getWidth() - cols) / 2;
        int yOffset = (mOpenCvCameraView.getHeight() - rows) / 2;

        int x = (int)event.getX() - xOffset;
        int y = (int)event.getY() - yOffset;

        Log.i(TAG, "Touch image coordinates: (" + x + ", " + y + ")");

        if ((x < 0) || (y < 0) || (x > cols) || (y > rows)) return false;

        Rect touchedRect = new Rect();

        touchedRect.x = (x>3) ? x-3 : 0;
        touchedRect.y = (y>3) ? y-3 : 0;

        touchedRect.width = (x+3 < cols) ? x + 3 - touchedRect.x : cols - touchedRect.x;
        touchedRect.height = (y+3 < rows) ? y + 3 - touchedRect.y : rows - touchedRect.y;

        Mat touchedRegionRgba = mRgba.submat(touchedRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV_FULL);

        // Calculate average color of touched region
        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = touchedRect.width*touchedRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        mBlobColorRgba = converScalarHsv2Rgba(mBlobColorHsv);

        Log.i(TAG, "Touched rgba color: (" + mBlobColorRgba.val[0] + ", " + mBlobColorRgba.val[1] +
                ", " + mBlobColorRgba.val[2] + ", " + mBlobColorRgba.val[3] + ")");

        setHsvColor(mBlobColorHsv);



        mIsColorSelected = true;

        touchedRegionRgba.release();
        touchedRegionHsv.release();

        return false; // don't need subsequent touch events

    }


    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();
        List<MatOfPoint> contours1;
        if (!mIsColorSelected)
            return mRgba;

            mRgba.copyTo(pointMatHsv);
            contours1=retContours(pointMatHsv,mRgba);


            if (contours1.size() <= 0) {
                return mRgba;
            }
            RotatedRect rect = Imgproc.minAreaRect(new MatOfPoint2f(contours1.get(0).toArray()));
            double boundWidth = rect.size.width;
            double boundHeight = rect.size.height;
            int boundPos = 0;

            for (int i = 1; i < contours1.size(); i++) {
                rect = Imgproc.minAreaRect(new MatOfPoint2f(contours1.get(i).toArray()));
                if (rect.size.width * rect.size.height > boundWidth * boundHeight) {
                    boundWidth = rect.size.width;
                    boundHeight = rect.size.height;
                    boundPos = i;
                }
            }
            Rect boundRect = Imgproc.boundingRect(new MatOfPoint(contours1.get(boundPos).toArray()));


            double a = boundRect.br().y - boundRect.tl().y;
            a = a * 0.7;
            a = boundRect.tl().y + a;



            MatOfPoint2f pointMat = new MatOfPoint2f();
            Imgproc.approxPolyDP(new MatOfPoint2f(contours1.get(boundPos).toArray()), pointMat, 3, true);
            contours1.set(boundPos, new MatOfPoint(pointMat.toArray()));



            MatOfInt hull = new MatOfInt();
            MatOfInt4 convexDefect = new MatOfInt4();
            Imgproc.convexHull(new MatOfPoint(contours1.get(boundPos).toArray()), hull);
            //if(hull.toArray().length < 3) return mRgba;


            if(hull.toArray().length>3) {
                Imgproc.convexityDefects(new MatOfPoint(contours1.get(boundPos).toArray()), hull, convexDefect);


                List<MatOfPoint> hullPoints = new LinkedList<MatOfPoint>();
                List<Point> listPo = new LinkedList<Point>();
                for (int j = 0; j < hull.toList().size(); j++) {
                    listPo.add(contours1.get(boundPos).toList().get(hull.toList().get(j)));
                }


                MatOfPoint e = new MatOfPoint();
                e.fromList(listPo);
                hullPoints.add(e);

                List<MatOfPoint> defectPoints = new LinkedList<MatOfPoint>();
                List<Point> listPoDefect = new LinkedList<Point>();
                for (int j = 0; j < convexDefect.toList().size(); j = j + 4) {
                    Point farPoint = contours1.get(boundPos).toList().get(convexDefect.toList().get(j + 2));
                    Integer depth = convexDefect.toList().get(j + 3);
                    if (depth > 8700 && farPoint.y < a) {
                        listPoDefect.add(contours1.get(boundPos).toList().get(convexDefect.toList().get(j + 2)));
                    }
                    Log.d(TAG, "defects [" + j + "] " + convexDefect.toList().get(j + 3));
                }

                MatOfPoint e2 = new MatOfPoint();
                e2.fromList(listPo);
                defectPoints.add(e2);

                Log.d(TAG, "hull: " + hull.toList());
                Log.d(TAG, "defects: " + convexDefect.toList());

                Imgproc.drawContours(mRgba, hullPoints, -1, new Scalar(0, 255, 0), 3);

                int defectsTotal = (int) convexDefect.total();
                Log.d(TAG, "Defect total " + defectsTotal);


                defTotal = 0;
                for (Point p : listPoDefect) {
                    Imgproc.circle(mRgba, p, 6, new Scalar(0, 0, 255));
                    defTotal = defTotal + 1;
                }

                recog = mDetector.detectionFunction(AR, AC, HULL_COUNTOURS, defTotal);
                Imgproc.putText(mRgba, recog, new Point(0, 30), 3, 1, new Scalar(0, 0, 255, 255), 2);
                return mRgba;
            }
        return mRgba;
    }

    public List<MatOfPoint> retContours(Mat pointMatHsv, Mat mRgba) {
        List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
        List<MatOfPoint> mContours = new ArrayList<MatOfPoint>();
        double contourArea1;
        Imgproc.cvtColor(pointMatHsv, pointMatHsv, Imgproc.COLOR_RGB2HSV);
        Core.inRange(pointMatHsv, mLowerBound, mUpperBound, pointMatHsv);
        Imgproc.dilate(pointMatHsv, pointMatHsv, new Mat());
        Imgproc.findContours(pointMatHsv, contours, heirarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = 0;
        Iterator<MatOfPoint> each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint wrapper = each.next();
            double area = Imgproc.contourArea(wrapper);
            if (area > maxArea)
                maxArea = area;
        }

        mContours.clear();
        each = contours.iterator();
        while (each.hasNext()) {
            MatOfPoint contour = each.next();
            if (Imgproc.contourArea(contour) > mMinContourArea * maxArea) {
                mContours.add(contour);

            }
        }

        contourArea1=0;
        each=mContours.iterator();
        while (each.hasNext())
        {
            MatOfPoint contourArea = each.next();
            contourArea1=Imgproc.contourArea(contourArea);
        }
        Imgproc.drawContours(mRgba, mContours, -1, new Scalar(0, 255, 0));

        for(int i=0;i<mContours.size();i++) {
            MatOfPoint currentContour = mContours.get(i);
            Rect rectArea=drawBoundingBox(currentContour);
            detection(contourArea1,rectArea);
        }
        return mContours;

    }

    private Rect drawBoundingBox(MatOfPoint currentContour) {
        Rect rectangle =  Imgproc.boundingRect(currentContour);
        Imgproc.rectangle(mRgba, rectangle.tl(), rectangle.br(), new Scalar(255, 255, 0), 3);
        return rectangle;

    }


    void detection(double area_contour,Rect area_rect)
    {

        AC=area_contour;
        AR=area_rect;

    }





























    private Scalar converScalarHsv2Rgba(Scalar hsvColor)
    {
        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, hsvColor);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB_FULL, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public void setHsvColor(Scalar hsvColor)
    {
        double minH = (hsvColor.val[0] >= mColorRadius.val[0]) ? hsvColor.val[0]-mColorRadius.val[0] : 0;
        double maxH = (hsvColor.val[0]+mColorRadius.val[0] <= 255) ? hsvColor.val[0]+mColorRadius.val[0] : 255;

        mLowerBound.val[0] = minH;
        mUpperBound.val[0] = maxH;

        mLowerBound.val[1] = hsvColor.val[1] - mColorRadius.val[1];
        mUpperBound.val[1] = hsvColor.val[1] + mColorRadius.val[1];

        mLowerBound.val[2] = hsvColor.val[2] - mColorRadius.val[2];
        mUpperBound.val[2] = hsvColor.val[2] + mColorRadius.val[2];

        mLowerBound.val[3] = 0;
        mUpperBound.val[3] = 255;
    }
























   /* private void drawConvexHull(MatOfPoint currentContour) {
        MatOfInt hull = new MatOfInt();
        //   MatOfInt4 cd= new MatOfInt4();
        //   int []CDarray;
        //   int []HullArray;
        //   int CDpoints,hullpoints;
        List<MatOfPoint> hullContours = new ArrayList<MatOfPoint>();
        Imgproc.convexHull(currentContour, hull);
        //    Imgproc.convexityDefects(currentContour, hull, cd);
    /*    CDarray=cd.toArray();
        HullArray=hull.toArray();
        CDpoints=CDarray.length;
        hullpoints=HullArray.length;
        CDpoints=CDpoints/4;
        hullpoints=hullpoints/4;
        Log.i("TAG","Convexity Defect= "+CDpoints+"Hull points ="+hullpoints);

        MatOfPoint hullMat = new MatOfPoint();
        hullMat.create((int) hull.size().height, 1, CvType.CV_32SC2);

        for (int j = 0; j < hull.size().height; j++) {
            int index = (int) hull.get(j, 0)[0];
            double[] point = new double[]{
                    currentContour.get(index, 0)[0], currentContour.get(index, 0)[1]
            };
            hullMat.put(j, 0, point);
        }
        hullContours.add(hullMat);
        HULL_COUNTOURS=hullContours;
        Imgproc.drawContours(mRgba, hullContours, 0, new Scalar(0, 0, 255), 2);
    }*/
}