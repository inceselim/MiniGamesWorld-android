package com.eomer91.minigamesworld.ui.games.football.engine

/**
 * Isim kulturleri: kurgusal oyuncu/personel isimleri ve kulup adi kaliplari.
 * Isimler yaygin ad/soyadlarin rastgele birlesimidir; gercek kisileri temsil etmez.
 */
enum class Culture(first: String, last: String, clubs: String = "FC {c},{c} FC,{c} Sport,Atletik {c}") {
    TURKISH(
        "Ahmet,Mehmet,Mustafa,Emre,Burak,Can,Kerem,Arda,Efe,Hakan,Oğuz,Serkan,Volkan,Kaan,Berk,Yusuf,Ömer,Cenk,Alper,Onur,Tolga,Barış,Furkan,Eren,Selim,Umut,Kenan,Ozan,Doğukan,Batuhan,Mert,Ufuk,Salih,İrfan,Taylan,Enes,Yunus,Halil",
        "Yılmaz,Kaya,Demir,Şahin,Çelik,Yıldız,Yıldırım,Öztürk,Aydın,Özdemir,Arslan,Doğan,Kılıç,Aslan,Çetin,Kara,Koç,Kurt,Özkan,Şimşek,Polat,Korkmaz,Güneş,Aktaş,Erdem,Bulut,Keskin,Tekin,Ünal,Sarı,Taş,Akın,Bozkurt,Turan,Uçar,Gök,Karaca,Toprak,Bayram,Akbaş,Ateş,Coşkun",
        "{c}spor,{c} Gücü,{c} Aslanları,{c} Kartalları,{c} Yıldızları,{c} Fırtınası,{c} İdman Yurdu,{c} Kaplanları,{c} Şimşekleri,{c} Gençlik,{c} Belediyespor,{c} Ateşi",
    ),
    AZERI(
        "Elvin,Ramil,Tural,Rashad,Anar,Emin,Orkhan,Mahir,Javid,Kamran,Nijat,Ilkin,Rauf,Vugar,Farid,Agil,Samir,Shahin,Elnur,Murad",
        "Aliyev,Mammadov,Huseynov,Hasanov,Guliyev,Ismayilov,Abbasov,Rzayev,Babayev,Jafarov,Karimov,Nasirov,Valiyev,Sultanov,Ibrahimov,Ahmadov",
        "Neftchi {c},{c} FK,Turan {c},Shamakhi {c},{c} PFK,Kapaz {c},Araz {c},Sumgayit {c}",
    ),
    BRITISH(
        "James,Jack,Harry,Oliver,George,Thomas,William,Charlie,Ben,Sam,Joe,Luke,Ryan,Jamie,Callum,Liam,Connor,Lewis,Kieran,Daniel,Adam,Josh,Tom,Alfie,Owen,Ethan,Max,Nathan,Aaron,Rhys,Declan,Ross,Euan,Scott,Gareth,Conor,Sean,Dylan",
        "Smith,Jones,Taylor,Brown,Williams,Wilson,Johnson,Davies,Robinson,Wright,Thompson,Evans,Walker,White,Roberts,Green,Hall,Wood,Jackson,Clarke,Hughes,Edwards,Turner,Harris,Cooper,Ward,Morris,Moore,King,Baker,Carter,Mitchell,Parker,Bennett,Price,Shaw,Murphy,Kelly,Campbell,Stewart,McDonald,Reid,Fraser,O'Brien,Byrne,Doyle,Lloyd,Griffiths",
        "{c} Rovers,{c} Wanderers,{c} Athletic,{c} Albion,{c} Town,{c} Harriers,{c} Lions,{c} Royals,{c} Swifts,{c} Borough,{c} Hawks,{c} Victoria",
    ),
    ANGLO(
        "Tyler,Brandon,Jordan,Christian,Cole,Mason,Jackson,Logan,Caleb,Hunter,Wyatt,Jalen,Marcus,Andre,Ryan,Ben,Jack,Liam,Noah,Ethan,Lucas,Kyle,Jamal,Riley,Cameron",
        "Johnson,Brown,Davis,Miller,Anderson,Martin,Lewis,Lee,Walker,Young,Allen,Scott,Adams,Nelson,Hill,Morgan,Smith,Taylor,Campbell,Thompson,Wilson,Clark,Wright,Bailey,Reid,Grant,Powell,Gordon",
        "{c} FC,{c} United",
    ),
    SPANISH(
        "Alejandro,Pablo,Javier,Sergio,David,Daniel,Adrián,Álvaro,Diego,Carlos,Iván,Rubén,Raúl,Hugo,Mario,Marcos,Jorge,Víctor,Íker,Unai,Gonzalo,Rodrigo,Aitor,Óscar,Nacho,Dani,Borja,Marc,Pau,Jordi,Xavi,Eneko",
        "García,Fernández,González,Rodríguez,López,Martínez,Sánchez,Pérez,Gómez,Martín,Jiménez,Ruiz,Hernández,Díaz,Moreno,Muñoz,Álvarez,Romero,Alonso,Gutiérrez,Navarro,Torres,Domínguez,Vázquez,Ramos,Gil,Serrano,Molina,Blanco,Suárez,Castro,Ortega,Delgado,Rubio,Medina,Iglesias,Etxeberria,Puig",
        "Real {c},Atlético {c},CD {c},Deportivo {c},Unión {c},Racing {c},{c} CF,Sporting {c},Club {c},Juventud {c},{c} Balompié,Gimnástica {c}",
    ),
    LATIN(
        "Juan,Carlos,Luis,José,Matías,Santiago,Nicolás,Facundo,Agustín,Lautaro,Sebastián,Joaquín,Rodrigo,Emiliano,Franco,Thiago,Gonzalo,Cristian,Andrés,Felipe,Kevin,Brayan,Jhon,Fabián,Germán,Ignacio,Mauricio,Héctor,Raúl,Jorge",
        "González,Rodríguez,Gómez,Fernández,López,Díaz,Martínez,Pérez,Sánchez,Romero,Sosa,Torres,Álvarez,Ruiz,Ramírez,Flores,Benítez,Acosta,Medina,Herrera,Suárez,Aguirre,Giménez,Gutiérrez,Pereyra,Rojas,Molina,Castro,Ortiz,Silva,Núñez,Cabrera,Valencia,Mosquera,Arango,Hernández,Vargas,Cárdenas,Paredes,Quintero",
    ),
    PORTUGUESE(
        "João,Pedro,Tiago,Diogo,Rúben,Bruno,Ricardo,André,Gonçalo,Rafael,Nuno,Rui,Miguel,Francisco,Bernardo,Duarte,Fábio,Hugo,Daniel,Luís,Sérgio,Tomás,Afonso,Martim,Vasco,Henrique",
        "Silva,Santos,Ferreira,Pereira,Oliveira,Costa,Rodrigues,Martins,Jesus,Sousa,Fernandes,Gonçalves,Gomes,Lopes,Marques,Alves,Almeida,Ribeiro,Pinto,Carvalho,Teixeira,Moreira,Correia,Mendes,Nunes,Soares,Vieira,Monteiro,Cardoso,Rocha",
        "Sporting {c},{c} FC,Desportivo {c},Académico {c},Atlético {c},União {c},Vitória {c},Clube {c},Leões {c},{c} Futebol Clube",
    ),
    BRAZILIAN(
        "Lucas,Gabriel,Matheus,Rafael,Felipe,Bruno,Gustavo,Thiago,Vinícius,Rodrigo,Leonardo,Diego,Douglas,Marcelo,Júnior,Wesley,Willian,Everton,Anderson,Fabrício,Caio,Igor,Renan,Danilo,Paulo,Murilo,Arthur,Breno",
        "Silva,Santos,Oliveira,Souza,Lima,Pereira,Costa,Ferreira,Alves,Rocha,Ribeiro,Carvalho,Gomes,Martins,Araújo,Barbosa,Melo,Cardoso,Teixeira,Nascimento,Moreira,Andrade,Freitas,Cavalcanti,Monteiro,Batista,Dias,Moura,Campos,Pinheiro",
    ),
    ITALIAN(
        "Marco,Luca,Andrea,Francesco,Alessandro,Matteo,Lorenzo,Davide,Simone,Federico,Riccardo,Gianluca,Stefano,Fabio,Nicolò,Daniele,Antonio,Giuseppe,Paolo,Roberto,Emanuele,Filippo,Tommaso,Mattia,Gabriele,Pietro,Salvatore,Leonardo",
        "Rossi,Russo,Ferrari,Esposito,Bianchi,Romano,Colombo,Ricci,Marino,Greco,Bruno,Gallo,Conti,De Luca,Costa,Giordano,Mancini,Rizzo,Lombardi,Moretti,Barbieri,Fontana,Santoro,Mariani,Rinaldi,Caruso,Ferrara,Galli,Martini,Leone,Longo,Gentile,Martinelli,Vitale,Serra,Coppola,De Santis",
        "AC {c},{c} Calcio,US {c},Sporting {c},Virtus {c},Pro {c},Unione {c},Atletico {c},Real {c},Polisportiva {c},Audace {c},Juventina {c}",
    ),
    FRENCH(
        "Lucas,Hugo,Théo,Nathan,Louis,Enzo,Mathis,Tom,Clément,Maxime,Antoine,Alexandre,Julien,Romain,Yanis,Adrien,Florian,Benjamin,Paul,Bastien,Quentin,Thomas,Nicolas,Jordan,Kévin,Mathieu,Loïc,Rayan,Ibrahima,Moussa,Wesley,Jérémy",
        "Martin,Bernard,Dubois,Thomas,Robert,Richard,Petit,Durand,Leroy,Moreau,Simon,Laurent,Lefebvre,Michel,Garcia,David,Bertrand,Roux,Vincent,Fournier,Morel,Girard,André,Mercier,Dupont,Lambert,Bonnet,François,Martinez,Legrand,Garnier,Faure,Rousseau,Blanc,Guerin,Muller,Henry,Roussel,Diallo,Traoré,Camara,Koné",
        "FC {c},Olympique {c},Stade {c},AS {c},Racing {c},US {c},Sporting {c},{c} FC,Union {c},Étoile {c},Red Star {c},Gazélec {c}",
    ),
    GERMAN(
        "Lukas,Leon,Jonas,Finn,Paul,Felix,Maximilian,Elias,Niklas,Tim,Jan,Julian,Moritz,Florian,Tobias,Kevin,Marcel,Dennis,Sebastian,Kai,Timo,Philipp,Benedikt,Lars,Robin,Nico,Jannik,Marvin,Fabian,Stefan",
        "Müller,Schmidt,Schneider,Fischer,Weber,Meyer,Wagner,Becker,Schulz,Hoffmann,Schäfer,Koch,Bauer,Richter,Klein,Wolf,Schröder,Neumann,Schwarz,Zimmermann,Braun,Krüger,Hofmann,Hartmann,Lange,Schmitt,Werner,Krause,Meier,Lehmann,Huber,Kaiser,Fuchs,Peters,Lang,Scholz,Möller,Weiß,Jung,Hahn,Gruber,Steiner",
        "FC {c},SV {c},TSV {c},VfB {c},Sportfreunde {c},Eintracht {c},{c} 04,Union {c},SC {c},Fortuna {c},Viktoria {c},1. FC {c},VfL {c},Rot-Weiß {c}",
    ),
    DUTCH(
        "Daan,Sem,Lucas,Milan,Levi,Luuk,Bram,Jesse,Thijs,Stijn,Ruben,Tim,Lars,Niels,Bas,Jeroen,Kevin,Joris,Wout,Sven,Jasper,Mats,Teun,Dirk,Jens,Arne,Pieter,Koen",
        "de Jong,de Vries,van den Berg,van Dijk,Bakker,Janssen,Visser,Smit,Meijer,de Boer,Mulder,de Groot,Bos,Vos,Peters,Hendriks,van Leeuwen,Dekker,Brouwer,de Wit,Dijkstra,Smits,de Graaf,van der Meer,Kok,Jacobs,Vermeulen,Peeters,Maes,Claes,Wouters,Mertens,Willems,Goossens,De Smet",
        "FC {c},SV {c},{c} Boys,Sparta {c},VV {c},Vitesse {c},Go Ahead {c},Quick {c},Excelsior {c},Racing {c},KV {c},Standaard {c}",
    ),
    NORDIC(
        "Erik,Lars,Anders,Magnus,Oskar,Emil,Jonas,Mikkel,Kasper,Rasmus,Mathias,Sander,Henrik,Kristian,Andreas,Fredrik,Johan,Viktor,Gustav,Isak,Eirik,Ole,Nils,Axel,Sigurd,Aron,Teemu,Joel,Elias",
        "Hansen,Johansen,Olsen,Larsen,Andersen,Pedersen,Nielsen,Jensen,Kristiansen,Karlsen,Johansson,Andersson,Karlsson,Nilsson,Eriksson,Larsson,Olsson,Persson,Svensson,Lindberg,Berg,Dahl,Lund,Holm,Strand,Haugen,Bakke,Nyström,Sigurdsson,Gunnarsson,Virtanen,Korhonen,Mäkinen,Nieminen,Hämäläinen,Poulsen",
        "{c} IF,{c} FF,{c} BK,IFK {c},{c} FK,IL {c},{c} SK,{c} United,{c} Boldklub,{c} JK",
    ),
    RUSSIAN(
        "Aleksandr,Dmitri,Sergei,Andrei,Ivan,Maksim,Nikita,Artem,Mikhail,Denis,Kirill,Pavel,Roman,Vladimir,Igor,Yuri,Anton,Oleg,Ruslan,Vitaliy,Daniil,Fyodor,Konstantin,Bogdan,Taras,Oleksandr,Mykola,Andriy,Serhiy,Yevhen",
        "Ivanov,Smirnov,Kuznetsov,Popov,Volkov,Sokolov,Lebedev,Kozlov,Novikov,Morozov,Petrov,Fedorov,Mikhailov,Orlov,Zaitsev,Pavlov,Semyonov,Golubev,Vinogradov,Bogdanov,Kovalenko,Bondarenko,Tkachenko,Melnyk,Kravchenko,Boyko,Lysenko,Marchenko,Rudenko,Moroz,Savchenko,Kovalev",
        "Dinamo {c},Spartak {c},Lokomotiv {c},Torpedo {c},Rotor {c},FC {c},Shakhtar {c},Metalist {c},Sokol {c},Avangard {c},Krylya {c},Volga {c}",
    ),
    POLISH(
        "Jakub,Kacper,Mateusz,Bartosz,Kamil,Piotr,Paweł,Michał,Tomasz,Krzysztof,Łukasz,Szymon,Dawid,Adrian,Marcin,Grzegorz,Wojciech,Maciej,Filip,Arkadiusz,Damian,Przemysław,Sebastian,Rafał",
        "Nowak,Kowalski,Wiśniewski,Wójcik,Kowalczyk,Kamiński,Zieliński,Szymański,Woźniak,Dąbrowski,Kozłowski,Jankowski,Mazur,Kwiatkowski,Krawczyk,Piotrowski,Grabowski,Nowakowski,Pawłowski,Michalski,Nowicki,Adamczyk,Dudek,Zając,Wieczorek,Jabłoński,Król,Majewski,Olszewski",
        "{c} KS,Polonia {c},Wisła {c},Górnik {c},Lechia {c},Stal {c},Odra {c},Zagłębie {c},Ruch {c},Pogoń {c},Warta {c},Sparta {c}",
    ),
    CZECH(
        "Jan,Tomáš,Jakub,Martin,Lukáš,Ondřej,Petr,Pavel,Michal,David,Adam,Vojtěch,Matěj,Filip,Marek,Daniel,Jiří,Patrik,Roman,Dominik,Radek,Milan,Peter,Juraj,Marián",
        "Novák,Svoboda,Novotný,Dvořák,Černý,Procházka,Kučera,Veselý,Horák,Němec,Pokorný,Marek,Pospíšil,Hájek,Jelínek,Král,Růžička,Beneš,Fiala,Sedláček,Doležal,Zeman,Kolář,Navrátil,Čermák,Kováč,Varga,Tóth,Hudák,Baláž",
        "SK {c},FK {c},Baník {c},Slavoj {c},Dukla {c},Viktoria {c},Slovan {c},Tatran {c},Spartak {c},Jiskra {c},MFK {c},Zbrojovka {c}",
    ),
    YUGO(
        "Marko,Luka,Nikola,Stefan,Filip,Ivan,Petar,Aleksandar,Milan,Dušan,Nemanja,Uroš,Matej,Ante,Josip,Mario,Dario,Domagoj,Edin,Emir,Haris,Amar,Tarik,Darko,Goran,Vedran,Žan,Matija,Bojan,Andrej",
        "Jovanović,Petrović,Nikolić,Marković,Đorđević,Stojanović,Ilić,Stanković,Pavlović,Milošević,Kovačević,Horvat,Babić,Marić,Jurić,Knežević,Vuković,Perić,Hodžić,Begić,Hadžić,Delić,Mehmedović,Krajnc,Kos,Popovski,Stojanovski,Ristovski,Vukčević,Radović",
        "FK {c},NK {c},Dinamo {c},Radnički {c},Borac {c},Sloga {c},Budućnost {c},Rudar {c},Mladost {c},Jedinstvo {c},Sutjeska {c},Proleter {c}",
    ),
    BULGARIAN(
        "Georgi,Ivan,Dimitar,Nikolay,Petar,Hristo,Stoyan,Todor,Martin,Valentin,Kiril,Aleksandar,Plamen,Iliya,Radoslav,Svetoslav,Bozhidar,Vasil",
        "Ivanov,Georgiev,Dimitrov,Petrov,Nikolov,Hristov,Stoyanov,Todorov,Iliev,Atanasov,Angelov,Vasilev,Kolev,Marinov,Yordanov,Kostov,Mihaylov",
        "Lokomotiv {c},Slavia {c},Botev {c},Spartak {c},Etar {c},Dunav {c},Septemvri {c},Beroe {c},Chernomorets {c},Minyor {c}",
    ),
    ROMANIAN(
        "Andrei,Alexandru,Ionuț,Mihai,Florin,Cristian,Răzvan,Adrian,Bogdan,Gabriel,Ștefan,Vlad,Darius,Marius,Ciprian,Denis,Nicolae,Sorin,Radu,Cosmin,Ion,Vasile,Victor",
        "Popescu,Ionescu,Popa,Pop,Radu,Dumitru,Stan,Stoica,Gheorghe,Matei,Ciobanu,Rusu,Munteanu,Constantin,Moldovan,Lungu,Ene,Dinu,Tudor,Florea,Mihai,Toma,Sârbu,Rotaru,Cojocaru,Ursu",
        "FC {c},CS {c},Dinamo {c},Rapid {c},Universitatea {c},Petrolul {c},Unirea {c},Gloria {c},Viitorul {c},Zimbru {c},Sheriff {c},Speranța {c}",
    ),
    HUNGARIAN(
        "Bence,Máté,Levente,Dávid,Dániel,Ádám,Balázs,Péter,Zsolt,Gergő,Tamás,Attila,Norbert,Krisztián,Roland,Dominik,Milán,Botond,Zoltán,Kristóf,Barnabás",
        "Nagy,Kovács,Tóth,Szabó,Horváth,Varga,Kiss,Molnár,Németh,Farkas,Balogh,Papp,Takács,Juhász,Lakatos,Mészáros,Oláh,Simon,Rácz,Fekete,Szilágyi,Török,Fehér,Gál,Bíró",
        "{c} FC,{c} SE,{c} TE,{c} VSC,Honvéd {c},Vasas {c},Haladás {c},{c} ETO,Dózsa {c},Kinizsi {c}",
    ),
    GREEK(
        "Giorgos,Dimitris,Kostas,Nikos,Giannis,Christos,Panagiotis,Vasilis,Thanasis,Michalis,Stelios,Andreas,Alexandros,Lefteris,Sotiris,Petros,Manolis,Antonis,Stavros,Konstantinos,Marios,Loizos",
        "Papadopoulos,Papadakis,Georgiou,Nikolaidis,Ioannidis,Konstantinou,Dimitriou,Karagiannis,Vlachos,Pappas,Oikonomou,Christodoulou,Antoniou,Makris,Kyriakou,Alexiou,Panagiotou,Theodorou,Stavrou,Michailidis,Charalambous,Lambrou,Demetriou,Kyriacou,Efthymiou",
        "Aris {c},Atromitos {c},Apollon {c},Ethnikos {c},Iraklis {c},Doxa {c},Asteras {c},AE {c},Ergotelis {c},Anagennisi {c},Olympias {c},Nea Salamis {c}",
    ),
    ALBANIAN(
        "Arben,Ardit,Besnik,Blerim,Driton,Elseid,Ermal,Erion,Florian,Gentian,Ilir,Klodian,Kristjan,Lorik,Milot,Valon,Visar,Arlind,Leart,Endrit",
        "Hoxha,Krasniqi,Berisha,Shala,Gashi,Morina,Kastrati,Rama,Bytyqi,Hasani,Ahmeti,Leka,Dervishi,Kelmendi,Shehu,Mustafa,Halili,Sadiku,Basha,Musliu",
        "KF {c},Vllaznia {c},Besa {c},Flamurtari {c},Teuta {c},Drita {c},Liria {c},Ballkani {c},Tomori {c},Bylis {c}",
    ),
    CAUCASUS(
        "Giorgi,Davit,Levan,Nika,Luka,Irakli,Lasha,Saba,Zurab,Otar,Tornike,Beka,Aram,Tigran,Hovhannes,Narek,Gor,Arman,Vahe,Artur,Edgar,Levon,Hayk",
        "Beridze,Kapanadze,Gelashvili,Maisuradze,Giorgadze,Lomidze,Tsiklauri,Mamaladze,Kvirikashvili,Abashidze,Japaridze,Chkhaidze,Bolkvadze,Kiknadze,Petrosyan,Sargsyan,Hakobyan,Harutyunyan,Grigoryan,Khachatryan,Hovhannisyan,Vardanyan,Karapetyan,Mkrtchyan,Ghazaryan",
        "Dinamo {c},Torpedo {c},FC {c},Ararat {c},Shirak {c},Lokomotiv {c},Sioni {c},Merani {c},Spartak {c},Gandzasar {c}",
    ),
    BALTIC(
        "Mārtiņš,Jānis,Kristaps,Edgars,Artūrs,Roberts,Lukas,Mantas,Tomas,Dovydas,Paulius,Karolis,Andres,Martin,Rasmus,Kaspar,Henrik,Markus,Deividas,Raivo",
        "Bērziņš,Kalniņš,Ozoliņš,Liepiņš,Krūmiņš,Jansons,Kazlauskas,Petrauskas,Jankauskas,Stankevičius,Vasiliauskas,Žukauskas,Tamm,Saar,Sepp,Mägi,Kask,Kukk,Rebane,Ilves",
        "FK {c},FC {c},JK {c},Metalurgs {c},Daugava {c},Levadia {c},Trans {c},Atlantas {c},Banga {c},Kalev {c}",
    ),
    HEBREW(
        "Yossi,Eran,Omer,Itay,Dor,Lior,Tomer,Eyal,Gal,Nir,Shon,Ido,Amit,Yonatan,Daniel,Roy,Oz,Ofir,Eden,Liel",
        "Cohen,Levi,Mizrahi,Peretz,Biton,Dahan,Avraham,Friedman,Azulay,Malka,Katz,Ohayon,Amar,Gabay,Hadad,Shapira,Golan,Ben Haim,Baruch,Elkayam",
        "Hapoel {c},Maccabi {c},Beitar {c},Bnei {c},Ironi {c},Sektzia {c},Hakoah {c}",
    ),
    ARABIC(
        "Mohamed,Ahmed,Ali,Omar,Youssef,Hassan,Mahmoud,Karim,Khalid,Ibrahim,Abdullah,Hamza,Amir,Tarek,Walid,Said,Bilal,Nabil,Rami,Fahad,Salem,Faisal,Mehdi,Yassine,Achraf,Sofiane,Ayoub,Ismail",
        "Hassan,Ali,Mansour,Saleh,Haddad,Khalil,Nasser,Farouk,Rahman,Saeed,Al-Dosari,Al-Shehri,Al-Harbi,Al-Qahtani,Al-Mutairi,El-Sayed,Mostafa,Abdelrahman,Benali,Mansouri,Bouazza,Amrani,Belhadj,Hamdi,Trabelsi,Jaziri,Khaled,Fathi,Zaki,Kamal",
    ),
    PERSIAN(
        "Ali,Reza,Mehdi,Saeed,Amir,Hossein,Mohammad,Milad,Morteza,Karim,Ehsan,Majid,Vahid,Omid,Saman,Arash,Kaveh,Ramin,Payam,Shahab",
        "Hosseini,Rezaei,Ahmadi,Mohammadi,Karimi,Moradi,Jafari,Rahimi,Hashemi,Ebrahimi,Sadeghi,Ghasemi,Mousavi,Kazemi,Abbasi,Taheri,Najafi,Shirazi,Asadi,Nouri",
    ),
    CENTRAL_ASIAN(
        "Jasur,Otabek,Sardor,Bobur,Eldor,Jaloliddin,Shohruh,Abbos,Dilshod,Rustam,Timur,Azamat,Nurlan,Daulet,Yerlan,Aibek,Islam,Bakhtiyor",
        "Rakhimov,Tursunov,Abdullaev,Yusupov,Karimov,Nazarov,Sharipov,Ergashev,Saidov,Khamdamov,Akhmetov,Suleimenov,Zhakupov,Kenzhebekov,Bekov,Omarov,Iskakov",
        "FC {c},Ordabasy {c},Tobol {c},Kairat {c},Aktobe {c},Kaisar {c},Zhetysu {c},Okzhetpes {c}",
    ),
    JAPANESE(
        "Takumi,Kenta,Yuto,Sho,Daiki,Ryota,Hiroki,Kaoru,Ritsu,Wataru,Takuma,Kento,Ao,Yuki,Shuto,Koki,Junya,Daichi,Ayase,Haruto,Sota,Riku,Hayato,Keita",
        "Suzuki,Tanaka,Watanabe,Ito,Yamamoto,Nakamura,Kobayashi,Kato,Yoshida,Yamada,Sasaki,Yamaguchi,Matsumoto,Inoue,Kimura,Hayashi,Shimizu,Yamazaki,Mori,Ikeda,Hashimoto,Ishikawa,Ogawa,Okada,Fujita",
    ),
    KOREAN(
        "Min-jae,Jae-won,Seung-ho,Hyun-woo,Dong-hyun,Jun-ho,Tae-hwan,Sang-woo,Woo-young,Sung-min,Ji-hoon,Do-yeon,Hyeon-jun,Gyu-seong,Chan-woo,Jae-sung,Young-woo,In-beom",
        "Kim,Lee,Park,Choi,Jung,Kang,Cho,Yoon,Jang,Lim,Han,Oh,Seo,Shin,Kwon,Hwang,Ahn,Song,Hong,Jeon",
    ),
    CHINESE(
        "Wei,Lei,Jun,Hao,Yang,Peng,Bin,Tao,Chen,Yu,Long,Jie,Kai,Ming,Xiang,Zhen,Bo,Wu,Lin,Yi",
        "Wang,Li,Zhang,Liu,Chen,Yang,Huang,Zhao,Wu,Zhou,Xu,Sun,Ma,Zhu,Hu,Guo,He,Gao,Lin,Luo",
    ),
    AFRICAN(
        "Ibrahim,Moussa,Cheikh,Mamadou,Ousmane,Victor,Samuel,Emmanuel,Kelechi,Wilfried,Serge,Yves,Joseph,Kwame,Kofi,Nana,Abdoulaye,Lamine,Idrissa,Bakary,Seydou,Amadou,Didier,Junior,Patrick,Musa,Chidi,Ademola,Tunde,Themba,Sipho,Bongani,Thabo,Christian,Ismaïla",
        "Diallo,Traoré,Koné,Coulibaly,Camara,Diop,Ndiaye,Sow,Cissé,Touré,Keita,Sarr,Ba,Mensah,Owusu,Boateng,Asante,Appiah,Osei,Okafor,Okonkwo,Adeyemi,Eze,Nwosu,Balogun,Nkosi,Dlamini,Mokoena,Khumalo,Ndlovu,Zulu,Kabongo,Ilunga,Mukendi,Bamba,Fofana,Sylla,Doumbia",
    );

    val firstNames: List<String> = first.split(',')
    val lastNames: List<String> = last.split(',')
    val clubPatterns: List<String> = clubs.split(',')
}
