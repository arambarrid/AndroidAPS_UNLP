package info.nightscout.androidaps.plugins.ARG;

import android.content.Intent;
import info.nightscout.androidaps.activities.PreferencesActivity;
import java.io.IOException;

import info.nightscout.utils.SP;
import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.crashlytics.android.answers.CustomEvent;
import com.squareup.otto.Subscribe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.Common.SubscriberFragment;
import info.nightscout.androidaps.plugins.OpenAPSMA.events.EventOpenAPSUpdateGui;
import info.nightscout.androidaps.plugins.OpenAPSMA.events.EventOpenAPSUpdateResultGui;
import info.nightscout.androidaps.plugins.OpenAPSSMB.OpenAPSSMBPlugin;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.FabricPrivacy;
import info.nightscout.utils.JSONFormatter;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.ARGTable;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.plugins.NSClientInternal.NSUpload;
import info.nightscout.utils.DateUtil;

import org.json.JSONObject;
import org.json.JSONException;

import android.support.v4.app.FragmentManager;
import info.nightscout.androidaps.plugins.ARG.Dialogs.NewInitBolusDialog;
import info.nightscout.androidaps.plugins.PumpCombo.ComboPlugin;

public class ARGFragment extends SubscriberFragment {
    private static Logger log = LoggerFactory.getLogger(L.APS);
    public static FragmentManager lastFragmentManager = null;

    @BindView(R.id.arg_run)
    Button run;
    @BindView(R.id.arg_config)
    Button config;
    @BindView(R.id.arg_reset)
    Button reset;
    @BindView(R.id.arg_force_controller)
    Button forceController;
    @BindView(R.id.arg_init_bolus)
    Button initBolus;
    @BindView(R.id.arg_reset_ruffy)
    Button resetRuffy;
    @BindView(R.id.arg_lastrun)
    TextView lastRunView;
    @BindView(R.id.arg_result)
    TextView resultView;
    @BindView(R.id.arg_request)
    TextView requestView;
    @BindView(R.id.arg_cf)
    TextView cfView;
    @BindView(R.id.arg_cr)
    TextView crView;
    @BindView(R.id.arg_setpoint)
    TextView setPointView;
    @BindView(R.id.arg_tdi)
    TextView tdiView;
    @BindView(R.id.arg_weight)
    TextView weightView;
    @BindView(R.id.arg_basal)
    TextView basalView;
    @BindView(R.id.arg_iob_factor)
    TextView iobFactorView;
    @BindView(R.id.arg_bac_conserv)
    TextView bacConservView;
    @BindView(R.id.arg_bac_food)
    TextView bacFoodView;
    @BindView(R.id.arg_variables_controller_last_modif)
    TextView ultModifControllerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.arg_fragment, container, false);

        lastFragmentManager = getFragmentManager();
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.arg_run)
    public void onRunClick() {
        try {
            ARGPlugin.getPlugin(this.getContext()).invoke("ARG button", false);
        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (L.isEnabled(L.APS))
                log.debug("invoke end");
        }

        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Run"));
    }

    @OnClick(R.id.arg_config)
    public void onConfigClick() {
        Intent i = new Intent(getContext(), PreferencesActivity.class);
        i.putExtra("id", ARGPlugin.getPlugin(this.getContext()).getPreferencesId());
        startActivity(i);

        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Config"));
    }

    @OnClick(R.id.arg_reset)
    public void onResetClick() {

        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Reset"));
    }


    public static void showInitBolus(){
        new NewInitBolusDialog().show(lastFragmentManager, "InitBolusDialog");
        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Init_Bolus"));
    }

    @OnClick(R.id.arg_init_bolus)
    public void onInitBolusClick() {
        FragmentManager manager = getFragmentManager();
        new NewInitBolusDialog().show(manager, "InitBolusDialog");
        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Init_Bolus"));
    }

    @OnClick(R.id.arg_reset_ruffy)
    public void onResetRuffyClick() {
        // Corroborar que sea la combo;
        ComboPlugin.getPlugin().killRuffyAndRestart();
        ComboPlugin.getPlugin().resetPlugin();

    }

    @OnClick(R.id.arg_force_controller)
    public void onForceControllerClick() {

        long now = System.currentTimeMillis();

        JSONObject mealTable = new JSONObject();
        try{
            mealTable.put("time", now/1000);
            mealTable.put("mealClass", 0);
            mealTable.put("lastTime", 0);
            mealTable.put("forCon", 1);
            mealTable.put("endAggIni", 0);
        }catch(JSONException e){

        }

        ARGTable argTable = new ARGTable(now, "ARG_MEAL", mealTable);

        MainApp.getDbHelper().createARGTableIfNotExists(argTable, "onForceControllerClick()");
        NSUpload.uploadARGTable(argTable);


        ultModifControllerView.setText("Controlador forzado a modo conservador, esperar actualizacion.");

        FabricPrivacy.getInstance().logCustom(new CustomEvent("arg_force_controller"));
    }

    @Subscribe
    public void onStatusEvent(final EventOpenAPSUpdateGui ev) {
        updateGUI();
    }

    @Subscribe
    public void onStatusEvent(final EventOpenAPSUpdateResultGui ev) {
        updateResultGUI(ev.text);
    }

    @Override
    protected void updateGUI() {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> {
                synchronized (ARGFragment.this) {

                    log.debug("[ARGPLUGIN] Update GUI Fragment");

                    if (!isBound()) return;
                    ARGPlugin plugin = ARGPlugin.getPlugin(this.getContext());
                    ARGResult lastAPSResult = plugin.lastAPSResult;
                    GController gController = plugin.gController;

                    if (lastAPSResult != null) {
                        resultView.setText(JSONFormatter.format(lastAPSResult.json));
                        requestView.setText(lastAPSResult.toSpanned());
                    }

                    if (plugin.lastAPSRun != 0) {
                        lastRunView.setText(DateUtil.dateAndTimeFullString(plugin.lastAPSRun));
                    }
                    
                    if (gController == null){
                        ultModifControllerView.setText("Controlador Null");
                        cfView.setText("");
                        crView.setText("");
                        setPointView.setText("");
                        tdiView.setText("");
                        weightView.setText("");
                        basalView.setText("");
                        iobFactorView.setText("");
                        bacConservView.setText("No hay informacion");
                        bacFoodView.setText("No hay informacion");
                    }else{
                        ultModifControllerView.setText("");
                        cfView.setText(String.valueOf(gController.getPatient().getCf()));
                        crView.setText(String.valueOf(gController.getPatient().getCr()));
                        setPointView.setText(String.valueOf(gController.getSetpoint()));
                        tdiView.setText(String.valueOf(gController.getPatient().getTdi()));
                        weightView.setText(String.valueOf(gController.getPatient().getWeight()));
                        basalView.setText(String.valueOf(gController.getPatient().getBasalU()));
                        iobFactorView.setText(String.valueOf( SP.getDouble(R.string.key_apsarg_iobfactor, 4d)));
                        bacConservView.setText("No hay informacion");
                        bacFoodView.setText("No hay informacion");
                    }
                }
            });
    }

    void updateResultGUI(final String text) {
        Activity activity = getActivity();
        if (activity != null)
            activity.runOnUiThread(() -> {
                synchronized (ARGFragment.this) {
                    if (isBound()) {
                        resultView.setText(text);
                        requestView.setText("");
                        lastRunView.setText("");
                    }
                }
            });
    }

    private boolean isBound() {
        return run != null
                && lastRunView != null
                && resultView != null
                && requestView != null;
    }
}
