package com.example.speechtotext;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements TextToSpeech.OnInitListener{

    private ActivityResultLauncher<Intent> sttLauncher;
    private Intent   sttIntent;
    private TextView tvStt;
    private EditText textoEscrito;
    private TextView textoConvertido;
    private ImageView   botonConvertir;

    private FloatingActionButton actionButton;

    private boolean ttsReady = false;
    private TextToSpeech tts;

    private String accion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_2);
        ImageView btStt = findViewById(R.id.btStt);
        tvStt = findViewById(R.id.tvStt);
        textoEscrito    = (EditText) findViewById(R.id.editText);
        textoConvertido = (TextView) findViewById(R.id.textView);
        botonConvertir  = (ImageView) findViewById(R.id.botonConvertir);

        tts = new TextToSpeech(this, this);
        sttLauncher = getSttLauncher();
        sttIntent = getSttIntent();

        accion = "";

        btStt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sttLauncher.launch(sttIntent);
            }
        });

        botonConvertir.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                metodoHablar();
                accion = textoEscrito.getText().toString();
                //textoEscrito.setText("");
                enviar(accion);
            }
        });
    }

    private void enviar(String texto){
        //Toast.makeText(getApplicationContext(), texto, Toast.LENGTH_SHORT).show();
    }

    private void metodoHablar() {
        String frase = textoEscrito.getText().toString();

        //textoConvertido.setText("Frase leida: " + frase);

        if(ttsReady && frase.length() > 0) {
            tts.speak(frase, TextToSpeech.QUEUE_ADD, null, null);
        }else{
            tts.speak("Esto es una prueba", TextToSpeech.QUEUE_ADD, null, null);
        }
    }

    private Intent getSttIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("spa", "ES"));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak));
        return intent;
    }

    private ActivityResultLauncher<Intent> getSttLauncher() {
        return registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    String text = "";
                    if(result.getResultCode() == Activity.RESULT_OK) {
                        List<String> r = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                        text = r.get(0);
                        //enviar(text);
                    } else if(result.getResultCode() == Activity.RESULT_CANCELED) {
                        text = getString(R.string.error);
                    }
                    showResult(text);
                }
        );
    }

    private void showResult(String result) {
        textoEscrito.setText(result);
    }

    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS){
            ttsReady = true;
            tts.setLanguage(new Locale("spa", "ES"));
        }

    }
}