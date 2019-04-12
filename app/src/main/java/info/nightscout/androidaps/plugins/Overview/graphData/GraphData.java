package info.nightscout.androidaps.plugins.Overview.graphData;

import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.ValueDependentColor;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.Series;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Comparator;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.db.ExtendedBolus;
import info.nightscout.androidaps.db.ProfileSwitch;
import info.nightscout.androidaps.db.TempTarget;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.IobCobCalculator.AutosensData;
import info.nightscout.androidaps.plugins.IobCobCalculator.BasalData;
import info.nightscout.androidaps.plugins.IobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.plugins.Loop.APSResult;
import info.nightscout.androidaps.plugins.Loop.LoopPlugin;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.AreaGraphSeries;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.DataPointWithLabelInterface;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.DoubleDataPoint;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.FixedLineGraphSeries;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.PointsWithLabelGraphSeries;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.Scale;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.ScaledDataPoint;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.TimeAsXAxisLabelFormatter;
import info.nightscout.androidaps.plugins.Treatments.Treatment;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.androidaps.plugins.ARG.Graphic.ARGDataPoint;
import info.nightscout.utils.Round;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.ARGTable;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.plugins.NSClientInternal.NSUpload;
import info.nightscout.utils.DateUtil;


/**
 * Created by mike on 18.10.2017.
 */

public class GraphData {
    private static Logger log = LoggerFactory.getLogger(L.OVERVIEW);

    private GraphView graph;
    public double maxY = Double.MIN_VALUE;
    public double minY = Double.MAX_VALUE;
    private List<BgReading> bgReadingsArray;
    private String units;
    private List<Series> series = new ArrayList<>();

    private IobCobCalculatorPlugin iobCobCalculatorPlugin;

    public GraphData(GraphView graph, IobCobCalculatorPlugin iobCobCalculatorPlugin) {
        units = ProfileFunctions.getInstance().getProfileUnits();
        this.graph = graph;
        this.iobCobCalculatorPlugin = iobCobCalculatorPlugin;
    }

    public void addBgReadings(long fromTime, long toTime, double lowLine, double highLine, List<BgReading> predictions) {
        double maxBgValue = Double.MIN_VALUE;
        //bgReadingsArray = MainApp.getDbHelper().getBgreadingsDataFromTime(fromTime, true);
        bgReadingsArray = iobCobCalculatorPlugin.getBgReadings();
        List<DataPointWithLabelInterface> bgListArray = new ArrayList<>();

        if (bgReadingsArray == null || bgReadingsArray.size() == 0) {
            if (L.isEnabled(L.OVERVIEW))
                log.debug("No BG data.");
            return;
        }

        for (BgReading bg : bgReadingsArray) {
            if (bg.date < fromTime || bg.date > toTime) continue;
            if (bg.value > maxBgValue) maxBgValue = bg.value;
            bgListArray.add(bg);
        }
        if (predictions != null) {
            Collections.sort(predictions, (o1, o2) -> Double.compare(o1.getX(), o2.getX()));
            for (BgReading prediction : predictions) {
                if (prediction.value >= 40)
                    bgListArray.add(prediction);
            }
        }

        maxBgValue = Profile.fromMgdlToUnits(maxBgValue, units);
        maxBgValue = units.equals(Constants.MGDL) ? Round.roundTo(maxBgValue, 40d) + 80 : Round.roundTo(maxBgValue, 2d) + 4;
        if (highLine > maxBgValue) maxBgValue = highLine;
        int numOfVertLines = units.equals(Constants.MGDL) ? (int) (maxBgValue / 40 + 1) : (int) (maxBgValue / 2 + 1);

        maxY = maxBgValue;
        minY = 0;

        ARGDataPoint title = new ARGDataPoint();
        title.dp_shape = PointsWithLabelGraphSeries.Shape.ARGTITLE;
        title.dp_color = 0xffffffff;
        title.dp_label = "G";
        title.dp_x = fromTime;
        title.dp_y = maxY;
        bgListArray.add(title);


        DataPointWithLabelInterface[] bg = new DataPointWithLabelInterface[bgListArray.size()];
        bg = bgListArray.toArray(bg);


        // set manual y bounds to have nice steps
        graph.getGridLabelRenderer().setNumVerticalLabels(numOfVertLines);

        addSeries(new PointsWithLabelGraphSeries<>(bg));
    }

    public void addInRangeArea(long fromTime, long toTime, double lowLine, double highLine) {
        AreaGraphSeries<DoubleDataPoint> inRangeAreaSeries;

        DoubleDataPoint[] inRangeAreaDataPoints = new DoubleDataPoint[]{
                new DoubleDataPoint(fromTime, lowLine, highLine),
                new DoubleDataPoint(toTime, lowLine, highLine)
        };
        inRangeAreaSeries = new AreaGraphSeries<>(inRangeAreaDataPoints);
        inRangeAreaSeries.setColor(0);
        inRangeAreaSeries.setDrawBackground(true);
        inRangeAreaSeries.setBackgroundColor(MainApp.gc(R.color.inrangebackground));

        addSeries(inRangeAreaSeries);
    }

