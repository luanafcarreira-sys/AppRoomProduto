# AppRoomProduto: Cadastro de Produtos com Room SQLite

**Aluna:** Luanaa Carreira
**RA:** 231572
**Disciplina:** Programação para Dispositivos Móveis
**Professor:** Alessandro

## Sobre o projeto

Aplicativo Android em Kotlin que cadastra e lista produtos
usando persistência local com Room.

## Tecnologias

- Kotlin
- Room 2.8.5 (SQLite) com KSP
- ViewBinding
- Corrotinas (lifecycleScope)

## Prints do app

[Tela inicial](docs/prints/tela-inicial.png)

[Logcat com a listagem](docs/prints/logcat.png)

---

## Parte A: Complemento prático

### A1. Limpar campos após inserir

**O que foi feito:** depois que o produto é gravado com sucesso, a função `limparCampos()` apaga o conteúdo dos campos (nome, preço, descrição e quantidade), remove mensagens de erro que tenham ficado e chama `requestFocus()` no campo Nome para o cursor voltar para ele.

**Trecho de código:**

```kotlin
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
```

**Print:** [A1](docs/prints/a1.png)

### A2. Validação de preço

**O que foi feito:** a função `lerPreco()` converte o texto do campo para número (aceitando vírgula ou ponto). Se o campo estiver vazio, for inválido, zero ou negativo, mostra o erro no próprio campo e o produto não é salvo.

**Trecho de código:**

```kotlin
private fun lerPreco(campo: EditText): Double? {
    val valor = campo.text.toString().trim().replace(',', '.').toDoubleOrNull()
    if (valor == null || valor <= 0.0) {
        campo.error = "Informe um preço válido"
        return null
    }
    return valor
}

// dentro de inserirProduto():
val price = lerPreco(binding.editProductPrice) ?: return
```

**Print:** [A2](docs/prints/a2.png)

### A3. Lista na tela

**O que foi feito:** foi adicionado um `TextView` (`textResultado`) no layout. Ao clicar em Listar Produtos, os produtos são lidos com `getAll()` e exibidos nele, um por linha, além de continuarem sendo escritos no Logcat.

**Trecho de código:**

```kotlin
val linhas = products.joinToString("\n") {
    "#${it.id} - ${it.name} | ${formatoMoeda.format(it.price)} | Qtd: ${it.quantity} | ${it.description ?: "-"}"
}
binding.textResultado.text = "$titulo\n\n$linhas\n\n..."
```

**Print:** [A3](docs/prints/a3.png)

### A4. Total do estoque

**O que foi feito:** junto da lista aparecem a quantidade de produtos cadastrados (`products.size`) e a soma dos preços (`products.sumOf { it.price }`). Também mostrei o valor em estoque (preço x quantidade).

**Trecho de código:**

```kotlin
val somaPrecos = products.sumOf { it.price }
val valorEstoque = products.sumOf { it.price * it.quantity }

// ...
"Quantidade de produtos: ${products.size}\n" +
"Soma dos preços: ${formatoMoeda.format(somaPrecos)}\n"
```

**Print:** [A4](docs/prints/a4.png)

### A5. Buscar por nome

**O que foi feito:** foi criada no DAO a consulta `buscarPorNome` com `LIKE`. Na tela, há um campo de busca e o botão Buscar. O texto é enviado com `%` antes e depois, para encontrar nomes que apenas contenham o que foi digitado.

**Trecho de código:**

```kotlin
// ProductDao.kt
@Query("SELECT * FROM product WHERE name LIKE :nome")
suspend fun buscarPorNome(nome: String): List<Product>

// MainActivity.kt
val encontrados = productDao.buscarPorNome("%$texto%")
exibirLista(encontrados, "Resultado da busca por \"$texto\"")
```

**Print:** [A5](docs/prints/a5.png)

### A6. Excluir produtos

**O que foi feito:** foram criados `deleteAll()` (botão Excluir Todos, com uma caixa de confirmação antes de apagar) e, como desafio extra, `@Delete` para excluir um produto específico pelo id (botão Excluir por ID).

**Trecho de código:**

```kotlin
// ProductDao.kt
@Query("DELETE FROM product")
suspend fun deleteAll()

@Delete
suspend fun delete(product: Product)

// MainActivity.kt (excluir por id)
val produto = productDao.buscarPorId(id)
if (produto == null) {
    toast("Produto com ID $id não encontrado")
    return@launch
}
productDao.delete(produto)
```

**Print:** [A6](docs/prints/a6.png)

### A7. Atualizar produto

**O que foi feito:** foi criada no DAO uma função com `@Update`. O usuário informa o id e o novo preço; o app busca o produto, cria uma cópia com o preço novo (`copy(price = novoPreco)`) e grava essa cópia. O `@Update` usa o id para saber qual linha alterar.

**Trecho de código:**

```kotlin
// ProductDao.kt
@Update
suspend fun update(product: Product)

// MainActivity.kt
val produto = productDao.buscarPorId(id)
// ...
productDao.update(produto.copy(price = novoPreco))
```

**Print:** [A7](docs/prints/a7.png)

### A8. Novo campo na tabela (quantity)

**O que foi feito:** foi adicionado o atributo `quantity: Int` na entidade `Product`, um campo "Quantidade em estoque" no layout, e o `version` do `@Database` passou de 1 para 2.

**Trecho de código:**

```kotlin
// Product.kt
@ColumnInfo(name = "quantity") val quantity: Int = 0

// AppDatabase.kt
@Database(entities = [Product::class], version = 2, exportSchema = false)
```

