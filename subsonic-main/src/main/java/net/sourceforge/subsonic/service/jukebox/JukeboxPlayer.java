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
package net.sourceforge.subsonic.service.jukebox;

import java.io.InputStream;

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.MusicFile;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Transcoding;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.service.AudioScrobblerService;
import net.sourceforge.subsonic.service.StatusService;
import net.sourceforge.subsonic.service.TranscodingService;

import static net.sourceforge.subsonic.service.jukebox.AudioPlayer.State.EOM;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JukeboxPlayer implements AudioPlayer.Listener {

    private static final Logger LOG = Logger.getLogger(TranscodingService.class);

    private float gain = 0.5f;

    private AudioPlayer audioPlayer;
    private TranscodingService transcodingService;
    private AudioScrobblerService audioScrobblerService;
    private StatusService statusService;

    private Player player;
    private TransferStatus status;
    private MusicFile currentPlayingFile;

    public synchronized void play(Player player) throws Exception {
        this.player = player;
        play(player.getPlaylist().getCurrentFile());
    }

    public synchronized void pause() {
        if (audioPlayer != null) {
            audioPlayer.pause();
        }
    }

    private synchronized void play(MusicFile file)  {
        try {

            // Resume if possible.
            boolean sameFile = file != null && file.equals(currentPlayingFile);
            boolean paused = audioPlayer != null && audioPlayer.getState() == AudioPlayer.State.PAUSED;
            if (sameFile && paused) {
                audioPlayer.play();
            }

            else {
                if (audioPlayer != null) {
                    audioPlayer.close();
                    if (currentPlayingFile != null) {
                        onSongEnd(currentPlayingFile);
                    }
                }

                if (file != null) {
                    TranscodingService.Parameters parameters = new TranscodingService.Parameters(file, null);
                    // TODO
                    parameters.setTranscoding(new Transcoding(null, null, null, null, "ffmpeg -i %s -v 0 -f au -", null, null));
                    InputStream in = transcodingService.getTranscodedInputStream(parameters);
                    audioPlayer = new AudioPlayer(in, this);
                    audioPlayer.setGain(gain);
                    audioPlayer.play();
                    onSongStart(file);
                }
            }

            currentPlayingFile = file;

        } catch (Exception x) {
            LOG.error("Error in jukebox: " + x, x);
        }
    }

    public synchronized void stateChanged(AudioPlayer audioPlayer, AudioPlayer.State state) {
        if (state == EOM) {
            player.getPlaylist().next();
            play(player.getPlaylist().getCurrentFile());
        }
    }

    public synchronized float getGain() {
        return gain;
    }

    public synchronized int getPosition() {
        return audioPlayer == null ? 0 : audioPlayer.getPosition();
    }

    private void onSongStart(MusicFile file) {
        status = statusService.createStreamStatus(player);
        status.setFile(file.getFile());
        status.addBytesTransfered(file.length());

        scrobble(file, false);
    }

    private void onSongEnd(MusicFile file) {
        if (status != null) {
            statusService.removeStreamStatus(status);
        }
        scrobble(file, true);
    }

    private void scrobble(MusicFile file, boolean submission) {
        if (player.getClientId() == null) {  // Don't scrobble REST players.
            audioScrobblerService.register(file, player.getUsername(), submission);
        }
    }

    public synchronized void setGain(float gain) {
        this.gain = gain;
        if (audioPlayer != null) {
            audioPlayer.setGain(gain);
        }
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }
}