    // scale in % of vertical size (like 0.3)
    public void addBasals(long fromTime, long toTime, double scale, boolean thirdGraph) {
        LineGraphSeries<ScaledDataPoint> basalsLineSeries;
        LineGraphSeries<ScaledDataPoint> absoluteBasalsLineSeries;
        LineGraphSeries<ScaledDataPoint> baseBasalsSeries;
        LineGraphSeries<ScaledDataPoint> tempBasalsSeries;

        double maxBasalValueFound = 0d;
        Scale basalScale = new Scale();

        List<ScaledDataPoint> baseBasalArray = new ArrayList<>();
        List<ScaledDataPoint> tempBasalArray = new ArrayList<>();
        List<ScaledDataPoint> basalLineArray = new ArrayList<>();
        List<ScaledDataPoint> absoluteBasalLineArray = new ArrayList<>();
        double lastLineBasal = 0;
        double lastAbsoluteLineBasal = -1;
        double lastBaseBasal = 0;
        double lastTempBasal = 0;
        for (long time = fromTime; time < toTime; time += 60 * 1000L) {
            Profile profile = ProfileFunctions.getInstance().getProfile(time);
            if (profile == null) continue;
            BasalData basalData = iobCobCalculatorPlugin.getBasalData(profile, time);
            double baseBasalValue = basalData.basal;
            double absoluteLineValue = baseBasalValue;
            double tempBasalValue = 0;
            double basal = 0d;
            if (basalData.isTempBasalRunning) {
                absoluteLineValue = tempBasalValue = basalData.tempBasalAbsolute;
                if (tempBasalValue != lastTempBasal) {
                    tempBasalArray.add(new ScaledDataPoint(time, lastTempBasal, basalScale));
                    tempBasalArray.add(new ScaledDataPoint(time, basal = tempBasalValue, basalScale));
                }
                if (lastBaseBasal != 0d) {
                    baseBasalArray.add(new ScaledDataPoint(time, lastBaseBasal, basalScale));
                    baseBasalArray.add(new ScaledDataPoint(time, 0d, basalScale));
                    lastBaseBasal = 0d;
                }
            } else {
                if (baseBasalValue != lastBaseBasal) {
                    baseBasalArray.add(new ScaledDataPoint(time, lastBaseBasal, basalScale));
                    baseBasalArray.add(new ScaledDataPoint(time, basal = baseBasalValue, basalScale));
                    lastBaseBasal = baseBasalValue;
                }
                if (lastTempBasal != 0) {
                    tempBasalArray.add(new ScaledDataPoint(time, lastTempBasal, basalScale));
                    tempBasalArray.add(new ScaledDataPoint(time, 0d, basalScale));
                }
            }

            if (baseBasalValue != lastLineBasal) {
                basalLineArray.add(new ScaledDataPoint(time, lastLineBasal, basalScale));
                basalLineArray.add(new ScaledDataPoint(time, baseBasalValue, basalScale));
            }
            if (absoluteLineValue != lastAbsoluteLineBasal) {
                absoluteBasalLineArray.add(new ScaledDataPoint(time, lastAbsoluteLineBasal, basalScale));
                absoluteBasalLineArray.add(new ScaledDataPoint(time, basal, basalScale));
            }

            lastAbsoluteLineBasal = absoluteLineValue;
            lastLineBasal = baseBasalValue;
            lastTempBasal = tempBasalValue;
            maxBasalValueFound = Math.max(maxBasalValueFound, Math.max(tempBasalValue, baseBasalValue));
        }

        basalLineArray.add(new ScaledDataPoint(toTime, lastLineBasal, basalScale));
        baseBasalArray.add(new ScaledDataPoint(toTime, lastBaseBasal, basalScale));
        tempBasalArray.add(new ScaledDataPoint(toTime, lastTempBasal, basalScale));
        absoluteBasalLineArray.add(new ScaledDataPoint(toTime, lastAbsoluteLineBasal, basalScale));

        ScaledDataPoint[] baseBasal = new ScaledDataPoint[baseBasalArray.size()];
        baseBasal = baseBasalArray.toArray(baseBasal);
        baseBasalsSeries = new LineGraphSeries<>(baseBasal);
        baseBasalsSeries.setDrawBackground(true);
        baseBasalsSeries.setBackgroundColor(MainApp.gc(R.color.basebasal));
        baseBasalsSeries.setThickness(0);

        ScaledDataPoint[] tempBasal = new ScaledDataPoint[tempBasalArray.size()];
        tempBasal = tempBasalArray.toArray(tempBasal);
        tempBasalsSeries = new LineGraphSeries<>(tempBasal);
        tempBasalsSeries.setDrawBackground(true);
        tempBasalsSeries.setBackgroundColor(MainApp.gc(R.color.tempbasal));
        tempBasalsSeries.setThickness(0);

        ScaledDataPoint[] basalLine = new ScaledDataPoint[basalLineArray.size()];
        basalLine = basalLineArray.toArray(basalLine);
        basalsLineSeries = new LineGraphSeries<>(basalLine);
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(MainApp.instance().getApplicationContext().getResources().getDisplayMetrics().scaledDensity * 2);
        paint.setPathEffect(new DashPathEffect(new float[]{2, 4}, 0));
        paint.setColor(MainApp.gc(R.color.basal));
        basalsLineSeries.setCustomPaint(paint);

        ScaledDataPoint[] absoluteBasalLine = new ScaledDataPoint[absoluteBasalLineArray.size()];
        absoluteBasalLine = absoluteBasalLineArray.toArray(absoluteBasalLine);
        absoluteBasalsLineSeries = new LineGraphSeries<>(absoluteBasalLine);
        Paint absolutePaint = new Paint();
        absolutePaint.setStyle(Paint.Style.STROKE);
        absolutePaint.setStrokeWidth(MainApp.instance().getApplicationContext().getResources().getDisplayMetrics().scaledDensity * 2);
        absolutePaint.setColor(MainApp.gc(R.color.basal));
        absoluteBasalsLineSeries.setCustomPaint(absolutePaint);

        if (!thirdGraph)
            basalScale.setMultiplier(maxY * scale / maxBasalValueFound);
        else
            basalScale.setMultiplier(1.0d);

        addSeries(baseBasalsSeries);
        addSeries(tempBasalsSeries);
        addSeries(basalsLineSeries);
        addSeries(absoluteBasalsLineSeries);
    }


