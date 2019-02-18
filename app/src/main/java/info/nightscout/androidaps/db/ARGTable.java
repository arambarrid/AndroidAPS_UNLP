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

    @DatabaseField
    public long date;

    // La idea de este campo es simular las tablas del DiAS
    // esto podría unificarse PEERO ahora respetamos tal cual
    // como estaba hecho (para agilizar). Tipos posibles:
    // Biometrics.USER_TABLE_1_URI
    // Biometrics.USER_TABLE_3_URI
    // Biometrics.USER_TABLE_4_URI
    // Biometrics.INSULIN_URI
    // Biometrics.CGM_URI
    // Biometrics.HMS_STATE_ESTIMATE_URI
    // Biometrics.TEMP_BASAL_URI

    @DatabaseField 
    public String diastype;
    
    @DatabaseField
    public String data;

    private JSONObject jsonData;

    public ARGTable(){
        
    }

    public ARGTable(long date, String diastype, JSONObject data) {
        this.jsonData = data;
        this.data = data.toString();
        this.diastype = diastype;
        this.date = date;
    }

    @Override
    public String toString() {
        return "ARGTable{" +
                "date=" + date +
                ",diastype=" + diastype +
                ", date=" + new Date(date).toLocaleString() +
                ", Data=" + data +
                '}';
    }

    public JSONObject getAllData(){
        return jsonData;
    }

    public Object getByColumn(String column){
        Object ret = null;
        try{
            ret = this.jsonData.get(column);
        }catch (JSONException e){

        }
        
        return ret;
    }

    public boolean getBoolean(String name){
        boolean ret = false;
        try{
            ret = this.jsonData.getBoolean(name);
        }catch (JSONException e){

        }
        
        return ret;
    }

    public double  getDouble(String name){
        double ret = 0;
        try{
            ret = this.jsonData.getDouble(name);
        }catch (JSONException e){

        }
        
        return ret;
    }

    public int getInt(String name){
        int ret = 0;
        try{
            ret = this.jsonData.getInt(name);
        }catch (JSONException e){

        }
        
        return ret;
    }


    public long    getLong(String name){
        long ret = 0;
        try{
            ret = this.jsonData.getLong(name);
        }catch (JSONException e){

        }
        
        return ret;
    }

    public String  getString(String name){
        String ret = null;
        try{
            ret = this.jsonData.getString(name);
        }catch (JSONException e){

        }
        
        return ret;
    }



}
