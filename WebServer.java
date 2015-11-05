import java.io.* ;
import java.net.* ;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.* ;
import java.util.concurrent.ConcurrentHashMap;

public final class WebServer
{
    /**
     * Auf diesem Port wird unser Webserver laufen und horchen.
     */
    final static int PORT = 6789;

    public static void main(String argv[]) throws Exception
    {
        // Wir parsen zuerst die Argumente der Kommandozeile

        ConcurrentHashMap MimeTypen = null;
        if (argv.length > 0) {
            if (argv[0].contentEquals("-mime")) {
                if (argv.length == 2) {
                    MimeTypen = ParseMimeTypes(argv[1]);
                } else {
                    System.out.println("Ungueltige Anzahl an Argumenten uebergeben. Ignoriere sie...");
                }
            } else {
                System.out.println("Ungueltige Argumente uebergeben. Ignoriere sie...");
            }
        }

        //TODO Fallback Mimetypes implementieren


        // Wir öffnen hier einen neuen Serversocket der auf eingehende Verbindungen wartet

        ServerSocket PrimaerSocket = null;
        try {
            PrimaerSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            System.err.println("IO-Fehler beim öffnen des Sockets aufgetreten. Breche ab...");
            System.exit(-1);
        } catch (SecurityException e) {
            System.err.println("Nicht genug Rechte zum öffnen des Sockets. Breche ab...");
            System.exit(-1);
        } catch (IllegalArgumentException e) {
            System.err.println("Gewünschter Port nicht belegbar. Breche ab...");
            System.exit(-1);
        } finally {
            if (PrimaerSocket == null) {
                System.err.println("Unbekannter Fehler beim öffnen des Sockets aufgetreten. Breche ab...");
                System.exit(-1);
            }
        }

        // In dieser Schleife werden Anfragen an unseren Server angenommen und fuer jede Anfrage werden neue Threads erzeugt.
        while (true) {
            // wir nehmen anfragen an und stellen eine neue Verbindung zum anfragenden her
            Socket SekundaerSocket = null;
            try {
                SekundaerSocket = PrimaerSocket.accept();
            } catch (IOException e) {
                System.err.println("IO-Fehler beim herstellen der Verbindung. Warte auf neuen Versuch...");
                continue;
            } catch (SecurityException e) {
                System.err.println("Nicht genug Rechte zum bearbeiten der Anfragen. Breche ab...");
                System.exit(-1);
            } finally {
                if (SekundaerSocket == null) {
                    System.err.println("Unbekannter Fehler beim bearbeiten einer Anfrage aufgetreten. Warte auf neuen Versuch...");
                    continue;
                }
            }

            // wir lagern die Anfrageverarbeitung in die HttpRequest Klasse aus
            HttpRequest AnfragenBearbeiter = new HttpRequest(SekundaerSocket, MimeTypen);
            Thread AnfragenBearbeiterThread = new Thread(AnfragenBearbeiter);
            AnfragenBearbeiterThread.start();
        }
    }

