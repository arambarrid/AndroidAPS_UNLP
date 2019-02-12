package info.nightscout.androidaps.db;

import android.content.res.Resources;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

import org.json.JSONObject;
import org.json.JSONException;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.ConfigBuilder.ProfileFunctions;
import info.nightscout.androidaps.plugins.NSClientInternal.data.NSSgv;
import info.nightscout.androidaps.plugins.Overview.OverviewPlugin;
import info.nightscout.utils.DecimalFormatter;
import info.nightscout.utils.SP;

@DatabaseTable(tableName = DatabaseHelper.DATABASE_ARGTABLE)
public class ARGTable {
    private static Logger log = LoggerFactory.getLogger(L.DATABASE);

    @DatabaseField(id = true)
    public long date;

    @DatabaseField
    public boolean isValid = true;

    @DatabaseField
    public String data;

    @DatabaseField
    public int source = Source.NONE;
    @DatabaseField
    public String _id = null; // NS _id

    public ARGTable() {
        data = new String("");
    }

    @Override
    public String toString() {
        return "ARGTable{" +
                "date=" + date +
                ", date=" + new Date(date).toLocaleString() +
                ", Data=" + data +
                '}';
    }

    public JSONObject getData(){
        JSONObject ret = new JSONObject();
        try{
            ret = new JSONObject(data);
        }catch (JSONException e){

        }
        
        return ret;
    }

    public boolean isDataChanging(ARGTable other) {
        if (date != other.date) {
            log.error("Comparing different");
            return false;
        }
        return false;
    }

    public boolean isEqual(ARGTable other) {
        if (date != other.date) {
            log.error("Comparing different");
            return false;
        }
        return true;
    }

    public void copyFrom(ARGTable other) {
        if (date != other.date) {
            log.error("Copying different");
            return;
        }
        data = other.data;
        _id = other._id;
    }

    public ARGTable date(long date) {
        this.date = date;
        return this;
    }

    public ARGTable date(Date date) {
        this.date = date.getTime();
        return this;
    }

    public ARGTable data(String data) {
        this.data = data;
        return this;
    }
    public ARGTable data(JSONObject data) {
        this.data = data.toString();
        return this;
    }

}
