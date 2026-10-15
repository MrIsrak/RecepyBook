package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NutritionAndUnitsTest {

    @Test fun foodMatching() {
        assertEquals("оливков", NutritionDb.match("оливковое масло")?.stems?.first())
        assertEquals("сливочн", NutritionDb.match("сливочное масло")?.stems?.first())
        assertEquals("зелен лук", NutritionDb.match("зелёный лук")?.stems?.first())
        assertNotNull(NutritionDb.match("eggplant"))
        assertNotNull(NutritionDb.match("קמח"))
    }

    @Test fun caloriesAreReasonable() {
        // 100 g of sugar ≈ 387 kcal
        val r = NutritionDb.calculate(listOf(IngredientParser.parse("100 г сахара")))
        assertEquals(387.0, r.total.kcal, 1.0)
        assertEquals(1.0, r.coverage, 0.0)
    }

    @Test fun conversion() {
        val (q, u) = Units.convert(12.0, "oz", UnitSystem.METRIC)
        assertEquals("g", u)
        assertEquals(340.2, q!!, 0.5)
        val (q2, u2) = Units.convert(1.0, "cup", UnitSystem.METRIC)
        assertEquals("ml", u2)
        assertEquals(240.0, q2!!, 0.01)
    }

    @Test fun quantityFormatting() {
        assertEquals("½", QtyFormat.format(0.5))
        assertEquals("1½", QtyFormat.format(1.5))
        assertEquals("2¼", QtyFormat.format(2.25))
        assertEquals("213", QtyFormat.format(212.7))
        assertTrue(QtyFormat.format(1.15).startsWith("1,"))
    }
}