    /**
     * Verwandelt eine Datei von Mime-Types in eine Hashstruktur. Diese Methode liest eine Datei an dem uebergebenen
     * Pfad ein, parst sie, und traegt ihren Inhalt in eine Hast-Struktur. Die Struktur hat Dateiendungen als Schluessel
     * und die zugehoerigen Mime-Types als Werte.
     *
     * @param PfadZuMimeTypes Dateipfad zur zu parsenden Datei
     * @return Bei erfolg Hashmap mit Mime-Types, sonst Nullpointer
     */
    private static ConcurrentHashMap ParseMimeTypes(String PfadZuMimeTypes) {
        Path MimeTypePfad = null;

        try {
            MimeTypePfad = Paths.get(PfadZuMimeTypes);
        } catch (InvalidPathException e) {
            System.err.println("Ungueltiger Dateipfad zur Mime Datei. Lese Datei nicht ein...");
            return null;
        } finally {
            if (MimeTypePfad == null) {
                System.err.println("Unbekannter Fehler beim Parsen des Mime Datei Pfads. Lese Datei nicht ein...");
                return null;
            }
        }

        // Wir zaehlen die Zeilen der einzulesenden Datei, um eine effiziente groesse fuer unsere Hashmap abzuschaetzen
        long ZeilenAnzahl = 0;
        try {
            ZeilenAnzahl = Files.lines(MimeTypePfad).count();
        } catch (IOException e) {
            System.err.println("Fehler beim lesen der Mime Datei. Lese Datei nicht ein...");
            return null;
        } catch (SecurityException e) {
            System.err.println("Nicht genug Rechte zum lesen der Mime Datei. Lese Datei nicht ein...");
            return null;
        } finally {
            if (ZeilenAnzahl == 0) {
                System.err.println("Mime Datei ist leer oder anderer Fehler. Lese Datei nicht ein...");
                return null;
            }
        }

        /* Wir erstellen unsere Hashmap. Sie wird dynamisch hochskalieren ind groesse wenn benoetigt.
        * Der Skalierungsfaktor gibt an, wieviel groesser unsere Hashmap als die Zeilenzahl wird. Man moechte eine
        * 30-40% groessere Hashmap haben als Eintraege vorhanden sind, um Kollisionen zu reduzieren und die
        * Zugriffszeiten zu verkleinern. Wir gehen von etwas weniger als einer Dateiendung pro Zeile in der
        * Mime Datei aus. */
        double SkalierungsFaktor = 1.1;
        double SkalierteGroesse = SkalierungsFaktor*ZeilenAnzahl;
        ConcurrentHashMap ParsedMimeTypes = new ConcurrentHashMap((int) SkalierteGroesse);

        try {
            Files.lines(MimeTypePfad).forEach(Zeile -> {
                if (!Zeile.isEmpty() && !Zeile.startsWith("#")) {
                    String[] GeparsteZeile = Zeile.split("\\s+");
                    for (int i = GeparsteZeile.length; i > 1 ; i--) {
                        ParsedMimeTypes.put(GeparsteZeile[i-1], GeparsteZeile[0]);
                    }
                }
            });
        } catch (IOException e) {
            System.err.println("Fehler beim lesen der Mime Datei. Lese Datei nicht ein...");
            return null;
        } catch (SecurityException e) {
            System.err.println("Nicht genug Rechte zum lesen der Mime Datei. Lese Datei nicht ein...");
            return null;
        } catch (NullPointerException e) {
            System.err.println("Nullpointerexception beim schreiben der Hashable. Das ist seltsam. Lese Datei nicht ein...");
            return null;
        }
        return ParsedMimeTypes;
    }

}

final class HttpRequest implements Runnable
{
    /**
     * Komfortvariable fuer das beantworten der HTTP Requests.
     */
    final static String CRLF = "\r\n";

    /**
     * Socket des Clients der von dieser Instanz bearbeitet werden soll.
     */
    Socket ClientSocket;

    /**
     * Map mit Mime Types um in O(1) passende Typ fuer Antwort zu finden.
     */
    Map MimeMap;

    /**
     * Wir speichern unsere Streams Klassenweit, damit wir die Fehlerbehandlung modularisieren und auslagern koennen.
     */
    BufferedReader ClientBufferedReader;
    DataOutputStream ClientDataOutputStream;

    HttpRequest(Socket AnfragenSocket, Map MimeTypes) {
        this.ClientSocket = AnfragenSocket;
        this.MimeMap = MimeTypes;
        this.ClientBufferedReader = null;
        this.ClientDataOutputStream = null;
    }

    @Override
    public void run() {
        try {
            processHttpRequest();
        } catch (Exception e) {
            System.err.println("Unbekannter Fehler beim bearbeiten einer Anfrage aufgetreten. Beende bearbeitung dieses Clients...");
        } finally {
            // Wir schliessen all unsere Streams und den Socket
            BrecheAllesAb();
        }
    }