    class DelivTimeInJSONComparator implements Comparator<ARGTable>
    {
        @Override
        public int compare(ARGTable o1, ARGTable o2) {    
            // Deolver 1 si o2 tiene que estar antes que o1
            // Devolver -1 si o1 tiene que estar antes que o2
            // devoler 0 si da igual
            long time1 = o1.getLong("deliv_time");
            long time2 = o2.getLong("deliv_time");
            if (time1 > time2)
                return -1;
            else if (time2 > time1)
                return 1;
            else
                return 0;
        }   
    }


    class TimeInJSONComparator implements Comparator<ARGTable>
    {
        @Override
        public int compare(ARGTable o1, ARGTable o2) {    
            // Deolver 1 si o2 tiene que estar antes que o1
            // Devolver -1 si o1 tiene que estar antes que o2
            // devoler 0 si da igual
            long time1 = o1.getLong("time");
            long time2 = o2.getLong("time");
            if (time1 > time2)
                return -1;
            else if (time2 > time1)
                return 1;
            else
                return 0;
        }   
    }

    public void addARGInsulin(long fromTime, long toTime) {
        FixedLineGraphSeries<ScaledDataPoint> insulinSeries;
        List<ScaledDataPoint> insulinArray = new ArrayList<>();
        List<DataPointWithLabelInterface> initBolusArray = new ArrayList<>();
        Double maxInsulinValueFound = Double.MIN_VALUE;
        double lastInsulin = 0;
        long lastInsulinTime = 0;
        Scale insulinScale = new Scale();

        List<ARGTable> bolusData = MainApp.getDbHelper()
                    .getAllARGTableFromTimeByDiASType("Biometrics.INSULIN_URI", fromTime, false);
        //List<ARGTable>

        log.debug("[ARG_GUI] Dibujando insulina desde "
         + fromTime + " hasta " + toTime + " (" + bolusData.size() + " resultados )");

        Collections.sort(bolusData, new DelivTimeInJSONComparator());

        //bolusData.sort()
        // esta ordenado desde la ultima medida (0) hasta la mas antigua (N)
        // Lo recorro desde la mas antigua hasta la mas nueva
        for (int i = bolusData.size() - 1;i >= 0; i--){
            double bolus = bolusData.get(i).getDouble("deliv_total");
            long time = bolusData.get(i).getLong("deliv_time") * 1000; // Paso de segs a ms
            int type = bolusData.get(i).getInt("type");

            if (time >= fromTime && time <= toTime){
                if (type == 2){
                    if ( lastInsulinTime > 0){
                        // Tolerancia de 60 segundos (delays de comunicacion con bomba)
                        // para que el grafico sea continuo, sino, que la discontinuidad
                        // sea visual para ver que hay lapsos sin infundir
                        if (time - lastInsulinTime > ((5*60) + 60) * 1000L){
                            insulinArray.add(new ScaledDataPoint(lastInsulinTime + (5*60*1000L), lastInsulin, insulinScale));   
                            insulinArray.add(new ScaledDataPoint(lastInsulinTime + (5*60*1000L) + 10, 0, insulinScale));    
                            insulinArray.add(new ScaledDataPoint(time - 10, 0, insulinScale));  
                            insulinArray.add(new ScaledDataPoint(time, bolus, insulinScale));    
                        }else{
                            insulinArray.add(new ScaledDataPoint(time - 10, lastInsulin, insulinScale));  
                            insulinArray.add(new ScaledDataPoint(time, bolus, insulinScale));    
                        }
                    }else{
                        insulinArray.add(new ScaledDataPoint(time-10, 0, insulinScale)); 
                        insulinArray.add(new ScaledDataPoint(time, bolus, insulinScale)); 
                    }

                    lastInsulinTime = time;
                    lastInsulin = bolus;
                    
                    maxInsulinValueFound = Math.max(maxInsulinValueFound, Math.abs(bolus));

                    log.debug("[ARG_GUI] INSULINA EN GRAFICO TIEMPO " + time + " de " + bolus);
                }else{
                    ARGDataPoint p = new ARGDataPoint();
                    p.dp_x = time;
                    p.dp_y = -1;
                    p.dp_shape = PointsWithLabelGraphSeries.Shape.ARGFOOD;
                    p.dp_color = 0xff01B9FF;
                    p.dp_label = "I: " + bolus + "U";

                    initBolusArray.add(p);
                }
            }else{
                log.debug("[ARG_GUI] Insulina ignorada en grafico tiempo " + time + " de " + bolus);
            }

        }

        if ( lastInsulinTime > 0){
            if (toTime - lastInsulinTime > ((5*60) + 30) * 1000L){
                insulinArray.add(new ScaledDataPoint(lastInsulinTime + (5*60*1000L), lastInsulin, insulinScale));   
                insulinArray.add(new ScaledDataPoint(lastInsulinTime + (5*60*1000L) + 10, 0, insulinScale));
            }else{
                insulinArray.add(new ScaledDataPoint(toTime, lastInsulin, insulinScale));
            }
        }

        maxY = Math.max(maxInsulinValueFound, 2);
        minY = 0;

        ARGDataPoint title = new ARGDataPoint();
        title.dp_shape = PointsWithLabelGraphSeries.Shape.ARGTITLE;
        title.dp_color = 0xffffffff;
        title.dp_label = "INS";
        title.dp_x = fromTime;
        title.dp_y = maxY;
        initBolusArray.add(title);


        List<ARGTable> bacData = MainApp.getDbHelper()
                    .getAllARGTableFromTimeByDiASType("ARG_REP_BAC", fromTime, false);

        for (int i = 0; i< bacData.size(); i++) {
            long time = bacData.get(i).getLong("time") * 1000; // Paso de segs a ms

            if (!(time < fromTime || time > toTime)){
                ARGDataPoint bac = new ARGDataPoint();

                bac.dp_x = time;
                bac.dp_y = maxY/2;
                bac.dp_shape = PointsWithLabelGraphSeries.Shape.ARGBAC;
                bac.dp_color = 0xFFFF0000;
                bac.dp_label = "BAC";

                initBolusArray.add(bac);
            }
        }

        DataPointWithLabelInterface[] filteredExtrasArray = new DataPointWithLabelInterface[initBolusArray.size()];
        filteredExtrasArray = initBolusArray.toArray(filteredExtrasArray);

        ScaledDataPoint[] insulinData = new ScaledDataPoint[insulinArray.size()];
        insulinData = insulinArray.toArray(insulinData);
        insulinSeries = new FixedLineGraphSeries<>(insulinData);
        insulinSeries.setDrawBackground(true);
        insulinSeries.setBackgroundColor(0x8042eef4); //50%
        insulinSeries.setColor(0xFF42EEF4);
        insulinSeries.setThickness(3);
        insulinScale.setMultiplier(1);
        
        addSeries(insulinSeries);
        addSeries(new PointsWithLabelGraphSeries<>(filteredExtrasArray));
    }

