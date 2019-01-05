package de.mide.wear.langeberechnung;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.text.NumberFormat;
import java.util.Locale;


/**
 * Diese WearOS-App zeigt, wie man eine lange Berechnung in einen Hintergrund-Thread
 * auslagert.
 * <br><br>
 *
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
     * Beispiel Formatierung von Tausend: <pre>1.000</pre><br>
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
     */
    protected static final long ZEHN_HOCH_NEUN = 1_000_000_000;

    /** Button zum Start oder Abbrechen der Berechnung. */
    protected Button _startStopButton = null;

    /** UI-Element zur Eingabe der Zahl, von der die dritte Potenz berechnet werden soll. */
    protected EditText _zahlEditText = null;

    /** UI-Element zur Fortschrittsanzeige. */
    protected ProgressBar _progressBar = null;

    /**
     * Variable ist genau dann <code>true</code>, wenn gerade eine Berechnung
     * läuft; darf nicht direkt geändert werden, sondern nur über Methode
     * {@link MainActivity#setzteStatusBerechnungLaueft(boolean)}.
     */
    protected boolean _berechnungLaeuft = false;

    /**
     * Wenn diese Variable auf <code>true</code> gesetzt ist, dann soll
     * der Berechnungs-Thread stoppen; damit dies erkannt wird muss
     * während der Berechnung vom Thread regelmäßig der Wert dieser
     * Variable abgefragt werden.
     * <br><br>
     *
     * Die Variable ist mit dem Modifizierer <code>volatile</code>
     * versehen, damit Threads den Wert dieser Variable nicht
     * cachen (siehe z.B.
     * <a href="http://bit.ly/2DahHsy" target="_blank">diesen Artikel</a>);
     * wenn ein Thread den Wert dieser Variable cachen würde, dann würde
     * der Abbruch des Threads nicht funktionieren.
     */
    protected volatile boolean _stoppSignalFuerBerechnung = false;


    /**
     * Lifecycle-Methode. Lädt Layout-Datei, füllt Member-Variablen
     * mit Referenzen auf UI-Elemente und registriert Activity-Instanz
     * selbst als Event-Handler-Objekt für den Button.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _zahlEditText    = findViewById( R.id.zahlEditText        );
        _startStopButton = findViewById( R.id.startStopButton     );
        _progressBar     = findViewById( R.id.fortschrittsanzeige );

        _startStopButton.setOnClickListener( this );

        setAmbientEnabled(); // Enables Always-on
    }


    /**
     * Methode zum Starten der Berechnung.
     */
    protected void starteBerechnung() {

        if (_berechnungLaeuft == true) {
            zeigeTextAufErgebnisActivity( "INTERNER FEHLER:\nBerechnung läuft schon." );
            return;
        }

        int inputZahl = -1;

        try {
            inputZahl = holeZahl();

        } catch (Exception ex) {

            String fehlerString = "Ungültige Eingabe:\n" + ex.getMessage();
            zeigeTextAufErgebnisActivity( fehlerString );
            return;
        }

        MeinAsyncTask meinAsyncTask = new MeinAsyncTask();
        meinAsyncTask.execute(inputZahl);
    }


    /**
     * Methode zum Abbrechen einer laufenden Berechnung.
     */
    protected void stoppeBerechnung() {

        if (_berechnungLaeuft == false) {

            zeigeTextAufErgebnisActivity( "INTERNER FEHLER:\nBerechnung läuft nicht." );
            return;
        }

        _stoppSignalFuerBerechnung = true;
    }


    /**
     * Setzt Status-Flag (Member-Variable) und aktualisiert UI entsprechend.
     *
     * @param berechnungLaeuft  Neuer Status.
     */
    protected void setzteStatusBerechnungLaueft(boolean berechnungLaeuft) {

        _berechnungLaeuft = berechnungLaeuft;

        if (_berechnungLaeuft == true) {

            _zahlEditText.setEnabled(false);
            _startStopButton.setText("Abbrechen");

            _progressBar.setProgress(0);
            _progressBar.setVisibility(View.VISIBLE);

        } else {

            _zahlEditText.setEnabled(true);
            _startStopButton.setText("Berechnung starten");
            _progressBar.setVisibility(View.INVISIBLE);
        }
    }


    /**
     * Event-Handler-Methode für den Button; je nach Status der Berechnung
     * wird entwender die Methode {@link MainActivity#starteBerechnung()}
     * oder die Methode {@link MainActivity#stoppeBerechnung()} aufgerufen.
     *
     * @param view  Button, der Event ausgelöst hat (wird nicht ausgewertet,
     *              da die Activity nur einen Button hat).
     */
    @Override
    public void onClick(View view ) {

        if (_berechnungLaeuft == false) {

            starteBerechnung();

        } else { // Berechnung abbrechen

            stoppeBerechnung();
        }
    }


    /**
     * Zeigt <code>text</code> auf {@link ErgebnisActivity} an; diese
     * Activity wird über einen expliziten Intent geöffnet, der Text
     * wird hierbei als Extra mitgegeben.
     *
     * @param text  Auf {@link ErgebnisActivity} anzuzeigender Text
     *              (kann auch Fehlermeldung sein).
     */
    protected void zeigeTextAufErgebnisActivity(String text) {

        Intent intent = new Intent( this, ErgebnisActivity.class );
        intent.putExtra( ErgebnisActivity.EXTRA_KEY_ERGEBNIS, text );
        startActivity( intent );
    }


    /**
     * Liefert die aktuell in das {@link EditText}-Element eingegebene
     * Zahl zurück. Wenn keine gültige Zahl größer 0 in dieses Element
     * eingegeben ist, dann wird eine Exception geworfen.
     *
     * @return Zahl aus {@link EditText}-Element, größer 0.
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


    /* **************************** */
    /* *** Start innere Klassen *** */
    /* **************************** */

    /**
     * Innere Klasse, Unterklasse von {@link AsyncTask},
     * zur Durchführung der Berechnung in einem Worker-Thread.
     * Die Klasse {@link AsyncTask} steht nur unter Android
     * (und nicht unter "normalem" Java) zur Verfügung.
     */
    public class MeinAsyncTask extends AsyncTask<Integer, Integer, String> {

        /**
         * Diese Methode wird im Main-Thread ausgeführt, kann also UI-Änderungen
         * vornehmen. Die Methode wird unmittelbar vor Start der Methode
         * {@link MeinAsyncTask#doInBackground(Integer...)} aufgerufen.
         */
        @Override
        public void onPreExecute() {

            setzteStatusBerechnungLaueft( true );
        }


        /**
         * Die lange Berechnung wird von dieser Methode in einem Hintergrund-Thread
         * (Worker-Thread) durchgeführt.
         *
         * @param params  Eine int-Zahl, von der die dritte Potenz berechnet
         *                wird; es wird davon ausgeganngen, dass dieser
         *                varags genau eine Zahl enthält.
         *
         * @return  String mit Berechnungs-Ergebnis, der der Methode
         *          {@link MeinAsyncTask#onPostExecute(String)} als
         *          Argument übergeben wird.
         */
        @Override
        public String doInBackground(Integer... params) {

            _stoppSignalFuerBerechnung = false;

            int inputZahl = params[0];

            long zeitpunktStart = System.nanoTime();

            /* ****** Eigentliche Berechnung ****** */
            String berechnungsErgebnisString = "";
            long result = 0;
            for (int i = 0; i < inputZahl; i++) {
                for (int j = 0; j < inputZahl; j++) {
                    for (int k = 0; k < inputZahl; k++) {
                        result += 1;
                    }
                }

                if (_stoppSignalFuerBerechnung == true) {
                    break;
                }

                if (i % 10 == 9) {
                    int prozentWertInt = (int) (i * 100.0 / inputZahl);
                    publishProgress(prozentWertInt);
                }
            }

            if (_stoppSignalFuerBerechnung == false) {
                berechnungsErgebnisString = result + "";
            }

            /* ****** Eigentliche Berechnung ****** */

            long zeitpunktEnde = System.nanoTime();

            long laufzeitSekunden = ( zeitpunktEnde - zeitpunktStart ) / ZEHN_HOCH_NEUN;

            final boolean berechnungAbgebrochen;
            if (berechnungsErgebnisString.trim().length() == 0) {
                return "Berechnung wurde vom Nutzer abgebrochen!";
            }

            String ergebnisString = "Ergebnis:\n"     + berechnungsErgebnisString   +
                                    "\n\nLaufzeit: ≈" + laufzeitSekunden            + " sec";

            return ergebnisString;
        }


        /**
         * Diese Methode wird im Main-Thread ausgeführt und für jeden Aufruf von
         * {@link AsyncTask#publishProgress(Object[])} aufgerufen.
         *
         * @param values  Fortschritt in Prozent
         */
        @Override
        protected void onProgressUpdate(Integer... values) {

            int fortschrittProzent = values[0];
            _progressBar.setProgress(fortschrittProzent);
        }


        /**
         * Methode zur Darstellung des Ergebnisses, wird im Main-Thread
         * ausgeführt.
         *
         * @param ergebnisString  Ergebnis (return-Wert) von Methode
         *                        {@link MeinAsyncTask#doInBackground(Integer...)}.
         */
        @Override
        public void onPostExecute(String ergebnisString) {

            setzteStatusBerechnungLaueft( false );
            zeigeTextAufErgebnisActivity( ergebnisString );
        }

    };

    /* **************************** */
    /* *** Ende innere Klassen  *** */
    /* **************************** */

}
