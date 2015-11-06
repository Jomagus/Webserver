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
     * Map mit Metadaten aus der Anfrage.
     */
    Map AnfrageMap;

    /**
     * Wir speichern unsere Streams Klassenweit, damit wir die Fehlerbehandlung modularisieren und auslagern koennen.
     */
    BufferedReader ClientBufferedReader;
    DataOutputStream ClientDataOutputStream;

    /**
     * Wahr wenn der Inputstream mit UTF8 dekodiert wird.
     */
    boolean UTF8EncodingAktiv;

    HttpRequest(Socket AnfragenSocket, Map MimeTypes) {
        this.ClientSocket = AnfragenSocket;
        this.MimeMap = MimeTypes;
        this.ClientBufferedReader = null;
        this.ClientDataOutputStream = null;
        this.UTF8EncodingAktiv = false;
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
        // Wir waehlen UTF-8 ENcoding, um die Content Length bei POST Request zu bestimmen
        ClientBufferedReader = null;
        try {
            ClientBufferedReader = new BufferedReader(new InputStreamReader(ClientInputStream, "UTF8"));
            UTF8EncodingAktiv = true;
        } catch (UnsupportedEncodingException e) {
            System.err.println("UTF-8 Encoding fuer den Inputstream nicht verfuegbar. Deaktiviere POST Funktionalitaet.");
            ClientBufferedReader = new BufferedReader(new InputStreamReader(ClientInputStream));
        }

        // Mit diesen Variablen speichern wir unsere Anfrage
        String RequestZeile;
        String AnfrageZeile;
        AnfrageMap = new HashMap<>(10);
        String[] GeteilteRequestZeile = null;
        String[] GeteilteAnfrageZeile = null;

        try {
            // Wir holen uns die Request Zeile
            RequestZeile = ClientBufferedReader.readLine();
            GeteilteRequestZeile = RequestZeile.split("\\s");

            // Wir speichern die kompletten Anfrage Header, aber ohne Requestzeile, in einer Hashmap
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

        String Header;

        if (GeteilteRequestZeile.length != 3) {
            Header = "HTTP/1.0 400 Bad Request" + CRLF + "Content-type: text/html" + CRLF;
            String FehlerSeite = GeneriereErrorSeite("400 Bad Request");
            try {
                // Hier versenden wir den geforderten Header, eine Fehlerseite und flushen zur Sicherheit (close flusht auch)
                ClientDataOutputStream.writeBytes(Header);
                ClientDataOutputStream.writeBytes(CRLF);
                ClientDataOutputStream.writeBytes(FehlerSeite);
                ClientDataOutputStream.flush();
            } catch (IOException e) {
                System.err.println("Fehler beim Senden eines 400 Fehlers. Breche ab...");
            }
            return;
        }

        switch (GeteilteRequestZeile[0]) {
            case "GET":
                Header = HoleHEADer(GeteilteRequestZeile[1]);
                // Wir schauen ob die Datei nicht existiert und senden dann eine 404 Seite; bei Zugriffsverletzung 403 Seite
                if (Header.startsWith("HTTP/1.0 40")) {
                    String FehlerSeite = "";

                    if (Header.startsWith("HTTP/1.0 404")) {
                        FehlerSeite = GeneriereErrorSeite("404 Not Found");
                    } else if (Header.startsWith("HTTP/1.0 403")) {
                        FehlerSeite = GeneriereErrorSeite("403 Forbidden");
                    }

                    // Und senden dann alles
                    try {
                        ClientDataOutputStream.writeBytes(Header);
                        ClientDataOutputStream.writeBytes(CRLF);
                        ClientDataOutputStream.writeBytes(FehlerSeite);
                        ClientDataOutputStream.flush();
                    } catch (Exception e) {
                        System.err.println("Fehler beim Senden einer GET Anfrage. Breche ab...");
                    }
                    return;
                }

                //Falls die Datei existiert, bereiten wir sie fuer den Versand vor, dazu oeffnen wir einen Filestream
                String DateiName = "." + GeteilteRequestZeile[1];

                FileInputStream DateiStrom = null;
                try {
                    DateiStrom = new FileInputStream(DateiName);
                } catch (Exception e) {
                    System.err.println("Datei ist magisch. Ein Einhorn wird sie gestohlen oder versteckt haben. Breche ab...");
                    return;
                }

                // Wir erstellen noch einen Buffer fuer die eigentliche Datei
                byte[] Buffer = new byte[1024];
                int bytes = 0;

                // Nun versenden wir alle Daten
                try {
                    ClientDataOutputStream.writeBytes(Header);
                    ClientDataOutputStream.writeBytes(CRLF);
                    while((bytes = DateiStrom.read(Buffer)) != -1 ) {
                        ClientDataOutputStream.write(Buffer, 0, bytes);
                    }
                    ClientDataOutputStream.flush();
                } catch (Exception e) {
                    System.err.println("Fehler beim Senden einer GET Anfrage. Breche ab...");
                } finally {
                    try {
                        DateiStrom.close();
                    } catch (IOException e) {
                        System.err.println("Fehler beim schliessen eines Dateistroms. Breche ab...");
                    }
                }
                break;
            case "HEAD":
                Header = HoleHEADer(GeteilteRequestZeile[1]);
                try {
                    ClientDataOutputStream.writeBytes(Header);
                    ClientDataOutputStream.writeBytes(CRLF);
                    ClientDataOutputStream.flush();
                } catch (IOException e) {
                    System.err.println("Fehler beim Senden einer HEAD Anfrage. Breche ab...");
                }
                break;
            case "POST":
                // Falls der Request keine gueltige Content-Length Angabe macht, wird Error 400 ausgegeben
                boolean GueltigeAnfrage = false;
                int InhaltsLaenge = -1;
                if (AnfrageMap.containsKey("Content-Length:")) {
                    InhaltsLaenge = Integer.parseInt((String) AnfrageMap.get("Content-Length:"));
                    if (InhaltsLaenge >= 0) {
                        GueltigeAnfrage = true;
                    }
                }

                // Eine POST Anfrage muss eine gueltige Content Length haben
                if (!GueltigeAnfrage) {
                    Header = "HTTP/1.0 400 Bad Request" + CRLF + "Content-type: text/html" + CRLF;
                    String FehlerSeite = GeneriereErrorSeite("400 Bad Request");
                    try {
                        ClientDataOutputStream.writeBytes(Header);
                        ClientDataOutputStream.writeBytes(CRLF);
                        ClientDataOutputStream.writeBytes(FehlerSeite);
                        ClientDataOutputStream.flush();
                    } catch (IOException e) {
                        System.err.println("Fehler beim Senden eines 400 Fehlers. Breche ab...");
                    }
                    return;
                }

                // Wenn der Inputstream nicht mit UTF8 dekodiert wird, koennen wir die Content Length nicht bestimmen
                if (!UTF8EncodingAktiv) {
                    Header = "HTTP/1.0 500 Internal Server Error" + CRLF + "Content-type: text/html" + CRLF;
                    String FehlerSeite = GeneriereErrorSeite("500 Internal Server Error");
                    try {
                        ClientDataOutputStream.writeBytes(Header);
                        ClientDataOutputStream.writeBytes(CRLF);
                        ClientDataOutputStream.writeBytes(FehlerSeite);
                        ClientDataOutputStream.flush();
                    } catch (IOException e) {
                        System.err.println("Fehler beim Senden eines 500 Fehlers. Breche ab...");
                    }
                    return;
                }

                /* Nun bearbeiten wir die eigentlich Anfrage. Dazu muessen wir den Request
                * Body lesen. Dieser ist noch im BufferedReader, da wir das lesen nachdem wir
                * mit dem Header durch waren eingestellt haben.
                * Wenn wir hier ankommen, wissen wir, dass der Stream mit UTF8 formatiert ist.
                * Das bedeutet, dass ein Zeichen 8 Bit lang ist. Die Content Length aus dem POST
                * Header ist eine Ganzzahl die die Anzahl gesendeter Octets angibt. Wir koennen also
                * einfach Zeichen zahlen. */

                boolean FehlerBeimEinlesen = false;
                StringBuilder AnfragenZusammensetzer = new StringBuilder();
                int GelesenesZeichen = -1;
                for (int i = 0; i < InhaltsLaenge; i++) {
                    // Wir koennen nur int lesen, daher casten wir spaeter in auf char
                    try {
                        GelesenesZeichen = ClientBufferedReader.read();
                    } catch (IOException e) {
                        FehlerBeimEinlesen = true;
                        break;
                    } finally {
                        if (GelesenesZeichen == -1) {
                            FehlerBeimEinlesen = true;
                            break;
                        }
                    }
                    AnfragenZusammensetzer.append((char) GelesenesZeichen);
                }

                // Falls ein Fehler beim Einlesen des POST Requests aufgetreten ist, geben wir eine Error Response
                if (FehlerBeimEinlesen) {
                    Header = "HTTP/1.0 500 Internal Server Error" + CRLF + "Content-type: text/html" + CRLF;
                    String FehlerSeite = GeneriereErrorSeite("500 Internal Server Error");
                    try {
                        ClientDataOutputStream.writeBytes(Header);
                        ClientDataOutputStream.writeBytes(CRLF);
                        ClientDataOutputStream.writeBytes(FehlerSeite);
                        ClientDataOutputStream.flush();
                    } catch (IOException e) {
                        System.err.println("Fehler beim Senden eines 500 Fehlers. Breche ab...");
                    }
                    return;
                }

                String PostAnfrage = AnfragenZusammensetzer.toString();

                //TODO  Das hier gut testen und was mit der Post Anfrage anfangen
                System.out.println(PostAnfrage);

                Header = "HTTP/1.0 200 OK" + CRLF;
                try {
                    ClientDataOutputStream.writeBytes(Header);
                    ClientDataOutputStream.writeBytes(CRLF);
                    ClientDataOutputStream.flush();
                } catch (IOException e) {
                    System.err.println("Fehler beim Senden des 200 OK fuer POST. Breche ab...");
                }

                break;
            default:
                Header = "HTTP/1.0 501 Not Implemented" + CRLF;
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
     * Liefert einen String mit HTML Code fuer eine Fehlerseite.
     * Ebenso werden Clientverbindungsinformationen mit eingebettet.
     *
     * @param FehlerTitel Der Titel der Fehlerseite
     * @return Eine HTML Fehlerseite
     */
    private String GeneriereErrorSeite(String FehlerTitel) {
        // Wir besorgen gewuenschte Informationen fuer die Fehlerseite
        String ClientIP = ClientSocket.getInetAddress().toString();
        if (ClientIP == null) {
            ClientIP = "Unbekannt";
        }

        String UserAgent = AnfrageMap.get("User-Agent:").toString();
        if (UserAgent == null) {
            UserAgent = "Unbekannt";
        }

        // Und genieren dann die Fehlerseite
        String FehlerSeite = "<HTML><HEAD><TITLE>"
                + FehlerTitel
                +"</TITLE></HEAD><BODY>"
                + FehlerTitel
                +"<br>"
                +"Aufrufende Client IP: " + ClientIP
                +"<br>User Agent: " + UserAgent
                +"</BODY></HTML>";

        return FehlerSeite;
    }

    /**
     * Liefert den Mime-Type zu einer Datei.
     *
     * @param DateiName Der Dateiname
     * @return Den zur Dateiendung der Datei gehoerenden Mime Type
     */
    private String contentType(String DateiName) {
        // Zunaechst muessen wir vom Dateinamen auf die Dateiendung kommen
        int PunktPosition = DateiName.lastIndexOf(".");
        String DateiEndung;
        try {
            DateiEndung = DateiName.substring(PunktPosition+1).toLowerCase();
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