package dtm.plugins.services;

import com.jediterm.terminal.TtyConnector;

import java.awt.*;
import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.util.concurrent.LinkedBlockingQueue;

public class OutputTtyConnector implements TtyConnector {
    private final LinkedBlockingQueue<char[]> queue = new LinkedBlockingQueue<>();
    private volatile boolean connected = true;
    private char[] current = null;
    private int currentPos = 0;

    public void feed(String text) {
        if (text == null || text.isEmpty() || !connected) return;
        queue.offer(text.toCharArray());
    }

    @Override
    public int read(char[] buf, int offset, int length) throws IOException {
        while (connected) {
            if (current != null && currentPos < current.length) {
                int available = current.length - currentPos;
                int count     = Math.min(available, length);
                System.arraycopy(current, currentPos, buf, offset, count);
                currentPos += count;
                if (currentPos >= current.length) {
                    current    = null;
                    currentPos = 0;
                }
                return count;
            }

            try {
                current    = queue.take();
                currentPos = 0;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return -1;
            }
        }
        return -1;
    }

    @Override
    public boolean ready() {
        return current != null && currentPos < current.length || !queue.isEmpty();
    }

    @Override public boolean isConnected()                        { return connected; }
    @Override public String  getName()                            { return "output"; }
    @Override public void    resize(Dimension ts, Dimension ps)   {}
    @Override public int     waitFor() throws InterruptedException { return 0; }
    @Override public void    write(byte[] bytes) throws IOException {}
    @Override public void    write(String s)    throws IOException {}

    @Override
    public void close() {
        connected = false;
        queue.offer(new char[0]);
    }
}
