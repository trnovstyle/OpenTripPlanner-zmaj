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
otp.locale.Slovenian = {

    config :
    {
        //Name of a language written in a language itself (Used in Frontend to
        //choose a language)
        name: 'Slovensko',
        //FALSE-imperial units are used
        //TRUE-Metric units are used
        metric : true, 
        //Name of localization file (*.po file) in src/client/i18n
        locale_short : "sl",
        //Name of datepicker localization in
        //src/client/js/lib/jquery-ui/i18n (usually
        //same as locale_short)
        //this is index in $.datepicker.regional array
        //If file for your language doesn't exist download it from here
        //https://github.com/jquery/jquery-ui/tree/1-9-stable/ui/i18n
        //into src/client/js/lib/jquery-ui/i18n
        //and add it in index.html after other localizations
        //It will be used automatically when UI is switched to this locale
        datepicker_locale_short: "sl"
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
                title: 'O strani',
                content: '<p> Načrtovalnik poti temelji na konceptu &quot;ZMAJ &ndash; Zmogljive mestne avtobusne linije: povezan javni promet v Ljubljani&quot;, ki ga je spomladi 2022 pripravila delovna skupina v okviru Koalicije za trajnostno prometno politiko (KTPP).</p>

<p>Načrtovalnik omogoča preveritev potovalnih časov z javnim potniškim prometom v Ljubljani, kakršni bi bili možni ob uveljavitvi predvidenega koncepta. Koncept temelji na sistemu hitrih avtobusnih linij (BRT &ndash; Bus Rapid Transit). Njegove ključne prvine so:<br />
1 &ndash; Uvedba novih visoko zmogljivih linij LPP, poimenovanih ZMAJ A, B in C, z novimi avtobusi dolžine 24 m in visoko frekvenco, ki potekajo po glavnih mestnih vpadnicah<br />
2 &ndash; Ureditev prestopnih postaj LPP Bavarski dvor, Emonika in Kolodvor za vzpostavitev odlične navezanosti nove železniške in avtobusne postaje na LPP omrežje;<br />
3 &ndash; Uvedba več obodnih linij, ki omogočajo potovanja mimo mestnega središča.</p>

<p>Več informacij o konceptu je na voljo na <a href="https://ipop.si/2022/05/11/koncept-zmaj-za-povezan-javni-promet-v-ljubljani/" target="_blank">spletni strani</a>.</p>',
                //cssClass: 'otp-contactWidget',
            },
            {
                title: 'Kontakt',
                content: '<p>Koalicija za trajnostno prometno politiko (KTPP) je neformalno združenje organizacij in posameznikov, ki si prizadeva za trajnostno prometno politiko v Sloveniji in Evropi. Spremlja in se odziva na aktualne procese v slovenski in evropski prometni politiki in hkrati povečuje medsebojno informiranost o dogajanju na prometnem področju. Člani KTPP so predstavniki nevladnih organizacij, znanstvenih institucij, regionalnih razvojnih agencij, občin, izobraževalnih inštitucij in ostalih zainteresiranih posameznikov.</p>

<p>Delovanje KTPP podpirata dve vsebinski mreži: PlanB za Slovenijo, mreža nevladnih organizacij za trajnostni razvoj ter Mreža za prostor, mreža nevladnih organizacij in lokalnih pobud na področju urejanja prostora.</p>

<p>Dosegljivi smo na <a href="mailto:info@prometnapolitika.si?subject=koncept%20ZMAJ">info@prometnapolitika.si</a></p>

<p>Koncept ZMAJ je pripravila delovna skupina KTPP: Nejc Geržinič, Marko Peterlin, Špela Berlot Veselko in Nela Halilović.</p>

<p>Načrtovalnik poti je pripravil Simon Koblar. Izvorna koda je dostopna v <a href="https://github.com/trnovstyle/OpenTripPlanner-zmaj" target="_blank">GitHub repozitoriju</a>.</p>'
            },
    ],


    time:
    {
        format         : "D. MM. YYYY H:mm", //momentjs
        date_format    : "DD.MM.YYYY", //momentjs
        time_format    : "H:mm", //momentjs
        time_format_picker : "HH:mm", //http://trentrichardson.com/examples/timepicker/#tp-formatting
    },


    CLASS_NAME : "otp.locale.Slovenian"
};