    public void addARGIob(long fromTime, long toTime, boolean useForScale, double scale) {
        FixedLineGraphSeries<ScaledDataPoint> iobSeries;

        List<DataPointWithLabelInterface> iobPoints = new ArrayList<>();
        List<ScaledDataPoint> iobArray = new ArrayList<>();
        Double maxIobValueFound = Double.MIN_VALUE;
        double lastIob = 0;
        long lastIobTime = 0;
        Scale iobScale = new Scale();

        List<ARGTable> iobARGData = MainApp.getDbHelper()
                    .getAllARGTableFromTimeByDiASType("ARG_IOB_STATES", fromTime, false);

        log.debug("[ARG_GUI] Dibujando iob estimado desde " + fromTime + " hasta " + toTime);


        //bolusData.sort()
        // esta ordenado desde la ultima medida (0) hasta la mas antigua (N)
        // Lo recorro desde la mas antigua hasta la mas nueva
        for (int i = iobARGData.size() - 1;i >= 0; i--){
            double iob = iobARGData.get(i).getDouble("iobEst");

            long time = iobARGData.get(i).getLong("time");
            if (time < 1)
                time = iobARGData.get(i).date;

            // Chequeo para ver si es de aproximadamente el mismo momento
            // el calculo de IOB, entonces calculo el mas reciente

            // Como estamos recorriendo desde el mas viejo al mas reciente
            // el primero que agarre es mas antiguo que los que siguen
            // entonces tengo que ver hacia adelante si hay muestras mas 
            // recientes, y directamente saltar hacia ellas.

            // Si i == 0, entonces estoy en la ultima de las ultimas
            if (i > 0){
                while (i > 0){
                    long timeNext = iobARGData.get(i-1).getLong("time");
                    if (timeNext < 1)
                        timeNext = iobARGData.get(i-1).date;


                    if (timeNext - time >= 0){

                        // Como mucho 1 segundo despues
                        // Me muevo a la siguiente con el i y actualizo el iob y time leido
                        // Estoy contemplando el caso en el que timeNext - time == 0
                        // que es cuando para el reloj estoy en el mismo lapso de tiempo cuantizado
                        
                        if (timeNext - time < 1000){
                            time = timeNext;
                            iob = iobARGData.get(i-1).getDouble("iobEst");
                            i--;
                        }else{
                            break;
                        }
                    }else{
                        // NO DEBERÍA ENTRAR ACA
                        // UN BUG DE JAVA??
                        log.debug("[ARG_GUI] Grafico IOB - Entre a una muestra siguiente que es mas antigua que la actual ¡Imposible!");                        
                    
                        /// Salgo para no entrar en loop infinito
                        break;
                    }
                }
            }

            if (time >= fromTime && time <= toTime){

                ARGDataPoint p = new ARGDataPoint();
                p.dp_x = time;
                p.dp_y = iob;
                p.dp_shape = PointsWithLabelGraphSeries.Shape.ARGIOB;
                p.dp_color = 0xffffffff;
                iobPoints.add(p);

                if ( lastIobTime > 0){  
                    iobArray.add(new ScaledDataPoint(time, iob, iobScale));    
                }else{
                    iobArray.add(new ScaledDataPoint(time-10, 0, iobScale)); 
                    iobArray.add(new ScaledDataPoint(time, iob, iobScale)); 
                }

                lastIobTime = time;
                lastIob = iob;

                maxIobValueFound = Math.max(maxIobValueFound, Math.abs(iob));

                log.debug("[ARG_GUI] IOB ESTIMADO EN GRAFICO TIEMPO " + time + " de " + iob);
            }else{
                log.debug("[ARG_GUI] IOB Estimado ignorado en grafico tiempo " + time + " de " + iob);
            }

        }


        ScaledDataPoint[] iobData = new ScaledDataPoint[iobArray.size()];
        iobData = iobArray.toArray(iobData);
        iobSeries = new FixedLineGraphSeries<>(iobData);
        iobSeries.setDrawBackground(true);
        iobSeries.setBackgroundColor(0x8042eef4); //50%
        iobSeries.setColor(0xFF42EEF4);
        iobSeries.setThickness(3);

        if (useForScale) {
            maxY = maxIobValueFound;
            minY = 0;
        }   

        iobScale.setMultiplier(maxY * scale / maxIobValueFound);
        addSeries(iobSeries);

        ARGDataPoint title = new ARGDataPoint();
        title.dp_shape = PointsWithLabelGraphSeries.Shape.ARGTITLE;
        title.dp_color = 0xffffffff;
        title.dp_label = "IOB";
        title.dp_x = fromTime;
        title.dp_y = maxY;
        iobPoints.add(title);

        DataPointWithLabelInterface[] filteredExtrasArray = new DataPointWithLabelInterface[iobPoints.size()];
        filteredExtrasArray = iobPoints.toArray(filteredExtrasArray);
        addSeries(new PointsWithLabelGraphSeries<>(filteredExtrasArray));
    }

