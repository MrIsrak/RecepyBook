package app.receptnik.core

/**
 * Встроенная таблица продуктов: ккал / белки / жиры / углеводы на 100 г,
 * вес одной штуки, плотность (г/мл) и отдел магазина.
 * Названия: русский, английский, иврит (основы слов).
 */
data class Food(
    val stems: List<String>,
    val kcal: Double, val p: Double, val f: Double, val c: Double,
    val piece: Double? = null,
    val density: Double = 1.0,
    val aisle: String = Aisles.OTHER,
    val can: Double? = null,
    val pack: Double? = null,
)

/** Отделы магазина. Хранятся как стабильные ключи; подписи — в ресурсах интерфейса. */
object Aisles {
    const val VEG = "veg"
    const val MEAT = "meat"
    const val DAIRY = "dairy"
    const val GROCERY = "grocery"
    const val BAKERY = "bakery"
    const val SPICES = "spices"
    const val NUTS = "nuts"
    const val SWEETS = "sweets"
    const val DRINKS = "drinks"
    const val OTHER = "other"
    val order = listOf(VEG, MEAT, DAIRY, BAKERY, GROCERY, SPICES, NUTS, SWEETS, DRINKS, OTHER)

    /** Версия 1.0 хранила русские названия — переводим их в ключи. */
    private val legacy = mapOf(
        "Овощи, фрукты, зелень" to VEG, "Мясо и рыба" to MEAT, "Молочное и яйца" to DAIRY,
        "Бакалея и крупы" to GROCERY, "Хлеб и выпечка" to BAKERY, "Специи, соусы, масла" to SPICES,
        "Орехи и сухофрукты" to NUTS, "Сладкое" to SWEETS, "Напитки" to DRINKS, "Другое" to OTHER,
    )

    fun normalize(v: String?): String = when {
        v == null -> OTHER
        v in order -> v
        else -> legacy[v] ?: OTHER
    }
}

data class NutritionResult(
    val total: Nutrition,
    val matched: Int,
    val considered: Int,
    val unmatched: List<String>,
) {
    val coverage: Double get() = if (considered == 0) 1.0 else matched.toDouble() / considered
}

object NutritionDb {
    private fun f(vararg stems: String) = stems.toList()

