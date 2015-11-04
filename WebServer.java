import java.io.* ;
import java.net.* ;
import java.util.* ;

/**
 *
 */
public final class WebServer
{
    public static void main(String argv[]) throws Exception
    {
        final int PORT = 6789;                                          // auf diesem Port wird unser Webserver laufen.

        // Wir öffnen hier einen neuen Serversocket der auf eingehende Verbindungen wartet

        ServerSocket MeinTollerServerSocket = null;
        try {
            MeinTollerServerSocket = new ServerSocket(PORT);
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
            if (MeinTollerServerSocket == null) {
                System.err.println("Unbekannter Fehler beim öffnen des Sockets aufgetreten. Breche ab...");
                System.exit(-1);
            }
        }

        //TODO Process HTTP service requests in an infinite loop.
        while (true) {
            //TODO Listen for a TCP connection request.
            Socket request = MeinTollerServerSocket.accept();


        }

    }
}

/**
 *
 */
final class HttpRequest implements Runnable
{

}