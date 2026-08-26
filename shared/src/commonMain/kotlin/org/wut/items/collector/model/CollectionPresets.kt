package org.wut.items.collector.model
























object CollectionPresets {

    val STAMPS = CollectionPreset(
        id = "stamps",
        name = "Znaczki",
        description = "Kolekcja znaczków pocztowych",
        schema = listOf(
            AttributeDef("country", "Kraj pochodzenia", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("price", "Cena (PLN)", AttributeType.NUMBER),
            AttributeDef("color", "Kolor dominujący", AttributeType.TEXT),
            AttributeDef(
                key = "condition",
                label = "Stan zachowania",
                type = AttributeType.SELECT,
                options = listOf("Idealny", "Bardzo dobry", "Dobry", "Średni", "Zły")
            )
        )
    )

    val COINS = CollectionPreset(
        id = "coins",
        name = "Monety",
        description = "Kolekcja monet kolekcjonerskich",
        schema = listOf(
            AttributeDef("country", "Kraj", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok bicia", AttributeType.NUMBER),
            AttributeDef("nominal", "Nominal", AttributeType.TEXT),
            AttributeDef("metal", "Metal", AttributeType.SELECT,
                options = listOf("Złoto", "Srebro", "Miedź", "Mosiądz", "Inny")),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Mennicza", "Ekstra Fine", "Fine", "Very Good", "Good"))
        )
    )


    val BOOKS = CollectionPreset(
        id = "books",
        name = "Książki",
        description = "Moja biblioteczka",
        schema = listOf(
            AttributeDef("author", "Autor", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("genre", "Gatunek", AttributeType.SELECT,
                options = listOf("Kryminał", "Fantasy", "Sci-Fi", "Literatura faktu")),
            AttributeDef("read", "Przeczytane", AttributeType.BOOLEAN)
        )
    )

    


    val VINYL = CollectionPreset(
        id = "vinyl",
        name = "Płyty winylowe",
        description = "Kolekcja płyt winylowych",
        schema = listOf(
            AttributeDef("artist", "Artysta", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("genre", "Gatunek", AttributeType.SELECT,
                options = listOf("Rock", "Jazz", "Klasyka", "Pop")),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("M", "NM", "VG+", "VG", "G"))
        )
    )

    val HW_GENERAL = CollectionPreset(
        id = "hw_general",
        name = "Hot Wheels - Ogólne",
        description = "Ogólna kolekcja modeli Hot Wheels",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("color", "Kolor", AttributeType.TEXT),
            AttributeDef("series", "Seria", AttributeType.TEXT),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("MINT (Blistr idealny)", "Karta zagięta/uszkodzona", "LOOSE (Rozpakowany)"))
        )
    )

    val HW_MAINLINE = CollectionPreset(
        id = "hw_mainline",
        name = "Hot Wheels Mainline",
        description = "Kolekcja podstawowych modeli Hot Wheels",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok rocznika na karcie", AttributeType.NUMBER),
            AttributeDef("col_number", "Numer w roczniku (np. 125/250)", AttributeType.TEXT),
            AttributeDef("variant", "Wariant", AttributeType.SELECT,
                options = listOf("Zwykły", "Treasure Hunt (TH)", "Super Treasure Hunt (STH)", "Zamac", "Red Edition")),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Blistr", "Short Card", "Loose"))
        )
    )

