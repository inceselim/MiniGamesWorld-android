package com.eomer91.minigamesworld.ui.games.football.engine

enum class Confed { UEFA, CONMEBOL, CONCACAF, AFC, CAF, OFC }

/**
 * Ulke tanimi.
 * @param code 3 harfli kod (oyun ici kimlik)
 * @param iso ISO-3166 alpha-2 (yerel dil ulke adi icin); Birlesik Krallik ulkeleri icin ozel ("XE","XS","XW","XI")
 * @param level futbol seviyesi 0..100 (lig kalitesi ve milli oyuncu yetenegi)
 * @param tiers lig kademelerindeki takim sayilari (bossa ulusal ligi yok)
 * @param cultures isim kulturleri ve agirliklari
 */
class CountryDef(
    val code: String,
    val iso: String,
    val englishName: String,
    val confed: Confed,
    val level: Int,
    val tiers: IntArray,
    val cultures: List<Pair<Culture, Float>>,
    val cities: List<String>,
)

object FbCountries {
    // KOD|ISO|Ingilizce ad|Konf|Seviye|Kademeler|Kultur:agirlik;...|Sehirler
    private const val DATA = """
ENG|XE|England|UEFA|92|20,24,24|BRITISH:1|London,Manchester,Liverpool,Birmingham,Leeds,Newcastle,Sheffield,Bristol,Nottingham,Leicester,Southampton,Brighton,Norwich,Ipswich,Derby,Coventry,Reading,Portsmouth,Plymouth,Hull,Stoke,Wolverhampton,Sunderland,Middlesbrough,Burnley,Blackburn,Bolton,Preston,Blackpool,Oxford,Cambridge,Luton,Watford,Bournemouth,Swindon,Exeter,Peterborough,Northampton,Wigan,Barnsley,Huddersfield,Bradford,Rotherham,Doncaster,York,Lincoln,Carlisle,Cheltenham,Gillingham,Charlton,Millwall,Fulham,Brentford,Crawley,Stevenage,Walsall,Shrewsbury,Colchester,Wycombe,Oldham
ESP|ES|Spain|UEFA|90|20,22|SPANISH:1|Madrid,Barcelona,Valencia,Sevilla,Bilbao,Málaga,Zaragoza,Alicante,Murcia,Valladolid,Vigo,Gijón,Oviedo,Santander,Pamplona,San Sebastián,Vitoria,Granada,Córdoba,Cádiz,Almería,Huelva,Elche,Castellón,Getafe,Leganés,Tenerife,Las Palmas,Palma,Girona,Tarragona,Burgos,Salamanca,León,A Coruña,Albacete,Huesca,Logroño,Mérida,Jaén
GER|DE|Germany|UEFA|88|18,18,20|GERMAN:1|Berlin,Hamburg,München,Köln,Frankfurt,Stuttgart,Düsseldorf,Dortmund,Essen,Leipzig,Bremen,Dresden,Hannover,Nürnberg,Duisburg,Bochum,Wuppertal,Bielefeld,Bonn,Münster,Karlsruhe,Mannheim,Augsburg,Wiesbaden,Gelsenkirchen,Mönchengladbach,Braunschweig,Kiel,Chemnitz,Aachen,Halle,Magdeburg,Freiburg,Krefeld,Lübeck,Rostock,Kassel,Mainz,Saarbrücken,Paderborn,Osnabrück,Darmstadt,Regensburg,Ingolstadt,Ulm,Heidelberg,Jena,Cottbus,Erfurt,Fürth
ITA|IT|Italy|UEFA|88|20,20|ITALIAN:1|Roma,Milano,Napoli,Torino,Palermo,Genova,Bologna,Firenze,Bari,Catania,Venezia,Verona,Messina,Padova,Trieste,Brescia,Parma,Taranto,Prato,Modena,Reggio,Perugia,Livorno,Cagliari,Foggia,Salerno,Ferrara,Rimini,Bergamo,Monza,Lecce,Pisa,Como,Cremona,Ancona,Pescara,Udine,Empoli,Frosinone,Spezia
FRA|FR|France|UEFA|85|18,18|FRENCH:1|Paris,Marseille,Lyon,Toulouse,Nice,Nantes,Strasbourg,Montpellier,Bordeaux,Lille,Rennes,Reims,Le Havre,Saint-Étienne,Toulon,Grenoble,Dijon,Angers,Nîmes,Clermont,Brest,Le Mans,Amiens,Tours,Limoges,Metz,Caen,Lorient,Auxerre,Bastia,Ajaccio,Sochaux,Troyes,Guingamp,Valenciennes,Niort
POR|PT|Portugal|UEFA|78|18,18|PORTUGUESE:1|Lisboa,Porto,Braga,Coimbra,Funchal,Setúbal,Guimarães,Aveiro,Faro,Viseu,Leiria,Évora,Chaves,Barcelos,Famalicão,Vila do Conde,Portimão,Estoril,Arouca,Moreira,Penafiel,Covilhã,Tondela,Mafra,Torres Novas
NED|NL|Netherlands|UEFA|78|18,20|DUTCH:1|Amsterdam,Rotterdam,Den Haag,Utrecht,Eindhoven,Groningen,Tilburg,Almere,Breda,Nijmegen,Enschede,Arnhem,Haarlem,Zwolle,Leeuwarden,Maastricht,Heerenveen,Alkmaar,Deventer,Venlo,Sittard,Emmen,Helmond,Dordrecht,Oss,Volendam,Den Bosch,Waalwijk,Kerkrade,Apeldoorn,Velsen,Doetinchem,Almelo,Zaandam,Leiden,Delft,Hengelo,Roosendaal
BEL|BE|Belgium|UEFA|74|16,16|DUTCH:0.55;FRENCH:0.45|Brussel,Antwerpen,Gent,Brugge,Liège,Charleroi,Leuven,Mechelen,Genk,Hasselt,Kortrijk,Oostende,Mons,Namur,Waregem,Sint-Truiden,Lommel,Beveren,Lokeren,Eupen,Seraing,Roeselare,Deinze,Tubize,Virton,Aalst
TUR|TR|Türkiye|UEFA|72|18,20|TURKISH:1|İstanbul,Ankara,İzmir,Bursa,Antalya,Trabzon,Konya,Adana,Kayseri,Gaziantep,Samsun,Eskişehir,Sivas,Denizli,Malatya,Rize,Manisa,Kocaeli,Sakarya,Hatay,Erzurum,Diyarbakır,Mersin,Bodrum,Bandırma,Giresun,Çorum,Ümraniye,Tuzla,Pendik,Alanya,Balıkesir,Tekirdağ,Kırklareli,Afyon,Isparta
SCO|XS|Scotland|UEFA|66|12,10|BRITISH:1|Glasgow,Edinburgh,Aberdeen,Dundee,Inverness,Perth,Kilmarnock,Motherwell,Paisley,Livingston,Ayr,Falkirk,Dunfermline,Greenock,Arbroath,Kirkcaldy,Hamilton,Dumfries,Stirling,Montrose,Airdrie,Cowdenbeath
AUT|AT|Austria|UEFA|67|12,16|GERMAN:1|Wien,Graz,Linz,Salzburg,Innsbruck,Klagenfurt,Villach,Wels,Sankt Pölten,Dornbirn,Wiener Neustadt,Steyr,Leoben,Hartberg,Ried,Altach,Wolfsberg,Lustenau,Horn,Amstetten,Kapfenberg,Bregenz,Mattersburg,Pasching,Lafnitz
SUI|CH|Switzerland|UEFA|66|12,10|GERMAN:0.6;FRENCH:0.3;ITALIAN:0.1|Zürich,Genève,Basel,Lausanne,Bern,Winterthur,Luzern,St. Gallen,Lugano,Biel,Thun,Aarau,Schaffhausen,Sion,Neuchâtel,Yverdon,Wil,Chiasso,Kriens,Bellinzona
CZE|CZ|Czechia|UEFA|64|16,16|CZECH:1|Praha,Brno,Ostrava,Plzeň,Liberec,Olomouc,České Budějovice,Hradec Králové,Pardubice,Zlín,Jablonec,Karviná,Teplice,Mladá Boleslav,Příbram,Opava,Jihlava,Sokolov,Vlašim,Chrudim,Prostějov,Trinec,Varnsdorf,Ústí,Kroměříž
GRE|GR|Greece|UEFA|64|14,16|GREEK:1|Athina,Thessaloniki,Patra,Irakleio,Larisa,Volos,Ioannina,Chania,Kalamata,Tripoli,Xanthi,Serres,Kavala,Lamia,Veria,Drama,Rodos,Karditsa,Trikala,Kozani,Levadeia,Agrinio,Korinthos,Kallithea,Peristeri,Piraeus,Chalkida,Katerini,Giannitsa
DEN|DK|Denmark|UEFA|64|12,12|NORDIC:1|København,Aarhus,Odense,Aalborg,Esbjerg,Randers,Kolding,Horsens,Vejle,Silkeborg,Herning,Viborg,Brøndby,Lyngby,Hobro,Fredericia,Næstved,Hillerød,Helsingør,Roskilde,Nykøbing,Vendsyssel,Hvidovre,Skive
NOR|NO|Norway|UEFA|62|16,16|NORDIC:1|Oslo,Bergen,Trondheim,Stavanger,Drammen,Fredrikstad,Kristiansand,Sandnes,Tromsø,Sarpsborg,Skien,Ålesund,Sandefjord,Haugesund,Tønsberg,Moss,Bodø,Arendal,Hamar,Larvik,Halden,Lillestrøm,Molde,Strømsgodset,Kongsvinger,Raufoss,Sogndal,Åsane,Mjøndalen
UKR|UA|Ukraine|UEFA|62|16,16|RUSSIAN:1|Kyiv,Kharkiv,Odesa,Dnipro,Donetsk,Zaporizhzhia,Lviv,Kryvyi Rih,Mykolaiv,Mariupol,Luhansk,Vinnytsia,Poltava,Chernihiv,Cherkasy,Sumy,Zhytomyr,Khmelnytskyi,Rivne,Ivano-Frankivsk,Ternopil,Lutsk,Uzhhorod,Kropyvnytskyi,Kremenchuk,Oleksandriia
SRB|RS|Serbia|UEFA|60|16,16|YUGO:1|Beograd,Novi Sad,Niš,Kragujevac,Subotica,Zrenjanin,Pančevo,Čačak,Kraljevo,Novi Pazar,Smederevo,Leskovac,Valjevo,Kruševac,Vranje,Šabac,Užice,Sombor,Bačka Topola,Lučani,Ivanjica,Surdulica,Inđija,Jagodina,Loznica,Zemun,Obrenovac,Bor,Zaječar,Paraćin,Kikinda,Vršac
CRO|HR|Croatia|UEFA|62|10,12|YUGO:1|Zagreb,Split,Rijeka,Osijek,Zadar,Pula,Slavonski Brod,Karlovac,Varaždin,Šibenik,Sisak,Velika Gorica,Vukovar,Dubrovnik,Koprivnica,Vinkovci,Dugopolje,Solin
POL|PL|Poland|UEFA|62|18,18|POLISH:1|Warszawa,Kraków,Łódź,Wrocław,Poznań,Gdańsk,Szczecin,Bydgoszcz,Lublin,Białystok,Katowice,Gdynia,Częstochowa,Radom,Toruń,Kielce,Rzeszów,Gliwice,Zabrze,Olsztyn,Bielsko-Biała,Bytom,Chorzów,Płock,Opole,Legnica,Mielec,Łęczna,Sosnowiec,Tychy,Głogów,Puławy,Niepołomice,Nieciecza,Grodzisk,Stalowa Wola
ISR|IL|Israel|UEFA|58|14,16|HEBREW:1|Tel Aviv,Jerusalem,Haifa,Beersheba,Netanya,Ashdod,Petah Tikva,Rishon,Ra'anana,Kfar Saba,Hadera,Nazareth,Sakhnin,Kiryat Shmona,Ashkelon,Herzliya,Ramat Gan,Holon,Bnei Yehuda,Nof HaGalil,Afula,Akko,Rehovot,Kfar Kasem,Umm al-Fahm,Tira,Ironi Tiberias,Modiin,Rosh HaAyin,Yokneam
SWE|SE|Sweden|UEFA|60|16,16|NORDIC:1|Stockholm,Göteborg,Malmö,Uppsala,Västerås,Örebro,Linköping,Helsingborg,Jönköping,Norrköping,Lund,Umeå,Gävle,Borås,Södertälje,Eskilstuna,Halmstad,Växjö,Karlstad,Sundsvall,Östersund,Trelleborg,Kalmar,Varberg,Degerfors,Mjällby,Värnamo
CYP|CY|Cyprus|UEFA|56|14,14|GREEK:1|Lefkosia,Lemesos,Larnaka,Pafos,Ammochostos,Paralimni,Achna,Kiti,Peyia,Deryneia,Xylotymvou,Aradippou,Ypsonas,Dali,Polemidia,Akritas,Ormideia,Kouklia,Pyla,Agia Napa,Episkopi,Chalkanoras
HUN|HU|Hungary|UEFA|56|12,16|HUNGARIAN:1|Budapest,Debrecen,Szeged,Miskolc,Pécs,Győr,Nyíregyháza,Kecskemét,Székesfehérvár,Szombathely,Szolnok,Tatabánya,Kaposvár,Érd,Veszprém,Békéscsaba,Zalaegerszeg,Sopron,Eger,Paks,Kisvárda,Mezőkövesd,Vác,Ajka,Siófok,Gyirmót,Csákvár,Kozármisleny
ROU|RO|Romania|UEFA|56|16,16|ROMANIAN:1|București,Cluj,Timișoara,Iași,Constanța,Craiova,Brașov,Galați,Ploiești,Oradea,Brăila,Arad,Pitești,Sibiu,Bacău,Târgu Mureș,Baia Mare,Buzău,Botoșani,Satu Mare,Suceava,Piatra Neamț,Drobeta,Târgu Jiu,Focșani,Mioveni,Voluntari,Slobozia
RUS|RU|Russia|UEFA|62|16,18|RUSSIAN:1|Moskva,Sankt-Peterburg,Novosibirsk,Yekaterinburg,Kazan,Nizhny Novgorod,Chelyabinsk,Samara,Omsk,Rostov,Ufa,Krasnoyarsk,Voronezh,Perm,Volgograd,Krasnodar,Saratov,Tula,Tolyatti,Izhevsk,Barnaul,Ulyanovsk,Vladivostok,Yaroslavl,Khabarovsk,Makhachkala,Orenburg,Kaliningrad,Sochi,Grozny,Tomsk,Kursk,Tambov,Ryazan
BUL|BG|Bulgaria|UEFA|52|16,16|BULGARIAN:1|Sofia,Plovdiv,Varna,Burgas,Ruse,Stara Zagora,Pleven,Sliven,Dobrich,Shumen,Pernik,Haskovo,Yambol,Pazardzhik,Blagoevgrad,Veliko Tarnovo,Vratsa,Gabrovo,Vidin,Montana,Kardzhali,Lovech,Razgrad,Targovishte,Botevgrad,Krumovgrad,Nesebar
SVK|SK|Slovakia|UEFA|52|12,16|CZECH:1|Bratislava,Košice,Prešov,Žilina,Nitra,Banská Bystrica,Trnava,Martin,Trenčín,Poprad,Prievidza,Zvolen,Považská Bystrica,Michalovce,Spišská Nová Ves,Komárno,Levice,Humenné,Ružomberok,Dunajská Streda,Skalica,Senica,Zlaté Moravce,Podbrezová,Pohronie,Šamorín,Dubnica,Liptovský Mikuláš
SVN|SI|Slovenia|UEFA|50|10,10|YUGO:1|Ljubljana,Maribor,Celje,Kranj,Koper,Velenje,Novo Mesto,Ptuj,Murska Sobota,Domžale,Nova Gorica,Kidričevo,Radomlje,Krško,Brežice,Rogaška,Grosuplje
AZE|AZ|Azerbaijan|UEFA|50|10,10|AZERI:1|Bakı,Gəncə,Sumqayıt,Mingəçevir,Lənkəran,Şəki,Şamaxı,Qəbələ,Ağdam,Naxçıvan,Şirvan,Zaqatala,Qusar,Masallı,Göyçay,Xaçmaz,İmişli,Saatlı,Səbail,Zirə
KAZ|KZ|Kazakhstan|UEFA|48|14,14|CENTRAL_ASIAN:0.6;RUSSIAN:0.4|Almaty,Astana,Shymkent,Karaganda,Aktobe,Taraz,Pavlodar,Oskemen,Semey,Atyrau,Kostanay,Kyzylorda,Oral,Petropavl,Aktau,Turkistan,Taldykorgan,Ekibastuz,Temirtau,Zhezkazgan,Kokshetau,Rudny,Balkhash,Zhanaozen,Kaskelen,Maktaaral
BLR|BY|Belarus|UEFA|48|16,16|RUSSIAN:1|Minsk,Gomel,Mogilev,Vitebsk,Grodno,Brest,Bobruisk,Baranovichi,Borisov,Pinsk,Orsha,Mozyr,Soligorsk,Novopolotsk,Lida,Molodechno,Polotsk,Zhlobin,Slutsk,Rechitsa,Zhodino,Smorgon,Slonim
MDA|MD|Moldova|UEFA|40|8,10|ROMANIAN:0.8;RUSSIAN:0.2|Chișinău,Tiraspol,Bălți,Bender,Rîbnița,Cahul,Ungheni,Soroca,Orhei,Dubăsari,Comrat,Ceadîr-Lunga,Strășeni,Căușeni,Drochia,Edineț,Hîncești,Ialoveni
IRL|IE|Ireland|UEFA|48|10,10|BRITISH:1|Dublin,Cork,Limerick,Galway,Waterford,Drogheda,Dundalk,Sligo,Bray,Athlone,Longford,Wexford,Kerry,Cobh
ISL|IS|Iceland|UEFA|44|12,12|NORDIC:1|Reykjavík,Kópavogur,Hafnarfjörður,Akureyri,Garðabær,Mosfellsbær,Akranes,Selfoss,Keflavík,Vestmannaeyjar,Grindavík,Njarðvík,Ísafjörður
FIN|FI|Finland|UEFA|46|12,10|NORDIC:1|Helsinki,Espoo,Tampere,Vantaa,Oulu,Turku,Jyväskylä,Lahti,Kuopio,Pori,Kouvola,Joensuu,Lappeenranta,Hämeenlinna,Vaasa,Seinäjoki,Rovaniemi,Mariehamn,Kotka,Kokkola,Jakobstad,Ekenäs
BIH|BA|Bosnia and Herzegovina|UEFA|46|12,16|YUGO:1|Sarajevo,Banja Luka,Tuzla,Zenica,Mostar,Bijeljina,Brčko,Prijedor,Trebinje,Doboj,Cazin,Bihać,Široki Brijeg,Goražde,Travnik,Gradiška,Konjic,Zvornik,Posušje,Velež,Igman,Sloboda,Rudar,Leotar,Kozara,Orašje,Čelik,Željezničar
ARM|AM|Armenia|UEFA|42|10,10|CAUCASUS:1|Yerevan,Gyumri,Vanadzor,Vagharshapat,Abovyan,Kapan,Hrazdan,Armavir,Gavar,Goris,Artashat,Ararat,Sevan,Ijevan,Charentsavan,Ashtarak
LVA|LV|Latvia|UEFA|42|10,10|BALTIC:0.7;RUSSIAN:0.3|Rīga,Daugavpils,Liepāja,Jelgava,Jūrmala,Ventspils,Rēzekne,Valmiera,Jēkabpils,Ogre,Tukums,Salaspils,Cēsis,Kuldīga,Olaine,Grobiņa,Smiltene,Saldus
LTU|LT|Lithuania|UEFA|40|10,10|BALTIC:1|Vilnius,Kaunas,Klaipėda,Šiauliai,Panevėžys,Alytus,Marijampolė,Mažeikiai,Jonava,Utena,Kėdainiai,Telšiai,Tauragė,Ukmergė,Visaginas,Palanga,Gargždai,Kretinga,Plungė,Šilutė
EST|EE|Estonia|UEFA|38|10,10|BALTIC:0.7;RUSSIAN:0.3|Tallinn,Tartu,Narva,Pärnu,Kohtla-Järve,Viljandi,Maardu,Rakvere,Kuressaare,Sillamäe,Valga,Võru,Jõhvi,Haapsalu,Keila,Paide,Nõmme,Elva
GEO|GE|Georgia|UEFA|46|10,10|CAUCASUS:1|Tbilisi,Batumi,Kutaisi,Rustavi,Zugdidi,Gori,Poti,Kobuleti,Khashuri,Samtredia,Senaki,Zestaponi,Marneuli,Telavi,Akhaltsikhe,Ozurgeti,Kaspi,Chiatura,Tkibuli,Sachkhere
ALB|AL|Albania|UEFA|42|10,10|ALBANIAN:1|Tirana,Durrës,Vlorë,Elbasan,Shkodër,Fier,Korçë,Berat,Lushnjë,Kavajë,Pogradec,Laç,Kukës,Gjirokastër,Patos,Krujë,Kuçovë,Sarandë,Lezhë,Peshkopi
MKD|MK|North Macedonia|UEFA|40|12,10|YUGO:0.7;ALBANIAN:0.3|Skopje,Bitola,Kumanovo,Prilep,Tetovo,Veles,Štip,Ohrid,Gostivar,Strumica,Kavadarci,Kočani,Kičevo,Struga,Radoviš,Gevgelija,Debar,Kriva Palanka,Sveti Nikole,Negotino,Delčevo,Probištip
XKX|XK|Kosovo|UEFA|40|10,10|ALBANIAN:1|Prishtinë,Prizren,Ferizaj,Pejë,Gjakovë,Gjilan,Mitrovicë,Podujevë,Vushtrri,Suharekë,Rahovec,Drenas,Lipjan,Malishevë,Kamenicë,Viti,Deçan,Istog,Klinë,Skenderaj
MNE|ME|Montenegro|UEFA|40|10,10|YUGO:1|Podgorica,Nikšić,Herceg Novi,Pljevlja,Bijelo Polje,Cetinje,Bar,Berane,Budva,Ulcinj,Tivat,Rožaje,Kotor,Danilovgrad,Mojkovac,Plav,Kolašin,Andrijevica,Petnjica,Tuzi
LUX|LU|Luxembourg|UEFA|34|16|FRENCH:0.6;GERMAN:0.4|Luxembourg,Esch-sur-Alzette,Differdange,Dudelange,Ettelbruck,Diekirch,Wiltz,Rumelange,Bettembourg,Strassen,Mondorf,Hesperange,Pétange,Niederkorn,Mamer,Mersch,Hostert,Wormeldange
NIR|XI|Northern Ireland|UEFA|40|12,12|BRITISH:1|Belfast,Derry,Lisburn,Newry,Bangor,Craigavon,Ballymena,Newtownabbey,Carrickfergus,Coleraine,Omagh,Larne,Dungannon,Portadown,Enniskillen,Banbridge,Ballymoney
WAL|XW|Wales|UEFA|38|12,12|BRITISH:1|Cardiff,Swansea,Newport,Wrexham,Barry,Neath,Bangor,Rhyl,Aberystwyth,Caernarfon,Llanelli,Bridgend,Merthyr,Pontypridd,Haverfordwest,Connah's Quay,Bala,Newtown,Flint,Colwyn Bay,Briton Ferry
MLT|MT|Malta|UEFA|34|14|ITALIAN:0.5;BRITISH:0.5|Valletta,Birkirkara,Mosta,Qormi,Żabbar,Sliema,Naxxar,Żejtun,Marsaskala,Fgura,Rabat,Ħamrun,Gżira,Floriana,Balzan,Pietà,Mellieħa,Marsa,Tarxien,Senglea
FRO|FO|Faroe Islands|UEFA|30|10|NORDIC:1|Tórshavn,Klaksvík,Runavík,Tvøroyri,Fuglafjørður,Vágur,Sandavágur,Sørvágur,Eiði,Gøta,Toftir,Skála,Hoyvík,Argir,Vestmanna,Miðvágur
AND|AD|Andorra|UEFA|24|10|SPANISH:1|Andorra la Vella,Escaldes,Encamp,Sant Julià,La Massana,Ordino,Canillo,Pas de la Casa,Santa Coloma,Engordany,Arinsal,Soldeu
GIB|GI|Gibraltar|UEFA|22|10|BRITISH:0.7;SPANISH:0.3|Gibraltar,Catalan Bay,Westside,South District,Upper Town,Europa Point,Sandpits,Rosia,Camp Bay,Glacis Estate
SMR|SM|San Marino|UEFA|18|12|ITALIAN:1|San Marino,Serravalle,Borgo Maggiore,Domagnano,Fiorentino,Acquaviva,Faetano,Chiesanuova,Montegiardino,Murata,Dogana,Cailungo
LIE|LI|Liechtenstein|UEFA|26||GERMAN:1|Vaduz
ARG|AR|Argentina|CONMEBOL|88||LATIN:1|
BRA|BR|Brazil|CONMEBOL|88||BRAZILIAN:1|
URU|UY|Uruguay|CONMEBOL|76||LATIN:1|
COL|CO|Colombia|CONMEBOL|74||LATIN:1|
CHI|CL|Chile|CONMEBOL|66||LATIN:1|
ECU|EC|Ecuador|CONMEBOL|66||LATIN:1|
PER|PE|Peru|CONMEBOL|60||LATIN:1|
PAR|PY|Paraguay|CONMEBOL|62||LATIN:1|
VEN|VE|Venezuela|CONMEBOL|58||LATIN:1|
BOL|BO|Bolivia|CONMEBOL|48||LATIN:1|
USA|US|United States|CONCACAF|70||ANGLO:0.8;LATIN:0.2|
MEX|MX|Mexico|CONCACAF|72||LATIN:1|
CAN|CA|Canada|CONCACAF|64||ANGLO:0.8;FRENCH:0.2|
CRC|CR|Costa Rica|CONCACAF|56||LATIN:1|
JAM|JM|Jamaica|CONCACAF|54||ANGLO:1|
PAN|PA|Panama|CONCACAF|54||LATIN:1|
HON|HN|Honduras|CONCACAF|50||LATIN:1|
JPN|JP|Japan|AFC|74||JAPANESE:1|
KOR|KR|South Korea|AFC|72||KOREAN:1|
AUS|AU|Australia|AFC|66||ANGLO:1|
IRN|IR|Iran|AFC|66||PERSIAN:1|
KSA|SA|Saudi Arabia|AFC|60||ARABIC:1|
QAT|QA|Qatar|AFC|56||ARABIC:1|
UAE|AE|United Arab Emirates|AFC|52||ARABIC:1|
IRQ|IQ|Iraq|AFC|54||ARABIC:1|
UZB|UZ|Uzbekistan|AFC|56||CENTRAL_ASIAN:1|
CHN|CN|China|AFC|50||CHINESE:1|
JOR|JO|Jordan|AFC|52||ARABIC:1|
OMA|OM|Oman|AFC|48||ARABIC:1|
MAR|MA|Morocco|CAF|78||ARABIC:1|
SEN|SN|Senegal|CAF|72||AFRICAN:1|
NGA|NG|Nigeria|CAF|70||AFRICAN:1|
EGY|EG|Egypt|CAF|66||ARABIC:1|
CIV|CI|Ivory Coast|CAF|68||AFRICAN:1|
GHA|GH|Ghana|CAF|64||AFRICAN:1|
CMR|CM|Cameroon|CAF|64||AFRICAN:1|
ALG|DZ|Algeria|CAF|66||ARABIC:1|
TUN|TN|Tunisia|CAF|60||ARABIC:1|
MLI|ML|Mali|CAF|62||AFRICAN:1|
RSA|ZA|South Africa|CAF|56||AFRICAN:1|
COD|CD|DR Congo|CAF|60||AFRICAN:1|
NZL|NZ|New Zealand|OFC|48||ANGLO:1|
"""

    val all: List<CountryDef> by lazy {
        DATA.trim().lines().map { line ->
            val f = line.split('|')
            CountryDef(
                code = f[0], iso = f[1], englishName = f[2], confed = Confed.valueOf(f[3]), level = f[4].toInt(),
                tiers = if (f[5].isBlank()) IntArray(0) else f[5].split(',').map { it.toInt() }.toIntArray(),
                cultures = f[6].split(';').map { c -> c.split(':').let { Culture.valueOf(it[0]) to it[1].toFloat() } },
                cities = if (f.size > 7 && f[7].isNotBlank()) f[7].split(',') else emptyList(),
            )
        }
    }

    val uefa: List<CountryDef> by lazy { all.filter { it.confed == Confed.UEFA } }
    fun byCode(code: String) = all.first { it.code == code }
}
