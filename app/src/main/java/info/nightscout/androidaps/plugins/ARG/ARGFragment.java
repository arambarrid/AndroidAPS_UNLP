package info.nightscout.androidaps.plugins.ARG;

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


public class ARGFragment extends SubscriberFragment {
    private static Logger log = LoggerFactory.getLogger(L.APS);

    @BindView(R.id.arg_run)
    Button run;
    @BindView(R.id.arg_config)
    Button config;
    @BindView(R.id.arg_lastrun)
    TextView lastRunView;
    @BindView(R.id.arg_result)
    TextView resultView;
    @BindView(R.id.arg_request)
    TextView requestView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.arg_fragment, container, false);

        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.arg_run)
    public void onRunClick() {
     //   ARGPlugin.getPlugin().invoke("ARG button", false);
        FabricPrivacy.getInstance().logCustom(new CustomEvent("ARG_Run"));
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
                    if (!isBound()) return;
                    ARGPlugin plugin = ARGPlugin.getPlugin(this.getContext());
                    ARGResult lastAPSResult = plugin.lastAPSResult;
                    if (lastAPSResult != null) {
                        resultView.setText(JSONFormatter.format(lastAPSResult.json));
                        requestView.setText(lastAPSResult.toSpanned());
                    }

                    if (plugin.lastAPSRun != 0) {
                        lastRunView.setText(DateUtil.dateAndTimeFullString(plugin.lastAPSRun));
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
