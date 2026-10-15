package app.receptnik.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class IngredientParserTest {

    private fun check(line: String, qty: Double?, unit: String?, name: String) {
        val i = IngredientParser.parse(line)
        assertEquals("quantity of '$line'", qty, i.quantity)
        assertEquals("unit of '$line'", unit, i.unit)
        assertEquals("name of '$line'", name, i.name)
    }

    @Test fun quantityFirst() {
        check("200 г муки", 200.0, "g", "муки")
        check("200г сыра", 200.0, "g", "сыра")
        check("1 tbsp olive oil", 1.0, "tbsp", "olive oil")
        check("2 eggs", 2.0, null, "eggs")
        check("1 can chickpeas", 1.0, "can", "chickpeas")
        check("• 1 ч.л. соды", 1.0, "tsp", "соды")
    }

    @Test fun quantityLast() {
        check("Мука — 200 г", 200.0, "g", "Мука")
        check("Яйца 2 шт.", 2.0, "piece", "Яйца")
        check("Куриное филе 500 г", 500.0, "g", "Куриное филе")
        check("Сливочное масло 50г", 50.0, "g", "Сливочное масло")
        check("✅ Молоко - 250 мл", 250.0, "ml", "Молоко")
        check("Помидоры черри (200 г)", 200.0, "g", "Помидоры черри")
    }

    @Test fun fractionsAndRanges() {
        check("1½ cups all-purpose flour", 1.5, "cup", "all-purpose flour")
        check("1/2 стакана молока", 0.5, "glass", "молока")
        val r = IngredientParser.parse("2-3 ст. л. сахара")
        assertEquals(2.0, r.quantity)
        assertEquals(3.0, r.quantityMax)
        assertEquals("tbsp", r.unit)
    }

    @Test fun hebrew() {
        check("3 כוסות קמח", 3.0, "glass", "קמח")
        check("400 גרם חזה עוף", 400.0, "g", "חזה עוף")
    }

    @Test fun noQuantity() {
        val i = IngredientParser.parse("Соль по вкусу")
        assertNull(i.quantity)
        assertEquals("Соль", i.name)
        assertEquals("по вкусу", i.note)
    }

    @Test fun groupHeader() {
        assertTrue(IngredientParser.parse("Для соуса:").isHeader)
    }
}