**O que aconteceu com os produtos já cadastrados e por quê:** (confirme com o seu teste e ajuste se o resultado foi diferente) os produtos cadastrados antes da mudança desapareceram. Ao abrir o app, o Room compara a versão do arquivo de banco que já está no celular (1) com a versão declarada no código (2). Como a estrutura mudou e não existe uma `Migration` ensinando a converter a versão 1 para a 2, entra em ação o `fallbackToDestructiveMigration()` configurado no `databaseBuilder`: ele apaga as tabelas antigas e cria as novas já com a coluna `quantity`. Por isso a listagem veio vazia. Sem esse método e sem Migration, o app fecharia com um erro de migração.

**Print:** [A8](docs/prints/a8.png)

---

## Parte B: Questões dissertativas

### B1. O que é persistência de dados? O que aconteceria com os produtos se fossem guardados apenas em uma MutableList dentro da MainActivity? Persistência é quando o dado continua existindo depois que o app fecha ou o celular reinicia, porque foi guardado em disco. Se os produtos ficassem só em uma MutableList dentro da MainActivity, eles viveriam apenas na memória. Bastaria fechar o app, o sistema encerrá-lo ou até girar a tela (a Activity é recriada) e a lista voltaria vazia. Com o Room, o insertAll grava no arquivo SQLite do celular, e por isso dá para fechar o app, abrir de novo, clicar em Listar e ver tudo lá.

### B2. Qual é a diferença entre usar o SQLite diretamente e usar o Room? Cite pelo menos duas vantagens do Room. No SQLite puro, com SQLiteOpenHelper, você escreve o SQL como texto, lê os resultados com Cursor coluna por coluna e monta os objetos na mão. É muito código repetitivo, e um erro de digitação no SQL só aparece quando o app roda. O Room traz duas vantagens claras: ele confere as consultas na compilação (erro de SQL vira erro de build) e converte linhas em objetos como Product automaticamente. Também é integrado às corrotinas (suspend), tem suporte a migrações e gera o código repetitivo sozinho.

### B3. Explique o papel de Entity, DAO e Database. Como elas se relacionam no seu projeto? A Entity é a classe que vira tabela. No projeto, Product vira a tabela product, e cada atributo vira uma coluna. O DAO é a interface com os comandos do banco, como getAll(), insertAll() e buscarPorNome(), e o Room escreve a implementação. O Database (AppDatabase) junta as duas coisas: lista as entidades, define a versão e expõe o productDao(). A MainActivity cria o banco com Room.databaseBuilder, pega o DAO e o usa nos botões.

### B4. Por que o Android não permite acessar o banco na thread principal? O que é um ANR? Como `suspend` e `lifecycleScope.launch` resolvem esse problema? A thread principal desenha a tela e responde aos toques. Acessar o disco pode demorar, e se ela ficar travada por alguns segundos o Android mostra o ANR (Application Not Responding), aquele aviso de que o app não está respondendo. Por isso o Room bloqueia consultas na thread principal e lança erro. Quando a função do DAO é suspend, o Room executa a consulta em outra thread e a corrotina fica suspensa sem travar a tela. O lifecycleScope.launch abre essa corrotina presa ao ciclo de vida da Activity: se a tela fechar, ela é cancelada. O resultado volta para a thread principal, onde dá para atualizar a interface.

### B5. O que faz o `fallbackToDestructiveMigration()`? Por que ele é perigoso em um app publicado? O que seria uma Migration? Quando a versão do banco muda e não existe uma regra de migração, esse método faz o Room apagar todas as tabelas e recriá-las vazias. Foi o que aconteceu no A8: ao subir para a versão 2, os produtos antigos sumiram. É perigoso em um app publicado porque o usuário atualiza o app e perde todos os seus dados. Uma Migration é uma classe que diz como ir de uma versão à outra preservando os dados. No A8, por exemplo, seria um ALTER TABLE product ADD COLUMN quantity INTEGER NOT NULL DEFAULT 0 de 1 para 2.

### B6. Qual é a vantagem do ViewBinding em relação ao `findViewById`? Que tipo de erro ele ajuda a evitar? O ViewBinding gera uma classe para cada layout, com referências já tipadas para os componentes (binding.btnInserirProduto). No findViewById você passa um id e recebe um View genérico, que pode ser nulo ou do tipo errado. O ViewBinding evita três erros: usar um id que não existe (vira erro de compilação), referenciar uma view que está em outro layout e converter para o tipo errado (ClassCastException). Ainda deixa o código mais curto e limpo.

### B7. Por que em apps maiores é recomendado criar o banco como Singleton? O que poderia acontecer se várias telas criassem cada uma o seu banco? Cada instância do banco abre conexões e mantém estruturas internas, então é pesada. Se cada tela criasse a sua, você teria vários objetos acessando o mesmo arquivo. Isso gera desperdício de memória e risco de conflitos de acesso (erros do tipo “database is locked”). Além disso, as instâncias não sabem das mudanças umas das outras, então uma tela poderia mostrar dado desatualizado. No próprio tutorial, cada rotação de tela recria a MainActivity e, com ela, um novo banco. O padrão é ter uma única instância para o app todo

### B8. Reflexão pessoal: As dificuldades que eu tive foi na criação das diferentes pastas que não tinha feito antes, e o kapt do PDF não funcionou (AGP 9) e foi trocado pelo KSP, o emulador pediu para ativar o Windows Hypervisor Platform, e tive que reabrir o projeto depois de reiniciar. Um exemplo de aplicativo real em que a persistência local com Room seria útil é um aplicativo de controle de estoque de uma loja. Eu aplicaria o Room para armazenar informações dos produtos, como nome, quantidade e preço, permitindo consultar e atualizar o estoque mesmo sem conexão com a internet. Assim, os dados permaneceriam salvos no dispositivo e poderiam ser acessados sempre que necessário.


