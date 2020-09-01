import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.Consumer;

public class NIOServer implements Runnable{

    // буффер для передачи сообщений в сеть и для чтения сообщений из сети
    private final ByteBuffer buffer = ByteBuffer.allocate(256);

    // считчик для ников
    private static int cnt = 0;

    @Override
    public void run() {
        try {
            // получаем инстанс объекта сервера
            ServerSocketChannel server = ServerSocketChannel.open();
            // запускаем сервер на порту 8189
            server.bind(new InetSocketAddress(8189));
            System.out.println("Server started on 8189");
            // получаем инстанс селектора
            Selector selector = Selector.open();
            // до регистрации событий на селекторе делаем неблокирующий режим (порядок важен)
            server.configureBlocking(false);
            // регистрация селектора, то есть селектор будет слушать порт
            // и принимать события типа соединение от клиентов
            server.register(selector, SelectionKey.OP_ACCEPT);

            while (server.isOpen()) {
                selector.select(); // блокирующая операция, получаем коллекцию событий (SelectionKeys)
                // формируем итератор
                Iterator<SelectionKey> keyIterator = selector.selectedKeys().iterator();
                while (keyIterator.hasNext()) {
                    // обработка события сразу
                    SelectionKey key = keyIterator.next();
                    if (key.isAcceptable()) {
                        // отловили соединение
                        cnt++;
                        // канал получаем из сервера путем вызова метода accept (так же как в IO)
                        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
                        // выключаем блокирующий режим
                        channel.configureBlocking(false);
                        // регистрируем селектор уже на события чтения, в аттач можно кинуть любой объект
                        channel.register(selector, SelectionKey.OP_READ, "user#" + cnt);
                        channel.register(selector, SelectionKey.OP_WRITE, "user#" + cnt);
                        System.out.println("Accepted connection: " + "user#" + cnt);
                    }
                    if (key.isReadable()) {
                        // отловили пачку байт (чтение)
                        System.out.println("Handled read operation");
                        // достали канал
                        SocketChannel channel = (SocketChannel) key.channel();
                        // будем пихать в него байты
                        StringBuilder message = new StringBuilder();
                        int read = 1;
                        while (true) {
                            // читем пачку байт
                            read = channel.read(buffer);
                            // если 0 то только подключился
                            if (read <= 0) {
                                // если -1 то отключился, нужно закрыть канал (иначе будут сыпаться -1)
                                if (read == -1) {
                                    channel.close();
                                    System.out.println("Client " + key.attachment() + " disconnected");
                                }
                                break;
                            }
                            // бефер в чтение
                            buffer.flip();
                            while (buffer.hasRemaining()) {
                                // читаем данные из буфера
                                message.append((char)buffer.get());
                            }
                            // буфер в запись
                            buffer.rewind();
                        }
                        // сообщение которое отдаем клиенту
                        String msg = key.attachment() + ": " + message.toString();
                        // идем по всем ключам на селекторе
                        for (SelectionKey selectionKey : selector.keys()) {
                            // могут иметься закрытые и испорченные каналы
                            // рассылаем только по валидным каналам с типом SocketChannel
                            if (key.isValid() && selectionKey.channel() instanceof SocketChannel) {
                                ((SocketChannel) selectionKey.channel())
                                        .write(ByteBuffer.wrap(msg.getBytes()));
                            }
                        }
                    }
                    // обязательно чистим отобранные ключи, иначе будут скапливаться
                    keyIterator.remove();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // запускаем в отдельном потоке так как имеется 1 блокирующая операция
        new Thread(new NIOServer()).start();
    }
}