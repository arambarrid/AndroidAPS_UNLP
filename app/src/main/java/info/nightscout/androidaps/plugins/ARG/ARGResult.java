package info.nightscout.androidaps.plugins.ARG;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.Loop.APSResult;
import info.nightscout.utils.DateUtil;

public class ARGResult extends APSResult {
    private static final Logger log = LoggerFactory.getLogger(L.APS);

    ARGResult(JSONObject result) {
        this();
        date = DateUtil.now();
        json = result;
        try {
            if (result.has("error")) {
                reason = result.getString("error");
                return;
            }

            reason = result.getString("reason");

            if (result.has("rate") && result.has("duration")) {
                tempBasalRequested = true;
                rate = result.getDouble("rate");
                if (rate < 0d) rate = 0d;
                duration = result.getInt("duration");
            } else {
                rate = -1;
                duration = -1;
            }

            if (result.has("bolus")) {
                bolusRequested = true;
                smb = result.getDouble("bolus");
            } else {
                smb = 0d;
            }

            if (result.has("deliverAt")) {
                String date = result.getString("deliverAt");
                try {
                    deliverAt = DateUtil.fromISODateString(date).getTime();
                } catch (Exception e) {
                    log.warn("Error parsing 'deliverAt' date: " + date, e);
                }
            }
        } catch (JSONException e) {
            log.error("Error parsing determine-basal result JSON", e);
        }
    }

    private ARGResult() {
        hasPredictions = true;
    }

    @Override
    public JSONObject json() {
        try {
            return new JSONObject(this.json.toString());
        } catch (JSONException e) {
            log.error("Error converting determine-basal result to JSON", e);
        }
        return null;
    }
}
