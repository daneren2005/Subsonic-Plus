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

import net.sourceforge.subsonic.Logger;
import net.sourceforge.subsonic.domain.Player;
import net.sourceforge.subsonic.domain.Playlist;
import net.sourceforge.subsonic.domain.TransferStatus;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.service.jukebox.JukeboxPlayer;

/**
 * Plays music on the local audio device.
 *
 * @author Sindre Mehus
 */
public class JukeboxService {

    private static final Logger LOG = Logger.getLogger(JukeboxService.class);

    private SecurityService securityService;
    private StatusService statusService;
    private TranscodingService transcodingService;
    private MusicInfoService musicInfoService;
    private SearchService searchService;
    private AudioScrobblerService audioScrobblerService;
    private JukeboxPlayer jukeboxPlayer;
    private TransferStatus status;

    /**
     * Start playing the playlist of the given player on the local audio device.
     *
     * @param player The player in question.
     */
    public synchronized void play(Player player) throws Exception {
        User user = securityService.getUserByName(player.getUsername());
        if (!user.isJukeboxRole()) {
            LOG.warn(user.getUsername() + " is not authorized for jukebox playback.");
            return;
        }

        stop();

        if (player.getPlaylist().getStatus() == Playlist.Status.PLAYING) {
            LOG.info("Starting jukebox player on behalf of " + player.getUsername());

            status = statusService.createStreamStatus(player);

            jukeboxPlayer.play(player, status);
        }
    }

    /**
     * Stop playing audio on the local device.
     */
    private synchronized void stop() {
        jukeboxPlayer.reset();
        if (status != null) {
            statusService.removeStreamStatus(status);
        }
    }

    public float getGain() {
        return jukeboxPlayer.getGain();
    }

    public void setGain(float gain) {
        jukeboxPlayer.setGain(gain);
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setStatusService(StatusService statusService) {
        this.statusService = statusService;
    }

    public void setTranscodingService(TranscodingService transcodingService) {
        this.transcodingService = transcodingService;
    }

    public void setMusicInfoService(MusicInfoService musicInfoService) {
        this.musicInfoService = musicInfoService;
    }

    public void setSearchService(SearchService searchService) {
        this.searchService = searchService;
    }

    public void setAudioScrobblerService(AudioScrobblerService audioScrobblerService) {
        this.audioScrobblerService = audioScrobblerService;
    }

    public void setJukeboxPlayer(JukeboxPlayer jukeboxPlayer) {
        this.jukeboxPlayer = jukeboxPlayer;
    }
}
