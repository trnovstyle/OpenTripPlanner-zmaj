/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

otp.namespace("otp.locale");

/**
  * @class
  */
otp.locale.English = {

    config :
    {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'English',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true, 
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "en",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "" //Doesn't use localization

    },

    /**
     * Info Widgets: a list of the non-module-specific "information widgets"
     * that can be accessed from the top bar of the client display. Expressed as
     * an array of objects, where each object has the following fields:
     * - content: <string> the HTML content of the widget
     * - [title]: <string> the title of the widget
     * - [cssClass]: <string> the name of a CSS class to apply to the widget.
     * If not specified, the default styling is used.
     */
    infoWidgets : [
            {
                title: 'About ZMAJ',
                content: '<p>The route planner is based on the concept "ZMAJ - Efficient city bus lines: connected public transport in Ljubljana", which was prepared in the spring of 2022 by a working group within the Coalition for Sustainable Transport Policy (KTPP).</p><p>The planner makes it possible to check the travel times with public passenger transport in Ljubljana, which would be possible if the envisaged concept was implemented. The concept is based on a system of rapid bus lines (BRT - Bus Rapid Transit). Its key elements are:<br />1 &ndash; New high-capacity bus lines, named ZMAJ A, B and C, with new 24 m long buses and high frequency running along the main arterial roads;<br />2 &ndash; Transfer stations Bavarski dvor, Emonika and Kolodvor to establish an excellent connection of the new railway and bus station to the local bus network;<br />3 &ndash;  Several tangential routes that provide fast connections between suburbs.</p><p>More information about the concept is available <a href="https://ipop.si/2022/05/11/koncept-zmaj-za-povezan-javni-promet-v-ljubljani/" target="_blank">on the website</a>.</p>',
                //cssClass: 'otp-contactWidget',
            },
            {
                title: 'Contact',
                content: '<p>The Coalition for Sustainable Transport Policy (KTPP) is an informal association of organisations and individuals that strives for a sustainable transport policy in Slovenia and Europe. It monitors and responds to current processes in Slovenian and European transport policy and at the same time increases mutual understanding about what is happening in the transport field. KTPP members are representatives of non-governmental organisations, scientific institutions, regional development agencies, municipalities, educational institutions and other interested individuals.</p><p>The operation of KTPP is supported by two substantive networks: PlanB for Slovenia, a network of non-governmental organisations for sustainable development, and Network for Space, a network of non-governmental organisations and local initiatives in the field of spatial planning.</p><p>You can reach us at <a href="mailto:info@prometnapolitika.si?subject=koncept%20ZMAJ">info@prometnapolitika.si</a></p><p>The ZMAJ concept was prepared by the KTPP working group: Nejc Geržinič, Marko Peterlin, Špela Berlot Veselko and Nela Halilović.</p><p>The route planner was prepared by Simon Koblar. Source code is available in the <a href="https://github.com/trnovstyle/OpenTripPlanner-zmaj" target="_blank">GitHub repository</a>.</p>'
            },
    ],


    time:
    {
        format         : "MMM Do YYYY, h:mma", //moment.js
        date_format    : "MM/DD/YYYY", //momentjs must be same as date_picker format which is by default: mm/dd/yy
        time_format    : "h:mma", //momentjs
        time_format_picker : "hh:mmtt", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },

    CLASS_NAME : "otp.locale.English"
};

