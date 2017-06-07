package de.hsaugsburg.ampelpilot;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private EditText inputScale;
    private EditText inputMinN;
    private EditText inputFlags;
    private SeekBar seekBar;

    private EditText inputScaleRED;
    private EditText inputMinNRED;
    private EditText inputFlagsRED;
    private TextView seekBarValue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        inputMinN = (EditText) findViewById(R.id.MinN);
        inputScale = (EditText) findViewById(R.id.Scale);

        inputMinNRED = (EditText) findViewById(R.id.MinNRed);
        inputScaleRED = (EditText) findViewById(R.id.ScaleRed);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar.setProgress(0);
        seekBar.setMax(100);
        seekBarValue = (TextView) findViewById(R.id.seekbarValue);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                seekBarValue.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        Button btnGo = (Button) findViewById(R.id.btnGo);
        btnGo.setOnClickListener(new View.OnClickListener(){
            public void onClick(View arg0){
                Intent nextScreen = new Intent(getApplicationContext(), LdActivity.class);
                nextScreen.putExtra("ScaleFactor",Double.parseDouble(inputScale.getText().toString()));
                nextScreen.putExtra("MinNeighbours",Integer.parseInt(inputMinN.getText().toString()));

                nextScreen.putExtra("ScaleFactorRED",Double.parseDouble(inputScaleRED.getText().toString()));
                nextScreen.putExtra("MinNeighboursRED",Integer.parseInt(inputMinNRED.getText().toString()));

                nextScreen.putExtra("ZoomFactor",seekBar.getProgress());
                startActivity(nextScreen);
            }
     });
    }

    private float roundFloat(final float number, final int decimalPlaces) {
        float precision = 1.0F;
        for (int i = 0; i < decimalPlaces; i++, precision *= 10);
    return ((int) (number * precision + 0.5)/ precision);
    }
}