    val foods: List<Food> = listOf(
        // Бакалея
        Food(f("мук", "flour", "קמח"), 364.0, 10.0, 1.0, 76.0, density = 0.53, aisle = Aisles.GROCERY),
        Food(f("сахарн пудр", "сахарной пудр", "пудр", "powdered sugar", "icing sugar", "אבקת סוכר"), 389.0, 0.0, 0.0, 100.0, density = 0.5, aisle = Aisles.GROCERY),
        Food(f("коричнев сахар", "brown sugar", "סוכר חום"), 380.0, 0.0, 0.0, 98.0, density = 0.9, aisle = Aisles.GROCERY),
        Food(f("сахар", "sugar", "סוכר"), 387.0, 0.0, 0.0, 100.0, density = 0.85, aisle = Aisles.GROCERY),
        Food(f("сол", "salt", "מלח"), 0.0, 0.0, 0.0, 0.0, density = 1.2, aisle = Aisles.SPICES),
        Food(f("рис", "rice", "אורז"), 360.0, 7.0, 0.6, 79.0, density = 0.85, aisle = Aisles.GROCERY),
        Food(f("гречк", "гречнев", "buckwheat", "כוסמת"), 343.0, 13.0, 3.4, 72.0, density = 0.8, aisle = Aisles.GROCERY),
        Food(f("овсян", "геркулес", "oat", "שיבולת שועל"), 379.0, 13.0, 6.5, 67.0, density = 0.4, aisle = Aisles.GROCERY),
        Food(f("макарон", "паст", "спагетти", "лапш", "pasta", "spaghetti", "penne", "noodle", "פסטה", "ספגטי", "אטריות"), 371.0, 13.0, 1.5, 75.0, density = 0.6, aisle = Aisles.GROCERY),
        Food(f("киноа", "quinoa", "קינואה"), 368.0, 14.0, 6.0, 64.0, density = 0.75, aisle = Aisles.GROCERY),
        Food(f("булгур", "bulgur", "בורגול"), 342.0, 12.0, 1.3, 76.0, density = 0.8, aisle = Aisles.GROCERY),
        Food(f("кускус", "couscous", "קוסקוס"), 376.0, 13.0, 0.6, 77.0, density = 0.75, aisle = Aisles.GROCERY),
        Food(f("чечевиц", "lentil", "עדשים"), 352.0, 25.0, 1.0, 63.0, density = 0.8, aisle = Aisles.GROCERY),
        Food(f("нут", "chickpea", "garbanzo", "גרגרי חומוס"), 364.0, 19.0, 6.0, 61.0, density = 0.8, aisle = Aisles.GROCERY, can = 240.0),
        Food(f("фасол", "beans", "שעועית"), 127.0, 8.7, 0.5, 22.0, density = 0.8, aisle = Aisles.GROCERY, can = 240.0),
        Food(f("крахмал", "cornstarch", "starch", "קורנפלור"), 381.0, 0.3, 0.1, 91.0, density = 0.6, aisle = Aisles.GROCERY),
        Food(f("разрыхлит", "baking powder", "אבקת אפייה"), 53.0, 0.0, 0.0, 28.0, density = 0.9, aisle = Aisles.GROCERY, pack = 10.0),
        Food(f("сод", "baking soda", "סודה לשתייה"), 0.0, 0.0, 0.0, 0.0, density = 1.1, aisle = Aisles.GROCERY),
        Food(f("дрожж", "yeast", "שמרים"), 325.0, 40.0, 7.6, 41.0, density = 0.6, aisle = Aisles.GROCERY, pack = 11.0),
        Food(f("желатин", "gelatin", "ג'לטין"), 335.0, 85.0, 0.0, 0.0, density = 0.6, aisle = Aisles.GROCERY, pack = 10.0),
        Food(f("панировоч", "сухар", "breadcrumb", "panko", "פירורי לחם"), 395.0, 13.0, 5.0, 72.0, density = 0.45, aisle = Aisles.GROCERY),
        Food(f("манк", "semolina", "סולת"), 360.0, 12.7, 1.0, 73.0, density = 0.7, aisle = Aisles.GROCERY),

        // Хлеб
        Food(f("хлеб", "батон", "багет", "bread", "baguette", "toast", "לחם"), 265.0, 9.0, 3.2, 49.0, piece = 30.0, aisle = Aisles.BAKERY),
        Food(f("лаваш", "пит", "pita", "lavash", "פיתה", "לאפה"), 275.0, 9.0, 1.2, 56.0, piece = 60.0, aisle = Aisles.BAKERY),
        Food(f("тортиль", "tortilla", "טורטייה"), 300.0, 8.0, 7.0, 50.0, piece = 45.0, aisle = Aisles.BAKERY),
        Food(f("слоен тест", "слоёное тест", "puff pastry", "בצק עלים"), 550.0, 7.0, 37.0, 45.0, aisle = Aisles.BAKERY, pack = 500.0),

        // Молочное и яйца
        Food(f("яйц", "яиц", "яйк", "egg", "ביצ"), 155.0, 13.0, 11.0, 1.1, piece = 55.0, aisle = Aisles.DAIRY),
        Food(f("белок яиц", "белк", "egg white", "חלבון"), 52.0, 11.0, 0.2, 0.7, piece = 33.0, aisle = Aisles.DAIRY),
        Food(f("желт", "желток", "egg yolk", "yolk", "חלמון"), 322.0, 16.0, 27.0, 3.6, piece = 17.0, aisle = Aisles.DAIRY),
        Food(f("сгущ", "condensed milk", "חלב מרוכז"), 321.0, 7.9, 8.7, 54.0, density = 1.3, aisle = Aisles.DAIRY, can = 380.0),
        Food(f("кокосов молок", "кокосовое молок", "coconut milk", "חלב קוקוס"), 230.0, 2.3, 24.0, 6.0, aisle = Aisles.GROCERY, can = 400.0),
        Food(f("молок", "milk", "חלב"), 60.0, 3.2, 3.2, 4.7, density = 1.03, aisle = Aisles.DAIRY),
        Food(f("сливк", "heavy cream", "whipping cream", "cream", "שמנת מתוקה"), 300.0, 2.5, 30.0, 3.0, aisle = Aisles.DAIRY, pack = 250.0),
        Food(f("сметан", "sour cream", "שמנת חמוצה", "שמנת"), 200.0, 2.8, 20.0, 3.2, aisle = Aisles.DAIRY, pack = 200.0),
        Food(f("кефир", "kefir", "קפיר"), 52.0, 3.0, 2.5, 4.0, density = 1.03, aisle = Aisles.DAIRY),
        Food(f("йогурт", "yogurt", "yoghurt", "יוגורט"), 66.0, 5.0, 3.2, 3.5, density = 1.03, aisle = Aisles.DAIRY, pack = 150.0),
        Food(f("сливочн", "butter", "חמאה"), 717.0, 0.9, 81.0, 0.1, density = 0.91, aisle = Aisles.DAIRY, pack = 200.0),
        Food(f("творог", "творож", "cottage cheese", "ricotta", "рикотт", "קוטג'"), 156.0, 16.0, 9.0, 2.0, aisle = Aisles.DAIRY, pack = 250.0),
        Food(f("сливочный сыр", "творожный сыр", "крем-сыр", "крем сыр", "филадельф", "маскарпоне", "cream cheese", "mascarpone", "גבינת שמנת"), 342.0, 6.0, 34.0, 4.0, aisle = Aisles.DAIRY, pack = 200.0),
        Food(f("пармезан", "parmesan", "פרמזן"), 431.0, 38.0, 29.0, 4.0, density = 0.4, aisle = Aisles.DAIRY),
        Food(f("моцарел", "mozzarella", "מוצרלה"), 280.0, 22.0, 17.0, 3.0, piece = 125.0, aisle = Aisles.DAIRY),
        Food(f("фет", "брынз", "feta", "פטה", "גבינה בולגרית", "בולגרית"), 264.0, 14.0, 21.0, 4.0, aisle = Aisles.DAIRY),
        Food(f("сыр", "cheese", "cheddar", "чеддер", "גבינה", "צהובה"), 350.0, 25.0, 27.0, 2.0, density = 0.45, piece = 20.0, aisle = Aisles.DAIRY),

        // Мясо и рыба
        Food(f("куриная грудк", "куриное филе", "филе куриц", "грудк", "chicken breast", "חזה עוף"), 165.0, 31.0, 3.6, 0.0, piece = 200.0, aisle = Aisles.MEAT),
        Food(f("бедр", "окорочк", "chicken thigh", "thigh", "ירכיים", "פרגית", "פרגיות"), 209.0, 26.0, 11.0, 0.0, piece = 120.0, aisle = Aisles.MEAT),
        Food(f("крыл", "wing", "כנפיים"), 203.0, 30.0, 8.0, 0.0, piece = 90.0, aisle = Aisles.MEAT),
        Food(f("курин", "куриц", "chicken", "עוף"), 190.0, 27.0, 9.0, 0.0, piece = 1500.0, aisle = Aisles.MEAT),
        Food(f("индейк", "индюш", "turkey", "הודו"), 135.0, 29.0, 1.5, 0.0, aisle = Aisles.MEAT),
        Food(f("фарш", "ground beef", "minced", "mince", "ground meat", "בשר טחון"), 250.0, 17.0, 20.0, 0.0, aisle = Aisles.MEAT),
        Food(f("говяд", "говяж", "beef", "steak", "стейк", "בקר", "סטייק"), 250.0, 26.0, 15.0, 0.0, piece = 250.0, aisle = Aisles.MEAT),
        Food(f("свин", "pork", "חזיר"), 260.0, 26.0, 17.0, 0.0, aisle = Aisles.MEAT),
        Food(f("баранин", "ягнят", "lamb", "כבש", "טלה"), 294.0, 25.0, 21.0, 0.0, aisle = Aisles.MEAT),
        Food(f("бекон", "bacon", "בייקון"), 541.0, 37.0, 42.0, 1.4, piece = 10.0, aisle = Aisles.MEAT),
        Food(f("колбас", "сосиск", "sausage", "נקניק"), 300.0, 12.0, 27.0, 2.0, piece = 50.0, aisle = Aisles.MEAT),
        Food(f("ветчин", "ham", "פסטרמה"), 145.0, 21.0, 6.0, 1.5, piece = 15.0, aisle = Aisles.MEAT),
        Food(f("лосос", "семг", "сёмг", "форел", "salmon", "trout", "סלמון", "פורל"), 208.0, 20.0, 13.0, 0.0, piece = 150.0, aisle = Aisles.MEAT),
        Food(f("тунец", "тунц", "tuna", "טונה"), 116.0, 26.0, 1.0, 0.0, aisle = Aisles.MEAT, can = 160.0),
        Food(f("кревет", "shrimp", "prawn", "שרימפס"), 99.0, 24.0, 0.3, 0.2, piece = 12.0, aisle = Aisles.MEAT),
        Food(f("рыб", "треск", "тилапи", "fish", "cod", "tilapia", "דג", "דגים", "אמנון"), 110.0, 20.0, 3.0, 0.0, piece = 150.0, aisle = Aisles.MEAT),

        // Овощи, фрукты, зелень
        Food(f("картоф", "картош", "potato", "תפוח אדמה", "תפוחי אדמה", "תפו\"א"), 77.0, 2.0, 0.1, 17.0, piece = 170.0, aisle = Aisles.VEG),
        Food(f("батат", "sweet potato", "בטטה"), 86.0, 1.6, 0.1, 20.0, piece = 200.0, aisle = Aisles.VEG),
        Food(f("зелен лук", "зелёный лук", "зеленый лук", "green onion", "scallion", "spring onion", "בצל ירוק"), 32.0, 1.8, 0.2, 7.0, piece = 15.0, aisle = Aisles.VEG),
        Food(f("лук", "onion", "shallot", "шалот", "בצל"), 40.0, 1.1, 0.1, 9.0, piece = 110.0, aisle = Aisles.VEG),
        Food(f("чеснок", "чеснок", "garlic", "שום"), 149.0, 6.4, 0.5, 33.0, piece = 5.0, aisle = Aisles.VEG),
        Food(f("имбир", "ginger", "ג'ינג'ר"), 80.0, 1.8, 0.8, 18.0, piece = 10.0, aisle = Aisles.VEG),
        Food(f("морков", "carrot", "גזר"), 41.0, 0.9, 0.2, 10.0, piece = 70.0, aisle = Aisles.VEG),
        Food(f("черри", "cherry tomato", "שרי"), 18.0, 0.9, 0.2, 3.9, piece = 15.0, aisle = Aisles.VEG),
        Food(f("томатн паст", "томатная паст", "tomato paste", "רסק"), 82.0, 4.3, 0.5, 19.0, aisle = Aisles.SPICES, can = 140.0),
        Food(f("помидор", "томат", "tomato", "עגבני"), 18.0, 0.9, 0.2, 3.9, piece = 120.0, aisle = Aisles.VEG, can = 400.0),
        Food(f("огур", "cucumber", "מלפפון", "מלפפונים"), 15.0, 0.7, 0.1, 3.6, piece = 120.0, aisle = Aisles.VEG),
        Food(f("кабач", "цукин", "zucchini", "courgette", "קישוא"), 17.0, 1.2, 0.3, 3.1, piece = 200.0, aisle = Aisles.VEG),
        Food(f("баклажан", "eggplant", "aubergine", "חציל"), 25.0, 1.0, 0.2, 6.0, piece = 300.0, aisle = Aisles.VEG),
        Food(f("перец чили", "чили", "chili", "jalapeno", "חריף", "צ'ילי"), 40.0, 2.0, 0.4, 9.0, piece = 15.0, aisle = Aisles.VEG),
        Food(f("черный перец", "чёрный перец", "молотый перец", "перец молот", "black pepper", "ground pepper", "פלפל שחור"), 251.0, 10.0, 3.3, 64.0, density = 0.5, aisle = Aisles.SPICES),
        Food(f("перец", "перц", "bell pepper", "pepper", "פלפל"), 27.0, 1.0, 0.3, 6.0, piece = 150.0, aisle = Aisles.VEG),
        Food(f("цветн капуст", "цветная капуст", "cauliflower", "כרובית"), 25.0, 1.9, 0.3, 5.0, piece = 600.0, aisle = Aisles.VEG),
        Food(f("капуст", "cabbage", "כרוב"), 25.0, 1.3, 0.1, 6.0, piece = 1000.0, aisle = Aisles.VEG),
        Food(f("брокколи", "broccoli", "ברוקולי"), 34.0, 2.8, 0.4, 7.0, piece = 300.0, aisle = Aisles.VEG),
        Food(f("шпинат", "spinach", "תרד"), 23.0, 2.9, 0.4, 3.6, density = 0.15, aisle = Aisles.VEG),
        Food(f("гриб", "шампиньон", "mushroom", "פטריות", "פטריה"), 22.0, 3.1, 0.3, 3.3, piece = 20.0, aisle = Aisles.VEG),
        Food(f("авокадо", "avocado", "אבוקדו"), 160.0, 2.0, 15.0, 9.0, piece = 150.0, aisle = Aisles.VEG),
        Food(f("кукуруз", "corn", "תירס"), 96.0, 3.4, 1.5, 21.0, piece = 200.0, aisle = Aisles.VEG, can = 340.0),
        Food(f("горош", "peas", "אפונה"), 81.0, 5.0, 0.4, 14.0, aisle = Aisles.VEG, can = 400.0),
        Food(f("свекл", "свёкл", "beet", "סלק"), 43.0, 1.6, 0.2, 10.0, piece = 150.0, aisle = Aisles.VEG),
        Food(f("тыкв", "pumpkin", "butternut", "דלעת"), 26.0, 1.0, 0.1, 6.5, aisle = Aisles.VEG),
        Food(f("сельдер", "celery", "סלרי"), 16.0, 0.7, 0.2, 3.0, piece = 40.0, aisle = Aisles.VEG),
        Food(f("салат", "латук", "руккол", "lettuce", "arugula", "rocket", "חסה", "רוקט"), 15.0, 1.4, 0.2, 2.9, piece = 300.0, aisle = Aisles.VEG),
        Food(f("зелен", "укроп", "петрушк", "кинз", "базилик", "мят", "dill", "parsley", "cilantro", "coriander leaves", "basil", "mint", "herbs", "פטרוזיליה", "שמיר", "כוסברה", "בזיליקום", "נענע"), 36.0, 3.0, 0.8, 6.0, piece = 2.0, density = 0.1, aisle = Aisles.VEG),
        Food(f("оливк", "маслин", "olive", "זית"), 115.0, 0.8, 11.0, 6.0, piece = 4.0, aisle = Aisles.GROCERY),
        Food(f("лимон", "lemon", "לימון"), 29.0, 1.1, 0.3, 9.0, piece = 100.0, aisle = Aisles.VEG),
        Food(f("лайм", "lime", "ליים"), 30.0, 0.7, 0.2, 11.0, piece = 60.0, aisle = Aisles.VEG),
        Food(f("яблок", "apple", "תפוח", "תפוחים"), 52.0, 0.3, 0.2, 14.0, piece = 180.0, aisle = Aisles.VEG),
        Food(f("банан", "banana", "בננה", "בננות"), 89.0, 1.1, 0.3, 23.0, piece = 120.0, aisle = Aisles.VEG),
        Food(f("апельсин", "orange", "תפוז"), 47.0, 0.9, 0.1, 12.0, piece = 150.0, aisle = Aisles.VEG),
        Food(f("груш", "pear", "אגס"), 57.0, 0.4, 0.1, 15.0, piece = 170.0, aisle = Aisles.VEG),
        Food(f("ягод", "клубник", "малин", "черник", "голубик", "berr", "strawberr", "raspberr", "blueberr", "תות", "פירות יער"), 50.0, 0.7, 0.3, 12.0, piece = 12.0, density = 0.6, aisle = Aisles.VEG),

        // Орехи и сухофрукты
        Food(f("арахисов паст", "арахисовая паст", "peanut butter", "חמאת בוטנים"), 588.0, 25.0, 50.0, 20.0, density = 1.1, aisle = Aisles.NUTS),
        Food(f("миндал", "almond", "שקד"), 579.0, 21.0, 50.0, 22.0, density = 0.6, aisle = Aisles.NUTS),
        Food(f("арахис", "peanut", "בוטנים"), 567.0, 26.0, 49.0, 16.0, density = 0.6, aisle = Aisles.NUTS),
        Food(f("тахин", "tahini", "טחינה"), 595.0, 17.0, 54.0, 21.0, density = 1.0, aisle = Aisles.SPICES),
        Food(f("кунжут", "sesame", "שומשום"), 573.0, 18.0, 50.0, 23.0, density = 0.6, aisle = Aisles.NUTS),
        Food(f("кокосов стружк", "кокосовая стружк", "кокос", "coconut", "קוקוס"), 660.0, 7.0, 65.0, 24.0, density = 0.35, aisle = Aisles.NUTS),
        Food(f("орех", "грецк", "фундук", "кешью", "фисташ", "walnut", "hazelnut", "cashew", "pistachio", "pecan", "nuts", "אגוז", "קשיו", "פיסטוק"), 650.0, 15.0, 60.0, 14.0, density = 0.5, aisle = Aisles.NUTS),
        Food(f("семечк", "семена", "seeds", "chia", "чиа", "גרעינים", "צ'יה"), 540.0, 20.0, 45.0, 20.0, density = 0.6, aisle = Aisles.NUTS),
        Food(f("изюм", "raisin", "צימוקים"), 299.0, 3.1, 0.5, 79.0, density = 0.65, aisle = Aisles.NUTS),
        Food(f("финик", "medjool", "תמר"), 282.0, 2.5, 0.4, 75.0, piece = 8.0, aisle = Aisles.NUTS),
        Food(f("курага", "кураг", "dried apricot", "משמש מיובש"), 241.0, 3.4, 0.5, 63.0, piece = 8.0, aisle = Aisles.NUTS),

        // Сладкое
        Food(f("мед", "мёд", "honey", "דבש"), 304.0, 0.3, 0.0, 82.0, density = 1.42, aisle = Aisles.SWEETS),
        Food(f("сироп", "syrup", "סילאן", "סירופ"), 270.0, 0.0, 0.0, 70.0, density = 1.3, aisle = Aisles.SWEETS),
        Food(f("шоколад", "chocolate", "שוקולד"), 546.0, 5.0, 31.0, 61.0, density = 0.6, piece = 10.0, aisle = Aisles.SWEETS, pack = 100.0),
        Food(f("какао", "cocoa", "קקאו"), 228.0, 20.0, 14.0, 58.0, density = 0.4, aisle = Aisles.SWEETS),
        Food(f("ванил", "vanilla", "וניל"), 288.0, 0.1, 0.1, 13.0, aisle = Aisles.SWEETS, pack = 10.0),
        Food(f("печенье", "печенья", "cookie", "biscuit", "עוגיות", "ביסקוויט"), 450.0, 6.0, 18.0, 68.0, piece = 10.0, aisle = Aisles.SWEETS),

        // Специи, соусы, масла
        Food(f("оливков", "olive oil", "שמן זית"), 884.0, 0.0, 100.0, 0.0, density = 0.92, aisle = Aisles.SPICES),
        Food(f("масл", "oil", "שמן"), 884.0, 0.0, 100.0, 0.0, density = 0.92, aisle = Aisles.SPICES),
        Food(f("соев", "soy", "סויה"), 53.0, 8.0, 0.6, 5.0, density = 1.2, aisle = Aisles.SPICES),
        Food(f("кетчуп", "ketchup", "קטשופ"), 112.0, 1.7, 0.2, 26.0, density = 1.15, aisle = Aisles.SPICES),
        Food(f("майонез", "mayo", "mayonnaise", "מיונז"), 680.0, 1.0, 75.0, 0.6, density = 0.95, aisle = Aisles.SPICES),
        Food(f("горчиц", "mustard", "חרדל"), 66.0, 4.4, 4.0, 5.8, density = 1.05, aisle = Aisles.SPICES),
        Food(f("уксус", "vinegar", "חומץ"), 20.0, 0.0, 0.0, 0.6, aisle = Aisles.SPICES),
        Food(f("песто", "pesto", "פסטו"), 450.0, 5.0, 45.0, 6.0, aisle = Aisles.SPICES),
        Food(f("бульон", "broth", "stock", "ציר", "אבקת מרק"), 5.0, 0.5, 0.2, 0.3, aisle = Aisles.SPICES),
        Food(f("паприк", "paprika", "פפריקה"), 282.0, 14.0, 13.0, 54.0, density = 0.45, aisle = Aisles.SPICES),
        Food(f("кориц", "cinnamon", "קינמון"), 247.0, 4.0, 1.2, 81.0, density = 0.55, aisle = Aisles.SPICES),
        Food(f("куркум", "turmeric", "כורכום"), 312.0, 10.0, 3.3, 67.0, density = 0.55, aisle = Aisles.SPICES),
        Food(f("зир", "кумин", "cumin", "כמון"), 375.0, 18.0, 22.0, 44.0, density = 0.45, aisle = Aisles.SPICES),
        Food(f("мускат", "nutmeg", "אגוז מוסקט"), 525.0, 6.0, 36.0, 49.0, density = 0.5, aisle = Aisles.SPICES),
        Food(f("карри", "curry", "קארי"), 325.0, 14.0, 14.0, 58.0, density = 0.45, aisle = Aisles.SPICES),
        Food(f("орегано", "тимьян", "розмарин", "прованс", "итальянск трав", "oregano", "thyme", "rosemary", "italian seasoning", "אורגנו", "טימין", "רוזמרין", "זעתר"), 270.0, 9.0, 4.3, 69.0, density = 0.3, aisle = Aisles.SPICES),
        Food(f("специ", "приправ", "spice", "seasoning", "תבלין", "תבלינים"), 250.0, 10.0, 8.0, 50.0, density = 0.5, aisle = Aisles.SPICES),

        // Напитки
        Food(f("вод", "water", "מים"), 0.0, 0.0, 0.0, 0.0, aisle = Aisles.DRINKS),
        Food(f("вин", "wine", "יין"), 85.0, 0.1, 0.0, 2.6, aisle = Aisles.DRINKS),
        Food(f("пив", "beer", "בירה"), 43.0, 0.5, 0.0, 3.6, aisle = Aisles.DRINKS),
        Food(f("кофе", "coffee", "espresso", "эспрессо", "קפה"), 2.0, 0.1, 0.0, 0.0, aisle = Aisles.DRINKS),
        Food(f("сок", "juice", "מיץ"), 45.0, 0.5, 0.1, 10.0, aisle = Aisles.DRINKS),
    )

