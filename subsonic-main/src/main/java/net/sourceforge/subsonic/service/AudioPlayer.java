/*
 This file is part of Subsonic.

 Subsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Subsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Subsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2009 (C) Sindre Mehus
 */
package net.sourceforge.subsonic.service;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.SourceDataLine;

import org.apache.commons.io.IOUtils;

import static net.sourceforge.subsonic.service.AudioPlayer.State.*;

/**
 * todo
 *
 * @author Sindre Mehus
 * @version $Id$
 */
public class AudioPlayer implements LineListener {

    private final InputStream in;
    private final Listener listener;
    private final SourceDataLine line;
    private final AtomicReference<State> state = new AtomicReference<State>(STOPPED);

    public AudioPlayer(InputStream in, Listener listener) throws Exception {
        this.in = new BufferedInputStream(in);
        this.listener = listener;

        AudioFormat format = AudioSystem.getAudioFileFormat(this.in).getFormat();
        line = AudioSystem.getSourceDataLine(format);
        line.addLineListener(this);
        line.open(format);
        System.out.println(format); // TODO

        new AudioDataWriter();
    }

    public synchronized void start() {
        if (state.get() == STOPPED) {
            line.start();
            setState(STARTED);
        }
    }

    public synchronized void stop() {
        if (state.get() == STARTED) {
            setState(STOPPED);
            line.stop();
            line.flush(); // TODO
        }
    }

    public synchronized void reset() {
        if (state.get() != COMPLETED) {
            setState(COMPLETED);

            // TODO: Catch exceptions
            line.stop();
            line.close();
            IOUtils.closeQuietly(in);
        }
    }

    private void setState(State state) {
        if (this.state.getAndSet(state) != state && listener != null) {
            listener.stateChanged(state);
        }
    }

    public void update(LineEvent event) {
        // TODO
        System.out.println(event);
    }

    private class AudioDataWriter implements Runnable {

        public AudioDataWriter() {
            new Thread(this).start();
        }

        public void run() {
            try {
                byte[] buffer = new byte[8192];

                while (true) {

                    switch (state.get()) {
                        case COMPLETED:
                            return;
                        case STOPPED:
                            System.out.println("sleep");
                            Thread.sleep(1000);
                            break;
                        case STARTED:
                            int n = in.read(buffer);
                            if (n == -1) {
                                return;
                            }
                            line.write(buffer, 0, n);
                            break;
                    }
                }
            } catch (Exception e) {
//                TODO
                e.printStackTrace();
            } finally {
                reset();
                System.out.println("Thread exiting.");
            }
        }
    }

    public interface Listener {
        void stateChanged(State state);
    }

    public static enum State {
        STOPPED,
        STARTED,
        COMPLETED
    }
}
