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
import info.nightscout.utils.DecimalFormatter;
import info.nightscout.utils.SP;

public class ARGPlugin extends PluginBase implements APSInterface {
    // Clases de control
    public GController gController;
    public IOMain ioMain;

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

    long lastAPSRun = 0;
    ARGResult lastAPSResult = null;

    private ARGPlugin() {
        super(new PluginDescription()
                .mainType(PluginType.APS)
                .fragmentClass(ARGFragment.class.getName())
                .pluginName(R.string.openapsarg)
                .shortName(R.string.arg_shortname)
                .preferencesId(R.xml.pref_apsarg)
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

        if (gController == null) {
            double parameterCF = profile.getIsf();
            double parameterCR = profile.getIc();
            double parameterUBasal = profile.getBasal();
            double parameterTDI = SP.getDouble(R.string.key_apsarg_tdi, 4d);
            double parameterWeight = SP.getDouble(R.string.key_apsarg_weight, 4d);
            double parameterSetpoint = SP.getDouble(R.string.key_apsarg_setpoint, 4d);
            
            gController = new GController(parameterSetpoint, 
                                    parameterTDI, 
                                    parameterCR, 
                                    parameterCF, 
                                    parameterWeight, 
                                    parameterUBasal, 
                                    miContexto);   
        }

        if (ioMain == null){
            ioMain = new IOMain();
        }

        // Resultado en unidades
        double bolusResult = ioMain.ejecutarCada5Min(gController) / 1200.0;

        // Caso ideal Si pasan 5 minutos y se ejecuta con una nueva muestra
        // Caso probable Se ejecute mas seguido
        JSONObject jsonResult = new JSONObject();
        try{
            jsonResult.put("reason", "Caso de prueba");

            // Siempre basal a 0%
            jsonResult.put("rate", 0);
            jsonResult.put("duration", 30);
            
            // Asegurarse de dar el bolo
            jsonResult.put("bolus", bolusResult);
        } catch(JSONException e){

        }

        ARGResult argResult = new ARGResult(jsonResult);

        // Estimación de IOB? 
        // argResult.iob = iobArray[0];
        argResult.iob = new IobTotal(0);

        // Es necesario determinar esto ? 
        // determineBasalResultARG.inputConstraints = inputConstraints;

        lastAPSResult = argResult;

        // TODO_APS: no se usa igual
        // lastAPSRun = now;   

        MainApp.bus().post(new EventOpenAPSUpdateGui());
    }

}


