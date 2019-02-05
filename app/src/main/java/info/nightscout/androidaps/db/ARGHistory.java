package info.nightscout.androidaps.db;

import android.content.res.Resources;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

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

@DatabaseTable(tableName = DatabaseHelper.DATABASE_ARGHISTORY)
public class ARGHistory {
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

    public ARGHistory() {
        data = new String("");
    }

    @Override
    public String toString() {
        return "ARGHistory{" +
                "date=" + date +
                ", date=" + new Date(date).toLocaleString() +
                ", Data=" + data +
                '}';
    }

    public boolean isDataChanging(ARGHistory other) {
        if (date != other.date) {
            log.error("Comparing different");
            return false;
        }
        return false;
    }

    public boolean isEqual(ARGHistory other) {
        if (date != other.date) {
            log.error("Comparing different");
            return false;
        }
        return true;
    }

    public void copyFrom(ARGHistory other) {
        if (date != other.date) {
            log.error("Copying different");
            return;
        }
        data = other.data;
        _id = other._id;
    }

    public ARGHistory date(long date) {
        this.date = date;
        return this;
    }

    public ARGHistory date(Date date) {
        this.date = date.getTime();
        return this;
    }

    public ARGHistory data(String data) {
        this.data = data;
        return this;
    }

}
