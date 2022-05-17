package com.example.speechtotext;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.speechtotext.adapters.ChatAdapter;
import com.example.speechtotext.helpers.SendMessageInBg;
import com.example.speechtotext.interfaces.BotReply;
import com.example.speechtotext.models.Message;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.dialogflow.v2.DetectIntentResponse;
import com.google.cloud.dialogflow.v2.QueryInput;
import com.google.cloud.dialogflow.v2.SessionName;
import com.google.cloud.dialogflow.v2.SessionsClient;
import com.google.cloud.dialogflow.v2.SessionsSettings;
import com.google.cloud.dialogflow.v2.TextInput;
import com.google.common.collect.Lists;
import com.google.protobuf.Value;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class MainActivity2 extends AppCompatActivity implements BotReply, TextToSpeech.OnInitListener {

    private ActivityResultLauncher<Intent> sttLauncher;
    private Intent   sttIntent;

    RecyclerView chatView;
    ChatAdapter chatAdapter;
    List<Message> messageList = new ArrayList<>();
    EditText editMessage;
    ImageButton btnSend, btnMic;

    private boolean ttsReady = false;
    private TextToSpeech tts;

    //dialogFlow
    private SessionsClient sessionsClient;
    private SessionName sessionName;
    private String uuid = UUID.randomUUID().toString();
    private String TAG = "mainactivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_3);

        onCall();

        chatView = findViewById(R.id.chatView);
        editMessage = findViewById(R.id.editMessage);
        btnSend = findViewById(R.id.btnSend);
        btnMic = findViewById(R.id.mic);
        tts = new TextToSpeech(this, this);

        sttLauncher = getSttLauncher();
        sttIntent = getSttIntent();

        chatAdapter = new ChatAdapter(messageList, this);
        chatView.setAdapter(chatAdapter);

        btnMic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sttLauncher.launch(sttIntent);
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) {
                //metodoHablar();
                String message = editMessage.getText().toString();
                if (!message.isEmpty()) {
                    messageList.add(new Message(message, false));
                    editMessage.setText("");
                    sendMessageToBot(message);
                    Objects.requireNonNull(chatView.getAdapter()).notifyDataSetChanged();
                    Objects.requireNonNull(chatView.getLayoutManager())
                            .scrollToPosition(messageList.size() - 1);
                } else {
                    Toast.makeText(getApplicationContext(), "Please enter text!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        setUpBot();
    }

    private void metodoHablar() {
        String frase = editMessage.getText().toString();

        if(ttsReady && frase.length() > 0) {
            tts.speak(frase, TextToSpeech.QUEUE_ADD, null, null);
        }else{
            tts.speak("Esto es una prueba", TextToSpeech.QUEUE_ADD, null, null);
        }
    }


    private void setUpBot() {
        try {
            InputStream stream = this.getResources().openRawResource(R.raw.credential);
            GoogleCredentials credentials = GoogleCredentials.fromStream(stream)
                    .createScoped(Lists.newArrayList("https://www.googleapis.com/auth/cloud-platform"));
            String projectId = ((ServiceAccountCredentials) credentials).getProjectId();

            SessionsSettings.Builder settingsBuilder = SessionsSettings.newBuilder();
            SessionsSettings sessionsSettings = settingsBuilder.setCredentialsProvider(
                    FixedCredentialsProvider.create(credentials)).build();
            sessionsClient = SessionsClient.create(sessionsSettings);
            sessionName = SessionName.of(projectId, uuid);

            Log.d(TAG, "projectId : " + projectId);
        } catch (Exception e) {
            Log.d(TAG, "setUpBot: " + e.getMessage());
        }
    }

    private void sendMessageToBot(String message) {
        QueryInput input = QueryInput.newBuilder()
                .setText(TextInput.newBuilder().setText(message).setLanguageCode("en-US")).build();
        new SendMessageInBg(this, sessionName, sessionsClient, input).execute();
    }



    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void callback(DetectIntentResponse returnResponse) {
        if(returnResponse!=null) {

            String intent = returnResponse.getQueryResult().getIntent().getDisplayName();
            String botReply = returnResponse.getQueryResult().getFulfillmentText();

            if(ttsReady && botReply.length() > 0) {
                tts.speak(botReply, TextToSpeech.QUEUE_ADD, null, null);
            }else{
                tts.speak("Esto es una prueba", TextToSpeech.QUEUE_ADD, null, null);
            }

            if (intent.equals("pedir_dia")){
                Map<String, Value> params = returnResponse.getQueryResult().getParameters().getFieldsMap();
                Value diaResponse = params.get("dia");
                String dia = String.valueOf(diaResponse.getStringValue());
                Value personaResponse = params.get("persona");
                String persona = String.valueOf(personaResponse.getStringValue());
                Value horaResponse = params.get("hora");
                String hora = String.valueOf(horaResponse.getStringValue());

                if (!dia.equals("") && !persona.equals("") && !hora.equals("")){
                    saveInCalendar(dia, persona, hora);
                }

            }

            if (intent.equals("llamar")){
                Map<String, Value> params = returnResponse.getQueryResult().getParameters().getFieldsMap();
                Value numberResponse = params.get("phone-number");
                String number = String.valueOf(numberResponse.getStringValue());
                if (!number.equals("")){
                    callPhoneNumber(number);
                }
            }

            if (intent.equals("wikipedia")){
                Map<String, Value> params = returnResponse.getQueryResult().getParameters().getFieldsMap();
                Value wikiResponse = params.get("any");
                String wiki = String.valueOf(wikiResponse.getStringValue());

                if (!wiki.equals("")){
                    buscarWikipedia(wiki);
                }
            }

            if(!botReply.isEmpty()){
                messageList.add(new Message(botReply, true));
                chatAdapter.notifyDataSetChanged();
                Objects.requireNonNull(chatView.getLayoutManager()).scrollToPosition(messageList.size() - 1);
            }else {
                Toast.makeText(this, "something went wrong", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "failed to connect!", Toast.LENGTH_SHORT).show();
        }
    }

    private void buscarWikipedia(String direccion) {
        String url = "https://es.wikipedia.org/wiki/" + direccion;
        System.out.println("-----------------------------------------------------");
        System.out.println(url);
        Uri webpage = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW, webpage);
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
    }

    private void callPhoneNumber(String number) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + number));
        if (intent.resolveActivity(getPackageManager()) != null){
            startActivity(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void saveInCalendar(String date, String name, String time) {
        String fecha = date.substring(0,10);
        String hora = time.substring(10);
        String fechaFinal = fecha + hora;

        DateTimeFormatter isoDateFormatter = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime ldate = LocalDateTime.parse(fechaFinal, isoDateFormatter);
        Date rDate = Date.from(ldate.atZone(ZoneId.of("UTC+2")).toInstant());
        long begin = rDate.getTime();

        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, name)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    @Override
    public void onInit(int i) {
        if(i == TextToSpeech.SUCCESS){
            ttsReady = true;
            tts.setLanguage(new Locale("spa", "ES"));
        }
    }

    private Intent getSttIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, new Locale("spa", "ES"));
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getString(R.string.speak));
        return intent;
    }

    public void onCall() {
        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE);

        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CALL_PHONE},
                    Integer.parseInt("123"));
        } else {
            //startActivity(new Intent(Intent.ACTION_CALL).setData(Uri.parse("tel:12345678901")));
        }
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
        editMessage.setText(result);
    }
}