    public void addARGExtras(long fromTime, long toTime) {
        List<DataPointWithLabelInterface> filteredExtras = new ArrayList<>();

        List<ARGTable> mealData = MainApp.getDbHelper()
                    .getAllARGTableFromTimeByDiASType("ARG_MEAL", fromTime, false);
        

        // COMIDAS
        for (int tx = 0; tx < mealData.size(); tx++) {
            ARGTable t = mealData.get(tx);

            if (!(t.date < fromTime || t.date > toTime)){
                ARGDataPoint p = new ARGDataPoint();
                int tipo = t.getInt("mealClass");

                p.dp_x = t.date;
                p.dp_y = -1;
                p.dp_shape = PointsWithLabelGraphSeries.Shape.ARGFOOD;
                p.dp_color = 0xfff2a935;
                
                if (tipo == 1)
                    p.dp_label = "Chica";
                else if (tipo == 2)
                    p.dp_label = "Media";
                else if (tipo == 3)
                    p.dp_label = "Grande";
                else
                    p.dp_label = "Descon.";

                filteredExtras.add(p);

                log.debug("[ARG_GUI] COMIDA ENCONTRADA");
            }else{

                log.debug("[ARG_GUI] COMIDA IGNORADA");
            }
        }


        List<ARGTable> bolusData = MainApp.getDbHelper()
                    .getAllARGTableFromTimeByDiASType("Biometrics.INSULIN_URI", fromTime, false);


        // Como maximo 24 en total time
        long minTimeDiff = (toTime - fromTime) / 23;
        long lastTime = 0;
/*
        // Bolos comunes
        for (int i = 0; i< bolusData.size(); i++) {
            double bolus = bolusData.get(i).getDouble("deliv_total");
            long time = bolusData.get(i).getLong("deliv_time") * 1000; // Paso de segs a ms
            int type = bolusData.get(i).getInt("type");

            if (!(time < fromTime || time > toTime) && (Math.abs(time - lastTime) > minTimeDiff)){
                if (type == 2){
                    ARGDataPoint p = new ARGDataPoint();

                    p.dp_x = time;
                    p.dp_y = -2;
                    p.dp_shape = PointsWithLabelGraphSeries.Shape.ARGBOLUS;
                    p.dp_color = 0xFF42EEF4;
                    p.dp_label = String.valueOf(bolus) + "U";

                    lastTime = time;

                    filteredExtras.add(p);
                }
            }
        }*/



        DataPointWithLabelInterface[] filteredExtrasArray = new DataPointWithLabelInterface[filteredExtras.size()];
        filteredExtrasArray = filteredExtras.toArray(filteredExtrasArray);
        addSeries(new PointsWithLabelGraphSeries<>(filteredExtrasArray));
    }


    public void addTargetLine(long fromTime, long toTime, Profile profile) {
        LineGraphSeries<DataPoint> targetsSeries;

        Scale targetsScale = new Scale();
        targetsScale.setMultiplier(1);

        List<DataPoint> targetsSeriesArray = new ArrayList<>();
        double lastTarget = -1;

        if (LoopPlugin.lastRun != null && LoopPlugin.lastRun.constraintsProcessed != null) {
            APSResult apsResult = LoopPlugin.lastRun.constraintsProcessed;
            long latestPredictionsTime = apsResult.getLatestPredictionsTime();
            if (latestPredictionsTime > toTime) {
                toTime = latestPredictionsTime;
            }
        }

        for (long time = fromTime; time < toTime; time += 5 * 60 * 1000L) {
            TempTarget tt = TreatmentsPlugin.getPlugin().getTempTargetFromHistory(time);
            double value;
            if (tt == null) {
                value = (profile.getTargetLow(time) + profile.getTargetHigh(time)) / 2;
            } else {
                value = Profile.fromMgdlToUnits(tt.target(), profile.getUnits());
            }
            if (lastTarget != value) {
                if (lastTarget != -1)
                    targetsSeriesArray.add(new DataPoint(time, lastTarget));
                targetsSeriesArray.add(new DataPoint(time, value));
            }
            lastTarget = value;
        }
        targetsSeriesArray.add(new DataPoint(toTime, lastTarget));

        DataPoint[] targets = new DataPoint[targetsSeriesArray.size()];
        targets = targetsSeriesArray.toArray(targets);
        targetsSeries = new LineGraphSeries<>(targets);
        targetsSeries.setDrawBackground(false);
        targetsSeries.setColor(MainApp.gc(R.color.tempTargetBackground));
        targetsSeries.setThickness(2);

        addSeries(targetsSeries);
    }

