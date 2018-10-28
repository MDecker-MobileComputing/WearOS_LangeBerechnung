package de.mide.wear.langeberechnung;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.Locale;


/**
 * This project is licensed under the terms of the BSD 3-Clause License.
 */
public class MainActivity extends WearableActivity
                          implements View.OnClickListener {

    /**
     * Objekt zum Formatieren der Ergebnis-Zahl, verwendet immer deutsches Zahlenformat,
     * unabhängig von der aktuell auf dem WearOS-Gerät eingestellten Sprache.
     * Deutsches Zahlenformat: Punkt statt Komma nach jeder 3er-Gruppe von Zahlen vor
     * dem Komma.
     * <br><br>
     *
     * Beispiel Formatierung von Tausend: <pre>1.000</pre>
     * <br>
     *
     * Beispiel Formatierung von einer Million: <pre>1.000.000</pre>
     */
    protected NumberFormat _zahlFormatierer = NumberFormat.getNumberInstance( Locale.GERMAN );

    /**
     * Faktor zur Umrechnung der Laufzeit in Nano-Sekunden in Sekunden;
     * 10<sup>9</sup> Nanosekunden entsprechen einer Sekunde.
     * <br><br>
     *
     * Diese Konstante wird benötigt, weil für die Laufzeitmessung
     * die Methode {@link System#nanoTime()} zur Bestimmung der Start-
     * und Endzeit verwendet wird.
     * <br><br>
     *
     * Es wird die mit Java7 eingeführte Möglichkeit zur Verwendung von Unterstrichen
     * für die Gruppierung eines Zahl-Literals zur besseren Lesbarkeit verwendet
     * (siehe auch <a href="http://bit.ly/2CKFQVM" target="_blank">hier</a>).
     *
     */
    protected static final long ZEHN_HOCH_NEUN = 1_000_000_000;


    /** Button zum Start der Berechnung. */
    protected Button _startButton = null;

    /** UI-Element zur Eingabe der Zahl die potenziert werden soll. */
    protected EditText _zahlEditText = null;


    /**
     * Lifecycle-Methode. Lädt Layout-Datei, füllt Member-Variablen
     * mit Referenzen auf UI-Elemente und registriert Activity-Instanz
     * selbst als Event-Handler-Objekt für den Button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _zahlEditText = findViewById( R.id.zahlEditText );
        _startButton  = findViewById( R.id.startButton  );

        _startButton.setOnClickListener( this );

        setAmbientEnabled(); // Enables Always-on
    }


    /**
     * Event-Handler-Methode für den Button zum Start der
     * Berechnung.
     *
     * @param view  Button, der Event ausgelöst hat.
     */
    @Override
    public void onClick(View view ) {

        String ergebnisString = "";

        try {

            int inputZahl = holeZahl();

            _startButton.setEnabled(false);
            long zeitpunktStart = System.nanoTime();

            // *** eigentliche Berechnung durchführen ***
            ergebnisString = berechnung( inputZahl );

            long zeitpunktEnde = System.nanoTime();
            _startButton.setEnabled(true);


            long laufzeitSekunden = ( zeitpunktEnde - zeitpunktStart ) / ZEHN_HOCH_NEUN;

            ergebnisString = "Ergebnis:\n"     + ergebnisString   +
                             "\n\nLaufzeit: ≈" + laufzeitSekunden + " sec";

        } catch (Exception ex) {

            ergebnisString = "FEHLER:\n" + ex.getMessage();
        }

        Intent intent = new Intent(this, ErgebnisActivity.class);
        intent.putExtra(ErgebnisActivity.EXTRA_KEY_ERGEBNIS, ergebnisString );
        startActivity(intent); // zur ErgebnisActivity springen
    }


    /**
     * Liefert die aktuell in das {@link EditText}-Element eingegebene
     * Zahl zurück. Wenn keine gültige Zahl größer 0 in dieses Element
     * eingegeben ist, dann wird eine Exception geworfen.
     *
     * @return Zahl aus {@link EditText}-Element, größer 0
     *
     * @throws Exception  Im {@link EditText}-Element steht gerade keine gültige Zahl.
     */
    protected int holeZahl() throws Exception {

        String zahlString = _zahlEditText.getText().toString();

        zahlString = zahlString.trim(); // Leerzeichen vor und nach Zahl entfernen

        if (zahlString.length() == 0) {
            throw new Exception("Leeren String eingegeben!");
        }

        int zahl = Integer.parseInt( zahlString ); // kann NumberFormatException werfen

        if (zahl <= 0) {
            throw new Exception("Zahl ist nicht größer als Null!");
        }

        return zahl;
    }


    /**
     * Berechnet <i>"inputParameter hoch drei"</i> auf bewusst ineffiziente Weise,
     * nämlich mit einer dreifach gestaffelten Schleife.<br>
     * Je größer der Wert <code>inputParameter</code> ist, desto länger dauert die Berechnung.
     * Der Speicherplatz steigt aber <i>NICHT</i> mit <code>inputParameter</code>.
     * Normalerweise würde man für diese Berechnung die Methode {@link Math#pow(double, double)}
     * verwenden.
     * <br>
     * <b>Achtung:</b> Laufzeit wächst kubisch mit Wert von <i>inputParameter</i>!
     *
     * @param inputParameter  Zahl, von der die dritte Potenz berechnet werden soll.
     *
     * @return  Berechnungsergebnis (<code>inputParameter</code> hoch 3) als formatierter String
     *          (Punkte zur 3er-Gruppierung vor dem Komma, z.B. <code>1.000.000</code> für
     *           "eine Million").
     */
    protected String berechnung(int inputParameter) {

        long result = 0;

        for (int i = 0; i < inputParameter; i++)
            for (int j = 0; j < inputParameter; j++)
                for (int k = 0; k < inputParameter; k++) {
                    result += 1;
                }


        return _zahlFormatierer.format(result);
    }
}
