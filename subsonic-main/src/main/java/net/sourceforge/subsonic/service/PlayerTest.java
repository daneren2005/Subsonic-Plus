package net.sourceforge.subsonic.service;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class PlayerTest implements AudioPlayer.Listener {

    private AudioPlayer player;

    public PlayerTest() throws Exception {

        player = new AudioPlayer(new FileInputStream("d:\\music\\test\\wav\\foo.wav"), this);
//        player = new AudioPlayer(new FileInputStream("c:\\progs\\JavaSoundDemo\\audio\\1-welcome.wav"), this);
//        line.start();
//
//        Thread.sleep(Long.MAX_VALUE);
//
        createGUI();

    }

    private void createGUI() {
        JFrame frame = new JFrame();

        JButton startButton = new JButton("Start");
        JButton stopButton = new JButton("Stop");
        JButton resetButton = new JButton("Reset");
        final JSlider gainSlider = new JSlider(0, 1000);

        startButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.start();
            }
        });
        stopButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.stop();
            }
        });
        resetButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                player.reset();
            }
        });
        gainSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                float gain = (float) gainSlider.getValue() / 1000.0F;
                player.setGain(gain);
            }
        });

        frame.setLayout(new FlowLayout());
        frame.add(startButton);
        frame.add(stopButton);
        frame.add(resetButton);
        frame.add(gainSlider);

        frame.pack();
        frame.setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        new PlayerTest();
    }

    public void stateChanged(AudioPlayer.State state) {
        System.out.println(state);
    }


    private static class MonitoredInputStream extends InputStream {
        private final InputStream in;

        public MonitoredInputStream(InputStream in) {
            this.in = in;
        }

        @Override
        public int read() throws IOException {
            System.out.println("read");
            return in.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            int n = in.read(b);
//            System.out.println("read " + n);
            return n;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int n = in.read(b, off, len);
//            System.out.println("read " + n);
            return n;
        }

        @Override
        public long skip(long n) throws IOException {
            System.out.println("skip");
            return in.skip(n);
        }

        @Override
        public int available() throws IOException {
//            System.out.println("available");
            return in.available();
        }

        @Override
        public void close() throws IOException {
            System.out.println("close");
            in.close();
        }

        @Override
        public void mark(int readlimit) {
            System.out.println("mark");
            in.mark(readlimit);
        }

        @Override
        public void reset() throws IOException {
            System.out.println("reset");
            in.reset();
        }

        @Override
        public boolean markSupported() {
            System.out.println("markSupported");
            return in.markSupported();
        }
    }


}

