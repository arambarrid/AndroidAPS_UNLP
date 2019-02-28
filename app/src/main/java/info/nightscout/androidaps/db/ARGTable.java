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

import  java.util.UUID;


@DatabaseTable(tableName = DatabaseHelper.DATABASE_ARGTABLE)
public class ARGTable {
    private static Logger log = LoggerFactory.getLogger(L.DATABASE);

    @DatabaseField(id = true)
    public String uuid;

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

    @DatabaseField
    public String _id = null; // NS _id

    private JSONObject jsonData;

    public ARGTable(){
        
    }

    public ARGTable(long date, String diastype, JSONObject data) {
        this.jsonData = data;
        this.data = data.toString();
        this.diastype = diastype;
        this.date = date;

        UUID id = UUID.randomUUID();
        uuid = id.toString();
    }

    public void setData(JSONObject data){
        this.data = data.toString();
    }

    @Override
    public String toString() {
        return "ARGTable{" +
                "date=" + date +
                ",diastype=" + diastype +
                ", date=" + new Date(date).toLocaleString() +
                ", Data=" + data + 
                ", uuid=" + uuid + 
                ", _id=" + _id +
                '}';
    }

    public JSONObject getAllData(){
        return jsonData;
    }

    private void checkUpdateStringDataToJsonData(){
        if (jsonData == null && data.length() > 0){
            try{
                jsonData = new JSONObject(data);
            }catch(JSONException e){
                log.error("[ARGPLUGIN] ARGTable Error al convertir string en json cuando jsonData == null."
                    + e.toString());
            }
        }
    }

    public Object getByColumn(String column){
        Object ret = null;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.get(column);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getByColumn() " + column
                    + e.toString());
        }
        
        return ret;
    }

    public boolean getBoolean(String name){
        boolean ret = false;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.getBoolean(name);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getBoolean() " + name
                    + e.toString());

        }
        
        return ret;
    }

    public double  getDouble(String name){
        double ret = 0;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.getDouble(name);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getDouble() " + name
                    + e.toString());

        }
        
        return ret;
    }

    public int getInt(String name){
        int ret = 0;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.getInt(name);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getInt() " + name
                    + e.toString());

        }
        
        return ret;
    }


    public long    getLong(String name){
        long ret = 0;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.getLong(name);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getLong() " + name
                    + e.toString());

        }
        
        return ret;
    }

    public String  getString(String name){
        String ret = null;
        checkUpdateStringDataToJsonData();
        try{
            if (jsonData != null)
                ret = this.jsonData.getString(name);
        }catch (JSONException e){
                log.error("[ARGPLUGIN] ARGTable - getString() " + name
                    + e.toString());

        }
        
        return ret;
    }



}
