package info.nightscout.androidaps.plugins.ARG.Graphic;

import android.graphics.Color;
import android.support.annotation.Nullable;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Objects;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.Iob;
import info.nightscout.androidaps.db.DatabaseHelper;
import info.nightscout.androidaps.db.Source;
import info.nightscout.androidaps.interfaces.InsulinInterface;
import info.nightscout.androidaps.plugins.ConfigBuilder.ConfigBuilderPlugin;
import info.nightscout.androidaps.plugins.Overview.OverviewPlugin;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.DataPointWithLabelInterface;
import info.nightscout.androidaps.plugins.Overview.graphExtensions.PointsWithLabelGraphSeries;
import info.nightscout.utils.DateUtil;
import info.nightscout.utils.DecimalFormatter;
import info.nightscout.utils.JsonHelper;

public class ARGDataPoint implements DataPointWithLabelInterface {

    public long dp_x = 0;
    public double dp_y = 0;
    public String dp_label = "";
    public PointsWithLabelGraphSeries.Shape dp_shape = PointsWithLabelGraphSeries.Shape.BOLUS;
    public float dp_size = 2;
    public int dp_color = -1;


    @Override
    public double getX() {
        return dp_x;
    }

    @Override
    public double getY() {
        if ((dp_y == -1)){
            return OverviewPlugin.getPlugin().determineLowLine();
        }else if ((dp_y == -2)){
            return OverviewPlugin.getPlugin().determineLowLine() * 1.5;
        }else if ((dp_y == -3)){
            return OverviewPlugin.getPlugin().determineLowLine() * 2;
        }else{
            return dp_y;
        }
    }

    @Override
    public String getLabel() {
        return dp_label;
    }

    @Override
    public long getDuration() {
        return 0;
    }

    @Override
    public PointsWithLabelGraphSeries.Shape getShape() {
        return dp_shape;
    }

    @Override
    public float getSize() {
        return dp_size;
    }

    @Override
    public int getColor() {
        return dp_color;
    }


    @Override
    public void setY(double y) {
        dp_y = y;
    }

}
