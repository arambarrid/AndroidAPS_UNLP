package info.nightscout.androidaps.plugins.ARG.Dialogs;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.crashlytics.android.answers.CustomEvent;
import com.google.common.base.Joiner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.LinkedList;
import java.util.List;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.DetailedBolusInfo;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.CareportalEvent;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.db.TempTarget;
import info.nightscout.androidaps.interfaces.Constraint;
import info.nightscout.androidaps.interfaces.PumpInterface;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.androidaps.queue.Callback;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.DecimalFormatter;
import info.nightscout.utils.FabricPrivacy;
import info.nightscout.utils.NumberPicker;
import info.nightscout.utils.SP;
import info.nightscout.utils.SafeParse;
import info.nightscout.utils.T;
import info.nightscout.utils.ToastUtils;

import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.db.ARGTable;
import info.nightscout.androidaps.db.BgReading;
import info.nightscout.androidaps.plugins.NSClientInternal.NSUpload;
import info.nightscout.utils.DateUtil;

import org.json.JSONObject;
import org.json.JSONException;

import static info.nightscout.utils.DateUtil.now;

public class NewInitBolusDialog extends DialogFragment implements OnClickListener {
    private static Logger log = LoggerFactory.getLogger(NewInitBolusDialog.class);

    public static final double PLUS1_DEFAULT = 0.5d;
    public static final double PLUS2_DEFAULT = 1d;
    public static final double PLUS3_DEFAULT = 2d;

    private LinearLayout editLayout;
    private NumberPicker editTime;
    private NumberPicker editInsulin;
    private Double maxInsulin;

    private EditText notesEdit;

    //one shot guards
    private boolean accepted;
    private boolean okClicked;

    public NewInitBolusDialog() {
        HandlerThread mHandlerThread = new HandlerThread(NewInitBolusDialog.class.getSimpleName());
        mHandlerThread.start();
    }

    final private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void afterTextChanged(Editable s) {
            validateInputs();
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }
    };

    private void validateInputs() {
        int time = editTime.getValue().intValue();
        if (Math.abs(time) > 12 * 60) {
            editTime.setValue(0d);
            ToastUtils.showToastInUiThread(MainApp.instance().getApplicationContext(), MainApp.gs(R.string.constraintapllied));
        }

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.arg_initbolus_dialog, container, false);

        view.findViewById(R.id.ok).setOnClickListener(this);
        view.findViewById(R.id.cancel).setOnClickListener(this);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        editLayout = view.findViewById(R.id.newinitbolus_time_layout);
        editLayout.setVisibility(View.GONE);
        editTime = view.findViewById(R.id.newinitbolus_time);
        editTime.setParams(0d, -12 * 60d, 12 * 60d, 5d, new DecimalFormat("0"), false, textWatcher);

        maxInsulin = MainApp.getConstraintChecker().getMaxBolusAllowed().value();

        editInsulin = view.findViewById(R.id.newinitbolus_amount);
        editInsulin.setParams(0d, 0d, maxInsulin, ConfigBuilderPlugin.getPlugin().getActivePump().getPumpDescription().bolusStep, DecimalFormatter.pumpSupportedBolusFormat(), false, textWatcher);

        Button plus1Button = view.findViewById(R.id.newinitbolus_plus05);
        plus1Button.setOnClickListener(this);
        plus1Button.setText(toSignedString(SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_1), PLUS1_DEFAULT)));
        Button plus2Button = view.findViewById(R.id.newinitbolus_plus10);
        plus2Button.setOnClickListener(this);
        plus2Button.setText(toSignedString(SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_2), PLUS2_DEFAULT)));
        Button plus3Button = view.findViewById(R.id.newinitbolus_plus20);
        plus3Button.setOnClickListener(this);
        plus3Button.setText(toSignedString(SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_3), PLUS3_DEFAULT)));

        LinearLayout notesLayout = view.findViewById(R.id.newinitbolus_notes_layout);
        notesLayout.setVisibility(SP.getBoolean(R.string.key_show_notes_entry_dialogs, false) ? View.VISIBLE : View.GONE);
        notesEdit = view.findViewById(R.id.newinitbolus_notes);

        setCancelable(true);
        getDialog().setCanceledOnTouchOutside(false);
        if (savedInstanceState != null) {
//            log.debug("savedInstanceState in onCreate is:" + savedInstanceState.toString());
            editInsulin.setValue(savedInstanceState.getDouble("editInsulin"));
            editTime.setValue(savedInstanceState.getDouble("editTime"));
        }
        return view;
    }

    private String toSignedString(double value) {
        String formatted = DecimalFormatter.toPumpSupportedBolus(value);
        return value > 0 ? "+" + formatted : formatted;
    }

    @Override
    public void onSaveInstanceState(Bundle insulinDialogState) {
        //insulinDialogState.putDouble("editTime", editTime.getValue());
        //insulinDialogState.putDouble("editInsulin", editInsulin.getValue());
        //insulinDialogState.putString("notesEdit",notesEdit.getText().toString());
        //log.debug("Instance state saved:"+insulinDialogState.toString());
        //super.onSaveInstanceState(insulinDialogState);
    }

    @Override
    public synchronized void onClick(View view) {
        switch (view.getId()) {
            case R.id.ok:
                submit();
                break;
            case R.id.cancel:
                dismiss();
                break;
            case R.id.newinitbolus_plus05:
                editInsulin.setValue(Math.max(0, editInsulin.getValue()
                        + SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_1), PLUS1_DEFAULT)));
                validateInputs();
                break;
            case R.id.newinitbolus_plus10:
                editInsulin.setValue(Math.max(0, editInsulin.getValue()
                        + SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_2), PLUS2_DEFAULT)));
                validateInputs();
                break;
            case R.id.newinitbolus_plus20:
                editInsulin.setValue(Math.max(0, editInsulin.getValue()
                        + SP.getDouble(MainApp.gs(R.string.key_insulin_button_increment_3), PLUS3_DEFAULT)));
                validateInputs();
                break;
        }
    }

    private void submit() {
        if (okClicked) {
            log.debug("guarding: ok already clicked");
            dismiss();
            return;
        }

        Double insulin = SafeParse.stringToDouble(editInsulin.getText());

        if (insulin != 0){
            long now = System.currentTimeMillis();

            JSONObject insulinTable = new JSONObject();
            try{
                insulinTable.put("time", now/1000);
                insulinTable.put("type", 3); // 3 es tipo de bolo de inicializacion
                insulinTable.put("status", 2); // 2 es que fue totalmente infundida
                insulinTable.put("deliv_total", insulin); // cantidad de insulina
                insulinTable.put("deliv_time", now/1000); 
	        }catch(JSONException e){

            }

            ARGTable argTable = new ARGTable(now, "Biometrics.INSULIN_URI", insulinTable);

            MainApp.getDbHelper().createARGTableIfNotExists(argTable, "ARGInitBolusDialog()");
            NSUpload.uploadARGTable(argTable);

            dismiss();
        }


        okClicked = true;
    }
}
