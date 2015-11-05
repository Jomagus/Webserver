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
            HttpRequest AnfragenBearbeiter = new HttpRequest(SekundaerSocket);
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
        * Zugriffszeiten zu verkleinern. Wir gehen von etwas mehr als einer Dateiendung pro Zeile in der
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

    HttpRequest(Socket AnfragenSocket) {
        this.ClientSocket = AnfragenSocket;
    }

    @Override
    public void run() {
        try {
            processHttpRequest();
        } catch (Exception e) {
            System.err.println("Unbekannter Fehler beim bearbeiten einer Anfrage aufgetreten. Beende bearbeitung dieses Clients...");
        } finally {
            if (!ClientSocket.isClosed()) {
                try {
                    ClientSocket.close();
                } catch (IOException e) {
                    System.err.println("Probleme beim schliessen des Sockets. Daumen druecken und durch...");
                }
            }
        }
    }

    private void processHttpRequest() throws Exception
    {
        // Wir oeffnen Input- und Outputstreams zu unserem Client
        InputStream ClientInputStream = null;
        DataOutputStream ClientDataOutputStream = null;

        try {
            ClientInputStream = ClientSocket.getInputStream();
            ClientDataOutputStream = new DataOutputStream(ClientSocket.getOutputStream());
        } catch (IOException e) {
            System.err.println("Probleme beim aufbauen von Streams zum Client. Breche ab...");
            ClientSocket.close();
            return;
        } finally {
            if (ClientInputStream == null || ClientDataOutputStream == null) {
                System.err.println("Seltsame Probleme beim aufbauen von Streams zum Client. Breche ab...");
                ClientSocket.close();
                return;
            }
        }

        // Wir dekodieren den ClientInputStream und wrappen um ihn einen BufferedReader
        BufferedReader ClientBufferedReader = new BufferedReader(new InputStreamReader(ClientInputStream));




        // Get the request line of the HTTP request message.
        String requestLine = ClientBufferedReader.readLine();

        // Display the request line.
        System.out.println();
        System.out.println(requestLine);

        // Get and display the header lines.
        String headerLine = null;
        while ((headerLine = ClientBufferedReader.readLine()).length() != 0) {
            System.out.println(headerLine);
        }

        // Wir schliessen all unsere Streams und den Socket
        ClientDataOutputStream.close();
        ClientBufferedReader.close();
        ClientInputStream.close();
        ClientSocket.close();
    }
}