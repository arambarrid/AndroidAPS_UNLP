package info.nightscout.androidaps.plugins.ARG;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJSON;
import org.mozilla.javascript.NativeObject;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;

import info.nightscout.androidaps.Constants;
import info.nightscout.androidaps.MainApp;
import info.nightscout.androidaps.R;
import info.nightscout.androidaps.data.GlucoseStatus;
import info.nightscout.androidaps.data.IobTotal;
import info.nightscout.androidaps.data.MealData;
import info.nightscout.androidaps.data.Profile;
import info.nightscout.androidaps.db.TemporaryBasal;
import info.nightscout.androidaps.logging.L;
import info.nightscout.androidaps.plugins.IobCobCalculator.IobCobCalculatorPlugin;
import info.nightscout.androidaps.plugins.Loop.ScriptReader;
import info.nightscout.androidaps.plugins.OpenAPSMA.LoggerCallback;
import info.nightscout.androidaps.plugins.Treatments.TreatmentsPlugin;
import info.nightscout.utils.SP;
import info.nightscout.utils.SafeParse;
import info.nightscout.utils.DateUtil;
import info.nightscout.androidaps.db.BgReading;

public class PDBasal {
    private static Logger log = LoggerFactory.getLogger(L.APS);

    List<BgReading> cgmList;

    private double tD;
    private double kP;
    private double CGMref;
    private double basal0;
    private double basal;
    private String reason;

    PDBasal() {
      basal0 = 1;
      reason = "Inicializado";

      log.debug("[PDBASAL] creacion de instancia");
    }

    public void run() {
          log.debug("[PDBASAL] llamada a run");

      double res, e, eAnt, der;
      double deltaT = 5;

      // Respuesta por defecto
      basal = basal0;
      if (cgmList.size() < 2){
        reason = "No hay suficientes datos";
        return;
      }

      BgReading lastCGM = cgmList.get(1), CGM = cgmList.get(0);

      long bgReadingAgo = System.currentTimeMillis() - CGM.date;
      int deltaTRealMin = (int) ((CGM.date - lastCGM.date)/(1000*60));
      int bgReadingAgoMin = (int) (bgReadingAgo / (1000 * 60));

      if (bgReadingAgoMin > 15){
        reason = "Tiempo de ultima lectura hace mas de 15 minutos.";
        return;
      }

      deltaTRealMin = 5;

      // Minimo deltaT de lectura
      if (deltaTRealMin < 1)
        deltaTRealMin = 1;

      eAnt = lastCGM.value - CGMref;
      e = CGM.value - CGMref;
      der = (e - eAnt) / deltaTRealMin;

      reason = "";
      reason += "lastCGM =" + String.valueOf(lastCGM.value) + "\n";
      reason += "lastCGM date =" + String.valueOf(lastCGM.date) + "\n";
      reason += "CGM =" + String.valueOf(CGM.value) + "\n";
      reason += "CGM date =" + String.valueOf(CGM.date) + "\n";
      reason += "CGMref =" + String.valueOf(CGMref) + "\n";
      reason += "deltaTRealMin =" + String.valueOf(deltaTRealMin) + "\n";
      reason += "kP =" + String.valueOf(kP) + "\n";
      reason += "tD =" + String.valueOf(tD) + "\n";
      reason += "basal0 =" + String.valueOf(basal0) + "\n";
      reason += "e(i) =" + String.valueOf(e) + "\n";
      reason += "e(i-1) =" + String.valueOf(eAnt) + "\n";
      reason += "de/dt =" + String.valueOf(der) + "\n";

      log.debug("[PDBASAL]   -> lastCGM " + String.valueOf(lastCGM.value));
      log.debug("[PDBASAL]   -> lastCGM date " + String.valueOf(lastCGM.date));
      log.debug("[PDBASAL]   -> CGM " + String.valueOf(CGM.value));
      log.debug("[PDBASAL]   -> CGM date " + String.valueOf(CGM.date));
      log.debug("[PDBASAL]   -> CGMref " + String.valueOf(CGMref));
      log.debug("[PDBASAL]   -> deltaTRealMin " + String.valueOf(deltaT));
      log.debug("[PDBASAL]   -> kP " + String.valueOf(kP));
      log.debug("[PDBASAL]   -> tD " + String.valueOf(tD));
      log.debug("[PDBASAL]   -> bastal0 " + String.valueOf(basal0));
      log.debug("[PDBASAL]   -> e(i) " + String.valueOf(e));
      log.debug("[PDBASAL]   -> e(i-1) " + String.valueOf(eAnt));
      log.debug("[PDBASAL]   -> de(t)/dt " + String.valueOf(der));

      basal = (kP * (e + tD * (der)) * 0.01) + basal0;
      if (basal < 0)
        basal = 0;

      reason += " resultado =" + String.valueOf(basal) + "\n";


      log.debug("[PDBASAL]   RESULTADO " + String.valueOf(basal));

      log.debug("[PDBASAL] fin llamada a run");
    }

    public double getTempBasal(){
      log.debug("[PDBASAL] solicitud de resultados");

      // Esto no debería pasar, pero....
      if (basal0 == 0)
        return 0;
      else
        return basal;
    }

    public String getReason(){
      return reason;
    }

    public void setData(Profile profile, List<BgReading> data,
                    double _kP, double _tD, double _CGMref, double _basal0){
      cgmList = data;

      log.debug("[PDBASAL] seteo de data");

      cgmList = data;

      tD = _tD;
      kP = _kP;
      CGMref = _CGMref;
      basal0 = _basal0;

      reason = "Datos recibidos, sin invocacion del algoritmo";
      log.debug("[PDBASAL] fin seteo de data");
    }

}