    private void processHttpRequest() throws Exception
    {
        // Wir oeffnen Input- und Outputstreams zu unserem Client
        InputStream ClientInputStream = null;

        try {
            ClientInputStream = ClientSocket.getInputStream();
            ClientDataOutputStream = new DataOutputStream(ClientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Probleme beim aufbauen von Streams zum Client. Breche ab...");
            if (!ClientSocket.isInputShutdown() && ClientInputStream != null) {
                ClientInputStream.close();
            }
            if (!ClientSocket.isOutputShutdown() && ClientDataOutputStream != null) {
                ClientDataOutputStream.close();
            }
            ClientSocket.close();
            return;
        } finally {
            if (ClientInputStream == null || ClientDataOutputStream == null) {
                System.err.println("Seltsame Probleme beim aufbauen von Streams zum Client. Breche ab...");
                BrecheAllesAb();
                return;
            }
        }

        // Wir dekodieren den ClientInputStream und wrappen um ihn einen BufferedReader
        ClientBufferedReader = new BufferedReader(new InputStreamReader(ClientInputStream));

        // Mit diesen Variablen speichern wir unsere Anfrage
        String RequestZeile;
        String AnfrageZeile;
        Map AnfrageMap = new HashMap<>(10);
        String[] GeteilteRequestZeile = null;
        String[] GeteilteAnfrageZeile = null;

        try {
            // Wir holen uns die Request Zeile
            RequestZeile = ClientBufferedReader.readLine();
            GeteilteRequestZeile = RequestZeile.split("\\s");

            // Wir speichern die komplette Anfrage, aber ohne Requestzeile, in einer Hashmap
            while ((AnfrageZeile = ClientBufferedReader.readLine()).length() != 0) {
                GeteilteAnfrageZeile = AnfrageZeile.split("\\s+", 2);
                if (GeteilteAnfrageZeile.length == 2) {
                    AnfrageMap.put(GeteilteAnfrageZeile[0], GeteilteAnfrageZeile[1]);
                }
            }
        } catch (IOException e) {
            System.err.println("Probleme beim lesen von Streams zum Client. Breche ab...");
            try {
                ClientBufferedReader.close();
                ClientDataOutputStream.close();
                ClientSocket.close();
            } catch (IOException ex) {
                System.err.println("Mehr Probleme beim lesen von Streams zum Client. Breche haerter ab...");
                ClientSocket.close();
            }
        } finally {
            if (GeteilteAnfrageZeile == null) {
                System.err.println("Unbekannte Probleme beim lesen von Streams zum Client. Breche ab...");
                try {
                    ClientBufferedReader.close();
                    ClientDataOutputStream.close();
                } catch (IOException ex) {
                    System.err.println("Viel Mehr Probleme beim lesen von Streams zum Client. Breche haerter ab...");
                    return;
                }
            }
        }

        //************************************************
        // Ab hier beginnt die Bearbeitung der Anfrage
        //************************************************

        if (GeteilteRequestZeile.length != 3) {
            //TODO evtl Error 400 Bad Request
            System.out.println("Ungueltigen Request-Line bekommen. Breche ab...");
            BrecheAllesAb();
            return;
        }

        String Header;

        switch (GeteilteRequestZeile[0]) {
            case "GET":
                Header = HoleHEADer(GeteilteRequestZeile[1]);
                // Wir schauen ob die Datei nicht existiert und senden dann eine 404 Seite
                if (Header.startsWith("HTTP/1.0 404")) {
                    //TODO konstruiere 404 Site
                    return;
                }

                //Falls die Datei existiert, bereiten wir sie fuer den Versand vor
                String DateiName = "." + GeteilteRequestZeile[1];
                File GeforderteDatei = new File(DateiName);






                break;
            case "HEAD":
                Header = HoleHEADer(GeteilteRequestZeile[1]);
                try {
                    // Hier versenden wir den geforderten Header und flushen zur Sicherheit (close flusht auch)
                    ClientDataOutputStream.writeBytes(Header);
                    ClientDataOutputStream.writeBytes(CRLF);
                    ClientDataOutputStream.flush();
                } catch (IOException e) {
                    System.err.println("Fehler beim Senden einer HEAD Anfrage. Breche ab...");
                }
                break;
            case "POST":
                break;
            default:
                Header = "HTTP/1.0 501 Not Implemented";
                try {
                    ClientDataOutputStream.writeBytes(Header);
                    ClientDataOutputStream.writeBytes(CRLF);
                    ClientDataOutputStream.flush();
                } catch (IOException e) {
                    System.err.println("Fehler beim Senden eines 501 Fehlers. Breche ab...");
                }
        }
    }

    /**
     * Generiert einen Header fuer GET oder HEAD Anfrage mit dem gegebenen URI.
     * @param RequestURI die URI aus der Anfrage
     * @return Den Header fuer die betreffende Anfrage
     */
    private String HoleHEADer(String RequestURI) {
        // Die URI ist eine Pfadangabe zur gewuenschten Datei. Der Punkt ist um sie aufs aktuelle Verzeichniss zu zentieren.
        String DateiName = RequestURI;
        DateiName = "." + DateiName;

        // Wir muessen nun schauen, ob diese Datei existiert. (Aber nicht oeffnen, diese Methode ist nur fuer den Header.)
        File DateiZumUeberpruefen = new File(DateiName);

        boolean FehlerVerboten = false;
        boolean DateiExistiert = false;

        try {
            DateiExistiert = DateiZumUeberpruefen.exists() && !DateiZumUeberpruefen.isDirectory();
        } catch (SecurityException e) {
            FehlerVerboten = true;
        }

        // Wir antworten nur mit HTTP/1.0
        String Statusline = "HTTP/1.0 ";

        // Wir geben den Mime Type der Datei im Responde-Header zurueck
        String ContentTypeLine = "Content-type: ";
        if (DateiExistiert) {
            Statusline += "200 OK" + CRLF;
            ContentTypeLine += contentType(DateiName) + CRLF;
        } else if (FehlerVerboten) {
            Statusline += "403 Forbidden" + CRLF;
            ContentTypeLine += "text/html" + CRLF;
        } else {
            Statusline += "404 Not Found" + CRLF;
            ContentTypeLine += "text/html" + CRLF;
        }

        // Der fertige Header besteht aus Statusline und Headerline
        return Statusline + ContentTypeLine;
    }

    /**
     * Liefert den Mime-Type zu einer Datei.
     * @param DateiName Der Dateiname
     * @return Den zur Dateiendung der Datei gehoerenden Mime Type
     */
    private String contentType(String DateiName) {
        // Zunaechst muessen wir vom Dateinamen auf die Dateiendung kommen
        int PunktPosition = DateiName.lastIndexOf(".");
        String DateiEndung;
        try {
            DateiEndung = DateiName.substring(PunktPosition+1);
        } catch (IndexOutOfBoundsException e) {
            DateiEndung = "";
        }

        // Nun muessen wir nur noch unsere Mime Type in der HashMap suchen (in O(1))
        String MimeType = (String) MimeMap.get(DateiEndung);
        //Falls kein MimeType gefunden worden ist, geben wir die geforderte Standartantwort
        if (MimeType == null) {
            MimeType = "application/octet-stream";
        }

        return MimeType;
    }

    /**
     * Versucht alle noch offenen Streams und den Socket zu schliessen.
     */
    private void BrecheAllesAb() {
        // Die Errorflag wird gestzt, wenn es das schliessen eines Teils fehlschlaegt
        boolean Errorflag = false;

        // Zuerst versuchen wir den Bufferedreader und damit auch den Inputstream zu schliessen
        if (!ClientSocket.isInputShutdown()) {
            if (ClientBufferedReader != null) {
                try {
                    ClientBufferedReader.close();
                } catch (IOException e) {
                    System.err.println("Fehler beim schliessen des Bufferedreaders.");
                    Errorflag = true;
                }
            } else {
                try {
                    ClientSocket.getInputStream().close();
                } catch (IOException e) {
                    System.err.println("Fehler beim schliessen des Inputstreams.");
                    Errorflag = true;
                }
            }
        }

        // Nun versuchen wir den Dataoutputstream zu schliessen
        if (!ClientSocket.isOutputShutdown()) {
            if (ClientDataOutputStream != null) {
                try {
                    ClientDataOutputStream.close();
                } catch (IOException e) {
                    Errorflag = true;
                    System.err.println("Fehler beim schliessen des Dataoutputstreams.");
                }
            }
        }

        // Jetzt bleibt es nur noch den Socket zu schliessen
        if (!ClientSocket.isClosed()) {
            try {
                ClientSocket.close();
            } catch (IOException e) {
                System.err.println("Fehler beim schliessen des Sockets.");
            }
        }

        if (Errorflag || !ClientSocket.isClosed()) {
            System.err.println("Manche Verbindungen konnten nicht terminiert werden. Bei Problemen starten sie den Server neu");
        }
    }
}