    public void addTreatments(long fromTime, long endTime, boolean drawBolus) {
        List<DataPointWithLabelInterface> filteredTreatments = new ArrayList<>();

        List<Treatment> treatments = TreatmentsPlugin.getPlugin().getTreatmentsFromHistory();
  

        // TODO_APS: Esto corresponde a los triangulitos de bolos y a los textos en gris 
        if (drawBolus){
            for (int tx = 0; tx < treatments.size(); tx++) {
                Treatment t = treatments.get(tx);
                if (t.getX() < fromTime || t.getX() > endTime) continue;
                if (t.isSMB && !t.isValid) continue;
                t.setY(getNearestBg((long) t.getX()));
                filteredTreatments.add(t);
            }

            // ProfileSwitch
            List<ProfileSwitch> profileSwitches = TreatmentsPlugin.getPlugin().getProfileSwitchesFromHistory().getList();

            for (int tx = 0; tx < profileSwitches.size(); tx++) {
                DataPointWithLabelInterface t = profileSwitches.get(tx);
                if (t.getX() < fromTime || t.getX() > endTime) continue;
                filteredTreatments.add(t);
            }
        }

        // Extended bolus
        if (!ConfigBuilderPlugin.getPlugin().getActivePump().isFakingTempsByExtendedBoluses()) {
            List<ExtendedBolus> extendedBoluses = TreatmentsPlugin.getPlugin().getExtendedBolusesFromHistory().getList();

            for (int tx = 0; tx < extendedBoluses.size(); tx++) {
                DataPointWithLabelInterface t = extendedBoluses.get(tx);
                if (t.getX() + t.getDuration() < fromTime || t.getX() > endTime) continue;
                if (t.getDuration() == 0) continue;
                t.setY(getNearestBg((long) t.getX()));
                filteredTreatments.add(t);
            }
        }

        // Careportal
        List<CareportalEvent> careportalEvents = MainApp.getDbHelper().getCareportalEventsFromTime(fromTime - 6 * 60 * 60 * 1000, true);

        for (int tx = 0; tx < careportalEvents.size(); tx++) {
            DataPointWithLabelInterface t = careportalEvents.get(tx);
            if (t.getX() + t.getDuration() < fromTime || t.getX() > endTime) continue;
            t.setY(getNearestBg((long) t.getX()));
            filteredTreatments.add(t);
        }

        DataPointWithLabelInterface[] treatmentsArray = new DataPointWithLabelInterface[filteredTreatments.size()];
        treatmentsArray = filteredTreatments.toArray(treatmentsArray);
        addSeries(new PointsWithLabelGraphSeries<>(treatmentsArray));
    }

    private double getNearestBg(long date) {
        if (bgReadingsArray == null)
            return Profile.fromMgdlToUnits(100, units);
        for (int r = 0; r < bgReadingsArray.size(); r++) {
            BgReading reading = bgReadingsArray.get(r);
            if (reading.date > date) continue;
            return Profile.fromMgdlToUnits(reading.value, units);
        }
        return bgReadingsArray.size() > 0
                ? Profile.fromMgdlToUnits(bgReadingsArray.get(0).value, units) : Profile.fromMgdlToUnits(100, units);
    }

    // scale in % of vertical size (like 0.3)
    public void addIob(long fromTime, long toTime, boolean useForScale, double scale) {
        FixedLineGraphSeries<ScaledDataPoint> iobSeries;
        List<ScaledDataPoint> iobArray = new ArrayList<>();
        Double maxIobValueFound = Double.MIN_VALUE;
        double lastIob = 0;
        Scale iobScale = new Scale();

        for (long time = fromTime; time <= toTime; time += 5 * 60 * 1000L) {
            Profile profile = ProfileFunctions.getInstance().getProfile(time);
            double iob = 0d;
            if (profile != null)
                iob = iobCobCalculatorPlugin.calculateFromTreatmentsAndTempsSynchronized(time, profile).iob;
            if (Math.abs(lastIob - iob) > 0.02) {
                if (Math.abs(lastIob - iob) > 0.2)
                    iobArray.add(new ScaledDataPoint(time, lastIob, iobScale));
                iobArray.add(new ScaledDataPoint(time, iob, iobScale));
                maxIobValueFound = Math.max(maxIobValueFound, Math.abs(iob));
                lastIob = iob;
                log.debug("[ARG_GUI] IOB AAPS EN GRAFICO TIEMPO " + time + " de " + iob);
            }else{
                log.debug("[ARG_GUI] IOB AAPS ignorado en grafico tiempo " + time + " de " + iob);
            }
        }

        ScaledDataPoint[] iobData = new ScaledDataPoint[iobArray.size()];
        iobData = iobArray.toArray(iobData);
        iobSeries = new FixedLineGraphSeries<>(iobData);
        iobSeries.setDrawBackground(true);
        iobSeries.setBackgroundColor(0x80FFFFFF & MainApp.gc(R.color.iob)); //50%
        iobSeries.setColor(MainApp.gc(R.color.iob));
        iobSeries.setThickness(3);

        if (useForScale) {
            maxY = maxIobValueFound;
            minY = -maxIobValueFound;
        }

        iobScale.setMultiplier(maxY * scale / maxIobValueFound);

        addSeries(iobSeries);
    }

