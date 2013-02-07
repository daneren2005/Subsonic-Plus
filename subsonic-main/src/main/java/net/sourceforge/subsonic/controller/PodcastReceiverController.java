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
package net.sourceforge.subsonic.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.ParameterizableViewController;

import net.sourceforge.subsonic.domain.PodcastChannel;
import net.sourceforge.subsonic.domain.PodcastEpisode;
import net.sourceforge.subsonic.domain.User;
import net.sourceforge.subsonic.domain.UserSettings;
import net.sourceforge.subsonic.service.PodcastService;
import net.sourceforge.subsonic.service.SecurityService;
import net.sourceforge.subsonic.service.SettingsService;
import net.sourceforge.subsonic.util.StringUtil;

/**
 * Controller for the "Podcast receiver" page.
 *
 * @author Sindre Mehus
 */
public class PodcastReceiverController extends ParameterizableViewController {

    private PodcastService podcastService;
    private SecurityService securityService;
    private SettingsService settingsService;

    @Override
    protected ModelAndView handleRequestInternal(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Map<String, Object> map = new HashMap<String, Object>();
        ModelAndView result = super.handleRequestInternal(request, response);
        result.addObject("model", map);

        Map<PodcastChannel, List<PodcastEpisode>> channels = new LinkedHashMap<PodcastChannel, List<PodcastEpisode>>();
        for (PodcastChannel channel : podcastService.getAllChannels()) {
            channels.put(channel, podcastService.getEpisodes(channel.getId(), false));
        }

        User user = securityService.getCurrentUser(request);
        UserSettings userSettings = settingsService.getUserSettings(user.getUsername());

        map.put("user", user);
        map.put("partyMode", userSettings.isPartyModeEnabled());
        map.put("channels", channels);
        map.put("expandedChannels", StringUtil.parseInts(request.getParameter("expandedChannels")));

        Date trialExpires = settingsService.getTrialExpires();
        map.put("trialExpires", trialExpires);
        map.put("trialExpired", trialExpires != null && trialExpires.before(new Date()));
        map.put("trial", trialExpires != null && !settingsService.isLicenseValid());

        return result;
    }

    public void setPodcastService(PodcastService podcastService) {
        this.podcastService = podcastService;
    }

    public void setSecurityService(SecurityService securityService) {
        this.securityService = securityService;
    }

    public void setSettingsService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }
}
