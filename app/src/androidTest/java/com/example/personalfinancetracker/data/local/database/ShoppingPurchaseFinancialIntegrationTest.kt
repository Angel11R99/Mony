package com.example.personalfinancetracker.data.local.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.personalfinancetracker.data.local.entity.CategoryEntity
import com.example.personalfinancetracker.data.repository.RoomShoppingListRepository
import com.example.personalfinancetracker.domain.model.ShoppingList
import com.example.personalfinancetracker.domain.model.ShoppingListItem
import com.example.personalfinancetracker.domain.model.ShoppingPaymentMethod
import com.example.personalfinancetracker.domain.model.ShoppingListStatus
import com.example.personalfinancetracker.domain.repository.FinalizePurchaseResult
import com.example.personalfinancetracker.domain.repository.TicketProductUpdate
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoppingPurchaseFinancialIntegrationTest {
    private lateinit var database: FinanceDatabase
    private lateinit var repository: RoomShoppingListRepository
    private var categoryId = 0L

    @Before
    fun setUp() = runBlocking {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            FinanceDatabase::class.java,
        ).allowMainThreadQueries().build()
        categoryId = database.categoryDao().insert(
            CategoryEntity(name = "Compras", type = "EXPENSE", icon = "shopping_cart", createdAtEpochMillis = 1),
        )
        repository = RoomShoppingListRepository(
            database.shoppingListDao(),
            database.transactionDao(),
            database.categoryDao(),
            database.pendingEntryDao(),
            database,
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun debitEditsUpdateSameExpenseAndPaymentChangesMigrateRepresentation() = runBlocking {
        val listId = createPurchase()
        val completed = repository.finalizePurchase(listId, categoryId, LocalDate.of(2026, 8, 15), ShoppingPaymentMethod.DEBIT)
            as FinalizePurchaseResult.Completed
        val expenseId = completed.financialRecordId
        assertEquals(15_000L, database.transactionDao().get(expenseId)?.amountInCents)

        val item = repository.getDetails(listId)!!.items.single()
        repository.saveItem(item.copy(quantity = 3, updatedAt = Instant.now()))
        assertEquals(22_500L, database.transactionDao().get(expenseId)?.amountInCents)
        assertEquals(expenseId, repository.getDetails(listId)!!.list.expenseTransactionId)

        repository.updatePurchaseSettings(listId, categoryId, LocalDate.of(2026, 8, 16), ShoppingPaymentMethod.CREDIT)
        val creditList = repository.getDetails(listId)!!.list
        assertNull(creditList.expenseTransactionId)
        assertNotNull(creditList.payableId)
        assertNull(database.transactionDao().get(expenseId))
        assertEquals(22_500L, database.pendingEntryDao().get(creditList.payableId!!)!!.amountInCents)

        repository.updatePurchaseSettings(listId, categoryId, LocalDate.of(2026, 8, 17), ShoppingPaymentMethod.DEBIT)
        val debitAgain = repository.getDetails(listId)!!.list
        assertNotNull(debitAgain.expenseTransactionId)
        assertNull(debitAgain.payableId)
        assertEquals(1, database.transactionDao().getAll().size)
    }

    @Test
    fun creditCreatesPayableWithoutImmediateExpense() = runBlocking {
        val listId = createPurchase()
        repository.finalizePurchase(listId, categoryId, LocalDate.of(2026, 8, 15), ShoppingPaymentMethod.CREDIT)
        val list = repository.getDetails(listId)!!.list
        assertNotNull(list.payableId)
        assertNull(list.expenseTransactionId)
        assertEquals(0, database.transactionDao().getAll().size)
    }

    @Test
    fun startingShoppingPersistsStatusAndEnablesManualCompletionFlow() = runBlocking {
        val listId = createPurchase()
        val details = repository.getDetails(listId)!!
        repository.update(details.list.copy(status = ShoppingListStatus.SHOPPING, updatedAt = Instant.now()))
        assertEquals(ShoppingListStatus.SHOPPING, repository.getDetails(listId)!!.list.status)
    }

    @Test
    fun ticketMatchUpdatesExistingProductNameQuantityAndActualPrice() = runBlocking {
        val listId = createPurchase()
        val initial = repository.getDetails(listId)!!.items.single()
        repository.saveItem(initial.copy(estimatedUnitPriceInCents = 7_000, updatedAt = Instant.now()))
        val original = repository.getDetails(listId)!!.items.single()
        repository.applyTicketReview(
            listId = listId,
            products = listOf(
                TicketProductUpdate(
                    detectedText = "LECHE MILEX 1L",
                    itemId = original.id,
                    displayName = "Leche Milex 1 L",
                    quantity = 3,
                    unitPriceInCents = 8_500,
                ),
            ),
            adjustments = emptyList(),
        )

        val updated = repository.getDetails(listId)!!.items.single()
        assertEquals(original.id, updated.id)
        assertEquals("Leche Milex 1 L", updated.name)
        assertEquals(3, updated.quantity)
        assertEquals(8_500L, updated.actualUnitPriceInCents)
        assertEquals(original.estimatedUnitPriceInCents, updated.estimatedUnitPriceInCents)
    }

    private suspend fun createPurchase(): Long {
        val now = Instant.now()
        val listId = repository.create(ShoppingList(name = "Supermercado", createdAt = now, updatedAt = now))
        repository.saveItem(
            ShoppingListItem(
                shoppingListId = listId,
                name = "Leche",
                quantity = 2,
                actualUnitPriceInCents = 7_500,
                isPurchased = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
        return listId
    }
}
