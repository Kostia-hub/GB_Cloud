import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedDeque;

public class IOServer {

    private ConcurrentLinkedDeque<ClientHandler> queue;
    private boolean isRunning = true;

    public void stop() {
        isRunning = false;
    }

    public IOServer() {
        try {
            queue = new ConcurrentLinkedDeque<>();
            ServerSocket server = new ServerSocket(8189);
            System.out.println("Server started on 8189");
            while (isRunning) {
                Socket socket = server.accept();
                ClientHandler client = new ClientHandler(socket, this);
                queue.add(client);
                new Thread(client).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void kick(ClientHandler clientHandler) {
        queue.remove(clientHandler);
    }

    public static void main(String[] args) {
        new IOServer();
    }
}