    // scale in % of vertical size (like 0.3)
    public void addCob(long fromTime, long toTime, boolean useForScale, double scale) {
        List<DataPointWithLabelInterface> minFailoverActiveList = new ArrayList<>();
        FixedLineGraphSeries<ScaledDataPoint> cobSeries;
        List<ScaledDataPoint> cobArray = new ArrayList<>();
        Double maxCobValueFound = 0d;
        int lastCob = 0;
        Scale cobScale = new Scale();

        for (long time = fromTime; time <= toTime; time += 5 * 60 * 1000L) {
            AutosensData autosensData = iobCobCalculatorPlugin.getAutosensData(time);
            if (autosensData != null) {
                int cob = (int) autosensData.cob;
                if (cob != lastCob) {
                    if (autosensData.carbsFromBolus > 0)
                        cobArray.add(new ScaledDataPoint(time, lastCob, cobScale));
                    cobArray.add(new ScaledDataPoint(time, cob, cobScale));
                    maxCobValueFound = Math.max(maxCobValueFound, cob);
                    lastCob = cob;
                }
                if (autosensData.failoverToMinAbsorbtionRate) {
                    autosensData.setScale(cobScale);
                    autosensData.setChartTime(time);
                    minFailoverActiveList.add(autosensData);
                }
            }
        }

        // COB
        ScaledDataPoint[] cobData = new ScaledDataPoint[cobArray.size()];
        cobData = cobArray.toArray(cobData);
        cobSeries = new FixedLineGraphSeries<>(cobData);
        cobSeries.setDrawBackground(true);
        cobSeries.setBackgroundColor(0x80FFFFFF & MainApp.gc(R.color.cob)); //50%
        cobSeries.setColor(MainApp.gc(R.color.cob));
        cobSeries.setThickness(3);

        if (useForScale) {
            maxY = maxCobValueFound;
            minY = 0;
        }

        cobScale.setMultiplier(maxY * scale / maxCobValueFound);

        addSeries(cobSeries);

        DataPointWithLabelInterface[] minFailover = new DataPointWithLabelInterface[minFailoverActiveList.size()];
        minFailover = minFailoverActiveList.toArray(minFailover);
        addSeries(new PointsWithLabelGraphSeries<>(minFailover));
    }

    // scale in % of vertical size (like 0.3)
    public void addDeviations(long fromTime, long toTime, boolean useForScale, double scale) {
        class DeviationDataPoint extends ScaledDataPoint {
            public int color;

            public DeviationDataPoint(double x, double y, int color, Scale scale) {
                super(x, y, scale);
                this.color = color;
            }
        }

        BarGraphSeries<DeviationDataPoint> devSeries;
        List<DeviationDataPoint> devArray = new ArrayList<>();
        Double maxDevValueFound = 0d;
        Scale devScale = new Scale();

        for (long time = fromTime; time <= toTime; time += 5 * 60 * 1000L) {
            AutosensData autosensData = iobCobCalculatorPlugin.getAutosensData(time);
            if (autosensData != null) {
                int color = MainApp.gc(R.color.deviationblack); // "="
                if (autosensData.type.equals("") || autosensData.type.equals("non-meal")) {
                    if (autosensData.pastSensitivity.equals("C"))
                        color = MainApp.gc(R.color.deviationgrey);
                    if (autosensData.pastSensitivity.equals("+"))
                        color = MainApp.gc(R.color.deviationgreen);
                    if (autosensData.pastSensitivity.equals("-"))
                        color = MainApp.gc(R.color.deviationred);
                } else if (autosensData.type.equals("uam")) {
                    color = MainApp.gc(R.color.uam);
                } else if (autosensData.type.equals("csf")) {
                    color = MainApp.gc(R.color.deviationgrey);
                }
                devArray.add(new DeviationDataPoint(time, autosensData.deviation, color, devScale));
                maxDevValueFound = Math.max(maxDevValueFound, Math.abs(autosensData.deviation));
            }
        }

        // DEVIATIONS
        DeviationDataPoint[] devData = new DeviationDataPoint[devArray.size()];
        devData = devArray.toArray(devData);
        devSeries = new BarGraphSeries<>(devData);
        devSeries.setValueDependentColor(new ValueDependentColor<DeviationDataPoint>() {
            @Override
            public int get(DeviationDataPoint data) {
                return data.color;
            }
        });

        if (useForScale) {
            maxY = maxDevValueFound;
            minY = -maxY;
        }

        devScale.setMultiplier(maxY * scale / maxDevValueFound);

        addSeries(devSeries);
    }

