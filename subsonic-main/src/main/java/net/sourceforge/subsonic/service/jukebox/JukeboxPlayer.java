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
    private Player player;
    private TransferStatus status;

    public void play(Player player, TransferStatus status) throws Exception {
        this.player = player;
        this.status = status;
        playNext();
    }

    public synchronized void reset() {
        if (audioPlayer != null) {
            audioPlayer.close();
        }
    }

    public synchronized float getGain() {
        return gain;
    }

    public synchronized void setGain(float gain) {
        this.gain = gain;
        if (audioPlayer != null) {
            audioPlayer.setGain(gain);
        }
    }

    public void stateChanged(AudioPlayer audioPlayer, AudioPlayer.State state) {
        if (player != null && state == EOM) {

            MusicFile currentFile = player.getPlaylist().getCurrentFile();
            if (player.getClientId() == null && currentFile != null) {  // Don't scrobble REST players.
                audioScrobblerService.register(currentFile, player.getUsername(), true);
            }

            player.getPlaylist().next();
            playNext();
        }
    }

    private synchronized void playNext()  {
        try {

            if (audioPlayer != null) {
                audioPlayer.close();
            } 

            MusicFile file = player.getPlaylist().getCurrentFile();
            if (file != null) {
                status.setFile(file.getFile());
                status.addBytesTransfered(file.length());

                TranscodingService.Parameters parameters = new TranscodingService.Parameters(file, null);
                // TODO
                parameters.setTranscoding(new Transcoding(null, null, null, null, "ffmpeg -i %s -v 0 -f au -", null, null));
                InputStream in = transcodingService.getTranscodedInputStream(parameters);
                audioPlayer = new AudioPlayer(in, this);
                audioPlayer.setGain(gain);
                audioPlayer.play();

                if (player.getClientId() == null) {  // Don't scrobble REST players.
                    audioScrobblerService.register(file, player.getUsername(), false);
                }
            }
        } catch (Exception x) {
            LOG.error("Error in jukebox: " + x, x);
        }
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }
}
