import java.net.*;
import java.util.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Client1 {
    public static void main(String[] args) throws IOException {


        Selector sel = Selector.open();

        SocketChannel sc = SocketChannel.open();
        sc.configureBlocking(false);

        // Мы заинтересованы в коннекте
        sc.register(sel, SelectionKey.OP_CONNECT);

        sc.connect(new InetSocketAddress("localhost", 8189));

        try {

            while (true) {

                System.out.println("1");
                sel.select();
                System.out.println("2");

                Iterator it = sel.selectedKeys().iterator();

                while (it.hasNext()) {

                    SelectionKey key = (SelectionKey) it.next();
                    it.remove();
                    sc = (SocketChannel) key.channel();

                    if (key.isConnectable()) {

                        System.out.println(" isConnected "+sc.isConnected());
                        // Продолжить попытки соединения, т.к. метод возвращает результат соединения мгновенно
                        // и не всегда сокет успевает законектится
                        sc.finishConnect();
                        System.out.println(" isConnected "+sc.isConnected());

                        // Когда подключимся, Мы захотим что то считать с сокета
                        // Для этого Мы указываем интересы на чтение
                        if(sc.isConnected()) key.interestOps(SelectionKey.OP_READ);
                    }

                    if (key.isReadable()) {

                        // Считываем данные с сокета
                        System.out.println(" isReadable ");
                        // Теперь Мы обработали считаемые данные и заинтересованы их записать в сокет обратно как ответ
                        key.attach(ByteBuffer.allocate(16)); // тут можно прикрепить то что Мы хотим записать (Данные ввиде ByteBuffer например)
                        // У Нас есть данные и теперь Мы заинтересованы на запись 
                        key.interestOps(SelectionKey.OP_WRITE);
                    }
                    if (key.isWritable()) {

                        // Тут что то пишем в сокет...
                        System.out.println(" isWritable ");
                        // Достаем данные и записываем в сокет...
                        ByteBuffer buff = (ByteBuffer) key.attachment();
                        // Закрываем сокет, Мы все сделали
                        sc.close();
                    }
                }

                // Притормозим поток чтобы проц не грузил в цикле
                try {
                    Thread.currentThread().sleep(200);
                } catch(Exception ex) {}

            }
        } finally {
            sc.close();
            sel.close();
        }
    }
}