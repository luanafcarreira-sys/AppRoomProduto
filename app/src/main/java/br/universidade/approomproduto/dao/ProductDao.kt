package br.universidade.approomproduto.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import br.universidade.approomproduto.model.Product

@Dao
interface ProductDao {

    // suspend: o Room não permite consultas na main thread
    @Query("SELECT * FROM product")
    suspend fun getAll(): List<Product>

    @Insert
    suspend fun insertAll(vararg products: Product)

    // A5: busca por nome (o app envia "%texto%" para achar nomes parecidos)
    @Query("SELECT * FROM product WHERE name LIKE :nome")
    suspend fun buscarPorNome(nome: String): List<Product>

    // Auxiliar para A7 e A6 (extra): localizar um produto pelo id
    @Query("SELECT * FROM product WHERE id = :id")
    suspend fun buscarPorId(id: Int): Product?

    // A7: atualizar um produto já cadastrado
    @Update
    suspend fun update(product: Product)

    // A6 (extra): excluir um produto específico
    @Delete
    suspend fun delete(product: Product)

    // A6: excluir todos os produtos
    @Query("DELETE FROM product")
    suspend fun deleteAll()
}