    private val stemIndex: List<Pair<String, Food>> = foods.flatMap { food -> food.stems.map { it.lowercase() to food } }

    private fun isHebrew(s: String) = s.any { it in '֐'..'׿' }

    /** Находит продукт по названию: длиннее основа — точнее; при равенстве — та, что ближе к концу (существительное). */
    fun match(name: String): Food? {
        val n = " " + name.lowercase().replace('ё', 'е') + " "
        var best: Food? = null
        var bestScore = -1
        for ((stem0, food) in stemIndex) {
            val stem = stem0.replace('ё', 'е')
            var idx = n.indexOf(stem)
            while (idx >= 0) {
                val ok = isHebrew(stem) || !n[idx - 1].isLetter()
                if (ok) {
                    val score = stem.length * 1000 + idx
                    if (score > bestScore) { bestScore = score; best = food }
                }
                idx = n.indexOf(stem, idx + 1)
            }
        }
        return best
    }

    fun aisleFor(name: String): String = match(name)?.aisle ?: Aisles.OTHER

    /** Вес ингредиента в граммах; null — если оценить нельзя. */
    fun grams(ing: Ingredient, food: Food): Double? {
        val q = ing.quantity ?: return null
        val qq = if (ing.quantityMax != null) (q + ing.quantityMax) / 2 else q
        val unit = Units.byKey(ing.unit)
        return when {
            unit == null -> food.piece?.let { qq * it } ?: if (qq >= 20) qq else qq * 100.0
            unit.kind == UnitKind.MASS -> qq * unit.factor
            unit.kind == UnitKind.VOLUME -> qq * unit.factor * food.density
            else -> when (unit.key) {
                "clove" -> qq * 5.0
                "can" -> qq * (food.can ?: 400.0)
                "pack" -> qq * (food.pack ?: 200.0)
                "slice" -> qq * 25.0
                "bunch" -> qq * 30.0
                "handful" -> qq * 30.0
                "sprig" -> qq * 2.0
                else -> qq * (food.piece ?: 100.0)
            }
        }
    }

    fun calculate(ingredients: List<Ingredient>): NutritionResult {
        var total = Nutrition()
        var matched = 0
        var considered = 0
        val unmatched = mutableListOf<String>()
        for (ing in ingredients) {
            if (ing.isHeader) continue
            val food = match(ing.name.ifBlank { ing.original })
            if (ing.quantity == null) {
                // «соль по вкусу» — не влияет на калорийность
                if (food == null && ing.note == null) unmatched += ing.name
                continue
            }
            considered++
            if (food == null) { unmatched += ing.name; continue }
            val g = grams(ing, food) ?: continue
            matched++
            total += Nutrition(food.kcal, food.p, food.f, food.c) * (g / 100.0)
        }
        return NutritionResult(total, matched, considered, unmatched)
    }
}
