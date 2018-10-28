package de.mide.wear.langeberechnung;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.w3c.dom.Text;


/**
 * Sekundäre Activity der App zur Anzeige eines Ergebnisses oder
 * einer Fehlermeldung.
 * <br><br>
 *
 * This project is licensed under the terms of the BSD 3-Clause License.
 */
public class ErgebnisActivity extends WearableActivity
                              implements View.OnClickListener {


    /** Key für Extra, das den von dieser Activity anzuzeigenden Text enthält. */
    public static final String EXTRA_KEY_ERGEBNIS = "ergebnis";

    /**
     * {@link TextView}-Element zur Anzeige der Ergebnisses (kann auch eine
     * Fehlermeldung sein.
     */
    protected TextView _ergebnisTextView = null;


    /**
     * Lifecycle-Methode. Lädt Layout-Datei, füllt Member-Variablen
     * mit Referenzen auf UI-Elemente und registriert Activity-Instanz
     * selbst als Event-Handler-Objekt für den Button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ergebnis);

        _ergebnisTextView = findViewById( R.id.ergebnisTextView );

        ergebnisTextAnzeigen();

        Button zurueckButton = findViewById( R.id.zurueckButton );
        zurueckButton.setOnClickListener( this );

        setAmbientEnabled(); // Enables Always-on
    }


    /**
     * Methode liest als Ergebnis anzuzeigenden Text aus dem Intent-Objekt
     * aus und stellt ihn im {@link TextView}-Element dar.
     */
    protected void ergebnisTextAnzeigen() {

        Intent intent = getIntent(); // Intent, mit dem diese Activity-Instanz gestartet wurde

        if ( intent.hasExtra(EXTRA_KEY_ERGEBNIS) == false ) {

            _ergebnisTextView.setText("INTERNER FEHLER:\nKein Ergebnis in Intent gefunden.");
            return;
        }

        String ergebnisText = intent.getStringExtra( EXTRA_KEY_ERGEBNIS );
        _ergebnisTextView.setText( ergebnisText );
    }


    /**
     * Event-Handler-Methode für den Button zur Rückkehr
     * zur {@link MainActivity}.
     *
     * @param view  Button, der Event ausgelöst hat.
     */
    @Override
    public void onClick(View view ) {

        finish();
    }

}
