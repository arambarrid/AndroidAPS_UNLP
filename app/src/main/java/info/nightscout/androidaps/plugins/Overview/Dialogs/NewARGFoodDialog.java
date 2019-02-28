package info.nightscout.androidaps.plugins.Overview.Dialogs;

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
import android.widget.RadioButton;
import android.widget.CompoundButton;

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

import org.json.JSONObject;
import org.json.JSONException;

import static info.nightscout.utils.DateUtil.now;

public class NewARGFoodDialog extends DialogFragment implements OnClickListener, CompoundButton.OnCheckedChangeListener  {
    private static Logger log = LoggerFactory.getLogger(NewARGFoodDialog.class);

    private RadioButton foodSmall;
    private RadioButton foodMedium;
    private RadioButton foodLarge;

    //one shot guards
    private boolean accepted;
    private boolean okClicked;

    public NewARGFoodDialog() {
        HandlerThread mHandlerThread = new HandlerThread(NewARGFoodDialog.class.getSimpleName());
        mHandlerThread.start();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.overview_argfood_dialog, container, false);

        view.findViewById(R.id.ok).setOnClickListener(this);
        view.findViewById(R.id.cancel).setOnClickListener(this);


        foodSmall = view.findViewById(R.id.argfood_small);
        foodSmall.setOnCheckedChangeListener(this);
        foodMedium = view.findViewById(R.id.argfood_medium);
        foodMedium.setOnCheckedChangeListener(this);
        foodLarge = view.findViewById(R.id.argfood_large);
        foodLarge.setOnCheckedChangeListener(this);

        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setCancelable(true);
        getDialog().setCanceledOnTouchOutside(false);

        return view;
    }

    @Override
    public void onSaveInstanceState(Bundle argFoodDialogState) {
        argFoodDialogState.putBoolean("foodSmall",foodSmall.isChecked());
        argFoodDialogState.putBoolean("foodMedium", foodMedium.isChecked());
        argFoodDialogState.putBoolean("foodLarge", foodLarge.isChecked());

        super.onSaveInstanceState(argFoodDialogState);
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
        }
    }


    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // Logic to disable a selected radio when pressed: when a checked radio
        // is pressed, no CheckChanged event is triggered, so register a Click event
        // when checking a radio. Since Click events come after CheckChanged events,
        // the Click event is triggered immediately after this. Thus, set togglingTT
        // var to true, so that the first Click event fired after this is ignored.
        // Radios remove themselves from Click events once unchecked.
        // Since radios are not in a group,  their state is manually updated here.
        switch (buttonView.getId()) {
            case R.id.argfood_small:
                foodSmall.setOnClickListener(this);

                foodMedium.setOnCheckedChangeListener(null);
                foodMedium.setChecked(false);
                foodMedium.setOnCheckedChangeListener(this);

                foodLarge.setOnCheckedChangeListener(null);
                foodLarge.setChecked(false);
                foodLarge.setOnCheckedChangeListener(this);
                break;
            case R.id.argfood_medium:
                foodMedium.setOnClickListener(this);

                foodSmall.setOnCheckedChangeListener(null);
                foodSmall.setChecked(false);
                foodSmall.setOnCheckedChangeListener(this);

                foodLarge.setOnCheckedChangeListener(null);
                foodLarge.setChecked(false);
                foodLarge.setOnCheckedChangeListener(this);
                break;
            case R.id.argfood_large:
                foodLarge.setOnClickListener(this);

                foodSmall.setOnCheckedChangeListener(null);
                foodSmall.setChecked(false);
                foodSmall.setOnCheckedChangeListener(this);

                foodMedium.setOnCheckedChangeListener(null);
                foodMedium.setChecked(false);
                foodMedium.setOnCheckedChangeListener(this);
                break;
        }
    }




    private void submit() {
        if (okClicked) {
            log.debug("guarding: ok already clicked");
            dismiss();
            return;
        }

        int mealClass = 0;

        if (foodSmall.isChecked()) {
            mealClass = 1;
        }else if (foodMedium.isChecked()){
            mealClass = 2;
        }else if (foodLarge.isChecked()){
            mealClass = 3;
        }

        if (mealClass != 0){
            long now = System.currentTimeMillis();

            JSONObject mealTable = new JSONObject();
            try{
                mealTable.put("time", now/1000);
                mealTable.put("mealClass", mealClass);
                mealTable.put("lastTime", 0);
                mealTable.put("forCon", 0);
                mealTable.put("endAggIni", 0);
            }catch(JSONException e){

            }

            ARGTable argTable = new ARGTable(now, "ARG_MEAL", mealTable);

            MainApp.getDbHelper().createARGTableIfNotExists(argTable, "ARGFoodDialog()");
            NSUpload.uploadARGTable(argTable);

            dismiss();
        }

        okClicked = true;

    }
}