    val HW_PREMIUM = CollectionPreset(
        id = "hw_premium",
        name = "Hot Wheels Premium",
        description = "Kolekcja modeli z serii Premium (np. Car Culture, Boulevard)",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("line", "Linia (np. Boulevard, Car Culture)", AttributeType.SELECT,
                options = listOf("Boulevard", "Car Culture", "Team Transport", "Pop Culture", "Inna")),
            AttributeDef("set", "Nazwa setu (np. Japan Historics)", AttributeType.TEXT),
            AttributeDef("set_number", "Numer w secie (np. 1/5)", AttributeType.TEXT),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Sealed (Zapakowany)", "Loose (Otwarty)"))
        )
    )

    val HW_SILVER = CollectionPreset(
        id = "hw_silver",
        name = "Hot Wheels Silver Series",
        description = "Kolekcja modeli z serii Silver (tzw. Mid-Premium)",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("theme", "Tematyka/Seria (np. Neon Speeders)", AttributeType.TEXT, required = true),
            AttributeDef("set_number", "Numer w serii (np. 3/6)", AttributeType.TEXT),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("condition", "Stan opakowania", AttributeType.SELECT,
                options = listOf("Idealny", "Uszkodzony", "Loose"))
        )
    )

    val HW_RLC = CollectionPreset(
        id = "hw_rlc",
        name = "Hot Wheels RLC",
        description = "Ekskluzywna kolekcja Red Line Club",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("serial", "Numer z hologramu (np. 1234/30000)", AttributeType.TEXT),
            AttributeDef("color", "Kolor lakieru (Spectraflame)", AttributeType.TEXT),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Zaplombowany (Sealed)", "Otwarty", "Brak oryginalnego kartonika"))
        )
    )

    val LEGO_SETS = CollectionPreset(
        id = "lego_sets",
        name = "Zestawy LEGO",
        description = "Kolekcja zestawów klocków LEGO",
        schema = listOf(
            AttributeDef("set_number", "Numer zestawu", AttributeType.TEXT, required = true),
            AttributeDef("name", "Nazwa", AttributeType.TEXT, required = true),
            AttributeDef("theme", "Seria (np. Star Wars, Technic)", AttributeType.TEXT),
            AttributeDef("completeness", "Kompletność", AttributeType.SELECT,
                options = listOf("100% z pudełkiem (MISB/CIB)", "100% bez pudełka", "Braki w klockach", "Tylko figurki")),
            AttributeDef("instructions", "Instrukcja w zestawie", AttributeType.BOOLEAN)
        )
    )

    val TCG_CARDS = CollectionPreset(
        id = "tcg_cards",
        name = "Karty TCG (Pokémon / MTG)",
        description = "Kolekcjonerskie karty do gry",
        schema = listOf(
            AttributeDef("name", "Nazwa karty", AttributeType.TEXT, required = true),
            AttributeDef("game", "Gra", AttributeType.SELECT,
                options = listOf("Pokémon TCG", "Magic: The Gathering", "Yu-Gi-Oh!", "Lorcana", "Inna")),
            AttributeDef("set", "Set / Dodatek", AttributeType.TEXT),
            AttributeDef("holo", "Karta Foil / Holo", AttributeType.BOOLEAN),
            AttributeDef("grading", "Ocena (Grading)", AttributeType.SELECT,
                options = listOf("Brak", "PSA", "CGC", "Beckett (BGS)"))
        )
    )

    val VIDEO_GAMES = CollectionPreset(
        id = "video_games",
        name = "Gry Wideo (Retro & Modern)",
        description = "Fizyczne wydania gier wideo",
        schema = listOf(
            AttributeDef("title", "Tytuł gry", AttributeType.TEXT, required = true),
            AttributeDef("platform", "Platforma", AttributeType.SELECT,
                options = listOf("PlayStation", "Xbox", "Nintendo", "PC", "Retro (Sega, Atari itp.)")),
            AttributeDef("completeness", "Stan wydania", AttributeType.SELECT,
                options = listOf("Nowa w folii (Sealed)", "CIB (Pudełko + Gra + Instrukcja)", "Gra + Pudełko", "Tylko nośnik (Loose)")),
            AttributeDef("region", "Region", AttributeType.SELECT,
                options = listOf("PAL (Europa)", "NTSC-U (Ameryka)", "NTSC-J (Japonia)"))
        )
    )

    val FUNKO_POP = CollectionPreset(
        id = "funko_pop",
        name = "Figurki Funko POP!",
        description = "Kolekcja winylowych figurek",
        schema = listOf(
            AttributeDef("name", "Postać", AttributeType.TEXT, required = true),
            AttributeDef("number", "Numer figurki", AttributeType.NUMBER),
            AttributeDef("franchise", "Franczyza (np. Marvel, Star Wars)", AttributeType.TEXT),
            AttributeDef("exclusive", "Naklejka / Exclusive", AttributeType.TEXT),
            AttributeDef("box_condition", "Stan pudełka", AttributeType.SELECT,
                options = listOf("Idealny (Mint)", "Lekkie zagięcia", "Uszkodzone", "Brak pudełka (Out of Box)"))
        )
    )

    val HW_ERRORS = CollectionPreset(
        id = "hw_errors",
        name = "Hot Wheels - Błędy Fabryczne (Errors)",
        description = "Kolekcja modeli z błędami produkcyjnymi (bardzo poszukiwane)",
        schema = listOf(
            AttributeDef("model", "Model auta", AttributeType.TEXT, required = true),
            AttributeDef("error_type", "Typ błędu", AttributeType.SELECT,
                options = listOf("Brak detali/malowania", "Złe koła (Wheel Error)", "Odwrotnie w blistrze", "Brak części", "Inny")),
            AttributeDef("year", "Rok produkcji", AttributeType.NUMBER),
            AttributeDef("verified", "Potwierdzony autentyk (Blistr nienaruszony)", AttributeType.BOOLEAN)
        )
    )

    val HW_MONSTER_TRUCKS = CollectionPreset(
        id = "hw_monster_trucks",
        name = "Hot Wheels Monster Trucks",
        description = "Kolekcja pojazdów typu Monster Truck",
        schema = listOf(
            AttributeDef("name", "Nazwa pojazdu", AttributeType.TEXT, required = true),
            AttributeDef("scale", "Skala", AttributeType.SELECT,
                options = listOf("1:64", "1:43", "1:24")),
            AttributeDef("crushable_car", "Zawiera autko do zgniatania?", AttributeType.BOOLEAN),
            AttributeDef("th", "Treasure Hunt?", AttributeType.BOOLEAN)
        )
    )

    val COMICS = CollectionPreset(
        id = "comics",
        name = "Komiksy",
        description = "Kolekcja komiksów i albumów graficznych",
        schema = listOf(
            AttributeDef("title", "Tytuł", AttributeType.TEXT, required = true),
            AttributeDef("issue_number", "Numer wydania", AttributeType.TEXT),
            AttributeDef("publisher", "Wydawnictwo", AttributeType.TEXT),
            AttributeDef("author", "Autor", AttributeType.TEXT),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("language", "Język", AttributeType.SELECT,
                options = listOf("Polski", "Angielski", "Japoński", "Inny")),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Idealny", "Bardzo dobry", "Dobry", "Średni", "Zły"))
        )
    )

    val AUDIO_MEDIA = CollectionPreset(
        id = "audio_media",
        name = "Płyty CD i kasety",
        description = "Kolekcja albumów muzycznych na nośnikach fizycznych",
        schema = listOf(
            AttributeDef("artist", "Wykonawca", AttributeType.TEXT, required = true),
            AttributeDef("album", "Album", AttributeType.TEXT, required = true),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("genre", "Gatunek", AttributeType.TEXT),
            AttributeDef("format", "Format", AttributeType.SELECT,
                options = listOf("CD", "Kaseta magnetofonowa", "MiniDisc", "Inny")),
            AttributeDef("edition", "Wydanie", AttributeType.TEXT),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Nowy", "Bardzo dobry", "Dobry", "Średni", "Zły"))
        )
    )

    val MOVIES = CollectionPreset(
        id = "movies",
        name = "Filmy DVD/Blu-ray",
        description = "Kolekcja filmów na nośnikach fizycznych",
        schema = listOf(
            AttributeDef("title", "Tytuł", AttributeType.TEXT, required = true),
            AttributeDef("format", "Format", AttributeType.SELECT,
                options = listOf("DVD", "Blu-ray", "4K UHD Blu-ray", "Inny")),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("region", "Region", AttributeType.SELECT,
                options = listOf("Region free", "Europa (2/B)", "Ameryka (1/A)", "Azja (3/C)", "Inny")),
            AttributeDef("edition", "Edycja", AttributeType.TEXT),
            AttributeDef("language", "Język", AttributeType.TEXT),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Nowy w folii", "Bardzo dobry", "Dobry", "Średni", "Zły"))
        )
    )

    val BOARD_GAMES = CollectionPreset(
        id = "board_games",
        name = "Gry planszowe",
        description = "Kolekcja gier planszowych i karcianych",
        schema = listOf(
            AttributeDef("title", "Tytuł", AttributeType.TEXT, required = true),
            AttributeDef("publisher", "Wydawca", AttributeType.TEXT),
            AttributeDef("year", "Rok wydania", AttributeType.NUMBER),
            AttributeDef("players", "Liczba graczy", AttributeType.TEXT),
            AttributeDef("language", "Język", AttributeType.TEXT),
            AttributeDef("completeness", "Kompletność", AttributeType.SELECT,
                options = listOf("Kompletna", "Brak pojedynczych elementów", "Niekompletna", "Niesprawdzona"))
        )
    )

    val COLLECTIBLE_FIGURES = CollectionPreset(
        id = "collectible_figures",
        name = "Figurki kolekcjonerskie",
        description = "Kolekcja figurek postaci i modeli ekspozycyjnych",
        schema = listOf(
            AttributeDef("character", "Postać", AttributeType.TEXT, required = true),
            AttributeDef("franchise", "Franczyza", AttributeType.TEXT),
            AttributeDef("manufacturer", "Producent", AttributeType.TEXT),
            AttributeDef("series", "Seria", AttributeType.TEXT),
            AttributeDef("scale", "Skala", AttributeType.TEXT),
            AttributeDef("limited_edition", "Edycja limitowana", AttributeType.BOOLEAN),
            AttributeDef("condition", "Stan", AttributeType.SELECT,
                options = listOf("Nowa w opakowaniu", "Idealny", "Bardzo dobry", "Dobry", "Uszkodzona"))
        )
    )

    val SPORTS_CARDS = CollectionPreset(
        id = "sports_cards",
        name = "Karty sportowe",
        description = "Kolekcja kart zawodników i drużyn sportowych",
        schema = listOf(
            AttributeDef("player", "Zawodnik", AttributeType.TEXT, required = true),
            AttributeDef("sport", "Dyscyplina", AttributeType.TEXT),
            AttributeDef("team", "Drużyna", AttributeType.TEXT),
            AttributeDef("season", "Sezon", AttributeType.TEXT),
            AttributeDef("series", "Seria", AttributeType.TEXT),
            AttributeDef("card_number", "Numer karty", AttributeType.TEXT),
            AttributeDef("grading", "Grading", AttributeType.SELECT,
                options = listOf("Brak", "PSA", "BGS", "CGC", "SGC", "Inny"))
        )
    )

    val ALL = listOf(
        STAMPS,
        COINS,
        BOOKS,
        VINYL,
        HW_GENERAL,
        HW_MAINLINE,
        HW_PREMIUM,
        HW_SILVER,
        HW_RLC,
        HW_ERRORS,
        HW_MONSTER_TRUCKS,
        LEGO_SETS,
        FUNKO_POP,
        VIDEO_GAMES,
        COMICS,
        AUDIO_MEDIA,
        MOVIES,
        BOARD_GAMES,
        COLLECTIBLE_FIGURES,
        SPORTS_CARDS
    )
}

data class CollectionPreset(
    val id: String,
    val name: String,
    val description: String,
    val schema: List<AttributeDef>,
    val isBuiltIn: Boolean = false
)
