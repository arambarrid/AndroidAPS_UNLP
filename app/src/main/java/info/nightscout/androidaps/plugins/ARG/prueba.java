import android.app.Activity;
import android.os.Bundle;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import info.nightscout.androidaps.R;

public class prueba extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.main);
        try {
            playWithRawFiles();
        } catch (IOException e) {
            Toast.makeText(getApplicationContext(), "Problems: " + e.getMessage(), 1).show();
        }
    }

    public void playWithRawFiles() throws IOException {
        String str = "";
        StringBuffer buf = new StringBuffer();
        InputStream is = this.getResources().openRawResource(R.raw.cmatrices);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            if (is != null) {
                while ((str = reader.readLine()) != null) {
                    buf.append(str + "\n" );
                }
            }
        } finally {
            try { is.close(); } catch (Throwable ignore) {}
        }
        Toast.makeText(getBaseContext(), buf.toString(), Toast.LENGTH_LONG).show();
    }
}