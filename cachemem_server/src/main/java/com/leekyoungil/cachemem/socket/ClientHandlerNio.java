package com.leekyoungil.cachemem.socket;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by leekyoungil (leekyoungil@gmail.com) on 3/31/15.
 * github : https://github.com/LeeKyoungIl/cachemem
 */
public class ClientHandlerNio extends Thread {
    private LinkedList<Thread> clientList = new LinkedList<>();
    private int socketNo = 0;

    public ClientHandlerNio (int socketNo) {
        this.socketNo = socketNo;
    }

    @Override
    public void run () {
        if (this.socketNo == 0) {
            return;
        }

        ServerSocketChannel server = null;
        Selector selector = null;

        try {
            server = ServerSocketChannel.open();
            selector = Selector.open();
            server.configureBlocking(false);
            server.socket().bind(new InetSocketAddress(this.socketNo));
            server.socket().setReceiveBufferSize(30 * 1024 * 1024);
            server.socket().setPerformancePreferences(0, 2, 1);
            server.register(selector, SelectionKey.OP_ACCEPT);

            System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@ make socket @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");

            while (!Thread.interrupted()) {
                selector.select();

                Iterator<SelectionKey> iter = selector.selectedKeys().iterator();

                while (iter.hasNext()) {
                    SelectionKey key = iter.next();
                    iter.remove();

                    if (key.isConnectable()) {
                        ((SocketChannel)key.channel()).finishConnect();
                    } else if (key.isAcceptable()) {
                        SocketChannel client = server.accept();

                        if (client != null) {
                            Thread cThread = new ClientHandlerThread(client);
                            cThread.start();

                            this.clientList.add(cThread);
                        }
                    }
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientList.stream().parallel().forEach(t -> {
                if (t != null && t.isAlive()) {
                    t.interrupt();

                    try {
                        t.join(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            if (server != null) {
                try {
                    System.out.println("@@@@@@@@@@@@@@@@@ server close @@@@@@@@@@@@@@@@");
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }
}
