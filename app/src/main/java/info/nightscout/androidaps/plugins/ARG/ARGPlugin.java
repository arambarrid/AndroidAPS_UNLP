 package info.nightscout.androidaps.plugins.ARG;

import android.content.Context;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.GlucoseStatus;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.data.MealData;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.db.TempTarget;
import info.nightscout.androidaps.db.ARGTable;
import info.nightscout.androidaps.interfaces.APSInterface;
import info.nightscout.androidaps.interfaces.Constraint;
import info.nightscout.androidaps.interfaces.PluginBase;
import info.nightscout.androidaps.interfaces.PluginDescription;
import info.nightscout.androidaps.interfaces.PluginType;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.IobCobCalculator.AutosensData;
import info.nightscout.androidaps.plugins.IobCobCalculator.AutosensResult;
import info.nightscout.androidaps.plugins.IobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.plugins.Loop.APSResult;
import info.nightscout.androidaps.plugins.Loop.ScriptReader;
import info.nightscout.androidaps.plugins.NSClientInternal.NSUpload;
import info.nightscout.androidaps.plugins.OpenAPSMA.events.EventOpenAPSUpdateGui;
import info.nightscout.androidaps.plugins.OpenAPSMA.events.EventOpenAPSUpdateResultGui;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.HardLimits;
import info.nightscout.utils.Profiler;
import info.nightscout.utils.Round;
import info.nightscout.utils.ToastUtils;




public class ARGPlugin extends PluginBase implements APSInterface {
    //prueba
    double resultado;
    GController gController;
    IOMain ioMain;

    private static final String STRING_ARRAY_SAMPLE = "./string-array-sample.csv";
    private static CSVReader reader=null;
    private static boolean firstExecution=true;
    private static Context miContexto;
    //prueba
    private static Logger log = LoggerFactory.getLogger(L.APS);

    private static ARGPlugin argPlugin;

    public static ARGPlugin getPlugin(Context contexto) {
        if (argPlugin == null) {
            argPlugin = new ARGPlugin();
        }
        miContexto=contexto;
        return argPlugin;
    }

    // last values

    DetermineBasalAdapterARG lastDetermineBasalAdapterARG = null;
    long lastAPSRun = 0;
    DetermineBasalResultARG lastAPSResult = null;
    AutosensResult lastAutosensResult = null;

    private ARGPlugin() {
        super(new PluginDescription()
                .mainType(PluginType.APS)
                .fragmentClass(ARGFragment.class.getName())
                .pluginName(R.string.openapsarg)
                .shortName(R.string.arg_shortname)
                .preferencesId(R.xml.pref_openapssmb)
                .description(R.string.description_arg)
        );
    }

    @Override
    public boolean specialEnableCondition() {
        PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();
        return pump == null || pump.getPumpDescription().isTempBasalCapable;
    }

    @Override
    public boolean specialShowInListCondition() {
        PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();
        return pump == null || pump.getPumpDescription().isTempBasalCapable;
    }

    @Override
    public APSResult getLastAPSResult() {
        return lastAPSResult;
    }

    @Override
    public long getLastAPSRun() {
        return lastAPSRun;
    }

