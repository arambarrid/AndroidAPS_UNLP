package info.nightscout.androidaps.plugins.ARG;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.GlucoseStatus;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.data.MealData;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.TempTarget;
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
    private static Logger log = LoggerFactory.getLogger(L.APS);

    private static ARGPlugin argPlugin;

    public static ARGPlugin getPlugin() {
        if (argPlugin == null) {
            argPlugin = new ARGPlugin();
        }
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
                .preferencesId(R.xml.pref_arg)
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
    public void invoke(String initiator, boolean tempBasalFallback) {
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
