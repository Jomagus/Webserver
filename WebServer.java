import java.io.* ;
import java.net.* ;
import java.util.* ;

/**
 *
 */
public final class WebServer
{
    /**
     * Auf diesem Port wird unser Webserver laufen und horchen.
     */
    final static int PORT = 6789;

    public static void main(String argv[]) throws Exception
    {
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
            //TODO hier Thread sicher beenden lassen (Sockets schliessen etc.)
        }
    }

    private void processHttpRequest() throws Exception
    {
        // Wir oeffnen Input- und Outputstreams zu unserem Client
        InputStream ClientInputStream = ClientSocket.getInputStream();
        DataOutputStream ClientDataOutputStream = new DataOutputStream(ClientSocket.getOutputStream());
        //TODO IO-Exceptions von dem Stream oeffnen abfangen

        // Wir dekodieren den ClientInputStream und wrappen um ihn einen BufferedReader
        BufferedReader ClientBufferedReader = new BufferedReader(new InputStreamReader(ClientInputStream));




        // Get the request line of the HTTP request message.
        String requestLine = ClientBufferedReader.readLine();

        // Display the request line.
        System.out.println();
        System.out.println(requestLine);


    }
}