    @Override
    public void invoke(String initiator, boolean tempBasalFallback) throws IOException {
        if (L.isEnabled(L.APS))
            log.debug("invoke from " + initiator + " tempBasalFallback: " + tempBasalFallback);
        lastAPSResult = null;
        DetermineBasalAdapterARG determineBasalAdapterARG;
        determineBasalAdapterARG = new DetermineBasalAdapterARG(new ScriptReader(MainApp.instance().getBaseContext()));

        GlucoseStatus glucoseStatus = GlucoseStatus.getGlucoseStatusData();
        Profile profile = ProfileFunctions.getInstance().getProfile();
        PumpInterface pump = ConfigBuilderPlugin.getPlugin().getActivePump();

        if (profile == null) {
            MainApp.bus().post(new EventOpenAPSUpdateResultGui(MainApp.gs(R.string.noprofileselected)));
            if (L.isEnabled(L.APS))
                log.debug(MainApp.gs(R.string.noprofileselected));
            return;
        }

        if (!isEnabled(PluginType.APS)) {
            MainApp.bus().post(new EventOpenAPSUpdateResultGui(MainApp.gs(R.string.openapsma_disabled)));
            if (L.isEnabled(L.APS))
                log.debug(MainApp.gs(R.string.openapsma_disabled));
            return;
        }

        if (glucoseStatus == null) {
            MainApp.bus().post(new EventOpenAPSUpdateResultGui(MainApp.gs(R.string.openapsma_noglucosedata)));
            if (L.isEnabled(L.APS))
                log.debug(MainApp.gs(R.string.openapsma_noglucosedata));
            return;
        }

        String units = profile.getUnits();

        Constraint<Double> inputConstraints = new Constraint<>(0d); // fake. only for collecting all results

        Constraint<Double> maxBasalConstraint = MainApp.getConstraintChecker().getMaxBasalAllowed(profile);
        inputConstraints.copyReasons(maxBasalConstraint);
        double maxBasal = maxBasalConstraint.value();
        double minBg = Profile.toMgdl(profile.getTargetLow(), units);
        double maxBg = Profile.toMgdl(profile.getTargetHigh(), units);
        double targetBg = Profile.toMgdl(profile.getTarget(), units);

        minBg = Round.roundTo(minBg, 0.1d);
        maxBg = Round.roundTo(maxBg, 0.1d);

        long start = System.currentTimeMillis();
        long startPart = System.currentTimeMillis();
        IobTotal[] iobArray = IobCobCalculatorPlugin.getPlugin().calculateIobArrayForSMB(profile);
        if (L.isEnabled(L.APS))
            Profiler.log(log, "calculateIobArrayInDia()", startPart);

        startPart = System.currentTimeMillis();
        MealData mealData = TreatmentsPlugin.getPlugin().getMealData();
        if (L.isEnabled(L.APS))
            Profiler.log(log, "getMealData()", startPart);

        double maxIob = MainApp.getConstraintChecker().getMaxIOBAllowed().value();

        minBg = verifyHardLimits(minBg, "minBg", HardLimits.VERY_HARD_LIMIT_MIN_BG[0], HardLimits.VERY_HARD_LIMIT_MIN_BG[1]);
        maxBg = verifyHardLimits(maxBg, "maxBg", HardLimits.VERY_HARD_LIMIT_MAX_BG[0], HardLimits.VERY_HARD_LIMIT_MAX_BG[1]);
        targetBg = verifyHardLimits(targetBg, "targetBg", HardLimits.VERY_HARD_LIMIT_TARGET_BG[0], HardLimits.VERY_HARD_LIMIT_TARGET_BG[1]);

        boolean isTempTarget = false;
        TempTarget tempTarget = TreatmentsPlugin.getPlugin().getTempTargetFromHistory(System.currentTimeMillis());
        if (tempTarget != null) {
            isTempTarget = true;
            minBg = verifyHardLimits(tempTarget.low, "minBg", HardLimits.VERY_HARD_LIMIT_TEMP_MIN_BG[0], HardLimits.VERY_HARD_LIMIT_TEMP_MIN_BG[1]);
            maxBg = verifyHardLimits(tempTarget.high, "maxBg", HardLimits.VERY_HARD_LIMIT_TEMP_MAX_BG[0], HardLimits.VERY_HARD_LIMIT_TEMP_MAX_BG[1]);
            targetBg = verifyHardLimits(tempTarget.target(), "targetBg", HardLimits.VERY_HARD_LIMIT_TEMP_TARGET_BG[0], HardLimits.VERY_HARD_LIMIT_TEMP_TARGET_BG[1]);
        }


        if (!checkOnlyHardLimits(profile.getDia(), "dia", HardLimits.MINDIA, HardLimits.MAXDIA))
            return;
        if (!checkOnlyHardLimits(profile.getIcTimeFromMidnight(Profile.secondsFromMidnight()), "carbratio", HardLimits.MINIC, HardLimits.MAXIC))
            return;
        if (!checkOnlyHardLimits(Profile.toMgdl(profile.getIsf(), units), "sens", HardLimits.MINISF, HardLimits.MAXISF))
            return;
        if (!checkOnlyHardLimits(profile.getMaxDailyBasal(), "max_daily_basal", 0.05, HardLimits.maxBasal()))
            return;
        if (!checkOnlyHardLimits(pump.getBaseBasalRate(), "current_basal", 0.01, HardLimits.maxBasal()))
            return;

        startPart = System.currentTimeMillis();
        if (MainApp.getConstraintChecker().isAutosensModeEnabled().value()) {
            AutosensData autosensData = IobCobCalculatorPlugin.getPlugin().getLastAutosensDataSynchronized("OpenAPSPlugin");
            if (autosensData == null) {
                MainApp.bus().post(new EventOpenAPSUpdateResultGui(MainApp.gs(R.string.openaps_noasdata)));
                return;
            }
            lastAutosensResult = autosensData.autosensResult;
        } else {
            lastAutosensResult = new AutosensResult();
            lastAutosensResult.sensResult = "autosens disabled";
        }

        Constraint<Boolean> smbAllowed = new Constraint<>(!tempBasalFallback);
        MainApp.getConstraintChecker().isSMBModeEnabled(smbAllowed);
        inputConstraints.copyReasons(smbAllowed);

        Constraint<Boolean> advancedFiltering = new Constraint<>(!tempBasalFallback);
        MainApp.getConstraintChecker().isAdvancedFilteringEnabled(advancedFiltering);
        inputConstraints.copyReasons(advancedFiltering);

        Constraint<Boolean> uam = new Constraint<>(true);
        MainApp.getConstraintChecker().isUAMEnabled(uam);
        inputConstraints.copyReasons(uam);

        if (L.isEnabled(L.APS))
            Profiler.log(log, "detectSensitivityandCarbAbsorption()", startPart);
        if (L.isEnabled(L.APS))
            Profiler.log(log, "SMB data gathering", start);

        start = System.currentTimeMillis();

        try {

            determineBasalAdapterARG.setData(profile, maxIob, maxBasal, minBg, maxBg, targetBg, ConfigBuilderPlugin.getPlugin().getActivePump().getBaseBasalRate(), iobArray, glucoseStatus, mealData,
                    lastAutosensResult.ratio, //autosensDataRatio
                    isTempTarget,
                    smbAllowed.value(),
                    uam.value(),
                    advancedFiltering.value()
            );

        } catch (JSONException e) {
            log.error(e.getMessage());
            return;
        }


        long now = System.currentTimeMillis();
        //-------------prueba------------
        String baseDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
        String anuncioFileName = "anuncio2.csv";
        String anuncioFilePath = baseDir + File.separator + anuncioFileName;
        String[] nextRecord;
        if(reader==null) {
            try {
                reader = new CSVReader(new FileReader(anuncioFilePath));
                nextRecord = reader.readNext(); //leo la primera linea porque sino no estan sincronizados cgm con anuncio
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
            if ((nextRecord = reader.readNext()) != null) {
                if (gController == null) {
                    gController = new GController(120.0, 25.0, 20.0, 20.0, 80.0, 1.22, miContexto);

                    double[][] xTemp = {{93.75},{93.75},{0}};
                    Matrix iobState  = new Matrix(xTemp);
                    gController.getSafe().getIob().setX(iobState);

                }
                long fromtime = DateUtil.now() - 60 * 1000L * 5; //ultimos 5 min
                List<BgReading> data = MainApp.getDbHelper().getBgreadingsDataFromTime(fromtime, false);
                resultado = gController.run(Boolean.valueOf(nextRecord[0]), 3, data.get(0).raw);
                double[][] xstates = gController.getSlqgController().getLqg().getX().getData();
                double [][] iobStates = gController.getSafe().getIob().getX().getData();
                String fileName = "TablaDeDatos2.csv";
                String filePath = baseDir + File.separator + fileName;
                CSVWriter writer = null;
                // File exist
                try {
                    FileWriter mFileWriter = new FileWriter(filePath, true);
                    writer = new CSVWriter(mFileWriter);
                    if(firstExecution) {
                        String[] headerRecord = {"time", "CGM", "IOB", "Gpred", "Gpred_correction", "Gpred_bolus", "Xi00", "Xi01", "Xi02", "Xi03", "Xi04", "Xi05", "Xi06", "Xi07", "brakes" +
                                "_coeff",  "tMeal", "ExtAgg", "pCBolus", "IobMax", "slqgState", "IOBMaxCF", "Listening", "MCount", "rCFBolus", "tEndAgg", "iobStates[0][0]", "iobStates[1][0]", "derivadaIOB", "iobEst", "Gamma", "Anuncio", "Resultado"};
                        writer.writeNext(headerRecord);
                        firstExecution=false;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                int slqgStateFlag = 0;
                if (Objects.equals(gController.getSlqgController().getSLQGState().getStateString(), "Aggressive"))
                    slqgStateFlag = 1;
                String [] dataToCSV = {String.valueOf(DateUtil.now()), String.valueOf(data.get(0).raw), String.valueOf(xstates[0][0]), String.valueOf(xstates[1][0]), String.valueOf(xstates[2][0]), String.valueOf(xstates[3][0]), String.valueOf(xstates[4][0]), String.valueOf(xstates[5][0]), String.valueOf(xstates[6][0]), String.valueOf(xstates[7][0]), String.valueOf(xstates[8][0]), String.valueOf(xstates[9][0]), String.valueOf(xstates[10][0]), String.valueOf(xstates[11][0]), String.valueOf(xstates[12][0]),  String.valueOf((double) gController.getSlqgController().gettMeal()), String.valueOf((double) gController.getSlqgController().getExtAgg()), String.valueOf(gController.getpCBolus()), String.valueOf(gController.getSafe().getIobMax()), String.valueOf(slqgStateFlag), String.valueOf(gController.getSafe().getIOBMaxCF()), String.valueOf((double) gController.getEstimator().getListening()), String.valueOf((double) gController.getEstimator().getMCount()), String.valueOf((double) gController.getrCFBolus()), String.valueOf((double) gController.gettEndAgg()), String.valueOf(iobStates[0][0]), String.valueOf(iobStates[1][0]), String.valueOf(iobStates[2][0]), String.valueOf(gController.getSafe().getIobEst(gController.getPatient().getWeight())), String.valueOf(gController.getSafe().getGamma()),  String.valueOf(nextRecord[0]), String.valueOf(resultado)};

                writer.writeNext(dataToCSV);
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                JSONObject argTableJSON = new JSONObject();
                try{
                    argTableJSON.put("time",String.valueOf(DateUtil.now()));
                    argTableJSON.put("CGM",String.valueOf(data.get(0).raw));
                    argTableJSON.put("IOB", String.valueOf(xstates[0][0]));
                    argTableJSON.put("Gpred", String.valueOf(xstates[1][0]));
                    argTableJSON.put("Gpred_correction", String.valueOf(xstates[2][0]));
                    argTableJSON.put("Gpred_bolus", String.valueOf(xstates[3][0]));
                    argTableJSON.put("Xi00", String.valueOf(xstates[4][0]));
                    argTableJSON.put("Xi01", String.valueOf(xstates[5][0]));
                    argTableJSON.put("Xi02", String.valueOf(xstates[6][0]));
                    argTableJSON.put("Xi03", String.valueOf(xstates[7][0]));
                    argTableJSON.put("Xi04", String.valueOf(xstates[8][0]));
                    argTableJSON.put("Xi05", String.valueOf(xstates[9][0]));
                    argTableJSON.put("Xi06", String.valueOf(xstates[10][0]));
                    argTableJSON.put("Xi07", String.valueOf(xstates[11][0]));
                    argTableJSON.put("brakes_coeff", String.valueOf(xstates[12][0]));
                    argTableJSON.put("tMeal",String.valueOf((double) gController.getSlqgController().gettMeal()));
                    argTableJSON.put("ExtAgg",String.valueOf((double) gController.getSlqgController().getExtAgg()));
                    argTableJSON.put("pCBolus" ,String.valueOf(gController.getpCBolus()));
                    argTableJSON.put("IobMax",String.valueOf(gController.getSafe().getIobMax()));
                    argTableJSON.put("slqgState",String.valueOf(slqgStateFlag));
                    argTableJSON.put("IOBMaxCF",String.valueOf(gController.getSafe().getIOBMaxCF()));
                    argTableJSON.put("Listening",String.valueOf((double) gController.getEstimator().getListening()));
                    argTableJSON.put("MCount", String.valueOf((double) gController.getEstimator().getMCount()));
                    argTableJSON.put("rCFBolus", String.valueOf((double) gController.getrCFBolus()));
                    argTableJSON.put("tEndAgg", String.valueOf((double) gController.gettEndAgg()));
                    argTableJSON.put("iobStates[0][0]", String.valueOf(iobStates[0][0]));
                    argTableJSON.put("iobStates[1][0]",String.valueOf(iobStates[1][0]));
                    argTableJSON.put("derivadaIOB", String.valueOf(iobStates[2][0]));
                    argTableJSON.put("iobEst", String.valueOf(gController.getSafe().getIobEst(gController.getPatient().getWeight())));
                    argTableJSON.put("Gamma", String.valueOf(gController.getSafe().getGamma()));
                    argTableJSON.put("Anuncio", String.valueOf(nextRecord[0]));
                    argTableJSON.put("Resultado", String.valueOf(resultado));
                }catch(JSONException e){
                    
                }

                // Este objeto sería la futura nueva fila
                ARGTable historialDeVariables = new ARGTable();

                // Se asigna el json como data y el tiempo de generacion         
                historialDeVariables = historialDeVariables.data(argTableJSON).date(now);

                // Subo a Nightscoute
                NSUpload.uploadARGTable(historialDeVariables);
                 
                // Actualizo la db local
                MainApp.getDbHelper().createARGTableIfNotExists(historialDeVariables, "ARGPlugin.invoke()");

            }
        }
                        

        // En esta sección del codigo podría llamarse a guardar todos los datos que tenga que guardar
        // de todas formas, el JSONObject DATA podría ser global a la clase y actualizarse y guardar
        // o bien podría ser local en este procedimiento y actualizarse donde tenga que actualizarse
        // con la informacion que haga falta, pero importante NO PISAR las keys

        // Este objeto sería la futura nueva fila
        // ARGTable historialDeVariables = new ARGTable();

        // Es como un diccionario
        // JSONObject argTableJSON = new JSONObject();

        // para agregar un campo a la table
        // argTableJSON.put("campo", valor);

        // Se asigna el json como data y el tiempo de generacion         
        // historialDeVariables = historialDeVariables.data(argTableJSON).date(now);

        // Subo a Nightscoute
        // NSUpload.uploadARGTable(historialDeVariables);
         
        // Actualizo la db local
        // MainApp.getDbHelper().createARGTableIfNotExists(historialDeVariables, "ARGPlugin.invoke()");


        // Para consultar ARGTables viejas desde la DB local (esta es la primera que guarda),
        // por lo que si el celular se apaga y no alcanzo a subir a internet, de todas formas,
        // los datos van a estar aca
        // List<ARGTable> argTableList = 
        //        MainApp.getDbHelper().getAllARGTableFromTime(DateUtil.now() - 2 * 1000L, false);

        // log.debug("[ARGPLUGIN] Consultando ARGTableList hace dos minutos " + String.valueOf(argTableList.size()));


        
        if (ioMain == null){
            ioMain = new IOMain();
        }
        
        ioMain.ejecutarCada5Min(gController);

        //prueba
        DetermineBasalResultARG determineBasalResultARG = determineBasalAdapterARG.invoke();
        if (L.isEnabled(L.APS))
            Profiler.log(log, "SMB calculation", start);
        // TODO still needed with oref1?
        // Fix bug determine basal

        if (determineBasalResultARG.rate == 0d && determineBasalResultARG.duration == 0 && !TreatmentsPlugin.getPlugin().isTempBasalInProgress())
            determineBasalResultARG.tempBasalRequested = false;

        determineBasalResultARG.iob = iobArray[0];

        try {
            determineBasalResultARG.json.put("timestamp", DateUtil.toISOString(now));
        } catch (JSONException e) {
            log.error("Unhandled exception", e);
        }

        determineBasalResultARG.inputConstraints = inputConstraints;

        lastDetermineBasalAdapterARG = determineBasalAdapterARG;
        lastAPSResult = determineBasalResultARG;
        lastAPSRun = now;
        MainApp.bus().post(new EventOpenAPSUpdateGui());

        //deviceStatus.suggested = determineBasalResultAMA.json;
    }

    // safety checks
    private static boolean checkOnlyHardLimits(Double value, String valueName, double lowLimit, double highLimit) {
        return value.equals(verifyHardLimits(value, valueName, lowLimit, highLimit));
    }

    private static Double verifyHardLimits(Double value, String valueName, double lowLimit, double highLimit) {
        Double newvalue = value;
        if (newvalue < lowLimit || newvalue > highLimit) {
            newvalue = Math.max(newvalue, lowLimit);
            newvalue = Math.min(newvalue, highLimit);
            String msg = String.format(MainApp.gs(R.string.valueoutofrange), valueName);
            msg += ".\n";
            msg += String.format(MainApp.gs(R.string.valuelimitedto), value, newvalue);
            log.error(msg);
            NSUpload.uploadError(msg);
            ToastUtils.showToastInUiThread(MainApp.instance().getApplicationContext(), msg, R.raw.error);
        }
        return newvalue;
    }

}


