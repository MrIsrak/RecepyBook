package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizationTest {

    @Test fun languageCodes() {
        assertEquals("he", Lang.of("iw"))
        assertEquals("he", Lang.of("he"))
        assertEquals("ru", Lang.of("ru"))
        assertEquals("en", Lang.of("de"))
        assertEquals("en", Lang.of(null))
    }

    @Test fun unitLabels() {
        assertEquals("ст. л.", Units.display("tbsp", "ru"))
        assertEquals("tbsp", Units.display("tbsp", "en"))
        assertEquals("כף", Units.display("tbsp", "iw"))
    }

    @Test fun decimalSeparator() {
        assertEquals("1,2", QtyFormat.format(1.2, "ru"))
        assertEquals("1.2", QtyFormat.format(1.2, "en"))
    }

    @Test fun amountsInEachLanguage() {
        val ing = IngredientParser.parse("200 г муки")
        assertEquals("200 г", QtyFormat.formatAmount(ing, 1.0, UnitSystem.ORIGINAL, "ru"))
        assertEquals("400 g", QtyFormat.formatAmount(ing, 2.0, UnitSystem.ORIGINAL, "en"))
    }

    @Test fun legacyAislesAreMigrated() {
        assertEquals(Aisles.DAIRY, Aisles.normalize("Молочное и яйца"))
        assertEquals(Aisles.VEG, Aisles.normalize(Aisles.VEG))
        assertEquals(Aisles.OTHER, Aisles.normalize("???"))
        assertEquals(Aisles.MEAT, NutritionDb.aisleFor("куриное филе"))
    }
}