    // scale in % of vertical size (like 0.3)
    public void addRatio(long fromTime, long toTime, boolean useForScale, double scale) {
        LineGraphSeries<ScaledDataPoint> ratioSeries;
        List<ScaledDataPoint> ratioArray = new ArrayList<>();
        Double maxRatioValueFound = Double.MIN_VALUE;
        Double minRatioValueFound = Double.MAX_VALUE;
        Scale ratioScale = new Scale();

        for (long time = fromTime; time <= toTime; time += 5 * 60 * 1000L) {
            AutosensData autosensData = iobCobCalculatorPlugin.getAutosensData(time);
            if (autosensData != null) {
                ratioArray.add(new ScaledDataPoint(time, autosensData.autosensResult.ratio - 1, ratioScale));
                maxRatioValueFound = Math.max(maxRatioValueFound, autosensData.autosensResult.ratio - 1);
                minRatioValueFound = Math.min(minRatioValueFound, autosensData.autosensResult.ratio - 1);
            }
        }

        // RATIOS
        ScaledDataPoint[] ratioData = new ScaledDataPoint[ratioArray.size()];
        ratioData = ratioArray.toArray(ratioData);
        ratioSeries = new LineGraphSeries<>(ratioData);
        ratioSeries.setColor(MainApp.gc(R.color.ratio));
        ratioSeries.setThickness(3);

        if (useForScale) {
            maxY = Math.max(maxRatioValueFound, Math.abs(minRatioValueFound));
            minY = -maxY;
        }

        ratioScale.setMultiplier(maxY * scale / Math.max(maxRatioValueFound, Math.abs(minRatioValueFound)));

        addSeries(ratioSeries);
    }

    // scale in % of vertical size (like 0.3)
    public void addDeviationSlope(long fromTime, long toTime, boolean useForScale, double scale) {
        LineGraphSeries<ScaledDataPoint> dsMaxSeries;
        LineGraphSeries<ScaledDataPoint> dsMinSeries;
        List<ScaledDataPoint> dsMaxArray = new ArrayList<>();
        List<ScaledDataPoint> dsMinArray = new ArrayList<>();
        Double maxFromMaxValueFound = 0d;
        Double maxFromMinValueFound = 0d;
        Scale dsMaxScale = new Scale();
        Scale dsMinScale = new Scale();

        for (long time = fromTime; time <= toTime; time += 5 * 60 * 1000L) {
            AutosensData autosensData = iobCobCalculatorPlugin.getAutosensData(time);
            if (autosensData != null) {
                dsMaxArray.add(new ScaledDataPoint(time, autosensData.slopeFromMaxDeviation, dsMaxScale));
                dsMinArray.add(new ScaledDataPoint(time, autosensData.slopeFromMinDeviation, dsMinScale));
                maxFromMaxValueFound = Math.max(maxFromMaxValueFound, Math.abs(autosensData.slopeFromMaxDeviation));
                maxFromMinValueFound = Math.max(maxFromMinValueFound, Math.abs(autosensData.slopeFromMinDeviation));
            }
        }

        // Slopes
        ScaledDataPoint[] ratioMaxData = new ScaledDataPoint[dsMaxArray.size()];
        ratioMaxData = dsMaxArray.toArray(ratioMaxData);
        dsMaxSeries = new LineGraphSeries<>(ratioMaxData);
        dsMaxSeries.setColor(MainApp.gc(R.color.devslopepos));
        dsMaxSeries.setThickness(3);

        ScaledDataPoint[] ratioMinData = new ScaledDataPoint[dsMinArray.size()];
        ratioMinData = dsMinArray.toArray(ratioMinData);
        dsMinSeries = new LineGraphSeries<>(ratioMinData);
        dsMinSeries.setColor(MainApp.gc(R.color.devslopeneg));
        dsMinSeries.setThickness(3);

        if (useForScale) {
            maxY = Math.max(maxFromMaxValueFound, maxFromMinValueFound);
            minY = -maxY;
        }

        dsMaxScale.setMultiplier(maxY * scale / maxFromMaxValueFound);
        dsMinScale.setMultiplier(maxY * scale / maxFromMinValueFound);

        addSeries(dsMaxSeries);
        addSeries(dsMinSeries);
    }

    // scale in % of vertical size (like 0.3)
    public void addNowLine(long now) {
        LineGraphSeries<DataPoint> seriesNow;
        DataPoint[] nowPoints = new DataPoint[]{
                new DataPoint(now, 0),
                new DataPoint(now, maxY)
        };

        seriesNow = new LineGraphSeries<>(nowPoints);
        seriesNow.setDrawDataPoints(false);
        // custom paint to make a dotted line
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(2);
        paint.setPathEffect(new DashPathEffect(new float[]{10, 20}, 0));
        paint.setColor(Color.WHITE);
        seriesNow.setCustomPaint(paint);

        addSeries(seriesNow);
    }

    public void formatAxis(long fromTime, long endTime) {
        graph.getViewport().setMaxX(endTime);
        graph.getViewport().setMinX(fromTime);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.getGridLabelRenderer().setLabelFormatter(new TimeAsXAxisLabelFormatter("HH:mm"));
        graph.getGridLabelRenderer().setNumHorizontalLabels(7); // only 7 because of the space
    }

    private void addSeries(Series s) {
        series.add(s);
    }

    public void performUpdate() {
        // clear old data
        graph.getSeries().clear();

        // add precalculated series
        for (Series s : series) {
            if (!s.isEmpty()) {
                s.onGraphViewAttached(graph);
                graph.getSeries().add(s);
            }
        }

        double step = 1d;
        if (maxY < 1) step = 0.1d;
        graph.getViewport().setMaxY(Round.ceilTo(maxY, step));
        graph.getViewport().setMinY(Round.floorTo(minY, step));
        graph.getViewport().setYAxisBoundsManual(true);

        // draw it
        graph.onDataChanged(false, false);
    }
}
