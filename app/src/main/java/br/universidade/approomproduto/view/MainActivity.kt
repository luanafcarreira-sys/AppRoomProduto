package br.universidade.approomproduto.view

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import br.universidade.approomproduto.dao.ProductDao
import br.universidade.approomproduto.databinding.ActivityMainBinding
import br.universidade.approomproduto.db.AppDatabase
import br.universidade.approomproduto.model.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private lateinit var productDao: ProductDao

    private val formatoMoeda: NumberFormat =
        NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicializa o ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Evita que a tela fique por baixo das barras do sistema (Android 15+)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val barras = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(barras.left, barras.top, barras.right, barras.bottom)
            insets
        }

        // Cria (ou abre) o banco de dados Room
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "database-name"
        )
            .fallbackToDestructiveMigration() // recria o banco se o esquema mudar (só para desenvolvimento)
            .build()
        productDao = db.productDao()

        binding.btnInserirProduto.setOnClickListener { inserirProduto() }
        binding.btnListarProdutos.setOnClickListener { listarProdutos() }
        binding.btnBuscar.setOnClickListener { buscarPorNome() }
        binding.btnAtualizarPreco.setOnClickListener { atualizarPreco() }
        binding.btnExcluirPorId.setOnClickListener { excluirPorId() }
        binding.btnExcluirTodos.setOnClickListener { confirmarExcluirTodos() }
    }

    // ---------------------------------------------------------------
    // Inserir (tutorial + A1, A2 e A8)
    // ---------------------------------------------------------------
    private fun inserirProduto() {
        val name = binding.editProductName.text.toString().trim()
        val description = binding.editProductDescription.text.toString().trim()
        val quantityText = binding.editProductQuantity.text.toString().trim()

        if (name.isEmpty()) {
            binding.editProductName.error = "Informe o nome do produto"
            return
        }

        // A2: preço vazio, zero ou negativo mostra o erro no próprio campo
        val price = lerPreco(binding.editProductPrice) ?: return

        // A8: quantidade (vazia vira 0; negativa ou inválida mostra erro)
        val quantity = if (quantityText.isEmpty()) 0 else quantityText.toIntOrNull()
        if (quantity == null || quantity < 0) {
            binding.editProductQuantity.error = "Informe uma quantidade válida"
            return
        }

        val product = Product(
            name = name,
            price = price,
            description = description.ifEmpty { null },
            quantity = quantity
        )

        // Grava em segundo plano usando corrotina
        lifecycleScope.launch {
            productDao.insertAll(product)
            Log.i(TAG, "Produto inserido: $name - R\$$price")
            toast("Produto inserido!")
            limparCampos() // A1
        }
    }

    // A2: lê o preço de um campo. Aceita vírgula ou ponto. Retorna null se for inválido.
    private fun lerPreco(campo: EditText): Double? {
        val valor = campo.text.toString().trim().replace(',', '.').toDoubleOrNull()
        if (valor == null || valor <= 0.0) {
            campo.error = "Informe um preço válido"
            return null
        }
        return valor
    }

    // A1: limpa os campos e devolve o cursor para o campo Nome
    private fun limparCampos() {
        binding.editProductName.text.clear()
        binding.editProductPrice.text.clear()
        binding.editProductDescription.text.clear()
        binding.editProductQuantity.text.clear()
        binding.editProductName.error = null
        binding.editProductPrice.error = null
        binding.editProductQuantity.error = null
        binding.editProductName.requestFocus()
    }

    // ---------------------------------------------------------------
    // Listar (tutorial + A3 e A4)
    // ---------------------------------------------------------------
    private fun listarProdutos() {
        lifecycleScope.launch { carregarLista() }
    }

    private suspend fun carregarLista() {
        val products = productDao.getAll()
        exibirLista(products)

        if (products.isEmpty()) {
            Log.i(TAG, "Nenhum produto encontrado.")
        } else {
            products.forEach {
                Log.i(TAG, "Produto = ${it.name}, Preço = R\$${it.price}, Descrição = ${it.description}")
            }
        }
    }

    // A3: mostra a lista no TextView | A4: quantidade de produtos e soma dos preços
    private fun exibirLista(products: List<Product>, titulo: String = "Produtos cadastrados") {
        if (products.isEmpty()) {
            binding.textResultado.text = "Nenhum produto encontrado."
            return
        }

        val linhas = products.joinToString("\n") {
            "#${it.id} - ${it.name} | ${formatoMoeda.format(it.price)} | Qtd: ${it.quantity} | ${it.description ?: "-"}"
        }
        val somaPrecos = products.sumOf { it.price }
        val valorEstoque = products.sumOf { it.price * it.quantity }

        binding.textResultado.text = "$titulo\n\n$linhas\n\n" +
                "Quantidade de produtos: ${products.size}\n" +
                "Soma dos preços: ${formatoMoeda.format(somaPrecos)}\n" +
                "Valor em estoque (preço x quantidade): ${formatoMoeda.format(valorEstoque)}"
    }

    // ---------------------------------------------------------------
    // A5: buscar por nome
    // ---------------------------------------------------------------
    private fun buscarPorNome() {
        val texto = binding.editBusca.text.toString().trim()
        if (texto.isEmpty()) {
            binding.editBusca.error = "Digite parte do nome"
            return
        }

        lifecycleScope.launch {
            val encontrados = productDao.buscarPorNome("%$texto%")
            exibirLista(encontrados, "Resultado da busca por \"$texto\"")
        }
    }

    // ---------------------------------------------------------------
    // A7: atualizar o preço de um produto pelo id
    // ---------------------------------------------------------------
    private fun atualizarPreco() {
        val id = binding.editProductId.text.toString().trim().toIntOrNull()
        if (id == null) {
            binding.editProductId.error = "Informe o ID do produto"
            return
        }
        val novoPreco = lerPreco(binding.editNovoPreco) ?: return

        lifecycleScope.launch {
            val produto = productDao.buscarPorId(id)
            if (produto == null) {
                toast("Produto com ID $id não encontrado")
                return@launch
            }
            productDao.update(produto.copy(price = novoPreco))
            toast("Preço atualizado!")
            binding.editNovoPreco.text.clear()
            carregarLista()
        }
    }

    // ---------------------------------------------------------------
    // A6: excluir
    // ---------------------------------------------------------------
    private fun excluirPorId() {
        val id = binding.editProductId.text.toString().trim().toIntOrNull()
        if (id == null) {
            binding.editProductId.error = "Informe o ID do produto"
            return
        }

        lifecycleScope.launch {
            val produto = productDao.buscarPorId(id)
            if (produto == null) {
                toast("Produto com ID $id não encontrado")
                return@launch
            }
            productDao.delete(produto)
            toast("Produto excluído!")
            carregarLista()
        }
    }

    private fun confirmarExcluirTodos() {
        AlertDialog.Builder(this)
            .setTitle("Excluir todos")
            .setMessage("Deseja apagar todos os produtos? Esta ação não pode ser desfeita.")
            .setPositiveButton("Excluir") { _, _ ->
                lifecycleScope.launch {
                    productDao.deleteAll()
                    toast("Todos os produtos foram excluídos")
                    binding.textResultado.text = "Nenhum produto encontrado."
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun toast(mensagem: String) {
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "TAG"
    }
}
